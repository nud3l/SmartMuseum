package kth.id2209.homework1.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by tharidu on 11/10/16.
 */
public class Utilities {
    public static DFAgentDescription buildDFAgent(AID aid, String localName, String type) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(aid);
        ServiceDescription sd = new ServiceDescription();
        sd.setType(type);
        sd.setName(localName);
        dfd.addServices(sd);

        return dfd;
    }

    public static AID getService(Agent agent, String service) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);
        dfd.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(agent, dfd);
            if (result.length > 0)
                return result[0].getName();
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        return null;
    }

    public static AID[] searchDF(Agent agent, String service) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(service);
        dfd.addServices(sd);

        SearchConstraints ALL = new SearchConstraints();
        ALL.setMaxResults(new Long(-1));

        try {
            DFAgentDescription[] result = DFService.search(agent, dfd, ALL);
            AID[] agents = new AID[result.length];
            for (int i = 0; i < result.length; i++)
                agents[i] = result[i].getName();
            return agents;

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        return null;
    }

    public static ACLMessage createAclMessage(int performative, AID[] receivers, String ontology, String content) {
        ACLMessage aclMessage = new ACLMessage(performative);
        aclMessage.setContent(content);

        for (AID receiver : receivers) {
            aclMessage.addReceiver(receiver);
        }

        aclMessage.setOntology(ontology);
        return aclMessage;
    }

    public static ACLMessage createAclMessage(int performative, AID[] receivers, String ontology, Serializable contentObject) throws IOException {
        ACLMessage aclMessage = new ACLMessage(performative);
        aclMessage.setContentObject(contentObject);

        for (AID receiver : receivers) {
            aclMessage.addReceiver(receiver);
        }

        aclMessage.setOntology(ontology);
        return aclMessage;
    }
}
