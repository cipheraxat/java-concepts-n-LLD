# Chess

**Difficulty:** Medium | **Companies:** Google, Amazon, Microsoft

---

## Requirements

> "Design the backend for a two-player chess game. Support standard piece movement, turn alternation, check/checkmate detection, and game termination."

### Clarifying Questions

> **You:** "Standard 8×8 chess with all standard pieces?"
>
> **Interviewer:** "Yes. King, Queen, Rook, Bishop, Knight, Pawn — all standard moves."

> **You:** "Do we need special moves like castling, en passant, pawn promotion?"
>
> **Interviewer:** "Skip those for now. Focus on basic movement, capture, check, and checkmate."

> **You:** "How do we detect check and checkmate?"
>
> **Interviewer:** "A move is legal only if it doesn't leave your own king in check. Checkmate means no legal moves exist for the player in check."

> **You:** "What about stalemate and draw?"
>
> **Interviewer:** "Support stalemate detection — it's a draw if a player has no legal moves but isn't in check."

### Final Requirements

```
Requirements:
1. 8×8 board with standard piece setup
2. Piece hierarchy: King, Queen, Rook, Bishop, Knight, Pawn
3. Each piece generates candidate moves based on its rules
4. A move is legal only if it doesn't leave your own king in check
5. Turn alternation between WHITE and BLACK
6. Detect check, checkmate, and stalemate
7. Game ends on checkmate (win) or stalemate (draw)

Out of scope:
- Castling, en passant, pawn promotion
- Move timer / clock
- Game history / undo
- AI opponent
```

---

## Core Entities and Relationships

| Entity | Responsibility |
|--------|---------------|
| **Game** | Orchestrates turns, validates moves, detects game-ending conditions. |
| **Board** | 8×8 grid of squares. Provides piece lookup, move execution, and board-copy for check simulation. |
| **Square** | Coordinates (row, col) on the board. |
| **Piece** | Abstract base. Each subclass generates candidate (pseudo-legal) moves based on its movement rules. |
| **SlidingPiece** | Abstract subclass for Rook, Bishop, Queen — generates moves along directions until blocked. |
| **Player** | Holds color (WHITE/BLACK) and the king reference for check detection. |

---

## Class Design

### Piece Hierarchy

| Requirement | Design decision |
|-------------|----------------|
| "Each piece has unique movement rules" | Abstract `getCandidateMoves(board)` method on Piece |
| "Rook/Bishop/Queen slide along lines" | SlidingPiece base class with configurable directions |
| "Knight jumps, King moves one square" | Direct offset arrays in respective subclasses |

```
abstract class Piece:
    - color: Color
    - position: Square

    + getCandidateMoves(board) → List<Square>   // pseudo-legal moves
    + getSymbol() → char

abstract class SlidingPiece extends Piece:
    - directions: int[][]

    + getCandidateMoves(board) → 
        for each direction: slide until off-board or blocked

class Rook extends SlidingPiece:
    directions = {{0,1},{0,-1},{1,0},{-1,0}}

class Bishop extends SlidingPiece:
    directions = {{1,1},{1,-1},{-1,1},{-1,-1}}

class Queen extends SlidingPiece:
    directions = all 8 directions

class Knight extends Piece:
    offsets = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}}

class King extends Piece:
    offsets = all 8 adjacent squares

class Pawn extends Piece:
    // forward 1 (or 2 from start), diagonal capture
```

### Board

```
class Board:
    - grid: Piece[8][8]

    + getPiece(row, col) → Piece?
    + movePiece(from, to) → Piece?  // returns captured piece
    + isWithinBounds(row, col) → boolean
    + findKing(color) → Square
    + deepCopy() → Board
    + getAllPieces(color) → List<Piece>
```

### Game

```
class Game:
    - board: Board
    - players: Player[2]
    - currentTurn: Color
    - status: GameStatus

    + makeMove(from, to) → boolean
    + isInCheck(color) → boolean
    + isCheckmate(color) → boolean
    + isStalemate(color) → boolean
    - getLegalMoves(piece) → List<Square>
    - switchTurn() → void

enum GameStatus: ACTIVE, WHITE_WINS, BLACK_WINS, STALEMATE
enum Color: WHITE, BLACK
```

---

## Implementation

### SlidingPiece.getCandidateMoves

**Core logic:** For each direction, keep stepping until we hit the edge or a piece. Can capture opponent pieces but not pass through any piece.

```
getCandidateMoves(board)
    moves = []
    for each (dr, dc) in directions:
        r = position.row + dr
        c = position.col + dc
        while board.isWithinBounds(r, c):
            occupant = board.getPiece(r, c)
            if occupant is null:
                moves.add(Square(r, c))
            else if occupant.color != this.color:
                moves.add(Square(r, c))  // capture
                break
            else:
                break  // own piece blocks
            r += dr
            c += dc
    return moves
```

### Game.makeMove

**Core logic:**
1. Validate it's the right player's piece
2. Check the move is in the piece's candidate moves
3. Simulate the move on a board copy — if it leaves the king in check, reject
4. Execute the move on the real board
5. Check if opponent is now in checkmate or stalemate

```
makeMove(from, to)
    piece = board.getPiece(from)
    if piece is null or piece.color != currentTurn → return false

    legalMoves = getLegalMoves(piece)
    if to not in legalMoves → return false

    board.movePiece(from, to)
    switchTurn()

    if isCheckmate(currentTurn):
        status = (currentTurn == WHITE) ? BLACK_WINS : WHITE_WINS
    else if isStalemate(currentTurn):
        status = STALEMATE

    return true
```

### Game.getLegalMoves (filters pseudo-legal → legal)

```
getLegalMoves(piece)
    candidates = piece.getCandidateMoves(board)
    legal = []
    for each target in candidates:
        // Simulate move on a copy
        copy = board.deepCopy()
        copy.movePiece(piece.position, target)
        if !isInCheck(piece.color, copy):
            legal.add(target)
    return legal
```

### Game.isInCheck

```
isInCheck(color, board)
    kingSquare = board.findKing(color)
    opponentColor = opposite(color)
    for each piece in board.getAllPieces(opponentColor):
        if piece.getCandidateMoves(board).contains(kingSquare):
            return true
    return false
```

### Complete Code Implementation

```java
public enum Color { WHITE, BLACK }
public enum GameStatus { ACTIVE, WHITE_WINS, BLACK_WINS, STALEMATE }
```

```java
public class Square {
    private final int row;
    private final int col;

    public Square(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Square s)) return false;
        return row == s.row && col == s.col;
    }

    @Override
    public int hashCode() { return 31 * row + col; }
}
```

```java
import java.util.List;

public abstract class Piece {
    protected Color color;
    protected Square position;

    public Piece(Color color, Square position) {
        this.color = color;
        this.position = position;
    }

    public abstract List<Square> getCandidateMoves(Board board);
    public abstract char getSymbol();

    public Color getColor()     { return color; }
    public Square getPosition() { return position; }
    public void setPosition(Square position) { this.position = position; }

    public Piece copy() {
        try {
            Piece clone = this.getClass()
                .getConstructor(Color.class, Square.class)
                .newInstance(color, new Square(position.getRow(), position.getCol()));
            return clone;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public abstract class SlidingPiece extends Piece {
    protected int[][] directions;

    public SlidingPiece(Color color, Square position, int[][] directions) {
        super(color, position);
        this.directions = directions;
    }

    @Override
    public List<Square> getCandidateMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        for (int[] dir : directions) {
            int r = position.getRow() + dir[0];
            int c = position.getCol() + dir[1];
            while (board.isWithinBounds(r, c)) {
                Piece occupant = board.getPiece(r, c);
                if (occupant == null) {
                    moves.add(new Square(r, c));
                } else if (occupant.getColor() != this.color) {
                    moves.add(new Square(r, c));
                    break;
                } else {
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
        return moves;
    }
}
```

```java
public class Rook extends SlidingPiece {
    public Rook(Color color, Square position) {
        super(color, position, new int[][]{{0,1},{0,-1},{1,0},{-1,0}});
    }
    @Override public char getSymbol() { return color == Color.WHITE ? 'R' : 'r'; }
}

public class Bishop extends SlidingPiece {
    public Bishop(Color color, Square position) {
        super(color, position, new int[][]{{1,1},{1,-1},{-1,1},{-1,-1}});
    }
    @Override public char getSymbol() { return color == Color.WHITE ? 'B' : 'b'; }
}

public class Queen extends SlidingPiece {
    public Queen(Color color, Square position) {
        super(color, position, new int[][]{{0,1},{0,-1},{1,0},{-1,0},{1,1},{1,-1},{-1,1},{-1,-1}});
    }
    @Override public char getSymbol() { return color == Color.WHITE ? 'Q' : 'q'; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Knight extends Piece {
    private static final int[][] OFFSETS = {
        {-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}
    };

    public Knight(Color color, Square position) { super(color, position); }

    @Override
    public List<Square> getCandidateMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        for (int[] off : OFFSETS) {
            int r = position.getRow() + off[0];
            int c = position.getCol() + off[1];
            if (board.isWithinBounds(r, c)) {
                Piece occupant = board.getPiece(r, c);
                if (occupant == null || occupant.getColor() != this.color) {
                    moves.add(new Square(r, c));
                }
            }
        }
        return moves;
    }

    @Override public char getSymbol() { return color == Color.WHITE ? 'N' : 'n'; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class King extends Piece {
    private static final int[][] OFFSETS = {
        {-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}
    };

    public King(Color color, Square position) { super(color, position); }

    @Override
    public List<Square> getCandidateMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        for (int[] off : OFFSETS) {
            int r = position.getRow() + off[0];
            int c = position.getCol() + off[1];
            if (board.isWithinBounds(r, c)) {
                Piece occupant = board.getPiece(r, c);
                if (occupant == null || occupant.getColor() != this.color) {
                    moves.add(new Square(r, c));
                }
            }
        }
        return moves;
    }

    @Override public char getSymbol() { return color == Color.WHITE ? 'K' : 'k'; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Pawn extends Piece {
    public Pawn(Color color, Square position) { super(color, position); }

    @Override
    public List<Square> getCandidateMoves(Board board) {
        List<Square> moves = new ArrayList<>();
        int direction = (color == Color.WHITE) ? -1 : 1;
        int startRow = (color == Color.WHITE) ? 6 : 1;
        int r = position.getRow();
        int c = position.getCol();

        // Forward one
        int nr = r + direction;
        if (board.isWithinBounds(nr, c) && board.getPiece(nr, c) == null) {
            moves.add(new Square(nr, c));
            // Forward two from start
            int nr2 = r + 2 * direction;
            if (r == startRow && board.getPiece(nr2, c) == null) {
                moves.add(new Square(nr2, c));
            }
        }

        // Diagonal capture
        for (int dc : new int[]{-1, 1}) {
            int nc = c + dc;
            if (board.isWithinBounds(nr, nc)) {
                Piece target = board.getPiece(nr, nc);
                if (target != null && target.getColor() != this.color) {
                    moves.add(new Square(nr, nc));
                }
            }
        }

        return moves;
    }

    @Override public char getSymbol() { return color == Color.WHITE ? 'P' : 'p'; }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Board {
    private final Piece[][] grid = new Piece[8][8];

    public void placePiece(Piece piece) {
        grid[piece.getPosition().getRow()][piece.getPosition().getCol()] = piece;
    }

    public Piece getPiece(int row, int col) { return grid[row][col]; }

    public boolean isWithinBounds(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }

    public Piece movePiece(Square from, Square to) {
        Piece piece = grid[from.getRow()][from.getCol()];
        Piece captured = grid[to.getRow()][to.getCol()];
        grid[to.getRow()][to.getCol()] = piece;
        grid[from.getRow()][from.getCol()] = null;
        piece.setPosition(to);
        return captured;
    }

    public Square findKing(Color color) {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] instanceof King && grid[r][c].getColor() == color)
                    return new Square(r, c);
        throw new IllegalStateException("King not found");
    }

    public List<Piece> getAllPieces(Color color) {
        List<Piece> pieces = new ArrayList<>();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] != null && grid[r][c].getColor() == color)
                    pieces.add(grid[r][c]);
        return pieces;
    }

    public Board deepCopy() {
        Board copy = new Board();
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if (grid[r][c] != null)
                    copy.placePiece(grid[r][c].copy());
        return copy;
    }
}
```

```java
import java.util.ArrayList;
import java.util.List;

public class Game {
    private final Board board;
    private Color currentTurn;
    private GameStatus status;

    public Game() {
        this.board = new Board();
        this.currentTurn = Color.WHITE;
        this.status = GameStatus.ACTIVE;
        setupPieces();
    }

    private void setupPieces() {
        // Black back rank (row 0)
        board.placePiece(new Rook(Color.BLACK, new Square(0, 0)));
        board.placePiece(new Knight(Color.BLACK, new Square(0, 1)));
        board.placePiece(new Bishop(Color.BLACK, new Square(0, 2)));
        board.placePiece(new Queen(Color.BLACK, new Square(0, 3)));
        board.placePiece(new King(Color.BLACK, new Square(0, 4)));
        board.placePiece(new Bishop(Color.BLACK, new Square(0, 5)));
        board.placePiece(new Knight(Color.BLACK, new Square(0, 6)));
        board.placePiece(new Rook(Color.BLACK, new Square(0, 7)));
        for (int c = 0; c < 8; c++)
            board.placePiece(new Pawn(Color.BLACK, new Square(1, c)));

        // White back rank (row 7)
        board.placePiece(new Rook(Color.WHITE, new Square(7, 0)));
        board.placePiece(new Knight(Color.WHITE, new Square(7, 1)));
        board.placePiece(new Bishop(Color.WHITE, new Square(7, 2)));
        board.placePiece(new Queen(Color.WHITE, new Square(7, 3)));
        board.placePiece(new King(Color.WHITE, new Square(7, 4)));
        board.placePiece(new Bishop(Color.WHITE, new Square(7, 5)));
        board.placePiece(new Knight(Color.WHITE, new Square(7, 6)));
        board.placePiece(new Rook(Color.WHITE, new Square(7, 7)));
        for (int c = 0; c < 8; c++)
            board.placePiece(new Pawn(Color.WHITE, new Square(6, c)));
    }

    public boolean makeMove(Square from, Square to) {
        if (status != GameStatus.ACTIVE) return false;
        Piece piece = board.getPiece(from.getRow(), from.getCol());
        if (piece == null || piece.getColor() != currentTurn) return false;

        List<Square> legalMoves = getLegalMoves(piece);
        if (!legalMoves.contains(to)) return false;

        board.movePiece(from, to);
        currentTurn = (currentTurn == Color.WHITE) ? Color.BLACK : Color.WHITE;

        if (isCheckmate(currentTurn)) {
            status = (currentTurn == Color.WHITE) ? GameStatus.BLACK_WINS : GameStatus.WHITE_WINS;
        } else if (isStalemate(currentTurn)) {
            status = GameStatus.STALEMATE;
        }

        return true;
    }

    private List<Square> getLegalMoves(Piece piece) {
        List<Square> legal = new ArrayList<>();
        for (Square target : piece.getCandidateMoves(board)) {
            Board copy = board.deepCopy();
            copy.movePiece(piece.getPosition(), target);
            if (!isInCheck(piece.getColor(), copy)) {
                legal.add(target);
            }
        }
        return legal;
    }

    public boolean isInCheck(Color color, Board b) {
        Square kingSquare = b.findKing(color);
        Color opponent = (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
        for (Piece p : b.getAllPieces(opponent)) {
            if (p.getCandidateMoves(b).contains(kingSquare)) return true;
        }
        return false;
    }

    public boolean isCheckmate(Color color) {
        if (!isInCheck(color, board)) return false;
        return hasNoLegalMoves(color);
    }

    public boolean isStalemate(Color color) {
        if (isInCheck(color, board)) return false;
        return hasNoLegalMoves(color);
    }

    private boolean hasNoLegalMoves(Color color) {
        for (Piece piece : board.getAllPieces(color)) {
            if (!getLegalMoves(piece).isEmpty()) return false;
        }
        return true;
    }

    public GameStatus getStatus() { return status; }
    public Color getCurrentTurn() { return currentTurn; }
}
```

### Verification

```
Setup: Fool's Mate (fastest checkmate).

Step 1: White moves pawn f2→f3 (Square(6,5) → Square(5,5))
  Piece = White Pawn at (6,5). Candidate: (5,5) forward.
  Simulate: White King still safe at (7,4). Legal ✓.
  Execute. Turn → BLACK.

Step 2: Black moves pawn e7→e5 (Square(1,4) → Square(3,4))
  Piece = Black Pawn at (1,4). Candidate: (2,4) and (3,4) from starting row.
  Legal ✓. Execute. Turn → WHITE.

Step 3: White moves pawn g2→g4 (Square(6,6) → Square(4,6))
  Legal ✓. Execute. Turn → BLACK.

Step 4: Black moves queen d8→h4 (Square(0,3) → Square(4,7))
  Queen (SlidingPiece) slides diagonally from (0,3) through (1,4)...
  Wait — (1,4) has Black pawn after step 2, it moved to (3,4). So (1,4) is empty.
  Queen slides: (1,4)→(2,5)→(3,6)→(4,7). All empty. Candidate ✓.
  Simulate: Black King safe. Legal ✓.
  Execute. Turn → WHITE.

  Check: is White in check? White King at (7,4).
  Black Queen at (4,7): candidate moves include diagonal (5,6)→(6,5)→(7,4) ← King!
  (5,5) is White Pawn, BUT diagonal from (4,7) is (5,6)→(6,5): is (5,5)? No, (5,6)→(6,5) is different diagonal.
  Actually: Queen at (4,7), direction (-1 rows not from king... let me check if queen attacks (7,4)).
  Queen at (4,7): sliding (+1,−1): (5,6)→(6,5)→(7,4) ← White King! ✓ CHECK.

  isCheckmate(WHITE)?
  - WHITE in check ✓
  - hasNoLegalMoves: King at (7,4) can try (6,3),(6,4),(6,5),(7,3),(7,5).
    (6,5) has own pawn. (7,3) has own Queen. (7,5) blocked by own Bishop.
    (6,3): Queen can reach via (5,4)→(6,3)? Yes, attacked. 
    (6,4): Queen attacks (5,5)→(6,4)? (5,5) is a pawn blocking.
    All king moves are attacked or blocked. No other piece can block the diagonal.
  → CHECKMATE. status = BLACK_WINS ✓
```

---

## Extensibility

### 1. "How would you add castling?"

> "I'd add a `hasMoved` flag to King and Rook. The King's `getCandidateMoves` checks: king hasn't moved, relevant rook hasn't moved, no pieces between them, king doesn't pass through or land on an attacked square. The move would be a special case in `Board.movePiece` that moves both pieces."

### 2. "How would you add en passant?"

> "I'd track the last move made (a `Move` record on Game). Pawn's `getCandidateMoves` checks if an adjacent enemy pawn just double-moved (last move was a two-square pawn advance to the adjacent column). If so, the diagonal capture square behind it is valid, and the captured pawn is removed."

### 3. "How would you support undo/redo?"

> "I'd use the Command pattern. Each move is a `MoveCommand` recording the from/to squares, captured piece, and any side effects (castling, en passant). Undo reverses the command. A stack of commands enables full undo, and a redo stack holds undone commands."

---

## What is Expected at Each Level?

### Junior

At the junior level, I'm looking for a clean Piece hierarchy with the board as a 2D array. Each piece type should correctly generate candidate moves. Basic move execution (pick up piece, place at target, capture) is the core deliverable. Check detection isn't required but is a plus.

### Mid-level

Mid-level candidates should implement the SlidingPiece abstraction to avoid duplicating sliding logic across Rook, Bishop, and Queen. Check detection via board deep-copy simulation is expected. The distinction between pseudo-legal moves (piece movement rules) and legal moves (doesn't leave king in check) should be clear.

### Senior

Senior candidates would optimize check detection — instead of deep-copying the entire board for every candidate move, you might use incremental attack tables or pin detection. Checkmate and stalemate should both work. You'd discuss the performance trade-off of deep-copy vs. make/unmake move patterns, and how special moves integrate without modifying existing piece logic.