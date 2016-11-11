package kth.id2209.homework1.behaviour;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerDetailRequestBehaviour extends OneShotBehaviour {
    @Override
    public void action() {
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_REF);
        msg.addReceiver(new AID("curator", AID.ISLOCALNAME));
        msg.setContent("A0001");
        MessageTemplate messageTemplate = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId(msg.getConversationId())
        );


        myAgent.send(msg);

    }
}

class ProfilerDetailResponseBehaviour extends OneShotBehaviour {

    @Override
    public void action() {
        ACLMessage reply = myAgent.receive();
    }
}
