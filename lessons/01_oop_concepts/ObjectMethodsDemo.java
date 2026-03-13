package lessons.oop_concepts;

import java.util.Objects;

/**
 * LESSON 01E — Object Class Methods (equals, hashCode, toString, clone)
 *
 * Every Java class implicitly extends Object. SDE2 must know these contracts cold.
 *
 * SDE2 Interview Questions:
 *  - "What happens if you override equals() but not hashCode()?"
 *    => Objects that are logically equal may have different hash codes →
 *       HashMap/HashSet will fail to find them (put in different buckets)
 *  - "What is the equals-hashCode contract?"
 *    => If a.equals(b) then a.hashCode() == b.hashCode() (converse NOT required)
 *  - "What is deep clone vs shallow clone?"
 *    => Shallow: copies references. Deep: recursively copies referenced objects.
 *  - "What does == do for objects?" => Compares references (memory addresses), NOT values
 */
public class ObjectMethodsDemo {

    // ─── Correct equals + hashCode implementation ────────────────────────────────
    static class Employee {
        private final int id;
        private final String name;
        private final String department;

        Employee(int id, String name, String department) {
            this.id = id;
            this.name = name;
            this.department = department;
        }

        // equals contract: reflexive, symmetric, transitive, consistent, null-safe
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;           // Same reference
            if (!(o instanceof Employee)) return false; // null-safe + type check
            Employee e = (Employee) o;
            return id == e.id && Objects.equals(name, e.name);  // business key: id + name
            // Note: intentionally ignoring department in equality
        }

        // hashCode contract: same fields used as in equals
        @Override
        public int hashCode() {
            return Objects.hash(id, name);  // Uses same fields as equals!
        }

        @Override
        public String toString() {
            return "Employee{id=" + id + ", name='" + name + "', dept='" + department + "'}";
        }

        // ── Shallow vs Deep Clone ────────────────────────────────────────────────
        static class Address implements Cloneable {
            String city;
            Address(String city) { this.city = city; }

            @Override
            public Address clone() {
                try { return (Address) super.clone(); }  // shallow copy of Address
                catch (CloneNotSupportedException e) { throw new AssertionError(); }
            }
        }

        static class Person implements Cloneable {
            String name;
            Address address;  // mutable reference

            Person(String name, Address address) { this.name = name; this.address = address; }

            // SHALLOW CLONE — address field shares same reference
            public Person shallowClone() {
                try { return (Person) super.clone(); }
                catch (CloneNotSupportedException e) { throw new AssertionError(); }
            }

            // DEEP CLONE — address is also cloned
            public Person deepClone() {
                try {
                    Person copy = (Person) super.clone();
                    copy.address = this.address.clone();  // clone the mutable field too
                    return copy;
                } catch (CloneNotSupportedException e) { throw new AssertionError(); }
            }

            @Override public String toString() { return name + " @ " + address.city; }
        }
    }

    public static void main(String[] args) {
        System.out.println("=== == vs equals ===");
        String s1 = new String("hello");
        String s2 = new String("hello");
        System.out.println(s1 == s2);       // false — different objects in heap
        System.out.println(s1.equals(s2));  // true  — same character sequence

        System.out.println("\n=== equals + hashCode contract ===");
        Employee e1 = new Employee(1, "Alice", "Engineering");
        Employee e2 = new Employee(1, "Alice", "Marketing");  // same id+name, different dept
        System.out.println("e1.equals(e2): " + e1.equals(e2));         // true (same id+name)
        System.out.println("Same hashCode: " + (e1.hashCode() == e2.hashCode())); // true — contract met!

        java.util.Set<Employee> set = new java.util.HashSet<>();
        set.add(e1);
        System.out.println("Set contains e2: " + set.contains(e2)); // true — hashCode+equals work

        System.out.println("\n=== toString ===");
        System.out.println(e1);   // calls toString()

        System.out.println("\n=== Shallow vs Deep Clone ===");
        Employee.Address addr = new Employee.Address("New York");
        Employee.Person original = new Employee.Person("Bob", addr);
        Employee.Person shallow  = original.shallowClone();
        Employee.Person deep     = original.deepClone();

        original.address.city = "San Francisco";  // mutate shared address

        System.out.println("Original: " + original);  // San Francisco
        System.out.println("Shallow:  " + shallow);   // San Francisco — shared reference!
        System.out.println("Deep:     " + deep);      // New York — independent copy

        System.out.println("\n=== Why clone() is problematic ===");
        /*
         * Effective Java (Bloch) advises AGAINST Cloneable:
         *  - Broken contract: super.clone() can return wrong type without covariant override
         *  - Fragile: must manually deep-clone all mutable fields
         * Preferred: copy constructor or static factory method
         */
        Employee e3 = new Employee(e1.id, e1.name, e1.department); // copy constructor pattern
        System.out.println("Copy: " + e3);
    }
}
