package tablut;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import boardgame.Player;

public abstract class TablutPlayer extends Player {
    public TablutPlayer(String name) {
        super(name);
    }

    public TablutPlayer() {
        super("Player");
    }

    @Override
    final public Board createBoard() {
        return new TablutBoard();
    }

    @Override
    final public Move chooseMove(BoardState boardState) {
        return chooseMove((TablutBoardState) boardState);
    }

    // Method to be overwritten.
    public abstract Move chooseMove(TablutBoardState boardState);
}
