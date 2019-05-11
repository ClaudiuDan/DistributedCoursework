public class Participant {
    private int serverPort, port, timeout, failureCondition;
    public static void main (String[] args) {
        Participant participant = new Participant();
        participant.parseInput(args);
        
    }

    private void parseInput (String[] args) {
        serverPort = Integer.parseInt(args[0]);
        port = Integer.parseInt(args[1]);
        timeout = Integer.parseInt(args[2]);
        failureCondition = Integer.parseInt(args[3]);
    }
}
