package kth.id2209.homework1.behaviour;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Created by nud3l on 11/9/16.
 */
public class TourGuideProfilerRequestBehaviour extends CyclicBehaviour{
    @Override
    public void action() {
        // Receive only profiler request messages
        MessageTemplate profilerRequestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage profilerRequest = myAgent.receive(profilerRequestTemplate);

        if (profilerRequest != null) {
            // Store request information and accept request
            String interestString = profilerRequest.getContent();

            ACLMessage reply = profilerRequest.createReply();
            System.out.println("Got request from profiler");

            // Propose to create virtual tour
            reply.setPerformative(ACLMessage.PROPOSE);
            reply.setContent(interestString);

            myAgent.send(reply);

        } else {
            block();
        }

    }
}
