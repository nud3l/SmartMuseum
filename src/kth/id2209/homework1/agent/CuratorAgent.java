package kth.id2209.homework1.agent;

import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
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
    Hashtable<Integer, Artifact> artifactHashtableById;
//    Hashtable<String, Artifact[]> artifactHashtableByInterests;

    protected void setup() {
        try {
            DFService.register(this, Utilities.buildDFAgent(this.getAID(), getLocalName(), "curator"));
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        artifactHashtableById = new Hashtable<>();
//        artifactHashtableByInterests = new Hashtable<>();
        artifactHashtableById = testArtifacts();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {
                    ACLMessage reply = aclMessage.createReply();

                    if (artifactHashtableById.containsKey(Integer.parseInt(aclMessage.getContent()))) {
                        // Artifact found
                        reply.setPerformative(ACLMessage.INFORM_REF);
                        try {
                            reply.setContentObject(artifactHashtableById.get(Integer.parseInt(aclMessage.getContent())));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Not available
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("not-available");
                    }

                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage aclMessage = myAgent.receive();

                if (aclMessage != null) {
                    ACLMessage reply = aclMessage.createReply();
                    try {
                        Enums.interest[] interests = (Enums.interest[]) aclMessage.getContentObject();
                        ArrayList<Artifact> artifacts = new ArrayList<>();

                        for (Enums.interest interest :
                                interests) {
                            artifacts.addAll(returnByInterests(interest));
                        }

                        Artifact[] artifactsArr = new Artifact[artifacts.size()];
                        reply.setContentObject(artifacts.toArray(artifactsArr));
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

    public Hashtable<Integer, Artifact> testArtifacts() {
        Hashtable<Integer, Artifact> artifactHashtableById = new Hashtable<>();

        artifactHashtableById.put(1, new Artifact(1, "Mona Lisa", "Leonardo da Vinci", 1505, "Italy", "portrait",
                new Enums.interest[]{Enums.interest.portrait, Enums.interest.woman}));
        artifactHashtableById.put(2, new Artifact(2, "Girl with a Pearl Earring", "Jan Vermeer", 1665, "Netherlands", "portrait",
                new Enums.interest[]{Enums.interest.portrait, Enums.interest.woman}));
        artifactHashtableById.put(3, new Artifact(3, "The Starry Night", "Vincent van Gough", 1889, "France", "landscape art",
                new Enums.interest[]{Enums.interest.landscape}));
        artifactHashtableById.put(4, new Artifact(4, "The Night Watch", "Rembrandt", 1642, "Netherlands", "portrait",
                new Enums.interest[]{Enums.interest.portrait}));
        artifactHashtableById.put(4, new Artifact(5, "Sunflowers", "Vincent van Gough", 1888, "Netherlands", "still life",
                new Enums.interest[]{Enums.interest.still_life, Enums.interest.flower}));

        return artifactHashtableById;
    }

    public ArrayList<Artifact> returnByInterests(Enums.interest interest) {
        ArrayList<Artifact> matchingArtifacts = new ArrayList<>();
        Iterator<Integer> iterator = artifactHashtableById.keySet().iterator();
        Integer key;
        Artifact artifact;

        while (iterator.hasNext()) {
            key = iterator.next();
            artifact = artifactHashtableById.get(key);
            if (artifact.matchCategory(interest)) {
                matchingArtifacts.add(artifact);
            }
        }
        return matchingArtifacts;
    }
}
