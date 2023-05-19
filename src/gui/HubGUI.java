package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.Random;
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

    private String telegramCode;

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
            Random random = new Random();
            telegramCode = "";
            for (int i = 0; i < 6; i++) {
                telegramCode += random.nextInt(0, 10);
            }
            JDialog dialog = new JDialog(mainframe, false);
            dialog.setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
            dialog.setBounds(440, 10, 410, 310);
            dialog.add(Box.createRigidArea(new Dimension(5, 20)));

            JLabel title = new JLabel("Telegram Verification Code");
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            title.setFont(new Font(title.getFont().getName(), Font.BOLD, 20));
            dialog.add(title);


            dialog.add(Box.createRigidArea(new Dimension(5, 40)));

            JLabel code = new JLabel(telegramCode);
            code.setFont(new Font(Font.MONOSPACED, Font.BOLD, 35));
            code.setAlignmentX(Component.CENTER_ALIGNMENT);
            dialog.add(code);

            dialog.add(Box.createRigidArea(new Dimension(5, 40)));


            JLabel label = new JLabel("use /register " + telegramCode + " to register your account");
            label.setForeground(new Color(100, 100, 100));
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            dialog.add(label);

            dialog.add(Box.createRigidArea(new Dimension(5, 40)));


            JButton closebtn = new JButton("Close");
            closebtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            closebtn.addActionListener((e) -> dialog.dispose());
            dialog.add(closebtn);
            dialog.setVisible(true);
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

    public String getTelegramCode() {
        return telegramCode;
    }
}
