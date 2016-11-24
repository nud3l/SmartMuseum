package kth.id2209.homework3task2.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import kth.id2209.homework1.agent.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nud3l on 11/21/16.
 * Bidder -> need to be multiple
 */
public class CuratorAgent extends MobileAgent {
    private int myBalance = ThreadLocalRandom.current().nextInt(1000000, 5000000 + 1);
    private ArrayList<String> interests;
    private ArrayList<String> artifacts = new ArrayList<>();
    public String container;

    protected void deregisterDF() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
        }
    }

    protected void registerDF() {
        // Register in Directory Facilitator
        try {
            container = super.getDestination().getName();
            String serviceName = "bidder" + container;
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), serviceName));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    @Override
    protected void setup() {
        super.setup();

        System.out.println("Hello! Curator " + getAID().getName() + " is ready.");

        interests = testInterests(2);
        System.out.println("Curator interests " + interests);

        addBehaviour(new DutchAuctionBidder(this));
    }

    @Override
    void init() { super.init(); }

    @Override
    protected void beforeMove() {
        // deregisterDF();
        super.beforeMove();
    }

    @Override
    protected void afterMove() {
        super.afterMove();
        registerDF();
    }

    @Override
    protected void beforeClone() { super.beforeClone(); }

    @Override
    protected void afterClone() {
        init();
        registerDF();
    }

    private static class DutchAuctionBidder extends SequentialBehaviour {
        CuratorAgent agent;
        String artifact;
        int myvalue = 0;
        Boolean interested = false;

        DutchAuctionBidder(CuratorAgent agent) {
            this.agent = agent;
            // Handle inform
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    MessageTemplate InformTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage inform = agent.receive(InformTemplate);
                    if (inform != null) {
                        String content = inform.getContent();
                        if (content.contains("START")) {
                            artifact = content.replace("START ", "");
                            System.out.println("Curator INFORM START " + artifact);
                            if (agent.interests.contains(artifact)) {
                                interested = true;
                                myvalue = ThreadLocalRandom.current().nextInt(1000000, 1500000 + 1);

                            } else {
                                interested = false;
                                myvalue = ThreadLocalRandom.current().nextInt(100000, 400000 + 1);
                            }

                        } else if (content.contains("NO BIDS")) {
                            String contentArtifact = content.replace("NO BIDS ", "");
                            if (contentArtifact.equals(artifact)) {
                                System.out.println("Curator NO BIDS " + artifact);
                                artifact = null;
                                interested = false;
                            }
                        }
                    } else {
                        block();
                    }

                }
            });


            // Handle bidding
            addSubBehaviour(new Behaviour() {
                private int currentPrice  = 0;
                private int startPrice = 0;
                private int step = 0;
                private int auctionRound = 0;
                private AID[] curators = Utilities.searchDF(agent, "bidder" + agent.container);
                private ArrayList<Integer> curatorsValues = new ArrayList<>();
                private int minCurators = 0;
                @Override
                public void action() {
                    switch(step)  {
                        // Handle CFP
                        case 0:
                            MessageTemplate CFPTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                            ACLMessage cfp = agent.receive(CFPTemplate);
                            if (cfp != null) {
                                if (cfp.getContent() != null) {
                                    auctionRound += 1;
                                    // read in current/start price
                                    String content = cfp.getContent();
                                    System.out.println("Curator CURRENT PRICE " + content);
                                    if (startPrice == 0) {
                                        startPrice = Integer.parseInt(content);
                                        currentPrice = startPrice;
                                    } else {
                                        currentPrice = Integer.parseInt(content);
                                    }
                                    // Assume values that other curators might have based on rounds
                                    for (int i = 0; i < curators.length; i++) {
                                        if (curators[i] != agent.getAID()) {
                                            curatorsValues.add(ThreadLocalRandom.current().nextInt(100000, 500000 + 1));
                                        }
                                    }
                                    // Determine
                                    minCurators = curatorsValues.indexOf(Collections.min(curatorsValues));
                                    ACLMessage propose = cfp.createReply();
                                    propose.setPerformative(ACLMessage.PROPOSE);
                                    // Different strategies
                                    System.out.println("Curator MY VALUE " + myvalue);
                                    System.out.println("Curator INTERESTED " + interested);
                                    System.out.println("Curator MY BALANCE " + agent.myBalance);
                                    System.out.println("Curator AUCTION ROUND " + auctionRound);
                                    if (
                                            interested
                                            && (currentPrice < myvalue)
                                            && (currentPrice < agent.myBalance)
                                            && (auctionRound > 2)) {
                                        propose.setContent("YES");
                                        step = 1;
                                    } else if (
                                            (currentPrice < minCurators)
                                            && (currentPrice < agent.myBalance)
                                            && (auctionRound > 3)) {
                                        // We might get a bargain
                                        propose.setContent("YES");
                                        step = 1;
                                    } else {
                                        // Wait for a new CFP
                                        propose.setContent("NO");
                                        step = 0;
                                    }
                                    System.out.println("Curator PROPOSE " + artifact + "; Decision: " + propose.getContent());
                                    agent.send(propose);
                                } else {
                                    // send do not understand message
                                    ACLMessage notUnderstood = cfp.createReply();
                                    notUnderstood.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                                    System.out.println("Curator NOT_UNDERSTOOD " + artifact);
                                    agent.send(notUnderstood);
                                    // Exit current bidding
                                    step = 2;
                                }
                            }
                            else {
                                block();
                            }
                            break;

                        case 1:
                            MessageTemplate accept = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            MessageTemplate reject = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
                            MessageTemplate answerTemplate = MessageTemplate.or(accept, reject);
                            ACLMessage answer = agent.receive(answerTemplate);
                            if (answer != null) {
                                if (answer.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                                    // Handle accepted proposal
                                    System.out.println("Curator I MAY GOT " + artifact);
                                    step = 2;
                                } else if (answer.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                                    // Handle rejected
                                    System.out.println("Curator I DIDN'T GET " + artifact);
                                    step = 2;
                                }
                            }
                            else {
                                block();
                            }
                            break;
                    }
                }

                @Override
                public boolean done() {
                    return (artifact == null || step == 2);
                }
            });
        }


        public int onEnd() {
            reset();
            myAgent.addBehaviour(this);
            return super.onEnd();
        }
    }


    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
        }
    }

    // Load sample artifacts
    public ArrayList<String> testInterests(int number) {
        ArrayList<String> interestsByName = new ArrayList<>();
        String[] interests = new String[]{
                "Mona Lisa",
                "Girl with a Pearl Earring",
                "The Starry Night",
                "The Night Watch",
                "Sunflowers"
        };

        for (int i = 0; i < number; i++) {
            int random = ThreadLocalRandom.current().nextInt(0, 5);
            interestsByName.add(interests[random]);
        }

        return interestsByName;
    }
}
