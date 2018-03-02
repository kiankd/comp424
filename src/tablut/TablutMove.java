package tablut;

import boardgame.Move;
import coordinates.Coord;
import coordinates.Coordinates;
import tablut.TablutBoardState;

public class TablutMove extends Move {

    /* We need to store the following information about a move. */
    private int playerId = TablutBoardState.ILLEGAL;
    private int xStart = -1;
    private int yStart = -1;
    private int xEnd = -1;
    private int yEnd = -1;
    private boolean fromBoard = false;

    /* Constructors */
    public TablutMove(Coord start, Coord end, int playerId) {
        this(start.x, start.y, end.x, end.y, playerId);
    }

    public TablutMove(int xStart, int yStart, int xEnd, int yEnd, int playerId) {
        this.xStart = xStart;
        this.yStart = yStart;
        this.xEnd = xEnd;
        this.yEnd = yEnd;
        this.playerId = playerId;
    }

    public TablutMove(String formatString) {
        String[] components = formatString.split(" ");
        try {
            this.xStart = Integer.parseInt(components[0]);
            this.yStart = Integer.parseInt(components[1]);
            this.xEnd = Integer.parseInt(components[2]);
            this.yEnd = Integer.parseInt(components[3]);
            this.playerId = Integer.parseInt(components[4]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Received an uninterpretable string format for a TablutMove.");
        }
    }

    /* Useful Getters */
    public Coord getStartPosition() {
        return Coordinates.get(this.xStart, this.yStart);
    }

    public Coord getEndPosition() {
        return Coordinates.get(this.xEnd, this.yEnd);
    }

    public static String getPlayerName(int player) {
        if (player != TablutBoardState.MUSCOVITE && player != TablutBoardState.SWEDE)
            return "Illegal";
        return (player == TablutBoardState.MUSCOVITE) ? "Muscovites" : "Swedes";
    }

    /*
     * Members below here are only used by the server; Player agents should not
     * worry about them.
     */

    @Override
    public int getPlayerID() {
        return this.playerId;
    }

    @Override
    public void setPlayerID(int player_id) {
        this.playerId = player_id;
    }

    @Override
    public void setFromBoard(boolean from_board) {
        this.fromBoard = from_board;
    }

    @Override
    public boolean doLog() {
        return true;
    }

    @Override
    public String toPrettyString() {
        return String.format("%s (p%d) move (%d, %d) to (%d, %d)", getPlayerName(playerId), playerId, xStart, yStart,
                xEnd, yEnd);
    }

    @Override
    public String toTransportable() {
        return String.format("%d %d %d %d %d", xStart, yStart, xEnd, yEnd, playerId);
    }
}
