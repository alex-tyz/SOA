package ru.itmo.soa.starship;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class StarshipHttpErrors {
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    ResponseEntity<String> notAcceptable() {
        return ResponseEntity.status(406).contentType(MediaType.APPLICATION_XML)
                .body(StarshipController.errorXml(406, "Поддерживается только application/xml"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowed() {
        return ResponseEntity.status(405).contentType(MediaType.APPLICATION_XML)
                .body(StarshipController.errorXml(405, "Метод не поддерживается"));
    }
}
