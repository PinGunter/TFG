package device;

import javax.swing.*;
import java.awt.*;

public class FullScreenWindow extends JFrame {
    public FullScreenWindow() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setTitle("Domotic Alerts");
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setBackground(Color.WHITE);
    }

    public static void main(String[] a) {
        FullScreenWindow f = new FullScreenWindow();
        f.setVisible(!f.isVisible());
    }
}
