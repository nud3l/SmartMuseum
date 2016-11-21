package kth.id2209.homework2;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by nud3l on 11/21/16.
 */
public class Main {
    private static final String PKG = "kth.id2209.homework2.agent";
    public static final int PORT = 1099;

    public static void main(String[] args) throws UnknownHostException, StaleProxyException {
        String ipAddress = InetAddress.getLocalHost().getHostAddress();

        Runtime runtime = Runtime.instance();
        runtime.setCloseVM(true);

        Profile mProfile = new ProfileImpl(ipAddress, PORT, null);
        runtime.createMainContainer(mProfile).createNewAgent("rma", "jade.tools.rma.rma", new Object[0]).start();

        Profile aProfile = new ProfileImpl(ipAddress, PORT, null);
        AgentContainer agentContainer = runtime.createAgentContainer(aProfile);

        // Create artist manager
        agentContainer.createNewAgent("artistmanager", PKG + ".ArtistManagementAgent", new Object[0]).start();

        // Create 3 curators
        for (int i = 0; i < 3; i++) {
            agentContainer.createNewAgent("curator" + i, PKG + ".CuratorAgent", new Object[0]).start();
        }
    }
}
