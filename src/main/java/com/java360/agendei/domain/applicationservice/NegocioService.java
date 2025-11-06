package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.PlanoPrestador;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.*;
import com.java360.agendei.infrastructure.dto.negocio.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import com.java360.agendei.infrastructure.util.DistanciaUtils;
import com.java360.agendei.infrastructure.util.GeocodingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NegocioService {
    private final NegocioRepository negocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;
    private final PrestadorRepository prestadorRepository;
    private final GeocodingService geocodingService;
    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public NegocioDTO criarNegocio(CreateNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;

        if (prestador.getNegocio() != null) {
            throw new IllegalArgumentException("Você já está vinculado a um negócio. Saia do atual antes de criar outro.");
        }

        if (negocioRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Nome do negócio já está em uso.");
        }

        Negocio negocio = Negocio.builder()
                .nome(dto.getNome())
                .endereco(dto.getEndereco())
                .numero(dto.getNumero())
                .cep(dto.getCep())
                .categoria(dto.getCategoria())
                .criador(prestador)
                .ativo(true) // por padrão negocio é criado ativo
                .build();

        Negocio criado = negocioRepository.save(negocio);

        prestador.setNegocio(criado);
        usuarioRepository.save(prestador); // ou prestadorRepository.save(prestador)

        return NegocioDTO.fromEntity(criado);
    }

    @Transactional
    public NegocioDTO atualizarNegocio(Integer id, UpdateNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        boolean isAdmin = PermissaoUtils.isAdmin(usuario);
        boolean isPrestador = usuario.getPerfil() == PerfilUsuario.PRESTADOR;
        boolean isDono = isPrestador && negocio.getCriador().getId().equals(usuario.getId());

        if (!isDono && !isAdmin) {
            throw new SecurityException("Você não tem permissão para atualizar este negócio.");
        }

        if (!negocio.isAtivo() && !isAdmin) {
            throw new IllegalArgumentException("Não é possível atualizar um negócio inativo.");
        }

        // Atualização conforme perfil
        if (isAdmin) {
            if (dto.getNome() != null && !dto.getNome().equalsIgnoreCase(negocio.getNome())) {
                if (negocioRepository.existsByNome(dto.getNome())) {
                    throw new IllegalArgumentException("Nome do negócio já está em uso.");
                }
                negocio.setNome(dto.getNome());
            }
            if (dto.getEndereco() != null) negocio.setEndereco(dto.getEndereco());
            if (dto.getNumero() != null) negocio.setNumero(dto.getNumero());
            if (dto.getCep() != null) negocio.setCep(dto.getCep());
            if (dto.getCategoria() != null) negocio.setCategoria(dto.getCategoria());
            if (dto.getAtivo() != null) negocio.setAtivo(dto.getAtivo());
        }

        if (isDono && !isAdmin) {
            if (dto.getNome() != null && !dto.getNome().equalsIgnoreCase(negocio.getNome())) {
                if (negocioRepository.existsByNome(dto.getNome())) {
                    throw new IllegalArgumentException("Nome do negócio já está em uso.");
                }
                negocio.setNome(dto.getNome());
            }
            if (dto.getCategoria() != null) negocio.setCategoria(dto.getCategoria());

            if (dto.getEndereco() != null || dto.getNumero() != null || dto.getCep() != null || dto.getAtivo() != null) {
                throw new SecurityException("Prestadores só podem alterar o nome e a categoria do negócio.");
            }
        }

        Negocio atualizado = negocioRepository.save(negocio);
        return NegocioDTO.fromEntity(atualizado);
    }


    @Transactional
    public void convidarPrestadorParaNegocio(ConviteNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestadorDono = (Prestador) usuario;
        Negocio negocio = prestadorDono.getNegocio();
        if (negocio == null) {
            throw new IllegalArgumentException("Você não está vinculado a nenhum negócio.");
        }

        if (!negocio.getCriador().getId().equals(prestadorDono.getId()) && !PermissaoUtils.isAdmin(usuario)) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode convidar prestadores.");
        }

        // Normaliza o e-mail antes da busca
        String emailNormalizado = dto.getEmailPrestador().toLowerCase().trim();

        // Verifica o limite do plano
        PlanoPrestador plano = prestadorDono.getPlano();
        long quantidadeAtual = prestadorRepository.findByNegocio_Id(negocio.getId()).size() - 1;

        if (quantidadeAtual >= plano.getLimiteConvites()) {
            throw new IllegalArgumentException(
                    String.format("Seu plano (%s) permite no máximo %d prestadores adicionais. Faça um upgrade para adicionar mais.",
                            plano.name(), plano.getLimiteConvites())
            );
        }

        // Busca o prestador com e-mail normalizado
        Usuario usuarioConvidado = usuarioRepository.findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("Prestador com esse e-mail não existe."));

        if (!(usuarioConvidado instanceof Prestador prestadorConvidado)) {
            throw new IllegalArgumentException("Usuário convidado não é um prestador.");
        }

        if (prestadorConvidado.getNegocio() != null) {
            throw new IllegalArgumentException("Prestador já está vinculado a outro negócio.");
        }

        prestadorConvidado.setNegocio(negocio);
        usuarioRepository.save(prestadorConvidado);
    }



    // Sair prestador convidado
    @Transactional
    public void sairDoNegocio() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;

        Negocio negocio = prestador.getNegocio();
        if (negocio == null) {
            throw new IllegalArgumentException("Prestador não está associado a nenhum negócio.");
        }

        if (negocio.getCriador().getId().equals(prestador.getId())) {
            throw new IllegalArgumentException("O dono do negócio não pode se desvincular.");
        }

        // Desativa os serviços ativos desse prestador nesse negócio
        List<Servico> servicos = servicoRepository.findByPrestadorIdAndNegocioId(prestador.getId(), negocio.getId());
        servicos.forEach(s -> s.setAtivo(false));

        // Cancela agendamentos pendentes
        List<Agendamento> agendamentosAtivos = agendamentoRepository.findByPrestadorId(prestador.getId());
        agendamentosAtivos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.PENDENTE)
                .forEach(a -> a.setStatus(StatusAgendamento.CANCELADO));

        // Desvincula o prestador do negócio
        prestador.setNegocio(null);
        usuarioRepository.save(prestador); // ou prestadorRepository.save(prestador);
    }

    // Sair APENAS DONO
    @Transactional
    public void excluirNegocio(Integer negocioId) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        if (!negocio.getCriador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode excluí-lo.");
        }

        negocio.setAtivo(false);

        // Desativa serviços
        List<Servico> servicos = servicoRepository.findByNegocio_IdAndAtivoTrue(negocioId);
        servicos.forEach(s -> s.setAtivo(false));

        // Desvincula prestadores e cancela agendamentos de todos
        List<Prestador> prestadores = prestadorRepository.findByNegocio_Id(negocioId);
        for (Prestador p : prestadores) {
            // Cancela todos os agendamentos pendentes do prestador
            List<Agendamento> ags = agendamentoRepository.findByPrestadorId(p.getId());
            ags.stream()
                    .filter(a -> a.getStatus() == StatusAgendamento.PENDENTE)
                    .forEach(a -> a.setStatus(StatusAgendamento.CANCELADO));

            p.setNegocio(null);
        }
    }

    // Todo ADD LIMITE 20KM
    @Transactional
    public List<NegocioBuscaDTO> buscarNegociosProximos(String nome, CategoriaNegocio categoria) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.CLIENTE);

        if (!(usuario instanceof Cliente cliente)) {
            throw new IllegalArgumentException("Somente clientes podem buscar negócios próximos.");
        }

        if (cliente.getCep() == null || cliente.getCep().isBlank()) {
            throw new IllegalArgumentException("Cliente não possui CEP cadastrado.");
        }

        LatLngDTO clienteLatLng = geocodingService.buscarLatLongPorCep(cliente.getCep());

        List<Negocio> negocios = negocioRepository.findByAtivoTrue().stream()
                .filter(n -> (nome == null || n.getNome().toLowerCase().contains(nome.toLowerCase())))
                .filter(n -> (categoria == null || n.getCategoria() == categoria))
                .filter(n -> n.getCep() != null && !n.getCep().isBlank())
                .toList();

        return negocios.stream()
                .map(n -> {
                    try {
                        LatLngDTO negocioLatLng = geocodingService.buscarLatLongPorCep(n.getCep());
                        double distancia = DistanciaUtils.calcularDistancia(
                                clienteLatLng.getLat(), clienteLatLng.getLng(),
                                negocioLatLng.getLat(), negocioLatLng.getLng()
                        );
                        return NegocioBuscaDTO.fromEntity(n, distancia);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .sorted((a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()))
                .toList();
    }

    @Transactional
    public NegocioDTO buscarNegocioPorId(Integer id) {
        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));
        return NegocioDTO.fromEntity(negocio);
    }


}
