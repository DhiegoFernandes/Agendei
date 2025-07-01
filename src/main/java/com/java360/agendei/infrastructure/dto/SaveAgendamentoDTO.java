package com.java360.agendei.infrastructure.dto;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SaveAgendamentoDataDTO {

    //delimitando tamanho e restrições dos parametros, mais restrições em: https://jakarta.ee/specifications/bean-validation/3.0/apidocs/jakarta/validation/constraints/package-summary
    @NotNull(message = "Nome não pode ser vazio")
    @Size(min = 1, max = 80, message = "Nome invalido")
    private final String name;

    @NotNull(message = "A descrição não pode ser vazia")
    @Size(min = 1, max = 150, message = "Descrição inválida")
    private final String description;

    @NotNull(message = "A data inicial não pode ser vazia")
    private final LocalDate initialDate;

    @NotNull(message = "A data final não pode ser vazia")
    private final LocalDate finalDate;

    private final String status;


    //Pequena logica com um metodo que valida as datas
    @AssertTrue(message = "Datas não são consistentes")
    @SuppressWarnings("unused") //Marca o metodo como em uso, mesmo não estando, corrige a IDE
    private boolean isInitialDateBeforeFinalDate(){
        return initialDate.isBefore(finalDate); //true
    }
}


