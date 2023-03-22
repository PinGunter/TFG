package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import utils.Timeout;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;

public class HubAgent extends BaseAgent {
    private ArrayList<String> notifiers;
    private ArrayList<String> devices;

    private boolean registered = false;

    private Timeout timer;

    private HashMap<String, Boolean> devicesConnected;

    @Override
    public void setup() {
        super.setup();
        notifiers = new ArrayList<String>();
        devices = new ArrayList<String>();
        devicesConnected = new HashMap<>();
        this.status = AgentStatus.LOGIN;
        timer = new Timeout();
        timer.setInterval(new TimerTask() {
            @Override
            public void run() {
                checkDevices();
            }
        }, 60000);
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
                String newDevice = msg.getSender().getLocalName();
                devices.add(newDevice);
                devicesConnected.put(newDevice, true);
                ACLMessage reply = msg.createReply();
                reply.setContent("hello, " + newDevice);
                reply.setPerformative(ACLMessage.INFORM);
                sendMsg(reply);

                // -------- Telegram
                ACLMessage out = new ACLMessage();
                out.setSender(getAID());
                out.setProtocol(Protocols.CONTROLLER_LOGIN.toString());
                out.setPerformative(ACLMessage.INFORM);
                out.setContent(newDevice);
                notifiers.forEach(notifier -> out.addReceiver(new AID(notifier, AID.ISLOCALNAME)));
                sendMsg(out);

                return status;
            }
            // is a normal msg
            //  from notifier
            else if (notifiers.contains(sender)) {
                switch (p) {
                    case COMMAND -> {

                    }
                    case WARNING -> {

                    }
                    default -> {

                    }
                }
            }
            // from some device
            else if (devices.contains(sender)) { // alarm system
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
                }

            }
        }
        return status;
    }

    public AgentStatus warning() {
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
            devices.forEach(device -> {
                m.addReceiver(new AID(device, AID.ISLOCALNAME));
            });
            sendMsg(m);
            timer.setTimeout(this::checkConnectionStatus, 10000); // 10 seconds for devices to send msg back
        }

    }

    public void checkConnectionStatus() {
        List<String> offlineDevices = new ArrayList<>();
        devicesConnected.forEach((device, status) -> {
            if (!status) offlineDevices.add(device);
        });
        devices.removeAll(offlineDevices);
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
