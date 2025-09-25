package com.elisasouza.agendadordesafiodev.event;

import com.elisasouza.agendadordesafiodev.dto.ArquivoRetornoDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ArquivoRetornoAtualizadoEvent extends ApplicationEvent {
    private final ArquivoRetornoDTO arquivoRetornoDTO;

    public ArquivoRetornoAtualizadoEvent(Object source, ArquivoRetornoDTO arquivoRetornoDTO) {
        super(source);
        this.arquivoRetornoDTO = arquivoRetornoDTO;
    }

}