public class Game {
    private final Board board;
    private final Player[] players;
    private  int currentPlayerIndex;
    private  GameStatus status;
    private  Player winner;

    public enum GameStatus {
        IN_PROGRESS, WIN, DRAW
    }
    public Game(String p1Name, String p2Name)
    {

    }
    boolean makemove(Player player, int column)
    {

    }
    private getCurrentplayer()
    {

    }
    private getGameState()
    {

    }
    private getWinner()
    {

    }
    /**private getBoard()
    {}
     **/
}

public class Board{

    private static  final int Rows =6;
    private static final int Cols =7;
    private static final int win_length=4;


}