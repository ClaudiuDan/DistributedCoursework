import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class ParticipantAcceptThread implements Runnable {

    private Participant participant;
    private List<Integer> ports = new ArrayList<>();
    private ServerSocket serverSocket;
    ParticipantAcceptThread(Participant participant, List<Integer> ports, ServerSocket serverSocket) {
        this.participant = participant;
        this.ports = ports;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            participant.addInputSocket(serverSocket.accept());
        } catch (IOException e) {
            System.out.println("Participant couldn't connect to other participant");
            e.printStackTrace();
        }
    }
}
