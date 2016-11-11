package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.Agent;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerAgent extends Agent {
    private static final String STATE_A = "A";
    private static final String STATE_B = "B";
    private static final String STATE_C = "C";
    private static final String RECV_ARTIFACT = "received-artifact";
    private MessageTemplate messageTemplate;

    protected void setup() {
//        addBehaviour(new ProfilerDetailRequestBehaviour());

        FSMBehaviour fsmBehaviour = new FSMBehaviour();
        fsmBehaviour.registerFirstState(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = new ACLMessage(ACLMessage.QUERY_REF);
                aclMessage.addReceiver(new AID("curator", AID.ISLOCALNAME));
                aclMessage.setContent("blabla");
                aclMessage.setReplyWith("req" + System.currentTimeMillis());
                myAgent.send(aclMessage);
                messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
            }
        }, STATE_A);

        Behaviour behaviour = new SimpleBehaviour() {
            boolean receivedMessage = false;

            @Override
            public void action() {
                ACLMessage reply = myAgent.receive(messageTemplate);

                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM_REF) {
                        getDataStore().put(RECV_ARTIFACT, reply);
                    } else if (reply.getPerformative() == ACLMessage.REFUSE) {
                        System.out.println("No matching artifact");
                    }
                    receivedMessage = true;
                } else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return receivedMessage;
            }
        };

        behaviour.setDataStore(fsmBehaviour.getDataStore());
        fsmBehaviour.registerState(behaviour, STATE_B);

        behaviour = new OneShotBehaviour() {
            @Override
            public void action() {
                if (getDataStore().containsKey(RECV_ARTIFACT)) {
                    System.out.println("Artifact received");
                } else {
                    System.out.println("*****Artifact not received");
                }
            }
        };

        behaviour.setDataStore(fsmBehaviour.getDataStore());
        fsmBehaviour.registerLastState(behaviour, STATE_C);

        fsmBehaviour.registerDefaultTransition(STATE_A, STATE_B);
        fsmBehaviour.registerDefaultTransition(STATE_B, STATE_C);

        addBehaviour(fsmBehaviour);
    }

    protected void takeDown() {
        System.out.println("Profiler-agent " + getAID().getName() + " terminating.");
    }
}
