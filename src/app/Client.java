package app;

import agents.ControllerAgent;
import device.Capabilities;
import launchers.JADELauncher;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Client extends JFrame {
    private JTextField nameField;
    private JCheckBox cameraCheckBox;
    private JCheckBox microphoneCheckBox;
    private JCheckBox batteryCheckBox;
    private JCheckBox speakersCheckBox;
    private JCheckBox screenCheckBox;
    private JButton startButton;
    private JTextArea errorArea;
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel rightPanel;
    private JPanel bottomPanel;
    private JTextField sipField;
    private JPasswordField passField;


    public Client() {
        super("CLIENT");
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setSize(600, 400);
        setVisible(true);
        startButton.addActionListener(this::onStartButton);
    }

    private void onStartButton(ActionEvent event) {
        String ip = sipField.getText();
        String name = nameField.getText();
        StringBuilder user_pass = new StringBuilder();
        for (char c : passField.getPassword()) {
            user_pass.append(c);
        }
        String pass = Base64.getEncoder().encodeToString(user_pass.toString().getBytes(StandardCharsets.UTF_8));
        List<Capabilities> capabilitiesList = new ArrayList<>();
        if (cameraCheckBox.isSelected()) capabilitiesList.add(Capabilities.CAMERA);
        if (microphoneCheckBox.isSelected()) capabilitiesList.add(Capabilities.MICROPHONE);
        if (batteryCheckBox.isSelected()) capabilitiesList.add(Capabilities.BATTERY);
        if (screenCheckBox.isSelected()) capabilitiesList.add(Capabilities.SCREEN);
        if (speakersCheckBox.isSelected()) capabilitiesList.add(Capabilities.SPEAKERS);

        Object[] arguments = new Object[2];
        arguments[0] = pass;
        arguments[1] = capabilitiesList;

        try {
            if (!user_pass.isEmpty() && !ip.isEmpty() && !name.isEmpty()) {
                JADELauncher boot = new JADELauncher();
                boot.boot(ip, 1099);
                boot.launchAgent(name, ControllerAgent.class, arguments);
                setVisible(false);
                boot.waitAndShutdown();
            } else {
                errorArea.setText("No field can be left blank");
            }

        } catch (Exception e) {
            errorArea.setText("ERROR: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Client c = new Client();
    }

}
