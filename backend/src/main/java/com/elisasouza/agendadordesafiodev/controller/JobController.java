package com.elisasouza.agendadordesafiodev.controller;

import com.elisasouza.agendadordesafiodev.dto.JobDTO;
import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.elisasouza.agendadordesafiodev.service.AgendamentoService;
import com.elisasouza.agendadordesafiodev.service.JobService;
import com.elisasouza.agendadordesafiodev.service.SseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final AgendamentoService agendamentoService;
    private final SseService sseService;

    public JobController(JobService jobService, AgendamentoService agendamentoService, SseService sseService) {
        this.jobService = jobService;
        this.agendamentoService = agendamentoService;
        this.sseService = sseService;
    }

    @PostMapping
    public ResponseEntity<JobDTO> criarJob(@RequestBody JobDTO jobDTO) {
        jobDTO.setStatus(StatusJob.AGENDADO);
        JobDTO novoJob = jobService.save(jobDTO);
        sseService.sendEvent(novoJob);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoJob);
    }

    @GetMapping
    public ResponseEntity<List<JobDTO>> listarJobs() {
        return ResponseEntity.ok(jobService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.findByIdDTO(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDTO> atualizarJob(@PathVariable Long id, @RequestBody JobDTO jobDTO) {
        JobDTO jobAtualizado = jobService.update(id, jobDTO);
        jobAtualizado.setStatus(StatusJob.PROCESSANDO);
        sseService.sendEvent(jobAtualizado);
        return ResponseEntity.ok(jobAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarJob(@PathVariable Long id) {
        jobService.deleteById(id);
        sseService.sendEvent(Map.of("action", "delete", "jobId", id));
        return ResponseEntity.noContent().build();
    }
}