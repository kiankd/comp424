package student_player;

import java.util.ArrayList;

import bohnenspiel.BohnenspielBoardState;
import bohnenspiel.BohnenspielMove;
import bohnenspiel.BohnenspielPlayer;
import bohnenspiel.BohnenspielMove.MoveType;
import student_player.mytools.MyTools;

/** A Hus player submitted by a student. */
public class StudentPlayer extends BohnenspielPlayer {

    /** You must modify this constructor to return your student number.
     * This is important, because this is what the code that runs the
     * competition uses to associate you with your agent.
     * The constructor should do nothing else. */
    public StudentPlayer() { super("xxxxxxxxx"); }

    /** This is the primary method that you need to implement.
     * The ``board_state`` object contains the current state of the game,
     * which your agent can use to make decisions. See the class
bohnenspiel.RandomPlayer
     * for another example agent. */
    public BohnenspielMove chooseMove(BohnenspielBoardState board_state)
    {
        // Get the contents of the pits so we can use it to make decisions.
        int[][] pits = board_state.getPits();

        // Use ``player_id`` and ``opponent_id`` to get my pits and opponent pits.
        int[] my_pits = pits[player_id];
        int[] op_pits = pits[opponent_id];

        // Use code stored in ``mytools`` package.
        MyTools.getSomething();

        // Get the legal moves for the current board state.
        ArrayList<BohnenspielMove> moves = board_state.getLegalMoves();
        BohnenspielMove move = moves.get(0);
  
     
        // We can see the effects of a move like this...
        BohnenspielBoardState cloned_board_state = (BohnenspielBoardState) board_state.clone();
        cloned_board_state.move(move);


        // But since this is a placeholder algorithm, we won't act on that information.
        return move;
    }
}