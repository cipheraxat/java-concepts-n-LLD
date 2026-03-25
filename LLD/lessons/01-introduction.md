# Lesson 1: Introduction to Low-Level Design

---

## What is Low-Level Design (LLD)?

Low-Level Design (also called **Object-Oriented Design** or **Machine Coding**) tests your ability to translate a vague problem statement into clean, working, object-oriented code under time pressure (~45 min).

It is different from High-Level Design (HLD/System Design), which focuses on distributed systems, scalability, and infrastructure. LLD focuses on:

- **Classes and their responsibilities**
- **Relationships between objects**
- **Design patterns and principles**
- **Clean, readable, extensible code**

---

## Two Interview Variants

| Variant | Description | Duration | Deliverable |
|---------|-------------|----------|-------------|
| **Whiteboard OOD** | Design classes and relationships on a whiteboard | ~45 min | Class diagram + pseudocode |
| **Machine Coding** | Write actual working code in an IDE | 60–90 min | Compiling, runnable code |

---

## Assessment Criteria

Interviewers evaluate you on five dimensions:

| Criterion | What They Look For |
|-----------|-------------------|
| **Problem Exploration** | Do you ask clarifying questions? Do you scope the problem correctly? |
| **Data Modeling** | Are entities well-chosen? Are relationships clean? |
| **Class Design** | Is state minimal and behavior well-placed? Does each class have a single responsibility? |
| **Code Quality** | Clean, readable, idiomatic Java? Meaningful names? No code smells? |
| **Extensibility** | Can the design handle new requirements without major rewrites? |

---

## Key Insight

> LLD is not about getting the "right" answer — multiple valid designs exist. What matters is **structured thinking**, **clean communication**, and **principled tradeoffs**.

---

## What LLD is NOT

- Not about databases or SQL queries
- Not about HTTP APIs or REST design
- Not about distributed systems or load balancing
- Not about UI/frontend code
- Not about authentication/authorization systems

Keep your scope narrow — model only what the requirements demand.

---

## Java in LLD Interviews

All code in this plan is in **Java**. Key Java features commonly used in LLD:

| Feature | Common Use |
|---------|-----------|
| `enum` | Fixed sets of values (Status, Direction, Type) |
| `interface` | Define contracts, enable polymorphism |
| `abstract class` | Shared state + behavior with partial implementation |
| `Optional<T>` | Nullable return values without null checks |
| `List`, `Map`, `Set`, `Queue`, `Deque` | Standard data structures |
| `PriorityQueue` | Ordered processing (schedulers, elevators) |
| `synchronized`, `ReentrantLock` | Thread safety |
| `AtomicInteger`, `AtomicLong` | Lock-free atomic operations |
| Generics `<T>` | Type-safe reusable containers (LRU cache) |

---

## Next Steps

Proceed to [Lesson 2: Delivery Framework](02-delivery-framework.md) to learn the 5-step structured approach for solving any LLD problem.
