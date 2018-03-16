package boardgame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

/**
 * GUI for the server. Can also be run standalone to examine a logfile, or to
 * launch servers/clients.
 *
 * No synchronization is done in this class. It is assumed that any external
 * calls are invoked by the AWT event dispatch thread.
 */
public class ServerGUI extends JFrame implements BoardPanel.BoardPanelListener {
    /** The list of games for which servers can be launched */
    protected static final String[] BOARD_CLASSES = { "tablut.TablutBoard" };
    /** The list of players that can be launched */
    protected static final String[] PLAYER_CLASSES = { "tablut.RandomTablutPlayer", "tablut.GreedyTablutPlayer",
            "student_player.StudentPlayer", };
    private static final int BOARD_SIZE = 800;
    private static final int LIST_WIDTH = 280;

    private Board board; // Most recently updated board
    private int currentBoard = -1; // Displayed board index
    private Vector moveHistory = new Vector();
    private Vector boardHistory = new Vector();
    private String outcome = null; // The outcome message from the server

    // Menu actions
    private AbstractAction firstAction, backAction, fwdAction, lastAction;
    private AbstractAction openAction, closeAction;
    private AbstractAction playAsAction;
    private AbstractAction killServerAction;
    private AbstractAction clientActions[];
    private AbstractAction serverActions[];
    private AbstractAction fromHereAction;

    // GUI Components
    private JList moveList;
    private MoveListModel moveListModel = new MoveListModel();
    private JLabel statusLabel;
    private final ServerGUI theFrame = this;
    private BoardPanel boardPanel = new BoardPanel();

    // Reference to the server, if any
    private Server server;

    // Should we be getting a move from the user
    private boolean userMoveNeeded = false, userMoveRequested = false;

    // A player instance for human input
    private HumanPlayer theHumanPlayer = null;

    public static void printUsage() {
        System.err.println("Usage: java ServerGUI [filename]\n" + "  Where 'filename' is the log file to load.");
    }

    public static void main(String[] args) {
        ServerGUI g = new ServerGUI();
        if (args.length > 0)
            try {
                g.loadLogFile(args[0]);
            } catch (Exception e) {
                printUsage();
                return;
            }
        g.pack();
        g.setVisible(true);
    }

    protected ServerGUI(Server svr) {
        this();
        setServer(svr);
    }

    /** Set the server from which we are receiving moves */
    private void setServer(Server svr) {
        this.server = svr;
        // Create a human player object and an action to launch it
        if (theHumanPlayer == null)
            theHumanPlayer = new HumanPlayer(svr.getBoard());
        killServerAction.setEnabled(true);
        openAction.setEnabled(false);
        closeAction.setEnabled(false);
        enableServerActions(false);
    }

    public ServerGUI() {
        // This constructor just builds the GUI, nothing
        // very interesting here...
        super("Board Game");
        currentBoard = -1;
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Define some menu/toolbar actions
        firstAction = new AbstractAction("First move", new ImageIcon("image/first.png")) {
            public void actionPerformed(ActionEvent arg0) {
                setCurrentBoard(0);
            }
        };

        backAction = new AbstractAction("Prev. move", new ImageIcon("image/prev.png")) {
            public void actionPerformed(ActionEvent arg0) {
                setCurrentBoard(currentBoard - 1);
            }
        };

        fwdAction = new AbstractAction("Next move", new ImageIcon("image/next.png")) {
            public void actionPerformed(ActionEvent arg0) {
                setCurrentBoard(currentBoard + 1);
            }
        };

        lastAction = new AbstractAction("Last move", new ImageIcon("image/last.png")) {
            public void actionPerformed(ActionEvent arg0) {
                setCurrentBoard(boardHistory.size() - 1);
            }
        };

        openAction = new AbstractAction("Open log...") {
            public void actionPerformed(ActionEvent ev) {
                JFileChooser chooser = new JFileChooser();
                chooser.setCurrentDirectory(new File(Server.log_dir));
                chooser.setFileFilter(new FileFilter() {
                    public boolean accept(File arg0) {
                        return arg0.isDirectory() || arg0.getName().endsWith(".log");
                    }

                    public String getDescription() {
                        return "Board game log files";
                    }
                });
                int returnVal = chooser.showOpenDialog(theFrame);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        clearData();
                        loadLogFile(chooser.getSelectedFile().getAbsolutePath());
                        closeAction.setEnabled(true);
                        killServerAction.setEnabled(false);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(theFrame, e, "Load Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

            }
        };

        closeAction = new AbstractAction("Close log") {
            public void actionPerformed(ActionEvent ev) {
                clearData();
                enableServerActions(true);
            }
        };

        lastAction.setEnabled(false);
        firstAction.setEnabled(false);
        backAction.setEnabled(false);
        fwdAction.setEnabled(false);
        closeAction.setEnabled(false);

        // An action to launch a human player
        playAsAction = new AbstractAction("Launch human player") {
            public void actionPerformed(ActionEvent arg0) {
                // Diable this action
                enableLaunchActions(false);
                // Create and launch a new client
                Client c = new Client(theHumanPlayer, server.getHostName(), server.getPort());
                (new Thread(c)).start();
            }
        };

        // An action to terminate the server
        killServerAction = new AbstractAction("End game") {
            public void actionPerformed(ActionEvent e) {
                server.killServer();
                killServerAction.setEnabled(false);
            }
        };
        killServerAction.setEnabled(false);
        // An action to start a server

        JMenu launchMenu = new JMenu("Launch");

        launchMenu.add(playAsAction);
        launchMenu.addSeparator();

        clientActions = new AbstractAction[PLAYER_CLASSES.length];
        for (int i = 0; i < PLAYER_CLASSES.length; i++) {
            clientActions[i] = new LaunchClientAction(PLAYER_CLASSES[i]);
            clientActions[i].setEnabled(false);
            launchMenu.add(clientActions[i]);
        }

        launchMenu.addSeparator();

        serverActions = new AbstractAction[BOARD_CLASSES.length];
        for (int i = 0; i < BOARD_CLASSES.length; i++) {
            serverActions[i] = new LaunchServerAction(BOARD_CLASSES[i]);
            launchMenu.add(serverActions[i]);
        }

        launchMenu.addSeparator();

        fromHereAction = new AbstractAction("Launch server from current position") {
            public void actionPerformed(ActionEvent arg0) {
                try {
                    Board bd = (Board) boardHistory.get(currentBoard);
                    int currentMove = currentBoard;
                    // The current move might be the special 'null' at the
                    // end of the list used to display the outcome
                    if (currentMove == moveHistory.size() - 1 && moveHistory.get(currentMove) == null)
                        currentMove--;
                    if (bd == null || bd.getWinner() != Board.NOBODY)
                        throw new IllegalStateException("Can't start game from move " + currentMove);
                    Object[] h = moveHistory.subList(1, currentMove + 1).toArray();
                    Move[] hist = new Move[h.length];

                    for (int i = 0; i < h.length; i++) {
                        hist[i] = (Move) h[i];
                    }

                    clearData();
                    java.lang.reflect.Constructor co = bd.getClass().getConstructor(new Class[0]);
                    Board b = (Board) co.newInstance(new Object[0]);
                    Server svr = new Server(b, false, true, Server.DEFAULT_PORT, Server.DEFAULT_TIMEOUT,
                            Server.FIRST_MOVE_TIMEOUT);
                    svr.setHistory(hist);
                    setServer(svr);
                    svr.setGUI(theFrame);
                    (new Thread(svr)).start();
                } catch (Exception ex) {
                    System.err.println("Error launching server:");
                    ex.printStackTrace();
                }
            }
        };
        fromHereAction.setEnabled(false);

        launchMenu.add(fromHereAction);
        launchMenu.addSeparator();
        launchMenu.add(killServerAction);

        enableLaunchActions(false);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(openAction);
        fileMenu.add(closeAction);
        menuBar.add(fileMenu);
        JMenu histMenu = new JMenu("History");
        histMenu.add(firstAction);
        histMenu.add(backAction);
        histMenu.add(fwdAction);
        histMenu.add(lastAction);
        menuBar.add(histMenu);

        menuBar.add(launchMenu);

        JToolBar toolBar = new JToolBar("History");
        toolBar.add(firstAction);
        toolBar.add(backAction);
        toolBar.add(fwdAction);
        toolBar.add(lastAction);
        toolBar.setFloatable(false);

        this.setJMenuBar(menuBar);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(toolBar, BorderLayout.NORTH);
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        this.getContentPane().add(boardPanel, BorderLayout.CENTER);

        moveList = new JList(moveListModel);
        moveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moveList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent arg0) {
                int idx = moveList.getSelectedIndex();
                if (idx >= 0 && idx < moveHistory.size() && idx != currentBoard)
                    setCurrentBoard(idx);
            }
        });
        JScrollPane movePane = new JScrollPane(moveList);
        movePane.setPreferredSize(new Dimension(LIST_WIDTH, BOARD_SIZE));
        this.getContentPane().add(movePane, BorderLayout.EAST);

        statusLabel = new JLabel("GUI Loaded");
        this.getContentPane().add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * Change the currently displayed board. Pass -1 for no board displayed.
     */
    protected void setCurrentBoard(int index) {
        if (moveList.getSelectedIndex() != index) {
            moveList.setSelectedIndex(index);
            moveList.ensureIndexIsVisible(index);
        }

        // If a move was requested, but we're changing from the
        // last board, cancel the request
        if (userMoveRequested && index != boardHistory.size() - 1) {
            boardPanel.cancelMoveRequest();
            userMoveRequested = false;
        }
        if (currentBoard != index) {
            currentBoard = index;
            // Might be no board, index -1
            if (index < 0) {
                boardPanel.setCurrentBoard(null);
                fromHereAction.setEnabled(false);
            } else {
                Board b = (Board) boardHistory.get(index);
                // Might be the last board, in which case there is no
                // matching board in the list
                if (b == null)
                    b = (Board) boardHistory.get(boardHistory.size() - 1);
                boardPanel.setCurrentBoard(b);
                fromHereAction.setEnabled(
                        b != null && b.getWinner() == Board.NOBODY && b.getTurnNumber() >= 0 && server == null);
            }
            backAction.setEnabled(index > 0);
            firstAction.setEnabled(index > 0);
            fwdAction.setEnabled(index < boardHistory.size() - 1);
            lastAction.setEnabled(index < boardHistory.size() - 1);
        }

        // If we need a move, and this is the last board, request it
        if (userMoveNeeded && index == boardHistory.size() - 1 && !userMoveRequested) {
            boardPanel.requestMove(this);
            userMoveRequested = true;
        }
    }

    /** Clears all the move/board data */
    private void clearData() {
        // How many moves in the list
        int max = moveHistory.size() - 1;
        this.boardHistory.clear();
        this.moveHistory.clear();
        // Update the list view
        if (max >= 0)
            moveListModel.cleared(max);
        this.outcome = null;
        this.board = null;
        this.setCurrentBoard(-1);
        closeAction.setEnabled(false);
        statusLabel.setText("");
    }

    /**
     * Load a log file and feed the moves to the GUI, as if the server were running.
     */
    private void loadLogFile(String file) throws Exception {
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            this.clearData();
            // Find the board class line
            String line;
            while (!(line = in.readLine()).startsWith("# Game ID:"))
                if (!line.startsWith("#"))
                    throw new IllegalArgumentException("No 'Game ID:' line found in header.");
            int gameID = Integer.parseInt(line.substring(10).trim());
            while (!(line = in.readLine()).startsWith("# Board class:"))
                if (!line.startsWith("#"))
                    throw new IllegalArgumentException("No 'Board class:' line found in header.");
            String cls = line.substring(14).trim();
            // Create a board instance
            Class cl = Class.forName(cls);
            java.lang.reflect.Constructor co = cl.getConstructor(new Class[0]);
            Board b = (Board) co.newInstance(new Object[0]);
            // Add the moves as if receiving them from the server
            String[] players = new String[b.getNumberOfPlayers()];
            boolean gameOver = false;
            int pcount = 0;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#")) {
                    // Skip rest of header
                } else if (line.startsWith("START")) {
                    String name = line.substring(line.indexOf(' ') + 1).trim();
                    name = name.substring(name.indexOf(' ') + 1).trim();
                    players[pcount++] = name;
                    if (pcount >= players.length)
                        this.gameStarted((Board) b.clone(), gameID, players);
                } else if (line.startsWith("GAMEOVER")) {
                    gameOver = true;
                } else if (line.startsWith("WINNER") || line.startsWith("DRAW") || line.startsWith("UNDECIDED")) {
                    if (!gameOver)
                        System.err.println("Warning: 'GAMEOVER' line missing");
                    this.gameEnded(line);
                    break;
                } else {
                    if (pcount < players.length)
                        throw new IllegalArgumentException("Missing 'START' message(s).");
                    Move m = b.parseMove(line);
                    b.move(m);
                    boardUpdated((Board) b.clone(), m);
                }
            }
            in.close();
            this.setCurrentBoard(0);
        } catch (Exception e) {
            this.clearData();
            System.err.println("Exception loading file:");
            e.printStackTrace();
            throw e;
        }
    }

    /** Called by server when waiting for connection */
    void waitingForConnection(String playerID) {
        enableLaunchActions(true);
        statusLabel.setText("Waiting for " + playerID + " to connect... " + "(Use 'Launch' menu to launch clients)");
    }

    /** Called by server on game start */
    void gameStarted(Board b, int gameID, String[] players) {
        clearData();
        getContentPane().remove(boardPanel);
        boardPanel = b.createBoardPanel();
        boardPanel.setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        getContentPane().add(boardPanel, BorderLayout.CENTER);
        pack();
        repaint();

        StringBuffer title = new StringBuffer("Game " + gameID + ": ");
        for (int i = 0; i < players.length; i++)
            title.append(players[i] + (i < players.length - 1 ? " vs. " : ""));

        this.setTitle(title.toString());
        this.board = b;
        boardUpdated(b, null);
        setCurrentBoard(0);
        enableLaunchActions(false);
        enableServerActions(false);
        openAction.setEnabled(false);
        closeAction.setEnabled(false);
        killServerAction.setEnabled(true);
        statusLabel.setText("Game in progress, " + board.getNameForID(board.getTurnPlayer()) + " to play.");
    }

    /** Called by server on update */
    void boardUpdated(Board b, Move m) {
        moveHistory.add(m);
        this.moveListModel.addedMove();
        board = b;
        boardHistory.add(b);

        // If displaying the last board, update to the current
        if (currentBoard == boardHistory.size() - 2)
            setCurrentBoard(boardHistory.size() - 1);

        if (board != null) // Might be calling from gameEnded...
            statusLabel.setText("Game in progress, " + board.getNameForID(board.getTurnPlayer()) + " to play.");
    }

    /** Called by server on game end */
    void gameEnded(String str) {
        // In case the game was in progress, may have
        // been waiting for a user move
        if (userMoveNeeded) {
            userMoveNeeded = false;
            userMoveRequested = false;
        }

        this.outcome = str;
        boardUpdated(board, null);
        server = null;
        openAction.setEnabled(true);
        closeAction.setEnabled(true);
        enableServerActions(true);
        enableLaunchActions(false);
        killServerAction.setEnabled(false);
        statusLabel.setText("Game ended, " + outcome + ".");

        // Let the human client thread stop waiting in case it is
        if (theHumanPlayer != null)
            theHumanPlayer.cancelMoveRequestThread();
    }

    private void enableLaunchActions(boolean arg) {
        playAsAction.setEnabled(arg);
        for (int i = 0; i < clientActions.length; i++)
            clientActions[i].setEnabled(arg);
    }

    private void enableServerActions(boolean arg) {
        for (int i = 0; i < serverActions.length; i++)
            serverActions[i].setEnabled(arg);

        if (currentBoard >= 0) {
            Board b = (Board) boardHistory.get(currentBoard);
            if (b == null)
                b = (Board) boardHistory.get(boardHistory.size() - 1);
            fromHereAction
                    .setEnabled(b != null && b.getWinner() == Board.NOBODY && b.getTurnNumber() > 0 && server == null);
        }
    }

    /** The HumanPlayer wants a move from the user */
    private void getMoveFromUser() {
        // Indicate that we should get a move from the user
        this.userMoveNeeded = true;
        // Move to the last board
        this.setCurrentBoard(boardHistory.size() - 1);
        Board bb = (Board) boardHistory.get(currentBoard);
        statusLabel.setText("Waiting for user to play as " + bb.getNameForID(bb.getTurnPlayer()) + "...");
    }

    /** Callback from the boardPanel */
    public void moveEntered(Move m) {
        if (!userMoveNeeded) {
            System.err.println("Unexpected user move received from BoardPanel");
            return;
        }
        userMoveNeeded = false;
        userMoveRequested = false;
        this.theHumanPlayer.moveEntered(m);
        statusLabel.setText("User move sent to server.");
    }

    // UTILITY CLASSES FOLLOW

    /** A player to represent a human entering moves into the server GUI */
    private class HumanPlayer extends Player implements BoardPanel.BoardPanelListener {
        public HumanPlayer(Board bd) {
            super("Human");
            bdCls = bd.getClass();
        }

        private Class bdCls;
        private Move myMove = null;
        private boolean moveNeeded = false;
        private Thread clientThread = null;
        private final Runnable guiNotifier = new Runnable() {
            public void run() {
                theFrame.getMoveFromUser();
            }
        };

        public void movePlayed(BoardState bs, Move m) {
        };

        public void gameOver(String msg, BoardState bs) {
        };

        /* Called by client threads when user input is needed */
        synchronized public Move chooseMove(BoardState bs) {
            if (moveNeeded) {
                throw new IllegalStateException("Requested concurrent human moves.");
            }

            server.cancelTimeout();
            clientThread = Thread.currentThread();
            moveNeeded = true;

            // Have the dispatch thread tell the GUI we're waiting for a move
            EventQueue.invokeLater(guiNotifier);

            // Sleep until we have the move
            while (moveNeeded && myMove == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }

            moveNeeded = false;
            Move theMove = myMove;
            myMove = null;
            clientThread = null;
            return theMove;
        }

        /* Callback by the AWT dispatch thread */
        synchronized public void moveEntered(Move m) {
            if (!moveNeeded) {
                System.err.println("BoardPanel sent unrequested move!");
                return;
            }
            myMove = m; // Set the move
            notify();
        }

        /*
         * Interrupt the thread waiting for a move if the game is ended
         */
        synchronized void cancelMoveRequestThread() {
            if (clientThread != null) {
                moveNeeded = false;
                clientThread.interrupt();
            }
        }

        public Board createBoard() {
            Board bd = null;

            try {
                java.lang.reflect.Constructor co = bdCls.getConstructor(new Class[0]);
                bd = (Board) co.newInstance(new Object[0]);
            } catch (Exception ex) {
                System.err.println("Error creating board class " + bdCls.getName());
                System.err.println(ex.getMessage());
            }
            return (bd);
        }
    } // End class HumanPlayer

    // An action to launch a server
    private class LaunchServerAction extends AbstractAction {
        String boardClass;

        public LaunchServerAction(String cls) {
            super("Launch server (" + cls + ")");
            boardClass = cls;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                clearData();
                Board b;
                Class cl = Class.forName(boardClass);
                java.lang.reflect.Constructor co = cl.getConstructor(new Class[0]);
                b = (Board) co.newInstance(new Object[0]);
                Server svr = new Server(b, false);
                setServer(svr);
                svr.setGUI(theFrame);
                (new Thread(svr)).start();
            } catch (Exception ex) {
                System.err.println("Error launching server:");
                ex.printStackTrace();
            }
        }
    };

    // An action to launch a client
    private class LaunchClientAction extends AbstractAction {
        String playerClass;

        public LaunchClientAction(String cls) {
            super("Launch client (" + cls + ")");
            playerClass = cls;
        }

        public void actionPerformed(ActionEvent e) {
            try {
                Player p;
                Class cl = Class.forName(playerClass);
                java.lang.reflect.Constructor co = cl.getConstructor(new Class[0]);
                p = (Player) co.newInstance(new Object[0]);
                // Diable this action
                enableLaunchActions(false);
                // Create and launch a new client
                Client c = new Client(p, server.getHostName(), server.getPort());
                (new Thread(c)).start();
                statusLabel.setText("Tried to launch " + playerClass);
            } catch (Exception ex) {
                System.err.println("Error launching client -- " + ex.toString());
                System.err.println("Make sure that the class " + playerClass + " has been compiled and that "
                        + "the classpath is set appropriately.");
            }
        }
    }

    // Custom move ListModel
    private class MoveListModel extends AbstractListModel {
        public int getSize() {
            return moveHistory.size();
        }

        public Object getElementAt(int arg0) {
            if (arg0 == 0)
                return outcome != null && moveHistory.size() == 1 ? outcome : "START";
            if (arg0 == moveHistory.size() - 1 && outcome != null)
                return outcome;
            return (arg0) + ". " + ((Move) moveHistory.get(arg0)).toPrettyString();
        }

        void addedMove() {
            int i = moveHistory.size();
            this.fireIntervalAdded(this, i, i);
        }

        void cleared(int maxIndex) {
            this.fireIntervalRemoved(this, 0, maxIndex);
        }
    };

} // end class ServerGUI
