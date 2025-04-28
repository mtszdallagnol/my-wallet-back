package General;

import Anotations.*;
import com.mysql.cj.x.protobuf.MysqlxPrepare;

import javax.management.Query;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    public List<String> executeValidation(Map<String, Object> row, Connection conn) {
        for(Map.Entry<String, Field> entry : fields.entrySet()) {
            String columnName = entry.getKey();
            Field field = entry.getValue();
            Object value = row.get(columnName);

            if (field.isAnnotationPresent(Required.class)) {
                if (value == null || value.equals("")) {
                    validationErrors.add(columnName + " é requerido mas tem valor nulo");
                }
            }
        }

        if (!validationErrors.isEmpty()) return validationErrors;

        for(Map.Entry<String, Field> entry : fields.entrySet()) {
            String columnName = entry.getKey();
            Field field = entry.getValue();
            Object value = row.get(columnName);

            if (value == null || (field.isAnnotationPresent(Required.class) && validationErrors.stream()
                .anyMatch(error -> error.startsWith(columnName)))) continue;

            Object convertedValue = convertInstanceOfObject(value, field);
            if (convertedValue == null) continue;

            if (validateFieldValue(field, convertedValue, conn, row)) row.put(columnName, convertedValue);
        }

        return validationErrors;
    }

    public boolean hasField(String fieldName) {
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.", 2);

            if (parts.length <= 1) return false;

            String prefix = parts[0];
            String column = parts[1];

            String tableName = classInstance.getAnnotation(Table.class).value();
            if (!tableName.substring(0, tableName.length() - 1).equals(prefix)) return false;

            fieldName = column;
        }

        return fields.containsKey(fieldName);
    }

    private boolean validateFieldValue(Field field, Object value, Connection conn, Map<String, Object> context) {

        String fieldName = field.getName();

        if (field.isAnnotationPresent(MaxDigits.class)) {
            MaxDigits annotation = field.getAnnotation(MaxDigits.class);

            int totalDigits = annotation.value()[0];

            int maxDecimalPart = annotation.value()[1];
            int maxIntegerPart = totalDigits - maxDecimalPart;

            boolean shouldReturn = false;
            String[] parts = value.toString().split("\\.", 2);
            if (parts[0].length() > maxIntegerPart) {
                validationErrors.add("O número de dígitos na parte inteira excede a capacidade máxima suportada: " + maxIntegerPart);
                shouldReturn = true;
            }

            if (parts.length > 1 && parts[1].length() > maxDecimalPart) {
                validationErrors.add("O número de dígitos na parte decimal excede a capacidade máxmia suportada: " + maxDecimalPart);
                shouldReturn = true;
            }

            if (shouldReturn) return false;
        }

        if (field.isAnnotationPresent(MaxLength.class)) {
            MaxLength annotation = field.getAnnotation(MaxLength.class);

            double maxCapacity = annotation.value();
            if (value instanceof String && ((String) value).length() > maxCapacity) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + maxCapacity);
                return false;
            } else if (value instanceof Number && ((Number) value).doubleValue() > maxCapacity ) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + maxCapacity);
                return false;
            }
        }

        if (field.isAnnotationPresent(MinLength.class)) {
            MinLength annotation = field.getAnnotation(MinLength.class);

            double minCapacity = annotation.value();
            if (value instanceof String && ((String) value).length() < minCapacity) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + minCapacity);
                return false;
            } else if (value instanceof Number && ((Number) value).doubleValue() < minCapacity) {
                validationErrors.add(fieldName + ": " + annotation.message() + " " + minCapacity);
                return false;
            }
        }

        if (field.isAnnotationPresent(Email.class)) {
            String regex = "^(?=.{1,64}@)[A-Za-z0-9_!#$%&'*+/=?`{|}~^-]+(\\.[A-Za-z0-9_!#$%&'*+/=?`{|}~^-]+)*"
                    + "@"
                    + "(?=.{1,255}$)"
                    + "("
                    + "[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z][A-Za-z0-9]*)$" // Normal domain with letter in TLD
                    + "|"
                    + "[A-Za-z0-9]+(\\.[0-9]+){3}$" // IP-like domain (123.123.123.123)
                    + ")";

            if (!value.toString().matches(regex)) {
                validationErrors.add(fieldName + " : " + field.getAnnotation(Email.class).message());
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

        if (field.isAnnotationPresent(Exists.class)) {
            Exists annotation = field.getAnnotation(Exists.class);

            try {
                String contextTable = classInstance.getAnnotation(Table.class).value();
                String contextCurrentFieldName = field.getName();
                if (!annotation.withTable().isEmpty()) {
                    contextTable = annotation.withTable();
                    contextCurrentFieldName = field.getName().substring(0, field.getName().indexOf("_"));
                }

                StringBuilder query = new StringBuilder(
                        "SELECT COUNT(*) AS total FROM " + contextTable + " " +
                        "WHERE " + contextCurrentFieldName + " = ?"
                );

                List<Object> contextParamsValues = new ArrayList<>();
                for (String contextFieldNames : annotation.withFields()) {
                    Object contextValue = context.get(contextFieldNames);
                    query.append(" AND ").append(contextFieldNames).append(" = ?");
                    contextParamsValues.add(contextValue);
                }

                PreparedStatement stmt = conn.prepareStatement(query.toString());

                stmt.setString(1, value.toString());

                int enumerator = 2;
                for (Object paramValue : contextParamsValues) {
                    stmt.setString(enumerator, paramValue.toString());
                    enumerator++;
                }

                ResultSet rs = stmt.executeQuery();

                rs.next();
                int count = rs.getInt("total");

                if (count < 1) {
                    validationErrors.add(fieldName + ": " + annotation.message());
                    return false;
                }
            } catch (SQLException e) {
                errors.add("Erro ao lidar com verificação de campo obrigatoriamente existente" + e.getMessage());
                return false;
            }
        }

        if (field.isAnnotationPresent(Unique.class)) {
            Unique annotation = field.getAnnotation(Unique.class);

            try {
                StringBuilder query = new StringBuilder(
                        "SELECT COUNT(*) AS total FROM " + classInstance.getAnnotation(Table.class).value() + " " +
                        "WHERE " + field.getName() + " = ?"
                );

                List<Object> contextParamsValues = new ArrayList<>();
                for (String contextFieldName : annotation.withFields()) {
                    Object contextValue = context.get(contextFieldName);
                    query.append(" AND ").append(contextFieldName).append(" = ?");
                    contextParamsValues.add(contextValue);
                }

                PreparedStatement stmt = conn.prepareStatement(query.toString());

                stmt.setString(1, value.toString());

                int enumerator = 2;
                for (Object paramsToSet : contextParamsValues) {
                    stmt.setString(enumerator, paramsToSet.toString());
                    enumerator++;
                }

                ResultSet rs = stmt.executeQuery();
                rs.next();

                int count = rs.getInt("total");

                if (count > 0) {
                    validationErrors.add(fieldName + ": " + annotation.message());
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

            try {
                if (target == BigDecimal.class) return new BigDecimal(value.toString());
                if (target == Date.class) return Date.valueOf(LocalDate.parse(value.toString()));
            }
            catch (Exception e) { validationErrors.add(targetField.getName() + ": Tipo de dado inválido" }
            return null;

        } catch (Exception e) {
            errors.add(e.getMessage());
            return null;
        }
    }

    public List<String> getErrors() { return errors; }

}