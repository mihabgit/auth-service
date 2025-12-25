package com.imran.authservice.exception;

public class ResourceNotFoundException extends RuntimeException {

    ResourceNotFoundException(String message) {
        super(message);
    }

}
