package bohnenspiel;

import boardgame.Move;

public class BohnenspielMove extends Move{

    int player_id = -1;

    public enum MoveType{
        // Choose a pit to begin sowing from
        PIT,
        SKIP,
        NOTHING
    }

    int pit;
    MoveType move_type;
    boolean from_board = false;

    /**
     * Create a degenerate move.
     */
    public BohnenspielMove(){
        this.move_type = MoveType.NOTHING;
    }

    /**
     * Create a standard pit-choosing move.
     * @param pit which pit to start from
     */
    public BohnenspielMove(int pit){
        this.pit = pit;
        this.move_type = MoveType.PIT;
    }

    /**
     * Create a standard pit-choosing move.
     * @param pit which pit to start from
     */
    public BohnenspielMove(int pit, int player_id){
        this.pit = pit;
        this.player_id = player_id;
        this.move_type = MoveType.PIT;
    }
    
    /**
     * Create a standard pit-choosing move.
     * @param pit which pit to start from
     */
    public BohnenspielMove(String skip, int player_id){
        this.player_id = player_id;
        this.move_type = MoveType.SKIP;
    }


    /**
     * Constructor from a string.
     * The string will be parsed and, if it is correct, will construct
     * the appropriate move. Mainly used by the server for reading moves
     * from a log file, and for constructing moves from strings sent
     * over the network.
     *
     * @param str The string to parse.
     */
    public BohnenspielMove(String str) {
        String[] components = str.split(" ");

        String type_string = components[0];
        this.player_id = Integer.valueOf(components[1]);

        if(type_string.equals("NOTHING")){
            this.move_type = MoveType.NOTHING;
            
        }else if(type_string.equals("SKIP")){
            this.move_type = MoveType.SKIP;

        }else if(type_string.equals("PIT")){
            this.pit = Integer.valueOf(components[2]);
            this.move_type = MoveType.PIT;

        }else{
            throw new IllegalArgumentException(
                "Received a string that cannot be interpreted as a BohnenspielMove.");
        }
    }

    public MoveType getMoveType() {
        return move_type;
    }

    public int getPit() {
        return pit;
    }

    /* Members below here are only used by the server; Player agents
     * should not worry about them. */

    @Override
    public void setPlayerID(int player_id) {
        this.player_id = player_id;
    }

    @Override
    public int getPlayerID() {
        return player_id;
    }

    @Override
    public void setFromBoard(boolean from_board) {
        this.from_board = from_board;
    }

    public boolean getFromBoard() {
        return from_board;
    }

    public int[] getReceivers() {
        return null;
    }

    public boolean doLog(){
        boolean regular_move = move_type == MoveType.PIT;

        return regular_move;
    }

    @Override
    public String toPrettyString() {
        String s = "";

        switch(move_type){
            case NOTHING:
                s = String.format("Player %d ends turn.", player_id);
                break;
            case SKIP:
                s = String.format("Player %d skips turn.", player_id);
                break;
            case PIT:
                s = String.format("Player %d plays pit %d", player_id, pit);
                break;
        }

        return s;
    }

    @Override
    public String toTransportable() {
        String s = "";

        switch(move_type){
            case NOTHING:
                s = String.format("NOTHING %d", player_id);
                break;
            case SKIP:
                s = String.format("SKIP %d", player_id);
                break;
            case PIT:
                s = String.format("PIT %d %d", player_id, pit);
                break;
        }

        return s;
    }
}
