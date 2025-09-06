package com.java360.agendei.domain.applicationservice;

import com.java360.agendei.domain.entity.Agendamento;
import com.java360.agendei.domain.entity.Prestador;
import com.java360.agendei.domain.entity.Usuario;
import com.java360.agendei.domain.model.PerfilUsuario;
import com.java360.agendei.domain.model.StatusAgendamento;
import com.java360.agendei.domain.repository.AgendamentoRepository;
import com.java360.agendei.infrastructure.dto.relatorios.*;
import com.java360.agendei.infrastructure.security.PermissaoUtils;
import com.java360.agendei.infrastructure.security.UsuarioAutenticado;
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
        Integer prestadorId = getPrestadorIdFromToken();

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

}
