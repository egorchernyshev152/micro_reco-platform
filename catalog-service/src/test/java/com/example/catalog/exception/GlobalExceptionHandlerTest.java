package com.example.catalog.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConstraintShouldReturnBadRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/movies/1");

        var response = handler.handleConstraint(new ConstraintViolationException("bad request", Collections.emptySet()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("bad request");
        assertThat(response.getBody().getPath()).isEqualTo("/movies/1");
    }

    @Test
    void handleValidationShouldAggregateMessages() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "movieDto");
        bindingResult.addError(new FieldError("movieDto", "title", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        var response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("title: must not be blank");
    }
}
