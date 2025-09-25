package com.elisasouza.agendadordesafiodev.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class JobDeletadoEvent extends ApplicationEvent {
    private final Long jobId;

    public JobDeletadoEvent(Object source, Long jobId) {
        super(source);
        this.jobId = jobId;
    }
}