package com.elisasouza.agendadordesafiodev.model;

import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TRANSACAO")
@Data
public class Transacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arquivo_retorno_id", nullable = false)
    private ArquivoRetorno arquivoRetorno;

    private Character tipo;
    private BigDecimal valor;
    private LocalDate data;
    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusArquivo status;

    private String codigoSaida;
}