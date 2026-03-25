# Task Scheduler

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft

---

## Requirements

> "Design a task scheduler that accepts tasks with priorities and scheduled execution times, dispatches them to a pool of worker threads, supports cancellation, and retries failed tasks with exponential backoff."

### Clarifying Questions

> **You:** "How are tasks prioritized?"
>
> **Interviewer:** "By priority level (HIGH, MEDIUM, LOW). Among same-priority tasks, execute by scheduled time (earliest first)."

> **You:** "What's the retry policy?"
>
> **Interviewer:** "Exponential backoff — 1s, 2s, 4s — up to a configurable max retries."

> **You:** "Can tasks be cancelled after submission?"
>
> **Interviewer:** "Yes. Return a handle that the submitter can use to cancel."

> **You:** "How many worker threads?"
>
> **Interviewer:** "Configurable pool size."

### Final Requirements

```
Requirements:
1. Submit tasks with a priority (HIGH/MEDIUM/LOW) and optional scheduled time
2. PriorityBlockingQueue orders tasks by priority, then by scheduled time
3. Worker pool dispatches tasks concurrently
4. Return a TaskHandle for cancellation
5. Retry failed tasks with exponential backoff (max retries configurable)

Out of scope:
- Persistent task storage
- Distributed scheduling
- Cron-like recurring schedules
- Task dependencies / DAGs
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **TaskScheduler** | Facade. Accepts tasks, manages the priority queue, starts workers. |
| **Task** | Encapsulates the work to execute (a Runnable-like interface). Holds priority, scheduled time, retry metadata. |
| **TaskHandle** | Returned to the submitter. Allows cancellation and status query. |
| **Worker** | Thread that polls tasks from the queue and executes them. |
| **RetryPolicy** | Configures max retries and backoff calculation. |

---

## Class Design

### Task

| Requirement | What Task must track |
|-------------|---------------------|
| "Priority ordering" | Priority enum + scheduled time |
| "Retry with backoff" | attempt count, max retries, next execution time |
| "Cancellation" | volatile cancelled flag via TaskHandle |

```
class Task implements Comparable<Task>:
    - taskId: String
    - runnable: Runnable
    - priority: Priority
    - scheduledTime: Instant
    - attempt: int
    - maxRetries: int
    - handle: TaskHandle

    + compareTo(other): compare by priority (HIGH < MED < LOW),
                        then by scheduledTime

enum Priority: HIGH(0), MEDIUM(1), LOW(2)
```

### TaskScheduler

```
class TaskScheduler:
    - queue: PriorityBlockingQueue<Task>
    - workers: List<Worker>
    - running: volatile boolean
    - taskCounter: AtomicLong

    + submit(runnable, priority) → TaskHandle
    + submit(runnable, priority, delay) → TaskHandle
    + shutdown() → void
    - rescheduleWithBackoff(task) → void
```

### TaskHandle

```
class TaskHandle:
    - task: Task
    - cancelled: AtomicBoolean
    - status: TaskStatus

    + cancel() → boolean
    + getStatus() → TaskStatus
    + isCancelled() → boolean

enum TaskStatus: PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
```

---

## Implementation

### TaskScheduler.submit

```
submit(runnable, priority, delay)
    handle = new TaskHandle()
    task = new Task(nextId(), runnable, priority, now + delay, maxRetries, handle)
    handle.task = task
    queue.offer(task)
    return handle
```

### Worker.run

**Core logic:** Continuously poll the queue. If the task's scheduled time hasn't arrived, re-queue and sleep briefly. Execute the task, handle success/failure.

```
run()
    while running:
        task = queue.poll(1, SECONDS)
        if task is null → continue
        if task.handle.isCancelled() → continue

        if now < task.scheduledTime:
            queue.offer(task)  // not yet due
            sleep(100ms)
            continue

        task.handle.status = RUNNING
        try:
            task.runnable.run()
            task.handle.status = COMPLETED
        catch Exception:
            if task.attempt < task.maxRetries:
                rescheduleWithBackoff(task)
            else:
                task.handle.status = FAILED
```

### rescheduleWithBackoff

```
rescheduleWithBackoff(task)
    task.attempt++
    backoff = 1000 × 2^(task.attempt - 1)  // 1s, 2s, 4s, ...
    task.scheduledTime = now + backoff ms
    queue.offer(task)
```

### Complete Code Implementation

```java
public enum Priority {
    HIGH(0), MEDIUM(1), LOW(2);

    private final int level;
    Priority(int level) { this.level = level; }
    public int getLevel() { return level; }
}

public enum TaskStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}
```

```java
import java.util.concurrent.atomic.AtomicBoolean;

public class TaskHandle {
    private volatile TaskStatus status = TaskStatus.PENDING;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public boolean cancel() {
        if (cancelled.compareAndSet(false, true)) {
            status = TaskStatus.CANCELLED;
            return true;
        }
        return false;
    }

    public boolean isCancelled() { return cancelled.get(); }
    public TaskStatus getStatus() { return status; }
    void setStatus(TaskStatus status) { this.status = status; }
}
```

```java
import java.time.Instant;

public class Task implements Comparable<Task> {
    private final String taskId;
    private final Runnable runnable;
    private final Priority priority;
    private Instant scheduledTime;
    private int attempt;
    private final int maxRetries;
    private final TaskHandle handle;

    public Task(String taskId, Runnable runnable, Priority priority,
                Instant scheduledTime, int maxRetries, TaskHandle handle) {
        this.taskId = taskId;
        this.runnable = runnable;
        this.priority = priority;
        this.scheduledTime = scheduledTime;
        this.attempt = 0;
        this.maxRetries = maxRetries;
        this.handle = handle;
    }

    @Override
    public int compareTo(Task other) {
        int cmp = Integer.compare(this.priority.getLevel(), other.priority.getLevel());
        if (cmp != 0) return cmp;
        return this.scheduledTime.compareTo(other.scheduledTime);
    }

    public String getTaskId()         { return taskId; }
    public Runnable getRunnable()     { return runnable; }
    public Priority getPriority()     { return priority; }
    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant t) { this.scheduledTime = t; }
    public int getAttempt()           { return attempt; }
    public void incrementAttempt()    { this.attempt++; }
    public int getMaxRetries()        { return maxRetries; }
    public TaskHandle getHandle()     { return handle; }
}
```

```java
import java.time.Instant;
import java.util.concurrent.PriorityBlockingQueue;

public class Worker implements Runnable {
    private final PriorityBlockingQueue<Task> queue;
    private final TaskScheduler scheduler;
    private volatile boolean running = true;

    public Worker(PriorityBlockingQueue<Task> queue, TaskScheduler scheduler) {
        this.queue = queue;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Task task = queue.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                if (task == null) continue;
                if (task.getHandle().isCancelled()) continue;

                if (Instant.now().isBefore(task.getScheduledTime())) {
                    queue.offer(task);
                    Thread.sleep(100);
                    continue;
                }

                task.getHandle().setStatus(TaskStatus.RUNNING);
                try {
                    task.getRunnable().run();
                    task.getHandle().setStatus(TaskStatus.COMPLETED);
                } catch (Exception e) {
                    if (task.getAttempt() < task.getMaxRetries()) {
                        scheduler.rescheduleWithBackoff(task);
                    } else {
                        task.getHandle().setStatus(TaskStatus.FAILED);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void stop() { running = false; }
}
```

```java
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class TaskScheduler {
    private final PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();
    private final List<Worker> workers = new ArrayList<>();
    private final List<Thread> threads = new ArrayList<>();
    private final int maxRetries;
    private final AtomicLong taskCounter = new AtomicLong(1);

    public TaskScheduler(int poolSize, int maxRetries) {
        this.maxRetries = maxRetries;
        for (int i = 0; i < poolSize; i++) {
            Worker worker = new Worker(queue, this);
            workers.add(worker);
            Thread t = new Thread(worker, "Worker-" + i);
            t.setDaemon(true);
            t.start();
            threads.add(t);
        }
    }

    public TaskHandle submit(Runnable runnable, Priority priority) {
        return submit(runnable, priority, Duration.ZERO);
    }

    public TaskHandle submit(Runnable runnable, Priority priority, Duration delay) {
        TaskHandle handle = new TaskHandle();
        Task task = new Task(
            "T-" + taskCounter.getAndIncrement(),
            runnable, priority,
            Instant.now().plus(delay), maxRetries, handle);
        queue.offer(task);
        return handle;
    }

    void rescheduleWithBackoff(Task task) {
        task.incrementAttempt();
        long backoffMs = 1000L * (1L << (task.getAttempt() - 1));
        task.setScheduledTime(Instant.now().plusMillis(backoffMs));
        queue.offer(task);
    }

    public void shutdown() {
        for (Worker w : workers) w.stop();
        for (Thread t : threads) t.interrupt();
    }
}
```

### Verification

```
Setup: TaskScheduler(poolSize=2, maxRetries=3)

Step 1: Submit Task A (HIGH priority, no delay)
  handle_A = scheduler.submit(() → print("A"), HIGH)
  Task T-1 added to queue. scheduledTime=now.

Step 2: Submit Task B (LOW priority, no delay)
  Task T-2 added to queue.

Step 3: Submit Task C (HIGH priority, 5-sec delay)
  Task T-3 added to queue. scheduledTime=now+5s.

Worker picks from queue (priority order):
  T-1 (HIGH, now) vs T-3 (HIGH, now+5s) vs T-2 (LOW, now)
  T-1 wins (HIGH, earliest time).
  Execute: prints "A". Status → COMPLETED ✓

  Next: T-3 (HIGH, now+5s) vs T-2 (LOW, now)
  T-3 has higher priority but scheduledTime is future.
  Worker picks T-3, checks now < scheduledTime → re-queue.
  Worker picks T-2 (LOW, now) → execute. Status → COMPLETED ✓

  Later (after 5s): Worker picks T-3. now ≥ scheduledTime → execute.

Step 4: Task D fails (simulate)
  submit(() → throw exception, MEDIUM)
  Attempt 0: fails → reschedule with 1s backoff (attempt=1)
  Attempt 1: fails → reschedule with 2s backoff (attempt=2)
  Attempt 2: fails → reschedule with 4s backoff (attempt=3)
  Attempt 3: fails → attempt=3 == maxRetries → Status = FAILED ✓

Step 5: Cancel Task E before execution
  handle_E = submit(longTask, LOW)
  handle_E.cancel() → true
  Worker polls T-E, sees cancelled → skip ✓
```

---

## Extensibility

### 1. "How would you add recurring / cron-like tasks?"

> "I'd add a `RecurringTask` subclass with a `Duration interval` or cron expression. After successful execution, the Worker creates a new Task with scheduledTime = now + interval and re-submits it. A `cancelRecurring` method on the handle stops future recurrences."

### 2. "How would you add task dependencies?"

> "I'd model a DAG where each Task has a `List<Task> dependencies`. A task is eligible for execution only when all dependencies are COMPLETED. A `DependencyResolver` scans ready tasks and submits them. This is essentially a topological sort execution engine."

### 3. "How would you persist tasks for crash recovery?"

> "I'd write each submitted task to a durable store (database or log file) with its status. On startup, the scheduler replays PENDING and RUNNING tasks from the store. Each status transition updates the store. This gives at-least-once execution semantics."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should be able to use a PriorityBlockingQueue and submit tasks with priorities. A basic worker loop that polls and executes tasks is the core deliverable. Cancellation and retries are not required.

### Mid-level

Mid-level candidates should implement the full flow: priority-based ordering, delayed execution, cancellation via TaskHandle, and retry with exponential backoff. The worker should properly handle exceptions. You should use daemon threads and provide a clean shutdown mechanism.

### Senior

Senior candidates would discuss the limitations of busy-polling for delayed tasks and propose a `DelayQueue` or `ScheduledExecutorService` for more efficient scheduling. Thread pool sizing strategies, fairness guarantees, and preventing starvation of LOW-priority tasks should all be covered. Persistence and crash recovery would round out the design.