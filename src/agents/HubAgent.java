package agents;

import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import utils.Timeout;

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

    private Timeout timer;


    private HashMap<String, Boolean> devicesConnected;

    private HashMap<String, AID> emergencies; // key-value with the emergency and the aid of the device that sent it
    private HashMap<String, Boolean> emergencyStatus; // stores the state of the emergency (false: hasn't been passed to the user | true : it's been passed)

    private final int warningDelay = 60000;
    private final int connectionStatusDelay = 10000;
    private final int connectionStatusPeriod = 60000;

    @Override
    public void setup() {
        super.setup();
        notifiers = new ArrayList<>();
        devices = new HashMap<>();
        devicesConnected = new HashMap<>();
        emergencies = new HashMap<>();
        emergencyStatus = new HashMap<>();
        this.status = AgentStatus.LOGIN;
        timer = new Timeout();
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
                /**
                 * First, we confirm the login, then we communicate the new device
                 * to the telegram agent
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
                                order.setContentObject(new Command(command.getOrder(), command.getTargetChild(), ""));
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
                    case CHECK_CONNECTION -> {
                        if (msg.getPerformative() == ACLMessage.CONFIRM) {
                            devicesConnected.put(sender, true);
                        }
                    }
                    case WARNING -> {
                        emergencies.put(msg.getContent(), msg.getSender());
                        emergencyStatus.put(msg.getContent(), false);
                        return AgentStatus.WARNING;
                    }
                    case COMMAND -> {
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            ACLMessage m = new ACLMessage(ACLMessage.INFORM);
                            m.setSender(getAID());
                            m.setProtocol(Protocols.COMMAND.toString());
                            m.setContent(msg.getContent());
                            notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                            sendMsg(m);
                        }
                    }
                }

            }
        }
        return status;
    }

    public AgentStatus warning() {
        emergencies.forEach((warning, device) -> {
            if (!emergencyStatus.get(warning)) {
                ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
                m.setContent(warning);
                m.setProtocol(Protocols.WARNING.toString());
                notifiers.forEach(notifier -> m.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                sendMsg(m);
                emergencyStatus.put(warning, true);
                timer.setTimeout(() -> {
                    emergencyStatus.put(warning, false);
                }, warningDelay); // we try to remind the user again
            }
        });
        ACLMessage response = receiveMsg(
                MessageTemplate.and(
                        MessageTemplate.MatchProtocol(Protocols.WARNING.toString()),
                        MessageTemplate.or(
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
                        )
                )
        );

        if (response != null) {
            switch (response.getPerformative()) {
                case ACLMessage.INFORM -> {
                    ACLMessage endWarning = new ACLMessage(ACLMessage.INFORM);
                    endWarning.setSender(getAID());
                    endWarning.addReceiver(emergencies.get(response.getContent()));
                    endWarning.setContent(response.getContent());
                    endWarning.setProtocol(Protocols.WARNING.toString());
                    sendMsg(endWarning);
                    emergencies.remove(response.getContent());
                }
                case ACLMessage.REQUEST -> {
                    emergencies.put(response.getContent(), response.getSender());
                    emergencyStatus.put(response.getContent(), false);
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
        // dont stop the warning status
        if (status != AgentStatus.WARNING) {
            for (String s : devicesConnected.keySet()) {
                devicesConnected.put(s, false);
            }
            logger.info("Checking online devices");
            ACLMessage m = new ACLMessage();
            m.setPerformative(ACLMessage.QUERY_IF);
            m.setSender(getAID());
            m.setProtocol(Protocols.CHECK_CONNECTION.toString());
            devices.forEach((device, _capabilities) -> {
                m.addReceiver(new AID(device, AID.ISLOCALNAME));
            });
            sendMsg(m);
            timer.setTimeout(this::checkConnectionStatus, connectionStatusDelay); // 10 seconds for devices to send msg back
        }

    }

    public void checkConnectionStatus() {
        List<String> offlineDevices = new ArrayList<>();
        devicesConnected.forEach((device, status) -> {
            if (!status) offlineDevices.add(device);
        });
        offlineDevices.stream().forEach(devices::remove);
        ACLMessage m = new ACLMessage();
        m.setPerformative(ACLMessage.INFORM); // TODO es esta?
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


}
