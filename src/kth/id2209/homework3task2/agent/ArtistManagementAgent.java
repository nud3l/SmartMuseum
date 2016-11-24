package kth.id2209.homework3task2.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import kth.id2209.homework1.agent.Utilities;
import kth.id2209.homework2.pojo.Auction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nud3l on 11/21/16.
 * Auctioneer
 */
public class ArtistManagementAgent extends MobileAgent {
    private static final int TIMEOUT = 5000;
    private AID[] curators;
    private AID[] auctioneers;
    public String container;
    private int item = 2;
    private int finalPrice;


    Hashtable<Integer, Auction> auctionsbyID;

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
            String serviceName = "auctioneer" + container;
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), serviceName));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    protected void sendBestPrice() {
        // find the other auctioneers
        auctioneers = Utilities.searchDF(this, "auctioneer" + container);
        // Send results from auction
        if (auctioneers.length != 0) {
            addBehaviour(new SendBestPrice(this));
        }
    }

    @Override
    protected void setup() {
        super.setup();

        registerDF();

        System.out.println("Hello! Artist Manager " + getAID().getName() + " is ready.");

        // Load test auctions
        auctionsbyID = new Hashtable<>();
        auctionsbyID = testAuctions();


        // find the other auctioneers
        auctioneers = Utilities.searchDF(this, "auctioneer" + container);

        // Handle results from auction
        if (auctioneers.length != 0) {
            addBehaviour(new BestPrice(this));
        }

        finalPrice = auctionsbyID.get(item).getInitialPrice();
    }

    @Override
    void init() {
        super.init();

        // find the bidders
        curators = Utilities.searchDF(this, "bidder" + container);

        // If invalid arguments or agents, exit with error
        //if (curators.length == 0)
        //    System.exit(1);

        // Handle auctions
        if (curators.length != 0) {
            addBehaviour(new ArtistManagerInteractionWake(this, TIMEOUT));
        }

        // find the other auctioneers
        auctioneers = Utilities.searchDF(this, "auctioneer" + container);

        // Handle results from auction
        if (auctioneers.length != 0) {
            addBehaviour(new BestPrice(this));
        }

    }

    @Override
    protected void beforeMove() {
        deregisterDF();
        super.beforeMove();
    }

    @Override
    protected void afterMove() {
        super.afterMove();
        registerDF();
        init();
        sendBestPrice();
    }

    @Override
    protected void beforeClone() {
        super.beforeClone();
    }

    @Override
    protected void afterClone() {
        super.afterClone();
    }

    // Negotiate best price
    private static class BestPrice extends CyclicBehaviour {
        ArtistManagementAgent agent;
        private ArrayList<AID> receivedPrice;
        private int repliesCount = 0;

        BestPrice(ArtistManagementAgent agent) {
            this.agent = agent;
        }

        @Override
        public void action() {
            MessageTemplate InformTemplate = MessageTemplate.MatchPerformative(ACLMessage.INFORM_IF);
            ACLMessage priceInfo = agent.receive(InformTemplate);
            if (priceInfo != null) {
                AID sender = priceInfo.getSender();
                if (!receivedPrice.contains(sender)) {
                    receivedPrice.add(sender);
                    repliesCount++;
                    int agentPrice = Integer.parseInt(priceInfo.getContent());
                    if ((agentPrice != 0) && (agentPrice < agent.finalPrice)) {
                        agent.finalPrice = agentPrice;
                    }
                }

                if ((repliesCount >= 2) && (agent.finalPrice != 0)) {
                    if (agent.finalPrice != agent.auctionsbyID.get(agent.item).getInitialPrice()) {
                        System.out.println("ARTIST MANAGER We sold the artifact for " + agent.finalPrice);
                    } else {
                        System.out.println("ARTIST MANAGER We didn't sell the artifact");
                    }
                }
            } else {
                // Update list of auctioneers
                agent.auctioneers = Utilities.searchDF(agent, "auctioneer" + agent.container);
                block();
            }
        }
    }

    // Send best price
    private class SendBestPrice extends OneShotBehaviour {
        ArtistManagementAgent agent;

        SendBestPrice(ArtistManagementAgent agent) {
            this.agent = agent;
        }

        @Override
        public void action() {
            System.out.println("Artist Manager send INFORM FINAL PRICE " + agent.finalPrice);
            ACLMessage aclMessage = Utilities.createAclMessage(
                    ACLMessage.INFORM_IF,
                    agent.auctioneers,
                    (Integer.toString(agent.finalPrice))
            );
            agent.send(aclMessage);
        }
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

            Auction artifact = agent.auctionsbyID.get(agent.item);

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
                private int rejectCount = 0;
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
                                    if ((proposal.getContent().equals("YES")) && (winner == null)) {
                                        System.out.println("Artist Manager got a WINNER " + artifact.getArtworkName());
                                        winner = proposal.getSender();
                                        agent.finalPrice = currentPrice;
                                    }
                                    // Late bidders will be rejected
                                    if ((proposal.getContent().equals("YES")) && (winner != null)) {
                                        AID late = proposal.getSender();
                                        System.out.println("Artist Manager got LOSERS " + artifact.getArtworkName());
                                        if (late != winner){
                                            rejected[rejectCount] = late;
                                            rejectCount += 1;
                                        }
                                    }
                                }
                                // System.out.println("Artist Manager REPLIES COUNT " + repliesCount);
                                // System.out.println("Artist Manager CURATORS " + agent.curators.length);
                                if ((repliesCount >= agent.curators.length) && (winner != null)) {
                                    step = 2;
                                } else if ((repliesCount >= agent.curators.length) && (winner == null)) {
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
                            // Send accept
                            ACLMessage acceptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            acceptMsg.addReceiver(winner);
                            System.out.println("Artist Manager ACCEPT_PROPOSAL " + artifact.getArtworkName());
                            agent.send(acceptMsg);
                            // Send rejects
                            ACLMessage rejectMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            for (int i = 0; i < rejected.length; i++) {
                                if (rejected[i] != null) {
                                    rejectMsg.addReceiver(rejected[i]);
                                    System.out.println("Artist Manager REJECT_PROPOSAL " + artifact.getArtworkName());
                                }
                            }
                            agent.send(rejectMsg);
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
