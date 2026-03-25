# Logger System

**Difficulty:** Medium | **Companies:** Amazon, Google, Microsoft

---

## Requirements

> "Design a logging framework similar to Log4j. Support multiple log levels, multiple output destinations (console, file), configurable formatting, and async logging."

### Clarifying Questions

> **You:** "What log levels?"
>
> **Interviewer:** "DEBUG, INFO, WARN, ERROR, FATAL — the standard five. A logger has a minimum level and ignores anything below it."

> **You:** "Multiple appenders or just one?"
>
> **Interviewer:** "Multiple. A logger can write to console and file simultaneously. Appenders are pluggable."

> **You:** "Synchronous or asynchronous logging?"
>
> **Interviewer:** "Support both. Async logging uses a background thread with a queue so the caller isn't blocked."

> **You:** "Should we support structured or customizable formatting?"
>
> **Interviewer:** "Yes — strategy pattern for formatters. Support simple text and JSON formats."

### Final Requirements

```
Requirements:
1. Log levels: DEBUG, INFO, WARN, ERROR, FATAL with severity ordering
2. Logger has a configurable minimum level — filters messages below it
3. Multiple appenders: ConsoleAppender, FileAppender (pluggable)
4. Pluggable formatter strategy: SimpleFormatter, JsonFormatter
5. Async logging via background queue + dispatch thread
6. Global logger access (Singleton)

Out of scope:
- Log rotation / archival
- Remote log shipping
- Logger hierarchy / namespaces
- MDC (mapped diagnostic context)
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **Logger** | Singleton entry point. Accepts log messages, filters by level, delegates to appenders. |
| **LogLevel** | Enum: DEBUG < INFO < WARN < ERROR < FATAL. Used for filtering. |
| **LogMessage** | Immutable record: level, message, timestamp, thread name. |
| **Appender** | Interface for output destinations. Each appender has a formatter. |
| **ConsoleAppender** | Writes formatted messages to stdout. |
| **FileAppender** | Writes formatted messages to a file. |
| **LogFormatter** | Strategy interface for formatting a LogMessage into a string. |
| **AsyncAppender** | Wraps another appender with a BlockingQueue and background thread. |

---

## Class Design

### Logger (Singleton)

| Requirement | What Logger must track |
|-------------|----------------------|
| "Minimum level filtering" | LogLevel minLevel |
| "Multiple appenders" | List of Appender |

```
class Logger:
    - static instance: Logger  (Bill Pugh Singleton)
    - minLevel: LogLevel
    - appenders: List<Appender>

    + static getInstance() → Logger
    + setLevel(level) → void
    + addAppender(appender) → void
    + debug/info/warn/error/fatal(message) → void
    - log(level, message) → void
```

### Appender & Formatter

```
interface Appender:
    + append(logMessage) → void
    + close() → void

class ConsoleAppender implements Appender:
    - formatter: LogFormatter
    + append(msg) → System.out.println(formatter.format(msg))

class FileAppender implements Appender:
    - formatter: LogFormatter
    - writer: BufferedWriter
    + append(msg) → writer.write(formatter.format(msg))
    + close() → writer.close()

interface LogFormatter:
    + format(logMessage) → String

class SimpleFormatter implements LogFormatter:
    + format(msg) → "[LEVEL] TIMESTAMP [THREAD] message"

class JsonFormatter implements LogFormatter:
    + format(msg) → {"level":"...", "timestamp":"...", "message":"..."}
```

### AsyncAppender

```
class AsyncAppender implements Appender:
    - delegate: Appender
    - queue: LinkedBlockingQueue<LogMessage>
    - thread: Thread
    - running: volatile boolean

    + append(msg) → queue.offer(msg)
    + close() → stop thread, flush remaining, close delegate
```

---

## Implementation

### Logger.log

```
log(level, message)
    if level < minLevel → return
    logMessage = new LogMessage(level, message, now, currentThread)
    for each appender in appenders:
        appender.append(logMessage)
```

### AsyncAppender dispatch loop

```
run()
    while running or queue is not empty:
        msg = queue.poll(100, MILLISECONDS)
        if msg != null:
            delegate.append(msg)
```

### Complete Code Implementation

```java
public enum LogLevel {
    DEBUG(0), INFO(1), WARN(2), ERROR(3), FATAL(4);

    private final int severity;
    LogLevel(int severity) { this.severity = severity; }
    public int getSeverity() { return severity; }
}
```

```java
import java.time.Instant;

public class LogMessage {
    private final LogLevel level;
    private final String message;
    private final Instant timestamp;
    private final String threadName;

    public LogMessage(LogLevel level, String message) {
        this.level = level;
        this.message = message;
        this.timestamp = Instant.now();
        this.threadName = Thread.currentThread().getName();
    }

    public LogLevel getLevel()     { return level; }
    public String getMessage()     { return message; }
    public Instant getTimestamp()   { return timestamp; }
    public String getThreadName()  { return threadName; }
}
```

```java
public interface LogFormatter {
    String format(LogMessage message);
}

public class SimpleFormatter implements LogFormatter {
    @Override
    public String format(LogMessage msg) {
        return String.format("[%s] %s [%s] %s",
            msg.getLevel(), msg.getTimestamp(), msg.getThreadName(), msg.getMessage());
    }
}

public class JsonFormatter implements LogFormatter {
    @Override
    public String format(LogMessage msg) {
        return String.format("{\"level\":\"%s\",\"timestamp\":\"%s\",\"thread\":\"%s\",\"message\":\"%s\"}",
            msg.getLevel(), msg.getTimestamp(), msg.getThreadName(),
            msg.getMessage().replace("\"", "\\\""));
    }
}
```

```java
public interface Appender {
    void append(LogMessage message);
    void close();
}
```

```java
public class ConsoleAppender implements Appender {
    private final LogFormatter formatter;

    public ConsoleAppender(LogFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void append(LogMessage message) {
        System.out.println(formatter.format(message));
    }

    @Override
    public void close() { /* no-op */ }
}
```

```java
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileAppender implements Appender {
    private final LogFormatter formatter;
    private final BufferedWriter writer;

    public FileAppender(String filePath, LogFormatter formatter) throws IOException {
        this.formatter = formatter;
        this.writer = new BufferedWriter(new FileWriter(filePath, true));
    }

    @Override
    public synchronized void append(LogMessage message) {
        try {
            writer.write(formatter.format(message));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try { writer.close(); } catch (IOException ignored) {}
    }
}
```

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsyncAppender implements Appender {
    private final Appender delegate;
    private final LinkedBlockingQueue<LogMessage> queue;
    private final Thread dispatchThread;
    private volatile boolean running = true;

    public AsyncAppender(Appender delegate, int queueCapacity) {
        this.delegate = delegate;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.dispatchThread = new Thread(this::dispatch, "AsyncLogger");
        this.dispatchThread.setDaemon(true);
        this.dispatchThread.start();
    }

    @Override
    public void append(LogMessage message) {
        if (!queue.offer(message)) {
            System.err.println("Log queue full, dropping message");
        }
    }

    private void dispatch() {
        while (running || !queue.isEmpty()) {
            try {
                LogMessage msg = queue.poll(100, TimeUnit.MILLISECONDS);
                if (msg != null) delegate.append(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Flush remaining
        LogMessage msg;
        while ((msg = queue.poll()) != null) {
            delegate.append(msg);
        }
    }

    @Override
    public void close() {
        running = false;
        try { dispatchThread.join(5000); } catch (InterruptedException ignored) {}
        delegate.close();
    }
}
```

```java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    private LogLevel minLevel = LogLevel.DEBUG;
    private final List<Appender> appenders = new CopyOnWriteArrayList<>();

    private Logger() {}

    private static class Holder {
        private static final Logger INSTANCE = new Logger();
    }

    public static Logger getInstance() { return Holder.INSTANCE; }

    public void setLevel(LogLevel level) { this.minLevel = level; }

    public void addAppender(Appender appender) { appenders.add(appender); }

    public void removeAppender(Appender appender) {
        appender.close();
        appenders.remove(appender);
    }

    public void debug(String message) { log(LogLevel.DEBUG, message); }
    public void info(String message)  { log(LogLevel.INFO, message); }
    public void warn(String message)  { log(LogLevel.WARN, message); }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void fatal(String message) { log(LogLevel.FATAL, message); }

    private void log(LogLevel level, String message) {
        if (level.getSeverity() < minLevel.getSeverity()) return;
        LogMessage logMessage = new LogMessage(level, message);
        for (Appender appender : appenders) {
            appender.append(logMessage);
        }
    }

    public void shutdown() {
        for (Appender appender : appenders) appender.close();
    }
}
```

### Verification

```
Setup: Logger with minLevel=WARN.
Appenders: ConsoleAppender(SimpleFormatter), AsyncAppender wrapping FileAppender(JsonFormatter).

Step 1: logger.debug("This is debug")
  level=DEBUG(0) < minLevel=WARN(2) → filtered out. No output. ✓

Step 2: logger.info("Server started")
  level=INFO(1) < WARN(2) → filtered out. ✓

Step 3: logger.warn("Disk usage high")
  level=WARN(2) >= WARN(2) → pass
  ConsoleAppender: [WARN] 2024-01-15T... [main] Disk usage high
  AsyncAppender: queue.offer(msg) → returns immediately
    dispatch thread: polls msg → FileAppender writes JSON:
    {"level":"WARN","timestamp":"...","thread":"main","message":"Disk usage high"}

Step 4: logger.error("Connection failed")
  level=ERROR(3) >= WARN(2) → pass
  Both appenders receive. Async appender doesn't block caller. ✓

Step 5: Rapid burst of 10000 ERROR messages
  ConsoleAppender: prints each synchronously (slow)
  AsyncAppender: queue buffers. If queue fills, drops with stderr warning. ✓
  Dispatch thread processes queue at its own pace.

Step 6: logger.shutdown()
  AsyncAppender: running=false, join thread, flush remaining queue, close file writer.
```

---

## Extensibility

### 1. "How would you add log rotation?"

> "I'd create a `RotatingFileAppender` that wraps FileAppender. It tracks the current file size. When it exceeds a threshold, it closes the current writer, renames the file (e.g., app.log.1), and opens a new one. Rotation count is configurable. This is transparent to the Logger."

### 2. "How would you add per-class logger namespaces?"

> "I'd add `Logger.getLogger(className)` that returns a named logger. Named loggers form a hierarchy (e.g., `com.app.service` inherits from `com.app`). Each logger can override the min level and appenders. Unset loggers inherit from the parent — this is the Log4j pattern."

### 3. "How would you add structured logging with context?"

> "I'd add an MDC (Mapped Diagnostic Context) using a ThreadLocal `Map<String, String>`. The LogMessage captures a snapshot of the MDC at creation time. Formatters can include MDC fields. This enables request-scoped fields like requestId or userId without passing them through every method."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model LogLevel with severity ordering and a Logger with a minimum level filter. A single appender (console) with basic formatting is fine. The Singleton pattern is a plus. Async logging is not required.

### Mid-level

Mid-level candidates should implement the Appender interface with Console and File implementations, use the Strategy pattern for formatters, and apply the Bill Pugh Singleton for thread-safe lazy initialization. The Logger should support multiple appenders simultaneously. Async logging via BlockingQueue is expected.

### Senior

Senior candidates would discuss the trade-offs of async logging — queue sizing, drop policies (drop vs. block), and graceful shutdown (flush remaining queue). You'd explain the performance implications of synchronous file I/O vs. async batching. Adding MDC, logger hierarchies, and dynamic level configuration at runtime should all be covered.