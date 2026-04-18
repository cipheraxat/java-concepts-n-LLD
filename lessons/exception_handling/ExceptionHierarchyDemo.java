package lessons.exception_handling;

import java.io.*;
import java.sql.SQLException;

/**
 * LESSON 04A — Exception Hierarchy and Core Mechanics
 *
 * SDE2 Interview Questions:
 *  - "What is the difference between throw and throws?"
 *    => throw: used to actually throw an exception; throws: declares what method may throw (checked)
 *  - "Can we have try without catch?" => YES, if there is a finally block (try-finally)
 *  - "What happens when exception is thrown in finally?" => It replaces the original exception!
 *  - "Order of catch blocks?" => Must go from most specific to least specific (subclass before superclass)
 */
public class  ExceptionHierarchyDemo {

    // ─── Checked Exception: must be declared or handled ─────────────────────────
    static void readFile(String path) throws IOException {
        // IOException is checked — compiler requires you to handle or declare it
        throw new IOException("File not found: " + path);
    }

    // ─── Unchecked Exception: RuntimeException — no declaration needed ───────────
    static int divide(int a, int b) {
        if (b == 0) throw new ArithmeticException("Cannot divide by zero");  // unchecked
        return a / b;
    }

    // ─── try-catch-finally execution order ──────────────────────────────────────
    static String tryCatchFinallyOrder() {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("try ");
            if (true) throw new RuntimeException("test");
            sb.append("try-after-throw ");  // never reached
        } catch (RuntimeException e) {
            sb.append("catch ");
        } finally {
            sb.append("finally");  // ALWAYS runs (except System.exit / JVM crash)
        }
        return sb.toString();  // "try catch finally"
    }

    // ─── finally return OVERRIDES try return (avoid this!) ──────────────────────
    static int dangerousFinally() {
        try {
            return 1;   // would return 1...
        } finally {
            return 2;   // ...but finally overrides! returns 2. Also suppresses any exception!
        }
    }

    // ─── Multi-catch (Java 7+) ───────────────────────────────────────────────────
    static void multiCatch(int choice) {
        try {
            switch (choice) {
                case 1 -> throw new IOException("IO error");
                case 2 -> throw new SQLException("SQL error");
                case 3 -> throw new ArithmeticException("Arithmetic error");
                // case 4 → throw new RuntimeException("too broad");  // could but loses specificity
            }
        } catch (IOException | SQLException e) {    // multi-catch — e is effectively final
            System.out.println("IO or SQL: " + e.getMessage());
            // e = new IOException("...");  // COMPILE ERROR — effectively final
        } catch (ArithmeticException e) {
            System.out.println("Arithmetic: " + e.getMessage());
        } catch (Exception e) {          // MUST come after more specific catches
            System.out.println("Generic: " + e.getMessage());
        }
    }

    // ─── Exception catch order (most specific first) ─────────────────────────────
    static void catchOrder() {
        try {
            // Demonstrate both branches reachable
            if (System.nanoTime() % 2 == 0) {
                throw new FileNotFoundException("file.txt not found");
            } else {
                throw new IOException("generic IO error");
            }
        } catch (FileNotFoundException e) {   // More specific — FileNotFoundException extends IOException
            System.out.println("FileNotFound: " + e.getMessage());
        } catch (IOException e) {             // Less specific — must come AFTER
            System.out.println("IO: " + e.getMessage());
        }
        // catch (IOException e) { ... }
        // catch (FileNotFoundException e) { ... }   // COMPILE ERROR — unreachable catch
    }

    // ─── Re-throwing and wrapping ────────────────────────────────────────────────
    static void rethrowExample() throws Exception {
        try {
            readFile("nonexistent.txt");
        } catch (IOException e) {
            System.out.println("Logging: " + e.getMessage());
            throw e;  // re-throw same exception after logging
        }
    }

    static void wrapExample() throws RuntimeException {
        try {
            readFile("nonexistent.txt");
        } catch (IOException e) {
            // Wrap checked → unchecked, preserving cause chain
            throw new RuntimeException("Failed to read config file", e);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== try-catch-finally Order ===");
        System.out.println(tryCatchFinallyOrder());  // try catch finally

        System.out.println("\n=== Dangerous finally return ===");
        System.out.println(dangerousFinally());   // 2 — finally overrides try's return

        System.out.println("\n=== Multi-catch ===");
        multiCatch(1); multiCatch(2); multiCatch(3);

        System.out.println("\n=== Catch Order ===");
        catchOrder();

        System.out.println("\n=== Checked Exception ===");
        try {
            readFile("test.txt");
        } catch (IOException e) {
            System.out.println("Caught checked: " + e.getMessage());
        }

        System.out.println("\n=== Unchecked Exception ===");
        try {
            divide(10, 0);
        } catch (ArithmeticException e) {
            System.out.println("Caught unchecked: " + e.getMessage());
        }

        System.out.println("\n=== Re-throw / Wrap ===");
        try {
            wrapExample();
        } catch (RuntimeException e) {
            System.out.println("Wrapped: " + e.getMessage());
            System.out.println("Cause: " + e.getCause().getMessage()); // original IOException
        }

        System.out.println("\n=== Common RuntimeExceptions ===");
        // NullPointerException
        try { String s = null; s.length(); } catch (NullPointerException e) { System.out.println("NPE"); }
        // ArrayIndexOutOfBoundsException
        try { int[] a = new int[3]; int x = a[5]; } catch (ArrayIndexOutOfBoundsException e) { System.out.println("AIOOBE"); }
        // ClassCastException
        try { Object o = "hello"; Integer i = (Integer) o; } catch (ClassCastException e) { System.out.println("CCE"); }
        // NumberFormatException
        try { int n = Integer.parseInt("abc"); } catch (NumberFormatException e) { System.out.println("NFE"); }
        // StackOverflowError (Error, not Exception)
        try { callRecursive(); } catch (StackOverflowError e) { System.out.println("StackOverflow"); }
    }

    static void callRecursive() { callRecursive(); }
}
