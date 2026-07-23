package io.debezium.v4.monitoring;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@ApplicationScoped
public class EventBus {

    private final Map<String, List<Consumer<Event>>> subscribers = new ConcurrentHashMap<>();
    private final List<Event> eventLog = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_EVENTS = 5000;

    public void publish(String topic, Object data) {
        Event event = new Event(UUID.randomUUID().toString(), topic, data, Instant.now());
        eventLog.add(event);
        if (eventLog.size() > MAX_EVENTS) eventLog.removeFirst();

        List<Consumer<Event>> subs = subscribers.get(topic);
        if (subs != null) {
            for (Consumer<Event> sub : subs) {
                try { sub.accept(event); } catch (Exception ignored) {}
            }
        }
    }

    public void publish(Event event) {
        eventLog.add(event);
        if (eventLog.size() > MAX_EVENTS) eventLog.removeFirst();
        List<Consumer<Event>> subs = subscribers.get(event.topic());
        if (subs != null) {
            for (Consumer<Event> sub : subs) {
                try { sub.accept(event); } catch (Exception ignored) {}
            }
        }
    }

    public void subscribe(String topic, Consumer<Event> handler) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public void unsubscribe(String topic, Consumer<Event> handler) {
        List<Consumer<Event>> subs = subscribers.get(topic);
        if (subs != null) subs.remove(handler);
    }

    public List<Event> getEventLog() { return List.copyOf(eventLog); }
    public List<Event> getEventsSince(Instant since) {
        return eventLog.stream().filter(e -> e.timestamp().isAfter(since)).toList();
    }

    public record Event(String id, String topic, Object data, Instant timestamp) {}
}
