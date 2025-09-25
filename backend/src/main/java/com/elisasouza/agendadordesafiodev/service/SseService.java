package com.elisasouza.agendadordesafiodev.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    private static final Logger logger = LoggerFactory.getLogger(SseService.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public SseService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(600_000L);

        emitters.add(emitter);
        logger.info("Novo cliente conectado. Total de clientes: {}", emitters.size());

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            logger.info("Cliente desconectado (onCompletion). Total de clientes: {}", emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            logger.info("Cliente desconectado (onTimeout). Total de clientes: {}", emitters.size());
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            logger.error("Erro no emitter. Cliente desconectado. Total de clientes: {}. Erro: {}", emitters.size(), e.getMessage());
        });

        try {
            emitter.send(SseEmitter.event().name("connected").data("Conexão SSE estabelecida com sucesso!"));
        } catch (IOException e) {
            logger.error("Falha ao enviar evento de conexão inicial.", e);
        }

        return emitter;
    }

    public void sendEvent(Object data) {
        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(data);
        } catch (IOException e) {
            logger.error("Erro ao serializar o objeto do evento para JSON", e);
            return;
        }

        logger.info("Enviando evento para {} clientes: {}", emitters.size(), eventJson);

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(eventJson));
            } catch (IOException e) {
                logger.warn("Falha ao enviar evento para um cliente. Pode ter sido desconectado: {}", e.getMessage());
            }
        }
    }
}