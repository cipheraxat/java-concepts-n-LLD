package lessons.design_patterns;

import java.util.*;
import java.util.function.*;

/**
 * LESSON 07B — Observer + Strategy + Template Method + Decorator + Proxy
 *
 * SDE2 Interview Questions:
 *  - "Observer vs Event Bus?" => Observer is direct reference; Event Bus decouples via message broker
 *  - "Strategy vs State?" => Strategy changes behavior; State changes based on internal state transitions
 *  - "Decorator vs Inheritance?" => Decorator is composable at runtime; inheritance is static at compile-time
 *  - "Proxy uses?" => Lazy initialization, caching, access control, logging, remote
 */
public class BehavioralPatternDemo {

    // ═══════════════════════════════════════════════════════════════════════════
    // OBSERVER PATTERN — pub/sub, event-driven decoupling
    // Real Java examples: java.util.Observer (deprecated), Spring ApplicationEvent, Swing listeners
    // ═══════════════════════════════════════════════════════════════════════════

    interface EventListener<T> {
        void onEvent(T event);
    }

    static class EventBus<T> {
        private final Map<String, List<EventListener<T>>> listeners = new HashMap<>();

        void subscribe(String eventType, EventListener<T> listener) {
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        }

        void publish(String eventType, T event) {
            listeners.getOrDefault(eventType, Collections.emptyList())
                     .forEach(l -> l.onEvent(event));
        }
    }

    record OrderEvent(String orderId, String status) {}

    // ═══════════════════════════════════════════════════════════════════════════
    // STRATEGY PATTERN — interchangeable algorithms
    // ═══════════════════════════════════════════════════════════════════════════

    @FunctionalInterface
    interface SortStrategy<T> {
        void sort(List<T> list);
    }

    static class Sorter<T> {
        private SortStrategy<T> strategy;

        Sorter(SortStrategy<T> strategy) { this.strategy = strategy; }

        // Swap strategy at runtime
        void setStrategy(SortStrategy<T> strategy) { this.strategy = strategy; }

        void sort(List<T> list) { strategy.sort(list); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TEMPLATE METHOD PATTERN — skeleton algorithm, subclass fills steps
    // ═══════════════════════════════════════════════════════════════════════════

    static abstract class DataProcessor {
        // Template method — defines the algorithm skeleton
        final void process(String data) {   // final prevents overriding the template
            String validated = validate(data);
            String transformed = transform(validated);
            save(transformed);
        }

        // Hook: optionally override
        String validate(String data) {
            System.out.println("Default validation: " + data);
            return data.trim();
        }

        // Abstract steps: MUST be implemented
        abstract String transform(String data);
        abstract void save(String data);
    }

    static class CSVProcessor extends DataProcessor {
        @Override
        public String transform(String data) {
            System.out.println("Transforming CSV: " + data);
            return data.replace(",", "|");
        }
        @Override
        public void save(String data) {
            System.out.println("Saving to CSV store: " + data);
        }
    }

    static class JSONProcessor extends DataProcessor {
        @Override
        public String transform(String data) {
            System.out.println("Transforming to JSON: " + data);
            return "{\"data\":\"" + data + "\"}";
        }
        @Override
        public void save(String data) {
            System.out.println("Saving to JSON store: " + data);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DECORATOR PATTERN — add behavior dynamically without subclassing
    // ═══════════════════════════════════════════════════════════════════════════

    interface TextProcessor {
        String process(String text);
    }

    // Base implementation
    static class PlainTextProcessor implements TextProcessor {
        @Override public String process(String text) { return text; }
    }

    // Decorators — each wraps a TextProcessor and adds behavior
    static class TrimDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        TrimDecorator(TextProcessor tp) { this.wrapped = tp; }
        @Override public String process(String text) { return wrapped.process(text).trim(); }
    }

    static class UpperCaseDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        UpperCaseDecorator(TextProcessor tp) { this.wrapped = tp; }
        @Override public String process(String text) { return wrapped.process(text).toUpperCase(); }
    }

    static class LoggingDecorator implements TextProcessor {
        private final TextProcessor wrapped;
        LoggingDecorator(TextProcessor tp) { this.wrapped = tp; }
        @Override public String process(String text) {
            System.out.println("Processing: '" + text + "'");
            String result = wrapped.process(text);
            System.out.println("Result: '" + result + "'");
            return result;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PROXY PATTERN — control access to subject
    // ═══════════════════════════════════════════════════════════════════════════

    interface UserRepository {
        Optional<String> findById(long id);
    }

    // Real implementation (expensive)
    static class RealUserRepository implements UserRepository {
        @Override public Optional<String> findById(long id) {
            System.out.println("[DB] Querying user " + id);
            return id > 0 ? Optional.of("User#" + id) : Optional.empty();
        }
    }

    // Caching Proxy — transparent to client
    static class CachingUserRepository implements UserRepository {
        private final UserRepository delegate;
        private final Map<Long, String> cache = new HashMap<>();

        CachingUserRepository(UserRepository delegate) { this.delegate = delegate; }

        @Override public Optional<String> findById(long id) {
            if (cache.containsKey(id)) {
                System.out.println("[Cache] Hit for user " + id);
                return Optional.of(cache.get(id));
            }
            Optional<String> result = delegate.findById(id);
            result.ifPresent(user -> cache.put(id, user));
            return result;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Observer Pattern ===");
        EventBus<OrderEvent> bus = new EventBus<>();
        bus.subscribe("ORDER_PLACED",    e -> System.out.println("Email: Order " + e.orderId() + " placed"));
        bus.subscribe("ORDER_PLACED",    e -> System.out.println("SMS: Order " + e.orderId() + " confirmed"));
        bus.subscribe("ORDER_SHIPPED",   e -> System.out.println("Email: Order " + e.orderId() + " shipped"));
        bus.publish("ORDER_PLACED",  new OrderEvent("ORD-001", "PLACED"));
        bus.publish("ORDER_SHIPPED", new OrderEvent("ORD-001", "SHIPPED"));

        System.out.println("\n=== Strategy Pattern ===");
        List<Integer> nums = new ArrayList<>(Arrays.asList(5, 3, 1, 4, 2));

        Sorter<Integer> sorter = new Sorter<>(list -> Collections.sort(list));  // natural sort
        sorter.sort(nums);
        System.out.println("Natural: " + nums);

        sorter.setStrategy(list -> list.sort(Comparator.reverseOrder()));       // swap strategy
        sorter.sort(nums);
        System.out.println("Reversed: " + nums);

        // Strategy is just a lambda here (since SortStrategy is @FunctionalInterface)

        System.out.println("\n=== Template Method ===");
        DataProcessor csv  = new CSVProcessor();
        DataProcessor json = new JSONProcessor();
        csv.process("  name,age,city  ");
        System.out.println();
        json.process("  hello world  ");

        System.out.println("\n=== Decorator Pattern ===");
        // Compose decorators — order matters!
        TextProcessor plain = new PlainTextProcessor();
        TextProcessor trimmed = new TrimDecorator(plain);
        TextProcessor uppercased = new UpperCaseDecorator(trimmed);
        TextProcessor logged = new LoggingDecorator(uppercased);

        logged.process("   hello world   ");

        // Can also compose with lambdas elegantly:
        Function<String, String> pipeline =
            ((Function<String, String>) String::trim)
            .andThen(String::toUpperCase)
            .andThen(s -> s + "!");
        System.out.println("Lambda pipeline: " + pipeline.apply("   hello   "));

        System.out.println("\n=== Proxy Pattern (Caching) ===");
        UserRepository real   = new RealUserRepository();
        UserRepository cached = new CachingUserRepository(real);

        cached.findById(1L);  // DB query
        cached.findById(1L);  // Cache hit
        cached.findById(2L);  // DB query
        cached.findById(2L);  // Cache hit
        cached.findById(1L);  // Cache hit
    }
}
