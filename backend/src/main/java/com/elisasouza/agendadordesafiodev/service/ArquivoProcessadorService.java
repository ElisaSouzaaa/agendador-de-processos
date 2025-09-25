package com.elisasouza.agendadordesafiodev.service;

import com.elisasouza.agendadordesafiodev.dto.ArquivoRetornoDTO;
import com.elisasouza.agendadordesafiodev.enums.StatusArquivo;
import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.elisasouza.agendadordesafiodev.event.ArquivoRetornoAtualizadoEvent;
import com.elisasouza.agendadordesafiodev.exception.ArquivoRetornoPersistenceException;
import com.elisasouza.agendadordesafiodev.model.ArquivoRetorno;
import com.elisasouza.agendadordesafiodev.model.Job;
import com.elisasouza.agendadordesafiodev.model.Transacao;
import com.elisasouza.agendadordesafiodev.repository.ArquivoRetornoRepository;
import com.elisasouza.agendadordesafiodev.repository.JobRepository;
import com.elisasouza.agendadordesafiodev.repository.TransacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ArquivoProcessadorService {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoProcessadorService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    @Value("${diretorio.arquivos.base}")
    private String diretorioBase;

    private final JobService jobService;
    private final JobRepository jobRepository;
    private final ArquivoRetornoRepository arquivoRetornoRepository;
    private final TransacaoRepository transacaoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ArquivoProcessadorService(JobService jobService, JobRepository jobRepository,
                                     ArquivoRetornoRepository arquivoRetornoRepository,
                                     TransacaoRepository transacaoRepository, ApplicationEventPublisher eventPublisher) {
        this.jobService = jobService;
        this.jobRepository = jobRepository;
        this.arquivoRetornoRepository = arquivoRetornoRepository;
        this.transacaoRepository = transacaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public boolean processarArquivoDeRetorno(Long jobId, String filePathStr) {
        ArquivoRetorno arqRetorno = arquivoRetornoRepository.findByNomeArquivo(filePathStr)
                .orElseThrow(() -> new RuntimeException("ArquivoRetorno com caminho " + filePathStr + " não encontrado no banco."));

        Job job = arqRetorno.getJob();

        try {
            List<String> linhas = Arrays.asList(this.readFileContent(Paths.get(filePathStr)).split("\\r?\\n"));
            if (linhas.isEmpty()) {
                logger.warn("Arquivo para o Job ID {} está vazio.", jobId);
                return false;
            }

            List<Transacao> transacoes = arqRetorno.getTransacoes();

            if (transacoes == null) {
                transacoes = new ArrayList<>();
            } else {
                transacoes.clear();
            }

            for (int i = 1; i < linhas.size(); i++) {
                String linhaDetalhe = linhas.get(i);
                if (linhaDetalhe.trim().isEmpty()) continue;
                transacoes.add(parseLinhaTransacao(linhaDetalhe, arqRetorno));
            }

            arqRetorno.setTransacoes(transacoes);
            transacaoRepository.saveAll(transacoes);

            boolean todos000 = transacoes.stream().allMatch(t -> "000".equals(t.getCodigoSaida()));
            boolean todos001 = transacoes.stream().allMatch(t -> "001".equals(t.getCodigoSaida()));

            StatusJob statusFinalJob;
            StatusArquivo statusFinalArquivo;
            String subdir;

            if (todos000) {
                statusFinalJob = StatusJob.CONCLUIDO;
                statusFinalArquivo = StatusArquivo.PROCESSADO;
                subdir = "processados";
            } else if (todos001) {
                statusFinalJob = StatusJob.FALHA;
                statusFinalArquivo = StatusArquivo.ERRO;
                subdir = "erros";
            } else {
                statusFinalJob = StatusJob.CONCLUIDO_COM_ERROS;
                statusFinalArquivo = StatusArquivo.CONCLUIDO_COM_ERROS;
                subdir = "com_erros";
            }

            arqRetorno.setStatus(statusFinalArquivo);
            arqRetorno.setDataProcessamento(LocalDateTime.now());

            ArquivoRetorno arqRetornoSalvo = arquivoRetornoRepository.save(arqRetorno);
            eventPublisher.publishEvent(new ArquivoRetornoAtualizadoEvent(this, convertToDTO(arqRetornoSalvo)));

            jobService.finalizarProcessamento(jobId, statusFinalJob);

            escreverArquivoDeSaida(filePathStr, subdir, linhas);

            logger.info("Arquivo {} processado. Job {} atualizado para status {}.",
                    filePathStr, jobId, statusFinalJob);

            return statusFinalJob == StatusJob.CONCLUIDO || statusFinalJob == StatusJob.CONCLUIDO_COM_ERROS;

        } catch (Exception e) {
            logger.error("Erro fatal ao processar arquivo para Job ID {}: {}", jobId, e.getMessage(), e);
            try {
                jobService.finalizarProcessamento(jobId, StatusJob.FALHA);
            } catch (Exception ex) {
                logger.error("Falha ao marcar job {} como FALHA: {}", jobId, ex.getMessage(), ex);
            }
            return false;
        }
    }

    public String readFileContent(Path filePath) {
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ArquivoRetornoPersistenceException("Erro ao ler o arquivo: " + filePath.getFileName() + " | " + e.getMessage());
        }
    }


    private Transacao parseLinhaTransacao(String linhaDetalhe, ArquivoRetorno arqRetorno) {
        Transacao transacao = new Transacao();
        transacao.setArquivoRetorno(arqRetorno);
        transacao.setTipo(linhaDetalhe.charAt(0));

        try {
            BigDecimal valor = new BigDecimal(linhaDetalhe.substring(1, 12)).movePointLeft(2);
            transacao.setValor(valor);
            transacao.setData(LocalDate.parse(linhaDetalhe.substring(12, 20), DATE_FORMATTER));
            transacao.setDescricao(linhaDetalhe.substring(20, 50).trim());

            String codigoSaida = linhaDetalhe.substring(50).trim();
            transacao.setCodigoSaida(codigoSaida);

            if ("000".equals(codigoSaida)) {
                transacao.setStatus(StatusArquivo.PROCESSADO);
            } else {
                transacao.setStatus(StatusArquivo.ERRO);
            }
        } catch (Exception e) {
            logger.warn("Não foi possível parsear a linha de transação: [{}]. Erro: {}", linhaDetalhe, e.getMessage());
            transacao.setStatus(StatusArquivo.ERRO);
        }
        return transacao;
    }

    private void escreverArquivoDeSaida(String originalFilePath, String subdiretorio, List<String> linhas) throws IOException {
        Path basePath = Paths.get(diretorioBase);
        Path outputDir = basePath.resolve(subdiretorio);
        Files.createDirectories(outputDir);

        Path originalPath = Paths.get(originalFilePath);
        String originalFileName = originalPath.getFileName().toString();
        String baseName = originalFileName.endsWith(".txt") ? originalFileName.substring(0, originalFileName.length() - 4) : originalFileName;

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String outputFileName = baseName + "_" + timestamp + "_" + subdiretorio + ".txt";

        Path outputFilePath = outputDir.resolve(outputFileName);
        Files.write(outputFilePath, linhas, StandardCharsets.UTF_8);
        logger.info("Arquivo de saída gerado em: {}", outputFilePath);
    }

    private ArquivoRetornoDTO convertToDTO(ArquivoRetorno entity) {
        ArquivoRetornoDTO dto = new ArquivoRetornoDTO();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJob().getId());
        dto.setNomeArquivo(entity.getNomeArquivo());
        dto.setDataProcessamento(entity.getDataProcessamento());
        dto.setStatus(entity.getStatus());

        dto.setCabecalhoNumerico(entity.getCabecalhoNumerico());
        dto.setCabecalhoTexto(entity.getCabecalhoTexto());
        dto.setCabecalhoCodigo(entity.getCabecalhoCodigo());
        return dto;
    }
}