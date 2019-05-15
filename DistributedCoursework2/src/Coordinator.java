import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Coordinator {
    private int port, participantsNr;
    private ServerSocket serverSocket;
    private List<ParticipantDetails> participants = new ArrayList<>();
    List<String> options;

    public static void main (String[] args) {
        Coordinator coordinator = new Coordinator();
        coordinator.parseArgs(args);
        coordinator.start();
    }

    private void start () {
        establishConnections();
        getPorts();
        try {
            sendInfo();
            sendOptions();
        }
        catch (IOException e) {
            //e.printStackTrace();
            System.out.println ("Couldn't send message to the participants");
        }
        while (true) {
            getOutcome();
        }
    }
    boolean fails[];
    private void getOutcome () {
        int counter = 0;
        Arrays.fill(fails, false);
        String messageReceived = null;
        String outcome = null;
        boolean atLeastOne = false;
        ParticipantDetails p;

        // waits for outcomes from the participants
        while (counter < participantsNr) {
            for (int i = 0; i < participants.size(); i++) {
                p = participants.get(i);
                try {
                    messageReceived = p.getReader().readLine();
                    if (messageReceived == null)
                        throw (new IOException());
                    atLeastOne = true;
                    fails[i] = false;
                    counter++;
                    System.out.println("COORDINATOR " + " received from " + p.getPort() + " " + messageReceived);
                } catch (IOException e) {
                    if (fails[i] == false) {
                        counter++;
                    }
                    fails[i] = true;
                }
                if (messageReceived != null && outcome == null)
                    outcome = messageReceived;
            }
        }
        System.out.println(atLeastOne + " " + counter + " " + participantsNr);
        if (!atLeastOne) {
            System.out.println("COORDINATOR " + " all participants crashed");
            System.exit(-1);
        }
        System.out.println(outcome);
        if (outcome.equals("COORDINATOR there is a tie, will restart")) {
            sendRestartToAll();
        }
        else {
            System.out.println("COORDINATOR the outcome is " + outcome);
            System.exit(0);
        }
    }

    private void getPorts () {
        BufferedReader reader;
        for (ParticipantDetails p : participants) {
            try {
                reader = p.getReader();
                p.setPort(Integer.parseInt(reader.readLine().split(" ")[1]));
                System.out.println("Port " + p.getPort() + " receive!");
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("COORDINATOR couldn't read ports");
            }

        }

    }

    private void sendRestartToAll () {
        OutputStreamWriter writer;
        for (ParticipantDetails p : participants) {
            try {
                writer = new OutputStreamWriter(p.getConnection().getOutputStream());
                writer.write("RESTART\n");
                writer.flush();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

    }

    private void sendOptions () throws IOException {
        OutputStreamWriter writer;
        for (int i = 0; i < participants.size(); i++) {
            writer = new OutputStreamWriter(participants.get(i).getConnection().getOutputStream());
            writer.write("VOTE_OPTIONS ");
            for (String o : options) {
                writer.write(o + " ");
            }
            writer.write("\n");

            writer.flush();
        }
    }

    private void sendInfo () throws IOException {
        OutputStreamWriter writer;
        for (int i = 0; i < participants.size(); i++) {
            writer = new OutputStreamWriter(participants.get(i).getConnection().getOutputStream());
            writer.write("DETAILS ");
            for (int j = 0; j < participants.size(); j++)
                if (i != j) {
                    writer.write(participants.get(j).getPort() + " ");
                }
            writer.write("\n");
            writer.flush();
        }
    }

    private void establishConnections() {

        for (int i = 0; i < participantsNr; i++) {
            try {
                participants.add(new ParticipantDetails(serverSocket.accept()));
            } catch (IOException e) {
                //e.printStackTrace();
                System.out.println("COORDINATOR a client could not connect");
            }
        }
    }

    private void parseArgs (String[] args) {
        port = Integer.parseInt(args[0]);
        participantsNr = Integer.parseInt(args[1]);
        fails = new boolean[participantsNr];
        options = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 2; i < args.length; i++)
            options.add(args[i]);
    }

    class ParticipantDetails {
        int port;
        Socket connection;

        public BufferedReader getReader() {
            return reader;
        }

        BufferedReader reader;
        ParticipantDetails (Socket connection) {
            this.connection = connection;
            try {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ParticipantDetails (int port) {
            this.port = port;
        }
        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Socket getConnection() {
            return connection;
        }

        public void setConnection(Socket connection) {
            this.connection = connection;
        }
    }
}
