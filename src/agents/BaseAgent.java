package agents;

import jade.core.Agent;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import utils.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Base Class for our agents. It's heavily inspired by Luis Castillo's LARVABaseAgent
 * but without any of the extra functionality needed for LARVA since this project does
 * not use it.
 *
 * It acts as a wrapper of a Jade Agent
 */

public class BaseAgent extends Agent {
    protected Logger logger;
    protected final long WAITANSWERMS = 5000;
    protected ACLMessage inbox, outbox;

    protected long ncycles;

    @Override
    public void setup(){
        super.setup();
        logger = new Logger();
        logger.setAgentName(getLocalName());
    }

    public void execute(){}

    public void preExecute(){}

    public void postExecute(){}

    public ArrayList<String> DFGetProviderList() {
        return DFGetAllProvidersOf("");
    }

    public ArrayList<String> DFGetServiceList(){
        return DFGetAllServicesProvidedBy("");
    }

    public ArrayList<String> DFGetAllProvidersOf(String service){
        ArrayList<String> res = new ArrayList<>();
        DFAgentDescription[] providers;
        providers = this.DFQueryAllProviders(service);
        if (providers != null && providers.length > 0){
            for (DFAgentDescription list : providers){
                if (!res.contains(list.getName().getLocalName())){
                    res.add(list.getName().getLocalName());
                }
            }
        }
        return res;
    }

    public ArrayList<String> DFGetAllServicesProvidedBy(String agentName) {
        ArrayList<String> res = new ArrayList<>();
        DFAgentDescription[] services;
        services = this.DFQueryAllServicesProvided(agentName);
        if (services != null && services.length > 0){
            for (DFAgentDescription dfAgentDescription : services){
                Iterator sdi = dfAgentDescription.getAllServices();
                while (sdi.hasNext()){
                    ServiceDescription sd = (ServiceDescription) sdi.next();
                    if (!res.contains(sd.getType())){
                        res.add(sd.getType());
                    }
                }
            }

        }
        return res;
    }

    public boolean DFHasSErvice(String agentName, String service){
        return DFGetAllProvidersOf(service).contains(service);
    }

    public boolean DFSetMyServices(String [] services){

    }
}
