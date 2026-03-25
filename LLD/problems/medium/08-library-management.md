# Library Management System

**Difficulty:** Medium | **Companies:** Amazon, Microsoft

---

## Requirements

> "Design the backend for a library management system. Members can search for books, borrow copies, return them with late fees, and reserve books that are currently checked out."

### Clarifying Questions

> **You:** "Physical books only, or digital too?"
>
> **Interviewer:** "Physical books only. Each book can have multiple copies."

> **You:** "What search capabilities are needed?"
>
> **Interviewer:** "Search by title, author, or ISBN."

> **You:** "What's the loan policy?"
>
> **Interviewer:** "14-day loan period. Late fee of $0.50 per day. Members can have at most 5 active loans."

> **You:** "What happens when all copies of a book are checked out?"
>
> **Interviewer:** "Members can reserve it. When a copy is returned, the first person in the reservation queue is notified."

> **You:** "Do we need admin operations?"
>
> **Interviewer:** "Yes — add and remove books and copies."

### Final Requirements

```
Requirements:
1. Books have metadata (title, author, ISBN) and multiple physical copies
2. Search by title, author, or ISBN
3. Members borrow a copy (14-day loan, max 5 active loans per member)
4. Late fee: $0.50 per day overdue
5. Reserve a book when all copies are out (FIFO queue)
6. On return: auto-notify the next person in reservation queue
7. Admin: add/remove books and copies

Out of scope:
- Digital/e-books
- Fine payment processing
- UI / notification delivery mechanism
- Inter-library loans
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **Library** | Main facade. Owns lookup indices (ISBN→Book, memberId→Member, loanId→Loan). All borrowing, returning, and reservation flows go through here. |
| **Book** | Catalogue entry grouping metadata + a list of BookCopy objects + a reservation queue. Shared metadata lives once here. |
| **BookCopy** | A physical copy with a mutable status (AVAILABLE, BORROWED, RESERVED). Status transitions live here. |
| **Member** | Library user. Tracks name and active loans list for borrow-limit enforcement. |
| **Loan** | Borrow lifecycle record. Links a member to a copy with borrow/due/return dates. Calculates late fee. |
| **NotificationService** | Interface for notifying members. Injected into Library for testability. |

---

## Class Design

### Library

| Requirement | What Library must track |
|-------------|------------------------|
| "Search by ISBN, title, author" | Maps: isbn→Book, plus stream filters on title/author |
| "Borrow / return / reserve" | Map: loanId→Loan for active loans |
| "Member management" | Map: memberId→Member |

```
class Library:
    - booksByIsbn: Map<String, Book>
    - members: Map<String, Member>
    - activeLoans: Map<String, Loan>
    - notificationService: NotificationService
    - loanCounter: AtomicLong

    + borrowBook(memberId, isbn) → Optional<Loan>
    + returnBook(loanId) → double
    + reserveBook(memberId, isbn) → boolean
    + searchByTitle(title) → List<Book>
    + searchByAuthor(author) → List<Book>
    + searchByIsbn(isbn) → Optional<Book>
    + addBook(book) → void
    + addMember(member) → void
```

### Book

| Requirement | What Book must track |
|-------------|---------------------|
| "Multiple copies" | List of BookCopy |
| "FIFO reservation queue" | Queue of Member |

```
class Book:
    - isbn: String
    - title: String
    - author: String
    - copies: List<BookCopy>
    - reservationQueue: Queue<Member>

    + getAvailableCopy() → Optional<BookCopy>
    + enqueueReservation(member) → void
    + pollNextReservation() → Optional<Member>
```

### BookCopy

```
class BookCopy:
    - copyId: String
    - book: Book
    - status: CopyStatus

    + markBorrowed() → void
    + markAvailable() → void
    + markReserved() → void

enum CopyStatus: AVAILABLE, BORROWED, RESERVED
```

### Loan

```
class Loan:
    - loanId: String
    - member: Member
    - copy: BookCopy
    - borrowDate: LocalDate
    - dueDate: LocalDate
    - returnDate: LocalDate?
    - returned: boolean

    + calculateFee() → double
    + isOverdue() → boolean
    + markReturned() → void
```

### Final Class Design

```
class Library:
    - booksByIsbn: Map<String, Book>
    - members: Map<String, Member>
    - activeLoans: Map<String, Loan>
    - notificationService: NotificationService

    + borrowBook(memberId, isbn) → Optional<Loan>
    + returnBook(loanId) → double
    + reserveBook(memberId, isbn) → boolean
    + searchByTitle/Author/Isbn(...)

class Book:
    - isbn, title, author: String
    - copies: List<BookCopy>
    - reservationQueue: Queue<Member>

class BookCopy:
    - copyId: String
    - status: CopyStatus

class Member:
    - memberId, name: String
    - activeLoans: List<Loan>

class Loan:
    - loanId: String
    - member, copy, borrowDate, dueDate, returnDate

interface NotificationService:
    + notifyMember(member, message) → void

enum CopyStatus: AVAILABLE, BORROWED, RESERVED
```

---

## Implementation

### Library.borrowBook

**Core logic:**
1. Look up member and book
2. Check member hasn't hit the 5-loan limit
3. Find an available copy
4. Mark copy as BORROWED, create a Loan, add to member's active loans

**Edge cases:**
- Member not found, book not found → return empty
- All copies checked out → return empty (member should reserve instead)
- Member at loan limit → return empty

```
borrowBook(memberId, isbn)
    member = members[memberId]
    book = booksByIsbn[isbn]
    if member is null or book is null → return empty
    if member.activeLoans.size >= 5 → return empty

    copy = book.getAvailableCopy()
    if copy is empty → return empty

    copy.markBorrowed()
    loan = new Loan(nextId(), member, copy, today, today + 14 days)
    activeLoans[loan.id] = loan
    member.activeLoans.add(loan)
    return loan
```

### Library.returnBook

**Core logic:**
1. Look up the loan
2. Mark it returned, calculate fee
3. Check the reservation queue — if someone's waiting, mark copy RESERVED and notify them; otherwise mark AVAILABLE
4. Remove from member's active loans

```
returnBook(loanId)
    loan = activeLoans.remove(loanId)
    if loan is null → return 0.0

    loan.markReturned()
    fee = loan.calculateFee()
    member = loan.member
    member.activeLoans.remove(loan)

    book = loan.copy.book
    nextReserver = book.pollNextReservation()
    if nextReserver is present:
        loan.copy.markReserved()
        notificationService.notify(nextReserver, "Your reserved book is ready")
    else:
        loan.copy.markAvailable()

    return fee
```

### Loan.calculateFee

```
calculateFee()
    if !returned or returnDate <= dueDate → return 0.0
    overdueDays = daysBetween(dueDate, returnDate)
    return overdueDays * 0.50
```

### Complete Code Implementation

```java
public enum CopyStatus { AVAILABLE, BORROWED, RESERVED }
```

```java
public class BookCopy {
    private final String copyId;
    private final Book book;
    private CopyStatus status;

    public BookCopy(String copyId, Book book) {
        this.copyId = copyId;
        this.book = book;
        this.status = CopyStatus.AVAILABLE;
    }

    public void markBorrowed()  { this.status = CopyStatus.BORROWED; }
    public void markAvailable() { this.status = CopyStatus.AVAILABLE; }
    public void markReserved()  { this.status = CopyStatus.RESERVED; }

    public String getCopyId()   { return copyId; }
    public Book getBook()       { return book; }
    public CopyStatus getStatus() { return status; }
    public boolean isAvailable() { return status == CopyStatus.AVAILABLE; }
}
```

```java
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class Book {
    private final String isbn;
    private final String title;
    private final String author;
    private final List<BookCopy> copies;
    private final Queue<Member> reservationQueue;

    public Book(String isbn, String title, String author) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.copies = new ArrayList<>();
        this.reservationQueue = new LinkedList<>();
    }

    public void addCopy(BookCopy copy) { copies.add(copy); }

    public Optional<BookCopy> getAvailableCopy() {
        return copies.stream().filter(BookCopy::isAvailable).findFirst();
    }

    public void enqueueReservation(Member member) {
        reservationQueue.offer(member);
    }

    public Optional<Member> pollNextReservation() {
        return Optional.ofNullable(reservationQueue.poll());
    }

    public boolean hasAvailableCopy() {
        return copies.stream().anyMatch(BookCopy::isAvailable);
    }

    public String getIsbn()   { return isbn; }
    public String getTitle()  { return title; }
    public String getAuthor() { return author; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Member {
    private final String memberId;
    private final String name;
    private final List<Loan> activeLoans;

    public Member(String memberId, String name) {
        this.memberId = memberId;
        this.name = name;
        this.activeLoans = new ArrayList<>();
    }

    public String getMemberId()      { return memberId; }
    public String getName()          { return name; }
    public List<Loan> getActiveLoans() { return activeLoans; }
    public boolean canBorrow()       { return activeLoans.size() < 5; }
}
```

```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Loan {
    private final String loanId;
    private final Member member;
    private final BookCopy copy;
    private final LocalDate borrowDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;
    private boolean returned;

    public Loan(String loanId, Member member, BookCopy copy,
                LocalDate borrowDate, LocalDate dueDate) {
        this.loanId = loanId;
        this.member = member;
        this.copy = copy;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returned = false;
    }

    public void markReturned() {
        this.returned = true;
        this.returnDate = LocalDate.now();
    }

    public double calculateFee() {
        if (!returned || !returnDate.isAfter(dueDate)) return 0.0;
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        return overdueDays * 0.50;
    }

    public boolean isOverdue() {
        LocalDate checkDate = returned ? returnDate : LocalDate.now();
        return checkDate.isAfter(dueDate);
    }

    public String getLoanId()      { return loanId; }
    public Member getMember()      { return member; }
    public BookCopy getCopy()      { return copy; }
    public LocalDate getDueDate()  { return dueDate; }
}
```

```java
public interface NotificationService {
    void notifyMember(Member member, String message);
}

public class ConsoleNotificationService implements NotificationService {
    @Override
    public void notifyMember(Member member, String message) {
        System.out.printf("[NOTIFY %s] %s%n", member.getName(), message);
    }
}
```

```java
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Library {
    private final Map<String, Book> booksByIsbn = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Loan> activeLoans = new HashMap<>();
    private final NotificationService notificationService;
    private final AtomicLong loanCounter = new AtomicLong(1);

    public Library(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Optional<Loan> borrowBook(String memberId, String isbn) {
        Member member = members.get(memberId);
        Book book = booksByIsbn.get(isbn);
        if (member == null || book == null) return Optional.empty();
        if (!member.canBorrow()) return Optional.empty();

        Optional<BookCopy> copy = book.getAvailableCopy();
        if (copy.isEmpty()) return Optional.empty();

        copy.get().markBorrowed();
        LocalDate now = LocalDate.now();
        Loan loan = new Loan("L-" + loanCounter.getAndIncrement(),
            member, copy.get(), now, now.plusDays(14));
        activeLoans.put(loan.getLoanId(), loan);
        member.getActiveLoans().add(loan);
        return Optional.of(loan);
    }

    public double returnBook(String loanId) {
        Loan loan = activeLoans.remove(loanId);
        if (loan == null) return 0.0;

        loan.markReturned();
        double fee = loan.calculateFee();
        loan.getMember().getActiveLoans().remove(loan);

        Book book = loan.getCopy().getBook();
        Optional<Member> nextReserver = book.pollNextReservation();
        if (nextReserver.isPresent()) {
            loan.getCopy().markReserved();
            notificationService.notifyMember(nextReserver.get(),
                "Your reserved book '" + book.getTitle() + "' is ready for pickup.");
        } else {
            loan.getCopy().markAvailable();
        }

        return fee;
    }

    public boolean reserveBook(String memberId, String isbn) {
        Member member = members.get(memberId);
        Book book = booksByIsbn.get(isbn);
        if (member == null || book == null) return false;
        if (book.hasAvailableCopy()) return false;

        book.enqueueReservation(member);
        return true;
    }

    public List<Book> searchByTitle(String title) {
        return booksByIsbn.values().stream()
            .filter(b -> b.getTitle().toLowerCase().contains(title.toLowerCase()))
            .collect(Collectors.toList());
    }

    public List<Book> searchByAuthor(String author) {
        return booksByIsbn.values().stream()
            .filter(b -> b.getAuthor().toLowerCase().contains(author.toLowerCase()))
            .collect(Collectors.toList());
    }

    public Optional<Book> searchByIsbn(String isbn) {
        return Optional.ofNullable(booksByIsbn.get(isbn));
    }

    public void addBook(Book book)     { booksByIsbn.put(book.getIsbn(), book); }
    public void addMember(Member member) { members.put(member.getMemberId(), member); }
}
```

### Verification

```
Setup: Library with Book "Clean Code" (ISBN: 978-01), 2 copies (C1, C2).
Members: Alice (M1), Bob (M2), Carol (M3).

Step 1: Alice borrows "978-01"
  member=Alice ✓, book found ✓, canBorrow() = true (0 < 5)
  getAvailableCopy() → C1 (AVAILABLE)
  C1.markBorrowed(), Loan L-1 created (due: today+14)
  activeLoans[L-1], Alice.activeLoans=[L-1]

Step 2: Bob borrows "978-01"
  getAvailableCopy() → C2 (AVAILABLE)
  C2.markBorrowed(), Loan L-2 created

Step 3: Carol tries to borrow "978-01"
  getAvailableCopy() → empty (both copies BORROWED)
  Return empty

Step 4: Carol reserves "978-01"
  hasAvailableCopy() = false → enqueueReservation(Carol)
  reservationQueue = [Carol]

Step 5: Alice returns L-1 (3 days late)
  loan.markReturned(), fee = 3 × $0.50 = $1.50
  pollNextReservation() → Carol
  C1.markReserved()
  Notify Carol: "Your reserved book 'Clean Code' is ready"

Step 6: Bob returns L-2 (on time)
  fee = $0.00
  pollNextReservation() → empty
  C2.markAvailable()
```

---

## Extensibility

### 1. "How would you add loan renewals?"

> "I'd add a `renew()` method on Loan that extends the due date by 14 days, limited to a maximum number of renewals (say 2). The method checks that the book has no pending reservations — if someone's waiting, renewal is denied. This keeps fairness intact."

```
class Loan:
    - renewalCount: int = 0
    - MAX_RENEWALS: int = 2

    + renew() → boolean
        if renewalCount >= MAX_RENEWALS → return false
        if copy.book.reservationQueue is not empty → return false
        dueDate = dueDate + 14 days
        renewalCount++
        return true
```

### 2. "How would you support digital books?"

> "I'd extend Book with an EBook subclass that has a `maxConcurrentReaders` instead of physical copies. Borrowing a digital book decrements a concurrent reader count; returning increments it. The Library's borrow logic would check `instanceof EBook` and call a different path. BookCopy wouldn't apply — EBooks don't have physical copies."

### 3. "What about priority borrowing for reservers?"

> "When a member with a reservation comes to borrow, I'd check if they're the first in the queue before handing out the reserved copy. If the RESERVED copy is available, only the member it was reserved for can borrow it. Others would need to wait or borrow a different available copy."

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can model the basic relationship between books, copies, and members. A working borrow and return flow is the main goal. Late fees might be a simple calculation. Reservations aren't expected. If your system correctly tracks who has which book and rejects double-borrowing, you're doing well.

### Mid-level

For mid-level candidates, I expect the Book/BookCopy separation (shared metadata vs. individual status), Loan with proper date-based late fee calculation, and a FIFO reservation queue with notification on return. The notification should be through an interface for testability. You should handle the 5-loan limit and be able to discuss extensibility.

### Senior

Senior candidates should produce a design where the return flow atomically handles the reservation handoff — poll the queue, mark the copy reserved, and notify in one operation. You'd explain why the notification service is injected (testability, decoupling). Extensibility discussions should cover renewals with fairness constraints, digital books with concurrent reader limits, and search indexing strategies for large catalogues.