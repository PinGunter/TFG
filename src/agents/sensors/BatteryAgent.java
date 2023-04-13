package agents.sensors;

import agents.AgentStatus;
import device.battery.*;
import messages.Emergency;


public class BatteryAgent extends SensorAgent {

    private boolean isLinux;
    private boolean isWindows;

    private boolean hasDataAccess = true; // we assume we can access it

    private BatteryAccessor batteryAccessor;


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
