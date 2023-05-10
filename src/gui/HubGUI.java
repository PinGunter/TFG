package gui;

import javax.swing.*;

public class HubGUI {
    private JPanel panel1;
    private JButton stopButton;
    private JLabel sattusLabel;
    private JScrollPane scrollPane;
    private JButton enableTelegramRegistrationButton;
    private JButton disableTelegramRegistrationButton;

    public static boolean Created = false;

    public static void createWindow() {
        if (!Created) {
            Created = true;
            JFrame frame = new JFrame("Domotic Alerts - HUB");
            frame.setContentPane(new HubGUI().panel1);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        }

    }
}
