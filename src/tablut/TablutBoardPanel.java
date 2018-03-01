package tablut;

import java.awt.event.ComponentEvent;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import boardgame.BoardPanel;
import boardgame.BoardPanel.BoardPanelListener;
import coordinates.Coord;
import coordinates.Coordinates;
import tablut.TablutBoardState.Piece;
import java.util.ArrayList;

public class TablutBoardPanel extends BoardPanel implements MouseListener, MouseMotionListener, ComponentListener {
	private static final long serialVersionUID = -7761120622454119879L;
	
	// Static constants about the GUI design.
	static final Color BACKGROUND_COLOR = Color.GRAY; // gray because yolo
	static final Color CORNER_COLOR = new Color(211, 211, 211); // silver
	static final Color CENTER_COLOR = new Color(255, 215, 0); // gold
	static final Color PROTECTED_COLOR = new Color(218, 165, 32); // a gentle "goldenrod"
	static final Color BOARD_COLOR2 = new Color(245, 222, 179); // a soft "wheat" color for the board...
	static final Color BOARD_COLOR1 = new Color(244, 164, 96); // complemented with a tasteful "sandybrown".
	static final Color MUSCOVITE_COLOR = Color.BLACK; // black
	static final Color SWEDE_COLOR = Color.WHITE; // white
	static final int BOARD_DIM = TablutBoardState.BOARD_SIZE;
	static final int PIECE_SIZE = 50;
	static final int FONT_SIZE = 25;
	static final int SQUARE_SIZE = (int) (PIECE_SIZE * 1.25); // Squares 25% bigger than pieces.
	
	
    // The class below is for the purpose of constructing an individual piece on the board.
    final class GUIPiece {
    	private Piece pieceType;
    	public int xPos;
    	public int yPos;
    	
    	// Construct a piece!
    	public GUIPiece(Piece pieceType, int xPos, int yPos) { 
    		this.pieceType = pieceType; 
    		this.xPos = xPos;
    		this.yPos = yPos;
    	}
    	
    	public void draw(Graphics g) {
    		draw(g, xPos, yPos);
    	}

    	public void draw(Graphics g, int cx, int cy) {
			int x = cx - PIECE_SIZE / 2;
			int y = cy - PIECE_SIZE / 2;

			g.setColor(pieceType == Piece.BLACK ? MUSCOVITE_COLOR : SWEDE_COLOR);
			
			// Paint piece.
			g.fillOval(x, y, PIECE_SIZE, PIECE_SIZE);
			if (pieceType!= Piece.BLACK) {// draw a border around whites
				g.setColor(Color.BLACK); 
				g.drawOval(x, y, PIECE_SIZE, PIECE_SIZE);
			}
			if (pieceType == Piece.KING) {
				g.setFont(new Font("TimesRoman", Font.BOLD, FONT_SIZE));
				g.setColor(Color.BLACK); // Black font color
				g.drawString("K", cx-9, cy+9);
			}
		}
    }
	
    // Stores all board pieces.
	private ArrayList<GUIPiece> boardPieces;
	
	// Internal parameters.
	BoardPanelListener listener;
	
	// Constructing with this as the listener for everything.
	public TablutBoardPanel() {
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
	}

	// Overriding BoardPanel methods to help with listener functionality.
	@Override
	protected void requestMove(BoardPanelListener l) { 
		listener = l; 
		System.out.println("REQUESTED.");
	}

	@Override
    protected void cancelMoveRequest() { listener = null; }
	
	// Drawing a board.
	@Override
    public void drawBoard(Graphics g) { 
    	super.drawBoard(g); // Paints the background and stuff.
    	
    	// Make it look pretty.
    	((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

		// Paint board.
    	Color crtColor;
		for (int row = 0; row < BOARD_DIM; row++) {
			crtColor = ((row & 1) != 0) ? BOARD_COLOR1 : BOARD_COLOR2;
			g.setColor(crtColor);
			for (int col = 0; col < BOARD_DIM; col++) {
				if (Coordinates.isCorner(row, col))
					g.setColor(CORNER_COLOR);
				else if (Coordinates.isCenter(row, col))
					g.setColor(CENTER_COLOR);
				else if (Coordinates.isCenterOrNeighborCenter(row, col))
					g.setColor(PROTECTED_COLOR);
				else
					g.setColor(crtColor);
					
				// Fill and paint the rectangle with the current color.
				g.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
				crtColor = (crtColor == BOARD_COLOR1) ? BOARD_COLOR2 : BOARD_COLOR1;
			}
		}
		// Put the pieces on the board.
		updateBoardPieces();
		for (GUIPiece gp : boardPieces) 
			gp.draw(g);
    }

    @Override
    public void paint(Graphics g) {
    	super.paint(g);
    }
    
	// Basically all of the work happens here for dynamic changes.
	@Override
	public void mousePressed(MouseEvent e) {
		repaint();
	}

	// Helpers.
	@Override
	public Color getBackground() { return BACKGROUND_COLOR; }
	
	private void updateBoardPieces() {
		TablutBoardState bs = (TablutBoardState) getCurrentBoard().getBoardState();
		boardPieces = new ArrayList<>();
		for (Coord c : Coordinates.iterCoordinates()) {
			Piece p = bs.getPieceAt(c);
			if (p != Piece.EMPTY) {
				int xPos = c.y * SQUARE_SIZE + SQUARE_SIZE / 2;
				int yPos = c.x * SQUARE_SIZE + SQUARE_SIZE / 2;
				GUIPiece gp = new GUIPiece(p, xPos, yPos);
				boardPieces.add(gp);
			}
		}
	}

    public static boolean contains(int x, int y, int cx, int cy) {
      return (cx - x) * (cx - x) + (cy - y) * (cy - y) < 
    		  PIECE_SIZE / 2 * PIECE_SIZE / 2;
    }
	
    /* Don't use these interface methods */
	public void mouseDragged(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
    public void componentResized(ComponentEvent arg0) {}
	public void mouseClicked(MouseEvent arg0) {}
    public void mouseEntered(MouseEvent arg0) {}
    public void mouseExited(MouseEvent arg0) {}
    public void mouseMoved(MouseEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {}
    public void componentShown(ComponentEvent arg0) {}
    public void componentHidden(ComponentEvent arg0) {}
}
