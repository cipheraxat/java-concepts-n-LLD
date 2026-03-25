# Connect Four

**Difficulty:** Easy | **Companies:** Google, Microsoft, Amazon

---

## Requirements

As the interview begins, we'll likely be greeted by a simple prompt to set the stage for the architecture we need to design.

> "Build the object-oriented design for a two-player Connect Four game. Players take turns dropping discs into a 7-column, 6-row board. The first to align four of their own discs vertically, horizontally, or diagonally wins."

Before jumping into class design, you should ask questions of your interviewer. The goal here is to turn that vague prompt into a concrete specification — something you can actually build against.

### Clarifying Questions

The goal is to surface ambiguity early and get to a concrete spec. A reliable way to structure your questions is to cover four areas: what the core actions are, how errors should be handled, what the boundaries of the system are, and whether we need to plan for future extensions.

> **You:** "How do players interact with the game? Do they just specify a column number and the disc drops?"
>
> **Interviewer:** "Yes, players choose a column from 0 to 6, and the disc falls to the lowest available spot."

Good. You've confirmed the main action. Now think about how the game ends.

> **You:** "What are all the ways a game can end? Is it just four in a row, or are there draws?"
>
> **Interviewer:** "Four in a row — vertical, horizontal, or diagonal — wins. If the board fills up with no winner, it's a draw."

Now think about what happens when things go wrong.

> **You:** "What should happen if someone tries to drop a disc in a column that's already full? Should I return an error, throw an exception, or just ignore it?"
>
> **Interviewer:** "Return false or raise an error. Don't let invalid moves break the game state."

> **You:** "And what if a player tries to move out of turn?"
>
> **Interviewer:** "Same thing. Reject it clearly."

Now figure out the scope.

> **You:** "Are we designing this to support one game at a time, or do we need to handle multiple concurrent games?"
>
> **Interviewer:** "Just one game. Keep it simple."

> **You:** "Got it. And is this backend logic only, or do we need UI support as well?"
>
> **Interviewer:** "Backend only. Someone else will handle rendering."

Finally, check for future features.

> **You:** "Do we need to track move history or support undo?"
>
> **Interviewer:** "No, don't overcomplicate it."

> **You:** "What about board size — does it need to be configurable, or always 7×6?"
>
> **Interviewer:** "Always 7×6."

### Final Requirements

```
Requirements:
1. Two players take turns dropping discs into a 7-column, 6-row board
2. A disc falls to the lowest available row in the chosen column
3. The game ends when:
   - A player gets four discs in a row (vertical, horizontal, or diagonal). They win.
   - The board is full. It's a draw.
4. Invalid moves should be rejected clearly:
   - Dropping in a full column
   - Moving out of turn
   - Moving after the game is over

Out of scope:
- UI support
- Concurrent games
- Move history / undo
- Board size configuration
```

---

## Core Entities and Relationships

With a clear set of requirements in hand, the next step is figuring out what objects we need and how they interact. Look for nouns in your requirements: the game itself, the board where pieces are placed, and the players making moves.

A common mistake is putting everything in one giant class or splitting things unnecessarily. Good design means each class has a single, clear job.

| Entity | Responsibility |
|--------|---------------|
| **Game** | The orchestrator. Holds the Board, tracks which Player's turn it is, manages game state (in progress, won, draw), and enforces turn-based rules. When a player makes a move, Game validates it, tells Board to place the disc, checks if that move won, and switches turns. |
| **Board** | The 7×6 grid where discs live. Owns the grid state and handles disc placement. Knows how to check if a column is full, where a disc should fall, and whether four discs are connected. Doesn't care about whose turn it is or who's winning. |
| **Player** | Represents a person in the game. Simple data holder with a name and disc color. No game logic here. |

---

## Class Design

Now that we've identified the three core entities, the next step is defining their interfaces. Start with a top-down approach — begin with Game since it's the orchestrator and primary entry point. Once we've defined Game's interface, work down to Board and Player.

### Game

The Game class is the orchestration layer. External code should interact with the game only through this class.

| Requirement | What Game must track |
|-------------|---------------------|
| "Two players take turns dropping discs into a 7-column, 6-row board" | The two players, whose turn it is, and the board |
| "The game ends when a player wins or the board is full" | The game state (in progress, won, draw) |
| "A player gets four discs in a row. They win." | Who won (if anyone) |

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState        // IN_PROGRESS, WON, DRAW
    - winner: Player?         // null if no winner yet or draw
```

We make `winner` nullable. In a draw, there simply is no winner, which is clearer than overloading a special "NONE" value.

Next, look at the actions the outside world needs to perform:

| Need from requirements | Method on Game |
|------------------------|---------------|
| "Players take turns dropping discs" | makeMove(player, column) → boolean |
| "Reject moves out of turn" | getCurrentPlayer() → Player |
| "The game ends when..." | getGameState() → GameState |
| "A player gets four discs in a row" | getWinner() → Player? |

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState
    - winner: Player?

    + Game(player1, player2)
    + makeMove(player, column) → boolean
    + getCurrentPlayer() → Player
    + getGameState() → GameState
    + getWinner() → Player?
    + getBoard() → Board
```

`makeMove` is the only method that mutates game state. Everything else is read-only.

### Board

Board owns the grid. It knows where discs are, whether a column has space, how discs "fall," and whether a given move creates four in a row.

| Requirement | What Board must track |
|-------------|----------------------|
| "7-column, 6-row board" | Fixed dimensions: number of rows and columns |
| "A disc falls to the lowest available row in the chosen column" | The current occupancy of each column (the grid) |
| "The board is full. It's a draw." | Whether there is at least one empty cell left |
| "A player gets four discs in a row…" | Enough information in the grid to check contiguous discs |

```
class Board:
    - rows: int = 6
    - cols: int = 7
    - grid: DiscColor?[rows][cols]  // null if empty; otherwise the disc color
```

We store `DiscColor` rather than `Player` to keep the board separately testable.

| Need from requirements | Method on Board |
|------------------------|----------------|
| "Check that column has space before placing" | canPlace(column) → boolean |
| "A disc falls to the lowest available row" | placeDisc(column, color) → int (returns row) |
| "The board is full. It's a draw." | isFull() → boolean |
| "A player gets four discs in a row" | checkWin(row, column, color) → boolean |

```
class Board:
    - rows: int = 6
    - cols: int = 7
    - grid: DiscColor?[rows][cols]

    + Board()
    + getRows() → int
    + getCols() → int
    + canPlace(column) → boolean
    + placeDisc(column, color) → int
    + isFull() → boolean
    + checkWin(row, column, color) → boolean
    + getCell(row, column) → DiscColor?
```

### Player

| Requirement | What Player must track |
|-------------|----------------------|
| "Two players take turns…" | A name or ID so the Game can compare players |
| "their own discs" | The disc color associated with that player |

```
class Player:
    - name: String
    - color: DiscColor

    + Player(name, color)
    + getName() → String
    + getColor() → DiscColor
```

Player stays deliberately simple. All game flow, move validation, and win logic belong elsewhere.

### Final Class Design

```
class Game:
    - board: Board
    - player1: Player
    - player2: Player
    - currentPlayer: Player
    - state: GameState
    - winner: Player?

    + Game(player1, player2)
    + makeMove(player, column) → boolean
    + getCurrentPlayer() → Player
    + getGameState() → GameState
    + getWinner() → Player?
    + getBoard() → Board

class Board:
    - rows: int = 6
    - cols: int = 7
    - grid: DiscColor?[rows][cols]

    + Board()
    + getRows() → int
    + getCols() → int
    + canPlace(column) → boolean
    + placeDisc(column, color) → int
    + isFull() → boolean
    + checkWin(row, column, color) → boolean
    + getCell(row, column) → DiscColor?

class Player:
    - name: String
    - color: DiscColor

    + Player(name, color)
    + getName() → String
    + getColor() → DiscColor

enum GameState:
    IN_PROGRESS, WON, DRAW

enum DiscColor:
    RED, YELLOW
```

---

## Implementation

For each method, follow a consistent pattern: define the core logic (happy path), then consider edge cases.

### Game

The core method is `makeMove` — it encapsulates the entire game flow.

**Core logic:**
1. Place the disc via `board.placeDisc(column, player.getColor())` → returns row
2. Check for win via `board.checkWin(row, column, player.getColor())`
3. If no win, check for draw via `board.isFull()`
4. Switch turn if game is still in progress
5. Return true

**Edge cases (reject before touching state):**
- Game is already over (state is WON or DRAW)
- Wrong player's turn
- Invalid column or column is full (delegated to Board)

```
makeMove(player, column)
    if state != IN_PROGRESS
        return false
    if player != currentPlayer
        return false

    row = board.placeDisc(column, player.getColor())
    if row == -1
        return false

    if board.checkWin(row, column, player.getColor())
        state = WON
        winner = player
    else if board.isFull()
        state = DRAW
    else
        currentPlayer = (player == player1) ? player2 : player1
    return true
```

Notice that we don't check column bounds or whether the column is full in Game. That's the Board's responsibility. We let `placeDisc` handle all board-related validation and return -1 if the move is invalid.

### Board

**placeDisc** — Core logic: find the lowest empty row in that column, place the disc, return the row. Edge cases: column out of bounds or full → return -1.

```
placeDisc(column, color)
    if column < 0 || column >= cols
        return -1
    for row = rows - 1 down to 0
        if grid[row][column] == null
            grid[row][column] = color
            return row
    return -1
```

**checkWin** — Core logic: define four directions (horizontal, vertical, two diagonals). For each, count contiguous discs in both directions from (row, column). If any reaches 4+, return true.

```
checkWin(row, col, color)
    if row < 0 || row >= rows || col < 0 || col >= cols
        return false
    if grid[row][col] != color
        return false

    directions = [[0,1], [1,0], [1,1], [-1,1]]
    for dr, dc in directions:
        count = 1
        count += countInDirection(row, col, dr, dc, color)
        count += countInDirection(row, col, -dr, -dc, color)
        if count >= 4
            return true
    return false

countInDirection(row, col, dr, dc, color)
    count = 0
    r = row + dr
    c = col + dc
    while inBounds(r, c) && grid[r][c] == color
        count++
        r += dr
        c += dc
    return count
```

Helper methods:

```
canPlace(column)
    if column < 0 || column >= cols
        return false
    return grid[0][column] == null

isFull()
    for c = 0 to cols - 1
        if canPlace(c)
            return false
    return true
```

### Complete Code Implementation

```java
public enum DiscColor { RED, YELLOW }

public enum GameState { IN_PROGRESS, WON, DRAW }
```

```java
public class Player {
    private final String name;
    private final DiscColor color;

    public Player(String name, DiscColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName()     { return name; }
    public DiscColor getColor() { return color; }
}
```

```java
public class Board {
    private static final int ROWS = 6;
    private static final int COLS = 7;
    private final DiscColor[][] grid;

    public Board() {
        grid = new DiscColor[ROWS][COLS]; // null = empty
    }

    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }

    public boolean canPlace(int col) {
        if (col < 0 || col >= COLS) return false;
        return grid[0][col] == null;
    }

    public int placeDisc(int col, DiscColor color) {
        if (col < 0 || col >= COLS) return -1;
        for (int row = ROWS - 1; row >= 0; row--) {
            if (grid[row][col] == null) {
                grid[row][col] = color;
                return row;
            }
        }
        return -1;
    }

    public boolean isFull() {
        for (int c = 0; c < COLS; c++) {
            if (canPlace(c)) return false;
        }
        return true;
    }

    public boolean checkWin(int row, int col, DiscColor color) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return false;
        if (grid[row][col] != color) return false;

        int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {-1, 1}};
        for (int[] dir : directions) {
            int count = 1;
            count += countInDirection(row, col, dir[0], dir[1], color);
            count += countInDirection(row, col, -dir[0], -dir[1], color);
            if (count >= 4) return true;
        }
        return false;
    }

    private int countInDirection(int row, int col, int dr, int dc, DiscColor color) {
        int count = 0;
        int r = row + dr, c = col + dc;
        while (r >= 0 && r < ROWS && c >= 0 && c < COLS && grid[r][c] == color) {
            count++;
            r += dr;
            c += dc;
        }
        return count;
    }

    public DiscColor getCell(int row, int col) { return grid[row][col]; }
}
```

```java
public class Game {
    private final Board board;
    private final Player player1;
    private final Player player2;
    private Player currentPlayer;
    private GameState state;
    private Player winner;

    public Game(Player player1, Player player2) {
        this.board = new Board();
        this.player1 = player1;
        this.player2 = player2;
        this.currentPlayer = player1;
        this.state = GameState.IN_PROGRESS;
        this.winner = null;
    }

    public boolean makeMove(Player player, int column) {
        if (state != GameState.IN_PROGRESS) return false;
        if (player != currentPlayer) return false;

        int row = board.placeDisc(column, player.getColor());
        if (row == -1) return false;

        if (board.checkWin(row, column, player.getColor())) {
            state = GameState.WON;
            winner = player;
        } else if (board.isFull()) {
            state = GameState.DRAW;
        } else {
            currentPlayer = (currentPlayer == player1) ? player2 : player1;
        }
        return true;
    }

    public Player getCurrentPlayer() { return currentPlayer; }
    public GameState getGameState()  { return state; }
    public Player getWinner()        { return winner; }
    public Board getBoard()          { return board; }
}
```

### Verification

```
Initial state:
Row 5 (bottom): [RED, YELLOW, RED, _, _, _, _]
Row 4:          [RED, YELLOW, _, _, _, _, _]
currentPlayer = player1, state = IN_PROGRESS

Move 1: player1 → column 0
  placeDisc(0, RED) → row 3
  checkWin(3, 0, RED)?
    Check vertical: (4,0)=RED, (5,0)=RED → count = 3
    No win yet
  currentPlayer = player2

Move 2: player2 → column 1
  placeDisc(1, YELLOW) → row 3
  checkWin(3, 1, YELLOW)?
    Check vertical: (4,1)=YELLOW, (5,1)=YELLOW → count = 3
    No win yet
  currentPlayer = player1

Move 3: player1 → column 2
  placeDisc(2, RED) → row 4
  checkWin(4, 2, RED)?
    Check horizontal: (4,1)=YELLOW → no consecutive 4
    No win yet
  currentPlayer = player2

Move 4: player2 → column 3
  placeDisc(3, YELLOW) → row 5
  checkWin(5, 3, YELLOW)? No
  currentPlayer = player1

Move 5: player1 → column 0
  placeDisc(0, RED) → row 2
  checkWin(2, 0, RED)?
    Check vertical down: (3,0)=RED, (4,0)=RED, (5,0)=RED
    count = 1 + 3 = 4 ✓
    Returns true!
  state = WON, winner = player1

Move 6: player2 tries column 1
  state != IN_PROGRESS → returns false immediately
```

This verifies disc placement, vertical win detection, state transitions, and move rejection after game ends.

---

## Extensibility

### 1. "How would you support different board sizes?"

> "Today I fix the board at 6 rows by 7 columns because that's the requirement. If we wanted configurable sizes, I'd make `rows` and `cols` constructor parameters on Board. All of the placement and win logic already works for arbitrary dimensions because it relies on `rows`, `cols`, and `inBounds`. Game doesn't need to change much: it just chooses what size board to construct."

### 2. "How would you add undo or move history?"

> "Undo belongs in Game because Game controls the lifecycle, turn order, and when state changes. I'd keep a `moveHistory` stack. Each time a move succeeds, I push a small Move record containing the player, row, and column. Undo would pop the last move, clear that cell in the Board, revert `currentPlayer`, and recalculate game state if needed. The Board doesn't need any new logic besides maybe an internal `clearCell` method."

```
class Move:
    - player: Player
    - row: int
    - col: int

undoLastMove()
    if moveHistory.isEmpty()
        return false
    last = moveHistory.pop()
    board.clearCell(last.row, last.col)
    currentPlayer = last.player
    state = IN_PROGRESS
    winner = null
    return true
```

### 3. "How would you add a computer opponent?"

> "I'd keep the game rules exactly where they are. Game and Board don't need to change. I'd introduce a small bot component that looks at the current board and returns a column. From Game's perspective, a bot move is just another call to `makeMove(currentPlayer, column)`."

```
class BotEngine:
    + chooseMove(game) → int

chooseMove(game)
    board = game.getBoard()
    for col = 0 to board.getCols() - 1
        if board.canPlace(col)
            return col
    return -1
```

The important point: we don't change Board or `makeMove`. We just add a thin decision-making layer that chooses a column on behalf of a Player. Keeping Player as simple data and separating identity from decision-making is cleaner than making Player an interface.

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm checking whether you can decompose the problem into logical pieces and implement a working game. You should identify that you need something to represent the board, something to represent players, and something to orchestrate turns. Your `placeDisc` logic should work — find the lowest empty row and place the disc. Win checking is the tricky part. I expect you to at least check horizontal and vertical wins correctly. Diagonal checking is harder, and it's fine if you need hints. Edge cases like full columns or playing out of turn should be handled. If you can play a complete game and correctly identify a winner or draw, you're doing well.

### Mid-level

For mid-level candidates, I expect a cleaner separation of concerns without needing guidance. Game should handle orchestration and turn management. Board should own grid state and win detection. Player should be minimal data. Your `makeMove` should validate state before mutating anything. The win-checking implementation should handle all four directions cleanly. I like seeing the directional vector approach rather than four separate methods. You should be able to discuss at least one extensibility scenario and explain where the changes would live.

### Senior

Senior candidates should produce a design I'd be comfortable reviewing as production code. The class boundaries should be obvious and well-justified. You should proactively point out design decisions: why Player is just data, why GameState is an enum rather than boolean flags, why win checking lives on Board rather than Game. Your `checkWin` should be elegant — the direction vector pattern with a single `countInDirection` helper. I expect you to catch your own edge cases during implementation. For extensibility, you should discuss multiple approaches with tradeoffs. Strong senior candidates often finish early and can discuss networked multiplayer or spectator support.
