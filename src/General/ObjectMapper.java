package General;

import Anotations.*;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectMapper<T> {
    private final Class<T> classInstance;
    private final Map<String, Field> fields = new HashMap<>();
    private final List<String> erros = new ArrayList<>();
    private final List<String> validationErrors = new ArrayList<>();

    public ObjectMapper(Class<T> classInstance) {
        this.classInstance = classInstance;

        Field[] fieldList = classInstance.getDeclaredFields();
        for (Field field : fieldList) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
    }

    public T map(Map<String, Object> row) {
        try {
            T dto = classInstance.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String columnName = entry.getKey();
                Field field = entry.getValue();
                Object value = row.get(columnName);

                if (field.isAnnotationPresent(Required.class) && (value == null || value.equals(""))) {
                    validationErrors.add("Campo: " + columnName + "é requerido mas tem valor nulo");
                    continue;
                }

                Object convertedValue = convertInstanceOfObject(value, field.getType());
                if (!validateFieldValue(field, convertedValue)) {
                    continue;
                }
            }

            return dto;
        } catch (Exception e) {
            erros.add(e.getMessage());
            return null;
        }
    }

    private boolean validateFieldValue(Field field, Object value) {
        String fieldName = field.getName();

        if (field.isAnnotationPresent(Email.class)) {
            String regex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

            if (!value.toString().matches(regex)) {
                erros.add(field.getAnnotation(Email.class).message());
                return false;
            }
        }

        if (field.isAnnotationPresent(MaxLength.class)) {
            int maxLength = field.getAnnotation(MaxLength.class).value();
            if (value.toString().length() > maxLength) {
                erros.add(field.getAnnotation(MaxLength.class).message() + " " + maxLength);
                return false;
            }
        }

        if (field.isAnnotationPresent(MinLength.class)) {
            int minLength = field.getAnnotation(MinLength.class).value();
            if (value.toString().length() < minLength) {
                erros.add(field.getAnnotation(MinLength.class).message() + " " + minLength);
                return false;
            }
        }

        if (field.isAnnotationPresent(Password.class)) {
            String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?]).*$";

            if (!value.toString().matches(regex)) {
                erros.add(field.getAnnotation(Password.class).message());
                return false;
            }
        }

        if ()
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

    public List<String> getErrors() {
        return erros;
    }
    public List<String> getValidationErrors() { return validationErrors; }
}