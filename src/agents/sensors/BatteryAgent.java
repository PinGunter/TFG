package agents.sensors;

import agents.AgentStatus;
import device.battery.*;
import messages.Emergency;


public class BatteryAgent extends SensorAgent {

    private boolean isLinux;
    private boolean isWindows;

    private boolean hasDataAccess = true; // we assume we can access it

    private BatteryAccessor batteryAccessor;

    Emergency lastEmergency;

    @Override
    public void setup() {
        super.setup();
        status = AgentStatus.IDLE;
        isLinux = System.getProperty("os.name").equals("Linux");
        isWindows = System.getProperty("os.name").contains("Windows");

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

    public AgentStatus idle() {
        super.idle();
        checkBattery();
        return AgentStatus.IDLE;
    }

    private void checkBattery() {
        try {
            BatteryStatus bStatus = batteryAccessor.getBatteryStatus();
            logger.info(bStatus.toString());
            if (!ack && !emergency && bStatus != BatteryStatus.CHARGING && bStatus != BatteryStatus.NOT_CHARGING) { // not plugged in
                lastEmergency = new Emergency(deviceController, getAID(), "The power is out");
                sendAlert(lastEmergency);
            }
            if (emergency && bStatus == BatteryStatus.CHARGING || bStatus == BatteryStatus.NOT_CHARGING) { // after emergency, plugged back in
                emergencyFinished(lastEmergency);
            }

        } catch (NoBatteryAccessException e) {
            logger.error(e.getMessage());
        }
    }


}
