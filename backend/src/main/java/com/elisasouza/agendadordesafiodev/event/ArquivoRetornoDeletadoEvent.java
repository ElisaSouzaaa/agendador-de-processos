package com.elisasouza.agendadordesafiodev.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ArquivoRetornoDeletadoEvent extends ApplicationEvent {
    private final Long arquivoId;

    public ArquivoRetornoDeletadoEvent(Object source, Long arquivoId) {
        super(source);
        this.arquivoId = arquivoId;
    }

}