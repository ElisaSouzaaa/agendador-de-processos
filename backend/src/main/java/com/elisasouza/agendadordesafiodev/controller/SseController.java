package com.elisasouza.agendadordesafiodev.controller;

import com.elisasouza.agendadordesafiodev.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        return sseService.createEmitter();
    }
}