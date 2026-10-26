package com.stackcraft.retroflow.exception;

/** Thrown when a request violates a business rule. */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

}
