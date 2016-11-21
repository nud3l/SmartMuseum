package kth.id2209.homework2.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import kth.id2209.homework1.agent.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nud3l on 11/21/16.
 * Bidder -> need to be multiple
 */
public class CuratorAgent extends Agent{
    private int myBalance = ThreadLocalRandom.current().nextInt(100000, 5000000 + 1);
    private ArrayList<String> interests;
    private ArrayList<String> artifacts;

    protected void setup() {
        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "bidder"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Curator " + getAID().getName() + " is ready.");

        interests = testInterests(3);

        addBehaviour(new DutchAuctionBidder(this));

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
                    String content = inform.getContent();
                    if (content.contains("START")) {
                        artifact = content.replace("START ", "");
                        if (agent.interests.contains(artifact)) {
                            interested = true;
                            myvalue = ThreadLocalRandom.current().nextInt(100000, 1500000 + 1);
                        } else {
                            interested = false;
                            myvalue = ThreadLocalRandom.current().nextInt(10000, 40000 + 1);
                        }
                    } else if (content.contains("NO BIDS")) {
                        String contentArtifact = content.replace("NO BIDS ", "");
                        if (contentArtifact.equals(artifact)) {
                            artifact = null;
                            interested = false;
                        }
                    }
                }
            });


            // Handle bidding
            addSubBehaviour(new Behaviour() {
                private int currentPrice  = 0;
                private int startPrice = 0;
                private int step = 0;
                private int auctionRound = 0;
                private AID[] curators = Utilities.searchDF(agent, "bidder");
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
                                    // read in current/start price
                                    if (startPrice == 0) {
                                        startPrice = Integer.valueOf(cfp.getContent());
                                        currentPrice = startPrice;
                                    } else {
                                        currentPrice = Integer.valueOf(cfp.getContent());
                                    }
                                    // Assume values that other curators might have based on rounds
                                    for (int i = 0; i < curators.length; i++) {
                                        if (curators[i] != agent.getAID()) {
                                            curatorsValues.add(ThreadLocalRandom.current().nextInt(10000, 1500000 + 1));
                                        }
                                    }
                                    // Determine
                                    minCurators = curatorsValues.indexOf(Collections.min(curatorsValues));
                                    ACLMessage propose = cfp.createReply();
                                    propose.setPerformative(ACLMessage.PROPOSE);
                                    // Different strategies
                                    if (interested) {
                                        // We are interested
                                        if (currentPrice < myvalue
                                                && currentPrice < agent.myBalance
                                                && auctionRound > 3) {
                                            propose.setContent("YES");
                                            step = 1;
                                        }
                                    } else if (currentPrice < minCurators
                                            && currentPrice < agent.myBalance
                                            && auctionRound > 5) {
                                        // We might get a bargain
                                        propose.setContent("YES");
                                        step = 1;
                                    } else {
                                        // Wait for a new CFP
                                        propose.setContent("NO");
                                        step = 0;
                                    }
                                    agent.send(propose);
                                } else {
                                    // send do not understand message
                                    ACLMessage notUnderstood = cfp.createReply();
                                    notUnderstood.setPerformative(ACLMessage.NOT_UNDERSTOOD);
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
                                    // Update my balance
                                    agent.myBalance -= currentPrice;
                                    // Update my list of artifacts and interests
                                    agent.interests.remove(artifact);
                                    agent.artifacts.add(artifact);
                                    step = 2;
                                } else {
                                    // Handle rejected
                                    step = 0;
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
