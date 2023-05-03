package agents;

import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import messages.Emergency;
import messages.EmergencyStatus;
import utils.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;

public class HubAgent extends BaseAgent {
    private ArrayList<String> notifiers;
    private HashMap<String, List<Capabilities>> devices;

    private boolean registered = false;

    private HashMap<String, Boolean> devicesConnected;

    private List<Emergency> emergencies;

    private final int warningDelay = 60000;
    private final int connectionStatusDelay = 10000;
    private final int connectionStatusPeriod = 60000;

    @Override
    public void setup() {
        super.setup();
        notifiers = new ArrayList<>();
        devices = new HashMap<>();
        devicesConnected = new HashMap<>();
        emergencies = new ArrayList<>();
        this.status = AgentStatus.LOGIN;
        timer.setInterval(new TimerTask() {
            @Override
            public void run() {
                checkDevices();
            }
        }, connectionStatusPeriod);
    }

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = idle();
            case WARNING -> status = warning();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    public AgentStatus login() {
        if (!registered) {
            this.DFAddMyServices(List.of("HUB"));
            registered = true;
        }

        ACLMessage notifierMsg = receiveMsg();
        if (notifierMsg != null) {
            if (notifierMsg.getPerformative() == ACLMessage.REQUEST && notifierMsg.getContent().equals("Hello")) {
                if (notifierMsg.getProtocol().equals(Protocols.NOTIFIER_LOGIN.toString())) {
                    notifiers.add(notifierMsg.getSender().getLocalName());
                    ACLMessage reply = notifierMsg.createReply();
                    reply.setContent("helloTelegram");
                    reply.setPerformative(ACLMessage.INFORM);
                    sendMsg(reply);
                    return AgentStatus.IDLE;
                }
            }

        }
        return AgentStatus.LOGIN;
    }

    public AgentStatus idle() {
        ACLMessage msg = receiveMsg();
        if (msg != null) {
            // get the protocol as an enum
            String sender = msg.getSender().getLocalName();
            Protocols p;
            try {
                p = Protocols.valueOf(msg.getProtocol());
            } catch (IllegalArgumentException e) {
                p = Protocols.NULL;
                logger.error("Not a valid protocol" + msg.getProtocol());
            }
            // is a login msg
            if (p == Protocols.CONTROLLER_LOGIN) {
                /*
                  First, we confirm the login, then we communicate the new device
                  to the telegram agent
                 */
                ControllerID controllerID;
                try {
                    controllerID = (ControllerID) msg.getContentObject();
                    ACLMessage reply = msg.createReply();
                    reply.setSender(getAID());
                    reply.setContent("hello, " + controllerID.getName());
                    reply.setPerformative(ACLMessage.INFORM);
                    sendMsg(reply);
                    devices.put(controllerID.getName(), controllerID.getCapabilities());
                    devicesConnected.put(controllerID.getName(), true);

                    // -------- Telegram
                    ACLMessage out = new ACLMessage();
                    out.setSender(getAID());
                    out.setProtocol(Protocols.CONTROLLER_LOGIN.toString());
                    out.setPerformative(ACLMessage.INFORM);
                    out.setContentObject(controllerID);
                    notifiers.forEach(notifier -> out.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                    sendMsg(out);

                } catch (UnreadableException | IOException e) {
                    logger.error("Error while deserializing");
                }
                return status;
            }
            // is a normal msg
            //  from notifier
            else if (notifiers.contains(sender)) {
                switch (p) {
                    case COMMAND -> {
                        try {
                            Command command = (Command) msg.getContentObject();
                            if (devices.containsKey(command.getTargetDevice())) {
                                ACLMessage order = new ACLMessage(ACLMessage.REQUEST);
                                order.setProtocol(Protocols.COMMAND.toString());
                                order.setSender(getAID());
                                order.addReceiver(new AID(command.getTargetDevice(), AID.ISLOCALNAME));
                                order.setContentObject(new Command(command.getOrder(), command.getObj(), command.getTargetChild(), ""));
                                sendMsg(order);
                            }

                        } catch (UnreadableException | IOException e) {
                            logger.error("Error while deserializing");
                        }
                    }
                }
            }
            // from some device
            else if (devices.containsKey(sender)) { // alarm system
                switch (p) {
                    case CONTROLLER_LOGOUT -> {
                        devices.remove(sender);
                        devicesConnected.remove(sender);
                        ACLMessage m = new ACLMessage();
                        m.setSender(getAID());
                        m.setProtocol(Protocols.CONTROLLER_LOGOUT.toString());
                        m.setPerformative(ACLMessage.INFORM);
                        m.setContent(sender);
                        notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                        sendMsg(m);
                    }
                    // not neccessary now
                    //case CHECK_CONNECTION -> {
                    //    if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    //        devicesConnected.put(sender, true);
                    //    }
                    //}
                    case WARNING -> {
                        Emergency em = null;
                        try {
                            em = (Emergency) msg.getContentObject();
                        } catch (UnreadableException e) {
                            logger.error("Error deserializing");
                        }
                        if (em != null) {
                            emergencies.add(em);
                        }
                        return AgentStatus.WARNING;
                    }
                    case COMMAND -> {
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            try {
                                Command command = (Command) msg.getContentObject();
                                ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                                m.setSender(getAID());
                                m.setProtocol(Protocols.COMMAND.toString());
                                m.setContentObject(command);
                                notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                                sendMsg(m);
                            } catch (UnreadableException | IOException e) {
                                logger.error("Error forwarding command");
                            }
                        }
                    }

                }

            }
        }
        return status;
    }

    public AgentStatus warning() {
        emergencies.forEach(emergency -> {
            if (emergency.getStatus() == EmergencyStatus.DISCOVERED) {
                try {
                    ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                    m.setContentObject(emergency);
                    m.setProtocol(Protocols.WARNING.toString());
                    notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                    sendMsg(m);
                    emergency.setStatus(EmergencyStatus.ALERTED);
                    timer.setTimeout(() -> emergency.setStatus(EmergencyStatus.DISCOVERED), warningDelay); // we try to remind the user again
                } catch (IOException e) {
                    logger.error("Error serializing");
                }
            }
        });

        ACLMessage response = receiveMsg();
        if (response != null)
            if (!(response.getProtocol().equals(Protocols.WARNING.toString()) && (response.getPerformative() == ACLMessage.INFORM || response.getPerformative() == ACLMessage.REQUEST))) {
                response = null;
            }

        if (response != null) {
            switch (response.getPerformative()) {
                case ACLMessage.INFORM -> {
                    try {
                        Emergency emReceived = (Emergency) response.getContentObject();
                        ACLMessage endWarning = new ACLMessage(ACLMessage.INFORM);
                        endWarning.setSender(getAID());
                        endWarning.addReceiver(emReceived.getOriginDevice());
                        endWarning.setContentObject(emReceived);
                        endWarning.setProtocol(Protocols.WARNING.toString());
                        sendMsg(endWarning);
                        Utils.RemoveEmergency(emergencies, emReceived.getMessage());
                    } catch (IOException e) {
                        logger.error("error serializing");
                    } catch (UnreadableException e) {
                        logger.error("error deserializing");
                    }

                }
                case ACLMessage.REQUEST -> {
                    try {
                        Emergency em = (Emergency) response.getContentObject();
                        if (em != null) {
                            emergencies.add(em);
                        }
                    } catch (UnreadableException e) {
                        logger.error("Error deserializing");
                    }
                }
            }
        }

        if (emergencies.size() == 0) {
            logger.info("Finished emergency");
            return AgentStatus.IDLE;
        }

        return status;
    }

    public AgentStatus logout() {
        return AgentStatus.END;
    }

    public void checkDevices() {
        devicesConnected.replaceAll((s, v) -> false);
        logger.info("Checking online devices");
        ACLMessage m = new ACLMessage();
        m.setPerformative(ACLMessage.QUERY_IF);
        m.setSender(getAID());
        m.setProtocol(Protocols.CHECK_CONNECTION.toString());
        devices.forEach((device, _capabilities) -> m.addReceiver(new AID(device, AID.ISLOCALNAME)));
        sendMsg(m);
        timer.setTimeout(this::checkConnectionStatus, connectionStatusDelay);

    }

    public void checkConnectionStatus() {
        List<String> offlineDevices = new ArrayList<>();
        List<Emergency> oldEmergencies = new ArrayList<>();
        devicesConnected.forEach((device, status) -> {
            if (!status) {
                logger.info(device + " has not confirmed");
                offlineDevices.add(device);
            }
        });
        offlineDevices.forEach(devices::remove);
        offlineDevices.forEach(devicesConnected::remove);

        offlineDevices.forEach(device -> {
            emergencies.forEach(emergency -> {
                if (emergency.getOriginDevice().getLocalName().equals(device)) {
                    oldEmergencies.add(emergency);
                }
            });
        });

        if (oldEmergencies.size() > 0) {
            emergencies.removeAll(oldEmergencies);
            logger.info("Se han borrado emergencias");
            emergencies.forEach(emergency -> System.out.println(emergency.getMessage()));
        }

        ACLMessage m = new ACLMessage();
        m.setPerformative(ACLMessage.INFORM);
        m.setSender(getAID());
        m.setProtocol(Protocols.CONTROLLER_DISCONNECT.toString());
        try {
            m.setContentObject((Serializable) offlineDevices);
        } catch (IOException e) {
            logger.error("Error while serializing");
        }
        notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
        sendMsg(m);
    }

    @Override
    public ACLMessage receiveMsg() {
        ACLMessage msg = super.receiveMsg();
        return confirmConnection(msg);
    }

    @Override
    public ACLMessage blockingReceiveMsg() {
        ACLMessage msg = super.blockingReceiveMsg();
        return confirmConnection(msg);
    }


    @Override
    public ACLMessage blockingReceiveMsg(int milis) {
        ACLMessage msg = super.blockingReceiveMsg(milis);
        return confirmConnection(msg);
    }


    private ACLMessage confirmConnection(ACLMessage msg) {
        if (msg != null) {
            logger.info("Protocol:" + msg.getProtocol() + " | Performative: " + ACLMessage.getPerformative(msg.getPerformative()));
            if (msg.getPerformative() == ACLMessage.CONFIRM && msg.getProtocol().equals(Protocols.CHECK_CONNECTION.toString())) {
                devicesConnected.put(msg.getSender().getLocalName(), true);
                logger.info(msg.getSender().getLocalName() + "confirmed");
                return null;
            }
        }
        return msg;
    }
}
