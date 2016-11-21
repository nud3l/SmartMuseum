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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nud3l on 11/21/16.
 * Auctioneer
 */
public class ArtistManagementAgent extends Agent {
    private static final int TIMEOUT = 5000;
    private AID[] curators;

    Hashtable<Integer, Auction> auctionsbyID;

    protected void setup() {

        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "auctioneer"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Artist Manager " + getAID().getName() + " is ready.");

        // Load test auctions
        auctionsbyID = new Hashtable<>();
        auctionsbyID = testAuctions();

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
            // Get random item
            int item = ThreadLocalRandom.current().nextInt(1, 5 + 1);
            Auction artifact = agent.auctionsbyID.get(item);

            // ACLMessage INFORM
            addSubBehaviour(new OneShotBehaviour() {
                @Override
                public void action() {
                    System.out.println("Artist Manager send INFORM START " + artifact.getArtworkName());
                    ACLMessage aclMessage = Utilities.createAclMessage(
                            ACLMessage.INFORM,
                            agent.curators,
                            ("START " + artifact.getArtworkName())
                    );
                    agent.send(aclMessage);
                }
            });


            addSubBehaviour(new Behaviour() {
                private AID winner = null;
                private AID[] rejected = new AID[agent.curators.length - 1];
                private int currentPrice;
                private int step = 0;
                private int auctionRound = 0;
                private int repliesCount = 0;
                @Override
                public void action() {
                    switch(step) {
                        case 0:
                            // System.out.println("Artist Manager CASE 0 " + artifact.getArtworkName());
                            // CFP with current price
                            currentPrice = artifact.getInitialPrice()
                                    - (artifact.getReductionRate() * auctionRound);
                            if (currentPrice > artifact.getReservePrice()) {
                                ACLMessage cfp = Utilities.createAclMessage(ACLMessage.CFP, agent.curators, Integer.toString(currentPrice));
                                cfp.setReplyWith("cfp" + System.currentTimeMillis());
                                agent.send(cfp);
                                System.out.println("Artist Manager send CFP " + artifact.getArtworkName());
                                messageTemplate = MessageTemplate.MatchInReplyTo(cfp.getReplyWith());
                                step = 1;
                            } else {
                                ACLMessage aclMessage = Utilities.createAclMessage(
                                        ACLMessage.INFORM,
                                        agent.curators,
                                        ("NO BIDS " + artifact.getArtworkName())
                                );
                                System.out.println("Artist Manager send INFORM NO BIDS " + artifact.getArtworkName());
                                agent.send(aclMessage);
                                step = 3;

                            }
                            // reset repliesCount
                            repliesCount = 0;
                            break;

                        case 1:
                            // System.out.println("Artist Manager CASE 1 " + artifact.getArtworkName());
                            ACLMessage proposal = agent.receive(messageTemplate);
                            if (proposal != null) {
                                repliesCount += 1;
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
                                }
                                if (proposal.getPerformative() == ACLMessage.PROPOSE) {
                                    // First one to bid is the winner
                                    if (proposal.getContent().equals("YES") && winner == null) {
                                        System.out.println("Artist Manager got a WINNER " + artifact.getArtworkName());
                                        winner = proposal.getSender();
                                    }
                                    // Late bidders will be rejected
                                    if (proposal.getContent().equals("YES") && winner != null) {
                                        AID late = proposal.getSender();
                                        System.out.println("Artist Manager got LOSERS " + artifact.getArtworkName());
                                        for (int i = 0; i < rejected.length; i++) {
                                            if (late != rejected[i]) {
                                                rejected[i] = late;
                                            }
                                        }
                                    }
                                }
                                // System.out.println("Artist Manager REPLIES COUNT " + repliesCount);
                                // System.out.println("Artist Manager CURATORS " + agent.curators.length);
                                if (repliesCount >= agent.curators.length && winner != null) {
                                    step = 2;
                                } else if (repliesCount >= agent.curators.length && winner == null) {
                                    // If we don't have a winner repeat
                                    System.out.println("Artist Manager NEXT ROUND " + auctionRound);
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
                            // System.out.println("Artist Manager CASE 2 " + artifact.getArtworkName());
                            // Send rejects
                            ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            for (int i = 0; i < rejected.length; i++) {
                                if (rejected[i] != null) {
                                    rejectMsg.addReceiver(rejected[i]);
                                }
                            }
                            System.out.println("Artist Manager ACCEPT_PROPOSAL " + artifact.getArtworkName());
                            agent.send(rejectMsg);
                            // Send accept
                            ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            acceptMsg.addReceiver(winner);
                            System.out.println("Artist Manager REJECT_PROPOSAL " + artifact.getArtworkName());
                            agent.send(acceptMsg);
                            step = 3;
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
    public Hashtable<Integer, Auction> testAuctions() {
        Hashtable<Integer, Auction> auctionsByName = new Hashtable<>();

        auctionsByName.put(1, new Auction(5000000, "Mona Lisa", 2000000, 200000));
        auctionsByName.put(2, new Auction(500000, "Girl with a Pearl Earring", 100000, 50000));
        auctionsByName.put(3, new Auction(2000000, "The Starry Night", 800000, 200000));
        auctionsByName.put(4, new Auction(1500000, "The Night Watch", 1200000, 100000));
        auctionsByName.put(5, new Auction(600000, "Sunflowers", 200000, 80000));

        return auctionsByName;
    }
}
