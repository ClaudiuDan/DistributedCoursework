import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Participant {
    private int serverPort;
    private int port;

    public int getTimeout() {
        return timeout;
    }

    private int timeout;
    private int failureCondition;
    private Socket serverConnection;
    BufferedReader coordinatorReader;
    OutputStreamWriter coordinatorWriter;
    public static void main (String[] args) {
        Participant participant = new Participant();

        //tests
        //args = new String[4];
        //args[0] = "12345";
        //args[1] = "12346";
        //args[2] = "5000";
        //args[3] = "0";

        participant.parseInput(args);
        participant.start();
    }

    private void start () {
        connectToServer();
        sendPortToServer();
        getOtherPorts();
        connectToOtherParticipants();
        getOptions();
        vote();
        runFirstRoundConsensusAlgorithm();

    }
    private void runFirstRoundConsensusAlgorithm() {
        for (Socket s : outputSocketsMap.values()) {
            sendMessage(s, "VOTE " + port + " " + vote);
        }
    }
    private List<Thread> threads = new ArrayList<>();

    private void decideOutcome() {
        if (port == 12346) {
            for (Thread t : threads)
                t.interrupt();
            System.exit(0);
        }
        Map<String, Integer> voteCounter = new HashMap<>();
        String message = new String(" ");
        for (Integer p : votes.keySet()) {
            message = message + String.valueOf(p) + " ";
            if (voteCounter.get(votes.get(p)) != null)
                voteCounter.put(votes.get(p), voteCounter.get(votes.get(p)) + 1);
            else 
                voteCounter.put(votes.get(p), 1);
        }
        int maxim = -1;
        String candidate = null;
        for (String s : voteCounter.keySet())
            if (maxim < voteCounter.get(s)) {
                maxim = voteCounter.get(s);
                candidate = s;
            }
        System.out.println(maxim + " " + candidate);
        if (maxim >= votes.keySet().size() / 2 + 1) {
            System.out.println("e majoritate");
            sendMessage(serverConnection, candidate + message);
        }
        else
            sendMessage(serverConnection, null);

        waitForRestart();
    }

    private void waitForRestart() {
        try {
            System.out.println("waiting for restart");
            serverConnection.setSoTimeout(60000);
            if (coordinatorReader.readLine().equals("RESTART")) {
                System.out.println("Dam restart");
                serverConnection.setSoTimeout(0);
            }
        } catch (IOException e) {
            for (Thread t : threads)
                t.interrupt();
            System.exit(0);
        }

    }

    synchronized public void announceFailure () {
        fails++;
    }

    private Map<Integer, String> votes = new HashMap<>();

    private int timeouts = 0, fails = 0;
    private List<Integer> newVotes = new ArrayList<>();
    synchronized void addVote (Integer port, String vote) {
        timeouts = 0;
        if (votes.get(port) == null ) {
            votes.put(port, vote);
            System.out.println(port + " added vote " + vote);
            newVotes.add(port);
        }
    }
    //TODO disconnect when all participants have timeouted

    synchronized public void increaseTimeouts () {
        timeouts++;
        System.out.println(timeouts);
        if (timeouts == ports.size() - fails) {
            decideOutcome();
        }
    }
    private String vote;

    private void vote () {
        System.out.println(" size " + options.size());
        vote = new String(options.get((new Random().nextInt(options.size()))));
        votes.put(port, vote);
        System.out.println("I chose " + vote);
    }
    List<String> options = new ArrayList<>();

    private void getOptions () {
        String options;
        try {
            while (!coordinatorReader.ready()) {
            }
            options = coordinatorReader.readLine();
            System.out.println("optiuni " + options);
            String[] spittedOptions = options.split(" ");
            for (int i = 1; i < spittedOptions.length; i++) {
                this.options.add(spittedOptions[i]);
                System.out.println("options receive on " + this.port + " " + spittedOptions[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    private ServerSocket serverSocket;

    Map<Integer, Socket> outputSocketsMap = new HashMap<>();
    private void connectToOtherParticipants () {
        Thread thread = new Thread(new ParticipantAcceptThread(this, ports, serverSocket));
        threads.add(thread);
        thread.start();

        for (Integer port : ports)
            addOutputSocket(port);
    }
     private void addOutputSocket (Integer port) {
        try {
            System.out.println("aici");
            Socket socket = new Socket("localhost", port);
            System.out.println(this.port + " connected to " + port);
            outputSocketsMap.put(port, socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Socket> inputSockets = new ArrayList<>();

    synchronized void addInputSocket (Socket socket) {
        inputSockets.add(socket);
    }
    List<Integer> ports = new ArrayList<>();

    private void getOtherPorts() {
        try {
            coordinatorReader = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
            String inputPorts;
            while (!coordinatorReader.ready()) {}
            inputPorts =  coordinatorReader.readLine();
            System.out.println("ports " + inputPorts);
            String[] splittedPorts = inputPorts.split(" ");
            for (int i = 1; i < splittedPorts.length; i++)
                ports.add(Integer.valueOf(splittedPorts[i]));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void sendPortToServer() {
        try {
            coordinatorWriter = new OutputStreamWriter(serverConnection.getOutputStream());
            System.out.println(port);
            coordinatorWriter.write("JOIN " + port + "\n");
            coordinatorWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectToServer() {
        try {
            serverConnection = new Socket("localhost", serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't connect to server");
        }
    }

    private void sendMessage (Socket s, String message) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(s.getOutputStream());
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void parseInput (String[] args) {
        serverPort = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        timeout = Integer.parseInt(args[2]);
        failureCondition = Integer.parseInt(args[3]);
    }

}
