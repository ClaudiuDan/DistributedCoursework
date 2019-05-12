import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class ParticipantAcceptThread implements Runnable {

    private Participant participant;
    private ServerSocket serverSocket;
    private List<Integer> ports;
    //TODO s-ar putea sa nu ai nevoie de ports, doar de size
    ParticipantAcceptThread(Participant participant, List<Integer> ports, ServerSocket serverSocket) {
        this.participant = participant;
        this.serverSocket = serverSocket;

        this.ports = ports;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < ports.size(); i++) {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(participant.getTimeout());
                participant.addInputSocket(socket);
                (new Thread(new ListenerThread(participant, new BufferedReader(new InputStreamReader(socket.getInputStream()))))).start();
            }
        } catch (IOException e) {
            participant.announceFailure();
        }
    }

    class ListenerThread implements Runnable {

        private Participant participant;
        private BufferedReader reader;

        ListenerThread (Participant participant, BufferedReader reader) {
            this.participant = participant;
            this.reader = reader;
        }

        @Override
        public void run() {
            boolean shouldReport = true;
            while (true) {

                try {
                    String message = reader.readLine();
                    shouldReport = true;
                    parseMessage(message);
                }
                catch (SocketTimeoutException e) {
                    System.out.println("timeout");
                    if (shouldReport == true) {
                        participant.increaseTimeouts();
                        shouldReport = false;
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void parseMessage(String message) {
            String[] splitedMessage = message.split(" ");
            if (splitedMessage[0].equals("VOTE")) {
                parseVoteMessage(splitedMessage);
            }
        }

        private void parseVoteMessage (String[] message) {
            for (int i = 1; i < message.length; i += 2) {
                participant.addVote(Integer.valueOf(message[i]), message[i + 1]);
            }
        }
    }
}
