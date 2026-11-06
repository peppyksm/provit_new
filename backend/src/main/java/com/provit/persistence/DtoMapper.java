package com.provit.persistence;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Converts persistence values at the DTO boundary without serializing entities. */
public final class DtoMapper {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private DtoMapper() {}

    public static <T> T create(Class<T> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Cannot create " + type, e);
        }
    }

    public static Map<String, Object> values(Object bean) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (bean instanceof Map<?, ?> map) {
            map.forEach((key, value) -> values.put(key.toString(), value));
            return values;
        }
        BeanWrapper source = PropertyAccessorFactory.forBeanPropertyAccess(bean);
        for (var property : source.getPropertyDescriptors()) {
            if (!property.getName().equals("class") && property.getReadMethod() != null)
                values.put(property.getName(), source.getPropertyValue(property.getName()));
        }
        return values;
    }

    public static <T> T copy(Object source, T target, Map<String, String> aliases) {
        values(source)
                .forEach((name, value) -> set(target, aliases.getOrDefault(name, name), value));
        return target;
    }

    public static void set(Object target, String name, Object value) {
        BeanWrapper bean = PropertyAccessorFactory.forBeanPropertyAccess(target);
        if (!bean.isWritableProperty(name)) return;
        Class<?> type = bean.getPropertyType(name);
        if (value == null && type.isPrimitive()) return;
        if (value instanceof java.util.Date date) {
            if (type == LocalDateTime.class)
                value = LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZONE);
            if (type == LocalDate.class)
                value = Instant.ofEpochMilli(date.getTime()).atZone(ZONE).toLocalDate();
        } else if (value instanceof LocalDate date && java.util.Date.class.isAssignableFrom(type)) {
            value = java.util.Date.from(date.atStartOfDay(ZONE).toInstant());
        } else if (value instanceof LocalDateTime time) {
            if (java.util.Date.class.isAssignableFrom(type))
                value = java.util.Date.from(time.atZone(ZONE).toInstant());
            if (type == String.class)
                value = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        if (value instanceof Number number && (type == boolean.class || type == Boolean.class))
            value = number.intValue() != 0;
        bean.setPropertyValue(name, value);
    }

    public static String camel(String column) {
        if (!column.contains("_")) return column;
        StringBuilder value = new StringBuilder();
        boolean upper = false;
        for (char c : column.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') upper = true;
            else {
                value.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return value.toString();
    }
}
