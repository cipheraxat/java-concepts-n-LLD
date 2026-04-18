package lessons.access_modifiers;

/**
 * LESSON 11A — ACCESS MODIFIERS
 *
 * Java has 4 access levels (most restrictive → least restrictive):
 *   private → default (package-private) → protected → public
 *
 * SDE2 Interview:
 *  - "Explain all four access modifiers with scope."
 *  - "Can a subclass in a different package access protected members?"
 *     => YES — via inheritance only, NOT through object reference of parent type.
 *  - "What is the default access level?" => Package-private (no keyword)
 *  - "Can a top-level class be private or protected?" => NO — only public or default
 */
public class AccessModifiersDemo {

    // ─── 1. Access Modifier Scope ────────────────────────────────────────────────
    /*
     * ┌───────────┬───────┬─────────┬────────────┬───────┐
     * │ Modifier  │ Class │ Package │ Subclass   │ World │
     * │           │       │         │ (diff pkg) │       │
     * ├───────────┼───────┼─────────┼────────────┼───────┤
     * │ private   │  ✅   │   ❌    │    ❌      │  ❌   │
     * │ default   │  ✅   │   ✅    │    ❌      │  ❌   │
     * │ protected │  ✅   │   ✅    │    ✅      │  ❌   │
     * │ public    │  ✅   │   ✅    │    ✅      │  ✅   │
     * └───────────┴───────┴─────────┴────────────┴───────┘
     */

    // ─── 2. private — Class-only access ──────────────────────────────────────────

    static class Credentials {
        private String password;  // only accessible within Credentials

        Credentials(String password) {
            this.password = password;
        }

        // Controlled access via public method — encapsulation
        public boolean verify(String attempt) {
            return password.equals(attempt);
        }

        // Private helper — internal implementation detail
        private String mask() {
            return "****" + password.substring(password.length() - 2);
        }

        public String getMasked() { return mask(); }
    }

    // ─── 3. default (package-private) — Same package only ────────────────────────
    /*
     * No keyword → default access.
     * Classes, methods, and fields without any modifier are package-private.
     * This is Java's way of grouping related collaborators within a package.
     */

    static class PackageHelper {
        // default access — visible only within lessons.access_modifiers package
        String internalName = "helper";

        String getInternalName() {
            return internalName;
        }
    }

    // ─── 4. protected — Same package + subclasses ────────────────────────────────

    static class Animal {
        protected String name;

        protected Animal(String name) {
            this.name = name;
        }

        protected String sound() {
            return "...";
        }
    }

    // Same package — subclass CAN access protected members
    static class Dog extends Animal {
        Dog(String name) { super(name); }

        @Override
        protected String sound() { return "Woof"; }

        // Accessing protected field from parent — allowed in subclass
        public String describe() {
            return name + " says " + sound();
        }
    }

    // ─── 5. public — Accessible everywhere ───────────────────────────────────────

    public static class MathUtils {
        public static final double PI = 3.14159265;

        public static double circleArea(double radius) {
            return PI * radius * radius;
        }
    }

    // ─── 6. Private Constructor — Utility Classes & Singleton ────────────────────
    /*
     * Interview: "When would you make a constructor private?"
     *  1. Singleton pattern — only one instance
     *  2. Utility class — all static methods, no instantiation
     *  3. Factory pattern — control object creation via static factory methods
     *  4. Builder pattern — construction through nested Builder
     */

    static class Registry {
        private static final Registry INSTANCE = new Registry();
        private int count = 0;

        private Registry() {}  // Private constructor — prevents external instantiation

        public static Registry getInstance() { return INSTANCE; }

        public void register(String name) {
            count++;
            System.out.println("  Registered: " + name + " (total: " + count + ")");
        }
    }

    // Utility class with private constructor
    static class StringUtils {
        private StringUtils() {
            throw new AssertionError("Utility class — do not instantiate");
        }

        public static boolean isNullOrEmpty(String s) {
            return s == null || s.isEmpty();
        }

        public static String capitalize(String s) {
            if (isNullOrEmpty(s)) return s;
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    // ─── 7. Access Modifiers & Method Overriding ─────────────────────────────────
    /*
     * RULE: Overriding method CANNOT have MORE restrictive access than the parent.
     *       (Can be same or wider)
     *
     *   Parent method      → Valid overrides in child
     *   ─────────────         ─────────────────────────
     *   public             → public ONLY
     *   protected          → protected OR public
     *   default            → default, protected, OR public
     *   private            → NOT overridden (not visible to child)
     *
     * WHY? Liskov Substitution — a subclass instance must be usable wherever
     * the parent type is expected. Restricting access would break that contract.
     */

    static class Base {
        protected void action() {
            System.out.println("  Base#action (protected)");
        }
    }

    static class Child extends Base {
        // Widening access: protected → public ✅ ALLOWED
        @Override
        public void action() {
            System.out.println("  Child#action (public — widened from protected)");
        }
    }

    // Narrowing would fail:
    // static class BadChild extends Base {
    //     @Override
    //     private void action() {}  // COMPILE ERROR — cannot reduce visibility
    // }

    // ─── 8. Access Modifiers on Interfaces ───────────────────────────────────────
    /*
     * Interface members have implicit modifiers:
     *  - Methods       → public abstract    (before Java 8)
     *  - Default methods → public           (Java 8+)
     *  - Static methods  → public           (Java 8+)
     *  - Private methods → private          (Java 9+ — helper for default methods)
     *  - Fields        → public static final (constants)
     *
     * You CANNOT use protected or default access on interface methods.
     */

    interface Loggable {
        // implicitly public abstract
        String getLogPrefix();

        // implicitly public
        default String log(String message) {
            return format(getLogPrefix(), message);
        }

        // private helper — Java 9+ (used internally by default methods)
        private String format(String prefix, String msg) {
            return "[" + prefix + "] " + msg;
        }
    }

    static class Service implements Loggable {
        @Override
        public String getLogPrefix() { return "SVC"; }
        // Must be public — interface methods are implicitly public
    }

    // ─── 9. Access Modifiers on Top-level Classes ────────────────────────────────
    /*
     * Top-level class can ONLY be:
     *   - public  → one public class per file, filename must match
     *   - default → package-private, accessible only within same package
     *
     * CANNOT be private or protected at top level.
     *
     * Inner/nested classes can use ALL four modifiers:
     *   public class Outer {
     *       private   class PrivateInner   {}  // ✅
     *       protected class ProtectedInner {}  // ✅
     *                 class DefaultInner   {}  // ✅
     *       public    class PublicInner    {}  // ✅
     *   }
     */

    // ─── 10. Nested Class Access Rules ───────────────────────────────────────────

    static class Outer {
        private int secret = 42;
        private static int staticSecret = 100;

        // Static nested class — can access outer's PRIVATE STATIC members
        static class StaticNested {
            int getStaticSecret() { return staticSecret; } // ✅
            // int getSecret() { return secret; }          // ❌ needs Outer instance
        }

        // Inner class — can access ALL outer members including PRIVATE
        class Inner {
            int getSecret() { return secret; }             // ✅
            int getStaticSecret() { return staticSecret; } // ✅
        }
    }

    // ═════════════════════════════════════════════════════════════════════════════
    // MAIN — demo all concepts
    // ═════════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {

        // --- private ---
        System.out.println("=== private ===");
        Credentials creds = new Credentials("myP@ss99");
        System.out.println("  Verify correct: " + creds.verify("myP@ss99"));
        System.out.println("  Verify wrong:   " + creds.verify("wrong"));
        System.out.println("  Masked:         " + creds.getMasked());
        // creds.password → COMPILE ERROR (private)

        // --- default (package-private) ---
        System.out.println("\n=== default (package-private) ===");
        PackageHelper helper = new PackageHelper();
        System.out.println("  " + helper.getInternalName()); // same package — OK

        // --- protected ---
        System.out.println("\n=== protected ===");
        Dog dog = new Dog("Rex");
        System.out.println("  " + dog.describe());

        // --- public ---
        System.out.println("\n=== public ===");
        System.out.println("  Circle area (r=5): " + MathUtils.circleArea(5));

        // --- Private constructor (Singleton) ---
        System.out.println("\n=== Private Constructor (Singleton) ===");
        Registry reg = Registry.getInstance();
        reg.register("ServiceA");
        reg.register("ServiceB");
        System.out.println("  Same instance? " + (reg == Registry.getInstance()));

        // --- Utility class ---
        System.out.println("\n=== Utility Class ===");
        System.out.println("  isNullOrEmpty(\"\"): " + StringUtils.isNullOrEmpty(""));
        System.out.println("  capitalize(\"hello\"): " + StringUtils.capitalize("hello"));

        // --- Overriding with wider access ---
        System.out.println("\n=== Access Widening on Override ===");
        Base b = new Child();
        b.action();  // Child#action runs — wider access is fine

        // --- Interface access ---
        System.out.println("\n=== Interface Access ===");
        Service svc = new Service();
        System.out.println("  " + svc.log("Request received"));

        // --- Nested class access ---
        System.out.println("\n=== Nested Class Access ===");
        Outer.StaticNested sn = new Outer.StaticNested();
        System.out.println("  Static nested → staticSecret: " + sn.getStaticSecret());

        Outer outer = new Outer();
        Outer.Inner inner = outer.new Inner();
        System.out.println("  Inner → secret: " + inner.getSecret());
    }
}
