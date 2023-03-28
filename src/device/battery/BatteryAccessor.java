package device.battery;

public interface BatteryAccessor {

    boolean HasBatteryAccess = false;

    public BatteryStatus getBatteryStatus() throws NoBatteryAccessException;
}
