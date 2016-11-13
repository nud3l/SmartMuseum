package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * Created by tharidu on 11/9/16.
 */
public class TourGuideAgent extends Agent {
    //Hashtable<Long, Interests[]> userInterests;
    //Hashtable<String, Artifact[]> virtualTour;
    private AID curator;

    protected void setup() {
        // Register virtual tour service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("tour-guide");
        sd.setName("virtual tour");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Get the curator
        curator = Utilities.getService(this, "curator");

        //userInterests = new Hashtable<>();
        //virtualTour = new Hashtable<>();

        System.out.println("Hello! Tour Guide "+getAID().getName()+" is ready.");

        // Listen for requests from profiler
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                // Receive only profiler request messages
                MessageTemplate profilerRequestTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage profilerRequest = myAgent.receive(profilerRequestTemplate);

                if (profilerRequest != null) {
                    // Store request information and accept request
                    String interest = profilerRequest.getContent();
                    ACLMessage reply = profilerRequest.createReply();

                    System.out.println("Got request from profiler");
                    // Propose to create virtual tour
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(interest);
                    myAgent.send(reply);

                } else {
                    block();
                }

            }
         });

        // Build virtual tour for profiler
        SequentialBehaviour virtualTourBuilder = new SequentialBehaviour() {
            public int onEnd() {
                reset();
                myAgent.addBehaviour(this);
                return super.onEnd();
            }
        };
        virtualTourBuilder.addSubBehaviour(new OneShotBehaviour(){
            @Override
            public void action() {
                // Receive only profiler accept messages
                MessageTemplate profilerAcceptTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage profilerAccept = myAgent.receive(profilerAcceptTemplate);

                if (profilerAccept != null) {
                    // Store request information and accept request
                    String interestString = profilerAccept.getContent();
                    System.out.println("Got accept from profiler");

                    // Request artifacts from curator
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(curator);
                    request.setContent(interestString);

                    myAgent.send(request);

                } else {
                    block();
                }

            }
        });
        virtualTourBuilder.addSubBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                // Receive only curator agree messages
                MessageTemplate curatorAgreeTemplate = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                ACLMessage curatorAgree = myAgent.receive(curatorAgreeTemplate);
                // TODO
                System.out.println("Preparing virtual tour");
                // Send message to profiler
            }
        });
        addBehaviour(virtualTourBuilder);
    }

    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Tour Guide "+getAID().getName()+" terminating.");
    }
}
