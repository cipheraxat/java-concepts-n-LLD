package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02C — Comparable vs Comparator + Set Implementations
 *
 * Comparable (java.lang): natural ordering — the class defines its own comparison
 * Comparator (java.util): external ordering — separation of sort strategy from class
 *
 * SDE2 Interview Questions:
 *  - "Comparable vs Comparator?" => Comparable: implemented in the class (compareTo),
 *    Comparator: separate class/lambda (compare) — allows multiple sort strategies
 *  - "HashSet vs TreeSet vs LinkedHashSet?"
 *    => HashSet: O(1) ops, unordered; TreeSet: O(log n), sorted; LinkedHashSet: O(1), insertion-ordered
 *  - "What if you put mutable objects in a HashSet and then mutate them?" => hash changes, object lost!
 */
public class ComparatorComparableDemo {

    // ─── Comparable: natural ordering inside the class ───────────────────────────
    static class Student implements Comparable<Student> {
        String name;
        int grade;
        double gpa;

        Student(String name, int grade, double gpa) {
            this.name = name; this.grade = grade; this.gpa = gpa;
        }

        // Natural order: by grade ascending, then name alphabetically
        @Override
        public int compareTo(Student other) {
            if (this.grade != other.grade) return Integer.compare(this.grade, other.grade);
            return this.name.compareTo(other.name);
        }

        double getGpa() { return gpa; }

        @Override public String toString() { return name + "(grade=" + grade + ",gpa=" + gpa + ")"; }
    }

    public static void main(String[] args) {

        // ── Comparable (natural ordering) ────────────────────────────────────────
        System.out.println("=== Comparable (natural ordering) ===");
        List<Student> students = new ArrayList<>(Arrays.asList(
            new Student("Charlie", 11, 3.5),
            new Student("Alice", 10, 3.9),
            new Student("Bob", 11, 3.7),
            new Student("Dave", 10, 3.6)
        ));
        Collections.sort(students);  // uses compareTo
        students.forEach(System.out::println);

        // ── Comparator (multiple sort strategies) ────────────────────────────────
        System.out.println("\n=== Comparator (external ordering) ===");

        // By GPA descending
        Comparator<Student> byGpaDesc = Comparator.comparingDouble(Student::getGpa).reversed();

        // By name, then gpa desc — chained comparators
        Comparator<Student> byNameThenGpa = Comparator.comparing((Student s) -> s.name)
                                                       .thenComparingDouble(s -> s.getGpa());

        // By grade ascending, then GPA descending
        Comparator<Student> byGradeAscGpaDes = Comparator.comparingInt((Student s) -> s.grade)
                                                          .thenComparingDouble(s -> -s.gpa);

        List<Student> byGpa = new ArrayList<>(students);
        byGpa.sort(byGpaDesc);
        System.out.println("By GPA desc: " + byGpa);

        List<Student> complex = new ArrayList<>(students);
        complex.sort(byGradeAscGpaDes);
        System.out.println("By grade asc, gpa desc: " + complex);

        // ── TreeSet with custom Comparator ────────────────────────────────────────
        System.out.println("\n=== TreeSet with Comparator ===");
        TreeSet<Student> sortedByGpa = new TreeSet<>(Comparator.comparingDouble(Student::getGpa).reversed()
                                                                .thenComparing(s -> s.name));
        sortedByGpa.addAll(students);
        System.out.println("TreeSet by GPA desc: " + sortedByGpa);

        // ── Set Implementations ───────────────────────────────────────────────────
        System.out.println("\n=== HashSet (unordered, O(1)) ===");
        Set<String> hashSet = new HashSet<>(Arrays.asList("Banana", "Apple", "Cherry", "Apple"));
        System.out.println(hashSet);  // no duplicates, order not guaranteed

        System.out.println("\n=== LinkedHashSet (insertion order) ===");
        Set<String> linkedSet = new LinkedHashSet<>(Arrays.asList("Banana", "Apple", "Cherry", "Apple"));
        System.out.println(linkedSet);  // [Banana, Apple, Cherry] — insertion order, no duplicates

        System.out.println("\n=== TreeSet (sorted) ===");
        Set<String> treeSet = new TreeSet<>(Arrays.asList("Banana", "Apple", "Cherry", "Apple"));
        System.out.println(treeSet);   // [Apple, Banana, Cherry] — sorted alphabetically

        // TreeSet NavigableSet operations
        System.out.println("First: " + ((TreeSet<String>) treeSet).first());
        System.out.println("Higher than Apple: " + ((TreeSet<String>) treeSet).higher("Apple"));
        System.out.println("Floor of 'B': " + ((TreeSet<String>) treeSet).floor("B")); // Banana

        // ── Set operations ────────────────────────────────────────────────────────
        System.out.println("\n=== Set Operations (union, intersection, difference) ===");
        Set<Integer> a = new HashSet<>(Set.of(1, 2, 3, 4));
        Set<Integer> b = new HashSet<>(Set.of(3, 4, 5, 6));

        Set<Integer> union = new HashSet<>(a); union.addAll(b);
        System.out.println("Union: " + union);         // [1,2,3,4,5,6]

        Set<Integer> intersection = new HashSet<>(a); intersection.retainAll(b);
        System.out.println("Intersection: " + intersection); // [3,4]

        Set<Integer> difference = new HashSet<>(a); difference.removeAll(b);
        System.out.println("A - B: " + difference);   // [1,2]

        // ── Null handling ─────────────────────────────────────────────────────────
        System.out.println("\n=== Null handling ===");
        Set<String> hashSetWithNull = new HashSet<>();
        hashSetWithNull.add(null);     // HashSet: allows ONE null
        // TreeSet doesn't allow null (NullPointerException on compareTo)

        // ── EnumSet — most efficient Set for enum types ───────────────────────────
        System.out.println("\n=== EnumSet ===");
        enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }
        EnumSet<Day> weekdays = EnumSet.range(Day.MON, Day.FRI);
        EnumSet<Day> weekend  = EnumSet.of(Day.SAT, Day.SUN);
        System.out.println("Weekdays: " + weekdays);
        System.out.println("Weekend: " + weekend);
    }
}
