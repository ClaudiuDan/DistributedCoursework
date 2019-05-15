import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

public class Participant {
    private int serverPort;
    private int port;


    private int timeout;
    private int failureCondition;
    private Socket serverConnection;
    BufferedReader coordinatorReader;
    OutputStreamWriter coordinatorWriter;
    public static void main (String[] args) {
        Participant participant = new Participant();
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
        int counter = 0;
        while (true) {
            ++counter;
            System.out.println("PARTICIPANT " + this.port + " enters round " + counter);
            runConsensusAlgorithm();
        }
    }
    private void runConsensusAlgorithm() {

        // sends any new votes to the other participants or empty vote otherwise
        for (Socket s : outputSocketsMap.values()) {
            String votesToSend = new String("VOTE");
            for (Integer port : newVotes.keySet())
                votesToSend = votesToSend + " " + port + " " + newVotes.get(port);
            System.out.println ("PARTICIPANT " + this.port + " sends votes " + votesToSend);
            sendMessage(s, votesToSend);
            if (failureCondition == 1) {
                System.out.println ("PARTICIPANT " + this.port + " will fail now");
                try {
                    Thread.sleep(100
                    );
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                System.exit(0);
            }
        }
        boolean justSent = (!newVotes.isEmpty());
        newVotes.clear();
        boolean toNextRound = false;

        // gets messages from other participants
        for (BufferedReader reader : inputSockets) {
            try {
                String message = reader.readLine();
                if (message == null) {
                    throw (new IOException());
                }
                System.out.println("PARTICIPANT " + this.port + " received " + message);
                toNextRound = parseVoteMessage(message) ||  toNextRound;
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
        if (failureCondition == 2)
            System.exit(0);

        // checks if another round will be played
        if (toNextRound || justSent)
            return;
        else
            decideOutcome();
    }

    boolean parseVoteMessage (String message) {
        boolean toNextRound = false;
        String[] splittedMessage = message.split(" ");

        // checks if it is an empty vote
        for (int i = 1; i < splittedMessage.length; i += 2) {
            toNextRound = true;
            addVote(Integer.valueOf(splittedMessage[i]), splittedMessage[i + 1]);
        }
        return toNextRound;
    }

    private List<Thread> threads = new ArrayList<>();


    // decides the outcome based on the votes obtained
    private void decideOutcome() {

        Map<String, Integer> voteCounter = new HashMap<>();
        String message = new String("");

        // counts the number of votes
        for (Integer p : votes.keySet()) {
            message = message + " " + p;
            if (voteCounter.get(votes.get(p)) != null)
                voteCounter.put(votes.get(p), voteCounter.get(votes.get(p)) + 1);
            else 
                voteCounter.put(votes.get(p), 1);
        }
        int maxim = -1;
        String candidate = null;

        // finds the most voted option
        for (String s : voteCounter.keySet())
            if (maxim < voteCounter.get(s)) {
                maxim = voteCounter.get(s);
                candidate = s;
            }

        if (maxim >= votes.keySet().size() / 2 + 1) {
            sendMessage(serverConnection, "OUTCOME " + candidate + message);
            System.out.println("PARTICIPANT " + this.port + " sends to coordinator OUTCOME " + candidate + message);
        }
        else {
            sendMessage(serverConnection, "OUTCOME null");
            System.out.println("PARTICIPANT " + this.port + " sends to coordinator OUTCOME null");
        }

        waitForRestart();
    }

    // waits for a message from the coordinator in case of a restart
    private void waitForRestart() {
        try {
            System.out.println("PARTICIPANT " + this.port + " waiting for restart");
            if (coordinatorReader.readLine().equals("RESTART")) {
                System.out.println("PARTICIPANT " + this.port + " restarting");
                reset();
            }
        } catch (IOException e) {
            System.out.println("PARTICIPANT " + port + " no restart needed, shutting down");
            System.exit(0);
        }

    }

    // resets the participant for a new run
    private void reset () {
        options.remove(options.size() - 1);
        votes.clear();
        vote();
        runConsensusAlgorithm();
    }

    private Map<Integer, String> votes = new HashMap<>(), newVotes = new HashMap<>();

    // adds a vote to the the list of votes
    synchronized void addVote (Integer port, String vote) {
        if (votes.get(port) == null ) {
            votes.put(port, vote);
            System.out.println("PARTICIPANT " + this.port + " added vote " + vote);
            newVotes.put(port, vote);
        }
    }

    private String vote;
    private void vote () {
        vote = new String(options.get((new Random().nextInt(options.size()))));
        votes.put(port, vote);
        newVotes.put(port, vote);
        System.out.println("PARTICIPANT " + this.port + " votes " + vote);
    }
    List<String> options = new ArrayList<>();

    //gets the list of options from the coordinator
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
            System.out.println ( "PARTICIPANT " + this.port + " received options from coordinator");
        }
        catch (IOException e) {
            System.out.println ( "PARTICIPANT " + this.port + " couldn't get options from coordinator");
            //e.printStackTrace();
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
                System.out.println ("PARTICIPANT " + this.port + " New connection accepted");
            } catch (IOException e) {
                System.out.println ( "PARTICIPANT " + this.port + " couldn't connect to someone");
                //e.printStackTrace();
            }
        }

    }
     private void addOutputSocket (Integer port) {
        try {
            Socket socket = new Socket("localhost", port);
            outputSocketsMap.put(port, socket);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    private List<BufferedReader> inputSockets = new ArrayList<>();

    synchronized void addInputSocket (Socket socket) throws IOException {
        inputSockets.add(new BufferedReader(new InputStreamReader(socket.getInputStream())));
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
            //e.printStackTrace();
            System.out.println("PARTICIPANT " + this.port + " couldn't connect to server");
        }
    }

    private void sendMessage (Socket s, String message) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(s.getOutputStream());
            writer.write(message + "\n");
            writer.flush();
        } catch (IOException e) {
            //e.printStackTrace();
        }

    }

    private void parseInput (String[] args) {
        serverPort = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        timeout = Integer.parseInt(args[2]);
        failureCondition = Integer.parseInt(args[3]);
    }

}
