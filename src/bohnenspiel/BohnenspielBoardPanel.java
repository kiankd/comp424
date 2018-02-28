package bohnenspiel;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;


import java.lang.Math;

import boardgame.BoardPanel;
import bohnenspiel.BohnenspielBoard;

/**
 * A board panel for display and input for Bohnenspiel. */
public class BohnenspielBoardPanel extends BoardPanel
implements MouseListener, MouseMotionListener, ComponentListener{

    private static final long serialVersionUID = 2648134549469132906L;

    // Some constants affecting display
    static final Color BGCOLOR = new Color(50,100,200);
    static final Color PITCOLOR = new Color(100, 191, 111);
    static final Color AHusENTCOLOR = new Color(255, 255, 255);
    static final Color FONTCOLOR = new Color(0, 0, 0);

    static final Color[] TEAMCOLOR= {new Color(50,50,50), new Color(217, 93, 81), new Color(56, 93, 103), new Color(250, 250,250)};
    static final Color HIGHLIGHTCOLOR= new Color(100, 100, 100);

    static final int BORDERX = 16;
    static final int BORDERY = 30;

    static final int WIDTH_IN_PITS = BohnenspielBoard.BOARD_WIDTH;

    final int pit_offset = 2;
    int pit_radius = 0;
    int grid_size = 0;
    float barrier_height = 0.7f;
    int[][] pit_centres = new int[2][WIDTH_IN_PITS * 4];

    // Width and height of game board
    float w, h;
    int x_off, y_off;

    //    Point dragStart = null, dragEnd = null;
    BoardPanelListener list = null; // Who needs a move input ?

    public BohnenspielBoardPanel() {
        this.addMouseListener( this );
        this.addMouseMotionListener( this );
        this.addComponentListener( this );
    }

    protected void requestMove( BoardPanelListener l ) {
        list = l;
    }

    protected void cancelMoveRequest() {
        list = null;
    }

    /*
     * Converts from pit indices used by the GUI to a player id and
     * pit indices used by the board. First item is player id, second
     * item is pit index. */
    protected int[] guiPitToBoardPit(int gui_pit){
        int player_id = gui_pit < 2 * WIDTH_IN_PITS ? 0 : 1;
        int pit = 0;
        
        if (gui_pit==0){
        	pit=5;
        }
        else if (gui_pit==1){
        	pit=4;
        }
        else if (gui_pit==2){
        	pit=3;
        }
        else if (gui_pit==3){
        	pit=2;
        }
        else if (gui_pit==4){
        	pit=1;
        }
        else if (gui_pit==5){
        	pit=0;
        }
        else if (gui_pit==6){
        	pit=0;
        }
        else if (gui_pit==7){
        	pit=1;
        }
        else if (gui_pit==8){
        	pit=2;
        }
        else if (gui_pit==9){
        	pit=3;
        }
        else if (gui_pit==10){
        	pit=4;
        }
        else{
        	pit=5;
        }
        

        return new int[] {player_id, pit};
    }

    private boolean clickInCircle(int centreX, int centreY, int clickX, int clickY, int radius){
       return (Math.pow((centreX - clickX), 2) +
               Math.pow((centreY - clickY), 2)) < Math.pow(radius, 2);
    }




    public void mousePressed(MouseEvent arg0) {
        // No move have been requested. E.g. if reviewing history.
        if(list == null)
            return;

        int clickX = arg0.getX();
        int clickY = arg0.getY();

        int clicked_pit = -1;

        for(int i = 0; i < WIDTH_IN_PITS * 4; i++){
            if(clickInCircle(pit_centres[0][i], pit_centres[1][i], clickX, clickY, pit_radius)){
                clicked_pit = i;
                break;
            }
        }

        BohnenspielBoard board = (BohnenspielBoard) getCurrentBoard();

        int player_id, pit;
        if(clicked_pit >= 0){
            int[] board_pit = guiPitToBoardPit(clicked_pit);

            player_id = board_pit[0];
            pit = board_pit[1];
            
            BohnenspielMove move = new BohnenspielMove(pit, player_id);
            if(board.isLegal(move)){
                list.moveEntered(move);
                cancelMoveRequest();
            }
        }
        
        else {
    		BohnenspielMove move = new BohnenspielMove("skip", board.getTurnPlayer());
            if(board.isLegal(move)){
                list.moveEntered(move);
                cancelMoveRequest();
        }   
    	}
    	

        repaint();
    }

    public void mouseDragged(MouseEvent arg0) {}

    public void mouseReleased(MouseEvent arg0) {}

    /** Paint the board to the offscreen buffer. This does the painting
     * of the actual board, but not the pieces being moved by the user.*/
    public void drawBoard( Graphics g ) {
        Rectangle clip = g.getClipBounds();

        g.setColor(BGCOLOR);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        float game_height = 4.0f + barrier_height;

        x_off = BORDERX;
        y_off = BORDERY;

        // Width of entire area within border
        w = clip.width - 2 * BORDERX;
        h = clip.height - 2 * BORDERY;

        if(w/h > WIDTH_IN_PITS / 4){
            float w_prime = h * WIDTH_IN_PITS / game_height;
            x_off += (w - w_prime) / 2.0f;
            w = w_prime;
        }else{
            float h_prime = w * (game_height / WIDTH_IN_PITS);
            y_off += (h - h_prime) / 2.0f;
            h = h_prime;
        }

        grid_size = (int)(h / game_height);
        pit_radius = (grid_size - (2 * pit_offset)) / 2;

        g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
        FontMetrics fm = g.getFontMetrics();

        // Draw the pits, and store the centres while we're at it.
        for( int i = 0; i < 4; i++ ) {
            int y=0;
        	if (i==1 || i==3){
        		
        		y = y_off + (i-1)*grid_size;
        	}
            else
            {	
            	  y = y_off + i*grid_size;
            }
        	
        	
            if(i > 1){
                y += barrier_height * grid_size;
            }

            for( int j = 0; j < WIDTH_IN_PITS; j++ ) {
           
            	
            	int x = 0;
            	if (i==0){
            		
            		x = x_off + (j-WIDTH_IN_PITS) * grid_size;
            	}
            	
            	else if(i==3 || i==1){
            		x = x_off + j * grid_size;
            	}
            	
            	else 
            	{
            		x = x_off + (j-WIDTH_IN_PITS) * grid_size;
            		
            	}
            	
                

            	
                g.setColor(PITCOLOR);

                g.fillOval(
                    x + pit_offset, y + pit_offset, 2 * pit_radius, 2 * pit_radius);

                pit_centres[0][i * WIDTH_IN_PITS + j] = x + pit_offset + pit_radius;
                pit_centres[1][i * WIDTH_IN_PITS + j] = y + pit_offset + pit_radius;


                g.setColor(FONTCOLOR);
                int pl = (i == 0 || i == 2) ? WIDTH_IN_PITS - 1 - j : j;
                if (i==0)
                {
                	pl=2*WIDTH_IN_PITS-1-j;
                }
                if (i==1)
                {
                	pl=WIDTH_IN_PITS-1-j;
                }
                if (i==2)
                {
                	pl=j;
                }
                if (i==3)
                {
                	pl=pl+WIDTH_IN_PITS;
                }
                


                String pit_label = Integer.toString(pl);

                Rectangle2D r = fm.getStringBounds(pit_label, g).getBounds2D();

                if(i == 0 || i == 2){
                    g.drawString(
                        pit_label, x + grid_size / 2 - (int)r.getWidth() / 2, y + grid_size / 2 + pit_radius / (int)Math.sqrt(2) + (int)r.getHeight());
                }else{
                    g.drawString(
                        pit_label, x + grid_size / 2 - (int)r.getWidth() / 2,
                        y + grid_size / 2 + pit_radius / (int)Math.sqrt(2) + (int)r.getHeight());
                }
            }
        }

        g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
        String p0 = "P0";
        Rectangle2D r = fm.getStringBounds(p0, g).getBounds2D();
        g.drawString(
            p0, x_off + (int)((w - r.getWidth()) / 2.0), 20);//(int)(h + r.getHeight() * 0.4));

        String p1 = "P1";
        r = fm.getStringBounds(p1, g).getBounds2D();
        g.drawString(
            p1, x_off + (int)((w - r.getWidth()) / 2), clip.height - 5);// r.getHeight() * 0.4));
    }

    /** We use the double-buffering provided by the superclass, but draw
     *  the "transient" elements in the paint() method. */
    public void paint( Graphics g ) {
        // Paint the board as usual, this will use the offscreen buffer
        super.paint(g);


        int n_seeds = 0;
        BohnenspielBoard board = (BohnenspielBoard) getCurrentBoard();
        if(board == null){
            return;
        }
        BohnenspielBoardState boardState = (BohnenspielBoardState) board.getBoardState();
        
        g.setFont(new Font("TimesRoman", Font.PLAIN, 25));

        FontMetrics fm = g.getFontMetrics();
        Rectangle clip = g.getClipBounds();
        
        String score0 = "Score: "+boardState.getScore(0);
        Rectangle2D r2 = fm.getStringBounds(score0, g).getBounds2D();
        g.drawString(
            score0, 100+x_off + (int)((w - r2.getWidth()) / 2.0), 20);//(int)(h + r.getHeight() * 0.4));
        
        String score1 = "Score: " +boardState.getScore(1);
        Rectangle2D r3 = fm.getStringBounds(score1, g).getBounds2D();
        g.drawString(
            score1, 100+x_off + (int)((w - r3.getWidth()) / 2), clip.height - 5);// r.getHeight() * 0.4));
        

        // Paint the number of seeds in each pit
        g.setColor(FONTCOLOR);
        for(int i = 0; i < 4 * WIDTH_IN_PITS; i++){
            int[] board_pit = guiPitToBoardPit(i);

            n_seeds = board.getNumSeeds(board_pit[0], board_pit[1]);

            if(n_seeds > 0){
                String s = Integer.toString(n_seeds);
                Rectangle2D r = fm.getStringBounds(s, g).getBounds2D();

                int pit_centre_x = pit_centres[0][i];
                int pit_centre_y = pit_centres[1][i];

                g.drawString(
                    s, (int)(pit_centre_x - r.getWidth()/2),
                    (int)(pit_centre_y + r.getHeight() * 0.4));
            }
        }
    }

    public void componentResized(ComponentEvent arg0) {
    }

    /* Don't use these interface methods */
    public void mouseClicked(MouseEvent arg0) {}
    public void mouseEntered(MouseEvent arg0) {}
    public void mouseExited(MouseEvent arg0) {}
    public void mouseMoved(MouseEvent arg0) {}
    public void componentMoved(ComponentEvent arg0) {}
    public void componentShown(ComponentEvent arg0) {}
    public void componentHidden(ComponentEvent arg0) {}
}


