# Tic-Tac-Toe

**Difficulty:** Easy | **Companies:** Google, Amazon, Meta

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Build the object-oriented design for a two-player Tic-Tac-Toe game. Players take turns placing X and O on a 3×3 grid. The first to align three of their marks in a row, column, or diagonal wins."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

The goal is to surface ambiguity early and get to a concrete spec. A reliable way to structure your questions is to cover four areas: what the core actions are, how errors should be handled, what the boundaries of the system are, and whether we need to plan for future extensions.

> **You:** "How do players interact with the game? Do they specify a row and column?"
>
> **Interviewer:** "Yes, players choose a cell by row and column (0-indexed). If the cell is empty, the mark is placed."

Good. You've confirmed the main action. Now think about how the game ends.

> **You:** "What are all the ways a game can end?"
>
> **Interviewer:** "Three in a row — horizontal, vertical, or diagonal — wins. If all 9 cells are filled with no winner, it's a draw."

Now think about what happens when things go wrong.

> **You:** "What should happen if someone tries to place on an already-occupied cell?"
>
> **Interviewer:** "Reject the move. Don't let invalid moves change the board."

> **You:** "What if a player tries to move after the game is already over?"
>
> **Interviewer:** "Same thing. Reject it clearly."

Now figure out the scope.

> **You:** "Is this a standard 3×3 board, or does the size need to be configurable?"
>
> **Interviewer:** "Always 3×3. Keep it simple."

> **You:** "Backend logic only, or do we need UI support?"
>
> **Interviewer:** "Backend only."

Finally, check for future features.

> **You:** "Do we need undo, move history, or AI opponents?"
>
> **Interviewer:** "No, just the basic two-player game."

### Final Requirements

```
Requirements:
1. Two players take turns placing X and O on a 3×3 grid
2. A player chooses a cell by (row, column); the mark is placed if the cell is empty
3. The game ends when:
   - A player gets three in a row (horizontal, vertical, or diagonal). They win.
   - All 9 cells are filled. It's a draw.
4. Invalid moves should be rejected clearly:
   - Placing on an occupied cell
   - Moving after the game is over

Out of scope:
- UI support
- AI opponents
- Move history / undo
- Configurable board size
```

---

## Core Entities and Relationships

Start by asking: what are the main "things" in this problem? Look for nouns in your requirements. In Tic-Tac-Toe, a few jump out: the game itself, the board where marks are placed, and the players making moves.

| Entity | Responsibility |
|--------|---------------|
| **Game** | The orchestrator. Holds the Board, tracks which Player's turn it is, manages game state (in progress, won, draw), and enforces turn-based rules. When a player makes a move, Game validates it, tells Board to place the mark, checks for a winner, and switches turns. |
| **Board** | The 3×3 grid where marks live. Owns the grid state and handles mark placement. Knows how to check if a cell is empty, place a mark, and detect three in a row. Doesn't care about whose turn it is or game status. |
| **Player** | Represents a person in the game. Simple data holder with a name and symbol (X or O). No game logic here. |

---

## Class Design

Now that we've identified the three core entities, the next step is defining their interfaces. Start with a top-down approach — begin with Game since it's the orchestrator and primary entry point.

### Game

The Game class is the orchestration layer. External code should interact with the game only through this class.

| Requirement | What Game must track |
|-------------|---------------------|
| "Two players take turns placing X and O" | The two players, whose turn it is, and the board |
| "The game ends when a player wins or the board is full" | The game state (in progress, won, draw) |
| "A player gets three in a row. They win." | Who won (if anyone) |

This leaves us with a simple state object:

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState        // IN_PROGRESS, WON, DRAW
    - winner: Player?         // null if no winner yet or draw
```

Next, look at the actions the outside world needs to perform:

| Need from requirements | Method on Game |
|------------------------|---------------|
| "Players take turns placing marks" | makeMove(row, column) → boolean |
| "Reject moves on occupied cells" | (delegated to Board) |
| "The game ends when..." | getState() → GameState |
| "A player gets three in a row" | getWinner() → Player? |

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState
    - winner: Player?

    + Game(player1Name, player2Name)
    + makeMove(row, column) → boolean
    + getCurrentPlayer() → Player
    + getState() → GameState
    + getWinner() → Player?
    + getBoard() → Board
```

### Board

Board owns the grid. It knows where marks are, whether a cell is empty, and whether three marks are aligned.

| Requirement | What Board must track |
|-------------|----------------------|
| "3×3 grid" | Fixed size and a 2D grid of symbols |
| "Mark is placed if the cell is empty" | The current occupancy of each cell |
| "All 9 cells are filled" | A move count to check fullness in O(1) |
| "Three in a row" | Enough information in the grid to check alignment |

```
class Board:
    - size: int = 3
    - grid: Symbol[size][size]
    - moveCount: int

    + Board()
    + placePiece(row, col, symbol) → boolean
    + checkWinner() → Symbol?
    + isFull() → boolean
    + getCell(row, col) → Symbol
```

### Player

| Requirement | What Player must track |
|-------------|----------------------|
| "Two players take turns" | A name to identify the player |
| "placing X and O" | The symbol (X or O) associated with the player |

```
class Player:
    - name: String
    - symbol: Symbol

    + Player(name, symbol)
    + getName() → String
    + getSymbol() → Symbol
```

### Final Class Design

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState
    - winner: Player?

    + Game(player1Name, player2Name)
    + makeMove(row, column) → boolean
    + getCurrentPlayer() → Player
    + getState() → GameState
    + getWinner() → Player?
    + getBoard() → Board

class Board:
    - size: int = 3
    - grid: Symbol[size][size]
    - moveCount: int

    + Board()
    + placePiece(row, col, symbol) → boolean
    + checkWinner() → Symbol?
    + isFull() → boolean
    + getCell(row, col) → Symbol

class Player:
    - name: String
    - symbol: Symbol

    + Player(name, symbol)
    + getName() → String
    + getSymbol() → Symbol

enum GameState:
    IN_PROGRESS, WON, DRAW

enum Symbol:
    X, O, EMPTY
```

---

## Implementation

For each method, follow a consistent pattern: define the core logic (happy path), then consider edge cases.

### Game

The core method is `makeMove` — it encapsulates the entire game flow.

**Core logic:**
1. Place the mark via `board.placePiece(row, col, currentPlayer.getSymbol())`
2. Check for winner via `board.checkWinner()`
3. If no winner, check for draw via `board.isFull()`
4. Switch turn if game is still in progress
5. Return true

**Edge cases (reject before touching state):**
- Game is already over
- Cell is occupied or out of bounds (delegated to Board)

```
makeMove(row, col)
    if state != IN_PROGRESS
        return false

    if !board.placePiece(row, col, currentPlayer.getSymbol())
        return false

    winner = board.checkWinner()
    if winner is present
        state = WON
        this.winner = currentPlayer
    else if board.isFull()
        state = DRAW
    else
        switchTurn()
    return true
```

### Board

**placePiece** — Core logic: validate bounds, check cell empty, place mark, increment count. Edge case: out of bounds or occupied cell → return false.

```
placePiece(row, col, symbol)
    if row < 0 || row >= size || col < 0 || col >= size
        return false
    if grid[row][col] != EMPTY
        return false
    grid[row][col] = symbol
    moveCount++
    return true
```

**checkWinner** — Core logic: scan all rows, columns, and both diagonals. If any line of 3 matches, return that symbol. For a 3×3 board, explicit checks are clearer than a general direction-vector approach.

```
checkWinner()
    // Check rows
    for r = 0 to 2:
        if grid[r][0] != EMPTY && grid[r][0] == grid[r][1] == grid[r][2]
            return grid[r][0]
    // Check columns
    for c = 0 to 2:
        if grid[0][c] != EMPTY && grid[0][c] == grid[1][c] == grid[2][c]
            return grid[0][c]
    // Diagonals
    if grid[0][0] != EMPTY && grid[0][0] == grid[1][1] == grid[2][2]
        return grid[0][0]
    if grid[0][2] != EMPTY && grid[0][2] == grid[1][1] == grid[2][0]
        return grid[0][2]
    return null
```

### Complete Code Implementation

```java
public enum Symbol { X, O, EMPTY }

public enum GameState { IN_PROGRESS, WON, DRAW }
```

```java
public class Player {
    private final String name;
    private final Symbol symbol;

    public Player(String name, Symbol symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    public String getName()   { return name; }
    public Symbol getSymbol() { return symbol; }
}
```

```java
import java.util.Arrays;

public class Board {
    private static final int SIZE = 3;
    private final Symbol[][] grid;
    private int moveCount;

    public Board() {
        grid = new Symbol[SIZE][SIZE];
        for (Symbol[] row : grid) Arrays.fill(row, Symbol.EMPTY);
        moveCount = 0;
    }

    public boolean placePiece(int row, int col, Symbol symbol) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return false;
        if (grid[row][col] != Symbol.EMPTY) return false;
        grid[row][col] = symbol;
        moveCount++;
        return true;
    }

    public Symbol checkWinner() {
        for (int r = 0; r < SIZE; r++) {
            if (grid[r][0] != Symbol.EMPTY
                    && grid[r][0] == grid[r][1] && grid[r][1] == grid[r][2])
                return grid[r][0];
        }
        for (int c = 0; c < SIZE; c++) {
            if (grid[0][c] != Symbol.EMPTY
                    && grid[0][c] == grid[1][c] && grid[1][c] == grid[2][c])
                return grid[0][c];
        }
        if (grid[0][0] != Symbol.EMPTY
                && grid[0][0] == grid[1][1] && grid[1][1] == grid[2][2])
            return grid[0][0];
        if (grid[0][2] != Symbol.EMPTY
                && grid[0][2] == grid[1][1] && grid[1][1] == grid[2][0])
            return grid[0][2];
        return null;
    }

    public boolean isFull() { return moveCount == SIZE * SIZE; }

    public Symbol getCell(int row, int col) { return grid[row][col]; }
}
```

```java
public class Game {
    private final Board board;
    private final Player[] players;
    private int currentPlayerIndex;
    private GameState state;
    private Player winner;

    public Game(String player1Name, String player2Name) {
        board = new Board();
        players = new Player[]{
            new Player(player1Name, Symbol.X),
            new Player(player2Name, Symbol.O)
        };
        currentPlayerIndex = 0;
        state = GameState.IN_PROGRESS;
        winner = null;
    }

    public boolean makeMove(int row, int col) {
        if (state != GameState.IN_PROGRESS) return false;

        Player current = players[currentPlayerIndex];
        if (!board.placePiece(row, col, current.getSymbol())) return false;

        Symbol winSymbol = board.checkWinner();
        if (winSymbol != null) {
            state = GameState.WON;
            winner = current;
        } else if (board.isFull()) {
            state = GameState.DRAW;
        } else {
            currentPlayerIndex = 1 - currentPlayerIndex;
        }
        return true;
    }

    public Player getCurrentPlayer() { return players[currentPlayerIndex]; }
    public GameState getState()      { return state; }
    public Player getWinner()        { return winner; }
    public Board getBoard()          { return board; }
}
```

### Verification

```
Initial state: empty 3×3 board, currentPlayer = Alice (X)

Move 1: Alice → (0,0)
  placePiece(0, 0, X) → true
  checkWinner()? No three in a row
  currentPlayer = Bob

Move 2: Bob → (1,1)
  placePiece(1, 1, O) → true
  checkWinner()? No
  currentPlayer = Alice

Move 3: Alice → (0,1)
  placePiece(0, 1, X) → true
  checkWinner()? No
  currentPlayer = Bob

Move 4: Bob → (2,2)
  placePiece(2, 2, O) → true
  checkWinner()? No
  currentPlayer = Alice

Move 5: Alice → (0,2)
  placePiece(0, 2, X) → true
  checkWinner()? Row 0: X, X, X → match!
  state = WON, winner = Alice

Move 6: Bob tries (1,0)
  state != IN_PROGRESS → returns false immediately
```

This verifies placement, row win detection, state transitions, and move rejection after game ends.

---

## Extensibility

### 1. "How would you support an N×N board with K-in-a-row?"

> "Today I fix the board at 3×3 because that's the requirement. If we wanted configurable sizes, I'd make `size` and `winLength` constructor parameters on Board. The `checkWinner` method would need to be generalized — instead of hardcoded index checks, I'd use the direction-vector approach (like Connect Four) with a `countInDirection` helper. Game doesn't change: it just constructs `new Board(n, k)`."

### 2. "How would you add undo?"

> "Undo belongs in Game because Game controls the lifecycle and turn order. I'd keep a `moveHistory` stack. Each time a move succeeds, I push a small Move record containing the row, column, and player index. Undo would pop the last move, clear that cell on the Board, revert `currentPlayerIndex`, and reset game state. Board just needs a `clearCell(row, col)` helper."

```
class Move:
    - row: int
    - col: int
    - playerIndex: int

undoLastMove()
    if moveHistory.isEmpty()
        return false
    last = moveHistory.pop()
    board.clearCell(last.row, last.col)
    currentPlayerIndex = last.playerIndex
    state = IN_PROGRESS
    winner = null
    return true
```

### 3. "How would you add a computer opponent?"

> "Game rules don't change. I'd introduce a `BotEngine` that looks at the current board and returns a (row, col). From Game's perspective, a bot move is just another call to `makeMove(row, col)`. The bot is a decision-making layer, not a Player subclass — Player stays as simple data."

```
class BotEngine:
    + chooseMove(board) → (row, col)

// Trivial implementation: pick any empty cell
chooseMove(board)
    for r = 0 to 2:
        for c = 0 to 2:
            if board.getCell(r, c) == EMPTY
                return (r, c)
    return (-1, -1)
```

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can decompose the problem into logical pieces and implement a working game. You should identify that you need something for the board, something for players, and something to orchestrate turns. Your `placePiece` logic should work. Win detection is the main challenge — I expect you to at least check rows and columns correctly. Diagonal checking is trickier, and it's fine if you need hints. Edge cases like occupied cells or playing after game over should be handled, even if basic. If your code correctly identifies a winner or draw, you're doing well.

### Mid-level

For mid-level candidates, I expect cleaner separation of concerns without guidance. Game handles orchestration and turn management. Board owns grid state and win detection. Player is minimal data. Your `makeMove` should validate state before mutating anything. The win-checking should cover all cases cleanly. You should be able to discuss at least one extensibility scenario — like configurable board size or undo — and explain where the changes would live without implementing them.

### Senior

Senior candidates should produce a design I'd be comfortable reviewing as production code. Class boundaries should be obvious and well-justified. You should proactively point out design decisions: why Player is just data, why GameState is an enum rather than booleans, why win checking lives on Board rather than Game. I expect you to catch your own edge cases during implementation. Extensibility discussions should cover multiple approaches with tradeoffs — for configurable board size, you'd explain the generalized direction-vector approach. For AI, you'd recognize that game rules don't change and you just need a decision-making component.
