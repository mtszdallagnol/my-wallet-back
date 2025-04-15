package Interfaces;

import java.util.Map;
import java.util.Optional;

public interface ServiceInterface {
    Object get(Map<String, Object> params) throws Exception;

    Optional<Object> post(Map<String, Object> userToPost) throws Exception;

    Optional<Object> update(Map<String, Object> params) throws Exception;

    void delete(Map<String, Object> params) throws Exception;
}
