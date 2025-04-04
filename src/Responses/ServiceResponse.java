package Responses;

import java.util.ArrayList;

public class ServiceResponse<T> {
    public boolean isSuccessful = true;
    public int rowChanges = 0;
    public String msg = "";
    public T data = null;
}
