import org.omg.CORBA.SystemException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Coordinator {
    private int port, participants;
    private ServerSocket serverSocket;
    private Socket[] clients;
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
        try {
            sendInfo();
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println ("Couldn't send info to the participants");
        }
    }

    private void sendInfo () throws IOException {
        OutputStream writer;
        for (int i = 0; i < clients.length; i++) {
            writer = clients[i].getOutputStream();
            for (int j = 0; j < clients.length; j++)
                if (i != j)
                    writer.write(clients[j].getPort());
        }
    }

    private void establishConnections() {
        for (int i = 0; i < participants; i++) {
            try {
                clients[i] = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Client could not connect");
            }
        }
    }

    private void parseArgs (String[] args) {
        port = Integer.parseInt(args[0]);
        participants = Integer.parseInt(args[1]);
        clients = new Socket[participants];
        options = new ArrayList();
        for (int i = 2; i < args.length; i++)
            options.add(args[i]);
    }

    private String[] testing () {
        String[] args = new String[5];
        args[0] = "12345";
        args[1] = "4";
        args[2] = "A";
        args[3] = "B";
        args[4] = "C";
        return args;
    }
}
