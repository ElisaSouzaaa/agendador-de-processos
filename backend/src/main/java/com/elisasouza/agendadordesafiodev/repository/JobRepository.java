package com.elisasouza.agendadordesafiodev.repository;

import com.elisasouza.agendadordesafiodev.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

}
