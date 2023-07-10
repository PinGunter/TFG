package gui;

import utils.Timeout;

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

    private JDialog codeDialog;
    private String telegramCode;

    private Timeout timer;

    public HubGUI(Consumer<ComponentEvent> exit, Consumer<ActionEvent> startTelegram, Consumer<ActionEvent> stopTelegram) {
        timer = new Timeout();
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
            enableTelegram(t);
            timer.setTimeout(() -> {
                codeDialog.dispose();
                disableTelegram(t);
            }, 120000);

        });
        disableTelegramRegistrationButton.addActionListener(this::disableTelegram);
    }

    private void enableTelegram(ActionEvent t) {
        enableTelegramRegistrationButton.setEnabled(false);
        disableTelegramRegistrationButton.setEnabled(true);
        Random random = new Random();
        telegramCode = "";
        for (int i = 0; i < 6; i++) {
            telegramCode += random.nextInt(0, 10);
        }
        codeDialog = new JDialog(mainframe, false);
        codeDialog.setLayout(new BoxLayout(codeDialog.getContentPane(), BoxLayout.Y_AXIS));
        codeDialog.setBounds(440, 10, 410, 310);
        codeDialog.add(Box.createRigidArea(new Dimension(5, 20)));

        JLabel title = new JLabel("Telegram Verification Code");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 20));
        codeDialog.add(title);


        codeDialog.add(Box.createRigidArea(new Dimension(5, 40)));

        JLabel code = new JLabel(telegramCode);
        code.setFont(new Font(Font.MONOSPACED, Font.BOLD, 35));
        code.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeDialog.add(code);

        codeDialog.add(Box.createRigidArea(new Dimension(5, 40)));


        JLabel label = new JLabel("use /register " + telegramCode + " to register your account");
        label.setForeground(new Color(100, 100, 100));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeDialog.add(label);

        JLabel label2 = new JLabel("This code will expire in 2 minutes");
        label2.setForeground(new Color(100, 100, 100));
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        codeDialog.add(label2);

        codeDialog.add(Box.createRigidArea(new Dimension(5, 40)));


        JButton closebtn = new JButton("Close");
        closebtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closebtn.addActionListener((e) -> codeDialog.dispose());
        codeDialog.add(closebtn);
        codeDialog.setVisible(true);
        startTelegram.accept(t);
    }

    private void disableTelegram(ActionEvent t) {
        enableTelegramRegistrationButton.setEnabled(true);
        disableTelegramRegistrationButton.setEnabled(false);
        stopTelegram.accept(t);
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
