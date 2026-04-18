package lessons.java8_features;

import java.util.Optional;
import java.util.List;

/**
 * LESSON 06C — Optional + Modern Java (Records, Sealed Classes, Switch Expressions, var)
 *
 * Optional: a container that may or may not hold a value — forces proper null handling.
 * NOT a replacement for all nulls — use for return types where absence is meaningful.
 *
 * SDE2 Interview Questions:
 *  - "When to use Optional?" => Return type of methods that may not return a value.
 *    NOT for fields, method parameters, or collections.
 *  - "Optional.get() vs orElseThrow()?" => Prefer orElseThrow() with a meaningful exception.
 *  - "What are Java Records?" => Immutable data classes: auto generates constructor, getters, equals, hashCode, toString
 *  - "Sealed classes?" => Restrict which classes can extend/implement — exhaustive pattern matching
 */
public class OptionalDemo {

    // ─── Simulated repository returning Optional ─────────────────────────────────
    record User(long id, String name, String email) {}

    static Optional<User> findById(long id) {
        if (id == 1L) return Optional.of(new User(1L, "Alice", "alice@example.com"));
        return Optional.empty();
    }

    static Optional<String> getEmail(long userId) {
        return findById(userId).map(User::email);  // safe transform — returns empty if not found
    }

    // ─── Optional chain (like null-safe navigation) ───────────────────────────────
    record Address(String city, String zip) {}
    record Order(Long id, Address shippingAddress) {}

    static Optional<String> getShippingCity(long orderId) {
        // Simulated DB lookup
        if (orderId == 1L)
            return Optional.of(new Order(1L, new Address("New York", "10001")))
                           .map(Order::shippingAddress)
                           .map(Address::city);
        return Optional.empty();
    }

    // Java 17+ sealed hierarchy (moved out of local method scope)
    sealed interface Shape permits Circle, Rectangle, Triangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}

    public static void main(String[] args) {
        System.out.println("=== Optional Basics ===");
        Optional<String> present = Optional.of("Hello");
        Optional<String> empty   = Optional.empty();
        Optional<String> nullable = Optional.ofNullable(null); // same as empty()
        Optional<String> notNull = Optional.ofNullable("World");

        System.out.println("isPresent: " + present.isPresent());   // true
        System.out.println("isEmpty:   " + empty.isEmpty());       // true (Java 11+)

        System.out.println("\n=== Optional retrieval methods ===");
        // get() — AVOID: throws NoSuchElementException if empty
        // present.get(); // risky

        // orElse — provide default (default is ALWAYS evaluated as expression)
        System.out.println("orElse: "         + empty.orElse("default"));
        // orElseGet — lazy (supplier only called if empty) — PREFER this
        System.out.println("orElseGet: "      + empty.orElseGet(() -> "lazy default"));
        // orElseThrow — throw meaningful exception
        System.out.println("orElseThrow: "    + present.orElseThrow(() -> new RuntimeException("not found")));

        // orElse vs orElseGet difference:
        System.out.println("\n=== orElse vs orElseGet ===");
        // orElse: expensive object ALWAYS constructed (even if present)
        Optional<String> hasValue = Optional.of("value");
        String r1 = hasValue.orElse(expensiveDefault());       // expensiveDefault() CALLED even though present!
        String r2 = hasValue.orElseGet(() -> expensiveDefault()); // NOT called (lazy)
        System.out.println("Results: " + r1 + ", " + r2);

        System.out.println("\n=== map, flatMap, filter ===");
        // map: transform value if present
        Optional<Integer> len = present.map(String::length);
        System.out.println("map length: "  + len);  // Optional[5]

        // filter: keep value only if predicate true
        Optional<String> filtered = present.filter(s -> s.length() > 3);
        System.out.println("filter(>3): "  + filtered);  // Optional[Hello]

        Optional<String> notFiltered = present.filter(s -> s.length() > 10);
        System.out.println("filter(>10): " + notFiltered); // Optional.empty

        // flatMap: for methods that return Optional (avoids Optional<Optional<T>>)
        Optional<String> email = findById(1L).flatMap(u -> Optional.ofNullable(u.email()));
        System.out.println("flatMap email: " + email);

        System.out.println("\n=== Optional chain ===");
        System.out.println("Shipping city(1): " + getShippingCity(1L).orElse("Unknown"));
        System.out.println("Shipping city(2): " + getShippingCity(2L).orElse("Unknown"));

        System.out.println("\n=== ifPresent / ifPresentOrElse ===");
        findById(1L).ifPresent(u -> System.out.println("Found: " + u.name()));
        findById(99L).ifPresentOrElse(
            u -> System.out.println("Found: " + u.name()),
            () -> System.out.println("User 99 not found")
        );

        System.out.println("\n=== Optional ANTI-PATTERNS ===");
        /*
         * AVOID:
         *  if (optional.isPresent()) { optional.get() }  => use map/orElse instead
         *  Optional as method parameter               => use overloads or @Nullable
         *  Optional as field in class                 => serialization issues, add null check instead
         *  Optional<Collection<T>>                    => return empty collection instead
         *
         * PREFER:
         *  optional.map(...).orElse(default)
         *  optional.orElseGet(() -> compute())
         *  optional.orElseThrow(() -> new CustomException("..."))
         *  optional.ifPresentOrElse(consumer, runnable)
         */

        System.out.println("\n=== Java 14+ Records ===");
        // Records: immutable data carriers — auto-generates: constructor, accessors, equals, hashCode, toString
        record Point(int x, int y) {
            // Compact canonical constructor for validation
            Point {
                if (x < 0 || y < 0) throw new IllegalArgumentException("Coordinates must be non-negative");
            }
            // Can add custom methods
            double distanceFromOrigin() { return Math.sqrt(x * x + y * y); }
        }

        Point p1 = new Point(3, 4);
        Point p2 = new Point(3, 4);
        System.out.println("Point: " + p1);                // Point[x=3, y=4]
        System.out.println("x=" + p1.x() + " y=" + p1.y()); // accessor methods
        System.out.println("equals: " + p1.equals(p2));   // true — value-based equality
        System.out.println("distance: " + p1.distanceFromOrigin()); // 5.0

        System.out.println("\n=== Java 17 Sealed Classes ===");
        // Sealed classes restrict which classes can extend them
        // Enables exhaustive pattern matching in switch
        Shape shape = new Circle(5.0);
        // Java 21 switch with pattern matching (exhaustive — compiler enforces all cases)
        double area = switch (shape) {
            case Circle    c -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle  t -> 0.5 * t.base() * t.height();
        };
        System.out.println("Circle area: " + area);

        System.out.println("\n=== Switch Expressions (Java 14+) ===");
        int day = 3;
        String dayName = switch (day) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4, 5 -> "Thu/Fri";         // multiple labels
            default -> {
                System.out.println("Weekend");
                yield "Weekend";             // yield for block form
            }
        };
        System.out.println("Day: " + dayName);

        System.out.println("\n=== var (Java 10+) ===");
        var list = List.of(1, 2, 3);   // inferred as List<Integer>
        var map  = new java.util.HashMap<String, Integer>();
        map.put("a", 1);
        // var x;           // ERROR: must have initializer
        // var x = null;    // ERROR: cannot infer type from null
        System.out.println("var list: " + list);

        System.out.println("\n=== Text Blocks (Java 15+) ===");
        String json = """
                {
                    "name": "Alice",
                    "age": 30
                }
                """;
        System.out.println("Text block:\n" + json);
    }

    static String expensiveDefault() {
        System.out.print("[expensiveDefault called] ");
        return "expensive";
    }
}
