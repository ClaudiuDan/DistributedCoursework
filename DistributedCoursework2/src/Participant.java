import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Participant {
    private int serverPort, port, timeout, failureCondition;
    private Socket serverConnection;
    Scanner coordinatorReader;
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
    }

    private ServerSocket serverSocket;
    Map<Integer, Socket> outputSockets = new HashMap<>();
    private void connectToOtherParticipants () {
        Thread thread = new Thread(new ParticipantAcceptThread(this, ports, serverSocket));
        thread.start();
        System.out.println("aici pre");

        for (Integer port : ports)
            addOutputSocket(port);
    }

     private void addOutputSocket (Integer port) {
        try {
            System.out.println("aici");
            Socket socket = new Socket("localhost", port);
            System.out.println(this.port + " connected to " + port);
            outputSockets.put(port, socket);
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
            coordinatorReader = new Scanner(serverConnection.getInputStream());
            String inputPorts;
            while (!coordinatorReader.hasNext()) {}
            inputPorts =  coordinatorReader.nextLine();
            String[] splittedPorts = inputPorts.split(" ");
            for (int i = 1; i < splittedPorts.length; i++)
                ports.add(Integer.valueOf(splittedPorts[i]));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Couldn't read ports from coordinator");
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

    class ParticipantConnection {
        Socket outputSocket;

        public Socket getOutputSocket() {
            return outputSocket;
        }

        public void setOutputSocket(Socket outputSocket) {
            this.outputSocket = outputSocket;
        }

        public Socket getInputSocket() {
            return inputSocket;
        }

        public void setInputSocket(Socket inputSocket) {
            this.inputSocket = inputSocket;
        }

        Socket inputSocket;

    }
}
