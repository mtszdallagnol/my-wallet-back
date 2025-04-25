package Exceptions;

import java.util.List;

public class InvalidParamsException extends RuntimeException{
    private final List<String> invalidFields;

    public InvalidParamsException(String message, List<String> invalidFields) {
        super(message);
        this.invalidFields = invalidFields;
    }

    public InvalidParamsException(List<String> invalidFields) {
        super("Parâmetro(s) inválido(s)");
        this.invalidFields = invalidFields;
    }

    public List<String> getErrors() {
        return invalidFields;
    }
}
