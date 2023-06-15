package org.selernaciowy;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YamlPropertiesProvider implements EnvironmentAware {
    private static final String PROPERTIES_FILE_NAME_PREFIX = "application";
    private static final String EXTENSION = ".yaml";
    private final Yaml yaml = new Yaml();

    private InputStream getResource(String name) {
        return this.getClass().getClassLoader().getResourceAsStream(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Map<String, Object> source) {
        Map<String, Object> result = new HashMap<>();
        source.forEach((key, value) -> {
            if (value instanceof Map<?, ?> properties) {
                Map<String, Object> subResult = flatten((Map<String, Object>) properties);
                subResult.forEach((subKey, subValue) -> result.put(key + "." + subKey, subValue));
                return;
            }
            if (value instanceof List<?> list) {
                Map<String, Object> subResult = flatten((List<Object>) list);
                subResult.forEach((subKey, subValue) -> result.put(key + subKey, subValue));
                return;
            }
            result.put(key, value);
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(List<Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (int idx = 0; idx < source.size(); idx++) {
            Object value = source.get(idx);
            Map<String, Object> subResult = null;
            String separator;
            if (value instanceof List<?> list) {
                subResult = flatten((List<Object>) list);
            }
            if (value instanceof Map<?,?> map) {
                subResult = flatten((Map<String, Object>) map);
                separator = ".";
            } else {
                separator = "";
            }
            if (subResult != null) {
                int finalIdx = idx;
                subResult.forEach((subKey, subValue) -> {
                    String newKey = "[" + finalIdx + "]" + separator + subKey;
                    result.put(newKey, subValue);
                });
            } else {
                result.put("[" + idx + "]", value);
            }
        }
        return result;
    }

    @Override
    public void setEnvironment(Environment environment) {
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            Map<String, Object> commonConfig = yaml.load(getResource(PROPERTIES_FILE_NAME_PREFIX + EXTENSION));
            Map<String, Object> flatConfig = flatten(commonConfig);
            Arrays.stream(environment.getActiveProfiles())
                    .map(profile -> PROPERTIES_FILE_NAME_PREFIX + "-" + profile + EXTENSION)
                    .forEach(configFile -> {
                        Map<String, Object> profileConfig = yaml.load(getResource(configFile));
                        Map<String, Object> flatProfileConfig = flatten(profileConfig);
                        flatConfig.putAll(flatProfileConfig);
                    });
            configurableEnvironment.getPropertySources().addFirst(new MapPropertySource("YAML", flatConfig));
        }
    }
}
