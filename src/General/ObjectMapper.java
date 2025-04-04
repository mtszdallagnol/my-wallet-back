package General;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectMapper<T> {
    private final Class<T> classInstance;
    private final Map<String, Field> fields = new HashMap<>();
    private List<String> erros = new ArrayList<>();

    public ObjectMapper(Class<T> classInstance) {
        this.classInstance = classInstance;

        Field[] fieldList = classInstance.getDeclaredFields();
        for (Field field : fieldList) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
    }

    public T map(Map<String, Object> row) throws Exception {
        try {
            T dto = (T) classInstance.getConstructor().newInstance();
            for (Map.Entry<String, Object> entity : row.entrySet()) {
                if (entity.getValue() == null) {
                    continue;
                }
                String column = entity.getKey();
                Field field = fields.get(column);
                if (field != null) {
                    field.set(dto, convertInstanceOfObject(entity.getValue(), field.getType()));
                }
            }

            return dto;
        } catch (Exception e) {
            erros.add(e.getMessage());
            return null;
        }
    }
    public List<String> getErros() {
        return erros;
    }

    private Object convertInstanceOfObject(Object value, Class<?> target) {
        if (value == null) {
            return null;
        }

        if (target.isInstance(value)) {
            return value;
        }

        try {
            if (value instanceof Timestamp && target == Instant.class) {
                return ((Timestamp) value).toInstant();
            }

            if (value instanceof Timestamp && target == LocalDateTime.class) {
                return ((Timestamp) value).toLocalDateTime();
            }

            if (target.isEnum() && value instanceof String) {
                return Enum.valueOf((Class<Enum>) target, (String) value);
            }

            return target.cast(value);
        } catch (Exception e) {
            erros.add(e.getMessage());
            return null;
        }
    }
}