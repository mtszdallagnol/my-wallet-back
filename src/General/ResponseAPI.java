package General;

public class ResponseAPI<T> {
    public int error = 1;
    public int httpStatus = 500;
    public String msg = "Falha na operação";
    public T data = null;
}
