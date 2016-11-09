package kth.id2209.homework1.agent;

import kth.id2209.homework1.behaviour.ProfilerDetailBehaviour;
import jade.core.Agent;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerAgent extends Agent {
    protected void setup() {
        addBehaviour(new ProfilerDetailBehaviour());
    }

    protected void takeDown() {
        System.out.println("Profiler-agent " + getAID().getName() + " terminating.");
    }
}
