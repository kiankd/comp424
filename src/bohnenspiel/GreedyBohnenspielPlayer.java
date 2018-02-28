package bohnenspiel;

import student_player.mytools.MyTools;

import java.util.ArrayList;
import java.util.Random;

import bohnenspiel.BohnenspielBoardState;
import bohnenspiel.BohnenspielMove;
import bohnenspiel.BohnenspielPlayer;

/** A random Bohnenspiel player. */
public class GreedyBohnenspielPlayer extends BohnenspielPlayer {
    Random rand = new Random();

    public GreedyBohnenspielPlayer() { super("GreedyPlayer"); }

    /** Choose moves randomly. */
    public BohnenspielMove chooseMove(BohnenspielBoardState board_state)
    {

        // Use code stored in ``mytools`` package.
        MyTools.getSomething();

        // Get the legal moves for the current board state.
        ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();

        int score;
        int bestscore=-1;
        BohnenspielMove bestMove=moves.get(0);
        // We can see the effects of a move like this...
        
        for (int i=0; i<moves.size(); i++)
        {
	        BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
	        BohnenspielMove move1 = moves.get(i);
	        cloned_board_state.move(move1);
	        score=cloned_board_state.getScore(player_id);
	        if (score>bestscore)
	        {
		        bestMove=move1;
		        bestscore=score;
	        }
        }
        
        // But since this is a placeholder algorithm, we won't act on that information.
        return bestMove;
    }
}
