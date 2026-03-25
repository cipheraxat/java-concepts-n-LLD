# Pub/Sub Messaging System

**Difficulty:** Medium | **Companies:** Google, Amazon, LinkedIn

---

## Requirements

> "Design an in-process publish-subscribe messaging system. Publishers send messages to topics. Subscribers subscribe to topics and receive messages asynchronously."

### Clarifying Questions

> **You:** "In-process or distributed?"
>
> **Interviewer:** "In-process. Single JVM. Focus on the core fanout and async delivery model."

> **You:** "Do subscribers receive messages asynchronously?"
>
> **Interviewer:** "Yes. Each subscriber gets messages delivered on a separate thread so slow subscribers don't block publishers."

> **You:** "Do we need subscriber groups (like Kafka consumer groups)?"
>
> **Interviewer:** "Yes. Within a group, only one subscriber gets each message (round-robin). Across groups, every group gets every message."

> **You:** "What about message ordering?"
>
> **Interviewer:** "Messages are delivered in publish order within a single topic."

### Final Requirements

```
Requirements:
1. Create topics
2. Publishers publish messages to a topic
3. Subscribers subscribe to a topic (individually or as part of a group)
4. Fanout: every individual subscriber and every group gets each message
5. Within a group: round-robin delivery to one member
6. Async delivery — publishers don't block on slow subscribers
7. Subscribers can unsubscribe

Out of scope:
- Persistence / message replay
- Distributed messaging
- Message acknowledgment / retry
- Dead letter queues
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **MessageBroker** | Facade. Creates topics, manages subscriptions, routes publish calls. |
| **Topic** | Holds subscriber list and group list. Delivers messages to all subscribers and groups. |
| **Message** | Immutable payload with an ID and timestamp. |
| **Subscriber** | Interface with an `onMessage` callback. Each subscriber has an internal message queue and a delivery thread. |
| **SubscriberGroup** | Groups multiple subscribers. Delivers each message to exactly one member via round-robin. |

---

## Class Design

### Topic

| Requirement | What Topic must track |
|-------------|----------------------|
| "Fanout to all subscribers" | CopyOnWriteArrayList of Subscriber |
| "Fanout to all groups" | CopyOnWriteArrayList of SubscriberGroup |
| "In-order delivery" | Per-subscriber BlockingQueue preserves order |

```
class Topic:
    - name: String
    - subscribers: CopyOnWriteArrayList<Subscriber>
    - groups: CopyOnWriteArrayList<SubscriberGroup>

    + publish(message) → void
    + addSubscriber(subscriber) → void
    + addGroup(group) → void
    + removeSubscriber(subscriber) → void
```

### Subscriber

```
class Subscriber:
    - subscriberId: String
    - handler: MessageHandler
    - queue: LinkedBlockingQueue<Message>
    - thread: Thread

    + start() → void
    + stop() → void
    + enqueue(message) → void

interface MessageHandler:
    + handle(message) → void
```

### SubscriberGroup

```
class SubscriberGroup:
    - groupId: String
    - members: List<Subscriber>
    - roundRobinIndex: AtomicInteger

    + deliver(message) → void
    + addMember(subscriber) → void
    + removeMember(subscriber) → void
```

---

## Implementation

### Topic.publish

**Core logic:** Fanout to every individual subscriber's queue, plus one member per group.

```
publish(message)
    for each subscriber in subscribers:
        subscriber.enqueue(message)

    for each group in groups:
        group.deliver(message)
```

### SubscriberGroup.deliver (round-robin)

```
deliver(message)
    if members is empty → return
    index = roundRobinIndex.getAndIncrement() % members.size()
    members.get(index).enqueue(message)
```

### Subscriber delivery thread

```
run()
    while running:
        message = queue.poll(1, SECONDS)
        if message != null:
            handler.handle(message)
```

### Complete Code Implementation

```java
import java.time.Instant;

public class Message {
    private final String messageId;
    private final String payload;
    private final Instant timestamp;

    public Message(String messageId, String payload) {
        this.messageId = messageId;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public String getMessageId() { return messageId; }
    public String getPayload()   { return payload; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() { return "[" + messageId + "] " + payload; }
}
```

```java
public interface MessageHandler {
    void handle(Message message);
}
```

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Subscriber {
    private final String subscriberId;
    private final MessageHandler handler;
    private final LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;
    private Thread thread;

    public Subscriber(String subscriberId, MessageHandler handler) {
        this.subscriberId = subscriberId;
        this.handler = handler;
    }

    public void start() {
        running = true;
        thread = new Thread(() -> {
            while (running) {
                try {
                    Message msg = queue.poll(1, TimeUnit.SECONDS);
                    if (msg != null) handler.handle(msg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Subscriber-" + subscriberId);
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (thread != null) thread.interrupt();
    }

    public void enqueue(Message message) {
        queue.offer(message);
    }

    public String getSubscriberId() { return subscriberId; }
}
```

```java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscriberGroup {
    private final String groupId;
    private final List<Subscriber> members = new CopyOnWriteArrayList<>();
    private final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    public SubscriberGroup(String groupId) {
        this.groupId = groupId;
    }

    public void deliver(Message message) {
        if (members.isEmpty()) return;
        int index = Math.abs(roundRobinIndex.getAndIncrement()) % members.size();
        members.get(index).enqueue(message);
    }

    public void addMember(Subscriber subscriber) { members.add(subscriber); }
    public void removeMember(Subscriber subscriber) { members.remove(subscriber); }
    public String getGroupId() { return groupId; }
}
```

```java
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Topic {
    private final String name;
    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    private final List<SubscriberGroup> groups = new CopyOnWriteArrayList<>();

    public Topic(String name) { this.name = name; }

    public void publish(Message message) {
        for (Subscriber sub : subscribers) {
            sub.enqueue(message);
        }
        for (SubscriberGroup group : groups) {
            group.deliver(message);
        }
    }

    public void addSubscriber(Subscriber subscriber) { subscribers.add(subscriber); }
    public void removeSubscriber(Subscriber subscriber) { subscribers.remove(subscriber); }
    public void addGroup(SubscriberGroup group) { groups.add(group); }
    public void removeGroup(SubscriberGroup group) { groups.remove(group); }

    public String getName() { return name; }
}
```

```java
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class MessageBroker {
    private final Map<String, Topic> topics = new HashMap<>();
    private final AtomicLong messageCounter = new AtomicLong(1);

    public Topic createTopic(String name) {
        Topic topic = new Topic(name);
        topics.put(name, topic);
        return topic;
    }

    public void publish(String topicName, String payload) {
        Topic topic = topics.get(topicName);
        if (topic == null) throw new IllegalArgumentException("Topic not found: " + topicName);
        Message message = new Message("M-" + messageCounter.getAndIncrement(), payload);
        topic.publish(message);
    }

    public void subscribe(String topicName, Subscriber subscriber) {
        Topic topic = topics.get(topicName);
        if (topic == null) throw new IllegalArgumentException("Topic not found: " + topicName);
        topic.addSubscriber(subscriber);
        subscriber.start();
    }

    public void subscribeGroup(String topicName, SubscriberGroup group) {
        Topic topic = topics.get(topicName);
        if (topic == null) throw new IllegalArgumentException("Topic not found: " + topicName);
        topic.addGroup(group);
        for (Subscriber member : List.of()) {
            // Members should be started when added to the group
        }
    }

    public void unsubscribe(String topicName, Subscriber subscriber) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            topic.removeSubscriber(subscriber);
            subscriber.stop();
        }
    }

    public Topic getTopic(String name) { return topics.get(name); }
}
```

### Verification

```
Setup: Topic "orders".
Individual subscribers: S1, S2.
Group G1 with members: S3, S4.

Step 1: Publish "Order-100" to "orders"
  Fanout to individuals: S1.enqueue(M-1), S2.enqueue(M-1)
  Fanout to groups: G1.deliver(M-1)
    roundRobin index=0 → S3.enqueue(M-1)

  Result: S1 gets M-1, S2 gets M-1, S3 gets M-1 (S4 does not)

Step 2: Publish "Order-101" to "orders"
  S1.enqueue(M-2), S2.enqueue(M-2)
  G1.deliver(M-2): index=1 → S4.enqueue(M-2)

  Result: S1 gets M-2, S2 gets M-2, S4 gets M-2 (S3 does not, round-robin)

Step 3: Publish "Order-102"
  G1.deliver(M-3): index=2 % 2 = 0 → S3.enqueue(M-3)

Step 4: S1 is slow (handler sleeps 5s)
  S1's queue buffers messages. Publisher returns immediately.
  S2, S3, S4 are unaffected — each has its own queue and thread. ✓

Step 5: Unsubscribe S2
  topic.removeSubscriber(S2). S2.stop().
  Next publish: only S1 receives individually. G1 still active.
```

---

## Extensibility

### 1. "How would you add message acknowledgment and retry?"

> "Each Subscriber tracks unacknowledged messages in a pending set with a timeout. If not ack'd within the timeout, the message is re-delivered. For groups, an unacknowledged message is rerouted to another group member. This gives at-least-once delivery semantics."

### 2. "How would you add message filtering?"

> "Subscribers register with a `MessageFilter` predicate. Before enqueuing, the Topic checks if the message passes the subscriber's filter. This keeps the subscriber's queue clean and avoids unnecessary processing."

### 3. "How would you support multiple topics per subscriber with different handlers?"

> "I'd allow a Subscriber to hold a `Map<String, MessageHandler>` keyed by topic name. When a message arrives, the subscriber's delivery loop routes to the correct handler based on which topic the message came from. The Message would carry the topic name as metadata."

---

## What is Expected at Each Level?

### Junior

At the junior level, a working topic → subscriber fanout with synchronous delivery is fine. Publishers call each subscriber's handler directly. The subscriber list can be a simple ArrayList. Async delivery and subscriber groups aren't required.

### Mid-level

Mid-level candidates should implement async delivery using per-subscriber BlockingQueues with dedicated threads. CopyOnWriteArrayList ensures thread-safe subscribe/unsubscribe during publishing. The round-robin SubscriberGroup should work correctly. You should discuss ordering guarantees.

### Senior

Senior candidates would discuss backpressure (what happens when a subscriber's queue fills up), bounded queues with blocking vs. dropping policies, and how to scale to many topics without a thread per subscriber (thread pool with fair scheduling). Fan-out performance in high-throughput scenarios should be addressed.