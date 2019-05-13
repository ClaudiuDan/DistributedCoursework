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
        runConsensusAlgorithm();

    }
    private void runConsensusAlgorithm() {
        for (Socket s : outputSocketsMap.values()) {
            String votesToSend = new String("VOTE");
            for (Integer port : newVotes.keySet())
                votesToSend = votesToSend + " " + port + " " + newVotes.get(port);
            System.out.println ("Sends votes " + votesToSend);
            sendMessage(s, votesToSend);
            if (failureCondition == 1) {
                System.out.println ("Will fail now");
                System.exit(0);
            }
        }
        boolean justSent = (!newVotes.isEmpty());
        newVotes.clear();
        boolean toNextRound = false;
        for (Socket s : inputSockets) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
                String message = reader.readLine();
                if (message == null)
                    throw (new IOException());
                System.out.println("received " + message);
                toNextRound = parseVoteMessage(message) ||  toNextRound;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (failureCondition == 2)
            System.exit(0);
        if (toNextRound && !justSent)
            runConsensusAlgorithm();
        else
            decideOutcome();
    }

    boolean parseVoteMessage (String message) {
        boolean toNextRound = false;
        String[] splittedMessage = message.split(" ");
        for (int i = 1; i < splittedMessage.length; i += 2) {
            toNextRound = true;
            addVote(Integer.valueOf(splittedMessage[i]), splittedMessage[i + 1]);
        }
        return toNextRound;
    }

    private List<Thread> threads = new ArrayList<>();

    private void decideOutcome() {
        /*if (port == 12346) {
            for (Thread t : threads)
                t.interrupt();
            System.exit(0);
        }*/
        for (Integer port : oldVotes.keySet())
            if (votes.get(port) == null)
                votes.put(port, oldVotes.get(port));

        Map<String, Integer> voteCounter = new HashMap<>();
        String message = new String(" ");
        for (Integer p : votes.keySet()) {
            message = message + p + " ";
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
        System.out.println("Sends to coordinator " + message);
        if (maxim >= votes.keySet().size() / 2 + 1) {
            System.out.println("E majoritate");
            sendMessage(serverConnection, candidate + message);
        }
        else
            sendMessage(serverConnection, null);

        waitForRestart();
    }

    private void waitForRestart() {
        try {
            System.out.println("Waiting for restart");
            if (coordinatorReader.readLine().equals("RESTART")) {
                System.out.println("Restarting");
                reset();
            }
        } catch (IOException e) {
            for (Thread t : threads)
                t.interrupt();
            System.exit(0);
        }

    }

    private Map<Integer, String> oldVotes = new HashMap<>();
    private void reset () {
        options.remove(options.size() - 1);
        for (Integer port : votes.keySet())
            if (options.contains(votes.get(port)))
                oldVotes.put(port, votes.get(port));
        votes.clear();
        vote();
        runConsensusAlgorithm();
    }

    private Map<Integer, String> votes = new HashMap<>();

    private int fails = 0;
    private Map<Integer, String> newVotes = new HashMap<>();
    synchronized void addVote (Integer port, String vote) {
        if (votes.get(port) == null ) {
            votes.put(port, vote);
            System.out.println(port + " added vote " + vote);
            newVotes.put(port, vote);
        }
    }

    private String vote;
    private void vote () {
        vote = new String(options.get((new Random().nextInt(options.size()))));
        votes.put(port, vote);
        newVotes.put(port, vote);
        System.out.println("I chose " + vote);
    }
    List<String> options = new ArrayList<>();

    private void getOptions () {
        String options;
        try {
            while (!coordinatorReader.ready()) {
            }
            options = coordinatorReader.readLine();
            String[] spittedOptions = options.split(" ");
            for (int i = 1; i < spittedOptions.length; i++) {
                this.options.add(spittedOptions[i]);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    private ServerSocket serverSocket;

    Map<Integer, Socket> outputSocketsMap = new HashMap<>();
    private void connectToOtherParticipants () {
        for (Integer port : ports)
            addOutputSocket(port);
        for (int i = 0; i < ports.size(); i++) {
            try {
                addInputSocket(serverSocket.accept());
                System.out.println ("New connection accepted");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
     private void addOutputSocket (Integer port) {
        try {
            Socket socket = new Socket("localhost", port);
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
