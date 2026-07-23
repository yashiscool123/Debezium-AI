package io.debezium.v4.plugins.registry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class PluginRegistry {

    private final Map<String, Object> plugins = new ConcurrentHashMap<>();

    @Inject
    Instance<Object> allBeans;

    @SuppressWarnings("unchecked")
    public <T> void register(String name, T plugin) {
        plugins.put(name, plugin);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getPlugin(String name, Class<T> type) {
        Object plugin = plugins.get(name);
        if (type.isInstance(plugin)) return Optional.of((T) plugin);
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getPlugins(Class<T> type) {
        List<T> result = plugins.values().stream()
            .filter(type::isInstance)
            .map(p -> (T) p)
            .collect(Collectors.toList());

        // Also scan CDI beans
        if (allBeans != null) {
            for (Object bean : allBeans) {
                if (type.isInstance(bean) && !result.contains(bean)) {
                    result.add((T) bean);
                }
            }
        }
        return result;
    }

    public void unregister(String name) {
        plugins.remove(name);
    }

    public Set<String> getPluginNames() {
        return plugins.keySet();
    }

    public int count() {
        return plugins.size();
    }

    public void clear() {
        plugins.clear();
    }
}
