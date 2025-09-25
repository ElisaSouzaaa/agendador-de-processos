package com.elisasouza.agendadordesafiodev.repository;

import com.elisasouza.agendadordesafiodev.model.ArquivoRetorno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArquivoRetornoRepository extends JpaRepository<ArquivoRetorno, Long> {
    List<ArquivoRetorno> findByJobId(Long jobId);
    Optional<ArquivoRetorno> findByNomeArquivo(String nomeArquivo);
}
