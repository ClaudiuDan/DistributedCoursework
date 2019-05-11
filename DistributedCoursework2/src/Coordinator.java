import org.omg.CORBA.SystemException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Coordinator {
    private int port, participantsNr;
    private ServerSocket serverSocket;
    private List<ParticipantDetails> participants = new ArrayList<>();
    List options;

    public static void main (String[] args) {
        Coordinator coordinator = new Coordinator();

        // just for testings
        args = coordinator.testing();

        coordinator.parseArgs(args);
        coordinator.start();
    }

    private void start () {
        establishConnections();
        getPorts();
        try {
            sendInfo();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println ("Couldn't send info to the participants");
        }
    }

    private void getPorts () {
        InputStreamReader reader;
        for (ParticipantDetails p : participants) {
            try {
                reader = new InputStreamReader(p.getConnection().getInputStream());
                while (!reader.ready()) {
                }
                p.setPort(reader.read());
                System.out.println("Port " + p.getPort() + " receive!");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Couldn't read ports");
            }

        }

    }

    private void sendInfo () throws IOException {
        OutputStream writer;
        for (int i = 0; i < participants.size(); i++) {
            writer = participants.get(i).getConnection().getOutputStream();
            for (int j = 0; j < participants.size(); j++)
                if (i != j)
                    writer.write(participants.get(j).getPort());
        }
    }

    private void establishConnections() {

        for (int i = 0; i < participantsNr; i++) {
            System.out.println("aici");
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
        options = new ArrayList();
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
        args[1] = "1";
        args[2] = "A";
        args[3] = "B";
        args[4] = "C";
        return args;
    }

    class ParticipantDetails {
        int port;
        Socket connection;

        ParticipantDetails (Socket connection) {
            this.connection = connection;
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
