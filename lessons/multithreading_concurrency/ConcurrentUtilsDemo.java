package lessons.multithreading_concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * LESSON 05C — Concurrency Utilities + Deadlock + Producer-Consumer
 *
 * SDE2 Interview Questions:
 *  - "How to implement Producer-Consumer?" => BlockingQueue (LinkedBlockingQueue/ArrayBlockingQueue)
 *  - "What is CountDownLatch?" => Countdown from N; threads await() till count reaches 0 (one-shot)
 *  - "What is CyclicBarrier?" => All parties call await(); released together; reusable
 *  - "What is Semaphore?" => Controls N concurrent accesses; acquire/release
 *  - "How to prevent deadlock?" => Consistent lock ordering, tryLock with timeout, avoid nested locks
 */
public class ConcurrentUtilsDemo {

    // ─── 1. Producer-Consumer with BlockingQueue ──────────────────────────────────
    static void producerConsumerDemo() throws InterruptedException {
        System.out.println("=== Producer-Consumer (BlockingQueue) ===");
        // ArrayBlockingQueue: bounded, better throughput guarantees
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    queue.put(i);     // blocks if queue full
                    System.out.println("Produced: " + i);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    int item = queue.take();  // blocks if queue empty
                    System.out.println("Consumed: " + item);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });

        producer.start(); consumer.start();
        producer.join(); consumer.join();
    }

    // ─── 2. CountDownLatch — wait for multiple threads to complete ────────────────
    static void countDownLatchDemo() throws InterruptedException {
        System.out.println("\n=== CountDownLatch ===");
        int numServices = 3;
        CountDownLatch latch = new CountDownLatch(numServices);

        String[] services = {"Database", "Cache", "MessageBroker"};
        for (String service : services) {
            new Thread(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 300));
                    System.out.println(service + " initialized");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();  // decrement counter
                }
            }).start();
        }

        System.out.println("Waiting for all services...");
        latch.await();          // blocks until count reaches 0
        // latch.await(5, TimeUnit.SECONDS);  // with timeout
        System.out.println("All services ready. Starting application.");
    }

    // ─── 3. CyclicBarrier — all threads meet at a point, then proceed ─────────────
    static void cyclicBarrierDemo() throws Exception {
        System.out.println("\n=== CyclicBarrier ===");
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties,
            () -> System.out.println("--- All threads reached barrier, proceeding ---"));
        // Barrier action runs once when last party arrives

        for (int i = 0; i < parties; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    System.out.println("Thread " + id + " doing phase 1");
                    Thread.sleep((long) (Math.random() * 200));
                    barrier.await();  // wait for all parties

                    System.out.println("Thread " + id + " doing phase 2");
                    Thread.sleep((long) (Math.random() * 200));
                    barrier.await();  // reusable! same barrier for phase 2

                    System.out.println("Thread " + id + " done");
                } catch (Exception e) { Thread.currentThread().interrupt(); }
            }).start();
        }
        Thread.sleep(1000); // wait for threads
    }

    // ─── 4. Semaphore — control concurrent access ────────────────────────────────
    static void semaphoreDemo() throws InterruptedException {
        System.out.println("\n=== Semaphore (connection pool simulation) ===");
        Semaphore semaphore = new Semaphore(3);  // 3 permits = 3 concurrent connections

        for (int i = 1; i <= 7; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    System.out.println("Task " + id + " waiting for connection...");
                    semaphore.acquire();  // blocks if no permits available
                    System.out.println("Task " + id + " acquired connection. Permits: "
                        + semaphore.availablePermits());
                    Thread.sleep(200);    // simulate work
                    System.out.println("Task " + id + " releasing connection");
                    semaphore.release();
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }
        Thread.sleep(1500);
    }

    // ─── 5. Deadlock example + prevention ────────────────────────────────────────
    static class DeadlockExample {
        static final Object lockA = new Object();
        static final Object lockB = new Object();

        // Thread 1: locks A then B
        static Runnable thread1 = () -> {
            synchronized (lockA) {
                System.out.println("Thread1 acquired A");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                synchronized (lockB) {  // Deadlock: Thread2 holds B, waiting for A
                    System.out.println("Thread1 acquired B");
                }
            }
        };

        // Thread 2: locks B then A — OPPOSITE order → deadlock!
        static Runnable thread2deadlock = () -> {
            synchronized (lockB) {
                System.out.println("Thread2 acquired B");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                synchronized (lockA) {
                    System.out.println("Thread2 acquired A");
                }
            }
        };

        // Prevention: always acquire locks in SAME ORDER
        static Runnable thread2fixed = () -> {
            synchronized (lockA) {   // Same order as Thread1: A then B
                System.out.println("Thread2-fixed acquired A");
                try { Thread.sleep(50); } catch (InterruptedException e) {}
                synchronized (lockB) {
                    System.out.println("Thread2-fixed acquired B");
                }
            }
        };
    }

    // ─── 6. wait/notify — low-level monitor-based coordination ──────────────────
    static class SharedBuffer {
        private Integer value = null;

        synchronized void produce(int v) throws InterruptedException {
            while (value != null) wait();  // wait until consumed
            value = v;
            System.out.println("Produced: " + v);
            notifyAll();  // wake waiting consumers
        }

        synchronized int consume() throws InterruptedException {
            while (value == null) wait();  // wait until produced
            int v = value;
            value = null;
            System.out.println("Consumed: " + v);
            notifyAll();  // wake waiting producer
            return v;
        }
    }

    public static void main(String[] args) throws Exception {
        producerConsumerDemo();
        countDownLatchDemo();
        cyclicBarrierDemo();
        semaphoreDemo();

        System.out.println("\n=== wait/notify ===");
        SharedBuffer buffer = new SharedBuffer();
        Thread producer = new Thread(() -> {
            try { for (int i = 1; i <= 3; i++) buffer.produce(i); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        Thread consumer = new Thread(() -> {
            try { for (int i = 0; i < 3; i++) buffer.consume(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        producer.start(); consumer.start();
        producer.join(); consumer.join();

        System.out.println("\n=== Deadlock Prevention ===");
        // No deadlock — both acquire in same order (A then B)
        Thread t1 = new Thread(DeadlockExample.thread1);
        Thread t2 = new Thread(DeadlockExample.thread2fixed);
        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("Completed without deadlock");
    }
}
