package com.elisasouza.agendadordesafiodev.quartz;

import com.elisasouza.agendadordesafiodev.service.ArquivoProcessadorService;
import com.elisasouza.agendadordesafiodev.service.JobService;
import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class ArquivoProcessingJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(ArquivoProcessingJob.class);

    private final ArquivoProcessadorService processorService;
    private final JobService jobService;

    public ArquivoProcessingJob(ArquivoProcessadorService processorService, JobService jobService) {
        this.processorService = processorService;
        this.jobService = jobService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long jobId = (Long) context.getMergedJobDataMap().get("jobId");
        String filePathStr = context.getMergedJobDataMap().getString("filePath");

        logger.info("Quartz Job acionado para o Job ID: {}. Delegando para o serviço de processamento.", jobId);

        Path arquivoOriginal = Paths.get(filePathStr);

        if(!Files.exists(arquivoOriginal)) {
            logger.warn("Arquivo {} não existe. Pulando processamento.", arquivoOriginal.getFileName());
            return;
        }
        Path pastaProcessados = Paths.get("processados/");
        Path pastaComErro = Paths.get("erros/");

        try {
            Files.createDirectories(pastaProcessados);
            Files.createDirectories(pastaComErro);

            boolean sucesso;
            try {
                sucesso = processorService.processarArquivoDeRetorno(jobId, arquivoOriginal.toString());
            } catch (Exception e) {
                logger.error("Erro crítico ao processar arquivo {}. Marcando FALHA.", arquivoOriginal.getFileName(), e);
                jobService.atualizarStatus(jobId, StatusJob.FALHA);
                sucesso = false;
            }

            StatusJob statusFinal = sucesso ? StatusJob.CONCLUIDO : StatusJob.CONCLUIDO_COM_ERROS;
            jobService.atualizarStatus(jobId, statusFinal);

            Path historico = pastaProcessados.resolve(arquivoOriginal.getFileName());
            try {
                Files.copy(arquivoOriginal, historico, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Cópia do arquivo {} mantida no histórico '{}'.", arquivoOriginal.getFileName(), historico);
            } catch (IOException e) {
                logger.error("Erro ao copiar arquivo {} para histórico {}.", arquivoOriginal.getFileName(), historico, e);
            }

            Path destino = sucesso ? pastaProcessados.resolve(arquivoOriginal.getFileName())
                    : pastaComErro.resolve(arquivoOriginal.getFileName());
            try {
                Files.move(arquivoOriginal, destino, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Arquivo {} movido para '{}'.", arquivoOriginal.getFileName(), destino);
            } catch (IOException e) {
                logger.error("Erro ao mover arquivo {} para destino {}.", arquivoOriginal.getFileName(), destino, e);
            }

        } catch (IOException e) {
            logger.error("Erro no ArquivoProcessingJob para o Job ID {}. Causa: {}", jobId, e.getMessage());
            jobService.atualizarStatus(jobId, StatusJob.FALHA);
            throw new JobExecutionException(e);
        }
    }
}