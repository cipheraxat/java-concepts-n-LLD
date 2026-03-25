# Snake and Ladder

**Difficulty:** Easy | **Companies:** Google, Amazon

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Design the object-oriented backend for a Snake and Ladder board game. Players take turns rolling a die to advance on a 100-square board. Landing on a snake sends them down; landing on a ladder sends them up. First to reach 100 wins."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

The goal is to surface ambiguity early and get to a concrete spec. A reliable way to structure your questions is to cover four areas: what the core actions are, how errors should be handled, what the boundaries of the system are, and whether we need to plan for future extensions.

> **You:** "Is it a standard 100-square board?"
>
> **Interviewer:** "Yes. Players start at position 0 (off the board) and need to reach exactly 100."

Good. Now figure out the dice mechanics.

> **You:** "How many dice and how many sides?"
>
> **Interviewer:** "A single standard 6-sided die."

> **You:** "How many players?"
>
> **Interviewer:** "2 to 4."

Now think about edge cases.

> **You:** "What happens if a roll would take a player past 100?"
>
> **Interviewer:** "They stay in place. No overshooting."

> **You:** "Can a snake head and ladder bottom occupy the same square?"
>
> **Interviewer:** "No. Each square has at most one portal."

Finally, check for additional features.

> **You:** "Any special rules like extra turns on rolling a 6?"
>
> **Interviewer:** "No. Keep it simple — just the basic game."

### Final Requirements

```
Requirements:
1. Board has 100 squares; players start at position 0 (off the board)
2. 2–4 players take turns rolling a single 6-sided die
3. Landing on a snake's head → slide down to its tail
4. Landing on a ladder's bottom → climb up to its top
5. First player to reach exactly 100 wins
6. If a roll would take a player past 100, they stay in place
7. Each square has at most one portal (snake or ladder)

Out of scope:
- Extra turns on rolling 6
- Power-up squares
- Multiple dice
- AI players
```

---

## Core Entities and Relationships

Start by asking: what are the main "things" in this problem? Look for nouns in your requirements. In Snake and Ladder, a few jump out: the game itself, the board with its portals, the players, and the dice.

| Entity | Responsibility |
|--------|---------------|
| **Game** | The orchestrator. Manages the circular turn queue, executes each turn (roll → move → resolve portal → check win), and tracks game state. External code interacts through this class. |
| **Board** | Owns the board topology — snakes and ladders stored as a portal map. Resolves a player's final position after a roll, including overshoot logic. Decoupled from turn management. |
| **Player** | Tracks a player's name and current position. Simple mutable data holder — position changes each turn. |
| **Dice** | Encapsulates random number generation. Separated so tests can inject a deterministic dice without changing Game logic. |

---

## Class Design

Now that we've identified the four core entities, the next step is defining their interfaces. Start top-down with Game since it's the orchestrator.

### Game

Game manages the turn loop using a circular queue of players.

| Requirement | What Game must track |
|-------------|---------------------|
| "2–4 players take turns" | A queue of players, circular ordering |
| "First to reach 100 wins" | Game state (in progress or over), the winner |
| "Roll die, move, resolve portals" | References to Board and Dice |

```
class Game:
    - board: Board
    - dice: Dice
    - turnQueue: Queue<Player>
    - players: List<Player>
    - isOver: boolean
    - winner: Player?

    + Game(board, dice, playerNames)
    + takeTurn() → boolean
    + play() → void
    + getWinner() → Player?
    + isOver() → boolean
```

### Board

Board owns the portal map. Both snakes and ladders are stored in a single map keyed by trigger square — this unifies lookup and prevents a position from being both a snake head and a ladder bottom.

| Requirement | What Board must track |
|-------------|----------------------|
| "Landing on snake → slide down" | A portal map: trigger → destination |
| "Landing on ladder → climb up" | Same portal map (unified) |
| "Overshoot → stay in place" | Board size (100) |

```
class Board:
    - boardSize: int = 100
    - portals: Map<Integer, Integer>

    + Board(snakes, ladders)
    + resolvePosition(currentPos, roll) → int
```

### Player

| Requirement | What Player must track |
|-------------|----------------------|
| "Players take turns" | A name for identification |
| "Advance on the board" | Current position |

```
class Player:
    - name: String
    - position: int = 0

    + getName() → String
    + getPosition() → int
    + setPosition(pos) → void
```

### Dice

| Requirement | What Dice must track |
|-------------|---------------------|
| "Single 6-sided die" | Number of sides, random source |

```
class Dice:
    - sides: int
    - random: Random

    + Dice(sides)
    + roll() → int
```

### Final Class Design

```
class Game:
    - board: Board
    - dice: Dice
    - turnQueue: Queue<Player>
    - players: List<Player>
    - isOver: boolean
    - winner: Player?

    + Game(board, dice, playerNames)
    + takeTurn() → boolean
    + play() → void
    + getWinner() → Player?
    + isOver() → boolean

class Board:
    - boardSize: int = 100
    - portals: Map<Integer, Integer>

    + Board(snakes, ladders)
    + resolvePosition(currentPos, roll) → int

class Player:
    - name: String
    - position: int

class Dice:
    - sides: int
    - random: Random

    + Dice(sides)
    + roll() → int
```

---

## Implementation

For each method, follow a consistent pattern: define the core logic (happy path), then consider edge cases.

### Board.resolvePosition

**Core logic:**
1. Calculate new position = current + roll
2. If new position > boardSize → return currentPos (overshoot, stay)
3. Check portal map: if portal exists at newPos, return the destination
4. Otherwise return newPos

```
resolvePosition(currentPos, roll)
    newPos = currentPos + roll
    if newPos > boardSize
        return currentPos           // overshoot → stay
    return portals.getOrDefault(newPos, newPos)
```

### Game.takeTurn

**Core logic:**
1. Poll the next player from the queue
2. Roll the dice
3. Resolve the new position via Board
4. Update the player's position
5. Check for win (position == 100)
6. If not won, re-add player to queue

**Edge cases:**
- Game already over → return false

```
takeTurn()
    if isOver → return false

    current = turnQueue.poll()
    roll = dice.roll()
    oldPos = current.position
    newPos = board.resolvePosition(oldPos, roll)
    current.position = newPos

    if newPos == board.boardSize
        isOver = true
        winner = current
        return false

    turnQueue.add(current)      // re-queue for next round
    return true
```

### Board constructor

**Validation:** Ensure no duplicate portals (same trigger square for a snake and a ladder). Build the unified portal map from both snakes and ladders.

```
Board(snakes, ladders)
    portals = empty map
    for each snake:
        if portals.contains(snake.head) → throw error
        portals[snake.head] = snake.tail
    for each ladder:
        if portals.contains(ladder.bottom) → throw error
        portals[ladder.bottom] = ladder.top
```

### Complete Code Implementation

```java
public class Player {
    private final String name;
    private int position;

    public Player(String name) {
        this.name = name;
        this.position = 0;
    }

    public String getName()        { return name; }
    public int getPosition()       { return position; }
    void setPosition(int position) { this.position = position; }
}
```

```java
import java.util.Random;

public class Dice {
    private final int sides;
    private final Random random;

    public Dice(int sides) {
        this.sides = sides;
        this.random = new Random();
    }

    public Dice() { this(6); }

    public int roll() { return random.nextInt(sides) + 1; }
}
```

```java
import java.util.HashMap;
import java.util.Map;

public class Board {
    private static final int BOARD_SIZE = 100;
    private final Map<Integer, Integer> portals = new HashMap<>();

    public Board(Map<Integer, Integer> snakes, Map<Integer, Integer> ladders) {
        for (var entry : snakes.entrySet()) {
            if (portals.containsKey(entry.getKey()))
                throw new IllegalArgumentException("Duplicate portal at " + entry.getKey());
            portals.put(entry.getKey(), entry.getValue());
        }
        for (var entry : ladders.entrySet()) {
            if (portals.containsKey(entry.getKey()))
                throw new IllegalArgumentException("Duplicate portal at " + entry.getKey());
            portals.put(entry.getKey(), entry.getValue());
        }
    }

    public int resolvePosition(int currentPos, int roll) {
        int newPos = currentPos + roll;
        if (newPos > BOARD_SIZE) return currentPos;
        return portals.getOrDefault(newPos, newPos);
    }

    public int getBoardSize() { return BOARD_SIZE; }
}
```

```java
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class Game {
    private final Board board;
    private final Dice dice;
    private final Queue<Player> turnQueue;
    private final List<Player> players;
    private boolean isOver;
    private Player winner;

    public Game(Board board, Dice dice, List<String> playerNames) {
        this.board = board;
        this.dice = dice;
        this.players = new ArrayList<>();
        this.turnQueue = new ArrayDeque<>();
        this.isOver = false;

        for (String name : playerNames) {
            Player p = new Player(name);
            players.add(p);
            turnQueue.add(p);
        }
    }

    public boolean takeTurn() {
        if (isOver) return false;

        Player current = turnQueue.poll();
        int roll = dice.roll();
        int oldPos = current.getPosition();
        int newPos = board.resolvePosition(oldPos, roll);

        current.setPosition(newPos);

        String event = "";
        if (newPos == oldPos && oldPos + roll > board.getBoardSize()) {
            event = " [OVERSHOOT — stayed at " + oldPos + "]";
        } else if (newPos != oldPos + roll && newPos < oldPos + roll) {
            event = " [SNAKE! slid to " + newPos + "]";
        } else if (newPos != oldPos + roll && newPos > oldPos + roll) {
            event = " [LADDER! climbed to " + newPos + "]";
        }

        System.out.printf("%s rolled %d: %d → %d%s%n",
            current.getName(), roll, oldPos, newPos, event);

        if (newPos == board.getBoardSize()) {
            isOver = true;
            winner = current;
            System.out.println(current.getName() + " WINS!");
            return false;
        }

        turnQueue.add(current);
        return true;
    }

    public void play() {
        while (takeTurn()) { }
    }

    public Optional<Player> getWinner() { return Optional.ofNullable(winner); }
    public boolean isOver()             { return isOver; }
}
```

### Verification

```
Setup: Board with snakes {97→78, 54→34} and ladders {2→38, 51→67}
Players: Alice, Bob (turnQueue: [Alice, Bob])

Turn 1: Alice rolls 2 → position 0+2 = 2 → portal at 2: ladder to 38
  Alice: 0 → 38 [LADDER! climbed to 38]

Turn 2: Bob rolls 5 → position 0+5 = 5 → no portal
  Bob: 0 → 5

Turn 3: Alice rolls 3 → position 38+3 = 41 → no portal
  Alice: 38 → 41

...

Turn N: Alice at 97, rolls 5 → 97+5 = 102 > 100 → stay at 97
  Alice: 97 → 97 [OVERSHOOT — stayed at 97]

Turn N+1: Alice rolls 3 → 97+3 = 100 → WIN!
  Alice: 97 → 100
  Alice WINS!
```

This verifies ladder activation, normal movement, overshoot logic, and win detection.

---

## Extensibility

### 1. "How would you support multiple dice?"

> "I'd make Dice composable. Either add a `count` parameter to Dice that sums multiple rolls, or create a `DiceSet` wrapper around a list of Dice objects. The `roll()` method returns the sum. Game doesn't change — it just calls `dice.roll()` as before."

```
class DiceSet:
    - dice: List<Dice>

    + DiceSet(numberOfDice, sidesPerDie)
    + roll() → int
        return dice.stream().mapToInt(Dice::roll).sum()
```

### 2. "How would you make the dice deterministic for testing?"

> "Dice already encapsulates randomness, so this is easy. I'd add a constructor that accepts a `long seed` to create a seeded Random. In tests, you pass a known seed and get reproducible rolls. Alternatively, you could pass in a pre-built Random object. Game doesn't change at all."

```
class Dice:
    + Dice(sides, seed)
        this.random = new Random(seed)

// In tests:
Dice testDice = new Dice(6, 42L)    // deterministic
Game game = new Game(board, testDice, playerNames)
```

### 3. "What if we want special squares like 'roll again' or 'skip turn'?"

> "I'd introduce a `SquareEffect` concept. Board would track a map of `position → SquareEffect`. After resolving portals, Game checks for additional effects. A 'roll again' effect would re-insert the current player at the front of the queue. A 'skip turn' effect would set a flag on the Player. This keeps Board as the source of board topology and Game as the rule enforcer."

```
enum SquareEffect:
    NONE, ROLL_AGAIN, SKIP_NEXT_TURN

// In Board:
- effects: Map<Integer, SquareEffect>
+ getEffect(position) → SquareEffect

// In Game.takeTurn after move:
effect = board.getEffect(newPos)
if effect == ROLL_AGAIN
    turnQueue.addFirst(current)     // immediate re-turn
else if effect == SKIP_NEXT_TURN
    // skip next player
    turnQueue.add(turnQueue.poll()) // move next player to back
    turnQueue.add(current)
else
    turnQueue.add(current)
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can decompose the game into logical pieces and implement a working game loop. You should identify that you need a board with portals, players with positions, a dice, and something to manage turns. A working circular turn queue and correct portal resolution are the main challenges. It's fine if you use separate lists for snakes and ladders rather than a unified portal map. If your game correctly moves players, applies portals, handles overshoot, and detects a winner, you're doing well.

### Mid-level

For mid-level candidates, I expect cleaner separation — Board owns all topology, Game owns all turn logic, Dice encapsulates randomness. The portal map should be unified (snakes and ladders in one map) with validation against duplicate portals. ArrayDeque for the turn queue is a good data structure choice you should be able to justify. You should handle edge cases cleanly and be able to discuss at least one extensibility point.

### Senior

Senior candidates should produce a design with clear justification for every class boundary. You'd explain why Board.resolvePosition handles overshoot (it's topology, not game logic), why Dice is separate (testability), and why portals are unified (prevents conflicts, simplifies lookup). You should proactively bring up testing strategy — seeded dice for deterministic tests. Extensibility discussions should address multiple approaches with tradeoffs. You might discuss recursive portals (landing on a ladder that leads to a snake) and whether resolvePosition should loop.