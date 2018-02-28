package tablut;

import boardgame.Board;
import boardgame.BoardState;
import boardgame.Move;

import tablut.TablutMove;
import tablut.Coord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TablutBoardState extends BoardState {

	/* Useful constants. */
	public static final int ILLEGAL = -1;
	public static final int SWEDE = 0;
	public static final int MUSCOVITE = 1;
	public static final int BOARD_SIZE = 9; // 9x9 board for tablut
	public static final int MAX_TURNS = 50;
	public static final boolean EASY_MOVEMENT = true;
	
	public static enum Piece { BLACK, WHITE, KING, EMPTY }
	public static List<Coord> allGameCoords;
	public static HashMap<Piece, String> piecesToSymbols;
	public static HashMap<Piece, Integer> piecesToPlayer;
	static {
		piecesToSymbols = new HashMap<>();
		piecesToSymbols.put(Piece.BLACK, "B");
		piecesToSymbols.put(Piece.WHITE, "W");
		piecesToSymbols.put(Piece.KING, "K");
		piecesToSymbols.put(Piece.EMPTY, " ");
		
		piecesToPlayer = new HashMap<>();
		piecesToPlayer.put(Piece.BLACK, MUSCOVITE);
		piecesToPlayer.put(Piece.WHITE, SWEDE);
		piecesToPlayer.put(Piece.KING, SWEDE);
		piecesToPlayer.put(Piece.EMPTY, ILLEGAL);
		
		allGameCoords = new ArrayList<>();
		for (int i = 0; i < BOARD_SIZE; i++) {
			for (int j = 0; j < BOARD_SIZE; j++) {
				allGameCoords.add(new Coord(i, j));
			}
        }
	}
	
	private static int FIRST_PLAYER = 0; // first player white, second player black
	
	/* These are our data storage things. */
	private Piece[][] board = new Piece[BOARD_SIZE][BOARD_SIZE];
	private List<Coord> muscoviteCoords;
	private List<Coord> swedeCoords;
	private Coord kingPosition;
	private Random rand = new Random(1917);
	private int turnPlayer;
	private int turnNumber;
	
	// Initial Board State creation. The genesis constructor.
	public TablutBoardState() {
		super();
		
		// Initialize to empty
		for (Coord c : allGameCoords) {
			board[c.x][c.y] = Piece.EMPTY;
		}
		
		/* We now place the pieces on the board */
		int middlePosition = 4;
		board[middlePosition][middlePosition] = Piece.KING;
		
		// Blacks. (Muscovites)
		List<Integer> blackSidePieces = Arrays.asList(3, 4, 5);
		List<Integer> axes = Arrays.asList(0, BOARD_SIZE - 1);
		for (Integer axis : axes) {
			for (Integer position : blackSidePieces) {
				board[position][axis] = Piece.BLACK;
				board[axis][position] = Piece.BLACK;
			}
		}
		axes = Arrays.asList(1, BOARD_SIZE - 2);
		for (Integer axis : axes) {
			board[axis][middlePosition] = Piece.BLACK;
			board[middlePosition][axis] = Piece.BLACK;
		}
		
		// Whites. (Swedes)
		axes = Arrays.asList(2, 3, BOARD_SIZE - 4, BOARD_SIZE - 3);
		for (Integer axis : axes) {
			board[axis][middlePosition] = Piece.WHITE;
			board[middlePosition][axis] = Piece.WHITE;
		}
		
		// Update player piece coordinates
		updatePlayerPieceCoords();
	}
	
	/** 
	 * This method needs to be called after every move to update the internal
	 * parameters of the board so that students can quickly access coordinates
	 * without (necessarily) having to do a big iteration. */
	private void updatePlayerPieceCoords() {
		swedeCoords = new ArrayList<Coord>();
		muscoviteCoords = new ArrayList<Coord>();
		for (Coord c : allGameCoords) {
			Piece piece = getPiece(c);
			if (piecesToPlayer.get(piece) == SWEDE)
				swedeCoords.add(c);
			else if (piecesToPlayer.get(piece) == MUSCOVITE)
				muscoviteCoords.add(c);
			else if (piece == Piece.KING)
				kingPosition = c;
		}
	}
	
	public boolean coordIsEmpty(Coord c) {
		return getPiece(c) == Piece.EMPTY;
	}
	
	/**
	 * Get all legal moves for the player. This may be expensive, so it may
	 * be more desirable to select a subset of moves from specific positions.
	 */
	public ArrayList<TablutMove> getAllLegalMoves() {
		ArrayList<TablutMove> allMoves = new ArrayList<>();
		for (Coord pos : getPlayerPieceCoordinates()) {
			allMoves.addAll(getLegalMovesForPosition(pos));
		}
		return allMoves;
	}
	
	/**
     * Get all legal moves for the passed position in the current board state.
     *
     * Returned moves are assumed to be moves for the player whose turn
     * it currently is. */
    public ArrayList<TablutMove> getLegalMovesForPosition(Coord position) {
        ArrayList<TablutMove> legalMoves = new ArrayList<TablutMove>();
        for (Coord end : allGameCoords) {
        		if (coordIsEmpty(end)) {
	        		TablutMove move = new TablutMove(position, end, this.turnPlayer);
	        		if (isLegal(move)) {
	        			legalMoves.add(move);
	        		}
        		}
        }
        return legalMoves;
    }
	
    /**
     * Returns all of the coordinates of pieces belonging to the current player.
     */
    public List<Coord> getPlayerPieceCoordinates() {
    		if (turnPlayer == MUSCOVITE)
    			return new ArrayList<Coord>(muscoviteCoords); // copy list so no funny business
    		else if (turnPlayer == SWEDE)
    			return new ArrayList<Coord>(swedeCoords);	
		return null;
    }
    
    public boolean isLegal(TablutMove move) {
    		// TODO: is the below statement necessary?
    		if (turnPlayer != move.getPlayerID() || move.getPlayerID() == ILLEGAL) 
    			return false;
    		
    		// Get useful things.
    		Coord start = move.getStartPosition();
    		Coord end = move.getEndPosition();
    		Piece startPiece = getPiece(start); // this will check if the position is on the board
    		
    		// Check that the piece being requested actually belongs to the player.
    		if (piecesToPlayer.get(startPiece) != turnPlayer)
    			return false;
    		
    		// Next, make sure move doesn't end on a piece.
    		if (!coordIsEmpty(end))
    			return false;
    		
    		// Next, make sure the move is actually a move.
    		if (start.equals(end))
    			return false;
    		
    		// Now for the actual game logic. First we make sure it is moving like a rook.
    		if (!(start.x == end.x || start.y == end.y))
    			return false;
    		
    		if (EASY_MOVEMENT && (start.maxDifference(end) > 1))
    			return false;
    		
    		// Now we make sure it isn't moving through any other pieces.
    		for (Coord throughCoordinate : start.getCoordsBetween(end)) {
    			if (!coordIsEmpty(throughCoordinate))
    				return false;
    		}
    		
    		// All of the conditions have been satisfied, we have a legal move!
    		return true;
    }
    
    // Useful helper functions.
    public Piece getPiece(int xPosition, int yPosition) {
    		return board[xPosition][yPosition];
    }
    
    public Piece getPiece(Coord position) {
    		return getPiece(position.x, position.y);
    }
    
    /* Used by server */
	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public int getTurnPlayer() {
		return turnPlayer;
	}

	@Override
	public int getTurnNumber() {
		return turnNumber;
	}

	@Override
	public int getWinner() {
		// TODO: check if king is at corner -- SWEDES WIN
		// TODO: check if king is captured -- MUSCOVITES WIN
		return Board.NOBODY;
	}

	@Override
	public void setWinner(int winner) {
		// TODO Auto-generated method stub

	}

	@Override
	public int firstPlayer() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean gameOver() {
		return turnNumber > MAX_TURNS;
	}

	@Override
	public Move getRandomMove() {
		ArrayList<TablutMove> moves = getAllLegalMoves();
        return moves.get(rand.nextInt(moves.size()));
	}

	@Override
	public int getScore(int player) {
		// TODO Auto-generated method stub
		return 0;
	}

	
	/*** Debugging functionality is found below. ***/
	
	// Useful method to show the board.
	public void printBoard() {
		int lastRow = 0;
		for (Coord c : allGameCoords) {
			if (c.x > lastRow) {
				lastRow = c.x;
				System.out.println();
			}
			System.out.print(piecesToSymbols.get(this.getPiece(c)) + " ");
		}
		System.out.println();
	}
	
	// Very useful for printing any special positions (such as legal moves).
	// onto the board visualization.
	public void printSpecialBoard(List<Coord> positions) {
		int lastRow = 0;
		for (Coord c : allGameCoords) {
			if (c.x > lastRow) {
				lastRow = c.x;
				System.out.println();
			}
			String s = piecesToSymbols.get(this.getPiece(c));
			for (Coord special : positions) {
				if (special.equals(c)) {
					s = "*";
				}
			}
			System.out.print(s + " ");
		}
		System.out.println();
	}
	
	public static void main(String[] args) {
		TablutBoardState b = new TablutBoardState();
//		b.printBoard();
//		System.out.println("---------\n\n");
		
		// compute branching factors
		for (Integer player : Arrays.asList(MUSCOVITE, SWEDE)) {
			b.turnPlayer = player;
			int totalMoves = b.getAllLegalMoves().size();
			System.out.println(String.format("Player %d, %d possible moves.", player, totalMoves));
		}
		
		// Single coordinate observation for debugging.
		for (int i=0; i < BOARD_SIZE; i++) {
			b.turnPlayer = MUSCOVITE;
			Coord start = new Coord(0, i);
			System.out.println("------------------\n" + start.toString());
			List<TablutMove> moves = b.getLegalMovesForPosition(start);
			List<Coord> positions = new ArrayList<>();
			for (TablutMove move : moves)
				positions.add(move.getEndPosition());
			b.printSpecialBoard(positions);	
		}
	}
}
