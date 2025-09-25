package com.elisasouza.agendadordesafiodev.dto;

import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransacaoDTO {
    private Long id;
    private Character tipo;
    private BigDecimal valor;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate data;

    private String descricao;
    private String codigoSaida;
    private StatusArquivo status;
}