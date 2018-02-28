package boardgame;


/**
 * Abstract base class for moves. Moves must provide
 * a way to be transformed to a string to be sent over
 * the network. Board implementations must provide a
 * facility for generating moves from such strings.
 *
 * The string representation of a move may not contain newline
 * characters.
 *
 * @author pkelle
 */
abstract public class Move implements Cloneable {
    /** Represent as an easily interpreted string */
    abstract public String toPrettyString();
    /** Represent as an easily parsed string to be sent
     * over the network and stored in the logfile. */
    abstract public String toTransportable();
    /** Return the player who plays this move */
    abstract public int getPlayerID();
    /** Set the player who plays this move */
    abstract public void setPlayerID(int player_id);
    /** Set whether the move comes from the board or a client */
    abstract public void setFromBoard(boolean from_board);
    /** The player IDs to which this move should be
     * sent by the server. This method may return null to indicate
     * moves are to be sent to all players. The default
     * implementation returns null. To indicate a move
     * should be logged but not sent to any players, pass
     * an empty array. */
    public int[] getReceivers() { return null; }

    /** Whether to write the move to the log */
    abstract public boolean doLog();
}
