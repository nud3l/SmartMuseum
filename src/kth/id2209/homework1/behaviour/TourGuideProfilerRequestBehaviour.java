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
        // Receive only request messages
        MessageTemplate profilerTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        ACLMessage profilerRequest = myAgent.receive(profilerTemplate);

        if (profilerRequest != null) {
            // Store request information and accept request
            AID profiler = profilerRequest.getSender();
            String conversationId = profilerRequest.getConversationId();
            String interestString = profilerRequest.getContent();
            List<String> interestList = Arrays.asList(interestString.split("\\s*,\\s*"));
            // TODO: Store request info

            ACLMessage reply = profilerRequest.createReply();
            System.out.println("Got something from profiler");

            // Propose to create virtual tour
            reply.setPerformative(ACLMessage.PROPOSE);
            myAgent.send(reply);

        } else {
            block();
        }

    }
}
