package com.elisasouza.agendadordesafiodev.quartz;

import com.elisasouza.agendadordesafiodev.enums.StatusJob;
import com.elisasouza.agendadordesafiodev.service.ArquivoProcessadorService;
import com.elisasouza.agendadordesafiodev.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
@Component
public class JobRecorrente implements Job {

    private final ArquivoProcessadorService processorService;
    private final JobService jobService;
    @Value("${diretorio.arquivos.base}")
    private String diretorioBase;

    public JobRecorrente(ArquivoProcessadorService processorService, JobService jobService) {
        this.processorService = processorService;
        this.jobService = jobService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long jobId = (Long) context.getMergedJobDataMap().get("jobId");
        log.info(">>>> VIGIA (CRON) ACORDOU PARA O AGENDAMENTO ID: {} <<<<", jobId);

        Path basePath = Paths.get(diretorioBase);
        Path pastaPendente = basePath.resolve("pendentes");
        Path pastaProcessados = basePath.resolve("processados");
        Path pastaComErro = basePath.resolve("erros");
        String padraoDoArquivo = "job_" + jobId + "_*.txt";

        jobService.atualizarExecucoes(jobId, context.getFireTime(), context.getNextFireTime());

        StatusJob statusAtual = jobService.buscarStatus(jobId);

        try {
            Files.createDirectories(pastaPendente);
            Files.createDirectories(pastaProcessados);
            Files.createDirectories(pastaComErro);



            if (statusAtual == StatusJob.AGENDADO) {
                log.info("Job {} está AGENDADO. Executando ping-pong sem arquivo.", jobId);
                jobService.atualizarStatus(jobId, StatusJob.PROCESSANDO);
                jobService.atualizarExecucoes(jobId, context.getFireTime(), context.getNextFireTime());
                jobService.atualizarStatus(jobId, StatusJob.AGENDADO);
                log.info("Job {} voltou para AGENDADO.", jobId);

            } else if (statusAtual == StatusJob.PROCESSANDO) {

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(pastaPendente, padraoDoArquivo)) {
                    boolean arquivoEncontradoParaProcessar = false;
                    for (Path arquivoEncontrado : stream) {
                        arquivoEncontradoParaProcessar = true;
                        String nomeArquivo = arquivoEncontrado.getFileName().toString();
                        log.info("Arquivo {} encontrado para Job {}. PROCESSANDO...", nomeArquivo, jobId);

                        boolean sucesso;
                        try {
                            sucesso = processorService.processarArquivoDeRetorno(jobId, arquivoEncontrado.toString());
                        } catch (Exception e) {
                            log.error("Erro crítico ao processar arquivo {}. Marcando FALHA.", nomeArquivo, e);
                            jobService.atualizarStatus(jobId, StatusJob.FALHA);
                            sucesso = false;
                        }

                        Path destinoFinal;
                        if (sucesso) {
                            destinoFinal = pastaProcessados.resolve(nomeArquivo);
                        } else {
                            destinoFinal = pastaComErro.resolve(nomeArquivo);
                        }

                        try {
                            Files.move(arquivoEncontrado, destinoFinal, StandardCopyOption.REPLACE_EXISTING);
                            log.info("Arquivo {} movido de 'pendentes' para '{}'.", nomeArquivo, destinoFinal.getFileName().toString());
                        } catch (IOException e) {
                            log.error("Erro ao mover arquivo {} para destino {}.", nomeArquivo, destinoFinal, e);
                        }
                    }
                    if (!arquivoEncontradoParaProcessar) {
                        log.warn("Job {} em status PROCESSANDO, mas nenhum arquivo encontrado em 'pendentes'. Verifique o processo de upload.", jobId);
                    }
                } catch (IOException e) {
                    log.error("Erro ao ler diretório 'pendentes' para o Job {}. Marcando FALHA.", jobId, e);
                    jobService.atualizarStatus(jobId, StatusJob.FALHA);
                }
            }

        } catch (IOException e) {
            log.error("Erro no JobRecorrente para o Job ID {}. Causa: {}", jobId, e.getMessage());
            jobService.atualizarStatus(jobId, StatusJob.FALHA);
            throw new JobExecutionException(e);
        }
    }
}