package boardgame;

/**
 * Abstract base class for the state of a board.
 *
 * This will contain methods for updating the board state, as well as a complete
 * specification of the current state of the game. This class captures
 * information that is directly relevant to the game, whereas the Board class
 * contains information and methods for administrative tasks like interfacing
 * with the server.
 */
abstract public class BoardState implements Cloneable {
    abstract public boolean isInitialized();

    abstract public int getTurnPlayer();

    abstract public int getTurnNumber();

    abstract public int getWinner();

    abstract public void setWinner(int winner);

    abstract public int firstPlayer();

    abstract public boolean gameOver();

    abstract public Move getRandomMove();
}
