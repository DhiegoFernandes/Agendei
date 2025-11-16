package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Negocio;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.*;
import com.java360.agendei.infrastructure.dto.admin.ResumoAdministrativoDTO;
import com.java360.agendei.infrastructure.dto.relatorios.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelatorioService {

    private final AgendamentoRepository agendamentoRepository;
    private final NegocioRepository negocioRepository;
    private final PrestadorRepository prestadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicoRepository servicoRepository;


    private Integer getPrestadorIdFromToken() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        if (usuario instanceof Prestador prestador) {
            return prestador.getId();
        }

        throw new SecurityException("Somente prestadores ou administradores podem acessar os relatórios.");
    }

    // Ganhos esperados / realizados / taxa de cancelamento
    public RelatorioFinanceiroDTO relatorioFinanceiroMensal(YearMonth mes) {
        Integer prestadorId  = getPrestadorIdFromToken();

        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        var agendamentos = agendamentoRepository
                .findByPrestadorIdAndDataHoraBetween(prestadorId, inicio.atStartOfDay(), fim.atTime(23, 59));

        BigDecimal ganhosEsperados = agendamentos.stream()
                .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ganhosRealizados = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long total = agendamentos.size();
        long cancelados = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CANCELADO)
                .count();

        // a taxa de cancelamento é a porcentagem total de agendamentos dividido pelos agendamentos cancelados
        double taxaCancelamento = total > 0 ? (cancelados * 100.0 / total) : 0;

        return new RelatorioFinanceiroDTO(ganhosEsperados, ganhosRealizados, taxaCancelamento);
    }

    // Evolução mensal
    public List<EvolucaoMensalDTO> evolucaoMensal(int ano) {
        Integer prestadorId = getPrestadorIdFromToken();

        return java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(m -> {
                    YearMonth mes = YearMonth.of(ano, m);
                    LocalDate inicio = mes.atDay(1);
                    LocalDate fim = mes.atEndOfMonth();

                    BigDecimal total = agendamentoRepository
                            .findByPrestadorIdAndStatusAndDataHoraBetween(
                                    prestadorId, StatusAgendamento.CONCLUIDO,
                                    inicio.atStartOfDay(), fim.atTime(23, 59))
                            .stream()
                            .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new EvolucaoMensalDTO(mes, total);
                })
                .toList();
    }

    // Evolução anual
    public List<EvolucaoAnualDTO> evolucaoAnual(int anoInicio, int anoFim) {
        Integer prestadorId = getPrestadorIdFromToken();

        return java.util.stream.IntStream.rangeClosed(anoInicio, anoFim)
                .mapToObj(ano -> {
                    LocalDate inicio = LocalDate.of(ano, 1, 1);
                    LocalDate fim = LocalDate.of(ano, 12, 31);

                    BigDecimal total = agendamentoRepository
                            .findByPrestadorIdAndStatusAndDataHoraBetween(
                                    prestadorId, StatusAgendamento.CONCLUIDO,
                                    inicio.atStartOfDay(), fim.atTime(23, 59))
                            .stream()
                            .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new EvolucaoAnualDTO(ano, total);
                })
                .toList();
    }

    // Serviços mais vendidos (ordenado do mais vendido para o menos)
    public List<ServicoMaisVendidoDTO> servicosMaisVendidos(YearMonth mes) {
        Integer prestadorId = getPrestadorIdFromToken();

        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        var agendamentos = agendamentoRepository
                .findByPrestadorIdAndStatusAndDataHoraBetween(
                        prestadorId, StatusAgendamento.CONCLUIDO,
                        inicio.atStartOfDay(), fim.atTime(23, 59));

        Map<String, List<Agendamento>> agrupado = agendamentos.stream()
                .collect(Collectors.groupingBy(a -> a.getServico().getTitulo()));

        return agrupado.entrySet().stream()
                .map(e -> new ServicoMaisVendidoDTO(
                        e.getKey(),
                        e.getValue().size(),
                        e.getValue().stream()
                                .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted((a, b) -> Long.compare(b.getQuantidadeAgendamentos(), a.getQuantidadeAgendamentos()))
                .toList();
    }

    @Transactional
    public RelatorioNegocioDTO relatorioNegocio(Integer negocioId, YearMonth mes) {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.PRESTADOR, PerfilUsuario.ADMIN);

        // Buscar o negócio
        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new IllegalArgumentException("Negócio não encontrado."));

        // Apenas o dono ou admin pode ver
        boolean isDono = negocio.getCriador().getId().equals(usuario.getId());
        if (!isDono && !PermissaoUtils.isAdmin(usuario)) {
            throw new SecurityException("Apenas o dono do negócio pode visualizar este relatório.");
        }

        LocalDate inicio = mes.atDay(1);
        LocalDate fim = mes.atEndOfMonth();

        // Buscar todos agendamentos no período
        List<Agendamento> agendamentos = agendamentoRepository.findAll().stream()
                .filter(a -> a.getPrestador() != null)
                .filter(a -> a.getPrestador().getNegocio() != null)
                .filter(a -> a.getPrestador().getNegocio().getId().equals(negocioId))
                .filter(a -> !a.getDataHora().isBefore(inicio.atStartOfDay()) && !a.getDataHora().isAfter(fim.atTime(23, 59)))
                .toList();

        // Totais gerais
        BigDecimal ganhosTotais = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalServicos = agendamentos.stream()
                .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                .count();

        // Lista de prestadores do negócio
        List<Prestador> todosPrestadores = prestadorRepository.findByNegocio_Id(negocioId);

        // Monta relatório por prestador
        List<PrestadorRelatorioDTO> relatorioPrestadores = todosPrestadores.stream().map(prestador -> {
            List<Agendamento> agsPrestador = agendamentos.stream()
                    .filter(a -> a.getPrestador().getId().equals(prestador.getId()))
                    .toList();

            // ganhos
            BigDecimal ganhos = agsPrestador.stream()
                    .filter(a -> a.getStatus() == StatusAgendamento.CONCLUIDO)
                    .map(a -> BigDecimal.valueOf(a.getServico().getValor()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // taxa de cancelamento
            long total = agsPrestador.size();
            long cancelados = agsPrestador.stream().filter(a -> a.getStatus() == StatusAgendamento.CANCELADO).count();
            double taxaCancelamento = total > 0 ? (cancelados * 100.0 / total) : 0.0;

            return new PrestadorRelatorioDTO(prestador.getId(), prestador.getNome(), ganhos, taxaCancelamento);
        }).toList();

        return new RelatorioNegocioDTO(
                negocio.getNome(),
                mes,
                ganhosTotais,
                totalServicos,
                relatorioPrestadores
        );
    }

    @Transactional
    public ResumoAdministrativoDTO resumoAdministrativo() {
        Usuario usuario = UsuarioAutenticado.get();
        PermissaoUtils.validarPermissao(usuario, PerfilUsuario.ADMIN);

        long totalPrestadores = usuarioRepository.countByPerfil(PerfilUsuario.PRESTADOR);
        long totalClientes = usuarioRepository.countByPerfil(PerfilUsuario.CLIENTE);

        long totalServicosAtivos = servicoRepository.countByAtivoTrue();
        long totalNegociosAtivos = negocioRepository.countByAtivoTrue();

        long totalAgendamentos = agendamentoRepository.count();

        return new ResumoAdministrativoDTO(
                totalPrestadores,
                totalClientes,
                totalServicosAtivos,
                totalNegociosAtivos,
                totalAgendamentos
        );
    }


}
