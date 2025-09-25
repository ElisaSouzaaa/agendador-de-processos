package com.elisasouza.agendadordesafiodev.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Date;

@Getter
public class JobAgendadoEvent extends ApplicationEvent {
    private final Long jobId;
    private final Date proximaExecucao;

    public JobAgendadoEvent(Object source, Long jobId, Date proximaExecucao) {
        super(source);
        this.jobId = jobId;
        this.proximaExecucao = proximaExecucao;
    }
}
