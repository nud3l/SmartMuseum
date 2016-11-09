package kth.id2209.homework1.agent;

import kth.id2209.homework1.behaviour.CuratorDetailBehaviour;
import kth.id2209.homework1.pojo.Artifact;
import jade.core.Agent;

import java.util.Hashtable;

/**
 * Created by tharidu on 11/9/16.
 */
public class CuratorAgent extends Agent {
    Hashtable<Long, Artifact> artifactHashtableById;
    Hashtable<String, Artifact[]> artifactHashtableByInterests;

    protected void setup() {
        artifactHashtableById = new Hashtable<>();
        artifactHashtableByInterests = new Hashtable<>();
        loadHashtables();

        addBehaviour(new CuratorDetailBehaviour());
    }

    protected void takeDown() {

    }

    private void loadHashtables() {
        // TODO
    }
}
