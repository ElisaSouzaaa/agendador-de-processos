package com.elisasouza.agendadordesafiodev.model;

import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "JOB")
@Data
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    @Size(max = 255)
    @NotNull
    private String nome;

    @Column(length = 50, nullable = false)
    @Size(max = 50)
    @NotNull
    private String cronExpression;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private StatusJob status;

    @Column
    private LocalDateTime ultimaExecucao;

    @Column
    private LocalDateTime proximaExecucao;

    @OneToMany(mappedBy = "job",fetch= FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArquivoRetorno> arquivoRetornos = new ArrayList<>();
}