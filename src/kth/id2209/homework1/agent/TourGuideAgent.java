package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import kth.id2209.homework1.behaviour.TourGuideGetArtifactsBehaviour;
import kth.id2209.homework1.behaviour.TourGuideProfilerRequestBehaviour;
import kth.id2209.homework1.behaviour.TourGuideVirtualTourBuilderBehaviour;
import kth.id2209.homework1.pojo.Artifact;
import kth.id2209.homework1.pojo.Enums;

import java.util.Hashtable;

/**
 * Created by tharidu on 11/9/16.
 */
public class TourGuideAgent extends Agent {
    //Hashtable<Long, Interests[]> userInterests;
    //Hashtable<String, Artifact[]> virtualTour;
    private AID[] curator;

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

        //userInterests = new Hashtable<>();
        //virtualTour = new Hashtable<>();

        System.out.println("Hello! Tour Guide "+getAID().getName()+" is ready.");

        // Listen for requests from profiler
        addBehaviour(new TourGuideProfilerRequestBehaviour());

        // Build virtual tour for profiler
        SequentialBehaviour virtualTourBuilder = new SequentialBehaviour();
        virtualTourBuilder.addSubBehaviour(new TourGuideGetArtifactsBehaviour());
        virtualTourBuilder.addSubBehaviour(new TourGuideVirtualTourBuilderBehaviour());
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
