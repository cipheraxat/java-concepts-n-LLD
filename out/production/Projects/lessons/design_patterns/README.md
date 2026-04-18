# Lesson 07 — Design Patterns (SDE2 Interview)

## Topics Covered

### Creational
1. **Singleton** — single instance, thread safety, double-checked locking
2. **Factory Method** — subclass decides which class to create
3. **Abstract Factory** — family of related objects
4. **Builder** — construct complex objects step-by-step

### Structural
5. **Decorator** — add behavior dynamically (extends over inheritance)
6. **Adapter** — bridge incompatible interfaces
7. **Proxy** — control access to another object (lazy init, logging, security)
8. **Composite** — tree structures of uniform interface

### Behavioral
9. **Observer** — event-driven notifications (pub/sub)
10. **Strategy** — swap algorithms at runtime
11. **Template Method** — skeleton algorithm, subclass fills in steps
12. **Command** — encapsulate request as object

## Key Interview Points

| Pattern | When to use | Java Example |
|---|---|---|
| Singleton | One shared resource (config, registry) | Runtime.getRuntime(), Spring @Component |
| Builder | Many optional constructor params | StringBuilder, Lombok @Builder, HttpRequest |
| Factory | Decouple creation from usage | Calendar.getInstance(), Collections.sort() |
| Observer | Decoupled event notification | Java events, Spring ApplicationEvent |
| Strategy | Interchangeable algorithms | Comparator, LayoutManager |
| Decorator | Add behavior without inheritance | BufferedReader(FileReader), Stream pipeline |
| Template Method | Shared skeleton, diff steps | AbstractList, Servlet |

## Files in this Lesson
- `SingletonDemo.java` — 5 ways to implement, thread safety, break-proofing
- `BuilderFactoryDemo.java` — Builder + Factory patterns
- `ObserverStrategyDemo.java` — Observer + Strategy patterns
- `DecoratorProxyDemo.java` — Decorator + Proxy patterns
