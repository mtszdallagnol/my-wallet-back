package Exceptions;

import java.util.List;

public class MappingException extends RuntimeException {
    private final List<String> errors;

    public MappingException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public MappingException(List<String> errors) {
        super("Falha ao mapear objeto");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
