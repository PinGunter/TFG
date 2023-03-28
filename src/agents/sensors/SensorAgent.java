package agents.sensors;

import agents.AgentStatus;
import agents.BaseAgent;
import jade.core.AID;

public abstract class SensorAgent extends BaseAgent {
    protected AID deviceController;
    protected int pollingPeriod;

    protected boolean emergency = false;

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = idle();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    protected AgentStatus login() {
        return status;
    }

    protected AgentStatus idle() {
        return status;
    }

    protected AgentStatus logout() {
        return status;
    }
}
