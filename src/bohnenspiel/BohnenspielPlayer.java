package bohnenspiel;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import boardgame.Player;


/** An abstract class for Bohnenspiel players. */
public abstract class BohnenspielPlayer extends Player {
    protected int opponent_id;

    public BohnenspielPlayer() { super("HusPlayer"); }
    public BohnenspielPlayer(String s) { super(s); }

    final public Board createBoard() { return new BohnenspielBoard(); }

    final public void setColor( int c ) {
        player_id = c;
        opponent_id = (c + 1) % 2;
    }

    // Casts the board to the correct type.
    final public Move chooseMove(BoardState bs)
    {
        // Cast the arguments to the objects we want to work with.
        BohnenspielBoardState board_state = (BohnenspielBoardState) bs;
        return this.chooseMove(board_state);
    }

    // The method that students implement.
    public abstract Move chooseMove(BohnenspielBoardState board_state);
}
