package com.elisasouza.agendadordesafiodev.exception;



public class JobNotFoundException extends RuntimeException{
    public JobNotFoundException(String message) {
        super(message);
    }
}
