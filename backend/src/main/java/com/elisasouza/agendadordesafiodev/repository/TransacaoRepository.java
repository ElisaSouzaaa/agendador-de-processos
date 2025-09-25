package com.elisasouza.agendadordesafiodev.repository;

import com.elisasouza.agendadordesafiodev.model.Transacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, Long> {
    List<Transacao> findByArquivoRetornoId(Long arquivoRetornoId);
}