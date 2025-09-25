package com.elisasouza.agendadordesafiodev.dto;

import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobDTO {

    private Long id;

    @NotNull
    @Size(max = 255)
    private String nome;

    @NotNull
    @Size(max = 50)
    private String cronExpression;

    @NotNull
    private StatusJob status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime ultimaExecucao;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime proximaExecucao;
}
