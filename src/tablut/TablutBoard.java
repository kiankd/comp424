package tablut;

import boardgame.Board;
import boardgame.BoardPanel;
import boardgame.BoardState;
import boardgame.Move;

public class TablutBoard extends Board {

    private TablutBoardState boardState;

    public TablutBoard() {
        super();
        boardState = new TablutBoardState();
    }

    @Override
    public int getWinner() {
        return boardState.getWinner();
    }

    @Override
    public void forceWinner(int win) {
        boardState.setWinner(win);
    }

    @Override
    public int getTurnPlayer() {
        return boardState.getTurnPlayer();
    }

    @Override
    public int getTurnNumber() {
        return boardState.getTurnNumber();
    }

    @Override
    public void move(Move m) throws IllegalArgumentException {
        boardState.processMove((TablutMove) m);
    }

    @Override
    public BoardState getBoardState() {
        return boardState;
    }

    @Override
    public BoardPanel createBoardPanel() {
        return new TablutBoardPanel();
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
    public Move parseMove(String str) throws NumberFormatException, IllegalArgumentException {
        return new TablutMove(str);
    }

    @Override
    public Object clone() {
        TablutBoard board = new TablutBoard();
        board.boardState = (TablutBoardState) boardState.clone();
        return board;
    }

    @Override
    public Move getRandomMove() {
        return boardState.getRandomMove();
    }

}
