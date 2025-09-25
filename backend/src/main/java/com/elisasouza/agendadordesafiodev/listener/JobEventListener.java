package com.elisasouza.agendadordesafiodev.listener;

import com.elisasouza.agendadordesafiodev.event.JobAtualizadoEvent;
import com.elisasouza.agendadordesafiodev.event.JobDeletadoEvent;
import com.elisasouza.agendadordesafiodev.service.SseService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
public class JobEventListener {

    private final SseService sseService;

    public JobEventListener(SseService sseService) {
        this.sseService = sseService;
    }

    @TransactionalEventListener
    public void handleJobAtualizadoEvent(JobAtualizadoEvent event) {
        sseService.sendEvent(event.getJobDTO());
    }

    @TransactionalEventListener
    public void handleJobDeletadoEvent(JobDeletadoEvent event) {
        sseService.sendEvent(Map.of(
                "action", "delete",
                "jobId", event.getJobId()
        ));
    }
}