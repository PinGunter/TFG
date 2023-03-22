package utils;

import java.util.Timer;
import java.util.TimerTask;

public class Timeout {
    private Timer timer;
    private boolean timeoutStarted = false;

    public Timeout() {
        timer = new Timer();
    }

    public void setTimeout(Runnable runnable, int delay) {
        if (!timeoutStarted) {
            timeoutStarted = true;
            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    runnable.run();
                    timeoutStarted = false;
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }).start();
        }

    }

    public void setInterval(TimerTask runnable, int period) {
        timer.scheduleAtFixedRate(runnable, 0, period);
    }

    public void cancelInterval() {
        timer.cancel();
    }
}
