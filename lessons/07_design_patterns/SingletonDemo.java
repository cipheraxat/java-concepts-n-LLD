package lessons.design_patterns;

import java.util.*;
import java.util.function.Supplier;

/**
 * LESSON 07A — Singleton + Builder + Factory Patterns
 *
 * SDE2 Interview Questions:
 *  - "How to make Singleton thread-safe?"
 *    => Enum (best), Initialization-on-Demand Holder, or double-checked locking with volatile
 *  - "How can Singleton be broken?" => Reflection, Serialization, Cloning — enum prevents all 3
 *  - "Builder vs Constructor?" => Builder: readable, immutable, optional params, no telescoping constructors
 *  - "Factory Method vs Abstract Factory?" => Factory: one product type; Abstract Factory: families of products
 */
public class SingletonDemo {

    // ─── 1. Singleton: Eager initialization ─────────────────────────────────────
    // Thread-safe (class loading is synchronized by JVM), but wastes memory if unused
    static class EagerSingleton {
        private static final EagerSingleton INSTANCE = new EagerSingleton();
        private int count = 0;
        private EagerSingleton() {}                      // private constructor
        static EagerSingleton getInstance() { return INSTANCE; }
        void increment() { count++; }
        int getCount()   { return count; }
    }

    // ─── 2. Singleton: Lazy with double-checked locking (DCL) ───────────────────
    // volatile prevents instruction reordering during construction
    static class LazyDCLSingleton {
        private static volatile LazyDCLSingleton instance;  // volatile is CRITICAL here
        private LazyDCLSingleton() {}
        static LazyDCLSingleton getInstance() {
            if (instance == null) {                          // first check (no locking - fast)
                synchronized (LazyDCLSingleton.class) {
                    if (instance == null) {                  // second check (under lock)
                        instance = new LazyDCLSingleton();   // volatile prevents partial construction visibility
                    }
                }
            }
            return instance;
        }
    }

    // ─── 3. Singleton: Initialization-on-demand Holder (BEST for lazy) ──────────
    // Leverages class loader guarantee: inner class only loads on first access (lazy & thread-safe)
    static class HolderSingleton {
        private HolderSingleton() {}
        private static class Holder {
            static final HolderSingleton INSTANCE = new HolderSingleton();
        }
        static HolderSingleton getInstance() { return Holder.INSTANCE; }
    }

    // ─── 4. Singleton: Enum (BEST overall — prevents reflection/serialization attacks) ─
    enum EnumSingleton {
        INSTANCE;
        private int count = 0;
        void increment() { count++; }
        int getCount()   { return count; }
        // Enum prevents: reflection (constructor throws Exception), serialization (enum guarantees one instance)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ─── BUILDER PATTERN ─────────────────────────────────────────────────────────
    // Problem: Telescoping constructors (too many params) / JavaBean mutability
    // Solution: Step-by-step construction, immutable result
    // ─────────────────────────────────────────────────────────────────────────────

    static final class HttpRequest {
        // Required fields
        private final String url;
        private final String method;
        // Optional fields
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMs;
        private final boolean followRedirects;

        private HttpRequest(Builder b) {
            this.url             = b.url;
            this.method          = b.method;
            this.headers         = Collections.unmodifiableMap(new HashMap<>(b.headers));
            this.body            = b.body;
            this.timeoutMs       = b.timeoutMs;
            this.followRedirects = b.followRedirects;
        }

        // Getters (no setters — immutable)
        String getUrl()    { return url; }
        String getMethod() { return method; }
        Map<String, String> getHeaders() { return headers; }
        @Override public String toString() {
            return method + " " + url + " headers=" + headers + " timeout=" + timeoutMs;
        }

        static class Builder {
            // Required
            private final String url;
            private final String method;
            // Optional with defaults
            private Map<String, String> headers = new HashMap<>();
            private String body   = null;
            private int timeoutMs = 5000;
            private boolean followRedirects = true;

            Builder(String url, String method) {
                Objects.requireNonNull(url, "url required");
                Objects.requireNonNull(method, "method required");
                this.url = url; this.method = method;
            }

            Builder header(String key, String value)  { headers.put(key, value); return this; }
            Builder body(String body)                  { this.body = body;        return this; }
            Builder timeout(int ms)                    { this.timeoutMs = ms;     return this; }
            Builder followRedirects(boolean follow)    { this.followRedirects = follow; return this; }

            HttpRequest build() { return new HttpRequest(this); }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // ─── FACTORY METHOD PATTERN ──────────────────────────────────────────────────
    // Decouple creation (factory subclass) from usage (client uses interface)
    // ─────────────────────────────────────────────────────────────────────────────

    interface NotificationService {
        void send(String to, String message);
    }

    static class EmailService implements NotificationService {
        @Override public void send(String to, String msg) {
            System.out.println("Email to " + to + ": " + msg);
        }
    }

    static class SMSService implements NotificationService {
        @Override public void send(String to, String msg) {
            System.out.println("SMS to " + to + ": " + msg);
        }
    }

    static class PushService implements NotificationService {
        @Override public void send(String to, String msg) {
            System.out.println("Push to " + to + ": " + msg);
        }
    }

    // Factory — encapsulates creation logic
    static class NotificationFactory {
        enum Type { EMAIL, SMS, PUSH }

        static NotificationService create(Type type) {
            return switch (type) {
                case EMAIL -> new EmailService();
                case SMS   -> new SMSService();
                case PUSH  -> new PushService();
            };
        }
    }

    // ─── ABSTRACT FACTORY — family of related objects ─────────────────────────
    interface Button  { void render(); }
    interface Checkbox{ void render(); }

    // Light theme family
    static class LightButton  implements Button   { public void render() { System.out.println("Light Button"); } }
    static class LightCheckbox implements Checkbox { public void render() { System.out.println("Light Checkbox"); } }
    // Dark theme family
    static class DarkButton  implements Button   { public void render() { System.out.println("Dark Button"); } }
    static class DarkCheckbox implements Checkbox { public void render() { System.out.println("Dark Checkbox"); } }

    interface UIFactory {
        Button   createButton();
        Checkbox createCheckbox();
    }

    static class LightThemeFactory implements UIFactory {
        public Button   createButton()   { return new LightButton(); }
        public Checkbox createCheckbox() { return new LightCheckbox(); }
    }

    static class DarkThemeFactory implements UIFactory {
        public Button   createButton()   { return new DarkButton(); }
        public Checkbox createCheckbox() { return new DarkCheckbox(); }
    }

    public static void main(String[] args) {
        System.out.println("=== Singleton ===");
        EnumSingleton s1 = EnumSingleton.INSTANCE;
        EnumSingleton s2 = EnumSingleton.INSTANCE;
        s1.increment(); s1.increment();
        System.out.println("Same instance: " + (s1 == s2));      // true
        System.out.println("Count: " + s2.getCount());           // 2 — shared state

        System.out.println("\n=== Builder Pattern ===");
        HttpRequest request = new HttpRequest.Builder("https://api.example.com/users", "POST")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer token123")
            .body("{\"name\": \"Alice\"}")
            .timeout(10000)
            .build();
        System.out.println(request);

        System.out.println("\n=== Factory Method ===");
        NotificationService email = NotificationFactory.create(NotificationFactory.Type.EMAIL);
        NotificationService sms   = NotificationFactory.create(NotificationFactory.Type.SMS);
        NotificationService push  = NotificationFactory.create(NotificationFactory.Type.PUSH);
        email.send("alice@example.com", "Your order is ready");
        sms.send("+1234567890", "Your order is ready");
        push.send("device-token-xyz", "Your order is ready");

        System.out.println("\n=== Abstract Factory ===");
        UIFactory factory = Boolean.getBoolean("dark.mode") ? new DarkThemeFactory() : new LightThemeFactory();
        Button   button   = factory.createButton();
        Checkbox checkbox = factory.createCheckbox();
        button.render(); checkbox.render();

        // Change theme: swap factory, not the code that creates components
        UIFactory darkFactory = new DarkThemeFactory();
        darkFactory.createButton().render();
        darkFactory.createCheckbox().render();
    }
}
