package tablut;

import java.awt.event.ComponentEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import boardgame.BoardPanel;
import coordinates.Coord;
import coordinates.Coordinates;
import tablut.TablutBoardState.Piece;
import java.util.ArrayList;
import java.util.HashSet;

public class TablutBoardPanel extends BoardPanel implements MouseListener, MouseMotionListener, ComponentListener {
    private static final long serialVersionUID = -7761120622454119879L;

    // Static constants about the GUI design.
    static final Color BACKGROUND_COLOR = Color.GRAY; // gray because yolo
    static final Color MOVE_COLOR = Color.RED; // pretty for projecting moves
    static final Color CORNER_COLOR = new Color(211, 211, 211); // silver
    static final Color CENTER_COLOR = new Color(255, 215, 0); // gold
    static final Color PROTECTED_COLOR = new Color(218, 165, 32); // a gentle "goldenrod"
    static final Color BOARD_COLOR2 = new Color(245, 222, 179); // a subtle "wheat" color for the board...
    static final Color BOARD_COLOR1 = new Color(244, 164, 96); // complemented with a tasteful "sandybrown".
    static final Color MUSCOVITE_COLOR = Color.BLACK; // black
    static final Color SWEDE_COLOR = Color.WHITE; // white
    static final int HIGHLIGHT_THICKNESS = 3;
    static final int BOARD_DIM = TablutBoardState.BOARD_SIZE;
    static final int PIECE_SIZE = 50;
    static final int FONT_SIZE = (int) (PIECE_SIZE * 0.5);
    static final int SQUARE_SIZE = (int) (PIECE_SIZE * 1.25); // Squares 25% bigger than pieces.

    // The class below is for the purpose of constructing an individual piece on the
    // board.
    final class GUIPiece {
        private Piece pieceType;
        public int xPos;
        public int yPos;
        public Coord coord;

        // Construct a piece!
        public GUIPiece(Piece pieceType, int xPos, int yPos, Coord coord) {
            this.pieceType = pieceType;
            this.xPos = xPos;
            this.yPos = yPos;
            this.coord = coord;
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
            if (pieceType != Piece.BLACK) {// draw a border around whites
                g.setColor(Color.BLACK);
                g.drawOval(x, y, PIECE_SIZE, PIECE_SIZE);
            }
            if (pieceType == Piece.KING) {
                g.setFont(new Font("TimesRoman", Font.BOLD, FONT_SIZE));
                g.setColor(Color.BLACK); // Black font color
                int textMod = (int) (PIECE_SIZE * (9.0 / 50.0)); // helps with centering
                g.drawString("K", cx - textMod, cy + textMod);
            }
        }
    }

    // Stores all board pieces.
    private ArrayList<GUIPiece> boardPieces;
    private GUIPiece pieceToMove;
    private HashSet<Coord> legalCoordsToMoveTo = new HashSet<>();

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
    protected void cancelMoveRequest() {
        listener = null;
    }

    // Drawing a board.
    @Override
    public void drawBoard(Graphics g) {
        super.drawBoard(g); // Paints the background and stuff.

        // Make it look pretty.
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint board.
        Color crtColor = BOARD_COLOR2;
        for (Coord c : Coordinates.iterCoordinates()) {
            g.setColor(crtColor);
            if (Coordinates.isCorner(c))
                g.setColor(CORNER_COLOR);
            else if (Coordinates.isCenter(c))
                g.setColor(CENTER_COLOR);
            else if (Coordinates.isCenterOrNeighborCenter(c))
                g.setColor(PROTECTED_COLOR);
            else
                g.setColor(crtColor);

            // Fill and paint the rectangle with the current color.
            g.fillRect(c.y * SQUARE_SIZE, c.x * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            crtColor = (crtColor == BOARD_COLOR1) ? BOARD_COLOR2 : BOARD_COLOR1;
        }

        // If there are any requested moves, highlight those squares. and then
        // draw border around square if human could move there, given current click.
        for (Coord c : legalCoordsToMoveTo) {
            g.setColor(MOVE_COLOR);
            Graphics2D g2 = (Graphics2D) g;
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(new BasicStroke(HIGHLIGHT_THICKNESS));
            g.drawRect(c.y * SQUARE_SIZE, c.x * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            g2.setStroke(oldStroke);
        }

        // Put the pieces on the board. We store them in a list since they may need to
        // be accessed later.
        updateBoardPieces();
        for (GUIPiece gp : boardPieces) {
            gp.draw(g);
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    // A bit sketchy, but has to be done to highlight possible moves for human.
    private void humanRepaint() {
        bufferDirty = true;
        repaint();
    }

    // Basically all of the work happens here for letting humans play.
    @Override
    public void mousePressed(MouseEvent e) {
        if (listener == null)
            return;
        int clickX = e.getX();
        int clickY = e.getY();

        // Check if we clicked on a piece.
        if (pieceToMove == null) {
            for (GUIPiece gp : boardPieces) {
                if (clickInCircle(clickX, clickY, gp.xPos, gp.yPos)) {
                    pieceToMove = gp;
                    break;
                }
            }
            if (pieceToMove != null) {
                // If we did, now we have to check a few things.
                TablutBoardState bs = (TablutBoardState) getCurrentBoard().getBoardState();
                Coord c = pieceToMove.coord;
                if (!bs.turnPlayerCanMoveFrom(c)) {
                    pieceToMove = null;
                    legalCoordsToMoveTo = new HashSet<>();
                }
                for (TablutMove move : bs.getLegalMovesForPosition(c)) {
                    legalCoordsToMoveTo.add(move.getEndPosition());
                }
            }
        } else { // Then the piece was already clicked on, and now we want to actually move it.
            TablutBoardState bs = (TablutBoardState) getCurrentBoard().getBoardState();
            Coord destination = null;
            for (Coord c : Coordinates.iterCoordinates()) { // not efficient, but its okay, just a GUI.
                if (bs.coordIsEmpty(c)) {
                    int xPos = c.y * SQUARE_SIZE + SQUARE_SIZE / 2; // have to switch x and y.
                    int yPos = c.x * SQUARE_SIZE + SQUARE_SIZE / 2;
                    if (clickInSquare(clickX, clickY, xPos, yPos)) {
                        destination = c;
                        break;
                    }
                }
            }
            if (legalCoordsToMoveTo.contains(destination)) {
                // Now we have a destination, lets do the move!
                TablutMove move = new TablutMove(pieceToMove.coord, destination, bs.getTurnPlayer());
                if (bs.isLegal(move)) { // sanity check
                    listener.moveEntered(move);
                    cancelMoveRequest();
                }
            }
            pieceToMove = null;
            legalCoordsToMoveTo = new HashSet<>();
        }
        humanRepaint();
    }

    // Helpers.
    @Override
    public Color getBackground() {
        return BACKGROUND_COLOR;
    }

    private void updateBoardPieces() {
        TablutBoardState bs = (TablutBoardState) getCurrentBoard().getBoardState();
        boardPieces = new ArrayList<>();
        for (Coord c : Coordinates.iterCoordinates()) {
            Piece p = bs.getPieceAt(c);
            if (p != Piece.EMPTY) {
                int xPos = c.y * SQUARE_SIZE + SQUARE_SIZE / 2;
                int yPos = c.x * SQUARE_SIZE + SQUARE_SIZE / 2;
                GUIPiece gp = new GUIPiece(p, xPos, yPos, c);
                boardPieces.add(gp);
            }
        }
    }

    private static boolean clickInCircle(int x, int y, int cx, int cy) {
        return Math.pow(cx - x, 2) + Math.pow(cy - y, 2) < Math.pow(PIECE_SIZE / 2, 2);
    }

    private static boolean clickInSquare(int x, int y, int cx, int cy) {
        return Math.abs(x - cx) < SQUARE_SIZE / 2 && Math.abs(y - cy) < SQUARE_SIZE / 2;
    }

    /* Don't use these interface methods */
    public void mouseDragged(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void componentResized(ComponentEvent arg0) {
    }

    public void mouseClicked(MouseEvent arg0) {
    }

    public void mouseEntered(MouseEvent arg0) {
    }

    public void mouseExited(MouseEvent arg0) {
    }

    public void mouseMoved(MouseEvent arg0) {
    }

    public void componentMoved(ComponentEvent arg0) {
    }

    public void componentShown(ComponentEvent arg0) {
    }

    public void componentHidden(ComponentEvent arg0) {
    }
}
