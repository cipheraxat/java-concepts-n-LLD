# Lesson 04 — Exception Handling (SDE2 Interview)

## Topics Covered
1. Exception hierarchy (Throwable → Error / Exception → RuntimeException)
2. Checked vs Unchecked exceptions
3. try-catch-finally execution order
4. try-with-resources (AutoCloseable)
5. Multi-catch blocks
6. Custom exceptions (checked & unchecked)
7. Exception chaining (`initCause`, `cause` constructor)
8. Best practices — what to do and NOT do

## Exception Hierarchy

```
Throwable
├── Error           (unchecked — JVM issues, don't catch: OutOfMemoryError, StackOverflowError)
└── Exception
    ├── RuntimeException  (unchecked — programming errors: NPE, AIOOBE, ClassCastException)
    └── [Checked exceptions]  (must declare/handle: IOException, SQLException, etc.)
```

## Key Interview Points

| Question | Answer |
|---|---|
| Checked vs Unchecked? | Checked: compiler enforces declare/handle (IOException). Unchecked: RuntimeException subclasses, programmer's fault |
| When does `finally` NOT run? | `System.exit()`, JVM crash, or infinite loop in try/catch before finally |
| try-with-resources vs finally? | TWR auto-closes in reverse order; if both TWR close and catch throw, TWR suppresses close exception |
| Can `return` in `finally` override try's return? | YES — finally's return replaces try's return value (avoid this!) |
| Best practice for custom exceptions? | Extend RuntimeException (unchecked) for app errors; always include original cause |
| What is exception chaining? | Wrapping original exception as cause so root cause is not lost |

## Files in this Lesson
- `ExceptionHierarchyDemo.java` — hierarchy, checked/unchecked, common exceptions
- `TryWithResourcesDemo.java` — AutoCloseable, resource ordering, suppressed exceptions
- `CustomExceptionDemo.java` — custom exceptions, chaining, best practices
