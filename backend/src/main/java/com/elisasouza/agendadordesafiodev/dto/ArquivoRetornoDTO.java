package com.elisasouza.agendadordesafiodev.dto;

import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import com.elisasouza.agendadordesafiodev.model.ArquivoRetorno;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArquivoRetornoDTO {
    private Long id;
    private Long jobId;
    private String nomeArquivo;
    private StatusArquivo status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataProcessamento;

    private String cabecalhoNumerico;
    private String cabecalhoTexto;
    private String cabecalhoCodigo;

    public ArquivoRetornoDTO(ArquivoRetorno entity) {
        this.id = entity.getId();
        this.jobId = entity.getJob().getId();
        this.nomeArquivo = entity.getNomeArquivo();
        this.status = entity.getStatus();
        this.dataProcessamento = entity.getDataProcessamento();
        this.cabecalhoNumerico = entity.getCabecalhoNumerico();
        this.cabecalhoTexto = entity.getCabecalhoTexto();
        this.cabecalhoCodigo = entity.getCabecalhoCodigo();
    }

    public ArquivoRetornoDTO() {
    }
}