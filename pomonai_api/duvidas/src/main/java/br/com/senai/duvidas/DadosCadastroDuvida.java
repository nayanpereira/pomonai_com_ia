package br.com.senai.duvidas;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DadosCadastroDuvida(
    @NotBlank String texto,
    @NotBlank @Pattern(regexp = "NENHUMA|CURTA|LONGA") String tipoPausa) {
 
}
