package bohnenspiel;

import java.util.ArrayList;
import java.util.Random;

import bohnenspiel.BohnenspielBoardState;
import bohnenspiel.BohnenspielMove;
import bohnenspiel.BohnenspielPlayer;
import bohnenspiel.BohnenspielMove.MoveType;

/** A random Bohnenspiel player. */
public class RandomBohnenspielPlayer extends BohnenspielPlayer {
    Random rand = new Random();

    public RandomBohnenspielPlayer() { super("RandomPlayer"); }

    /** Choose moves randomly. */
    public BohnenspielMove chooseMove(BohnenspielBoardState board_state)
    {
        // Pick a random move from the set of legal moves.
        ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
        BohnenspielMove move = moves.get(rand.nextInt(moves.size()));
        
        return move;
    }
}
