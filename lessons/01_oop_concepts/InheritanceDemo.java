package lessons.oop_concepts;

/**
 * LESSON 01B — INHERITANCE
 *
 * Inheritance = a child class acquires all non-private members of a parent class.
 * Java supports: Single, Multi-level, Hierarchical  (NOT multiple via classes — use interfaces for that)
 *
 * SDE2 Interview Questions:
 *  - "Why doesn't Java support multiple inheritance with classes?"
 *    => Diamond problem — ambiguous method resolution. Solved via interfaces.
 *  - "What is the difference between method hiding and method overriding?"
 *    => Static methods are hidden (resolved at compile time by reference type),
 *       instance methods are overridden (resolved at runtime by actual object type).
 *  - "Can a subclass constructor skip calling super()?"
 *    => Compiler auto-inserts super() if no explicit call. If parent has no no-arg constructor,
 *       you MUST explicitly call super(args).
 */
public class InheritanceDemo {

    // ─── Base Class ──────────────────────────────────────────────────────────────
    static class Animal {
        String name;

        Animal(String name) { this.name = name; }

        void speak() { System.out.println(name + " makes a sound"); }

        // Static method — will be HIDDEN (not overridden) in subclass
        static String kingdom() { return "Animalia"; }

        @Override
        public String toString() { return "Animal(" + name + ")"; }
    }

    // ─── Single Inheritance ──────────────────────────────────────────────────────
    static class Dog extends Animal {
        String breed;

        Dog(String name, String breed) {
            super(name);               // Must call parent constructor
            this.breed = breed;
        }

        @Override                      // Runtime polymorphism
        void speak() { System.out.println(name + " barks: Woof!"); }

        // Method HIDING — static methods are resolved by reference type, not object type
        static String kingdom() { return "Animalia [Dog override - this is HIDING, not overriding]"; }

        @Override
        public String toString() { return "Dog(" + name + ", breed=" + breed + ")"; }
    }

    // ─── Multi-level Inheritance ─────────────────────────────────────────────────
    static class GoldenRetriever extends Dog {
        GoldenRetriever(String name) {
            super(name, "Golden Retriever");
        }

        @Override
        void speak() {
            super.speak();             // Call immediate parent's method
            System.out.println(name + " also wags its tail!");
        }
    }

    // ─── Hierarchical Inheritance ────────────────────────────────────────────────
    static class Cat extends Animal {
        Cat(String name) { super(name); }

        @Override
        void speak() { System.out.println(name + " meows: Meow!"); }
    }

    // ─── Constructor Chaining Demo ───────────────────────────────────────────────
    static class Vehicle {
        String type;
        Vehicle(String type) {
            this.type = type;
            System.out.println("Vehicle constructor: " + type);
        }
    }

    static class Car extends Vehicle {
        int seats;
        Car(String type, int seats) {
            super(type);               // Parent constructor called first
            this.seats = seats;
            System.out.println("Car constructor: " + seats + " seats");
        }
    }

    static class ElectricCar extends Car {
        int range;
        ElectricCar(int seats, int range) {
            super("Electric", seats); // Calls Car → Vehicle chain
            this.range = range;
            System.out.println("ElectricCar constructor: range=" + range + "km");
        }
    }

    public static void main(String[] args) {
        System.out.println("=== Single Inheritance ===");
        Dog dog = new Dog("Rex", "Labrador");
        dog.speak();           // Rex barks: Woof!

        System.out.println("\n=== Multi-level Inheritance ===");
        GoldenRetriever golden = new GoldenRetriever("Buddy");
        golden.speak();

        System.out.println("\n=== Hierarchical Inheritance ===");
        Animal[] animals = { new Dog("Rex", "Lab"), new Cat("Whiskers"), golden };
        for (Animal a : animals) a.speak();   // Runtime polymorphism

        System.out.println("\n=== Method Hiding vs Overriding ===");
        Animal ref = new Dog("Rex", "Lab");
        ref.speak();                           // Dog.speak() — runtime dispatch (OVERRIDING)
        System.out.println(Animal.kingdom());  // Animalia (called on Animal reference → Animal's static)
        System.out.println(Dog.kingdom());     // Dog's static (HIDING)
        // IMPORTANT: ref.kingdom() would call Animal.kingdom() because ref is type Animal

        System.out.println("\n=== Constructor Chaining ===");
        ElectricCar tesla = new ElectricCar(5, 450);
        // Output order: Vehicle → Car → ElectricCar
    }
}
