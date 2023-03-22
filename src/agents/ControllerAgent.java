package agents;

import utils.Timeout;

import java.util.List;
import java.util.Random;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;

    private Timeout timeout;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        timeout = new Timeout();
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
        if (isFirstTime) {
            this.DFAddMyServices(List.of("DEVICE-CONTROLLER"));
            isFirstTime = false;
        }
        return this.lookForHub(Protocols.CONTROLLER_LOGIN.toString());
    }

    public AgentStatus idle() {
        // TESTING PURPOSES
        timeout.setTimeout(() -> status = AgentStatus.LOGOUT, new Random().nextInt(20000, 30000));
        return status;
    }

    public AgentStatus logout() {
        // logout process
        goodBye(Protocols.CONTROLLER_LOGOUT.toString());
        return AgentStatus.END;
    }

    public AgentStatus warning() {
        // warn the hub and wait for response
        return status;
    }

}
