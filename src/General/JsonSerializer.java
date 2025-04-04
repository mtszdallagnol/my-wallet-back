package General;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSerializer {
    // Lightweight, thread-safe field cache
    private static final ConcurrentHashMap<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

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
            return "\"" + obj.toString() + "\"";
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
                continue;
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
}