package com.elisasouza.agendadordesafiodev.service;

import com.elisasouza.agendadordesafiodev.dto.ArquivoRetornoDTO;
import com.elisasouza.agendadordesafiodev.dto.TransacaoDTO;
import com.elisasouza.agendadordesafiodev.event.ArquivoRetornoDeletadoEvent;
import com.elisasouza.agendadordesafiodev.exception.ArquivoRetornoNotFoundException;
import com.elisasouza.agendadordesafiodev.model.ArquivoRetorno;
import com.elisasouza.agendadordesafiodev.model.Transacao;
import com.elisasouza.agendadordesafiodev.repository.ArquivoRetornoRepository;
import com.elisasouza.agendadordesafiodev.repository.TransacaoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArquivoRetornoService {

    private final ArquivoRetornoRepository arquivoRetornoRepository;
    private final TransacaoRepository transacaoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ArquivoRetornoService(ArquivoRetornoRepository arquivoRetornoRepository,
                                 TransacaoRepository transacaoRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.arquivoRetornoRepository = arquivoRetornoRepository;
        this.transacaoRepository = transacaoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void deleteById(Long id) {
        ArquivoRetorno arquivo = arquivoRetornoRepository.findById(id)
                .orElseThrow(() -> new ArquivoRetornoNotFoundException("Arquivo não encontrado com o id: " + id));

        try {
            Path filePath = Paths.get(arquivo.getNomeArquivo());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Aviso: Erro ao deletar arquivo físico, mas o registro do banco será removido: " + e.getMessage());
        }

        eventPublisher.publishEvent(new ArquivoRetornoDeletadoEvent(this, id));
        arquivoRetornoRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ArquivoRetornoDTO> findArquivosByJobId(Long jobId) {
        return arquivoRetornoRepository.findByJobId(jobId).stream()
                .map(this::convertToArquivoRetornoDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransacaoDTO> findTransacoesByArquivoId(Long arquivoId) {
        return transacaoRepository.findByArquivoRetornoId(arquivoId).stream()
                .map(this::convertToTransacaoDTO)
                .collect(Collectors.toList());
    }

    private ArquivoRetornoDTO convertToArquivoRetornoDTO(ArquivoRetorno entity) {
        ArquivoRetornoDTO dto = new ArquivoRetornoDTO();
        dto.setId(entity.getId());
        dto.setJobId(entity.getJob().getId());
        dto.setNomeArquivo(entity.getNomeArquivo());
        dto.setDataProcessamento(entity.getDataProcessamento());
        dto.setCabecalhoNumerico(entity.getCabecalhoNumerico());
        dto.setCabecalhoTexto(entity.getCabecalhoTexto());
        dto.setCabecalhoCodigo(entity.getCabecalhoCodigo());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    private TransacaoDTO convertToTransacaoDTO(Transacao entity) {
        TransacaoDTO dto = new TransacaoDTO();
        dto.setId(entity.getId());
        dto.setTipo(entity.getTipo());
        dto.setValor(entity.getValor());
        dto.setData(entity.getData());
        dto.setDescricao(entity.getDescricao());
        dto.setCodigoSaida(entity.getCodigoSaida());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}