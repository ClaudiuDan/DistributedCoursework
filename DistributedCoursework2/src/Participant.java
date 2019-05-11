import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class Participant {
    private int serverPort, port, timeout, failureCondition;
    private Socket serverConnection;
    public static void main (String[] args) {
        Participant participant = new Participant();

        //tests
        args = new String[4];
        args[0] = "12345";
        args[1] = "12346";
        args[2] = "5000";
        args[3] = "0";

        participant.parseInput(args);
        participant.start();
    }

    private void start () {
        connectToServer();
        sendPortToServer();
        while(true);
    }

    private void parseInput (String[] args) {
        serverPort = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        failureCondition = Integer.parseInt(args[3]);
    }

    private void sendPortToServer() {
        try {
            OutputStream writer = serverConnection.getOutputStream();
            writer.write(("JOIN " + port).getBytes());
            writer.flush();
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

}
