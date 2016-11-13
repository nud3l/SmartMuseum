package kth.id2209.homework1.behaviour;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import kth.id2209.homework1.agent.TourGuideAgent;

/**
 * Created by nud3l on 11/9/16.
 */
public class TourGuideGetArtifactsBehaviour extends OneShotBehaviour{
    @Override
    public void action() {
        // Receive only profiler accept messages
        MessageTemplate profilerAcceptTemplate = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
        ACLMessage profilerAccept = myAgent.receive(profilerAcceptTemplate);

        // Update the curator AID
        DFAgentDescription curatorTemplate = new DFAgentDescription();
        ServiceDescription curatorService = new ServiceDescription();
        curatorService.setType("curator");
        curatorTemplate.addServices(curatorService);
        try {
            DFAgentDescription[] result = DFService.search(myAgent, curatorTemplate);
            System.out.println("Found the following curator agent:");
            curator = new AID(result);
            for (int i = 0; i < result.length; ++i) {
                curator[i] = result[i].getName();
                System.out.println(curator[i].getName());
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }


        if (profilerAccept != null) {
            // Store request information and accept request
            String interestString = profilerAccept.getContent();
            System.out.println("Got accept from profiler");

            // Request artifacts from curator
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(curator);
            request.setContent(interestString);

            myAgent.send(request);

        } else {
            block();
        }

    }
}
