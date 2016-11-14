package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.Agent;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;
import kth.id2209.homework1.pojo.Artifact;
import kth.id2209.homework1.pojo.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by tharidu on 11/9/16.
 */
public class ProfilerAgent extends Agent {
    private static final String STATE_A = "A";
    private static final String STATE_B = "B";
    private static final String RECV_ARTIFACT = "received-artifact";
    private static final int TIMEOUT = 5000;

    private User user;
    private AID[] tourAgents;
    private AID curator;
    private Long[] tour;

    protected void setup() {

        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "profiler"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Profiler " + getAID().getName() + " is ready.");

        // Set user
        user = (User) getArguments()[0];

        System.out.println("----User interests are " + Arrays.toString(user.getInterests()) + "----");

        // Set other agents
        tourAgents = Utilities.searchDF(this, "tour-guide");
        curator = Utilities.getService(this, "curator");

        // If invalid arguments or agents, exit with error
        if (curator == null || tourAgents.length == 0 || user == null)
            System.exit(1);

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
            // Start profiler behaviour after the timeout
            agent.addBehaviour(new ProfilerInteractionFSM(agent));
        }
    }

    private static class ProfilerInteractionFSM extends FSMBehaviour {

        public ProfilerInteractionFSM(ProfilerAgent a) {
            super(a);

            // Register FSM states
            registerFirstState(new SeqBehaviourTourAgent(a), STATE_A);
            registerLastState(new SeqBehaviourCurator(a), STATE_B);

            registerDefaultTransition(STATE_A, STATE_B);
        }
    }

    // Sequential behaviour with tour agent
    private static class SeqBehaviourTourAgent extends SequentialBehaviour {
        ProfilerAgent agent;
        MessageTemplate messageTemplate;
        DataStore ds;

        SeqBehaviourTourAgent(ProfilerAgent agent) {
            this.agent = agent;

            // Initial request to tour agent
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    try {
                        ACLMessage aclMessage = Utilities.createAclMessage(ACLMessage.REQUEST, agent.tourAgents, agent.user.getInterests());
                        aclMessage.setReplyWith("req" + System.currentTimeMillis());
                        agent.send(aclMessage);
                        messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Response from tour agent and agreeing to the proposal
            addSubBehaviour(new SimpleBehaviour() {
                boolean finished = false;

                @Override
                public void action() {
                    ACLMessage aclMessage = myAgent.receive(messageTemplate);
                    if (aclMessage != null) {
                        finished = true;
                        if (aclMessage.getPerformative() == ACLMessage.PROPOSE) {
                            ACLMessage reply = aclMessage.createReply();
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            try {
                                reply.setContentObject(agent.user.getInterests());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            myAgent.send(reply);
                            messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                        } else {
                            reset();
                        }
                    } else {
                        block();
                    }
                }

                @Override
                public boolean done() {
                    return finished;
                }
            });

            addSubBehaviour(new MsgReceiver(agent, messageTemplate, MsgReceiver.INFINITE, ds = new DataStore(), 0));
        }

        @Override
        public int onEnd() {
            ACLMessage aclMessage = (ACLMessage) ds.get(0);
            try {
                // Receive artifact IDs relevant to user's interests
                agent.tour = (Long[]) aclMessage.getContentObject();
                System.out.println("----Tour received from the tour agent----");
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            return 0;
        }
    }

    // Sequential behaviour with curator
    private static class SeqBehaviourCurator extends SequentialBehaviour {
        ProfilerAgent agent;
        MessageTemplate messageTemplate;
        DataStore ds;

        public SeqBehaviourCurator(ProfilerAgent agent) {
            this.agent = agent;
            ds = new DataStore();

            // Send the tour to curator
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    System.out.println("----Contacting curator for detailed information about the tour----");

                    ACLMessage aclMessage = null;
                    try {
                        aclMessage = Utilities.createAclMessage(ACLMessage.QUERY_REF, new AID[]{agent.curator}, agent.tour);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    aclMessage.setReplyWith("req" + System.currentTimeMillis());
                    myAgent.send(aclMessage);
                    messageTemplate = MessageTemplate.MatchInReplyTo(aclMessage.getReplyWith());
                }
            });

            // Receive artifact details on the tour
            addSubBehaviour(new SimpleBehaviour() {
                boolean receivedMessage = false;

                @Override
                public void action() {
                    ACLMessage reply = myAgent.receive(messageTemplate);

                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM_REF) {
                            try {
                                ds.put(RECV_ARTIFACT, reply.getContentObject());
                            } catch (UnreadableException e) {
                                e.printStackTrace();
                            }
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
                        ArrayList<Artifact> artifacts = (ArrayList<Artifact>) ds.get(RECV_ARTIFACT);
                        System.out.println("----Artifacts received----");

                        for (Artifact artifact :
                                artifacts) {
                            System.out.println(artifact.toString());
                        }
                    } else {
                        System.out.println("****Artifacts not received****");
                    }
                }
            });
        }
    }

    protected void takeDown() {
        System.out.println("Profiler-agent " + getAID().getName() + " terminating.");
    }
}
