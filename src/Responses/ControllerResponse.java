package Responses;

import java.util.List;

public class ControllerResponse {
    public boolean error = true;
    public int httpStatus = 500;
    public String msg = "Falha na operação";
    public Object data = null;
    public List<String> errors = null;
}
