package Responses;

public class ServiceResponse<T> {
    public boolean isSuccessful;
    public int rowChanges;
    public String msg;
    public T data;
}
