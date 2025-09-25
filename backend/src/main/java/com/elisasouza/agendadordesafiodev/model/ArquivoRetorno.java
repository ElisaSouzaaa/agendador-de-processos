package com.elisasouza.agendadordesafiodev.model;

import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name= "ARQUIVO_RETORNO")
@Data
public class ArquivoRetorno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jobId", nullable = false)
    private Job job;

    @Column(nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusArquivo status;

    @Column(nullable = false)
    private LocalDateTime dataProcessamento;

    private String cabecalhoNumerico;
    private String cabecalhoTexto;
    private String cabecalhoCodigo;

    @OneToMany(mappedBy = "arquivoRetorno", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transacao> transacoes = new ArrayList<>();
}