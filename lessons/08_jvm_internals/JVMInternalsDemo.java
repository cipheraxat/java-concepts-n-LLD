package lessons.jvm_internals;

/**
 * LESSON 08 — JVM Internals (SDE2 Interview)
 *
 * SDE2 Interview Questions (conceptual — no runnable code for all):
 *  - "What are the JVM memory areas?"
 *  - "What is the difference between Heap and Stack?"
 *  - "How does Garbage Collection work?"
 *  - "What is the difference between minor GC and major GC?"
 *  - "What is a ClassLoader?"
 *  - "What is the Java Memory Model (JMM)?"
 *  - "What is JIT compilation?"
 */
public class JVMInternalsDemo {

    /*
     * ═══════════════════════════════════════════════════════════════════════════
     *  JVM MEMORY AREAS
     * ═══════════════════════════════════════════════════════════════════════════
     *
     *  1. HEAP (shared across threads)
     *     ├── Young Generation
     *     │   ├── Eden Space     — new objects allocated here
     *     │   ├── Survivor S0    — after first GC that survives
     *     │   └── Survivor S1    — second pass
     *     └── Old Generation (Tenured) — long-lived objects promoted here
     *
     *  2. METHOD AREA / METASPACE (Java 8+, shared)
     *     — class metadata, static variables, runtime constant pool
     *     — PermGen in Java 7 and before (fixed size, often caused OutOfMemoryError)
     *     — Metaspace in Java 8+ (uses native memory, auto-grows, configurable max)
     *
     *  3. STACK (per thread)
     *     — one stack frame per method call
     *     — each frame: local variables, operand stack, return address
     *     — StackOverflowError if too deep (default ~512 frames)
     *
     *  4. PC REGISTER (per thread)
     *     — address of currently executing JVM instruction
     *
     *  5. NATIVE METHOD STACK (per thread)
     *     — for native (JNI) method calls
     *
     * ═══════════════════════════════════════════════════════════════════════════
     *  GARBAGE COLLECTION
     * ═══════════════════════════════════════════════════════════════════════════
     *
     *  HOW GC WORKS:
     *  1. Mark phase: identify all objects reachable from GC roots
     *     (GC roots: local variables in active threads, static fields, JNI references)
     *  2. Sweep phase: reclaim memory of unreachable objects
     *  3. Compact phase: defragment memory (not all collectors do this)
     *
     *  GENERATIONAL HYPOTHESIS:
     *  "Most objects die young" → collect young gen frequently (fast), old gen occasionally (slow)
     *
     *  MINOR GC: CollectS Young Generation
     *   - Eden → survivors (objects that survive)
     *   - Stop-The-World (short pause, typically < 100ms)
     *   - Objects that survive enough minor GCs get promoted to Old Gen
     *
     *  MAJOR GC (Full GC): Collects Old Generation
     *   - Much longer pause (seconds for large heaps with Serial/Parallel GC)
     *   - Usually also includes Young Gen
     *
     *  GC COLLECTORS:
     *  ┌─────────────────────────────────────────────────────────────────────────┐
     *  │ Collector      │ Default      │ Characteristics                         │
     *  ├─────────────────────────────────────────────────────────────────────────┤
     *  │ Serial GC      │ Small heaps  │ Single thread, simple, high pause      │
     *  │ Parallel GC    │ Java 8 def   │ Multi-thread, throughput-focused        │
     *  │ CMS            │ (deprecated) │ Concurrent, low-pause, fragmentation    │
     *  │ G1 GC          │ Java 9+ def  │ Region-based, predictable pause goals  │
     *  │ ZGC            │ Java 15+     │ Sub-millisecond pauses, scalable        │
     *  │ Shenandoah     │ Java 12+     │ Concurrent compaction, low latency      │
     *  └─────────────────────────────────────────────────────────────────────────┘
     *
     * ═══════════════════════════════════════════════════════════════════════════
     *  CLASS LOADING
     * ═══════════════════════════════════════════════════════════════════════════
     *
     *  Class Loaders (parent delegation model):
     *  Bootstrap ClassLoader → Extension (Platform) ClassLoader → Application ClassLoader
     *
     *  Steps:
     *  1. Loading    — reads .class bytes from source (file, jar, network)
     *  2. Linking
     *     a. Verification  — verify bytecode is valid and safe
     *     b. Preparation   — allocate memory for static fields, set to defaults
     *     c. Resolution    — resolve symbolic references to actual references
     *  3. Initialization — run <clinit> static initializer blocks
     *
     *  Class is initialized ONCE (thread-safe per JLS). This is why
     *  Initialization-on-Demand Holder singleton is safe.
     *
     * ═══════════════════════════════════════════════════════════════════════════
     *  JIT COMPILATION
     * ═══════════════════════════════════════════════════════════════════════════
     *
     *  Interpreter: executes bytecode line by line (slow for hot code)
     *  JIT: identifies "hot" code (methods/loops called many times) and compiles to native machine code
     *  Tiered Compilation (Java 8+):
     *    Level 0: Interpreter
     *    Level 1-3: C1 compiler (fast, less optimized)
     *    Level 4: C2 compiler (slow, heavily optimized)
     *
     *  Common JIT Optimizations:
     *  - Inlining: replace method call with method body (biggest impact)
     *  - Dead code elimination
     *  - Escape analysis: if object doesn't escape method → stack allocate (no GC)
     *  - Loop unrolling
     *  - Devirtualization: if only one implementation of interface → direct call
     *
     * ═══════════════════════════════════════════════════════════════════════════
     *  JAVA MEMORY MODEL (JMM)
     * ═══════════════════════════════════════════════════════════════════════════
     *
     *  Problem: CPU caches and instruction reordering make memory updates non-visible
     *  Solution: JMM defines when writes become visible to other threads
     *
     *  Happens-Before rules:
     *  - Monitor unlock happens-before subsequent lock on same monitor
     *  - volatile write happens-before subsequent volatile read
     *  - Thread.start() happens-before any action in started thread
     *  - Thread's last action happens-before Thread.join() returns
     *  - Object construction happens-before finalizer
     *
     *  volatile: guarantees visibility (no caching), prevents reordering around the access
     *  synchronized: guarantees visibility + atomicity (mutual exclusion on monitor)
     */

    // ─── Demonstrable GC behavior ────────────────────────────────────────────────

    static class HeavyObject {
        byte[] data = new byte[1024 * 1024]; // 1MB

        @Override
        protected void finalize() {
            // NOTE: finalize() is deprecated in Java 9+
            // Use Cleaner API (Java 9+) or try-with-resources instead
        }
    }

    static void gcDemo() {
        System.out.println("=== GC Demo ===");
        System.out.println("Heap before: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB used");

        // Create objects that become eligible for GC
        for (int i = 0; i < 10; i++) {
            HeavyObject obj = new HeavyObject(); // allocated in Eden
            // obj goes out of scope here — eligible for GC
        }

        System.gc();  // Suggestion — not guaranteed (never do this in production!)
        System.out.println("After GC suggestion: memory may or may not be reclaimed");
        System.out.println("Heap after: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + "MB free");
    }

    // ─── Stack vs Heap ───────────────────────────────────────────────────────────
    static void stackHeapDemo() {
        System.out.println("\n=== Stack vs Heap ===");
        /*
         * Stack:
         *  - int x = 5;         → stored on stack (primitive)
         *  - String ref = "hi"; → 'ref' is on stack, String object is on heap (or String pool)
         *  - Each method call = new stack frame
         *  - Automatically freed when method returns
         *
         * Heap:
         *  - new Object()       → always on heap (unless escape analysis allows stack allocation)
         *  - All instance fields → heap
         *  - String pool is in Heap since Java 7 (was in PermGen in Java 6)
         */
        int x = 42;                    // x on stack
        Integer boxed = x;             // autoboxing: new Integer on heap (or cached for -128..127)
        String str = new String("hi"); // str reference on stack, String object on heap
        String interned = "hi";        // in String pool (special area in heap)

        System.out.println("Stack var: " + x);
        System.out.println("Heap object: " + str);
        System.out.println("Same pool object? " + (interned == "hi")); // true (String pool)
        System.out.println("New String == pooled? " + (str == interned)); // false

        // Integer cache: -128 to 127 are cached (interned)
        Integer a = 127; Integer b = 127;
        Integer c = 128; Integer d = 128;
        System.out.println("127 == 127: " + (a == b)); // true (cached)
        System.out.println("128 == 128: " + (c == d)); // false (not cached, new objects)
    }

    // ─── Weak/Soft/Phantom References ─────────────────────────────────────────
    static void referenceTypesDemo() {
        System.out.println("\n=== Reference Types ===");
        /*
         * Strong reference: default — Object obj = new Object() — GC NEVER collects while reachable
         * Soft reference: collected only when JVM needs memory (good for caches)
         * Weak reference: collected at ANY GC run (good for WeakHashMap, canonical maps)
         * Phantom reference: object already finalized but not yet reclaimed (rarely used directly)
         *
         * java.lang.ref.WeakReference<MyObj> ref = new WeakReference<>(obj);
         * ref.get() returns null after GC collects the referent
         *
         * Common use: WeakHashMap — keys held weakly; entry removed when key is GC'd
         *             Perfect for association of metadata without preventing GC
         */
        java.lang.ref.WeakReference<String> weak = new java.lang.ref.WeakReference<>(new String("weak"));
        java.lang.ref.SoftReference<byte[]> soft = new java.lang.ref.SoftReference<>(new byte[1024]);

        System.out.println("Weak ref: " + weak.get());  // may or may not be null
        System.out.println("Soft ref: " + (soft.get() != null ? "present" : "collected"));
    }

    // ─── JVM Flags cheat sheet ──────────────────────────────────────────────────
    static void jvmFlagsInfo() {
        System.out.println("\n=== Common JVM Flags ===");
        /*
         * Memory:
         *   -Xms512m              — initial heap size
         *   -Xmx2g                — max heap size (set equal to -Xms to avoid resizing)
         *   -Xss1m                — thread stack size
         *   -XX:MetaspaceSize=256m — initial metaspace size
         *   -XX:MaxMetaspaceSize=512m
         *
         * GC:
         *   -XX:+UseG1GC           — use G1 collector
         *   -XX:+UseZGC            — use ZGC (Java 15+)
         *   -XX:MaxGCPauseMillis=200 — target pause time for G1
         *   -verbose:gc            — print GC logs
         *   -XX:+PrintGCDetails
         *
         * Diagnostics:
         *   -XX:+HeapDumpOnOutOfMemoryError  — dump heap on OOM
         *   -XX:HeapDumpPath=/tmp/dump.hprof
         *   -XX:+PrintCompilation          — JIT compilation events
         */
        System.out.println("JVM version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max heap: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + "MB");
    }

    public static void main(String[] args) {
        gcDemo();
        stackHeapDemo();
        referenceTypesDemo();
        jvmFlagsInfo();
    }
}
