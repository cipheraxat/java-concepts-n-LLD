package lessons.static_and_final;

import java.util.*;

/**
 * LESSON 12A — MASTERING static AND final KEYWORDS
 *
 * Two of the most frequently misunderstood keywords in Java interviews.
 *
 * static = belongs to the CLASS, not to any instance.
 * final  = "cannot be changed" — but what "changed" means depends on context.
 *
 * SDE2 Interview:
 *  - "Can a static method access instance variables?"
 *     => NO — no `this` reference in static context.
 *  - "What does final mean on a reference variable?"
 *     => The reference cannot be reassigned, but the object it points to CAN be mutated.
 *  - "Can we override a static method?"
 *     => NO — static methods are hidden, not overridden (resolved at compile time).
 *  - "Can we override a final method?"
 *     => NO — final methods cannot be overridden.
 */
public class StaticFinalDemo {

    // ═════════════════════════════════════════════════════════════════════════════
    // PART 1 — THE static KEYWORD
    // ═════════════════════════════════════════════════════════════════════════════

    // ─── 1.1 Static Fields (Class Variables) ─────────────────────────────────────
    /*
     * Shared across ALL instances of the class.
     * Stored in the Metaspace (not heap per-object).
     * Accessed via ClassName.field (preferred) or instance.field (discouraged).
     */

    static class Employee {
        private static int totalCount = 0;   // shared — one copy for the class
        private static final String COMPANY = "TechCorp"; // static + final = constant

        private final int id;     // instance final — set once per object
        private String name;

        Employee(String name) {
            this.id = ++totalCount;  // auto-increment from shared counter
            this.name = name;
        }

        // Static method — accesses only static members
        static int getTotalCount() { return totalCount; }

        // Instance method — can access both static and instance members
        String describe() {
            return name + " (#" + id + " at " + COMPANY + ")";
        }
    }

    // ─── 1.2 Static Methods ─────────────────────────────────────────────────────
    /*
     * - No `this` reference → cannot access instance fields/methods directly.
     * - Cannot be overridden (only hidden) — dispatch is at compile time.
     * - Common uses: utility/helper methods, factory methods, main().
     *
     * Interview: "Why is main() static?"
     * => JVM needs to invoke it without creating an instance of the class.
     */

    static class MathHelper {
        static int factorial(int n) {
            if (n < 0) throw new IllegalArgumentException("Negative input");
            int result = 1;
            for (int i = 2; i <= n; i++) result *= i;
            return result;
        }

        static boolean isPrime(int n) {
            if (n < 2) return false;
            for (int i = 2; i * i <= n; i++) {
                if (n % i == 0) return false;
            }
            return true;
        }
    }

    // ─── 1.3 Static Blocks (Class Initializers) ─────────────────────────────────
    /*
     * Executed ONCE when the class is first loaded by the ClassLoader.
     * Runs BEFORE any constructor or static method is called.
     * Multiple static blocks execute in declaration order.
     *
     * Use cases: complex initialization of static fields, loading native libs,
     *            populating lookup maps.
     */

    static class Config {
        private static final Map<String, String> DEFAULTS;
        private static final String VERSION;

        static {
            System.out.println("  [Config] Static block 1 — loading defaults");
            DEFAULTS = new HashMap<>();
            DEFAULTS.put("timeout", "30");
            DEFAULTS.put("retries", "3");
            DEFAULTS.put("env", "production");
        }

        static {
            System.out.println("  [Config] Static block 2 — setting version");
            VERSION = "2.1.0";
        }

        static String get(String key) {
            return DEFAULTS.getOrDefault(key, "N/A");
        }

        static String getVersion() { return VERSION; }
    }

    // ─── 1.4 Static Nested Class vs Inner Class ─────────────────────────────────
    /*
     * Static nested class:
     *   - Does NOT hold reference to enclosing instance.
     *   - Can only access outer class's STATIC members.
     *   - Lower memory footprint — no hidden Outer.this pointer.
     *   - Preferred unless you need access to instance members.
     *
     * Inner class (non-static):
     *   - Holds implicit reference to the enclosing instance (Outer.this).
     *   - Can access ALL outer members, including private instance fields.
     *   - Can cause memory leaks if the inner object outlives the outer.
     *
     * Interview: "When to use static nested class vs inner class?"
     * => Prefer static nested unless you genuinely need access to instance state.
     */

    static class Connection {
        private String url = "jdbc:mysql://localhost/db";
        private static String driver = "com.mysql.cj.jdbc.Driver";

        // Static nested — no reference to Connection instance
        static class DriverInfo {
            String getDriver() { return driver; } // ✅ static members only
        }

        // Inner — has implicit Connection.this
        class Session {
            String getUrl() { return url; }        // ✅ can access instance members
        }
    }

    // ─── 1.5 Static Method Hiding (NOT overriding) ──────────────────────────────
    /*
     * Static methods are resolved at COMPILE TIME based on reference type.
     * When a subclass defines a static method with the same signature, it HIDES
     * the parent's version — it does NOT override it.
     *
     * This means: no polymorphic dispatch for static methods.
     */

    static class Parent {
        static String greet() { return "Hello from Parent"; }
        String instanceGreet() { return "Instance Parent"; }
    }

    static class ChildClass extends Parent {
        static String greet() { return "Hello from Child"; }  // HIDES — not overrides
        @Override
        String instanceGreet() { return "Instance Child"; }   // TRUE override
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // PART 2 — THE final KEYWORD
    // ═════════════════════════════════════════════════════════════════════════════

    // ─── 2.1 final Variables ─────────────────────────────────────────────────────
    /*
     * Primitive final    → value cannot change.
     * Reference final    → reference cannot be reassigned, but object can be MUTATED.
     *
     *   final int x = 10;       x = 20;     // ❌ COMPILE ERROR
     *   final List<String> list = new ArrayList<>();
     *   list.add("hello");                   // ✅ OK — mutating the object
     *   list = new ArrayList<>();            // ❌ COMPILE ERROR — reassigning reference
     *
     * Interview: "Does final make an object immutable?"
     * => NO. It only prevents reassignment of the reference. The object's internal
     *    state can still change. For true immutability, design the class to be immutable.
     */

    static void finalVariableDemo() {
        final int MAX = 100;
        // MAX = 200;  // ❌ COMPILE ERROR

        final List<String> names = new ArrayList<>();
        names.add("Alice");     // ✅ mutation is allowed
        names.add("Bob");       // ✅
        // names = new ArrayList<>();  // ❌ COMPILE ERROR — cannot reassign

        System.out.println("  final List (mutated): " + names);
    }

    // ─── 2.2 Blank Final Variables ──────────────────────────────────────────────
    /*
     * A final instance field that is NOT initialized at declaration.
     * Must be assigned EXACTLY ONCE — in every constructor path.
     *
     * Useful for values that depend on constructor arguments.
     */

    static class ImmutablePoint {
        private final int x;  // blank final
        private final int y;  // blank final

        ImmutablePoint(int x, int y) {
            this.x = x;  // assigned once
            this.y = y;  // assigned once
        }

        // Alternative constructor — still must assign both
        ImmutablePoint(int xy) {
            this(xy, xy);  // delegates
        }

        @Override
        public String toString() { return "(" + x + ", " + y + ")"; }
    }

    // ─── 2.3 final Methods ──────────────────────────────────────────────────────
    /*
     * Cannot be overridden by subclasses.
     * Use cases:
     *   - Prevent breaking critical logic (e.g., template method skeleton).
     *   - Security — prevent subclass from altering behavior.
     *   - JVM can inline final methods → potential performance gain.
     *
     * Interview: "Can we override a final method?"
     * => NO — compile error.
     */

    static class TemplateProcessor {
        // Template method pattern — final skeleton, customizable steps
        final void process() {
            validate();
            execute();
            cleanup();
        }

        void validate() { System.out.println("    Default validate"); }
        void execute()  { System.out.println("    Default execute"); }
        void cleanup()  { System.out.println("    Default cleanup"); }
    }

    static class CustomProcessor extends TemplateProcessor {
        @Override
        void validate() { System.out.println("    Custom validate"); }
        @Override
        void execute()  { System.out.println("    Custom execute"); }

        // @Override void process() {}  // ❌ COMPILE ERROR — process() is final
    }

    // ─── 2.4 final Classes ──────────────────────────────────────────────────────
    /*
     * Cannot be subclassed.
     * Examples in JDK: String, Integer, Math, LocalDate.
     *
     * Why make a class final?
     *  1. Immutability guarantee — no subclass can add mutable state.
     *  2. Security — prevents malicious subclass overriding equals/hashCode.
     *  3. Design intent — class is complete, not designed for extension.
     *
     * Interview: "Why is String final?"
     * => Immutability + security (class loading, String pool) + thread safety.
     *    If String could be subclassed, a MaliciousString could override methods.
     */

    static final class Money {
        private final long cents;       // final field
        private final String currency;  // final field

        Money(long cents, String currency) {
            this.cents = cents;
            this.currency = Objects.requireNonNull(currency);
        }

        Money add(Money other) {
            if (!currency.equals(other.currency))
                throw new IllegalArgumentException("Currency mismatch");
            return new Money(cents + other.cents, currency);  // returns NEW object
        }

        @Override
        public String toString() {
            return String.format("%s %.2f", currency, cents / 100.0);
        }
    }

    // static class ExtendedMoney extends Money {}  // ❌ COMPILE ERROR — Money is final

    // ─── 2.5 final Parameters ───────────────────────────────────────────────────
    /*
     * Prevents reassignment of method parameters inside the method body.
     * Commonly seen in lambdas — captured variables must be effectively final.
     */

    static String formatGreeting(final String name, final int age) {
        // name = "other";  // ❌ COMPILE ERROR — parameter is final
        return "Hello, " + name + " (age " + age + ")";
    }

    // ─── 2.6 Effectively Final (Java 8+) ────────────────────────────────────────
    /*
     * A local variable that is not declared final but is never reassigned.
     * Can be used in lambdas and anonymous classes just like final.
     *
     * Interview: "What is effectively final?"
     * => A variable not declared final but never reassigned after initialization.
     *    The compiler treats it as if it were final.
     */

    static void effectivelyFinalDemo() {
        String prefix = "LOG";  // effectively final — never reassigned
        List<String> messages = List.of("start", "process", "end");

        // prefix is effectively final → can be captured by lambda
        messages.forEach(msg -> System.out.println("  " + prefix + ": " + msg));

        // If we uncomment the next line, lambda above would NOT compile:
        // prefix = "DEBUG";
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // PART 3 — static + final COMBINED PATTERNS
    // ═════════════════════════════════════════════════════════════════════════════

    // ─── 3.1 Constants (static final) ────────────────────────────────────────────
    /*
     * static final = compile-time or class-level constant.
     * Convention: UPPER_SNAKE_CASE.
     *
     * Primitive/String constants are inlined by the compiler:
     *   static final int X = 10;  → references to X become literal 10 in bytecode.
     *
     * Interview: "Where are static final constants stored?"
     * => In the constant pool of the class. Primitives/Strings may be inlined.
     */

    static final int MAX_RETRIES = 3;
    static final String DEFAULT_ENCODING = "UTF-8";
    static final List<String> SUPPORTED_TYPES = List.of("JSON", "XML", "CSV"); // immutable list

    // ─── 3.2 Static Final Collections — Immutable vs Unmodifiable ────────────────
    /*
     * List.of()  / Map.of()  → truly immutable (Java 9+)
     * Collections.unmodifiableList() → unmodifiable VIEW of a mutable list
     *
     * Interview: "Difference between immutable and unmodifiable collection?"
     * => Unmodifiable is a read-only wrapper — if someone has the original reference,
     *    they can still mutate it, and changes are reflected through the wrapper.
     *    Truly immutable (List.of) has no backing mutable list.
     */

    static void collectionsDemo() {
        // Truly immutable
        List<String> immutable = List.of("A", "B", "C");
        // immutable.add("D");  // ❌ UnsupportedOperationException

        // Unmodifiable wrapper — backed by mutable list
        List<String> mutable = new ArrayList<>(List.of("X", "Y"));
        List<String> view = Collections.unmodifiableList(mutable);
        mutable.add("Z");  // original mutated
        System.out.println("  Unmodifiable view sees mutation: " + view); // [X, Y, Z]
    }

    // ─── 3.3 Static Factory with Final (Cached Instances) ────────────────────────

    static final class Color {
        private final int r, g, b;

        private static final Color BLACK = new Color(0, 0, 0);
        private static final Color WHITE = new Color(255, 255, 255);

        private Color(int r, int g, int b) { // private constructor
            this.r = r; this.g = g; this.b = b;
        }

        // Static factory — can return cached instances
        static Color of(int r, int g, int b) {
            if (r == 0 && g == 0 && b == 0) return BLACK;
            if (r == 255 && g == 255 && b == 255) return WHITE;
            return new Color(r, g, b);
        }

        @Override
        public String toString() {
            return "rgb(" + r + "," + g + "," + b + ")";
        }
    }

    // ─── 3.4 Static Initializer + Final Field Gotcha ─────────────────────────────
    /*
     * Order matters! Static fields are initialized in declaration order.
     * A static block referencing a field declared later sees the default value (0/null).
     *
     * Interview trick question:
     */

    static class OrderMatters {
        static final int A;
        static final int B;

        static {
            A = 10;
            B = A * 2;  // B = 20 ✅ (A is already assigned above)
        }
        // If B were declared before A's assignment, B would be 0 * 2 = 0
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // PART 4 — COMMON INTERVIEW TRAPS
    // ═════════════════════════════════════════════════════════════════════════════

    // ─── 4.1 Static method hiding vs instance method overriding ──────────────────

    static void staticHidingDemo() {
        Parent p = new ChildClass();
        System.out.println("  Static via Parent ref: " + p.greet());         // Parent — hiding
        System.out.println("  Instance via Parent ref: " + p.instanceGreet()); // Child — overriding

        ChildClass c = new ChildClass();
        System.out.println("  Static via Child ref: " + c.greet());          // Child
        System.out.println("  Instance via Child ref: " + c.instanceGreet()); // Child
    }

    // ─── 4.2 Can we have static methods in interfaces? ──────────────────────────
    /*
     * YES (Java 8+). They are NOT inherited by implementing classes.
     * Must be called via InterfaceName.method().
     */

    interface IdGenerator {
        static String generate() {
            return UUID.randomUUID().toString().substring(0, 8);
        }
    }

    // ─── 4.3 final and try-with-resources ────────────────────────────────────────
    /*
     * Since Java 9, effectively final variables can be used in try-with-resources:
     *   final var resource = new FileInputStream("file.txt");
     *   try (resource) { ... }
     */

    // ─── 4.4 static and Serialization ────────────────────────────────────────────
    /*
     * Static fields are NOT serialized — they belong to the class, not the instance.
     * After deserialization, static fields hold whatever value the class currently has.
     *
     * Interview: "Are static fields serialized?"
     * => NO.
     */

    // ═════════════════════════════════════════════════════════════════════════════
    // MAIN — demo all concepts
    // ═════════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {

        // --- 1.1 Static Fields ---
        System.out.println("=== 1.1 Static Fields ===");
        Employee e1 = new Employee("Alice");
        Employee e2 = new Employee("Bob");
        Employee e3 = new Employee("Carol");
        System.out.println("  " + e1.describe());
        System.out.println("  " + e2.describe());
        System.out.println("  Total employees: " + Employee.getTotalCount());

        // --- 1.2 Static Methods ---
        System.out.println("\n=== 1.2 Static Methods ===");
        System.out.println("  factorial(6) = " + MathHelper.factorial(6));
        System.out.println("  isPrime(17)  = " + MathHelper.isPrime(17));
        System.out.println("  isPrime(18)  = " + MathHelper.isPrime(18));

        // --- 1.3 Static Blocks ---
        System.out.println("\n=== 1.3 Static Blocks ===");
        System.out.println("  Config version: " + Config.getVersion());
        System.out.println("  timeout = " + Config.get("timeout"));
        System.out.println("  retries = " + Config.get("retries"));

        // --- 1.4 Static Nested vs Inner ---
        System.out.println("\n=== 1.4 Static Nested vs Inner ===");
        Connection.DriverInfo di = new Connection.DriverInfo(); // no Connection instance needed
        System.out.println("  DriverInfo: " + di.getDriver());
        Connection conn = new Connection();
        Connection.Session sess = conn.new Session();           // needs Connection instance
        System.out.println("  Session URL: " + sess.getUrl());

        // --- 1.5 Static Method Hiding ---
        System.out.println("\n=== 1.5 Static Method Hiding ===");
        staticHidingDemo();

        // --- 2.1 final Variables ---
        System.out.println("\n=== 2.1 final Variables ===");
        finalVariableDemo();

        // --- 2.2 Blank Final ---
        System.out.println("\n=== 2.2 Blank Final ===");
        ImmutablePoint p1 = new ImmutablePoint(3, 7);
        ImmutablePoint p2 = new ImmutablePoint(5);
        System.out.println("  p1 = " + p1);
        System.out.println("  p2 = " + p2);

        // --- 2.3 final Methods (Template) ---
        System.out.println("\n=== 2.3 final Methods (Template) ===");
        System.out.println("  CustomProcessor:");
        new CustomProcessor().process();

        // --- 2.4 final Classes ---
        System.out.println("\n=== 2.4 final Classes ===");
        Money a = new Money(1050, "USD");
        Money b = new Money(2575, "USD");
        System.out.println("  " + a + " + " + b + " = " + a.add(b));

        // --- 2.5 final Parameters ---
        System.out.println("\n=== 2.5 final Parameters ===");
        System.out.println("  " + formatGreeting("Alice", 30));

        // --- 2.6 Effectively Final ---
        System.out.println("\n=== 2.6 Effectively Final ===");
        effectivelyFinalDemo();

        // --- 3.1 Constants ---
        System.out.println("\n=== 3.1 Constants ===");
        System.out.println("  MAX_RETRIES = " + MAX_RETRIES);
        System.out.println("  SUPPORTED_TYPES = " + SUPPORTED_TYPES);

        // --- 3.2 Immutable vs Unmodifiable ---
        System.out.println("\n=== 3.2 Immutable vs Unmodifiable ===");
        collectionsDemo();

        // --- 3.3 Static Factory ---
        System.out.println("\n=== 3.3 Static Factory ===");
        Color black1 = Color.of(0, 0, 0);
        Color black2 = Color.of(0, 0, 0);
        System.out.println("  Cached? " + (black1 == black2));  // true — same instance
        System.out.println("  Custom: " + Color.of(128, 64, 255));

        // --- Interface static method ---
        System.out.println("\n=== 4.2 Interface Static Method ===");
        System.out.println("  Generated ID: " + IdGenerator.generate());

        // --- Order matters ---
        System.out.println("\n=== 3.4 Static Init Order ===");
        System.out.println("  A = " + OrderMatters.A + ", B = " + OrderMatters.B);
    }
}
