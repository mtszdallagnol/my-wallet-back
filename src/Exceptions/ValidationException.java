package Exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(List<String> errors) {
        super("Falha de validação");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return this.errors;
    }
}
