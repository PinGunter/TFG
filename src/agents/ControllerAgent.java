package agents;

import java.util.List;

public class ControllerAgent extends ClientAgent {

    private List<String> sensors;
    private List<String> actuators;

    private boolean logout = false;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
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
        return this.lookForHub("CONTROLLER-LOGIN");
    }

    public AgentStatus idle() {
        return status;
    }

    public AgentStatus logout() {
        // logout process
        return status;
    }

    public AgentStatus warning() {
        // warn the hub and wait for response
        return status;
    }

}
