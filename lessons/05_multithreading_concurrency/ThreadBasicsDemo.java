package lessons.multithreading_concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * LESSON 05A — Thread Basics + Synchronization
 *
 * SDE2 Interview Questions:
 *  - "Runnable vs Callable vs Thread?"
 *    => Thread: is a thread. Runnable: task without result. Callable: task with result (+ checked exception).
 *  - "What is a race condition?"
 *    => Two threads read-modify-write shared state concurrently: lost updates.
 *  - "What is synchronized?" => JVM-level mutual exclusion on object monitor (intrinsic lock)
 *  - "What is the Java Memory Model (JMM)?"
 *    => Defines when writes by one thread become visible to others. synchronized & volatile provide visibility.
 */
public class ThreadBasicsDemo {

    // ─── 1. Thread Creation: Thread subclass ────────────────────────────────────
    static class CounterThread extends Thread {
        private final String label;
        CounterThread(String label) { this.label = label; }

        @Override
        public void run() {
            for (int i = 1; i <= 3; i++) {
                System.out.println(label + ": " + i);
                try { Thread.sleep(50); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();  // restore interrupt flag
                    return;
                }
            }
        }
    }

    // ─── 2. Thread Creation: Runnable (preferred — separates task from thread) ──
    static Runnable makeRunnable(String label) {
        return () -> {   // Lambda Runnable
            for (int i = 1; i <= 3; i++) {
                System.out.println(label + ": " + i);
                try { Thread.sleep(50); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); return;
                }
            }
        };
    }

    // ─── 3. Race Condition — unsynchronized shared counter ───────────────────────
    static class UnsafeCounter {
        int count = 0;
        void increment() { count++; }   // NOT atomic: read → add → write (3 steps!)
    }

    // ─── 4. Synchronized method — fixes race condition ──────────────────────────
    static class SafeCounter {
        private int count = 0;
        synchronized void increment() { count++; }  // holds intrinsic lock on `this`
        synchronized int getCount()   { return count; }
    }

    // ─── 5. Synchronized block (fine-grained locking) ───────────────────────────
    static class FineGrainedCounter {
        private int countA = 0, countB = 0;
        private final Object lockA = new Object();  // separate locks for A and B
        private final Object lockB = new Object();

        void incrementA() { synchronized (lockA) { countA++; } }
        void incrementB() { synchronized (lockB) { countB++; } }
        // A and B can be incremented concurrently — only block each other within same lock
        int getA() { synchronized (lockA) { return countA; } }
        int getB() { synchronized (lockB) { return countB; } }
    }

    // ─── 6. Volatile — visibility guarantee, NOT atomicity ───────────────────────
    static class StopFlag {
        private volatile boolean stopped = false;  // volatile: writes visible across threads

        void stop() { stopped = true; }

        void run() {
            System.out.println("Worker starting...");
            while (!stopped) {
                // Without volatile, this loop might never see updates from another thread
            }
            System.out.println("Worker stopped.");
        }
    }

    // ─── 7. Atomic classes — lock-free thread safety using CAS ──────────────────
    static class AtomicCounter {
        private final AtomicInteger count = new AtomicInteger(0);

        void increment()      { count.incrementAndGet(); }          // atomic
        void add(int delta)   { count.addAndGet(delta); }           // atomic
        void conditionalSet() { count.compareAndSet(10, 0); }       // CAS: if 10 → set 0
        int getCount()        { return count.get(); }
    }

    public static void main(String[] args) throws InterruptedException {

        System.out.println("=== Thread Subclass ===");
        Thread t1 = new CounterThread("T1");
        Thread t2 = new CounterThread("T2");
        t1.start();   // start() creates new thread and calls run()
        t2.start();
        t1.join();    // wait for t1 to finish
        t2.join();

        System.out.println("\n=== Runnable Lambda ===");
        Thread r1 = new Thread(makeRunnable("R1"));
        Thread r2 = new Thread(makeRunnable("R2"));
        r1.start(); r2.start();
        r1.join(); r2.join();

        System.out.println("\n=== Race Condition Demo ===");
        UnsafeCounter unsafe = new UnsafeCounter();
        Thread[] racers = new Thread[100];
        for (int i = 0; i < 100; i++) {
            racers[i] = new Thread(unsafe::increment);
            racers[i].start();
        }
        for (Thread t : racers) t.join();
        System.out.println("Unsafe count (expected 100, may be less): " + unsafe.count);

        System.out.println("\n=== Synchronized Counter ===");
        SafeCounter safe = new SafeCounter();
        Thread[] safeThreads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            safeThreads[i] = new Thread(safe::increment);
            safeThreads[i].start();
        }
        for (Thread t : safeThreads) t.join();
        System.out.println("Safe count (always 100): " + safe.getCount());

        System.out.println("\n=== Atomic Counter ===");
        AtomicCounter atomicCounter = new AtomicCounter();
        Thread[] atomicThreads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            atomicThreads[i] = new Thread(atomicCounter::increment);
            atomicThreads[i].start();
        }
        for (Thread t : atomicThreads) t.join();
        System.out.println("Atomic count (always 100): " + atomicCounter.getCount());

        System.out.println("\n=== Thread States ===");
        /*
         *  NEW          — created, not started
         *  RUNNABLE     — running or ready to run
         *  BLOCKED      — waiting to acquire a lock (synchronized)
         *  WAITING      — waiting indefinitely (wait(), join(), LockSupport.park())
         *  TIMED_WAITING — waiting for specified time (sleep(), wait(timeout))
         *  TERMINATED   — run() completed or exception thrown
         */
        Thread demo = new Thread(() -> {});
        System.out.println("State before start: " + demo.getState());  // NEW
        demo.start();
        demo.join();
        System.out.println("State after join: "  + demo.getState());   // TERMINATED
    }
}
