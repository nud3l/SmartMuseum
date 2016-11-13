package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.Agent;
import jade.proto.states.MsgReceiver;
import kth.id2209.homework1.pojo.User;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerAgent extends Agent {
    private static final String STATE_A = "A";
    private static final String STATE_B = "B";
    private static final String STATE_C = "C";
    private static final String RECV_ARTIFACT = "received-artifact";
    private MessageTemplate messageTemplate;
    private User user;
    private static final int TIMEOUT = 5000;
    public static final String ONTOLOGY = "English";

    private AID[] tourAgents;
    private AID curator;

    protected void setup() {
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "profiler"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        user = (User) getArguments()[0];

        tourAgents = Utilities.searchDF(this, "tour-guide");
        curator = Utilities.getService(this, "curator");

        if (curator == null || tourAgents.length == 0 || user == null)
            System.exit(1);

//        FSMBehaviour fsmBehaviour = new FSMBehaviour();
//
//        fsmBehaviour.registerFirstState(new OneShotBehaviour() {
//            @Override
//            public void action() {
//                ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.QUERY_REF, new AID[]{curator}, ONTOLOGY, "blabla");
//                aclMessage.setReplyWith("req" + System.currentTimeMillis());
//                myAgent.send(aclMessage);
//                messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
//            }
//        }, STATE_A);
//
//        Behaviour behaviour = new SimpleBehaviour() {
//            boolean receivedMessage = false;
//
//            @Override
//            public void action() {
//                ACLMessage reply = myAgent.receive(messageTemplate);
//
//                if (reply != null) {
//                    if (reply.getPerformative() == ACLMessage.INFORM_REF) {
//                        getDataStore().put(RECV_ARTIFACT, reply);
//                    } else if (reply.getPerformative() == ACLMessage.REFUSE) {
//                        System.out.println("No matching artifact");
//                    }
//                    receivedMessage = true;
//                } else {
//                    block();
//                }
//            }
//
//            @Override
//            public boolean done() {
//                return receivedMessage;
//            }
//        };
//
//        behaviour.setDataStore(fsmBehaviour.getDataStore());
//        fsmBehaviour.registerState(behaviour, STATE_B);
//
//        behaviour = new OneShotBehaviour() {
//            @Override
//            public void action() {
//                if (getDataStore().containsKey(RECV_ARTIFACT)) {
//                    System.out.println("Artifact received");
//                } else {
//                    System.out.println("*****Artifact not received");
//                }
//            }
//        };
//
//        behaviour.setDataStore(fsmBehaviour.getDataStore());
//        fsmBehaviour.registerLastState(behaviour, STATE_C);
//
//        fsmBehaviour.registerDefaultTransition(STATE_A, STATE_B);
//        fsmBehaviour.registerDefaultTransition(STATE_B, STATE_C);
//
//        addBehaviour(fsmBehaviour);
        addBehaviour(new ProfilerInteractionWake(this, TIMEOUT));
    }

    private static class ProfilerInteractionWake extends WakerBehaviour {
        ProfilerAgent agent;

        ProfilerInteractionWake(ProfilerAgent a, long timeout) {
            super(a, timeout);
            this.agent = a;
        }

        @Override
        protected void onWake() {
            agent.addBehaviour(new ProfilerInteractionFSM(agent));
        }
    }

    private static class ProfilerInteractionFSM extends FSMBehaviour {
        ArrayList<Integer> artifactList;

        public ProfilerInteractionFSM(ProfilerAgent a) {
            super(a);
            artifactList = new ArrayList<>();

            registerFirstState(new SeqBehaviourTourAgent(a), "1");
            registerLastState(new SeqBehaviourCurator(a), "2");

            registerDefaultTransition("1", "2");
        }
    }

    private static class SeqBehaviourTourAgent extends SequentialBehaviour {
        ProfilerAgent agent;
        MessageTemplate messageTemplate;
        DataStore ds;

        SeqBehaviourTourAgent(ProfilerAgent agent) {
            this.agent = agent;

            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    try {
                        ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.REQUEST, agent.tourAgents, ONTOLOGY, agent.user.getInterests());
                        aclMessage.setReplyWith("req" + System.currentTimeMillis());
                        agent.send(aclMessage);
                        messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            addSubBehaviour(new MsgReceiver(agent, messageTemplate, MsgReceiver.INFINITE, ds = new DataStore(), 0));
        }

        @Override
        public int onEnd() {
            Object o = ds.get(0);
            //TODO - get the tour and save to a state in FSM

            return 0;
        }
    }

    private static class SeqBehaviourCurator extends SequentialBehaviour {
        ProfilerAgent agent;
        MessageTemplate messageTemplate;
        DataStore ds;

        public SeqBehaviourCurator(ProfilerAgent agent) {
            this.agent = agent;
            ds = new DataStore();

            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.QUERY_REF, new AID[]{agent.curator}, ONTOLOGY, "blabla");
                    aclMessage.setReplyWith("req" + System.currentTimeMillis());
                    myAgent.send(aclMessage);
                    messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
                }
            });

            addSubBehaviour(new SimpleBehaviour() {
                boolean receivedMessage = false;

                @Override
                public void action() {
                    ACLMessage reply = myAgent.receive(messageTemplate);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM_REF) {
                            ds.put(RECV_ARTIFACT, reply);
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
            });

            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    if (ds.containsKey(RECV_ARTIFACT)) {
                        System.out.println("Artifact received");
                    } else {
                        System.out.println("*****Artifact not received");
                    }
                }
            });
        }
    }

    protected void takeDown() {
        System.out.println("Profiler-agent " + getAID().getName() + " terminating.");
    }
}
