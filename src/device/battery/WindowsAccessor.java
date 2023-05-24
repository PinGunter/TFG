package device.battery;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.win32.StdCallLibrary;

import java.util.ArrayList;
import java.util.List;

/**
 * Found at:
 * <a href="https://stackoverflow.com/questions/3434719/how-to-get-the-remaining-battery-life-in-a-windows-system">...</a>
 */
public class WindowsAccessor implements BatteryAccessor {
    public interface Kernel32 extends StdCallLibrary {
        public Kernel32 INSTANCE = Native.load("Kernel32", Kernel32.class);

        public class SYSTEM_POWER_STATUS extends Structure {
            public byte ACLineStatus;
            public byte BatteryFlag;
            public byte BatteryLifePercent;
            public byte Reserved1;
            public int BatteryLifeTime;
            public int BatteryFullLifeTime;

            @Override
            protected List<String> getFieldOrder() {
                ArrayList<String> fields = new ArrayList<String>();
                fields.add("ACLineStatus");
                fields.add("BatteryFlag");
                fields.add("BatteryLifePercent");
                fields.add("Reserved1");
                fields.add("BatteryLifeTime");
                fields.add("BatteryFullLifeTime");
                return fields;
            }

            public String getACLineStatusString() {
                switch (ACLineStatus) {
                    case (0):
                        return "Offline";
                    case (1):
                        return "Online";
                    default:
                        return "Unknown";
                }
            }

        }

        public int GetSystemPowerStatus(SYSTEM_POWER_STATUS result);

    }

    @Override
    public BatteryStatus getBatteryStatus() throws NoBatteryAccessException {
        Kernel32.SYSTEM_POWER_STATUS batteryStatus = new Kernel32.SYSTEM_POWER_STATUS();
        Kernel32.INSTANCE.GetSystemPowerStatus(batteryStatus);
        switch (batteryStatus.getACLineStatusString()) {
            case "Offline" -> {
                return BatteryStatus.DISCHARGING;
            }
            case "Online" -> {
                return BatteryStatus.CHARGING;
            }
            case "Unkown" -> {
                return BatteryStatus.UNKNOWN;
            }
            default -> {
                return BatteryStatus.UNKNOWN;
            }
        }
    }
}
