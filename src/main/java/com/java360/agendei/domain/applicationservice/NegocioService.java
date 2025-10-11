package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.CategoriaNegocio;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.domain.repository.PrestadorRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.*;
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

    @Transactional
    public Negocio criarNegocio(CreateNegocioDTO dto) { // do dto vem os dados para criação
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
                .cep(dto.getCep())
                .categoria(dto.getCategoria())
                .criador(prestador)
                .ativo(true) // Por padrão negocio é criado ativo
                .build();

        Negocio criado = negocioRepository.save(negocio);

        // Associa o criador ao negócio e persiste a atualização do prestador
        prestador.setNegocio(criado);
        usuarioRepository.save(prestador); // ou prestadorRepository.save(prestador)

        return criado;
    }

    @Transactional
    public Negocio atualizarNegocio(Integer id, UpdateNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Negocio negocio = negocioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        boolean isDono = negocio.getCriador().getId().equals(usuario.getId());
        boolean isAdmin = PermissaoUtils.isAdmin(usuario);

        if (!isDono && !isAdmin) {
            throw new SecurityException("Você não tem permissão para atualizar este negócio.");
        }

        if (!negocio.isAtivo() && !isAdmin) {
            throw new IllegalArgumentException("Não é possível atualizar um negócio inativo.");
        }

        if (dto.getNome() != null && !dto.getNome().equalsIgnoreCase(negocio.getNome())) {
            if (negocioRepository.existsByNome(dto.getNome())) {
                throw new IllegalArgumentException("Nome do negócio já está em uso.");
            }
            negocio.setNome(dto.getNome());
        }

        if (dto.getEndereco() != null) negocio.setEndereco(dto.getEndereco());
        if (dto.getCep() != null) negocio.setCep(dto.getCep());
        if (dto.getCategoria() != null) negocio.setCategoria(dto.getCategoria());

        // Só admin pode ativar/desativar
        if (isAdmin && dto.getAtivo() != null) negocio.setAtivo(dto.getAtivo());

        return negocioRepository.save(negocio);
    }



    @Transactional
    public void convidarPrestadorParaNegocio(ConviteNegocioDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;
        Negocio negocio = prestador.getNegocio();
        if (negocio == null) {
            throw new IllegalArgumentException("Você não está vinculado a nenhum negócio.");
        }

        if (!negocio.getCriador().getId().equals(prestador.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new IllegalArgumentException("Apenas o dono do negócio pode convidar prestadores.");
        }

        // Busca o usuário pelo e-mail e valida se é um Prestador
        Usuario usuarioConvidado = usuarioRepository.findByEmail(dto.getEmailPrestador())
                .orElseThrow(() -> new IllegalArgumentException("Prestador com esse e-mail não existe."));

        if (!(usuarioConvidado instanceof Prestador prestadorConvidado)) {
            throw new IllegalArgumentException("Usuário convidado não é um prestador.");
        }
        if (prestadorConvidado.getNegocio() != null) {
            throw new IllegalArgumentException("Prestador já está vinculado a outro negócio.");
        }

        // Associa o prestador convidado ao negócio
        prestadorConvidado.setNegocio(negocio);
        usuarioRepository.save(prestadorConvidado); // garantir persistência
    }

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

        // Desvincula o prestador do negócio
        prestador.setNegocio(null);
        usuarioRepository.save(prestador); // ou prestadorRepository.save(prestador);
    }

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

        // Desvincula prestadores
        List<Prestador> prestadores = prestadorRepository.findByNegocio_Id(negocioId);
        prestadores.forEach(p -> p.setNegocio(null));
    }

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





}
