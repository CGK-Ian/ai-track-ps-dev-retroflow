package com.stackcraft.retroflow.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Makes enum request parameters case-insensitive (e.g. status=open). */
@Configuration
public class CaseInsensitiveEnumConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new ConverterFactory<String, Enum>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
                return source -> {
                    String value = source.trim();
                    if (value.isEmpty()) {
                        return null;
                    }
                    return (T) Enum.valueOf(targetType, value.toUpperCase());
                };
            }
        });
    }
}