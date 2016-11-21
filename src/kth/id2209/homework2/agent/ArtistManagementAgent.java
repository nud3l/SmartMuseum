package kth.id2209.homework2.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import kth.id2209.homework1.agent.Utilities;
import kth.id2209.homework2.pojo.Auction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by nud3l on 11/21/16.
 * Auctioneer
 */
public class ArtistManagementAgent extends Agent {
    private static final int TIMEOUT = 5000;
    private AID[] curators;

    Hashtable<String, Auction> auctionsbyName;

    protected void setup() {

        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "auctioneer"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Artist Manager " + getAID().getName() + " is ready.");

        // Load test auctions
        auctionsbyName = new Hashtable<>();
        auctionsbyName = testAuctions();

        // find the bidders
        curators = Utilities.searchDF(this, "bidder");

        // If invalid arguments or agents, exit with error
        if (curators.length == 0)
            System.exit(1);

        // Handle auctions
        addBehaviour(new ArtistManagerInteractionWake(this, TIMEOUT));
    }

    // Wake after timeout
    private static class ArtistManagerInteractionWake extends WakerBehaviour {
        ArtistManagementAgent agent;

        ArtistManagerInteractionWake(ArtistManagementAgent a, long timeout) {
            super(a, timeout);
            this.agent = a;
        }

        @Override
        protected void onWake() {
            // Start profiler behaviour after the timeout
            agent.addBehaviour(new DutchAuction(agent));
        }
    }

    private static class DutchAuction extends SequentialBehaviour {
        ArtistManagementAgent agent;
        MessageTemplate messageTemplate;

        DutchAuction(ArtistManagementAgent agent) {
            this.agent = agent;
            ArrayList<AID> respondedCurators = new ArrayList<>();
            Auction artifact = agent.auctionsbyName.get("Mona Lisa");

            // ACLMessage INFORM
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    ACLMessage aclMessage = Utilities.createAclMessage(
                            ACLMessage.INFORM,
                            agent.curators,
                            ("Auction start of " + artifact.getArtworkName())
                    );
                    agent.send(aclMessage);
                }
            });


            addSubBehaviour(new Behaviour() {
                private AID winner = null;
                private AID[] rejected = new AID[agent.curators.length - 1];
                private int currentPrice;
                private int step = 0;
                int auctionRound = 0;
                @Override
                public void action() {
                    switch(step) {
                        case 0:
                            // CFP with current price
                            currentPrice = artifact.getInitialPrice()
                                    - (artifact.getReductionRate() * auctionRound);
                            if (currentPrice > artifact.getReservePrice()) {
                                try {
                                    ACLMessage cfp = Utilities.createAclMessage(ACLMessage.CFP, agent.curators, currentPrice);
                                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                                    agent.send(cfp);
                                    messageTemplate = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
                                    step = 1;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                ACLMessage aclMessage = Utilities.createAclMessage(
                                        ACLMessage.INFORM,
                                        agent.curators,
                                        ("No bids " + artifact.getArtworkName())
                                );
                                agent.send(aclMessage);
                                step = 3;

                            }
                            break;

                        case 1:
                            int repliesCount = 0;

                            ACLMessage proposal = myAgent.receive(messageTemplate);
                            if (proposal != null) {
                                // Exclude not understood curators from auction
                                if (proposal.getPerformative() == ACLMessage.NOT_UNDERSTOOD) {
                                    AID[] updatedCurators = new AID[agent.curators.length - 1];
                                    int counter = 0;
                                    AID curatorExclude = proposal.getSender();
                                    System.out.println("Got not understood from curator " + curatorExclude);
                                    for (int i = 0; i < agent.curators.length; i++) {
                                        if (agent.curators[i] != curatorExclude) {
                                            updatedCurators[counter] = agent.curators[i];
                                            counter += 1;
                                        }
                                    }
                                    agent.curators = updatedCurators;
                                    repliesCount += 1;
                                }
                                if (proposal.getPerformative() == ACLMessage.PROPOSE) {
                                    // First one to bid is the winner
                                    if (proposal.getContent().equals("YES") && winner == null) {
                                        winner = proposal.getSender();
                                    }
                                    // Late bidders will be rejected
                                    if (proposal.getContent().equals("YES") && winner != null) {
                                        AID late = proposal.getSender();
                                        for (int i = 0; i < rejected.length; i++) {
                                            if (late != rejected[i]) {
                                                rejected[i] = late;
                                            }
                                        }
                                    }
                                    repliesCount += 1;
                                }
                                if (repliesCount >= agent.curators.length && winner != null) {
                                    step = 2;
                                } else if (repliesCount >= agent.curators.length && winner == null) {
                                    // If we don't have a winner repeat
                                    step = 0;
                                    // Increment auction round to reduce the price step by step
                                    auctionRound += 1;
                                }
                            }
                            else {
                                block();
                            }
                            break;
                        case 2:
                            // Send rejects
                            ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            for (int i = 0; i < rejected.length; i++) {
                                if (rejected[i] != null) {
                                    rejectMsg.addReceiver(rejected[i]);
                                }
                            }
                            agent.send(rejectMsg);
                            // Send accept
                            ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            acceptMsg.addReceiver(winner);
                            agent.send(acceptMsg);
                            break;
                    }
                }

                @Override
                public boolean done() {
                    return (step == 3);
                }
            });
        }
    }
    


    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
        }
    }

    // Load sample artifacts
    public Hashtable<String, Auction> testAuctions() {
        Hashtable<String, Auction> auctionsByName = new Hashtable<>();

        auctionsByName.put("Mona Lisa", new Auction(5000000, "Mona Lisa", 2000000, 20000));
        auctionsByName.put("Girl with a Pearl Earring", new Auction(500000, "Girl with a Pearl Earring", 100000, 5000));
        auctionsByName.put("The Starry Night", new Auction(2000000, "The Starry Night", 800000, 20000));
        auctionsByName.put("The Night Watch", new Auction(1500000, "The Night Watch", 1200000, 50000));
        auctionsByName.put("Sunflowers", new Auction(600000, "Sunflowers", 200000, 8000));

        return auctionsByName;
    }
}
