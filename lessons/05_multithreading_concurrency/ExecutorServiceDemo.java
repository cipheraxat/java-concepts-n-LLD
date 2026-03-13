package lessons.multithreading_concurrency;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.*;

/**
 * LESSON 05B — ExecutorService, CompletableFuture, Locks
 *
 * SDE2 Interview Questions:
 *  - "Why use ExecutorService over creating threads directly?"
 *    => Thread reuse, controlled pool size, lifecycle management, better task scheduling
 *  - "What's the difference between submit() and execute()?"
 *    => execute(): Runnable, no return. submit(): Runnable/Callable, returns Future
 *  - "What is CompletableFuture?" => Promise-style async programming with chaining, combining
 *  - "ReentrantLock vs synchronized?"
 *    => ReentrantLock: tryLock, fairness, multiple conditions. synchronized: simpler, JVM-optimized
 */
public class ExecutorServiceDemo {

    // ─── 1. Thread Pools ─────────────────────────────────────────────────────────
    static void threadPoolDemo() throws InterruptedException, ExecutionException {
        System.out.println("=== Fixed Thread Pool ===");
        // newFixedThreadPool: 4 threads, tasks queue if all busy
        ExecutorService pool = Executors.newFixedThreadPool(4);

        // submit Runnable (no return value)
        pool.execute(() -> System.out.println("Task 1 on " + Thread.currentThread().getName()));

        // submit Callable (returns Future<V>)
        Future<Integer> future = pool.submit(() -> {
            Thread.sleep(100);
            return 42;
        });

        System.out.println("Future result: " + future.get()); // blocks until done

        pool.shutdown();  // graceful shutdown — accepts no new tasks, waits for running
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n=== Cached Thread Pool ===");
        // newCachedThreadPool: 0-Integer.MAX_VALUE threads, idle threads recycled after 60s
        // Good for: many short-lived tasks
        ExecutorService cached = Executors.newCachedThreadPool();
        for (int i = 0; i < 5; i++) {
            int taskId = i;
            cached.submit(() -> System.out.println("Cached task " + taskId
                + " on " + Thread.currentThread().getName()));
        }
        cached.shutdown();
        cached.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("\n=== Single Thread Executor ===");
        // newSingleThreadExecutor: 1 thread, tasks execute sequentially in submission order
        ExecutorService single = Executors.newSingleThreadExecutor();
        for (int i = 0; i < 3; i++) {
            int taskId = i;
            single.submit(() -> System.out.println("Sequential task " + taskId));
        }
        single.shutdown();
        single.awaitTermination(2, TimeUnit.SECONDS);
    }

    // ─── 2. CompletableFuture ─────────────────────────────────────────────────────
    static void completableFutureDemo() throws ExecutionException, InterruptedException {
        System.out.println("\n=== CompletableFuture ===");

        // Async computation
        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            // runs on ForkJoinPool.commonPool() by default
            return "Hello";
        });

        // Chain transformations (like Stream map)
        CompletableFuture<String> result = cf
            .thenApply(s -> s + " World")         // sync transform
            .thenApply(String::toUpperCase);       // sync transform

        System.out.println("Chained: " + result.get()); // HELLO WORLD

        // thenCompose — flatMap (for dependent async tasks)
        CompletableFuture<String> chained = CompletableFuture.supplyAsync(() -> "user-123")
            .thenCompose(userId -> CompletableFuture.supplyAsync(() -> "Profile for " + userId));
        System.out.println("Composed: " + chained.get());

        // thenCombine — combine two independent async tasks
        CompletableFuture<String> price    = CompletableFuture.supplyAsync(() -> "$100");
        CompletableFuture<String> discount = CompletableFuture.supplyAsync(() -> "10%");
        CompletableFuture<String> combined = price.thenCombine(discount,
            (p, d) -> "Price " + p + " with discount " + d);
        System.out.println("Combined: " + combined.get());

        // allOf — wait for ALL futures
        CompletableFuture<Void> all = CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> System.out.println("Task A")),
            CompletableFuture.runAsync(() -> System.out.println("Task B")),
            CompletableFuture.runAsync(() -> System.out.println("Task C"))
        );
        all.get();  // wait for all 3

        // anyOf — first to complete wins
        CompletableFuture<Object> any = CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> { try {Thread.sleep(100);} catch(Exception e){} return "slow"; }),
            CompletableFuture.supplyAsync(() -> "fast")
        );
        System.out.println("First completed: " + any.get()); // "fast"

        // Exception handling
        CompletableFuture<String> withError = CompletableFuture
            .supplyAsync(() -> { throw new RuntimeException("oops"); })
            .exceptionally(e -> "Recovered from: " + e.getMessage())
            .thenApply(String::toUpperCase);
        System.out.println("Error handled: " + withError.get());
    }

    // ─── 3. ReentrantLock ─────────────────────────────────────────────────────────
    static class SafeResource {
        private final ReentrantLock lock = new ReentrantLock(true); // fair lock
        private int value = 0;

        void increment() {
            lock.lock();
            try {
                value++;
            } finally {
                lock.unlock();  // ALWAYS unlock in finally
            }
        }

        boolean tryIncrement() {
            if (lock.tryLock()) {  // non-blocking attempt
                try { value++; return true; }
                finally { lock.unlock(); }
            }
            return false;  // could not acquire lock
        }

        int getValue() { return value; }
    }

    // ─── 4. ReadWriteLock ─────────────────────────────────────────────────────────
    // Multiple threads can READ concurrently; only one can WRITE (exclusive)
    static class Cache {
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
        private final Lock readLock  = rwLock.readLock();
        private final Lock writeLock = rwLock.writeLock();
        private final Map<String, String> map = new HashMap<>();

        String get(String key) {
            readLock.lock();   // Multiple threads can hold read lock simultaneously
            try { return map.get(key); }
            finally { readLock.unlock(); }
        }

        void put(String key, String value) {
            writeLock.lock();  // Exclusive — all readers/writers blocked
            try { map.put(key, value); }
            finally { writeLock.unlock(); }
        }
    }

    public static void main(String[] args) throws Exception {
        threadPoolDemo();
        completableFutureDemo();

        System.out.println("\n=== ReentrantLock ===");
        SafeResource resource = new SafeResource();
        ExecutorService pool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) pool.submit(resource::increment);
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("ReentrantLock value (should be 100): " + resource.getValue());

        System.out.println("\n=== ReadWriteLock Cache ===");
        Cache cache = new Cache();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        System.out.println("key1: " + cache.get("key1"));

        System.out.println("\n=== Custom ThreadPoolExecutor ===");
        // Fine-tune pool parameters for production use
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            2,                          // core pool size
            5,                          // max pool size
            60L, TimeUnit.SECONDS,      // keep-alive for idle threads > core
            new ArrayBlockingQueue<>(10), // bounded work queue
            new ThreadPoolExecutor.CallerRunsPolicy()  // rejection policy: caller thread runs task
        );
        // Rejection policies:
        //   AbortPolicy (default): throws RejectedExecutionException
        //   CallerRunsPolicy: calling thread runs the task (backpressure)
        //   DiscardPolicy: silently discards task
        //   DiscardOldestPolicy: discards oldest queued task
        for (int i = 0; i < 5; i++) {
            int id = i;
            customPool.submit(() -> System.out.println("Custom pool task " + id));
        }
        customPool.shutdown();
        customPool.awaitTermination(2, TimeUnit.SECONDS);
    }
}
