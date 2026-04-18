package lessons.oop_concepts;

/**
 * LESSON 01D — ABSTRACTION
 *
 * Abstraction = exposing essential features, hiding implementation details.
 * Two mechanisms: Abstract Class, Interface
 *
 * SDE2 Interview Questions:
 *  - "When to use abstract class vs interface?"
 *    => Abstract class: shared state/code + IS-A relationship (e.g., Template Method pattern)
 *       Interface: pure contract, multiple types that aren't related by hierarchy
 *  - "Can an interface have constructors?" => NO
 *  - "Can an abstract class have no abstract methods?" => YES (prevents direct instantiation)
 *  - "What are default methods in Java 8 interfaces?" => Concrete method in interface; enables
 *    adding methods without breaking existing implementations
 *  - "Diamond problem with default methods?" => Must override in implementing class when ambiguous
 */
public class AbstractionDemo {

    // ─── 1. Abstract Class ───────────────────────────────────────────────────────
    // Has: state, constructors, abstract + concrete methods, access modifiers on members
    static abstract class DatabaseConnector {
        private final String host;
        private final int port;

        // Abstract class CAN have constructor (called via super() from subclass)
        DatabaseConnector(String host, int port) {
            this.host = host;
            this.port = port;
        }

        // Abstract methods — MUST be implemented by concrete subclasses
        abstract void connect();
        abstract void disconnect();
        abstract <T> T executeQuery(String query);

        // Concrete method — shared across all subclasses (Template Method pattern)
        void performOperation(String query) {
            connect();
            Object result = executeQuery(query);
            System.out.println("Result: " + result);
            disconnect();
        }

        String getConnectionString() { return host + ":" + port; }
    }

    static class MySQLConnector extends DatabaseConnector {
        MySQLConnector() { super("localhost", 3306); }

        @Override void connect()    { System.out.println("MySQL connected to " + getConnectionString()); }
        @Override void disconnect() { System.out.println("MySQL disconnected"); }

        @Override
        @SuppressWarnings("unchecked")
        <T> T executeQuery(String query) {
            System.out.println("Executing MySQL query: " + query);
            return (T) "MySQL Result";
        }
    }

    // ─── 2. Interface ────────────────────────────────────────────────────────────
    // All fields are implicitly: public static final
    // All methods are implicitly: public abstract (unless default/static/private)
    interface Drawable {
        double PI = 3.14159;    // public static final

        void draw();            // public abstract

        // Java 8: default method — concrete implementation in interface
        default void drawWithBorder() {
            System.out.println("Drawing border...");
            draw();
        }

        // Java 8: static method in interface
        static String getVersion() { return "Drawable v2.0"; }

        // Java 9: private method (for code reuse within interface)
        // private void helper() { ... }
    }

    interface Resizable {
        void resize(double factor);

        default void resizeAndDescribe(double factor) {
            System.out.println("Resizing by " + factor);
            resize(factor);
        }
    }

    // Multiple interface implementation (Java's answer to multiple inheritance of type)
    static class Circle implements Drawable, Resizable {
        double radius;

        Circle(double r) { this.radius = r; }

        @Override public void draw() { System.out.println("Drawing circle with radius " + radius); }
        @Override public void resize(double factor) { radius *= factor; }
    }

    // ─── 3. Abstract Class vs Interface — Side-by-Side ──────────────────────────
    /*
     *  Feature                    | Abstract Class     | Interface
     *                                (extends)            (implements)
     * ────────────────────────────|────────────────────|──────────────────────────
     * Instantiation               | NO                 | NO
     * Constructors                | YES                | NO
     * Instance fields (state)     | YES                | NO (only static final)
     * Access modifiers on members | YES                | public only (mostly)
     * Multiple inheritance        | NO (single)        | YES (implement many)
     * Default/static methods      | YES                | YES (Java 8+)
     * When to use                 | Shared base impl   | Pure contract/capability
     *
     * Example: AbstractList (abstract class) vs List (interface)
     */

    // ─── 4. Diamond Problem with Interfaces ──────────────────────────────────────
    interface A {
        default String hello() { return "Hello from A"; }
    }

    interface B extends A {
        default String hello() { return "Hello from B"; }
    }

    interface C extends A {
        default String hello() { return "Hello from C"; }
    }

    // D implements B and C — diamond ambiguity, MUST override
    static class D implements B, C {
        @Override
        public String hello() {
            return B.super.hello();  // Explicitly choose which default to use
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Abstract Class ===");
        DatabaseConnector db = new MySQLConnector();
        db.performOperation("SELECT * FROM users");

        System.out.println("\n=== Interface ===");
        Circle c = new Circle(5.0);
        c.draw();
        c.drawWithBorder();     // default method
        c.resizeAndDescribe(2.0);
        System.out.println(Drawable.getVersion());  // static interface method

        System.out.println("\n=== Diamond Problem Resolution ===");
        D d = new D();
        System.out.println(d.hello());  // Hello from B (explicitly chosen)

        System.out.println("\n=== Polymorphism via Interface ===");
        Drawable[] drawables = { new Circle(3), new Circle(7) };
        for (Drawable drawable : drawables) drawable.draw(); // runtime dispatch
    }
}
