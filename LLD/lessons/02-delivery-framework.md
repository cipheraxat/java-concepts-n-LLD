# Lesson 2: Delivery Framework

The **5-Step Delivery Framework** gives you a repeatable, structured approach to solve any LLD interview problem in ~45 minutes.

---

## Overview

| Step | Time | Goal |
|------|------|------|
| 1. Requirements | ~5 min | Clarify scope, pin down functional requirements |
| 2. Entities | ~3 min | Identify core entities and their relationships |
| 3. Class Design | ~10–15 min | Define state (fields) and behavior (methods) per class |
| 4. Implementation | ~10 min | Write Java code for core flows |
| 5. Extensibility | ~5 min | Show the design handles follow-up modifications cleanly |

---

## Full Example: Tic-Tac-Toe

---

### Step 1: Requirements (~5 min)

Ask 3–5 clarifying questions, then state final requirements.

**Clarifying Questions:**
1. Q: Standard 3×3 board? A: Yes
2. Q: Two human players? A: Yes
3. Q: Any special rules? A: No

**Final Requirements:**
1. Two players take turns placing X and O on a 3×3 grid
2. First to get 3 in a row (horizontal, vertical, diagonal) wins
3. All 9 cells filled with no winner → draw
4. Cannot place on an already-occupied cell

---

### Step 2: Entities and Relationships (~3 min)

- **Game** — orchestrates the game loop, tracks status
- **Board** — holds the 3×3 grid, handles placement and win detection
- **Player** — represents a player with name and symbol (X/O)

Relationships:
- `Game` has-a `Board`
- `Game` has two `Player`s
- `Board` has a 2D array of `Symbol`

---

### Step 3: Class Design (~10–15 min)

```java
public enum Symbol {
    X, O, EMPTY
}

public enum GameStatus {
    IN_PROGRESS, WIN, DRAW
}

public enum MoveResult {
    SUCCESS, INVALID_MOVE, WIN, DRAW, GAME_OVER
}
```

```java
public class Player {
    private final String name;
    private final Symbol symbol;

    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public String getName() { return name; }
    public Symbol getSymbol() { return symbol; }
}
```

```java
public class Board {
    private final int size = 3;
    private final Symbol[][] grid;
    private int moveCount;

    public Board() {
        grid = new Symbol[size][size];
        for (Symbol[] row : grid) {
            Arrays.fill(row, Symbol.EMPTY);
        }
    }

    public boolean placePiece(int row, int col, Symbol symbol) {
        if (row < 0 || row >= size || col < 0 || col >= size) return false;
        if (grid[row][col] != Symbol.EMPTY) return false;
        grid[row][col] = symbol;
        moveCount++;
        return true;
    }

    public Optional<Symbol> checkWinner() {
        // Check rows
        for (int r = 0; r < size; r++) {
            if (grid[r][0] != Symbol.EMPTY
                    && grid[r][0] == grid[r][1]
                    && grid[r][1] == grid[r][2]) {
                return Optional.of(grid[r][0]);
            }
        }
        // Check columns
        for (int c = 0; c < size; c++) {
            if (grid[0][c] != Symbol.EMPTY
                    && grid[0][c] == grid[1][c]
                    && grid[1][c] == grid[2][c]) {
                return Optional.of(grid[0][c]);
            }
        }
        // Check diagonals
        if (grid[0][0] != Symbol.EMPTY
                && grid[0][0] == grid[1][1]
                && grid[1][1] == grid[2][2]) {
            return Optional.of(grid[0][0]);
        }
        if (grid[0][2] != Symbol.EMPTY
                && grid[0][2] == grid[1][1]
                && grid[1][1] == grid[2][0]) {
            return Optional.of(grid[0][2]);
        }
        return Optional.empty();
    }

    public boolean isFull() { return moveCount == size * size; }
}
```

```java
public class Game {
    private final Board board;
    private final Player[] players;
    private int currentPlayerIndex;
    private GameStatus status;
    private Player winner;

    public Game(String player1Name, String player2Name) {
        board = new Board();
        players = new Player[]{
            new Player(player1Name, Symbol.X),
            new Player(player2Name, Symbol.O)
        };
        currentPlayerIndex = 0;
        status = GameStatus.IN_PROGRESS;
    }

    public MoveResult makeMove(int row, int col) {
        if (status != GameStatus.IN_PROGRESS) return MoveResult.GAME_OVER;

        Player currentPlayer = players[currentPlayerIndex];

        if (!board.placePiece(row, col, currentPlayer.getSymbol())) {
            return MoveResult.INVALID_MOVE;
        }

        Optional<Symbol> winnerSymbol = board.checkWinner();
        if (winnerSymbol.isPresent()) {
            status = GameStatus.WIN;
            winner = currentPlayer;
            return MoveResult.WIN;
        }

        if (board.isFull()) {
            status = GameStatus.DRAW;
            return MoveResult.DRAW;
        }

        switchTurn();
        return MoveResult.SUCCESS;
    }

    private void switchTurn() {
        currentPlayerIndex = 1 - currentPlayerIndex;
    }

    public Player getCurrentPlayer() { return players[currentPlayerIndex]; }
    public Optional<Player> getWinner() { return Optional.ofNullable(winner); }
    public GameStatus getStatus() { return status; }
}
```

---

### Step 4: Implementation (~10 min)

Walk through the core game loop and verify with a concrete trace.

```java
// Driver / game loop (for demonstration)
public class Main {
    public static void main(String[] args) {
        Game game = new Game("Alice", "Bob");

        // Trace: X wins on top row
        game.makeMove(0, 0); // X at (0,0)
        game.makeMove(1, 1); // O at (1,1)
        game.makeMove(0, 1); // X at (0,1)
        game.makeMove(2, 2); // O at (2,2)
        MoveResult result = game.makeMove(0, 2); // X at (0,2) → WIN

        System.out.println("Result: " + result);             // WIN
        System.out.println("Winner: " + game.getWinner()
            .map(Player::getName).orElse("None"));           // Alice
    }
}
```

**Verification Trace:**
```
Move 1: X at (0,0) → [X,_,_; _,_,_; _,_,_] → no winner → switch to O
Move 2: O at (1,1) → [X,_,_; _,O,_; _,_,_] → no winner → switch to X
Move 3: X at (0,1) → [X,X,_; _,O,_; _,_,_] → no winner → switch to O
Move 4: O at (2,2) → [X,X,_; _,O,_; _,_,O] → no winner → switch to X
Move 5: X at (0,2) → [X,X,X; _,O,_; _,_,O] → X wins! ✓
```

---

### Step 5: Extensibility (~5 min)

**"What if we want an N×N board with K-in-a-row?"**  
Parameterize `Board(int size, int winLength)` and generalize the win-check loops.

**"What if we add an AI player?"**  
Extract a `MoveStrategy` interface:
```java
public interface MoveStrategy {
    int[] getMove(Board board);  // returns {row, col}
}

public class HumanStrategy implements MoveStrategy {
    // reads from Scanner
}

public class RandomAIStrategy implements MoveStrategy {
    // picks a random available cell
}
```

**"What if we add undo?"**  
```java
// In Game, add a move history stack:
private final Deque<int[]> moveHistory = new ArrayDeque<>();

// In makeMove, push move before placing:
moveHistory.push(new int[]{row, col});

// Undo:
public void undo() {
    if (!moveHistory.isEmpty()) {
        int[] last = moveHistory.pop();
        board.clearPiece(last[0], last[1]);
        switchTurn();
        status = GameStatus.IN_PROGRESS;
        winner = null;
    }
}
```

---

## Tips for Delivering Well

1. **Talk while you design** — say your reasoning out loud
2. **Don't jump to code** — confirm entities and class design before coding
3. **Keep it compiling** — prefer correctness over coverage
4. **Verify your logic** — trace through a sample scenario
5. **Leave time for extensibility** — it signals senior-level thinking
