package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.function.Consumer;

public class HubGUI {
    private JPanel panel1;
    private JButton stopButton;
    private JLabel statusLabel;
    private JScrollPane scrollPane;
    private JButton enableTelegramRegistrationButton;
    private JButton disableTelegramRegistrationButton;
    private JList deviceList;

    private JFrame mainframe;
    Consumer<ComponentEvent> onExit;
    Consumer<ActionEvent> startTelegram;
    Consumer<ActionEvent> stopTelegram;


    public HubGUI(Consumer<ComponentEvent> exit, Consumer<ActionEvent> startTelegram, Consumer<ActionEvent> stopTelegram) {
        this.onExit = exit;
        this.startTelegram = startTelegram;
        this.stopTelegram = stopTelegram;
        mainframe = new JFrame("Domotic Alerts - HUB");
        mainframe.setContentPane(panel1);
        mainframe.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        mainframe.pack();
        disableTelegramRegistrationButton.setEnabled(false);
        mainframe.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                onExit.accept(e);
                ((JFrame) (e.getComponent())).dispose();
            }
        });
        stopButton.addActionListener((ActionEvent ev) -> {
            onExit.accept(null);
            mainframe.dispose();
        });
        enableTelegramRegistrationButton.addActionListener((t) -> {
            enableTelegramRegistrationButton.setEnabled(false);
            disableTelegramRegistrationButton.setEnabled(true);
            startTelegram.accept(t);
        });
        disableTelegramRegistrationButton.addActionListener((t) -> {
            enableTelegramRegistrationButton.setEnabled(true);
            disableTelegramRegistrationButton.setEnabled(false);
            stopTelegram.accept(t);
        });
    }

    public void createWindow() {
        mainframe.setVisible(true);
    }

    public void setStatus(String status, Color c) {
        this.statusLabel.setForeground(c);
        this.statusLabel.setText(status);
    }


    public void setDevices(List<String> devices) {
        this.deviceList.removeAll();
        this.deviceList.setListData(devices.toArray());
    }


}
