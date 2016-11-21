package kth.id2209.homework2.agent;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import kth.id2209.homework1.agent.Utilities;

/**
 * Created by nud3l on 11/21/16.
 * Bidder -> need to be multiple
 */
public class CuratorAgent extends Agent{
    protected void setup() {

        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "bidder"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Curator " + getAID().getName() + " is ready.");

        // Behaviours
        // Receive INFORM
        // Handle CFP
        // Handle accepted proposal
        // Handle rejected
        // Handle two strategies

    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
        }
    }
}
