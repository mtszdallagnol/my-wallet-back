package Exceptions;

import java.util.List;

public class MappingException extends RuntimeException {
    private final List<String> errors;

    public MappingException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
