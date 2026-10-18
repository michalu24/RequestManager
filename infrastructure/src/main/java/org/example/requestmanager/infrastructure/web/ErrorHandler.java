package org.example.requestmanager.infrastructure.web;

import org.example.requestmanager.application.exception.RequestNotFoundException;
import org.example.requestmanager.domain.exception.ContentNotEditableException;
import org.example.requestmanager.domain.exception.InvalidTransitionException;
import org.example.requestmanager.domain.exception.ReasonRequiredException;
import org.example.requestmanager.domain.exception.RequestDomainException;
import org.example.requestmanager.domain.exception.RequiredFieldMissingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps domain and application exceptions to RFC 9457 problem details.
 * 400 - the client sent incomplete data,
 * 404 - the request does not exist,
 * 409 - the data is valid but conflicts with the current state of the request,
 * 422 - any other business rule violation.
 * Malformed HTTP input is handled by {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
class ErrorHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    ProblemDetail handle(RequiredFieldMissingException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, e);
        problem.setProperty("field", e.getField());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail handle(ReasonRequiredException e) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, e);
        problem.setProperty("action", e.getAction());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail handle(InvalidTransitionException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, e);
        problem.setProperty("state", e.getFrom());
        problem.setProperty("action", e.getAction());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail handle(ContentNotEditableException e) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, e);
        problem.setProperty("state", e.getState());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail handle(RequestNotFoundException e) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, e);
        problem.setProperty("id", e.getId().id());
        return problem;
    }

    @ExceptionHandler
    ProblemDetail handle(RequestDomainException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, e);
    }

    private static ProblemDetail problem(HttpStatus status, Exception e) {
        return ProblemDetail.forStatusAndDetail(status, e.getMessage());
    }
}
