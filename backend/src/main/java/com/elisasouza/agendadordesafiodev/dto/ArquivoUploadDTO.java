package com.elisasouza.agendadordesafiodev.dto;

import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArquivoUploadDTO {
    private String nomeArquivo;
    private StatusJob statusJob;
    private int linhasProcessadas;
}