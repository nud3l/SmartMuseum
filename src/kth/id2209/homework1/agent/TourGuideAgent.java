package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import kth.id2209.homework1.pojo.Artifact;
import kth.id2209.homework1.pojo.Enums;

import java.io.IOException;
import java.util.*;

/**
 * Created by tharidu on 11/9/16.
 */
public class TourGuideAgent extends Agent {
    private AID curator;

    public ACLMessage getProfilerMessage() {
        return profilerMessage;
    }

    public void setProfilerMessage(ACLMessage profilerMessage) {
        this.profilerMessage = profilerMessage;
    }

    private ACLMessage profilerMessage;

    protected void setup() {
        // Register virtual tour service in the yellow pages
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "tour-guide"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        curator = Utilities.getService(this, "curator");

        System.out.println("Hello! Tour Guide " + getAID().getName() + " is ready.");

        // Listen for requests from profiler
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Receive only profiler request messages
                MessageTemplate profilerRequestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage profilerRequest = myAgent.receive(profilerRequestTemplate);

                if (profilerRequest != null) {
                    // Store request information and accept request
                    ACLMessage reply = profilerRequest.createReply();
                    try {
                        Enums.interest[] interests = (Enums.interest[]) profilerRequest.getContentObject();
                        System.out.println("Got request from profiler");
                        // Propose to create virtual tour
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContentObject(interests);
                        myAgent.send(reply);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    block();
                }

            }
        });

        // Build virtual tour for profiler with looping sequential behaviour
        SequentialBehaviour virtualTourBuilder = new SequentialBehaviour() {
            public int onEnd() {
                reset();
                myAgent.addBehaviour(this);
                return super.onEnd();
            }
        };
        // Add subbehaviour to accept and store accept messages from the profiler
        virtualTourBuilder.addSubBehaviour(new SimpleBehaviour() {
            private boolean finished = false;

            @Override
            public void action() {
                // Receive only profiler accept messages
                MessageTemplate profilerAcceptTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage profilerAccept = myAgent.receive(profilerAcceptTemplate);

                if (profilerAccept != null) {
                    // Store profiler accept request
                    System.out.println("Got accept from profiler");
                    setProfilerMessage(profilerAccept);
                    finished = true;
                } else {
                    block();
                }
            }

            public boolean done() {
                return finished;
            }
        });

        // Add subbehaviour to request artifacts from curator
        virtualTourBuilder.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    // Load request message
                    ACLMessage profilerAccept = getProfilerMessage();
                    Enums.interest[] interests = (Enums.interest[]) profilerAccept.getContentObject();

                    // Request artifacts from curator
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(curator);
                    request.setContentObject(interests);
                    myAgent.send(request);
                } catch (UnreadableException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Add subbehaviour to receive artifacts from curator and send tour to profiler
        virtualTourBuilder.addSubBehaviour(new SimpleBehaviour() {
            boolean finished = false;

            @Override
            public void action() {
                // Receive only curator agree messages
                MessageTemplate curatorAgreeTemplate = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                ACLMessage curatorAgree = myAgent.receive(curatorAgreeTemplate);

                if (curatorAgree != null) {
                    finished = true;

                    // Get initial profiler accept message from datastore
                    ACLMessage profilerAccept = getProfilerMessage();
                    System.out.println("Preparing virtual tour");

                    // Send message to profiler
                    ACLMessage reply = profilerAccept.createReply();

                    // Get artifacts from curator
                    try {
                        Long[] artifacts = (Long[]) curatorAgree.getContentObject();
                        Set<Long> set = new LinkedHashSet<>(Arrays.asList(artifacts));
                        artifacts = new Long[set.size()];
                        reply.setPerformative(ACLMessage.AGREE);
                        reply.setContentObject(set.toArray(artifacts));
                        send(reply);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
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
        addBehaviour(virtualTourBuilder);
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Tour Guide " + getAID().getName() + " terminating.");
    }
}
