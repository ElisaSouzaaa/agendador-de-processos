package com.elisasouza.agendadordesafiodev.service;

import com.elisasouza.agendadordesafiodev.event.JobAgendadoEvent;
import com.elisasouza.agendadordesafiodev.model.Job;
import com.elisasouza.agendadordesafiodev.quartz.ArquivoProcessingJob;
import com.elisasouza.agendadordesafiodev.quartz.JobRecorrente;
import org.quartz.*;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Date;
import java.util.UUID;

@Service
public class AgendamentoService {

    private final Scheduler scheduler;
    private final ApplicationEventPublisher publisher;

    public AgendamentoService(Scheduler scheduler, ApplicationEventPublisher publisher) {
        this.scheduler = scheduler;
        this.publisher = publisher;
    }

    public void agendarProcessamentoDeArquivo(Long jobId, Path filePath) throws SchedulerException {
        String uniqueJobIdentity = jobId.toString() + "_" + UUID.randomUUID();
        JobDetail jobDetail = JobBuilder.newJob(ArquivoProcessingJob.class)
                .withIdentity(uniqueJobIdentity, "processamento-arquivos")
                .usingJobData("jobId", jobId)
                .usingJobData("filePath", filePath.toString())
                .build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(uniqueJobIdentity)
                .startNow()
                .build();
        scheduler.scheduleJob(jobDetail, trigger);
    }

    public void agendarOuAtualizarJobRecorrente(Job job) throws SchedulerException {
        JobKey jobKey = new JobKey(job.getId().toString(), "jobs-recorrentes");

        JobDetail jobDetail = JobBuilder.newJob(JobRecorrente.class)
                .withIdentity(jobKey)
                .usingJobData("jobId", job.getId())
                .storeDurably()
                .build();

        TriggerKey triggerKey = new TriggerKey(job.getId().toString(), "triggers-recorrentes");
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .forJob(jobDetail)
                .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                .build();

        if (scheduler.checkExists(jobKey)) {
            scheduler.addJob(jobDetail, true);
            scheduler.rescheduleJob(triggerKey, trigger);
        } else {
            scheduler.scheduleJob(jobDetail, trigger);
        }

        Date proximaExecucao = trigger.getNextFireTime();
        publisher.publishEvent(new JobAgendadoEvent(this, job.getId(), proximaExecucao));
    }

    public void deletarJobRecorrente(Long jobId) throws SchedulerException {
        JobKey jobKey = new JobKey(jobId.toString(), "jobs-recorrentes");
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }
}