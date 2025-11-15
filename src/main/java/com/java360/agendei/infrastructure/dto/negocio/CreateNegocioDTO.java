package com.java360.agendei.infrastructure.dto.negocio;

import com.java360.agendei.domain.model.CategoriaNegocio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNegocioDTO {

    @NotBlank(message = "O nome do negócio é obrigatório.")
    private String nome;

    @NotBlank(message = "O endereço é obrigatório.")
    private String endereco;

    @NotBlank(message = "O número do endereço é obrigatório.")
    @Size(max = 10, message = "O número deve ter no máximo 10 caracteres.")
    private String numero;

    @NotBlank(message = "O CEP é obrigatório.")
    @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP inválido. Use o formato 00000000 ou 00000-000.")
    private String cep;

    @NotNull(message = "A categoria do negócio é obrigatória.")
    private CategoriaNegocio categoria;

}
