package Responses;

import java.util.List;

public class ControllerResponse<T> {
    public boolean error = true;
    public int rowChanges = 0;
    public int httpStatus = 500;
    public String msg = "Falha na operação";
    public T data = null;
    public List<String> errors = null;
}
