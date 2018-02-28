package bohnenspiel;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.BoardPanel;
import boardgame.Move;
import bohnenspiel.BohnenspielBoardState;

public class BohnenspielBoard extends Board{

    private BohnenspielBoardState board_state = new BohnenspielBoardState();
    public static int BOARD_WIDTH = BohnenspielBoardState.BOARD_WIDTH;

    private BohnenspielBoard(BohnenspielBoardState board_state){

        super();

        this.board_state = (BohnenspielBoardState) board_state.clone();
    }

    public BohnenspielBoard() {
        board_state = new BohnenspielBoardState();
    }

    @Override
    public void move(Move m) throws IllegalArgumentException {
        BohnenspielMove ccm = (BohnenspielMove) m;
        board_state.move(ccm);
    }

    @Override
    public BoardState getBoardState(){
        return (BoardState) board_state.clone();
    }

    boolean isLegal(BohnenspielMove move){
        return board_state.isLegal(move);
    }

    /** Return the number of seeds in a given pit. */
    int getNumSeeds(int player_id, int pit){
        return board_state.getPits()[player_id][pit];
    }

    /* Methods called by the Server. */

    @Override
    public Move getRandomMove(){
        return board_state.getRandomMove();
    }

    @Override
    public int getWinner() {
        return board_state.getWinner();
    }
    
    @Override
    public int getScore(int player) {
        return board_state.getScore(player);
    }

    @Override
    public void forceWinner(int player_id) {
        board_state.setWinner(player_id);
    }

    @Override
    public int getTurnPlayer() {
        return board_state.getTurnPlayer();
    }

    @Override
    public int getTurnNumber() {
        return board_state.getTurnNumber();
    }

    @Override
    public Object filterMove(Move m) throws IllegalArgumentException {
        return m;
    }

    @Override
    public String getNameForID(int p) {
        return String.format("Player-%d", p);
    }

    @Override
    public int getIDForName(String s) {
        return Integer.valueOf(s.split("-")[1]);
    }

    @Override
    public int getNumberOfPlayers() {
        return 2;
    }

    @Override
    public Move parseMove(String str)
               throws NumberFormatException, IllegalArgumentException {

        return new BohnenspielMove(str);
    }

    @Override
    public Object clone() {
        return new BohnenspielBoard(board_state);
    }

    @Override
    public String toString(){
        return board_state.toString();
    }

    @Override
    public BoardPanel createBoardPanel() { return new BohnenspielBoardPanel(); }
}
