package tablut;

import java.util.List;
import java.util.Random;

import boardgame.Move;
import boardgame.Player;
import coordinates.Coord;
import coordinates.Coordinates;

public class GreedyTablutPlayer extends TablutPlayer {
    private Random rand = new Random(1848);

    public GreedyTablutPlayer() {
        super("GreedyPlayer");
    }

    public GreedyTablutPlayer(String name) {
        super(name);
    }

    @Override
    public Move chooseMove(TablutBoardState bs) {
        List<TablutMove> options = bs.getAllLegalMoves();

        // Set an initial move as some random one.
        TablutMove bestMove = options.get(rand.nextInt(options.size()));

        // This greedy player seeks to capture as many opponents as possible.
        int opponent = bs.getOpponent();
        int minNumberOfOpponentPieces = bs.getNumberPlayerPieces(opponent);
        boolean moveCaptures = false;

        // Iterate over move options and evaluate them.
        for (TablutMove move : options) {
            // To evaluate a move, clone the boardState so that we can do modifications on
            // it.
            TablutBoardState cloneBS = (TablutBoardState) bs.clone();

            // Process that move, as if we actually made it happen.
            cloneBS.processMove(move);

            // Check how many opponent pieces there are now, maybe we captured some!
            int newNumberOfOpponentPieces = cloneBS.getNumberPlayerPieces(opponent);

            // If this move caused some capturing to happen, then do it! Greedy!
            if (newNumberOfOpponentPieces < minNumberOfOpponentPieces) {
                bestMove = move;
                minNumberOfOpponentPieces = newNumberOfOpponentPieces;
                moveCaptures = true;
            }

            /*
             * If we also want to check if the move would cause us to win, this works for
             * both! This will check if black can capture the king, and will also check if
             * white can move to a corner, since if either of these things happen then a
             * winner will be set.
             */
            if (cloneBS.getWinner() == player_id) {
                bestMove = move;
                moveCaptures = true;
                break;
            }
        }

        /*
         * The king-functionality below could be included in the above loop to improve
         * efficiency. But, this is written separately for the purpose of exposition to
         * students.
         */

        // If we are SWEDES we also want to check if we can get closer to the closest
        // corner. Greedy!
        // But let's say we'll only do this if we CANNOT do a capture.
        if (player_id == TablutBoardState.SWEDE && !moveCaptures) {
            Coord kingPos = bs.getKingPosition();

            // Don't do a move if it wouldn't get us closer than our current position.
            int minDistance = Coordinates.distanceToClosestCorner(kingPos);

            // Iterate over moves from a specific position, the king's position!
            for (TablutMove move : bs.getLegalMovesForPosition(kingPos)) {
                /*
                 * Here it is not necessary to actually process the move on a copied boardState.
                 * Note that it is more efficient NOT to copy the boardState. Consider this
                 * during implementation...
                 */
                int moveDistance = Coordinates.distanceToClosestCorner(move.getEndPosition());
                if (moveDistance < minDistance) {
                    minDistance = moveDistance;
                    bestMove = move;
                }
            }
        }
        return bestMove;
    }

    // For Debugging purposes only.
    public static void main(String[] args) {
        TablutBoardState b = new TablutBoardState();
        Player swede = new GreedyTablutPlayer("GreedySwede");
        swede.setColor(TablutBoardState.SWEDE);

        // Player swede = new RandomTablutPlayer("RandomSwede");
        // swede.setColor(TablutBoardState.SWEDE);
        //
        Player muscovite = new GreedyTablutPlayer("GreedyMuscovite");
        muscovite.setColor(TablutBoardState.MUSCOVITE);
        ((GreedyTablutPlayer) muscovite).rand = new Random(4);

        // Player muscovite = new RandomTablutPlayer("RandomMuscovite");
        // muscovite.setColor(TablutBoardState.MUSCOVITE);

        Player player = muscovite;
        while (!b.gameOver()) {
            Move m = player.chooseMove(b);
            b.processMove((TablutMove) m);
            player = (player == muscovite) ? swede : muscovite;
            System.out.println("\nMOVE PLAYED: " + m.toPrettyString());
            b.printBoard();
        }
        System.out.println(TablutMove.getPlayerName(b.getWinner()) + " WIN!");
    }
}
