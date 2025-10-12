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
import java.time.LocalDate;
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
    public HorariosDisponiveisDTO listarHorariosPorServicoEData(Integer servicoId, LocalDate dataSelecionada) {
        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado."));

        if (!servico.isAtivo()) {
            throw new IllegalArgumentException("Serviço está desativado.");
        }

        Prestador prestador = servico.getPrestador();
        int duracao = servico.getDuracaoMinutos();
        DayOfWeek diaSemana = dataSelecionada.getDayOfWeek();

        // Obtém a disponibilidade do prestador para aquele dia da semana
        Disponibilidade disponibilidade = disponibilidadeRepository
                .findByPrestadorId(prestador.getId()).stream()
                .filter(d -> d.getDiaSemana().name().equalsIgnoreCase(traduzirDiaDaSemana(diaSemana)))
                .filter(Disponibilidade::isAtivo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("O prestador não tem disponibilidade nesse dia."));

        // Agendamentos já marcados para aquele dia
        List<Agendamento> agendamentos = agendamentoRepository.findByPrestadorIdAndDataHoraBetween(
                prestador.getId(),
                dataSelecionada.atStartOfDay(),
                dataSelecionada.plusDays(1).atStartOfDay()
        );

        List<String> horariosDisponiveis = new ArrayList<>();
        LocalTime hora = disponibilidade.getHoraInicio();
        LocalTime limite = disponibilidade.getHoraFim().minusMinutes(duracao);

        while (!hora.isAfter(limite)) {
            LocalTime inicio = hora;
            LocalTime fim = hora.plusMinutes(duracao);

            boolean conflita = agendamentos.stream().anyMatch(ag ->
                    overlaps(inicio, fim,
                            ag.getDataHora().toLocalTime(),
                            ag.getDataHora().toLocalTime().plusMinutes(ag.getServico().getDuracaoMinutos()))
            );

            if (!conflita) {
                horariosDisponiveis.add(inicio.toString());
            }

            hora = hora.plusMinutes(duracao);
        }

        List<HorariosPorDiaDTO> dias = new ArrayList<>();
        if (!horariosDisponiveis.isEmpty()) {
            dias.add(new HorariosPorDiaDTO(DiaSemanaDisponivel.valueOf(traduzirDiaDaSemana(diaSemana)), horariosDisponiveis));
        }

        return new HorariosDisponiveisDTO(servicoId, dias);
    }


    private boolean overlaps(LocalTime inicio1, LocalTime fim1, LocalTime inicio2, LocalTime fim2) {
        return !(fim1.isBefore(inicio2) || inicio1.isAfter(fim2) || fim1.equals(inicio2) || inicio1.equals(fim2));
    }

    private String traduzirDiaDaSemana(DayOfWeek dia) {
        return switch (dia) {
            case SUNDAY -> "DOMINGO";
            case MONDAY -> "SEGUNDA";
            case TUESDAY -> "TERCA";
            case WEDNESDAY -> "QUARTA";
            case THURSDAY -> "QUINTA";
            case FRIDAY -> "SEXTA";
            case SATURDAY -> "SABADO";
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