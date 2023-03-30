package device.battery;

public class NoBatteryAccessException extends Exception {
    NoBatteryAccessException(String msg) {
        super(msg);
    }
}
