package lessons.oop_concepts;

/**
 * LESSON 01C — POLYMORPHISM
 *
 * Two Types:
 *  1. Compile-time (static)  — Method OVERLOADING, resolved by compiler based on parameter types
 *  2. Runtime (dynamic)      — Method OVERRIDING, resolved by JVM based on actual object type
 *
 * SDE2 Interview Questions:
 *  - "Can we overload methods that differ only in return type?" => NO, compile error (ambiguous call)
 *  - "What is covariant return type?" => Overriding method can return a subtype of parent's return type
 *  - "Can we achieve runtime polymorphism with static/private/final methods?" => NO
 *  - "What is dynamic method dispatch?" => JVM looks up the vtable at runtime for the actual type
 */
public class PolymorphismDemo {

    // ─── 1. COMPILE-TIME Polymorphism (Method Overloading) ──────────────────────
    static class Calculator {
        // Same method name, different parameter types/count
        int add(int a, int b)           { return a + b; }
        double add(double a, double b)  { return a + b; }
        int add(int a, int b, int c)    { return a + b + c; }
        String add(String a, String b)  { return a + b; }

        // ILLEGAL — cannot overload by return type alone
        // long add(int a, int b) { return a + b; }  // COMPILE ERROR

        // Autoboxing / widening priority in overloading resolution:
        // widening > boxing > varargs
        void test(long x)     { System.out.println("long: " + x); }
        void test(Integer x)  { System.out.println("Integer: " + x); }
        // int arg → prefers widening to long over boxing to Integer
    }

    // ─── 2. RUNTIME Polymorphism (Method Overriding) ────────────────────────────
    static abstract class Shape {
        abstract double area();

        // Template method pattern — uses runtime polymorphism internally
        void describe() {
            System.out.println(getClass().getSimpleName() + " with area = " + area());
        }
    }

    static class Circle extends Shape {
        double radius;
        Circle(double r) { this.radius = r; }

        @Override
        double area() { return Math.PI * radius * radius; }
    }

    static class Rectangle extends Shape {
        double width, height;
        Rectangle(double w, double h) { width = w; height = h; }

        @Override
        double area() { return width * height; }
    }

    static class Square extends Rectangle {
        Square(double side) { super(side, side); }

        // Covariant return type example: returning more specific type
        // If parent returned Shape, child could return Square
    }

    // ─── 3. Covariant Return Type ────────────────────────────────────────────────
    static class AnimalFactory {
        Animal create() { return new Animal("Generic"); }
    }

    static class DogFactory extends AnimalFactory {
        @Override
        Dog create() { return new Dog("Buddy"); }  // Covariant — more specific return type OK
    }

    static class Animal {
        String name; Animal(String n) { name = n; }
        void speak() { System.out.println(name); }
    }
    static class Dog extends Animal {
        Dog(String n) { super(n); }
        @Override void speak() { System.out.println(name + " barks"); }
    }

    // ─── 4. Overriding Rules Summary ────────────────────────────────────────────
    /*
     *  Rule                        | Detail
     * ─────────────────────────────|─────────────────────────────────────────────
     * Same method signature        | name + parameter list must match exactly
     * Return type                  | same OR covariant (subtype) — since Java 5
     * Access modifier              | can WIDEN (protected → public), cannot NARROW
     * Exception                    | can throw fewer/narrower checked exceptions
     * Cannot override              | static, private, final methods
     * @Override annotation         | best practice — compile error if not actually overriding
     */

    public static void main(String[] args) {
        System.out.println("=== Compile-time Polymorphism (Overloading) ===");
        Calculator calc = new Calculator();
        System.out.println(calc.add(2, 3));             // int version → 5
        System.out.println(calc.add(2.5, 3.5));         // double version → 6.0
        System.out.println(calc.add(1, 2, 3));          // 3-arg version → 6
        System.out.println(calc.add("Hello", "World")); // String version → HelloWorld
        calc.test(10);  // widening wins: long version called even though int fits Integer too

        System.out.println("\n=== Runtime Polymorphism (Overriding) ===");
        Shape[] shapes = { new Circle(5), new Rectangle(4, 6), new Square(3) };
        for (Shape s : shapes) {
            s.describe();   // JVM calls the correct area() at runtime via vtable
        }

        System.out.println("\n=== Covariant Return Type ===");
        AnimalFactory factory = new DogFactory();
        Animal a = factory.create();   // returns Dog, stored as Animal — polymorphism!
        a.speak();                     // "Buddy barks" — runtime dispatch

        System.out.println("\n=== Dynamic Dispatch ===");
        // Reference type = Animal, actual object type = Dog
        Animal ref = new Dog("Max");
        ref.speak();   // Calls Dog.speak(), NOT Animal.speak()
        // ref.fetch(); // COMPILE ERROR — Animal ref doesn't know about Dog.fetch()
    }
}
