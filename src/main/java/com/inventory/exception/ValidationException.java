package com.inventory.exception;

import org.springframework.http.HttpStatus;

public class ValidationException extends RuntimeException {

    private HttpStatus httpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
    public ValidationException(String message) {
        super(message);
    }


    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.NOT_FOUND;
    }

    public ValidationException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public ValidationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }


    public void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        // TODO Auto-generated method stub
        return httpStatus;
    }

} 