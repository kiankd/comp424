package boardgame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.awt.EventQueue;

/**
 * Generic boardgame server. TODO The winner determination in case of a client
 * error won't make sense unless it's a 2-player game. TODO The facility for the
 * board to issue moves by returning Move.BOARD as the turn has not been tested
 * all that well. TODO handling of Move.getReceivers() is not well tested.
 *
 * To start a server, execute its run() method. This will listen for clients and
 * launch handler threads for each before returning. Any further work is done by
 * these handler threads. The GUI dispatch thread may also call certain methods.
 *
 * Note that the call to run() returns as soon as the ClientHandler threads are
 * started.
 *
 * A note on synchronization: The methods of this class will be called by 3
 * types of threads: - A) The thread executing the run() method - B) The
 * ClientHandler threads (started by the run() method) - C) The GUI Thread
 *
 * The entry points for these threads are synchronized on the Server object's
 * lock, except for the run() method. This is because the thread would not
 * release the lock while waiting for connections. For this reason it is unsafe
 * to manipulate the Server object while the run() method may be executing, with
 * the exception of the killServer() method.
 *
 * This could probably be fixed by using yet another thread to accept the
 * connections.
 *
 * Calls to the GUI, if present, are done using the EventQueue.invokeLater()
 * facility, so that they are invoked by the AWT event dispatch thread.
 */
public class Server implements Runnable {
    protected static final String VERSION = "0.08";
    protected static final int DEFAULT_PORT = 8123;

    public static final int DEFAULT_TIMEOUT = 2000;
    private static final int DEFAULT_TIMEOUT_CUSHION = 1000;

    public static final int FIRST_MOVE_TIMEOUT = 30000;
    private static final int FIRST_MOVE_TIMEOUT_CUSHION = 1000;

    protected static final String DEFAULT_BOARDCLASS = "tablut.TablutBoard";

    protected static final boolean DBGNET = false;
    protected static final boolean DUMPBOARD = false;
    protected static final int MAX_SERVERS = 10;

    public static String log_dir = "logs";
    protected static final String OUTCOME_FILE = "outcomes.txt";
    protected static final String LOG_PREFIX = "game";
    protected static final String LOG_SUFFIX = ".log";

    // Command line parameters
    private static int cmdArgPort = DEFAULT_PORT;
    private static int cmdArgTimeout = DEFAULT_TIMEOUT;
    private static int cmdArgFirstTimeout = FIRST_MOVE_TIMEOUT;
    private static boolean cmdArgQuiet = false;

    // Parameters
    private int port = DEFAULT_PORT;
    private int timeout = DEFAULT_TIMEOUT;
    private int first_move_timeout = FIRST_MOVE_TIMEOUT;
    private boolean quiet = false;

    // Files, sockets and threads
    final private ClientHandler players[];
    // A fake client handler for the board, if it wants to play moves
    private ClientHandler boardClientHandler = new ClientHandler(Board.BOARD, this);
    private Timer timer = new Timer();
    private TimerTask timeoutTask;
    private TimerTask killTimeoutTask;
    private String hostname = "localhost";
    private ServerSocket svrSock;

    // The game board
    private final Board board;
    private boolean gameStarted = false;
    private boolean gameEnded = false;

    // The GUI, may be null
    private ServerGUI gui;

    // Logging stuff
    private File logDir = null;
    private PrintStream logOut = null;
    private String logfilename;
    private int gameID = -1;
    // This is a history, if the game wasn't started from scratch
    private Move[] history = null;
    private boolean playingHistory = false;

    private static void printUsage() {
        System.err.println("\nUsage: java boardgame.Server [-p port] [-ng] [-q] [-t n] [-b class]\n"
                + "  Where '-p port' sets the port to listen on. (default=" + DEFAULT_PORT + ")\n"
                + "        '-ng' indicates not to show a GUI.\n"
                + "        '-q' indicates not to dump log to console.\n" + "        '-t n' sets timeout. (default="
                + DEFAULT_TIMEOUT + ")\n" + "        '-ft n' sets timeout for the first move. (default="
                + FIRST_MOVE_TIMEOUT + ")\n" + "        '-k' indicates to start a new server once a game is running"
                + "  e.g.\n" + "    java boardgame.Server -p " + DEFAULT_PORT + " -t " + DEFAULT_TIMEOUT + "\n"
                + "  launches a server with a GUI and the default parameters.\n");
    }

    public static void main(String[] args) {
        String argClass = DEFAULT_BOARDCLASS;
        boolean argGui = true;
        boolean argKeep = false;
        Server svr = null;
        Vector<Server> servers = new Vector<Server>();
        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-p"))
                    cmdArgPort = Integer.parseInt(args[++i]);
                else if (args[i].equals("-ng"))
                    argGui = false;
                else if (args[i].equals("-t"))
                    cmdArgTimeout = Integer.parseInt(args[++i]);
                else if (args[i].equals("-ft"))
                    cmdArgFirstTimeout = Integer.parseInt(args[++i]);
                else if (args[i].equals("-q"))
                    cmdArgQuiet = true;
                else if (args[i].equals("-k"))
                    argKeep = true;
                else if (args[i].equals("-l"))
                    log_dir = args[++i];
                else {
                    printUsage();
                    return;
                }
            }
        } catch (Exception e) {
            printUsage();
            return;
        }
        // Store the comand line parameters
        ServerSocket ss = null;
        do { // Keep launching servers
            try {
                // If we have too many servers running, wait for one to finish
                while (servers.size() >= MAX_SERVERS) {
                    for (int i = 0; i < servers.size(); i++) {
                        Server s = (Server) servers.get(i);
                        synchronized (s) {
                            if (s.gameEnded)
                                servers.removeElementAt(i);
                        }
                    }
                    Thread.sleep(500); // Wait half a second
                }

                // Get the board instance
                Class cl = Class.forName(argClass);
                java.lang.reflect.Constructor co = cl.getConstructor(new Class[0]);
                Board b = (Board) co.newInstance(new Object[0]);

                // Open a server socket, we can reuse this multiple times
                // since the server won't close it unless cancelled by the GUI,
                // in which case we want to quit anyway
                if (ss == null) {
                    ss = new ServerSocket(cmdArgPort);
                }

                // Create the server
                svr = new Server(b, argGui, cmdArgQuiet, ss, cmdArgTimeout, cmdArgFirstTimeout);

                // Launch the server
                svr.run();

                // Add it to the list of running servers
                servers.add(svr);
            } catch (Exception e) {
                System.err.println("Failed to start server:");
                e.printStackTrace();
                printUsage();
                if (ss != null)
                    try {
                        ss.close();
                    } catch (Exception ex) {
                    }
                return;
            }
        } while (argKeep);
        if (ss != null)
            try {
                ss.close();
            } catch (Exception e) {
            }
    }

    /**
     * Create a server which accepts two connections from the given socket.
     */
    public Server(Board b, boolean createGUI, boolean qt, ServerSocket ss, int to, int fto) {
        this(b, createGUI, qt, ss.getLocalPort(), to, fto);
        this.svrSock = ss;
    }

    /** Create a server which will create its own socket to listen on */
    public Server(Board b, boolean createGUI) {
        this(b, createGUI, cmdArgQuiet, cmdArgPort, cmdArgTimeout, cmdArgFirstTimeout);
    }

    /** Create a server which will create its own socket to listen on */
    public Server(Board b, boolean createGUI, boolean qt, int svPort, int to, int fto) {
        this.board = b;
        this.port = svPort;
        this.timeout = to;
        this.first_move_timeout = fto;
        this.quiet = qt;
        this.svrSock = null;
        if (createGUI)
            this.gui = new ServerGUI(this);
        players = new ClientHandler[b.getNumberOfPlayers()];
    }

    public Board getBoard() {
        return board;
    }

    // Allow the GUI to provide a history.
    synchronized void setHistory(Move[] moves) {
        this.history = moves;
    }

    // Allow the GUI to set itself for this server
    synchronized void setGUI(ServerGUI g) {
        this.gui = g;
    }

    /** Lets the GUI end the game */
    synchronized void killServer() {
        endGame("USER CANCEL");
    }

    /**
     * Returns "localhost" or the hostname if the run() method has completed
     */
    public String getHostName() {
        return hostname;
    }

    public int getPort() {
        return this.port;
    }

    // The run method just starts the server's connections and
    // then returns.
    public void run() {
        // Get the logfile directory
        logDir = new File(log_dir);
        if (!logDir.isDirectory()) {
            try {
                if (!logDir.mkdirs()) {
                    System.err.println("Failed to create log directory.");
                    endGame("SERVER ERROR");
                }
            } catch (Exception e) {
                System.err.println("Exception creating log directory.");
                e.printStackTrace();
                endGame("SERVER ERROR");
            }
        }

        // If we have a GUI, display it
        if (gui != null) {
            gui.pack();
            gui.setVisible(true);
        }

        // Listen for 2 incoming connections, start ClientHandlers
        // and then exit.
        boolean ownSocket = false;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
            ServerSocket ss;
            synchronized (this) {
                if (svrSock == null) {
                    svrSock = new ServerSocket(port);
                    ownSocket = true;
                }
                ss = svrSock;
            }
            System.out.println("Server started. Waiting for incoming connections on " + hostname + ":" + port + " ...");
            int accepted = 0;
            while (!gameEnded && accepted < board.getNumberOfPlayers()) {

                if (gui != null)
                    new RWaitFor(board.getNameForID(accepted));

                Socket client = ss.accept();
                players[accepted] = new ClientHandler(client, accepted, this);

                new Thread(players[accepted]).start();

                System.out.println("Accepted connection from " + client.getInetAddress().getHostName() + ": "
                        + board.getNameForID(players[accepted].getPlayerID()));
                accepted++;
            }
        } catch (Exception e) {
            if (gameEnded)
                return; // The game is cancelled, this is OK
            System.err.println("Failed to accept connections:");
            e.printStackTrace();
            endGame("CONNECTION ERROR");
        } finally {
            if (ownSocket)
                try {
                    svrSock.close();
                } catch (Exception e) {
                }
            // Tell any other threads we're done accepting connections
            synchronized (this) {
                svrSock = null;
            }
        }
    }

    /** receives messages from the client sockets */
    private synchronized void processMessage(String inputLine, ClientHandler h) {
        if (DBGNET)
            System.out.println(board.getNameForID(h.getPlayerID()) + "> " + inputLine);
        if (gameEnded)
            return;

        // If the game has started, only want message if it's your turn!
        if (gameStarted && h.getPlayerID() != board.getTurnPlayer()) {
            System.err.println("It is currently: " + board.getNameForID(board.getTurnPlayer()) + "'s turn. "
                    + "Ignoring out of turn message from " + board.getNameForID(h.getPlayerID()) + ": " + inputLine);

            // Check for START messages if we haven't started yet
        } else if (!gameStarted && inputLine.startsWith("START")) {
            h.setReady(inputLine.substring(5).trim());

            // Start the game if everyone is ready
            for (int i = 0; i < board.getNumberOfPlayers(); i++)
                if (players[i] == null || !players[i].isReady())
                    return;

            try {
                initLogFile();

                // Tell the GUI we're starting, this needs to happen
                // before we sent messages to the client, so the GUI
                // is ready to process move requests from any human players
                if (gui != null) {
                    String p[] = new String[players.length];
                    for (int i = 0; i < players.length; i++)
                        p[i] = players[i].getName();
                    // gui.gameStarted(board,gameID,p);
                    new RStarting(board, gameID, p);
                }

                // Send the start messages
                for (int i = 0; i < board.getNumberOfPlayers(); i++) {
                    String msg = "START " + board.getNameForID(players[i].getPlayerID()) + " " + players[i].getName();
                    log(msg);
                    players[i].send(msg);
                }

                // Game is started
                gameStarted = true;

                // If we're not starting from scratch, play through the move history
                if (history != null) {
                    playingHistory = true;
                    for (int i = 0; i < history.length; i++) {
                        ClientHandler player = null;

                        for (int p = 0; p < players.length; p++) {
                            if (players[p].getPlayerID() == history[i].getPlayerID()) {
                                player = players[p];
                                break;
                            }
                        }

                        processMessage(history[i].toTransportable(), player);
                    }

                    playingHistory = false;
                }

                // Request the first move
                requestMove(board.getTurnPlayer());
            } catch (Exception e) {
                System.err.println("Exception starting game.");
                e.printStackTrace();
                endGame("SERVER ERROR");
            }

            // Otherwise, expect a move
        } else {
            Move m;
            try {
                m = board.parseMove(inputLine);
            } catch (Exception e) {
                System.err.println("Ignoring unparseable move from " + h.getName() + ": " + inputLine);
                e.printStackTrace();
                return;
            }

            cancelTimeout();

            try {
                Move ms[];

                // Let the board modify the move if not playing a history
                Object o = playingHistory ? m : board.filterMove(m);

                if (o instanceof Move) { // The board provided a move
                    Move myArray[] = { (Move) o };
                    ms = myArray;
                } else { // An array of moves instead
                    ms = (Move[]) o;
                }

                // Execute the move(s)
                for (int i = 0; i < ms.length; i++) {
                    m = ms[i];
                    board.move(m);
                    if (gui != null)
                        new RUpdated(board, m);// gui.boardUpdated( m );
                    broadcast(m);
                }

                if (DUMPBOARD)
                    System.out.println(board.toString());

                if (board.getWinner() != Board.NOBODY) {
                    endGame("");
                } else if (!playingHistory) {
                    requestMove(board.getTurnPlayer());
                }

            } catch (IllegalArgumentException e) {
                System.err.println("Error executing move: " + m.toPrettyString());
                e.printStackTrace();
                forceLoser(h.getPlayerID());
                endGame("ILLEGAL MOVE: " + m.toPrettyString());
            }
        }
    }

    private void initLogFile() throws Exception {
        // Find an unused filename
        File[] files = logDir.listFiles();
        if (files == null)
            throw new IOException("Log directory doesn't seem to exist.");

        int max = 0, plen = LOG_PREFIX.length(), slen = LOG_SUFFIX.length();
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().startsWith(LOG_PREFIX) && files[i].getName().endsWith(LOG_SUFFIX)) {

                int v = Integer.parseInt(files[i].getName().substring(plen, files[i].getName().length() - slen));
                if (v > max)
                    max = v;
            }
        }

        gameID = max + 1;
        String name = "000000" + Integer.toString(gameID);
        logfilename = LOG_PREFIX + name.substring(name.length() - 5) + LOG_SUFFIX;

        // Open the log and print some header stuff
        File logFile = new File(logDir, logfilename);
        logOut = new PrintStream(new FileOutputStream(logFile));
        logOut.println("# Server version " + VERSION + " running on " + hostname + ":" + port);
        logOut.println("# Game ID: " + gameID);
        logOut.println("# Board class: " + board.getClass().getName());
        logOut.println("# Timeout: " + timeout);
        logOut.println("# First Move Timeout: " + first_move_timeout);
        logOut.println("# Date: " + (new Date()).toString());

        if (history != null)
            logOut.println("# Starting at move " + (history.length + 1));

        for (int i = 0; i < players.length; i++) {
            logOut.println("# Player " + (i + 1) + ": " + board.getNameForID(players[i].getPlayerID()) + ", '"
                    + players[i].getName() + "', running on " + players[i].getHostName());
        }
    }

    /** Callback for socket error in ClientHandler. */
    private synchronized void connectionError(ClientHandler h, IOException e) {
        System.err.println("Connection error for " + board.getNameForID(h.getPlayerID()) + " : " + e);
        forceLoser(h.getPlayerID());
        endGame("DISCONNECTION " + board.getNameForID(h.getPlayerID()));
    }

    /** Callback for timeout timer. Play a random move. */
    private synchronized void timeOut(int player_id) {
        Move random_move = board.getRandomMove();
        random_move.setPlayerID(player_id);
        random_move.setFromBoard(false);

        players[player_id].setMove(random_move.toTransportable());
    }

    /** Callback for kill timeout timer. End the game. */
    private synchronized void killTimeOut(int player_id) {
        forceLoser(player_id);
        endGame("TIMEOUT");
    }

    private void endGame(String reason) {
        if (gameEnded)
            return;

        gameEnded = true;

        // Maybe we're still waiting for connections. Closing the
        // server socket will cause an exception in that thread.
        synchronized (this) {
            if (svrSock != null)
                try {
                    svrSock.close();
                } catch (IOException e) {
                }
        }

        // Make sure we get rid of the timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        // Log reason for ending the game
        String msg = "GAMEOVER ";

        if (!reason.isEmpty()) {
            msg += reason + " ";
        }

        // Log winner
        switch (board.getWinner()) {
        case Board.DRAW:
            msg += "DRAW";
            break;
        case Board.NOBODY:
            msg += "UNDECIDED";
            break;
        case Board.CANCELLED0:
            msg += "CANCELLED 0";
            break;
        case Board.CANCELLED1:
            msg += "CANCELLED 1";
            break;
        default:
            msg += "WINNER " + board.getWinner();
        }

        if (gui != null)
            new REnded(msg); // gui.gameEnded(msg);

        broadcast(msg);

        // Close sockets
        for (int i = 0; i < players.length; i++)
            if (players[i] != null)
                players[i].closeConnection();

        // Close the log file
        if (logOut != null) {
            logOut.println("# Game ended: " + (new Date()).toString());
            logOut.close();

            // Append the outcome to the outcome file
            try {
                PrintStream out = new PrintStream(new FileOutputStream(new File(log_dir, OUTCOME_FILE), true));
                String delim = ",";
                out.print(Integer.toString(gameID) + delim);

                int win = -1;
                for (int i = 0; i < players.length; i++) {
                    if (players[i] == null)
                        out.print("NOBODY" + delim);
                    else {
                        out.print(players[i].getName() + delim);

                        if (board.getWinner() == players[i].getPlayerID()) {
                            win = i;
                        }
                    }
                }

                out.print((win > -1 ? players[win].getPlayerID() : msg) + delim);
                out.print((win > -1 ? players[win].getName() : "NOBODY") + delim);
                out.print(Integer.toString(board.getTurnNumber()) + delim);
                out.print(logfilename + delim);
                out.println(reason);
                out.close();
            } catch (Exception e) {
                System.err.println("Failed to append outcome to '" + OUTCOME_FILE + "': ");
                e.printStackTrace();
            }
        }
    }

    /** send the PLAY message to the client, and start the timer */
    private void requestMove(int player_id) {
        // Check if its the environment which wants to make a move
        if (player_id == Board.BOARD) {
            Move m = board.getBoardMove();
            // Pass the move as if coming over the network
            this.processMessage(m.toTransportable(), boardClientHandler);
        } else {
            // One of the players to move
            for (int i = 0; i < players.length; i++) {
                if (players[i].getPlayerID() == player_id) {
                    players[i].send("PLAY " + board.getNameForID(player_id));

                    if (board.getTurnNumber() == 0) {
                        resetTimer(first_move_timeout, FIRST_MOVE_TIMEOUT_CUSHION, i);
                    } else {
                        resetTimer(timeout, DEFAULT_TIMEOUT_CUSHION, i);
                    }

                    return;
                }
            }

            throw new IllegalStateException("Invalid player ID: " + player_id);
        }
    }

    private void resetTimer(int timeout, int kill_cushion, int player_id) {
        cancelTimeout();

        final int f_player_id = player_id;

        timeoutTask = new TimerTask() {
            public void run() {
                timeOut(f_player_id);
            }
        };

        killTimeoutTask = new TimerTask() {
            public void run() {
                killTimeOut(f_player_id);
            }
        };

        timer.schedule(timeoutTask, timeout);
        timer.schedule(killTimeoutTask, timeout + kill_cushion);
    }

    // So the GUI can cancel the timeout
    synchronized void cancelTimeout() {
        if (timeoutTask != null)
            timeoutTask.cancel();
        if (killTimeoutTask != null)
            killTimeoutTask.cancel();
        timeoutTask = null;
        killTimeoutTask = null;
    }

    private void log(String str) {
        if (!quiet)
            System.out.println("% " + str);
        if (logOut != null)
            logOut.println(str);
    }

    /** Send string to all players */
    private void broadcast(String str) {
        log(str);
        for (int i = 0; i < board.getNumberOfPlayers(); i++)
            if (players[i] != null)
                players[i].send(str);
    }

    /** Send string to all players */
    private void broadcast(String str, boolean do_log) {
        if (do_log) {
            log(str);
        }

        for (int i = 0; i < board.getNumberOfPlayers(); i++)
            if (players[i] != null)
                players[i].send(str);
    }

    /** Send move m to the players identified by m.getReceivers() */
    private void broadcast(Move m) {
        String str = m.toTransportable();
        int[] rec = m.getReceivers();
        if (rec == null) {
            // Send to everyone
            broadcast(str, m.doLog());
        } else {
            if (m.doLog()) {
                log(str);
            }

            // Send to players in the rec array
            for (int i = 0; i < rec.length; i++) {
                for (int p = 0; p < players.length; p++) {
                    if (players[p].getPlayerID() == rec[i]) {
                        players[p].send(str);
                    }
                }
            }
        }
    }

    private void forceLoser(int c) {
        if (c == Board.BOARD)
            board.forceWinner(Board.DRAW);
        else
            board.forceWinner((c + 1) % 2);
    }

    // Runnables to call the GUI's methods in the dispatch thread
    private class RWaitFor implements Runnable {
        String who;

        public RWaitFor(String str) {
            who = str;
            EventQueue.invokeLater(this);
        }

        public void run() {
            gui.waitingForConnection(who);
        }
    }

    private class RStarting implements Runnable {
        String who[];
        Board b;
        int id;

        public RStarting(Board bd, int i, String str[]) {
            who = str;
            b = (Board) bd.clone();
            id = i;
            EventQueue.invokeLater(this);
        }

        public void run() {
            gui.gameStarted(b, id, who);
        }
    }

    private class RUpdated implements Runnable {
        Board b;
        Move m;

        public RUpdated(Board bb, Move mm) {
            b = (Board) bb.clone();
            m = mm;
            EventQueue.invokeLater(this);
        }

        public void run() {
            gui.boardUpdated(b, m);
        }
    }

    private class REnded implements Runnable {
        String how;

        public REnded(String str) {
            how = str;
            EventQueue.invokeLater(this);
        }

        public void run() {
            gui.gameEnded(how);
        }
    }

    /** Communicates with one client. */
    class ClientHandler implements Runnable {
        private Server server;
        private Socket sock;
        private BufferedReader sockIn;
        private PrintStream sockOut;
        private boolean closed = false; // Shared var: synchronize on this object
        private boolean ready = false;

        private volatile String move;
        private int colour;
        private String name;

        public ClientHandler(Socket sock, int colour, Server server) {
            this.sock = sock;
            this.server = server;
            this.colour = colour;
            try {
                sockIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                sockOut = new PrintStream(sock.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
                closeConnection();
            }
            this.move = null;
        }

        /**
         * Create a fake client handler for the board, to pass to the processMessage()
         * function
         */
        public ClientHandler(int colour, Server server) {
            this.sock = null;
            this.server = server;
            this.colour = colour;
            this.name = "theBoard";
            this.move = null;
        }

        public int getPlayerID() {
            return colour;
        }

        public String getName() {
            return name;
        }

        public String getHostName() {
            return sock.getInetAddress().getCanonicalHostName();
        }

        public synchronized void setMove(String move) {
            this.move = move;
        }

        public synchronized String getMove() {
            return move;
        }

        public synchronized boolean moveIsSet() {
            return move != null;
        }

        public synchronized void clearMove() {
            move = null;
        }

        /** Set this player's ready flag and name */
        void setReady(String name) {
            this.name = name;
            ready = true;
        }

        public boolean isReady() {
            return ready;
        }

        public void run() {
            String inputLine;
            try {
                while (true) {
                    // Check if the connection has been closed, and get out of
                    // here if that's the case
                    synchronized (this) {
                        if (closed)
                            break;
                    }
                    // Blocking read
                    inputLine = sockIn.readLine();

                    if (moveIsSet()) {
                        System.out.println("Player " + colour + " timeout - Ignoring move from player: " + inputLine);

                        // Move did not come in time. Using a random move instead.
                        // Random move is set by the timeOut method of the Server.
                        inputLine = getMove();
                        System.out.println("Player " + colour + " timeout - Playing random move: " + inputLine);
                    } else if (inputLine == null) {
                        continue;
                    }

                    server.processMessage(inputLine, this);
                    clearMove();
                }
            } catch (IOException e) {
                // Most likely because the socket was closed by a
                // closeConnection() call
                synchronized (this) {
                    // But if not, we lost the connection
                    if (!closed) {
                        server.connectionError(this, e);
                        closeConnection();
                    }
                }
            } finally {
                closeConnection();
            }
        }

        /** Send a string to this client. */
        public synchronized void send(String msg) {
            if (!closed) {
                if (Server.DBGNET)
                    System.out.println(server.board.getNameForID(getPlayerID()) + "< " + msg);

                sockOut.println(msg);
            }
        }

        /**
         * Close the connection to the client and signal the thread for this connection
         * to exit.
         */
        public synchronized void closeConnection() {
            if (!closed) {
                closed = true;
                try {
                    sock.close();
                } catch (IOException e) {
                    System.err.println("Failed to close client socket:");
                    e.printStackTrace();
                }
            }
        }
    }
} // End class Server
