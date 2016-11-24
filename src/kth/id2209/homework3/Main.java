package kth.id2209.homework3;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Created by tharidu on 11/23/16.
 */
public class Main {
    private static final String PKG = "kth.id2209.homework3.agent";
    public static final int PORT = 60000;
    public static final int N = 4;

    public static int[] board;

    public static void main(String[] args) throws UnknownHostException, StaleProxyException {
        String ipAddress = InetAddress.getLocalHost().getHostAddress();
        Runtime runtime = Runtime.instance();
        runtime.setCloseVM(true);

        Profile mProfile = new ProfileImpl(ipAddress, PORT, null);
        runtime.createMainContainer(mProfile).createNewAgent("rma", "jade.tools.rma.rma", new Object[0]).start();

        Profile aProfile = new ProfileImpl(ipAddress, PORT, null);
        AgentContainer agentContainer = runtime.createAgentContainer(aProfile);

        // Create N * N chess board using one-dimensional array
        board = new int[N];
        Arrays.fill(board, -1);

        // First queen
        agentContainer.createNewAgent("queen0", PKG + ".Queen", new Object[]{N, 0, board, true, true}).start();

        // Create N queens
        for (int i = 1; i < N; i++) {
            agentContainer.createNewAgent("queen" + i, PKG + ".Queen", new Object[]{N, i}).start();
        }
    }
}
