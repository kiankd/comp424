package boardgame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.lang.Exception;

/**
 * Boardgame player client code. Do not modify this class, implement Player
 * subclass instead.
 */
public class Client implements Runnable {
    protected static final String DEFAULT_SERVER = "localhost";
    protected static final int DEFAULT_PORT = Server.DEFAULT_PORT;
    protected static final String DEFAULT_PLAYER = "tablut.RandomPlayer";
    protected static final boolean DBGNET = true;

    private Socket socket;
    private PrintWriter sockOut;
    private BufferedReader sockIn;
    private String serverName;
    private int serverPort;

    Player player;
    int playerID;
    Board board;
    boolean gameOver = false;

    private static void printUsage() {
        System.err.println("Usage: java boardgame.Client [playerClass [serverName [serverPort]]]\n"
                + "  Where playerClass is the player to be run (default=" + DEFAULT_PLAYER + "\n"
                + "        serverName is the server address (default=" + DEFAULT_SERVER + ") and\n"
                + "        serverPort is the port number (default=" + DEFAULT_PORT + ").\n" + "  e.g.\n"
                + "  java boardgame.Client " + DEFAULT_PLAYER + " localhost " + DEFAULT_PORT);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 3) {
                printUsage();
                throw new UnsupportedOperationException("Too many args.");
            } else {
                Player p;
                try {
                    Class cl = Class.forName(args.length > 0 ? args[0] : DEFAULT_PLAYER);
                    java.lang.reflect.Constructor co = cl.getConstructor(new Class[0]);
                    p = (Player) co.newInstance(new Object[0]);
                } catch (Exception e) {
                    System.err.println("Failed to create Player object: " + e);
                    printUsage();
                    throw e;
                }

                Client client;
                try {
                    client = new Client(p, args.length > 1 ? args[1] : DEFAULT_SERVER,
                            args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_PORT);
                } catch (Exception e) {
                    printUsage();
                    throw e;
                }

                client.run();
            }
        } catch (Exception e) {
            System.out.println("Caught exception: " + e);
            System.exit(1);
        }

        System.exit(0);
    }

    public Client(Player p, String svr, int pt) {
        this.board = p.createBoard();
        this.player = p;
        this.serverName = svr;
        this.serverPort = pt;
    }

    public void run() {
        if (connect())
            clientLoop();
    }

    /** Process message received from server. */
    protected void processMessage(String msg) {
        if (msg.startsWith("GAMEOVER")) {
            String[] tokens = msg.split(" ");
            String winner = tokens[tokens.length - 1];

            if (winner.equals("DRAW")) {
                board.forceWinner(Board.DRAW);
            } else if (winner != "UNDECIDED") {
                board.forceWinner(Integer.parseInt(winner));
            }

            BoardState bs = board.getBoardState();
            player.gameOver(msg, bs);
            this.gameOver = true;
        } else if (msg.startsWith("PLAY")) { // My turn
            playMove();
        } else {
            // Expect a move
            Move m;
            try {
                m = board.parseMove(msg);
            } catch (Exception e) {
                System.err.println("Ignoring unparseable move from server: " + msg);
                return;
            }

            try {
                board.move(m);
            } catch (Exception e) {
                System.err.println("Failed executing move from server: " + msg);
                e.printStackTrace();
                return;
            }

            player.movePlayed(board.getBoardState(), m);
        }
    }

    protected void playMove() {
        Move myMove = null;

        try {
            myMove = player.chooseMove(board.getBoardState());

            if (myMove == null) {
                System.err.println("ATTENTION: Player didn't return a move.");
                throw new Exception();
            }

            myMove.setPlayerID(playerID);
            myMove.setFromBoard(false);
        } catch (Exception e) {
            System.err.println(
                    "ATTENTION: Exception in " + player.getClass().getName() + ".chooseMove(). Playing random move.");

            e.printStackTrace();

            myMove = board.getBoardState().getRandomMove();
        }

        try {
            String msg = myMove.toTransportable();
            sockOut.println(msg);

            if (DBGNET)
                System.err.println(player.getColor() + "< " + msg);
        } catch (Exception e) {
            System.err.println("Error sending move to server: ");
            e.printStackTrace();
            gameOver = true;
        }
    }

    /** Connect to a server. This blocks until the game starts. */
    protected boolean connect() {
        System.out.println("Connecting to " + serverName + ":" + serverPort + "... ");

        try {
            socket = new Socket(serverName, serverPort);
            sockOut = new PrintWriter(socket.getOutputStream(), true);
            sockIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send the start message to the server and wait for reply
            sockOut.println("START " + player.getName());
            if (DBGNET)
                System.err.println(player.getColor() + "< START " + player.getName());

            System.out.println("Connected. Waiting for game to start...");
            String msg = null;

            while (msg == null || !msg.startsWith("START")) {
                msg = sockIn.readLine(); // Waits for server response.
                if (DBGNET)
                    System.err.println(player.getColor() + "> " + msg);
            }

            // Set the colour
            String str = msg.substring(6);
            String clr = str.substring(0, str.indexOf(' '));
            playerID = board.getIDForName(clr);
            player.setColor(playerID);
            player.gameStarted(msg);

            return true;
        } catch (Exception e) {
            System.err.println("Failed to connect: ");
            e.printStackTrace();
            return false;
        }
    }

    /** Pump messages from the server */
    protected void clientLoop() {
        String inputLine;
        try {
            while (!gameOver) {
                // Blocking read
                inputLine = sockIn.readLine();
                if (inputLine == null)
                    continue;
                if (DBGNET)
                    System.err.println(player.getColor() + "> " + inputLine);
                processMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e);
            e.printStackTrace();
            player.gameOver("CONNECTION ERROR " + e, board.getBoardState());
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
            }
        }
    }

} // End class Client
