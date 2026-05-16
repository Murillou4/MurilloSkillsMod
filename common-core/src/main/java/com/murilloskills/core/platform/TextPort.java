package com.murilloskills.core.platform;

public interface TextPort<T> {
    T literal(String value);

    T translatable(String key, Object... args);

    T colored(T text, String colorName);
}
