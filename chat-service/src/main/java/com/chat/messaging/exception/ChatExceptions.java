package com.chat.messaging.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public final class ChatExceptions {

    private ChatExceptions() {
    }

    public static class NotMemberException extends RuntimeException {
        public NotMemberException(String message) {
            super(message);
        }
    }

    public static class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(String message) {
            super(message);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }
    }

    @RestControllerAdvice
    public static class ApiExceptionHandler {

        public record ErrorResponse(String error, String message) {
        }

        @ExceptionHandler(NotMemberException.class)
        public ResponseEntity<ErrorResponse> handleNotMember(NotMemberException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("NOT_MEMBER", e.getMessage()));
        }

        @ExceptionHandler(ConversationNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(ConversationNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("CONVERSATION_NOT_FOUND", e.getMessage()));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
            String message = e.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .orElse("Dữ liệu không hợp lệ");
            return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
        }
    }
}
