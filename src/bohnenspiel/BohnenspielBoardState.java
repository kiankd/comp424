package bohnenspiel;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Random;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;
import bohnenspiel.BohnenspielMove.MoveType;

/** Stores the state of a Bohnenspiel board. The public members of this class
 * can be used by agents to get information about the current state of the game,
 * such as the current contents of both their own and their opponents pits, whether
 * the game is over, who has won, which player plays first, etc. The ``move``
 * method of this class is used by the server to update the game state in response
 * to moves submitted by players, but can also be used by agents to determine the
 * consequences of a move. */
public class BohnenspielBoardState extends BoardState{

    // Width of the board, in pits.
    public final static int BOARD_WIDTH = 3;

    // Number of turns before a draw is declared
    public final static int MAX_TURN = 200;

    // Maximum number of iterations to implement a move.
    public final static int MAX_TURN_LENGTH = 200;

    Random rand = new Random();

    public enum Direction{
        CCW, CW
    }

    /** Special constant to indicate a draw */
    public static final int DRAW = Board.DRAW;

    /** Special constant to indicate no winner yet */
    public static final int NOBODY = Board.NOBODY;

    /** Special constant to indicate that the game was cancelled because of P0.*/
    public static final int CANCELLED0 = Board.CANCELLED0;

    /** Special constant to indicate that the game was cancelled because of P1.*/
    public static final int CANCELLED1 = Board.CANCELLED1;

    /** Number of seeds in each of the pits.
     * board[0] : Player 0's pits.
     * board[1] : Player 1's pits.
     * In each sub array, the pits are enumerated in CCW order, starting
     * from the left most pit (from that player's perspective) closest to
     * that player. So board[0][0] is the left most pit closest to Player 0,
     * board[0][2 * BOARD_WIDTH - 1] is the left most pit second closest to Player 0
     * (all from Player 0's perspective). Similarly, board[1][0] is the left
     * most pit (from Player 1's perspective) that is closest to Player 1, etc.*/
    private int[][] board = new int[2][1*BOARD_WIDTH];

    private  int [] score = {0,0};

    private int turn_number;
    private int winner;

    // ID of the player whose turn it is.
    private int turn_player;

    // ID of the player that plays first.
    private int first_player;
    
    private int [] skipCredit={2,2};
    private int [] turnsToSkip={0,0};

    public int getCredit(int player)
    {
    	return skipCredit[player];
    }
    
    private void updateCredit(int player)
    {
    	
    	skipCredit[player]=skipCredit[player]-1;
    }
    
    public int getScore(int player)
    {
    	return score[player];
    }
    
    private void updateScore(int player, int points)
    {
    	
    	score[player]=score[player]+points;
    }
    

    
    public BohnenspielBoardState(int[][] board, int turn_number, int winner, int turn_player, int first_player, int[] score, int[] skipCredit, int[] turnsToSkip){
        super();

        this.board[0] = Arrays.copyOf(board[0], 2 * BOARD_WIDTH);
        this.board[1] = Arrays.copyOf(board[1], 2 * BOARD_WIDTH);

        this.turn_number = turn_number;
        this.winner = winner;
        this.turn_player = turn_player;
        this.first_player = first_player;
        this.score = score.clone();
        this.skipCredit=skipCredit.clone();
        this.turnsToSkip=turnsToSkip.clone();
    }

    public BohnenspielBoardState() {
        this.board[0] = new int[2 * BOARD_WIDTH];
        this.board[1] = new int[2 * BOARD_WIDTH];

        placeInitialSeeds();

        turn_number = 0;
        winner = Board.NOBODY;
        turn_player = 0;
        first_player = 0;
        score[0]=0;
        score[1]=0;
        skipCredit[0]=2;
        skipCredit[1]=2;
        turnsToSkip[0]=0;
        turnsToSkip[1]=0;
    }

    private void placeInitialSeeds(){
        for(int i = 0; i < (2)*BOARD_WIDTH; i++){
            this.board[0][i] = 6;
            this.board[1][i] = 6;
        }
    }

    /* Methods for use by agent code. */

    /** Whether the board has been initialized. */
    public boolean isInitialized(){
        return true;
    }

    /** Return the ID of the player whose turn it is. */
    public int getTurnPlayer() {
        return turn_player;
    }

    /** Return the number of turns that have been played so far. */
    public int getTurnNumber() {
        return turn_number;
    }

    /** Returns the player id of the player who has won, or NOBODY
     * if the game is not over, or DRAW if the game ended in a draw.
     * If the game was cancelled due to an infinite move being played,
     * either CANCELLED0 or CANCELLED1 is returned, depending
     * on which player played the infinite move. */
    public int getWinner(){
        return winner;
    }

    /** Return the player id of the player that plays first. */
    @Override
    public int firstPlayer() {
        return 0;
    }

    /** Whether the game represented by this board state has completed. */
    public boolean gameOver() {
        return winner != NOBODY;
    }

    /** Return a random legal move. */
    public Move getRandomMove(){
        ArrayList<BohnenspielMove> moves = getLegalMoves();
        return moves.get(rand.nextInt(moves.size()));
    }

    /** Array of integers representing the current state of the board.
     * First sub array gives player 0's pit information, second
     * sub array gives player 1's pit information. */
    public int[][] getPits(){
        return board;
    }

    /**
     * Get all legal move for the current board state.
     *
     * Returned moves are assumed to be moves for the player whose turn
     * it currently is. */
    public ArrayList<BohnenspielMove> getLegalMoves(){
        ArrayList<BohnenspielMove> legal_moves = new ArrayList<BohnenspielMove>();

        for(int i = 0; i < 2 * BOARD_WIDTH; i++){
            BohnenspielMove move = new BohnenspielMove(i, turn_player);
            if(isLegal(move)){
                legal_moves.add(move);
            }

        }
        
        if(getCredit(turn_player)>0 && turnsToSkip[turn_player]==0)
        {
	        BohnenspielMove move = new BohnenspielMove("skip", turn_player);
	        if(isLegal(move)){
	            legal_moves.add(move);
	        }
        }
        

        
        return legal_moves;
    }

    /**
     * Return whether the supplied move is legal given the current
     * state of the board. */
    public boolean isLegal(BohnenspielMove m){
        
    	if (m.move_type == MoveType.PIT)
    	{
    		return  (m.player_id == turn_player && board[turn_player][m.getPit()] > 0 );
    	}
    		
    	else if (m.player_id == turn_player && m.move_type == MoveType.SKIP && getCredit(turn_player)>0  && turnsToSkip[turn_player]==0)
    	{
    	    return true;
    	}
    	else
    	{
    		return false;
    	}
    }
    	


    /**
     * Apply the given move to the board, updating the board's state.
     * This is used by the server to implement the game logic, but can
     * also be used by the player to explore the consequences of making
     * moves.
     *
     * Returns true if the move was implemented successfuly, and false
     * if the move required a number of iterations exceeding MAX_TURN_LENGTH
     * (ie. an infinite move). */
    public boolean move(Move m) throws IllegalArgumentException {
        BohnenspielMove hus_move = (BohnenspielMove) m;
        if(!isLegal(hus_move)){
            throw new IllegalArgumentException(
                "Invalid move for current context. " +
                "Move: " + hus_move.toPrettyString());
        }
        
        if(hus_move.getMoveType()==MoveType.SKIP)
    	{   	
    		updateCredit(turn_player);
	        turnsToSkip[(turn_player + 1) % 2]=1;
    	}
        else
        {
	        
	        int start_pit = hus_move.getPit();
	        int end_pit = runMove(start_pit);
	
	        // Signals that a too-large move occurred.
	        if(end_pit == -1){
	            return false;
	        }
	        turnsToSkip[turn_player]=0;

        }
        if(turn_player == 1){
            turn_number++;
        }

        turn_player = (turn_player + 1) % 2;
        updateWinner(turn_player);

        return true;
    }

    /* Helper methods for implementing game logic. */

    /**
     * Given a starting pit, a number of seeds, and a direction,
     * adds one seed to each of the next `num_seeds` pits
     * in the given direction from the supplied pit. */
    private int []  sowSeeds(int pit, int num_seeds, Direction d, int side){
        
    	while(num_seeds > 0){
        	
    		boolean crossedOver=false;
 	
      
            pit = getNextPit(pit, d);
            if (pit==0)
            {
            	
            	crossedOver=!crossedOver;
            	if (crossedOver)
            	{
            		
            	if (side==1)
            	{
            		side=0;
            	}
            	else
            	{
            		side=1;
            	}

            	}
            	
            }

            board[side][pit]++;
            num_seeds--;
        }

    	int [] pitAndSide={pit,side};
        return pitAndSide;
    }

    /**
     * Implements a normal move, using the given pit as the starting
     * pit. Implements all relays and captures caused by the move,
     * so this function does not return until the final seed in a
     * sowing sequence is placed in an empty pit. */
    private int runMove(int start_pit){
        int n_seeds_in_hand = board[turn_player][start_pit];
        board[turn_player][start_pit] = 0;

        int end_pit;
        int num_iterations = 0;

        while(true){
            if(num_iterations >= MAX_TURN_LENGTH){
                winner = turn_player == 0 ? CANCELLED0 : CANCELLED1;
                return -1;
            }

            int [] pitandSide = sowSeeds(start_pit, n_seeds_in_hand, Direction.CCW, turn_player);
            end_pit=pitandSide[0];
            int end_side=pitandSide[1];
           
            num_iterations++;
            
            boolean keepChecking=true;
            int end_side_copy=end_side;
            int end_pit_copy=end_pit;

          //Check to see if you can capture constantly until empty
            while(keepChecking)
            {
            	
                if(board[end_side_copy][end_pit_copy] ==2 || board[end_side_copy][end_pit_copy] == 4 || board[end_side_copy][end_pit_copy] ==6 ){
                	
                	updateScore(turn_player, board[end_side_copy][end_pit_copy]); //capture(turn_player,end_pit);
                	board[end_side_copy][end_pit_copy]=0;
                	
                	
                	if (end_pit_copy==0)
                	{
                		end_pit_copy=5;
                		end_side_copy=1-end_side_copy;
                	}
                	else
                	{
                		end_pit_copy-=1;
                	}
                	
                }
                else{
                    // Landed in an empty pit
                	keepChecking=false;
                    break;
                    }
            }
        	
            break;

        }

        return end_pit;
    }

    /** Given a pit index and a direction, returns the index of
     * the next pit in that direction. */
    public int getNextPit(int pit, Direction d){
        if(d == Direction.CCW){
            return (pit + 1) % (BOARD_WIDTH * 2);
        }else if(d == Direction.CW){
            return (pit - 1) % (BOARD_WIDTH * 2);
        }else{
            throw new IllegalArgumentException("Invalid direction.");
        }
    }


    /** Return whether the given player has any more valid moves. */
    private boolean hasValidMoves(int player_id){
        
    	if (getCredit(player_id)>0)
    		return true;
    	
    	int pit = 0;

        for(int i = 0; i < 2 * BOARD_WIDTH; i++){
            if (board[player_id][pit] > 0){
                return true;
            }

            pit = getNextPit(pit, Direction.CCW);
        }

        return false;
    }

    /** Detect when a player has won. Called at the end of a turn. A player
     * wins when their opponent is about to play but has no legal moves.*/
    private void updateWinner(int next_to_play){

        if(winner != NOBODY){
            return;
        }

        if(!hasValidMoves(next_to_play)){
        	
        	
            for(int i = 0; i < (2)*BOARD_WIDTH; i++){
                updateScore(1-next_to_play, this.board[0][i]);
                updateScore(1-next_to_play, this.board[1][i]);
                this.board[1][i] = 0;
                this.board[0][i] = 0;
            }
            
            winner = score[0]>score[1]? 0:1;
            if (score[0]==score[1])
            	winner = DRAW;
            
            return;
        }

        if(turn_number > MAX_TURN){
        	winner = score[0]>score[1]? 0:1;
            if (score[0]==score[1])
            	winner = DRAW;
        }
    }

    @Override
    public Object clone() {
        return new BohnenspielBoardState(board, turn_number, winner, turn_player, first_player, score, skipCredit, turnsToSkip);
    }

    /** Used by the server to force a winner in the event of an error. */
    public void setWinner(int winner){
        this.winner = winner;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Omweso board:\n");
        sb.append("Player 0: \n");
        for(int i = 0; i < 2 * BOARD_WIDTH; i++){
            if(i > 0)
                sb.append(",");

            sb.append(Integer.toString(board[0][i]));
        }

        sb.append("\nPlayer 1: \n");
        for(int i = 0; i < 2 * BOARD_WIDTH; i++){
            if(i > 0)
                sb.append(",");

            sb.append(Integer.toString(board[1][i]));
        }

        sb.append("\nNext to play: " + turn_player);
        sb.append("\nPlays first: " + first_player);
        sb.append("\nWinner: " + winner);
        sb.append("\nTurns played: " + turn_number);

        return sb.toString();
    }
}
