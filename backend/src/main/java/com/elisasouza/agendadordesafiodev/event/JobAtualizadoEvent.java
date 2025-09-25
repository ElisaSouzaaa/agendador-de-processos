package com.elisasouza.agendadordesafiodev.event;

import com.elisasouza.agendadordesafiodev.dto.JobDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
@Getter
public class JobAtualizadoEvent extends ApplicationEvent {
    private final JobDTO jobDTO;

    public JobAtualizadoEvent(Object source, JobDTO jobDTO) {
        super(source);
        this.jobDTO = jobDTO;
    }

}