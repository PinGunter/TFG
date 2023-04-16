package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.MicroRuntime;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import utils.Logger;
import utils.Timeout;
import utils.Utils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;


/**
 * Base Class for our agents. It's heavily inspired by Luis Castillo's LARVABaseAgent
 * but without any of the extra functionality needed for LARVA since this project does
 * not use it.
 * <p>
 * It acts as a wrapper of a Jade Agent
 */

public class BaseAgent extends Agent {
    protected AgentStatus status;
    protected Logger logger;
    protected Timeout timer;

    protected final long WAITANSWERMS = 5000;

    public Behaviour defaultBehaviour;

    protected ACLMessage inbox, outbox;

    protected long ncycles;

    protected boolean exit = false;

    protected boolean isRequesting = false;

    protected String cryptKey;

    @Override
    public void setup() {
        super.setup();
        timer = new Timeout();
        logger = new Logger();
        logger.setAgentName(getLocalName());
        this.setDefaultBehaviour();
        cryptKey = (String) getArguments()[0];
    }

    @Override
    public void takeDown() {
        super.takeDown();
    }

    private void setDefaultBehaviour() {
        defaultBehaviour = new Behaviour() {
            @Override
            public void action() {
                shield(() -> {
                    preExecute();
                    execute();
                    postExecute();
                    ncycles++;
                });
                if (exit) {
                    doDelete();
                }
            }

            @Override
            public boolean done() {
                return exit;
            }
        };
        this.addBehaviour(defaultBehaviour);
    }

    private void shield(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            logger.error(e.getMessage());
            exit = true;
        }
    }

    public void execute() {
    }

    public void preExecute() {
    }

    public void postExecute() {
    }

    public ArrayList<String> DFGetProviderList() {
        return DFGetAllProvidersOf("");
    }

    public ArrayList<String> DFGetServiceList() {
        return DFGetAllServicesProvidedBy("");
    }

    public ArrayList<String> DFGetAllProvidersOf(String service) {
        ArrayList<String> res = new ArrayList<>();
        DFAgentDescription[] providers;
        providers = this.DFQueryAllProviders(service);
        if (providers != null && providers.length > 0) {
            for (DFAgentDescription list : providers) {
                if (!res.contains(list.getName().getLocalName())) {
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
        if (services != null && services.length > 0) {
            for (DFAgentDescription dfAgentDescription : services) {
                Iterator sdi = dfAgentDescription.getAllServices();
                while (sdi.hasNext()) {
                    ServiceDescription sd = (ServiceDescription) sdi.next();
                    if (!res.contains(sd.getType())) {
                        res.add(sd.getType());
                    }
                }
            }

        }
        return res;
    }

    public boolean DFHasService(String agentName, String service) {
        return DFGetAllProvidersOf(service).contains(service);
    }

    public boolean DFSetMyServices(List<String> services) {
        logger.info("Services registered" + services.toString());
        if (this.DFGetAllServicesProvidedBy(getLocalName()).size() > 0) {
            DFRemoveAllMyServices();
        }
        return DFSetServices(getLocalName(), services);
    }

    public boolean DFAddMyServices(List<String> services) {
        List<String> previous;
        logger.info("Removing services" + services.toString());
        previous = DFGetAllServicesProvidedBy(getLocalName());
        previous.addAll(services);
        return DFSetMyServices(previous);
    }

    public boolean DFRemoveMyServices(List<String> services) {
        List<String> previous;
        logger.info("Removing services" + services.toString());
        previous = DFGetAllServicesProvidedBy(getLocalName());
        previous.removeAll(services);
        return DFSetMyServices(previous);
    }

    public void DFRemoveAllMyServices() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            logger.error(e.getMessage());
        }
    }

    private boolean DFSetServices(String agentName, List<String> services) {
        DFAgentDescription dfAgentDescription;
        ServiceDescription serviceDescription;
        boolean okay = false;

        dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(new AID(agentName, AID.ISLOCALNAME));

        for (String s : services) {
            serviceDescription = new ServiceDescription();
            serviceDescription.setName(s);
            serviceDescription.setType(s);
            dfAgentDescription.addServices(serviceDescription);
        }
        try {
            DFService.register(this, dfAgentDescription);
            okay = true;
        } catch (FIPAException e) {
            logger.error(e.getMessage());
        }
        return okay;
    }

    private DFAgentDescription[] DFQueryAllServicesProvided(String agentName) {
        DFAgentDescription dfAgentDescription;
        ServiceDescription serviceDescription;
        DFAgentDescription services[] = new DFAgentDescription[0];

        dfAgentDescription = new DFAgentDescription();
        if (!agentName.equals("")) {
            dfAgentDescription.setName(new AID(agentName, AID.ISLOCALNAME));
        }
        serviceDescription = new ServiceDescription();
        dfAgentDescription.addServices(serviceDescription);
        SearchConstraints constraints = new SearchConstraints();
        constraints.setMaxResults((long) -1);
        try {
            services = DFService.search(this, dfAgentDescription, constraints);
        } catch (FIPAException e) {
            logger.error(e.getMessage());
        }
        return services;
    }

    private DFAgentDescription[] DFQueryAllProviders(String service) {
        DFAgentDescription dfAgentDescription;
        ServiceDescription serviceDescription;
        DFAgentDescription[] agents = new DFAgentDescription[0];
        dfAgentDescription = new DFAgentDescription();
        SearchConstraints constraints = new SearchConstraints();
        constraints.setMaxResults((long) -1);
        serviceDescription = new ServiceDescription();
        if (!service.equals("")) {
            serviceDescription.setName(service);
        }
        dfAgentDescription.addServices(serviceDescription);
        try {
            agents = DFService.search(this, dfAgentDescription, constraints);
        } catch (FIPAException e) {
            logger.error(e.getMessage());
        }

        return agents;
    }

    protected void logError(String msg) {
        logger.error(msg);
    }

    protected void logInfo(String msg) {
        logger.info(msg);
    }

    protected String inputLine(String message) {
        System.out.println("\n\n" + message + " ");
        return new Scanner(System.in).nextLine();
    }

    protected boolean confirm(String message) {
        String line = inputLine(message);
        return line.length() == 0 || line.toUpperCase().charAt(0) == 'Y';
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public void sendMsg(ACLMessage msg) {
        if (msg.getByteSequenceContent() != null) {
            try {
                byte[] encrypted = Utils.EncryptObj(msg.getByteSequenceContent(), cryptKey);
                msg.setByteSequenceContent(encrypted);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IOException |
                     IllegalBlockSizeException | BadPaddingException e) {
                logger.error("Error while encrypting");
            }
        }
        logger.message(prettyPrint(msg));
        send(msg);
    }

    public ACLMessage receiveMsg() {
        ACLMessage msg = receive();
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    public ACLMessage receiveMsg(MessageTemplate template) {
        ACLMessage msg = receive(template);
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    public ACLMessage blockingReceiveMsg() {
        ACLMessage msg = blockingReceive();
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    public ACLMessage blockingReceiveMsg(int milis) {
        ACLMessage msg = blockingReceive(milis);
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    public ACLMessage blockingReceiveMsg(MessageTemplate template) {
        ACLMessage msg = blockingReceive(template);
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    public ACLMessage blockingReceiveMsg(MessageTemplate template, int milis) {
        ACLMessage msg = blockingReceive(template, milis);
        if (msg != null) {
            if (msg.getByteSequenceContent() != null) {
                try {
                    byte[] decrypted = Utils.DecryptObj(msg.getByteSequenceContent(), cryptKey);
                    msg.setByteSequenceContent(decrypted);
                } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException |
                         IllegalBlockSizeException | BadPaddingException e) {
                    logger.error("Error while decrypting");
                }
            }
            logger.message(prettyPrint(msg));
        }
        return msg;
    }

    protected String prettyPrint(ACLMessage msg) {
        boolean hasSender = msg.getSender() != null;
        Iterator<AID> receivers = msg.getAllReceiver();
        String res = " ";
        res += (hasSender ? msg.getSender().getLocalName() : "") + " ---" + ACLMessage.getPerformative(msg.getPerformative()) + "--> [";
        while (receivers.hasNext()) {
            res += receivers.next().getLocalName() + (receivers.hasNext() ? "," : "");
        }
        res += "] - {" + msg.getContent() + "}";
        res += " @" + msg.getProtocol() + " || RW:" + msg.getReplyWith() + " || IRT:" + msg.getInReplyTo();
        return res;
    }

    public synchronized void launchSubAgent(String name, Class c, Object[] args) {
        try {
            File settings = new File("data/settings/micro.txt");
            Scanner scanner = new Scanner(settings);
            String isMicroString = "";
            while (scanner.hasNext()) {
                isMicroString = scanner.nextLine();
            }
            scanner.close();

            if (isMicroString.equals("true")) {
                MicroRuntime.startAgent(name, c.getName(), args);
            } else if (isMicroString.equals("false")) {
                AgentController ag = getContainerController().createNewAgent(name, c.getName(), args);
                ag.start();
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
