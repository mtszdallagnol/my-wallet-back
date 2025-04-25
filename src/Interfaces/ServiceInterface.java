package Interfaces;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServiceInterface<T> {
    List<T> get(Map<String, Object> params) throws Exception;

    T post(Map<String, Object> userToPost) throws Exception;

    T update(Map<String, Object> params) throws Exception;

    void delete(Map<String, Object> params) throws Exception;
}
