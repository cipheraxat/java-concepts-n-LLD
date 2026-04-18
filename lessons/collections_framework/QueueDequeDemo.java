package lessons.collections_framework;

import java.util.*;

/**
 * LESSON 02D — Queue, Deque, PriorityQueue
 *
 * Queue: FIFO — offer/poll/peek (preferred over add/remove/element to avoid exceptions)
 * Deque: double-ended queue — addFirst/addLast/pollFirst/pollLast
 * PriorityQueue: heap-based, elements dequeued by natural order or custom Comparator
 * ArrayDeque: preferred over Stack (legacy) and LinkedList for queue/stack use cases
 *
 * SDE2 Interview:
 *  - "Why prefer ArrayDeque over Stack?" => Stack extends Vector (synchronized, legacy).
 *    ArrayDeque is resizable array-based, faster, no synchronization overhead.
 *  - "PriorityQueue default ordering?" => Natural ordering (min-heap). For max-heap use reverseOrder().
 *  - "Is PriorityQueue thread-safe?" => No. Use PriorityBlockingQueue for thread safety.
 */
public class QueueDequeDemo {

    public static void main(String[] args) {

        // ── Queue (FIFO) ─────────────────────────────────────────────────────────
        System.out.println("=== Queue (FIFO) ===");
        // offer/poll/peek: no exceptions on empty queue (returns false/null)
        // add/remove/element: throws exception on empty queue
        Queue<String> queue = new ArrayDeque<>();
        queue.offer("First");
        queue.offer("Second");
        queue.offer("Third");

        System.out.println("peek (does not remove): " + queue.peek());  // First
        System.out.println("poll (removes):         " + queue.poll());  // First
        System.out.println("Queue now: " + queue);                      // [Second, Third]

        // ── ArrayDeque as Stack (LIFO) ────────────────────────────────────────────
        System.out.println("\n=== ArrayDeque as Stack (LIFO) ===");
        Deque<String> stack = new ArrayDeque<>();
        stack.push("Bottom");
        stack.push("Middle");
        stack.push("Top");        // push = addFirst

        System.out.println("peek: " + stack.peek());   // Top
        System.out.println("pop: "  + stack.pop());    // Top (removeFirst)
        System.out.println("Stack: " + stack);

        // ── ArrayDeque as Deque ───────────────────────────────────────────────────
        System.out.println("\n=== ArrayDeque as Deque ===");
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(2);
        deque.addFirst(1);  // [1, 2]
        deque.addLast(3);   // [1, 2, 3]
        deque.addLast(4);   // [1, 2, 3, 4]
        System.out.println("Deque: " + deque);
        System.out.println("pollFirst: " + deque.pollFirst()); // 1
        System.out.println("pollLast: "  + deque.pollLast());  // 4
        System.out.println("Deque: " + deque);

        // ── PriorityQueue (min-heap by default) ──────────────────────────────────
        System.out.println("\n=== PriorityQueue (min-heap) ===");
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        minHeap.offer(5); minHeap.offer(1); minHeap.offer(3); minHeap.offer(2); minHeap.offer(4);

        System.out.print("PQ poll order (min-heap): ");
        while (!minHeap.isEmpty()) System.out.print(minHeap.poll() + " "); // 1 2 3 4 5
        System.out.println();

        // ── PriorityQueue (max-heap) ──────────────────────────────────────────────
        System.out.println("\n=== PriorityQueue (max-heap) ===");
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        maxHeap.offer(5); maxHeap.offer(1); maxHeap.offer(3); maxHeap.offer(2); maxHeap.offer(4);

        System.out.print("PQ poll order (max-heap): ");
        while (!maxHeap.isEmpty()) System.out.print(maxHeap.poll() + " "); // 5 4 3 2 1
        System.out.println();

        // ── PriorityQueue with custom comparator ──────────────────────────────────
        System.out.println("\n=== PriorityQueue (custom order) ===");
        record Task(String name, int priority) {}

        // Higher priority number = higher priority (max-heap on priority field)
        PriorityQueue<Task> taskQueue = new PriorityQueue<>(
            Comparator.comparingInt(Task::priority).reversed()
        );
        taskQueue.offer(new Task("Low task", 1));
        taskQueue.offer(new Task("High task", 10));
        taskQueue.offer(new Task("Medium task", 5));

        while (!taskQueue.isEmpty()) System.out.println("  " + taskQueue.poll());

        // ── K largest elements (classic interview problem) ─────────────────────
        System.out.println("\n=== Find K Largest Elements ===");
        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3};
        int k = 3;
        // Use min-heap of size k
        PriorityQueue<Integer> kLargest = new PriorityQueue<>(k);
        for (int num : arr) {
            kLargest.offer(num);
            if (kLargest.size() > k) kLargest.poll();  // remove smallest
        }
        System.out.println("Top " + k + " largest: " + kLargest); // [5, 6, 9] (heap order)

        // ── Merge K sorted arrays (PriorityQueue pattern) ──────────────────────
        System.out.println("\n=== Merge K Sorted Lists (PQ pattern) ===");
        int[][] sorted = {{1, 4, 7}, {2, 5, 8}, {3, 6, 9}};
        // Store [value, arrayIndex, elementIndex]
        PriorityQueue<int[]> merge = new PriorityQueue<>(Comparator.comparingInt(x -> x[0]));
        for (int i = 0; i < sorted.length; i++) merge.offer(new int[]{sorted[i][0], i, 0});

        System.out.print("Merged: ");
        while (!merge.isEmpty()) {
            int[] curr = merge.poll();
            System.out.print(curr[0] + " ");
            if (curr[2] + 1 < sorted[curr[1]].length) {
                merge.offer(new int[]{sorted[curr[1]][curr[2]+1], curr[1], curr[2]+1});
            }
        }
        System.out.println();
    }
}
