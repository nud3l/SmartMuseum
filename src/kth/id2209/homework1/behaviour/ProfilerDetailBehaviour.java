package kth.id2209.homework1.behaviour;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerDetailBehaviour extends OneShotBehaviour {
    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("curator", AID.ISLOCALNAME));
        msg.setContent("A0001");
        myAgent.send(msg);
    }
}
