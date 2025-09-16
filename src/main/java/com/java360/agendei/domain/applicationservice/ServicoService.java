package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.*;
import com.java360.agendei.domain.model.CategoriaServico;
import com.java360.agendei.domain.model.DiaSemanaDisponivel;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadeRepository disponibilidadeRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final NegocioRepository negocioRepository;

    @Transactional
    public Servico cadastrarServico(SaveServicoDTO dto) {

        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Prestador prestador = (Prestador) usuario;

        if (prestador.getNegocio() == null) {
            throw new IllegalArgumentException("Prestador não está associado a um negócio.");
        }

        Negocio negocio = prestador.getNegocio();
        if (negocio == null) {
            throw new IllegalArgumentException("Você não está associado a um negócio.");
        }

        boolean tituloDuplicado = servicoRepository.existsByTituloAndNegocioId(dto.getTitulo(), negocio.getId());
        if (tituloDuplicado) {
            throw new IllegalArgumentException("Já existe um serviço com esse título neste negócio.");
        }

        Servico servico = Servico.builder()
                .titulo(dto.getTitulo())
                .descricao(dto.getDescricao())
                .valor(dto.getValor())
                .duracaoMinutos(dto.getDuracaoMinutos())
                .ativo(true)
                .prestador(prestador)
                .negocio(negocio)
                .build();

        return servicoRepository.save(servico);
    }

    public List<Servico> listarServicosAtivos() {
        return servicoRepository.findAllByAtivoTrue();
    }

    public List<ServicoDTO> listarServicosPorNegocio(Integer negocioId) {
        List<Servico> servicos = servicoRepository.findByNegocio_IdAndAtivoTrue(negocioId);
        return servicos.stream()
                .map(ServicoDTO::fromEntity)
                .toList();
    }

    @Transactional
    public HorariosDisponiveisDTO listarHorariosPorServico(Integer servicoId) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Serviço está desativado.");
        }

        Integer prestadorId = servico.getPrestador().getId();
        int duracao = servico.getDuracaoMinutos();

        List<Disponibilidade> disponibilidades = disponibilidadeRepository.findByPrestadorId(prestadorId);
        List<Agendamento> agendamentos = agendamentoRepository.findByPrestadorId(prestadorId);

        List<HorariosPorDiaDTO> dias = new ArrayList<>();

        for (Disponibilidade d : disponibilidades) {
            List<String> horarios = new ArrayList<>();
            LocalTime hora = d.getHoraInicio();
            LocalTime limite = d.getHoraFim().minusMinutes(duracao);

            while (!hora.isAfter(limite)) {
                LocalTime inicio = hora;
                LocalTime fim = hora.plusMinutes(duracao);

                boolean conflita = agendamentos.stream().anyMatch(ag ->
                        ag.getDataHora().getDayOfWeek().equals(toDayOfWeek(d.getDiaSemana())) &&
                                overlaps(inicio, fim, ag.getDataHora().toLocalTime(), ag.getDataHora().toLocalTime().plusMinutes(ag.getServico().getDuracaoMinutos()))
                );

                if (!conflita) {
                    horarios.add(inicio.toString());
                }

                hora = hora.plusMinutes(duracao);
            }

            if (!horarios.isEmpty()) {
                dias.add(new HorariosPorDiaDTO(d.getDiaSemana(), horarios));
            }
        }

        return new HorariosDisponiveisDTO(servicoId, dias);
    }

    private boolean overlaps(LocalTime inicio1, LocalTime fim1, LocalTime inicio2, LocalTime fim2) {
        return !(fim1.isBefore(inicio2) || inicio1.isAfter(fim2) || fim1.equals(inicio2) || inicio1.equals(fim2));
    }

    private DayOfWeek toDayOfWeek(DiaSemanaDisponivel dia) {
        return switch (dia) {
            case DOMINGO -> DayOfWeek.SUNDAY;
            case SEGUNDA -> DayOfWeek.MONDAY;
            case TERCA -> DayOfWeek.TUESDAY;
            case QUARTA -> DayOfWeek.WEDNESDAY;
            case QUINTA -> DayOfWeek.THURSDAY;
            case SEXTA -> DayOfWeek.FRIDAY;
            case SABADO -> DayOfWeek.SATURDAY;
        };
    }


    @Transactional
    public Servico atualizarServico(Integer id, SaveServicoDTO dto) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.getPrestador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Você não tem permissão para editar este serviço.");
        }

        // Verifica duplicação de título para o mesmo prestador, ignorando o próprio serviço atual
        boolean tituloDuplicado = servicoRepository
                .existsByTituloAndPrestadorIdAndIdNot(dto.getTitulo(), usuario.getId(), id);

        if (tituloDuplicado) {
            throw new IllegalArgumentException("Já existe outro serviço com esse título.");
        }

        servico.setTitulo(dto.getTitulo());
        servico.setDescricao(dto.getDescricao());
        servico.setValor(dto.getValor());
        servico.setDuracaoMinutos(dto.getDuracaoMinutos());
        servico.setAtivo(dto.getAtivo());

        return servico;
    }

    @Transactional
    public void excluirServico(Integer servicoId) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.getPrestador().getId().equals(usuario.getId()) &&
                !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Você não tem permissão para excluir este serviço.");
        }

        servico.setAtivo(false);
    }


    public List<ServicoDTO> buscarServicos(String titulo, String nomePrestador, DiaSemanaDisponivel diaSemana) {
        List<Servico> resultados = servicoRepository.buscarServicos(titulo, nomePrestador, diaSemana);
        return resultados.stream().map(ServicoDTO::fromEntity).toList();
    }
}