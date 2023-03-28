package device.battery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class LinuxBatteryAccessor implements BatteryAccessor {
    private final String BatInfoDir = "/sys/class/power_supply/";
    private final String statusExt = "/status";

    // possible status
    // https://www.kernel.org/doc/Documentation/ABI/testing/sysfs-class-power
    private final String Discharging = "Discharging";
    private final String NotCharging = "Not charging"; // plugged in but is not charging
    private final String Charging = "Charging";
    private final String Unknown = "Unknown";
    private final String Full = "Full";

    @Override
    public BatteryStatus getBatteryStatus() throws NoBatteryAccessException {
        File batterySpec = new File(BatInfoDir);
        int battTries = 0;
        String result = "";
        for (String intent : batterySpec.list()) {
            File intentBatt = new File(BatInfoDir + intent + statusExt);
            try {
                Scanner myReader = new Scanner(intentBatt);
                result = myReader.nextLine();
            } catch (FileNotFoundException e) {
                battTries++;
            }
        }
        if (battTries == batterySpec.list().length) {
            throw new NoBatteryAccessException("Unable to access battery information");
        }
        return switch (result) {
            case Charging -> BatteryStatus.CHARGING;
            case Full -> BatteryStatus.FULL;
            case Discharging -> BatteryStatus.DISCHARGING;
            case NotCharging -> BatteryStatus.NOT_CHARGING;
            default -> BatteryStatus.UNKNOWN;
        };
    }
}
