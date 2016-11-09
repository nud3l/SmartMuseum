package kth.id2209.homework1.behaviour;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * Created by tharidu on 11/9/16.
 */
public class CuratorDetailBehaviour extends CyclicBehaviour {
    @Override
    public void action() {
        ACLMessage aclMessage = myAgent.receive();

        if (aclMessage != null) {
            ACLMessage reply = aclMessage.createReply();
            System.out.println("Got something from profiler");
            // TODO - lookup

            reply.setContent("blabla");
            myAgent.send(reply);
        } else {
            block();
        }
    }
}
