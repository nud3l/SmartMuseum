package kth.id2209.homework3task2.agent;

import java.util.*;
import java.io.*;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.content.*;
import jade.content.lang.*;
import jade.content.lang.sl.*;
import jade.content.onto.basic.*;
import jade.domain.*;
import jade.domain.mobility.*;
import jade.domain.JADEAgentManagement.*;
import jade.gui.*;

/**
 * Created by nud3l on 11/24/16.
 * Adjusted code based on https://www.iro.umontreal.ca/~vaucher/Agents/Jade/Mobility.html
 */

public class MobileAgent extends GuiAgent {
    private AID controller;
    private Location destination;
    transient protected MobileAgentGui myGui;

    public Location getDestination() {
        return destination;
    }

    protected void setup() {
        // Retrieve arguments passed during this agent creation
        Object[] args = getArguments();
        controller = (AID) args[0];
        destination = here();

        init();

        // Program the main behaviour of this agent
        addBehaviour(new ReceiveCommands(this));
    }

    void init() {
        // Register language and ontology
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());

        // Create and display the gui
        myGui = new MobileAgentGui(this);
        myGui.setVisible(true);
        myGui.setLocation(destination.getName());
    }

    protected void onGuiEvent(GuiEvent e) {
        //No interaction with the gui
    }

    protected void beforeMove() {
        System.out.println("Moving now to location : " + destination.getName());
        myGui.setVisible(false);
        myGui.dispose();
    }

    protected void afterMove() {
        init();
        myGui.setInfo("Arrived at location : " + destination.getName());
    }

    protected void beforeClone() {
        myGui.setInfo("Cloning myself to location : " + destination.getName());
    }

    protected void afterClone() {
        init();
    }


    /*
    * Receive all commands from the controller agent
    */
    class ReceiveCommands extends CyclicBehaviour {
        ReceiveCommands(Agent a) { super(a); }

        public void action() {


            ACLMessage msg = receive(MessageTemplate.MatchSender(controller));

            if (msg == null) { block(); return; }

            if (msg.getPerformative() == ACLMessage.REQUEST){

                try {
                    ContentElement content = getContentManager().extractContent(msg);
                    Concept concept = ((Action)content).getAction();
                    System.out.println(concept);

                    if (concept instanceof CloneAction){

                        CloneAction ca = (CloneAction)concept;
                        String newName = ca.getNewName();
                        Location l = ca.getMobileAgentDescription().getDestination();
                        if (l != null) destination = l;
                        doClone(destination, newName);
                    }
                    else if (concept instanceof MoveAction){

                        MoveAction ma = (MoveAction)concept;
                        Location l = ma.getMobileAgentDescription().getDestination();
                        System.out.println("I am moving to " + l.getName());

                        if (l != null) doMove(destination = l);
                    }
                    else if (concept instanceof KillAgent){

                        myGui.setVisible(false);
                        myGui.dispose();
                        doDelete();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            else { System.out.println("Unexpected msg from controller agent"); }
        }
    }

}