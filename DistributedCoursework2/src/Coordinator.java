import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Coordinator {
    private int port, participantsNr;
    private ServerSocket serverSocket;
    private List<ParticipantDetails> participants = new ArrayList<>();
    List<String> options;

    public static void main (String[] args) {
        Coordinator coordinator = new Coordinator();

        // just for testings
        //args = coordinator.testing();

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
            e.printStackTrace();
            System.out.println ("Couldn't send message to the participants");
        }
        getOutcome();
    }

    private void getOutcome () {
        while (true) {
            for (ParticipantDetails p : participants) {
                try {
                    if (p.getReader().ready()) {
                        System.out.println(p.getReader().readLine());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void getPorts () {
        Scanner reader;
        for (ParticipantDetails p : participants) {
            try {
                reader = new Scanner(p.getConnection().getInputStream());
                while (!reader.hasNext()) {
                }
                p.setPort(Integer.parseInt(reader.nextLine().split(" ")[1]));
                System.out.println("Port " + p.getPort() + " receive!");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Couldn't read ports");
            }

        }

    }

    private void sendOptions () throws IOException {
        OutputStreamWriter writer;
        for (int i = 0; i < participants.size(); i++) {
            writer = new OutputStreamWriter(participants.get(i).getConnection().getOutputStream());
            writer.write("VOTE_OPTIONS ");
            for (String o : options) {
                System.out.println(o);
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
                    System.out.println (" portul este "+ participants.get(j).getPort());
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
                e.printStackTrace();
                System.out.println("Client could not connect");
            }
        }
    }

    private void parseArgs (String[] args) {
        port = Integer.parseInt(args[0]);
        participantsNr = Integer.parseInt(args[1]);
        options = new ArrayList<>();
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 2; i < args.length; i++)
            options.add(args[i]);
    }

    private String[] testing () {
        String[] args = new String[5];
        args[0] = "12345";
        args[1] = "2";
        args[2] = "A";
        args[3] = "B";
        args[4] = "C";
        return args;
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
