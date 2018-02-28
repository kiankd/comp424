package boardgame;

/**
 * Abstract base class for board representations.
 *
 * INITIALIZATION
 * Board implementations must provide a default constructor accepting no
 * arguments so that they can be instantiated by the server or client software.
 *
 * PLAYER IDs
 * A Board implementation must define a mapping between players and
 * nonzero integer IDs 1,2,3 ... through the getNameForID()
 * and getIDForName() methods. (e.g. "WHITE" -> 1. "BLACK" -> 2)
 *
 * SERVER INTERACTION
 * The server asks the Board who's turn it is, using getTurnPlayer(), and requests
 * a move from the appropriate player. Once the move is received, the board is asked
 * to "filter" the moves, by calling the filterMove() method. This method may replace
 * the player's move with an arbitrary sequence of moves. These moves are then
 * executed in the given order by calling the Board.move() method.
 *
 * To allow environment actions, the Board may return the value Board.BOARD in
 * it's getTurnPlayer() method. Then a move will be obtained using the getBoardMove()
 * method instead of querying a player.
 *
 * To allow for incomplete information, boards may indicate who receives a move
 * by overriding the Move.getReceivers() method in the move objects. In this case
 * the board state known to the clients may differ from that of the server since
 * clients may not received all moves. All moves are still written to the log file.
 *
 * BOARD STATE ENCODING
 * It is important that all board state changes are implemented as moves for
 * logging purposes, even if no player receives the Move message. This is so
 * that the log file contains a complete description of the game.
 *
 * When a log file is loaded, the logfile viewer (ServerGUI) passes the sequence
 * of moves in the log to the Board.move() method. This should reconstruct the
 * state of the board completely.
 *
 * CLIENT INTERACTION
 * The client simply updates the board with moves received from the server, by
 * calling the Board.move() methods for all received moves. Take this into account
 * if not sending all moves to all players. It may be necessary to create seperate
 * board classes for use by the clients and the server.
 *
 * This scheme should allow a large variety of games/environments to be
 * represented while ensuring appropriate logging and communication.
 *
 * Essentially, CHANGES TO THE BOARD'S STATE SHOULD ONLY BE MADE IN THE
 * Board.move() METHOD to ensure proper communication and logging.
 */
abstract public class Board implements Cloneable {
    /** Special constant to indicate a draw */
    public static final int DRAW = Integer.MAX_VALUE;

    /** Special constant to indicate no winner yet */
    public static final int NOBODY = Integer.MAX_VALUE - 1;

    /** Special constant to indicate that the game was cancelled because of P0.*/
    public static final int CANCELLED0 = Integer.MAX_VALUE - 2;

    /** Special constant to indicate that the game was cancelled because of P1.*/
    public static final int CANCELLED1 = Integer.MAX_VALUE - 3;

    /** Special constant to indicate that the environment wants to play a turn */
    public static final int BOARD = Integer.MAX_VALUE;

    /** Return winner ID, DRAW or NOBODY if no winner yet. Or CANCELLED[0 | 1]
     * if the game has been cancelled as a result of a player action. */
    abstract public int getWinner();

    /** Set a winner without finishing the game. Argument may
     * be a player ID or DRAW. */
    abstract public void forceWinner( int win );

    /** Return the next player, or the special constant BOARD if the environment
     is to execute a move. */
    abstract public int getTurnPlayer();

    /** Get the number of turns played. */
    abstract public int getTurnNumber();

    /** If getTurn() returns the special constant BOARD, the server queries
    the board using this method for a move, instead of querying the players */
    public Move getBoardMove() {
        throw new UnsupportedOperationException( "getBoardMove() not implemented." );
    }

    /** This is a hook for the board to modify moves just before they are
    executed using the move() method.  It may return either a Move object, or
    an array of moves to be executed instead of the given move. Default
    implementation just returns its argument. Throw an Exception if the
    move is illegal. */
    public Object filterMove(Move m) throws IllegalArgumentException {
        return m;
    }

    /** Execute a move, throw an exception if Illegal. */
    abstract public void move( Move m ) throws IllegalArgumentException;

    /** Get the state of the board. */
    abstract public BoardState getBoardState();

    /** Get the name corresponding to a player ID. This function
     should also return an appropriate string for the value Board.BOARD
     if the board actions mechanism is used. */
    abstract public String getNameForID( int player_id);

    /** Get the player ID corresponding to name. */
    abstract public int getIDForName( String s );

    /** Get the number of players. This must correspond to the
     number of player IDs! */
    abstract public int getNumberOfPlayers();

    /** Parse a move from a string */
    abstract public Move parseMove( String str )
        throws NumberFormatException, IllegalArgumentException;

    /** Return an independent copy of the board. */
    abstract public Object clone();

    /** Get a random legal move. */
    abstract public Move getRandomMove();

    /** Construct a BoardPanel to display boards of this class. The
     * default implementation returns a board that displays a blank
     * area. */
    public BoardPanel createBoardPanel() { return new BoardPanel(); }

} // End class Board
