package lessons.string_handling;

import java.util.*;
import java.util.regex.*;

/**
 * LESSON 09 — String Handling (SDE2 Interview)
 *
 * Key Topics:
 *  1. String immutability
 *  2. String pool (interning)
 *  3. StringBuilder vs StringBuffer vs String concatenation
 *  4. String methods (charAt, indexOf, substring, split, replace, matches)
 *  5. equals vs == for Strings
 *  6. String.format vs formatted()
 *  7. Regular expressions
 *  8. Interview coding patterns
 *
 * SDE2 Interview Questions:
 *  - "Why is String immutable in Java?"
 *    => Security (class loading, network), thread safety, hashCode caching, String pool feasibility
 *  - "String vs StringBuilder vs StringBuffer?"
 *    => String: immutable. StringBuilder: mutable, NOT thread-safe, faster. StringBuffer: mutable, synchronized (thread-safe), slower.
 *  - "What is the String pool?"
 *    => Special area in heap; string literals are deduplicated. new String("hi") bypasses the pool.
 *  - "When is == true for strings?"
 *    => Only when both references point to same object (same pool literal or same new String reference)
 */
public class StringHandlingDemo {

    public static void main(String[] args) {

        // ── 1. Immutability + String Pool ────────────────────────────────────────
        System.out.println("=== String Pool and ==  ===");
        String a = "hello";           // String pool
        String b = "hello";           // same pool object
        String c = new String("hello"); // new heap object — NOT from pool
        String d = c.intern();        // explicitly add to/get from pool

        System.out.println("a == b:        " + (a == b));   // true  — same pool object
        System.out.println("a == c:        " + (a == c));   // false — different objects
        System.out.println("a == d:        " + (a == d));   // true  — d is interned
        System.out.println("a.equals(c):   " + a.equals(c)); // true  — same content
        System.out.println("ALWAYS use equals() for String comparison!");

        // ── 2. String is immutable ─────────────────────────────────────────────
        System.out.println("\n=== Immutability ===");
        String s = "Hello";
        String s2 = s.toUpperCase();  // creates NEW String, s is unchanged
        System.out.println("s: " + s + " (unchanged)");
        System.out.println("s2: " + s2 + " (new string)");
        // s += " World";  // creates new String objects on heap: "Hello" + " World" = "Hello World"

        // ── 3. StringBuilder (mutable, not thread-safe) ──────────────────────────
        System.out.println("\n=== StringBuilder ===");
        StringBuilder sb = new StringBuilder();
        sb.append("Hello")
          .append(", ")
          .append("World")
          .append("!")
          .insert(5, " Beautiful");  // insert at index
        System.out.println("Built: " + sb);
        sb.reverse();
        System.out.println("Reversed: " + sb);
        sb.delete(0, 7);
        System.out.println("After delete(0,7): " + sb);
        System.out.println("Capacity: " + sb.capacity()); // default 16, grows as needed
        System.out.println("Length: "   + sb.length());

        // Performance: String concat in loops creates many objects
        System.out.println("\n=== String concat vs StringBuilder performance ===");
        long start = System.nanoTime();
        String result = "";
        for (int i = 0; i < 1000; i++) result += i; // BAD: O(n²) — creates 1000 intermediate Strings
        System.out.println("String concat (1000 iters): " + (System.nanoTime() - start) / 1_000_000 + "ms");

        start = System.nanoTime();
        StringBuilder sbPerf = new StringBuilder();
        for (int i = 0; i < 1000; i++) sbPerf.append(i);  // GOOD: O(n) — single buffer
        System.out.println("StringBuilder (1000 iters): " + (System.nanoTime() - start) / 1_000_000 + "ms");

        // Note: Java compiler auto-optimizes simple + in single expression, but NOT in loops

        // ── 4. Core String Methods ────────────────────────────────────────────────
        System.out.println("\n=== Core String Methods ===");
        String str = "  Hello, World!  ";
        System.out.println("trim:         '" + str.trim() + "'");  // trims whitespace
        System.out.println("strip:        '" + str.strip() + "'"); // Java 11+, Unicode-aware
        System.out.println("stripLeading: '" + str.stripLeading() + "'");
        System.out.println("stripTrailing:'" + str.stripTrailing() + "'");
        System.out.println("isBlank:       " + "   ".isBlank());   // Java 11+: true if empty/whitespace

        String clean = str.trim();
        System.out.println("length:  " + clean.length());
        System.out.println("charAt:  " + clean.charAt(0));         // 'H'
        System.out.println("indexOf: " + clean.indexOf('o'));       // 4
        System.out.println("lastIndexOf: " + clean.lastIndexOf('o')); // 8
        System.out.println("substring(7):   '" + clean.substring(7) + "'");    // "World!"
        System.out.println("substring(7,12):'" + clean.substring(7, 12) + "'"); // "World"
        System.out.println("contains:  " + clean.contains("World"));
        System.out.println("startsWith:" + clean.startsWith("Hello"));
        System.out.println("endsWith:  " + clean.endsWith("!"));
        System.out.println("replace:   " + clean.replace("World", "Java"));
        System.out.println("replaceAll:" + clean.replaceAll("[aeiou]", "*")); // regex replace

        // ── 5. split ─────────────────────────────────────────────────────────────
        System.out.println("\n=== split ===");
        String csv = "Alice,Bob,,Charlie,Dave";
        String[] parts = csv.split(",");         // default: removes trailing empty strings
        System.out.println("Parts: " + Arrays.toString(parts) + " length=" + parts.length);

        String[] allParts = csv.split(",", -1);  // -1 limit: keeps trailing empty strings
        System.out.println("All parts: " + Arrays.toString(allParts) + " length=" + allParts.length);

        // join (reverse of split)
        System.out.println("Join: " + String.join(" | ", "Alice", "Bob", "Charlie"));
        System.out.println("Join list: " + String.join(", ", Arrays.asList("X", "Y", "Z")));

        // ── 6. String.format and formatted ───────────────────────────────────────
        System.out.println("\n=== String formatting ===");
        String formatted = String.format("Name: %-10s Age: %3d GPA: %.2f", "Alice", 30, 3.987);
        System.out.println(formatted);
        // Java 15+ instance method:
        String fmt = "Hello, %s! You have %d messages.".formatted("Alice", 5);
        System.out.println(fmt);

        // ── 7. Regular Expressions ───────────────────────────────────────────────
        System.out.println("\n=== Regular Expressions ===");
        // matches: entire string must match
        System.out.println("Email valid: " + "test@example.com".matches("[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}"));
        System.out.println("Number: "      + "12345".matches("\\d+"));

        // Pattern + Matcher for groups / find
        Pattern emailPattern = Pattern.compile("([\\w.%+-]+)@([\\w.-]+)\\.([a-zA-Z]{2,})");
        Matcher m = emailPattern.matcher("Contact alice@example.com or bob@test.org for info");
        while (m.find()) {
            System.out.println("Found email: " + m.group() + " (user=" + m.group(1) + ", domain=" + m.group(2) + ")");
        }

        // replaceAll with regex
        String cleaned = "He11o W0r1d".replaceAll("[^a-zA-Z]", "");
        System.out.println("Remove non-letters: " + cleaned);

        // ── 8. Common Interview Coding Patterns ───────────────────────────────────
        System.out.println("\n=== Interview: Reverse a String ===");
        System.out.println(new StringBuilder("Hello").reverse());

        System.out.println("\n=== Interview: Check Palindrome ===");
        System.out.println(isPalindrome("racecar"));  // true
        System.out.println(isPalindrome("hello"));    // false

        System.out.println("\n=== Interview: Count character frequency ===");
        charFrequency("programming");

        System.out.println("\n=== Interview: First non-repeating character ===");
        System.out.println("First non-repeating in 'programming': " + firstNonRepeating("programming")); // 'p'... actually let's trace

        System.out.println("\n=== Interview: Anagram check ===");
        System.out.println("listen/silent anagram: " + areAnagrams("listen", "silent")); // true

        System.out.println("\n=== Interview: String compression ===");
        System.out.println(compress("aabcccdddd")); // a2b1c3d4
    }

    static boolean isPalindrome(String s) {
        int left = 0, right = s.length() - 1;
        while (left < right) {
            if (s.charAt(left++) != s.charAt(right--)) return false;
        }
        return true;
    }

    static void charFrequency(String s) {
        Map<Character, Integer> freq = new LinkedHashMap<>();
        for (char c : s.toCharArray()) freq.merge(c, 1, Integer::sum);
        System.out.println("Frequency of '" + s + "': " + freq);
    }

    static char firstNonRepeating(String s) {
        Map<Character, Integer> freq = new LinkedHashMap<>(); // preserves insertion order
        for (char c : s.toCharArray()) freq.merge(c, 1, Integer::sum);
        for (Map.Entry<Character, Integer> e : freq.entrySet())
            if (e.getValue() == 1) return e.getKey();
        return '\0';
    }

    static boolean areAnagrams(String a, String b) {
        if (a.length() != b.length()) return false;
        int[] count = new int[26];
        for (char c : a.toCharArray()) count[c - 'a']++;
        for (char c : b.toCharArray()) count[c - 'a']--;
        for (int n : count) if (n != 0) return false;
        return true;
    }

    static String compress(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        int count = 1;
        for (int i = 1; i <= s.length(); i++) {
            if (i < s.length() && s.charAt(i) == s.charAt(i - 1)) {
                count++;
            } else {
                sb.append(s.charAt(i - 1)).append(count);
                count = 1;
            }
        }
        return sb.length() < s.length() ? sb.toString() : s;
    }
}
