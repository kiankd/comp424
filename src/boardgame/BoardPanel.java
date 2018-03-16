package boardgame;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JPanel;

/**
 * Base class for panels used to display boards. This component has two roles:
 * displaying a board, and optionally accepting user input.
 *
 * Double buffering is implemented to reduce the number of times the board needs
 * to be redrawn: if disableDrawingSupport is false (the defaults), the
 * BoardPanel calls drawBoard() to have the board drawn onto an offscreen buffer
 * and responds to any paint() calls by copying from this buffer.
 *
 * Double buffering can be disabled by setting disableDrawingSupport to true in
 * the constructor. In this case, the BoardPanel behaves as a regular Swing
 * component and may contain child components.
 *
 * A BoardPanel may respond to requests for user input by overriding the
 * requestMove() and cancelMoveRequest() methods.
 */
public class BoardPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private Board currentBoard = null;
    protected boolean bufferDirty = true;
    private Image buffer;
    protected boolean disableDrawingSupport = false;

    /** Get the board to be displayed */
    protected Board getCurrentBoard() {
        return currentBoard;
    }

    /** Change the board to be displayed */
    protected void setCurrentBoard(Board b) {
        cancelMoveRequest();
        bufferDirty = true;
        currentBoard = b;
        repaint();
    }

    /**
     * Paint a section of the board to the screen. The default implementation paints
     * the dirty area by copying from the offscreen buffer, and calling the
     * drawBoard() method if necessary.
     */
    public void paint(Graphics g) {
        if (disableDrawingSupport) {
            super.paint(g);
        } else {
            // Check if buffer is correct size
            if (buffer == null || buffer.getWidth(this) != this.getSize().width
                    || buffer.getHeight(this) != this.getSize().height) {

                buffer = this.createImage(getSize().width, getSize().height);
                bufferDirty = true;
            }

            // Repaint board if it has changed
            if (bufferDirty) {
                Graphics buf = buffer.getGraphics();
                buf.setClip(0, 0, buffer.getWidth(this), buffer.getHeight(this));

                if (getCurrentBoard() == null) {
                    buf.setColor(this.getBackground());
                    buf.fillRect(0, 0, buffer.getWidth(this), buffer.getHeight(this));
                } else {
                    drawBoard(buf);
                }
                bufferDirty = false;
            }
            // Paint from our buffer to the screen
            g.drawImage(buffer, 0, 0, this);
        }
    }

    /**
     * Update the offscreen image of the board. This is called by the paint() method
     * when needed. The default implementation fills the buffer with the component's
     * background color.
     */
    public void drawBoard(Graphics g) {
        Rectangle clip = g.getClipBounds();
        g.setColor(this.getBackground());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
    }

    /**
     * Indicate that the BoardPanel should obtain a move from the user, and notify
     * the given BoardPanelListener on completion.
     */
    synchronized protected void requestMove(BoardPanelListener l) {
        throw new UnsupportedOperationException(this.getClass().getName() + " does not implement user input routines.");
    }

    /** Cancel a request for a move. */
    synchronized protected void cancelMoveRequest() {
    }

    /** Interface for objects requesting move inputs from a BoardPanel */
    public interface BoardPanelListener {
        public void moveEntered(Move m);
    }
} // End class
