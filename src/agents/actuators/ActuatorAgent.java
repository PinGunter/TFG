package agents.actuators;

import agents.AgentStatus;
import agents.BaseAgent;
import jade.core.AID;

public abstract class ActuatorAgent extends BaseAgent {
    protected AID deviceController;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.LOGIN;
        deviceController = (AID) getArguments()[1];

    }

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = idle();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    protected AgentStatus idle() {
        return status;
    }

    protected AgentStatus login() {
        return status;
    }

    protected AgentStatus logout() {
        return AgentStatus.END;
    }

}

