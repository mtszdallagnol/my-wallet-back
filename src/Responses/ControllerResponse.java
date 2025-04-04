package Responses;

public class ControllerResponse<T> {
    public boolean error = true;
    public int httpStatus = 500;
    public String msg = "Falha na operação";
    public T data = null;
}
