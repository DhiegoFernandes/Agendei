package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Servico;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.NegocioRepository;
import com.java360.agendei.domain.repository.PrestadorRepository;
import com.java360.agendei.domain.repository.ServicoRepository;
import com.java360.agendei.domain.repository.UsuarioRepository;
import com.java360.agendei.infrastructure.dto.ConviteNegocioDTO;
import com.java360.agendei.infrastructure.dto.CreateNegocioDTO;
import com.java360.agendei.infrastructure.dto.LatLngDTO;
import com.java360.agendei.infrastructure.dto.NegocioBuscaDTO;
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
    public List<NegocioBuscaDTO> buscarNegociosProximos(String cepCliente) {
        LatLngDTO clienteLatLng = geocodingService.buscarLatLongPorCep(cepCliente);

        List<Negocio> negocios = negocioRepository.findAll()
                .stream()
                .filter(Negocio::isAtivo)
                .toList();

        return negocios.stream()
                .map(n -> {
                    LatLngDTO negocioLatLng = geocodingService.buscarLatLongPorCep(n.getCep());
                    double distancia = DistanciaUtils.calcularDistancia(
                            clienteLatLng.getLat(), clienteLatLng.getLng(),
                            negocioLatLng.getLat(), negocioLatLng.getLng()
                    );
                    return NegocioBuscaDTO.fromEntity(n, distancia);
                })
                .sorted((a, b) -> Double.compare(a.getDistanciaKm(), b.getDistanciaKm()))
                .toList();
    }




}
