# Stack Overflow

**Difficulty:** Hard | **Companies:** Google, Microsoft, LinkedIn

---

## Requirements

> "Design a Q&A platform like Stack Overflow where users can post questions, write answers, vote, comment, and accept answers."

### Clarifying Questions

> **You:** "What are the core content types?"
>
> **Interviewer:** "Questions and Answers. Both support voting and comments. Questions also have tags."

> **You:** "How does voting work?"
>
> **Interviewer:** "Upvote (+1) and downvote (−1). Use ConcurrentHashMap to track who voted what. AtomicInteger for vote score."

> **You:** "Should we track user reputation?"
>
> **Interviewer:** "Yes. +10 for receiving an upvote on a question or answer, −2 for receiving a downvote. Use AtomicInteger."

> **You:** "Only the question author can accept an answer?"
>
> **Interviewer:** "Correct. And only one answer can be accepted per question."

### Final Requirements

```
Requirements:
1. Users can post questions with title, body, and tags
2. Users can post answers to questions
3. Upvote / downvote on questions and answers (one vote per user per post)
4. Comments on questions and answers
5. Accept an answer (only by question author, one per question)
6. User reputation: +10 per upvote received, -2 per downvote received
7. Search questions by keyword or tag
8. Thread-safe voting with ConcurrentHashMap

Out of scope:
- Markdown rendering
- Badges / achievements
- Bounties
- Edit history / revisions
- Moderation tools
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **StackOverflow** | Facade. Manages users, questions, answers, search. |
| **User** | Profile with reputation (AtomicInteger). Posts questions and answers. |
| **Post** | Abstract base for Question and Answer. Holds body, author, votes, comments. |
| **Question** | Extends Post. Has title, tags, answers list, accepted answer reference. |
| **Answer** | Extends Post. References parent question. Can be accepted. |
| **Comment** | Text + author + timestamp on a post. |
| **Vote** | Enum: UPVOTE, DOWNVOTE. Tracked per user per post. |
| **SearchService** | Searches questions by keyword in title/body or by tag. |

---

## Class Design

### Post (abstract base)

```
abstract class Post:
    - postId: String
    - body: String
    - author: User
    - createdAt: Instant
    - voteScore: AtomicInteger
    - votes: ConcurrentHashMap<String, Vote>  // userId → Vote
    - comments: CopyOnWriteArrayList<Comment>

    + vote(user, voteType) → void
    + addComment(comment) → void
    + getScore() → int
```

### Question

```
class Question extends Post:
    - title: String
    - tags: List<String>
    - answers: CopyOnWriteArrayList<Answer>
    - acceptedAnswer: Answer? (volatile)

    + addAnswer(answer) → void
    + acceptAnswer(answer, user) → void  // only author
    + getTags() → List<String>
```

### Answer

```
class Answer extends Post:
    - question: Question
    - accepted: boolean

    + markAccepted() → void
```

### User

```
class User:
    - userId: String
    - username: String
    - email: String
    - reputation: AtomicInteger

    + addReputation(points) → void
    + getReputation() → int
```

### StackOverflow

```
class StackOverflow:
    - users: Map<String, User>
    - questions: Map<String, Question>
    - tagIndex: Map<String, Set<Question>>

    + registerUser(username, email) → User
    + postQuestion(userId, title, body, tags) → Question
    + postAnswer(userId, questionId, body) → Answer
    + vote(userId, postId, voteType) → void
    + acceptAnswer(userId, answerId) → void
    + addComment(userId, postId, text) → Comment
    + searchByKeyword(keyword) → List<Question>
    + searchByTag(tag) → List<Question>
```

---

## Implementation

### Post.vote (thread-safe voting)

```
vote(user, voteType)
    existingVote = votes.get(user.id)
    if existingVote == voteType → throw "Already voted"
    if existingVote != null:
        // Undo previous vote's reputation effect
        reputationDelta = (existingVote == UPVOTE) ? -10 : +2
        author.addReputation(reputationDelta)
        // Undo score
        voteScore.addAndGet(existingVote == UPVOTE ? -1 : +1)

    votes.put(user.id, voteType)
    voteScore.addAndGet(voteType == UPVOTE ? +1 : -1)
    reputationDelta = (voteType == UPVOTE) ? +10 : -2
    author.addReputation(reputationDelta)
```

### Question.acceptAnswer

```
acceptAnswer(answer, user)
    if user != this.author → throw "Only author can accept"
    if acceptedAnswer != null → throw "Already accepted an answer"
    if !answers.contains(answer) → throw "Not an answer to this question"
    this.acceptedAnswer = answer
    answer.markAccepted()
```

### StackOverflow.postQuestion (with tag indexing)

```
postQuestion(userId, title, body, tags)
    user = users.get(userId)
    question = new Question(user, title, body, tags)
    questions.put(question.id, question)
    for each tag in tags:
        tagIndex.computeIfAbsent(tag, k → ConcurrentSet).add(question)
    return question
```

### Complete Code Implementation

```java
public enum Vote {
    UPVOTE(1, 10),
    DOWNVOTE(-1, -2);

    private final int scoreDelta;
    private final int reputationDelta;

    Vote(int scoreDelta, int reputationDelta) {
        this.scoreDelta = scoreDelta;
        this.reputationDelta = reputationDelta;
    }

    public int getScoreDelta() { return scoreDelta; }
    public int getReputationDelta() { return reputationDelta; }
}
```

```java
import java.util.concurrent.atomic.AtomicInteger;

public class User {
    private final String userId;
    private final String username;
    private final String email;
    private final AtomicInteger reputation = new AtomicInteger(0);

    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public void addReputation(int points) { reputation.addAndGet(points); }
    public int getReputation() { return reputation.get(); }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}
```

```java
import java.time.Instant;

public class Comment {
    private final String commentId;
    private final String text;
    private final User author;
    private final Instant createdAt;

    public Comment(String commentId, String text, User author) {
        this.commentId = commentId;
        this.text = text;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public String getCommentId() { return commentId; }
    public String getText() { return text; }
    public User getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
}
```

```java
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Post {
    private final String postId;
    private String body;
    private final User author;
    private final Instant createdAt;
    private final AtomicInteger voteScore = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Vote> votes = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Comment> comments = new CopyOnWriteArrayList<>();

    protected Post(String body, User author) {
        this.postId = UUID.randomUUID().toString();
        this.body = body;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public void vote(User voter, Vote voteType) {
        if (voter.getUserId().equals(author.getUserId()))
            throw new IllegalArgumentException("Cannot vote on your own post");

        Vote existing = votes.get(voter.getUserId());
        if (existing == voteType)
            throw new IllegalArgumentException("Already voted " + voteType);

        // Undo previous vote if changing
        if (existing != null) {
            voteScore.addAndGet(-existing.getScoreDelta());
            author.addReputation(-existing.getReputationDelta());
        }

        votes.put(voter.getUserId(), voteType);
        voteScore.addAndGet(voteType.getScoreDelta());
        author.addReputation(voteType.getReputationDelta());
    }

    public Comment addComment(String text, User commenter) {
        Comment comment = new Comment(UUID.randomUUID().toString(), text, commenter);
        comments.add(comment);
        return comment;
    }

    public String getPostId() { return postId; }
    public String getBody() { return body; }
    public User getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
    public int getScore() { return voteScore.get(); }
    public List<Comment> getComments() { return comments; }
}
```

```java
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Question extends Post {
    private final String title;
    private final List<String> tags;
    private final CopyOnWriteArrayList<Answer> answers = new CopyOnWriteArrayList<>();
    private volatile Answer acceptedAnswer;

    public Question(String title, String body, User author, List<String> tags) {
        super(body, author);
        this.title = title;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
    }

    public void addAnswer(Answer answer) {
        answers.add(answer);
    }

    public void acceptAnswer(Answer answer, User user) {
        if (!user.getUserId().equals(getAuthor().getUserId()))
            throw new IllegalArgumentException("Only the question author can accept an answer");
        if (acceptedAnswer != null)
            throw new IllegalStateException("An answer is already accepted");
        if (!answers.contains(answer))
            throw new IllegalArgumentException("Answer does not belong to this question");
        this.acceptedAnswer = answer;
        answer.markAccepted();
    }

    public String getTitle() { return title; }
    public List<String> getTags() { return tags; }
    public List<Answer> getAnswers() { return answers; }
    public Answer getAcceptedAnswer() { return acceptedAnswer; }
}
```

```java
public class Answer extends Post {
    private final Question question;
    private volatile boolean accepted;

    public Answer(String body, User author, Question question) {
        super(body, author);
        this.question = question;
    }

    public void markAccepted() { this.accepted = true; }
    public boolean isAccepted() { return accepted; }
    public Question getQuestion() { return question; }
}
```

```java
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StackOverflow {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, Question> questions = new ConcurrentHashMap<>();
    private final Map<String, Post> allPosts = new ConcurrentHashMap<>();
    private final Map<String, Set<Question>> tagIndex = new ConcurrentHashMap<>();

    public User registerUser(String username, String email) {
        String userId = UUID.randomUUID().toString();
        User user = new User(userId, username, email);
        users.put(userId, user);
        return user;
    }

    public Question postQuestion(String userId, String title, String body,
                                  List<String> tags) {
        User user = getUser(userId);
        Question question = new Question(title, body, user, tags);
        questions.put(question.getPostId(), question);
        allPosts.put(question.getPostId(), question);

        for (String tag : tags) {
            tagIndex.computeIfAbsent(tag.toLowerCase(),
                k -> ConcurrentHashMap.newKeySet()).add(question);
        }
        return question;
    }

    public Answer postAnswer(String userId, String questionId, String body) {
        User user = getUser(userId);
        Question question = questions.get(questionId);
        if (question == null)
            throw new IllegalArgumentException("Question not found");
        Answer answer = new Answer(body, user, question);
        question.addAnswer(answer);
        allPosts.put(answer.getPostId(), answer);
        return answer;
    }

    public void vote(String voterId, String postId, Vote voteType) {
        User voter = getUser(voterId);
        Post post = allPosts.get(postId);
        if (post == null)
            throw new IllegalArgumentException("Post not found");
        post.vote(voter, voteType);
    }

    public void acceptAnswer(String userId, String answerId) {
        User user = getUser(userId);
        Post post = allPosts.get(answerId);
        if (!(post instanceof Answer))
            throw new IllegalArgumentException("Not an answer");
        Answer answer = (Answer) post;
        answer.getQuestion().acceptAnswer(answer, user);
    }

    public Comment addComment(String userId, String postId, String text) {
        User user = getUser(userId);
        Post post = allPosts.get(postId);
        if (post == null)
            throw new IllegalArgumentException("Post not found");
        return post.addComment(text, user);
    }

    public List<Question> searchByKeyword(String keyword) {
        String lower = keyword.toLowerCase();
        return questions.values().stream()
            .filter(q -> q.getTitle().toLowerCase().contains(lower)
                      || q.getBody().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public List<Question> searchByTag(String tag) {
        Set<Question> result = tagIndex.get(tag.toLowerCase());
        return result == null ? Collections.emptyList() : new ArrayList<>(result);
    }

    private User getUser(String userId) {
        User user = users.get(userId);
        if (user == null)
            throw new IllegalArgumentException("User not found");
        return user;
    }
}
```

### Verification

```
Setup: StackOverflow system.

Step 1: Register users
  User A = register("alice", "alice@test.com"). reputation=0
  User B = register("bob", "bob@test.com"). reputation=0

Step 2: postQuestion(A, "Java Concurrency", "How to use locks?", ["java", "concurrency"])
  Question Q1 created. tagIndex: {java: {Q1}, concurrency: {Q1}} ✓

Step 3: postAnswer(B, Q1.id, "Use ReentrantLock for mutual exclusion...")
  Answer ANS1 created. Q1.answers = [ANS1] ✓

Step 4: vote(B, Q1.id, UPVOTE) — Bob upvotes Alice's question
  Q1.voteScore: 0→1. Alice.reputation: 0→10 ✓

Step 5: vote(A, ANS1.id, UPVOTE) — Alice upvotes Bob's answer
  ANS1.voteScore: 0→1. Bob.reputation: 0→10 ✓

Step 6: vote(A, Q1.id, UPVOTE) → throws "Cannot vote on your own post" ✓

Step 7: vote(B, Q1.id, UPVOTE) → throws "Already voted UPVOTE" ✓

Step 8: vote(B, Q1.id, DOWNVOTE) — Bob changes vote to downvote
  Undo upvote: score 1→0, Alice rep 10→0.
  Apply downvote: score 0→-1, Alice rep 0→-2. ✓

Step 9: addComment(B, Q1.id, "Can you be more specific?")
  Comment added to Q1.comments ✓

Step 10: acceptAnswer(A, ANS1.id) — Alice accepts Bob's answer
  A == Q1.author ✓. acceptedAnswer is null ✓.
  Q1.acceptedAnswer = ANS1. ANS1.accepted = true ✓

Step 11: acceptAnswer(B, ANS1.id) → throws "Only question author can accept" ✓

Step 12: searchByTag("java") → [Q1] ✓
Step 13: searchByKeyword("locks") → [Q1] (body contains "locks") ✓
```

---

## Extensibility

### 1. "How would you add a bounty system?"

> "I'd create a Bounty class tied to a Question with an amount (deducted from the asker's reputation), an expiry, and a state (ACTIVE, AWARDED, EXPIRED). When expired without manual award, the bounty auto-awards to the highest-scored answer. The bounty reputation is added to the awardee. This encourages answers on difficult questions."

### 2. "How would you add edit history?"

> "Each Post gets a List of Revision objects, each holding the previous body, editor, and timestamp. On edit, the current body becomes a new Revision before the body is updated. Users can view the diff between any two revisions. An EditPolicy controls who can edit (author, high-rep users, moderators)."

### 3. "How would you implement a recommendation feed?"

> "I'd track each user's tags of interest (from their questions, answers, and followed tags). For the feed, I'd score questions by: tag relevance × recency × vote score. An inverted index maps tags to questions sorted by score. Pagination uses a cursor. For scale, this becomes a precomputed feed stored per user, updated asynchronously."

---

## What is Expected at Each Level?

### Junior

At the junior level, you should model User, Question, Answer, and Comment correctly with proper relationships. Posting questions and answers, adding comments, and basic vote counting (without concurrency) are the key goals. The abstract Post base class should be identified.

### Mid-level

Mid-level candidates should use ConcurrentHashMap for votes to prevent duplicate voting, AtomicInteger for reputation and vote scores, and implement vote-change logic (undo previous, apply new). Tag-based search with an inverted index and the accept-answer authorization check should be demonstrated.

### Senior

Senior candidates would discuss full-text search integration (Elasticsearch), eventual consistency for vote counts in a distributed system, and caching strategies for hot questions. The trade-off between strong consistency per-post voting and eventual consistency for reputation aggregation should be analyzed. Rate limiting on votes, spam detection, and the moderation workflow are advanced topics.