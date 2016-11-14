package kth.id2209.homework1.agent;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import kth.id2209.homework1.pojo.Artifact;
import jade.core.Agent;
import kth.id2209.homework1.pojo.Enums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created by tharidu on 11/9/16.
 */
public class CuratorAgent extends Agent {
    Hashtable<Long, Artifact> artifactHashtableById;

    protected void setup() {

        // Register in Directory Facilitator
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "curator"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello! Curator " + getAID().getName() + " is ready.");

        artifactHashtableById = new Hashtable<>();
        artifactHashtableById = testArtifacts();

        // Curator and profiler - receive artifact ids and reply back with artifact objects
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

                if (aclMessage != null) {
                    ACLMessage reply = aclMessage.createReply();
                    Long[] artifactIds = new Long[0];
                    ArrayList<Artifact> artifacts = new ArrayList<>();

                    try {
                        artifactIds = (Long[]) aclMessage.getContentObject();
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }

                    // Add the matching artifacts given the id
                    for (int i = 0; i < artifactIds.length; i++) {
                        if (artifactHashtableById.containsKey(artifactIds[i])) {
                            artifacts.add(artifactHashtableById.get(artifactIds[i]));
                        }
                    }

                    if (artifacts.isEmpty()) {
                        // Not available
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("not-available");
                    } else {
                        // Artifact found
                        reply.setPerformative(ACLMessage.INFORM_REF);
                        try {
                            reply.setContentObject(artifacts);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });

        // Curator and tour guide - receive user interests and reply back with matching artifact ids
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

                if (aclMessage != null) {
                    ACLMessage reply = aclMessage.createReply();
                    try {
                        Enums.interest[] interests = (Enums.interest[]) aclMessage.getContentObject();
                        ArrayList<Long> artifacts = new ArrayList<>();

                        // Add all matching artifacts
                        for (Enums.interest interest :
                                interests) {
                            artifacts.addAll(returnByInterests(interest));
                        }

                        Long[] artifactsIdArr = new Long[artifacts.size()];
                        reply.setContentObject(artifacts.toArray(artifactsIdArr));
                        reply.setPerformative(ACLMessage.AGREE);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
        }
    }

    // Load sample artifacts
    public Hashtable<Long, Artifact> testArtifacts() {
        Hashtable<Long, Artifact> artifactHashtableById = new Hashtable<>();

        artifactHashtableById.put(1L, new Artifact(1, "Mona Lisa", "Leonardo da Vinci", 1505, "Italy", "portrait",
                new Enums.interest[]{Enums.interest.portrait, Enums.interest.woman}));
        artifactHashtableById.put(2L, new Artifact(2, "Girl with a Pearl Earring", "Jan Vermeer", 1665, "Netherlands", "portrait",
                new Enums.interest[]{Enums.interest.portrait, Enums.interest.woman}));
        artifactHashtableById.put(3L, new Artifact(3, "The Starry Night", "Vincent van Gough", 1889, "France", "landscape art",
                new Enums.interest[]{Enums.interest.landscape}));
        artifactHashtableById.put(4L, new Artifact(4, "The Night Watch", "Rembrandt", 1642, "Netherlands", "portrait",
                new Enums.interest[]{Enums.interest.portrait}));
        artifactHashtableById.put(4L, new Artifact(5, "Sunflowers", "Vincent van Gough", 1888, "Netherlands", "still life",
                new Enums.interest[]{Enums.interest.still_life, Enums.interest.flower}));

        return artifactHashtableById;
    }

    // Helper method for matching interests with artifacts
    public ArrayList<Long> returnByInterests(Enums.interest interest) {
        ArrayList<Long> matchingArtifacts = new ArrayList<>();
        Iterator<Long> iterator = artifactHashtableById.keySet().iterator();
        Long key;
        Artifact artifact;

        while (iterator.hasNext()) {
            key = iterator.next();
            artifact = artifactHashtableById.get(key);
            if (artifact.matchCategory(interest)) {
                matchingArtifacts.add(artifact.getId());
            }
        }

        return matchingArtifacts;
    }
}
