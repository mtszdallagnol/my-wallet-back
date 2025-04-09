package General;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JsonParsers {
    // Lightweight, thread-safe field cache
    private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    // ===== SERIALIZATION SECTION =====

    // Immutable serialization result
    public static class SerializationResult {
        private final String jsonString;
        private final Exception error;

        public SerializationResult(String jsonString, Exception error) {
            this.jsonString = jsonString;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null && jsonString != null;
        }

        public String getJsonString() {
            if (!isSuccess()) {
                throw new IllegalStateException("Serialization failed. Check error first.");
            }
            return jsonString;
        }

        public Exception getError() {
            return error;
        }
    }

    // Main serialization method - stateless and thread-safe
    public static SerializationResult serialize(Object obj) {
        try {
            String jsonString = serializeInternal(obj, new SerializationContext());
            return new SerializationResult(jsonString, null);
        } catch (Exception e) {
            return new SerializationResult(null, e);
        }
    }

    // Context to prevent circular references
    private static class SerializationContext {
        private final IdentityHashSet processedObjects = new IdentityHashSet();

        private boolean hasBeenProcessed(Object obj) {
            return processedObjects.contains(obj);
        }

        private void markProcessed(Object obj) {
            processedObjects.add(obj);
        }
    }

    // Custom lightweight IdentityHashSet (thread-compatible)
    private static class IdentityHashSet {
        private final ConcurrentHashMap<Integer, Object> map = new ConcurrentHashMap<>();

        public boolean contains(Object obj) {
            return map.containsKey(System.identityHashCode(obj));
        }

        public void add(Object obj) {
            map.put(System.identityHashCode(obj), obj);
        }
    }

    // Internal serialization method
    private static String serializeInternal(Object obj, SerializationContext context) {
        if (obj == null) {
            return "null";
        }

        // Special case for enums - don't check for circular references
        if (obj instanceof Enum) {
            return serializeEnum((Enum<?>) obj);
        }

        // Handle immutable types that cannot have circular references
        if (isPrimitiveOrWrapper(obj) || obj instanceof String || obj instanceof Temporal) {
            // These types are immutable and cannot have circular references
            if (obj instanceof String) {
                return "\"" + escapeString((String) obj) + "\"";
            } else if (obj instanceof Temporal) {
                return serializeTemporal(obj);
            } else {
                return obj.toString();
            }
        }

        // Prevent circular references for all other types
        if (context.hasBeenProcessed(obj)) {
            return "\"[Circular Reference]\"";
        }
        context.markProcessed(obj);

        // Collections
        if (obj instanceof Collection) {
            return serializeCollection((Collection<?>) obj, context);
        }

        // Maps
        if (obj instanceof Map) {
            return serializeMap((Map<?, ?>) obj, context);
        }

        // Arrays
        if (obj.getClass().isArray()) {
            return serializeArray(obj, context);
        }

        // Custom objects
        return serializeObject(obj, context);
    }

    // Temporal type serialization
    private static String serializeTemporal(Object temporalObj) {
        // Simply use toString() for all temporal classes and wrap in quotes
        return "\"" + temporalObj.toString() + "\"";
    }

    // Enum serialization
    private static String serializeEnum(Enum<?> enumObj) {
        return "\"" + enumObj.name() + "\"";
    }

    // Check if object is a primitive or wrapper type
    private static boolean isPrimitiveOrWrapper(Object obj) {
        return obj instanceof Number
                || obj instanceof Boolean
                || obj instanceof Character;
    }

    // Collection serialization
    private static String serializeCollection(Collection<?> collection, SerializationContext context) {
        List<String> serializedItems = new ArrayList<>();
        for (Object item : collection) {
            serializedItems.add(serializeInternal(item, context));
        }
        return "[" + String.join(",", serializedItems) + "]";
    }

    // Map serialization
    private static String serializeMap(Map<?, ?> map, SerializationContext context) {
        List<String> serializedEntries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = serializeInternal(entry.getKey(), context);
            String value = serializeInternal(entry.getValue(), context);
            serializedEntries.add(key + ":" + value);
        }
        return "{" + String.join(",", serializedEntries) + "}";
    }

    // Array serialization
    private static String serializeArray(Object array, SerializationContext context) {
        List<String> serializedItems = new ArrayList<>();
        int length = java.lang.reflect.Array.getLength(array);

        for (int i = 0; i < length; i++) {
            Object item = java.lang.reflect.Array.get(array, i);
            serializedItems.add(serializeInternal(item, context));
        }

        return "[" + String.join(",", serializedItems) + "]";
    }

    // Object serialization with field caching and improved security
    private static String serializeObject(Object obj, SerializationContext context) {
        List<String> serializedFields = new ArrayList<>();
        Class<?> clazz = obj.getClass();

        // Skip trying to serialize Java internal classes
        if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.")) {
            return "\"" + obj + "\"";
        }

        // Cached field retrieval
        List<Field> fields = FIELD_CACHE.computeIfAbsent(clazz, cls -> {
            List<Field> computedFields = new ArrayList<>();
            // Include fields from superclasses
            Class<?> currentClass = cls;
            while (currentClass != null && !currentClass.equals(Object.class)) {
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    computedFields.add(field);
                }
                currentClass = currentClass.getSuperclass();
            }
            return computedFields;
        });

        // Serialize fields
        for (Field field : fields) {
            try {
                String fieldName = field.getName();
                Object fieldValue = field.get(obj);
                String serializedValue = serializeInternal(fieldValue, context);
                serializedFields.add("\"" + fieldName + "\":" + serializedValue);
            } catch (IllegalAccessException | SecurityException e) {
                // Skip fields that can't be accessed
            }
        }

        return "{" + String.join(",", serializedFields) + "}";
    }

    // String escaping
    private static String escapeString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ===== DESERIALIZATION SECTION =====

    // Immutable deserialization result
    public static class DeserializationResult<T> {
        private final T value;
        private final Exception error;

        public DeserializationResult(T value, Exception error) {
            this.value = value;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }

        public T getValue() {
            if (!isSuccess()) {
                throw new IllegalStateException("Deserialization failed. Check error first.");
            }
            return value;
        }

        public Exception getError() {
            return error;
        }
    }

    // Main deserialization method for InputStream to Map
    public static DeserializationResult<Map<String, Object>> deserialize(InputStream inputStream) {
        try {
            String jsonString = streamToString(inputStream);
            JsonParser parser = new JsonParser(jsonString);
            Object result = parser.parse();

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return new DeserializationResult<>(resultMap, null);
            } else {
                throw new JsonParseException("Root element must be an object for Map deserialization");
            }
        } catch (Exception e) {
            return new DeserializationResult<>(null, e);
        }
    }

    // Overloaded method to deserialize from String directly
    public static DeserializationResult<Map<String, Object>> deserialize(String jsonString) {
        try {
            JsonParser parser = new JsonParser(jsonString);
            Object result = parser.parse();

            if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> resultMap = (Map<String, Object>) result;
                return new DeserializationResult<>(resultMap, null);
            } else {
                throw new JsonParseException("Root element must be an object for Map deserialization");
            }
        } catch (Exception e) {
            return new DeserializationResult<>(null, e);
        }
    }

    // Convert InputStream to String
    private static String streamToString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    // Custom JSON Parse Exception
    public static class JsonParseException extends Exception {
        public JsonParseException(String message) {
            super(message);
        }

        public JsonParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Custom JSON parser class
    private static class JsonParser {
        private final String json;
        private int position = 0;

        public JsonParser(String json) {
            this.json = json;
        }

        public Object parse() throws JsonParseException {
            skipWhitespace();
            Object result = parseValue();
            skipWhitespace();

            // Check that we've consumed all input
            if (position < json.length()) {
                throw new JsonParseException("Unexpected character after JSON end: " + json.charAt(position));
            }

            return result;
        }

        private Object parseValue() throws JsonParseException {
            char current = peek();

            if (current == '{') {
                return parseObject();
            } else if (current == '[') {
                return parseArray();
            } else if (current == '"') {
                return parseString();
            } else if (current == 't' || current == 'f') {
                return parseBoolean();
            } else if (current == 'n') {
                return parseNull();
            } else if (Character.isDigit(current) || current == '-') {
                return parseNumber();
            } else {
                throw new JsonParseException("Unexpected character: " + current);
            }
        }

        private Map<String, Object> parseObject() throws JsonParseException {
            Map<String, Object> map = new LinkedHashMap<>();

            // Consume opening brace
            expect('{');
            skipWhitespace();

            // Check for empty object
            if (peek() == '}') {
                next();
                return map;
            }

            while (true) {
                skipWhitespace();

                // Parse key (must be a string)
                if (peek() != '"') {
                    throw new JsonParseException("Expected a string key, got: " + peek());
                }

                String key = parseString();
                skipWhitespace();

                // Expect colon
                expect(':');
                skipWhitespace();

                // Parse value
                Object value = parseValue();
                map.put(key, value);

                skipWhitespace();

                // Check for end of object or next pair
                char c = next();
                if (c == '}') {
                    break;
                } else if (c != ',') {
                    throw new JsonParseException("Expected ',' or '}', got: " + c);
                }
            }

            return map;
        }

        private List<Object> parseArray() throws JsonParseException {
            List<Object> list = new ArrayList<>();

            // Consume opening bracket
            expect('[');
            skipWhitespace();

            // Check for empty array
            if (peek() == ']') {
                next();
                return list;
            }

            while (true) {
                skipWhitespace();

                // Parse value
                Object value = parseValue();
                list.add(value);

                skipWhitespace();

                // Check for end of array or next value
                char c = next();
                if (c == ']') {
                    break;
                } else if (c != ',') {
                    throw new JsonParseException("Expected ',' or ']', got: " + c);
                }
            }

            return list;
        }

        private String parseString() throws JsonParseException {
            // Consume opening quote
            expect('"');

            StringBuilder sb = new StringBuilder();
            boolean escape = false;

            while (position < json.length()) {
                char c = next();

                if (escape) {
                    switch (c) {
                        case '"':
                        case '\\':
                        case '/':
                            sb.append(c);
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u':
                            // Parse 4 hex digits
                            if (position + 4 > json.length()) {
                                throw new JsonParseException("Incomplete Unicode escape sequence");
                            }
                            String hex = json.substring(position, position + 4);
                            position += 4;
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                            } catch (NumberFormatException e) {
                                throw new JsonParseException("Invalid Unicode escape sequence: \\u" + hex);
                            }
                            break;
                        default:
                            throw new JsonParseException("Invalid escape sequence: \\" + c);
                    }
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    // End of string
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }

            throw new JsonParseException("Unterminated string");
        }

        private Object parseNumber() throws JsonParseException {
            int start = position;
            boolean isFloat = false;

            // Allow leading minus
            if (peek() == '-') {
                next();
            }

            // Read integer part
            if (peek() == '0') {
                next();
            } else if (Character.isDigit(peek())) {
                while (position < json.length() && Character.isDigit(peek())) {
                    next();
                }
            } else {
                throw new JsonParseException("Invalid number format");
            }

            // Read decimal part
            if (peek() == '.') {
                isFloat = true;
                next();

                if (!Character.isDigit(peek())) {
                    throw new JsonParseException("Expected digit after decimal point");
                }

                while (position < json.length() && Character.isDigit(peek())) {
                    next();
                }
            }

            // Read exponent part
            if (peek() == 'e' || peek() == 'E') {
                isFloat = true;
                next();

                if (peek() == '+' || peek() == '-') {
                    next();
                }

                if (!Character.isDigit(peek())) {
                    throw new JsonParseException("Expected digit in exponent");
                }

                while (position < json.length() && Character.isDigit(peek())) {
                    next();
                }
            }

            String number = json.substring(start, position);
            try {
                if (isFloat) {
                    // Return BigDecimal for better precision
                    return new BigDecimal(number);
                } else {
                    long longValue = Long.parseLong(number);
                    // Use Integer if it fits, otherwise Long
                    if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                        return (int) longValue;
                    } else {
                        return longValue;
                    }
                }
            } catch (NumberFormatException e) {
                throw new JsonParseException("Invalid number format: " + number);
            }
        }

        private Boolean parseBoolean() throws JsonParseException {
            if (matches("true")) {
                position += 4;
                return Boolean.TRUE;
            } else if (matches("false")) {
                position += 5;
                return Boolean.FALSE;
            } else {
                throw new JsonParseException("Expected 'true' or 'false'");
            }
        }

        private Object parseNull() throws JsonParseException {
            if (matches("null")) {
                position += 4;
                return null;
            } else {
                throw new JsonParseException("Expected 'null'");
            }
        }

        // Attempts to parse string values as Temporal objects
        private Object attemptTemporalParse(String value) {
            // Try to parse as LocalDateTime
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e1) {
                // Try to parse as LocalDate
                try {
                    return LocalDate.parse(value);
                } catch (DateTimeParseException e2) {
                    // Try to parse as LocalTime
                    try {
                        return LocalTime.parse(value);
                    } catch (DateTimeParseException e3) {
                        // Try to parse as ZonedDateTime
                        try {
                            return ZonedDateTime.parse(value);
                        } catch (DateTimeParseException e4) {
                            // If all attempts fail, return original string
                            return value;
                        }
                    }
                }
            }
        }

        private void skipWhitespace() {
            while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
                position++;
            }
        }

        private char peek() throws JsonParseException {
            if (position >= json.length()) {
                throw new JsonParseException("Unexpected end of JSON");
            }
            return json.charAt(position);
        }

        private char next() throws JsonParseException {
            if (position >= json.length()) {
                throw new JsonParseException("Unexpected end of JSON");
            }
            return json.charAt(position++);
        }

        private void expect(char expected) throws JsonParseException {
            char actual = next();
            if (actual != expected) {
                throw new JsonParseException("Expected '" + expected + "', got '" + actual + "'");
            }
        }

        private boolean matches(String prefix) {
            if (position + prefix.length() > json.length()) {
                return false;
            }
            return json.startsWith(prefix, position);
        }
    }
}