package com.elisasouza.agendadordesafiodev.listener;

import com.elisasouza.agendadordesafiodev.event.ArquivoRetornoAtualizadoEvent;
import com.elisasouza.agendadordesafiodev.event.ArquivoRetornoDeletadoEvent;
import com.elisasouza.agendadordesafiodev.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class ArquivoRetornoEventListener {

    private final SseService sseService;

    public ArquivoRetornoEventListener(SseService sseService) {
        this.sseService = sseService;
    }


    @TransactionalEventListener
    public void handleArquivoRetornoAtualizadoEvent(ArquivoRetornoAtualizadoEvent event) {
        sseService.sendEvent(event.getArquivoRetornoDTO());
    }

    @TransactionalEventListener
    public void handleArquivoRetornoDeletadoEvent(ArquivoRetornoDeletadoEvent event) {
        sseService.sendEvent(Map.of(
                "action", "delete_arquivo",
                "arquivoId", event.getArquivoId()
        ));
    }
}