package Responses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerResponse {
    public boolean error = true;
    public int httpStatus = 500;
    public String msg = "Falha na operação";
    public Map<String, Object> data = new HashMap<>();
    public List<String> errors = null;
}
