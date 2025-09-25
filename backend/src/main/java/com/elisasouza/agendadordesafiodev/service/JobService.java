package com.elisasouza.agendadordesafiodev.service;

import com.elisasouza.agendadordesafiodev.dto.JobDTO;
import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.elisasouza.agendadordesafiodev.event.JobAgendadoEvent;
import com.elisasouza.agendadordesafiodev.event.JobAtualizadoEvent;
import com.elisasouza.agendadordesafiodev.event.JobDeletadoEvent;
import com.elisasouza.agendadordesafiodev.exception.JobNotFoundException;
import com.elisasouza.agendadordesafiodev.exception.JobPersistenceException;
import com.elisasouza.agendadordesafiodev.model.Job;
import com.elisasouza.agendadordesafiodev.repository.JobRepository;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final AgendamentoService agendamentoService;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(JobService.class);

    public JobService(JobRepository jobRepository, AgendamentoService agendamentoService, ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.agendamentoService = agendamentoService;
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    @Transactional
    public void handleJobAgendadoEvent(JobAgendadoEvent event) {
        logger.info("Job Agendado Event: {}", event.getJobId());
        this.atualizarProximaExecucao(event.getJobId(), event.getProximaExecucao());
    }

    public List<JobDTO> findAll() {
        List<Job> jobs = jobRepository.findAll();
        if (jobs.isEmpty()) {
            throw new JobNotFoundException("Nenhum job encontrado");
        }
        return jobs.stream()
                .map(this::convertToDTO)
                .toList();
    }

    public Job findByIdEntity(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Nenhum job encontrado com o id: " + id));
    }

    public JobDTO findByIdDTO(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Nenhum job encontrado com o id: " + id));
        return convertToDTO(job);
    }

    @Transactional
    public JobDTO save(JobDTO jobDTO) {
        try {
            Job job = convertToEntity(jobDTO);
            Job savedJob = jobRepository.save(job);

            agendamentoService.agendarOuAtualizarJobRecorrente(savedJob);

            Job jobComProximaExecucao = findByIdEntity(savedJob.getId());
            JobDTO dtoFinal = convertToDTO(jobComProximaExecucao);

            eventPublisher.publishEvent(new JobAtualizadoEvent(this, dtoFinal));

            return dtoFinal;
        } catch (Exception e) {
            throw new JobPersistenceException("Erro ao salvar job: " + e.getMessage());
        }
    }

    public Job saveEntity(Job job) {
        try {
            LocalDateTime now = LocalDateTime.now();

            if ((job.getStatus() == StatusJob.PROCESSANDO || job.getStatus() == StatusJob.CONCLUIDO || job.getStatus() == StatusJob.CONCLUIDO_COM_ERROS || job.getStatus() == StatusJob.FALHA)) {
                job.setUltimaExecucao(now);
            }

            if (job.getCronExpression() != null && !job.getCronExpression().isEmpty()) {
                CronExpression cron = new CronExpression(job.getCronExpression());
                Date nextValid = cron.getNextValidTimeAfter(Date.from(now.atZone(ZoneId.systemDefault()).toInstant()));
                if (nextValid != null) {
                    job.setProximaExecucao(nextValid.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                }
            }

            return jobRepository.save(job);
        } catch (Exception e) {
            throw new JobPersistenceException("Erro ao salvar job: " + e.getMessage());
        }
    }

    @Transactional
    public JobDTO update(Long id, JobDTO jobDTO) {
        try {
            Job jobExistente = findByIdEntity(id);

            if (jobDTO.getNome() != null) jobExistente.setNome(jobDTO.getNome());
            if (jobDTO.getCronExpression() != null) jobExistente.setCronExpression(jobDTO.getCronExpression());

           jobExistente.setStatus(StatusJob.PROCESSANDO);

            Job jobAtualizado = saveEntity(jobExistente);

            agendamentoService.agendarOuAtualizarJobRecorrente(jobAtualizado);

            Job jobComProximaExecucao = findByIdEntity(jobAtualizado.getId());
            JobDTO dtoFinal = convertToDTO(jobComProximaExecucao);

            eventPublisher.publishEvent(new JobAtualizadoEvent(this, dtoFinal));
            return dtoFinal;
        } catch (SchedulerException e) {
            throw new JobPersistenceException("Erro ao atualizar agendamento no Quartz: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteById(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new JobNotFoundException("Nenhum job encontrado com o id: " + id);
        }
        try {
            agendamentoService.deletarJobRecorrente(id);
        } catch (SchedulerException e) {
            logger.warn("Não foi possível remover o job {} do Quartz. Erro: {}", id, e.getMessage());
        }

        jobRepository.deleteById(id);

        eventPublisher.publishEvent(new JobDeletadoEvent(this, id));
    }

    public StatusJob buscarStatus(Long jobId) {
        return jobRepository.findById(jobId)
                .map(Job::getStatus)
                .orElse(StatusJob.FALHA);
    }

    @Transactional
    public Job atualizarStatus(Long jobId, StatusJob novoStatus) {
        Job job = findByIdEntity(jobId);
        job.setStatus(novoStatus);

        Job jobAtualizado = this.saveEntity(job);

        JobDTO updatedDTO = convertToDTO(jobAtualizado);

        logger.info("Publicando JobAtualizadoEvent para o Job ID: {} com status: {}",
                updatedDTO.getId(), updatedDTO.getStatus());

        eventPublisher.publishEvent(new JobAtualizadoEvent(this, updatedDTO));

        return jobAtualizado;
    }

    @Transactional
    public void atualizarExecucoes(Long jobId, Date ultima, Date proxima) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setUltimaExecucao(ultima.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            if (proxima != null) {
                job.setProximaExecucao(proxima.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }else {
                job.setProximaExecucao(null);
            }

            Job jobSalvo = jobRepository.save(job);
            JobDTO dtoAtualizado = convertToDTO(job);

            logger.info("Publicando JobAtualizadoEvent para o Job ID: {} com novas datas de execução.", dtoAtualizado.getId());
            eventPublisher.publishEvent(new JobAtualizadoEvent(this, dtoAtualizado));
        });
    }

    @Transactional
    public void atualizarProximaExecucao(Long jobId, Date proximaExecucao) {
        Job job = findByIdEntity(jobId);
        if (proximaExecucao != null) {
            job.setProximaExecucao(proximaExecucao.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        } else {
            job.setProximaExecucao(null);
        }
        Job jobSalvo = jobRepository.save(job);

        eventPublisher.publishEvent(new JobAtualizadoEvent(this, convertToDTO(jobSalvo)));
    }


    @Transactional
    public void finalizarProcessamento(Long jobId, StatusJob statusFinal) {
        Job job = findByIdEntity(jobId);
        job.setStatus(statusFinal);
        job.setUltimaExecucao(LocalDateTime.now());

        Job jobAtualizado = this.saveEntity(job);

        JobDTO dtoCompleto = convertToDTO(jobAtualizado);

        logger.info("Finalizando processamento do Job ID: {}. Publicando evento completo.", jobId);
        eventPublisher.publishEvent(new JobAtualizadoEvent(this, dtoCompleto));
    }

    private JobDTO convertToDTO(Job job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setNome(job.getNome());
        dto.setCronExpression(job.getCronExpression());
        dto.setStatus(job.getStatus());
        dto.setUltimaExecucao(job.getUltimaExecucao());
        dto.setProximaExecucao(job.getProximaExecucao());
        return dto;
    }

    private Job convertToEntity(JobDTO dto) {
        Job job = new Job();
        job.setId(dto.getId());
        job.setNome(dto.getNome());
        job.setCronExpression(dto.getCronExpression());
        job.setStatus(dto.getStatus());
        job.setUltimaExecucao(dto.getUltimaExecucao());
        job.setProximaExecucao(dto.getProximaExecucao());
        return job;
    }
}