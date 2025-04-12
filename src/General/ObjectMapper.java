package General;

import Anotations.*;

import java.lang.reflect.Field;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectMapper<T> {
    private final Class<T> classInstance;
    private final Map<String, Field> fields = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
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

                Object convertedValue = convertInstanceOfObject(value, field);
                field.set(dto, convertedValue);
            }

            return dto;
        } catch (Exception e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    public T map(Map<String, Object> row, Connection conn) {
        try {
            T dto = classInstance.getDeclaredConstructor().newInstance();

            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String columnName = entry.getKey();
                Field field = entry.getValue();
                Object value = row.get(columnName);

                if (field.isAnnotationPresent(Required.class) && (value == null || value.equals(""))) {
                    validationErrors.add("Campo: " + columnName + " é requerido mas tem valor nulo");
                    continue;
                }

                Object convertedValue = convertInstanceOfObject(value, field);
                if (convertedValue == null) {
                    field.set(dto, null);
                    continue;
                }

                if (!validateFieldValue(field, convertedValue, conn)) {
                    continue;
                }

                field.set(dto, convertedValue);
            }

            return dto;
        } catch (Exception e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    private boolean validateFieldValue(Field field, Object value, Connection conn) {

        String fieldName = field.getName();

        if (field.isAnnotationPresent(MaxLength.class)) {
            MaxLength annotation = field.getAnnotation(MaxLength.class);
            int maxLength = annotation.value();
            if (value.toString().length() > maxLength) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + maxLength);
                return false;
            }
        }

        if (field.isAnnotationPresent(MinLength.class)) {
            MinLength annotation = field.getAnnotation(MinLength.class);
            int minLength = annotation.value();
            if (value.toString().length() < minLength) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + minLength);
                return false;
            }
        }

        if (field.isAnnotationPresent(Email.class)) {
            String regex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

            if (!value.toString().matches(regex)) {
                validationErrors.add(fieldName + ": " + field.getAnnotation(Email.class).message());
                return false;
            }
        }

        if (field.isAnnotationPresent(Password.class)) {
            String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).*$";
            if (!value.toString().matches(regex)) {
                validationErrors.add(fieldName + ": " + field.getAnnotation(Password.class).message());
                return false;
            }
        }

        if (field.isAnnotationPresent(Unique.class)) {
            try {
                PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + classInstance.getAnnotation(Table.class).TableName() +
                        " WHERE " + field.getName() + " = ?");
                stmt.setObject(1, value);

                ResultSet rs = stmt.executeQuery();
                rs.next();

                int count = rs.getInt("COUNT(*)");
                if (count > 1) {
                    validationErrors.add(fieldName + ": " + field.getAnnotation(Unique.class).message());
                    return false;
                }
            } catch (SQLException e) {
                errors.add("Erro ao lidar com verificação de campo único: " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    private Object convertInstanceOfObject(Object value, Field targetField) {
        Class<?> target = targetField.getType();

        if (value == null) {
            return null;
        }

        if (target.isInstance(value) ) {
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
                Object[] enumConstants = target.getEnumConstants();

                for (Object constant : enumConstants) {
                    if (((Enum<?>) constant).name().equalsIgnoreCase(value.toString())) {
                        return Enum.valueOf((Class<Enum>) target, (String) value);
                    }
                }

                validationErrors.add(targetField.getName() + ": Valor de enum inválido");
                return null;
            } else if (target.isEnum()) {
                validationErrors.add(targetField.getName() + ": Tipo de enum inválido");
                return null;
            }

            if (!target.isInstance(value)) {
                validationErrors.add(targetField.getName() + ": Tipo de dado inválido");
                return null;
            }

            return target.cast(value);
        } catch (Exception e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    public List<String> getErrors() { return errors; }

    public List<String> getValidationErrors() { return validationErrors; }
}