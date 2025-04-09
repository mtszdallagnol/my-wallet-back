package General;

import Responses.ControllerResponse;

import java.util.List;
import java.util.Map;

public interface ServiceInterface<T> {
    List<T> get(Map<String, Object> params) throws Exception;

    void post(Map<String, Object> userToPost) throws Exception;

    void update(Map<String, Object> userToUpdate) throws Exception;

    void delete(Map<String, Object> params) throws Exception;
}
