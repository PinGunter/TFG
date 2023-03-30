package agents.sensors;

import agents.AgentStatus;
import agents.Protocols;
import device.battery.*;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import messages.Emergency;

import java.io.Serializable;


public class BatteryAgent extends SensorAgent {

    private boolean isLinux;
    private boolean isWindows;

    private boolean hasDataAccess = true; // we assume we can access it

    private BatteryAccessor batteryAccessor;


    @Override
    public void setup() {
        super.setup();
        deviceController = (AID) getArguments()[0];
        status = AgentStatus.IDLE;
        isLinux = System.getProperty("os.name").equals("Linux");
        isWindows = System.getProperty("os.name").equals("Windows");

        if (isLinux) {
            batteryAccessor = new LinuxBatteryAccessor();
            try {
                batteryAccessor.getBatteryStatus();
            } catch (NoBatteryAccessException e) {
                hasDataAccess = false;
            }
        } else if (isWindows) {
            batteryAccessor = new WindowsAccessor();
        }
        pollingPeriod = 30000;
    }

    @Override
    public AgentStatus login() {
        return AgentStatus.IDLE;
    }

    @Override
    public AgentStatus idle() {
        super.idle();
        checkBattery();
        return AgentStatus.IDLE;
    }

    private void checkBattery() {
        try {
            BatteryStatus bStatus = batteryAccessor.getBatteryStatus();
            logger.info(bStatus.toString());
            if (!emergency && bStatus != BatteryStatus.CHARGING && bStatus != BatteryStatus.NOT_CHARGING) { // not plugged in
                sendAlert(new Emergency(deviceController, getAID(), "The power is out"));
            }
            if (emergency && bStatus == BatteryStatus.CHARGING || bStatus == BatteryStatus.NOT_CHARGING) { // after emergency, plugged back in
                // TODO maybe confirm to user (pls no)
            }
        } catch (NoBatteryAccessException e) {
            logger.error(e.getMessage());
        }
    }


}