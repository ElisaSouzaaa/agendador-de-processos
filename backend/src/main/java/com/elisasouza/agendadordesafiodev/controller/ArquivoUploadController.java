package com.elisasouza.agendadordesafiodev.controller;

import com.elisasouza.agendadordesafiodev.dto.ArquivoRetornoDTO;
import com.elisasouza.agendadordesafiodev.dto.TransacaoDTO;
import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.elisasouza.agendadordesafiodev.event.ArquivoRetornoAtualizadoEvent;
import com.elisasouza.agendadordesafiodev.model.ArquivoRetorno;
import com.elisasouza.agendadordesafiodev.model.Job;
import com.elisasouza.agendadordesafiodev.repository.ArquivoRetornoRepository;
import com.elisasouza.agendadordesafiodev.service.AgendamentoService;
import com.elisasouza.agendadordesafiodev.service.ArquivoRetornoService;
import com.elisasouza.agendadordesafiodev.service.JobService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/arquivos")
public class ArquivoUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoUploadController.class);

    @Value("${diretorio.arquivos.base}")
    private String diretorioBase;

    private final AgendamentoService agendamentoService;
    private final ArquivoRetornoService arquivoRetornoService;
    private final JobService jobService;
    private final ArquivoRetornoRepository arquivoRetornoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ArquivoUploadController(AgendamentoService agendamentoService,
                                   ArquivoRetornoService arquivoRetornoService,
                                   JobService jobService,
                                   ArquivoRetornoRepository arquivoRetornoRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.agendamentoService = agendamentoService;
        this.arquivoRetornoService = arquivoRetornoService;
        this.jobService = jobService;
        this.arquivoRetornoRepository = arquivoRetornoRepository;
        this.eventPublisher = eventPublisher;
    }


    @PostMapping("/upload/{jobId}")
    public ResponseEntity<String> uploadArquivo(@PathVariable Long jobId, @RequestParam("file") MultipartFile file) {
        try {
            Job job = jobService.findByIdEntity(jobId);
            Path basePath = Paths.get(diretorioBase);
            Path pendentesDir = basePath.resolve("pendentes");
            Files.createDirectories(pendentesDir);

            String nomeOriginal = Objects.requireNonNull(file.getOriginalFilename());
            String nomePadronizado = "job_" + jobId + "_" + nomeOriginal;
            Path caminhoPendente = pendentesDir.resolve(nomePadronizado);

            Files.write(caminhoPendente, file.getBytes());
            logger.info("Arquivo salvo em 'pendentes' como: {}", nomePadronizado);

            ArquivoRetorno novoArquivo = new ArquivoRetorno();
            novoArquivo.setJob(job);
            novoArquivo.setNomeArquivo(caminhoPendente.toString());
            novoArquivo.setStatus(StatusArquivo.PENDENTE);
            novoArquivo.setDataProcessamento(LocalDateTime.now());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String linhaCabecalho = reader.readLine();
                if (linhaCabecalho != null) {
                    parseLinhaCabecalho(linhaCabecalho, novoArquivo);
                }
            }

            ArquivoRetorno arquivoSalvo = arquivoRetornoRepository.save(novoArquivo);

            eventPublisher.publishEvent(new ArquivoRetornoAtualizadoEvent(this, new ArquivoRetornoDTO(arquivoSalvo)));

            jobService.atualizarStatus(jobId, StatusJob.PROCESSANDO);

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Arquivo recebido. O processamento foi agendado e o status atualizado.");
        } catch (Exception e) {
            logger.error("Erro ao fazer upload para Job ID {}: {}", jobId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao fazer upload: " + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPorId(@PathVariable Long id) {
        arquivoRetornoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ArquivoRetornoDTO>> getArquivosPorJobId(@PathVariable Long jobId) {
        return ResponseEntity.ok(arquivoRetornoService.findArquivosByJobId(jobId));
    }

    @GetMapping("/{arquivoId}/transacoes")
    public ResponseEntity<List<TransacaoDTO>> getTransacoesPorArquivoId(@PathVariable Long arquivoId) {
        return ResponseEntity.ok(arquivoRetornoService.findTransacoesByArquivoId(arquivoId));
    }


    private void parseLinhaCabecalho(String linhaHeader, ArquivoRetorno arqRetorno) {
        int fimNum = 0;
        for (char c : linhaHeader.toCharArray()) {
            if (!Character.isDigit(c)) break;
            fimNum++;
        }
        int iniCod = linhaHeader.indexOf('_', fimNum);

        if (fimNum > 0 && iniCod != -1) {
            arqRetorno.setCabecalhoNumerico(linhaHeader.substring(0, fimNum));
            arqRetorno.setCabecalhoTexto(linhaHeader.substring(fimNum, iniCod));
            arqRetorno.setCabecalhoCodigo(linhaHeader.substring(iniCod));
        }
    }
}