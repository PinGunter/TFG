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


    public Client(boolean graphic, String[] args) {
        super("CLIENT");
        if (graphic) {
            setContentPane(mainPanel);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            pack();
            setSize(600, 400);
            setVisible(true);
            startButton.addActionListener(this::onStartButton);
        } else {
            String host = "", pass = "", name = "";
            List<Capabilities> capabilities = new ArrayList<>();
            for (int i = 0; i < args.length; ) {
                if (args[i].equals("-host")) {
                    host = args[++i];
                    i++;
                } else if (args[i].equals("-name")) {
                    name = args[++i];
                    i++;
                } else if (args[i].equals("-pass")) {
                    pass = Base64.getEncoder().encodeToString(args[++i].getBytes(StandardCharsets.UTF_8));
                    i++;
                } else if (args[i].equals("-cam")) {
                    capabilities.add(Capabilities.CAMERA);
                    i++;
                } else if (args[i].equals("-micro")) {
                    capabilities.add(Capabilities.MICROPHONE);
                    i++;
                } else if (args[i].equals("-batt")) {
                    capabilities.add(Capabilities.BATTERY);
                    i++;
                } else if (args[i].equals("-screen")) {
                    capabilities.add(Capabilities.SCREEN);
                    i++;
                } else if (args[i].equals("-speak")) {
                    capabilities.add(Capabilities.SPEAKERS);
                    i++;
                } else {
                    System.err.println("Invalid arguments. Launch the program like this:\njava -jar client.java -host <hostname> -pass <password> -name <name> [-cam] [-micro] [-batt] [-screen] [-speak]");
                    System.exit(1);
                }
            }
            try {
                startClient(host, pass, name, capabilities, graphic);
            } catch (Exception e) {
                System.err.println("Error launching agent: " + e.getMessage());
                System.exit(1);
            }
        }

    }

    private void startClient(String host, String pass, String name, List<Capabilities> capabilities, boolean graphics) throws Exception {
        Object[] arguments = new Object[3];
        arguments[0] = pass;
        arguments[1] = capabilities;
        arguments[2] = graphics;
        JADELauncher boot = new JADELauncher();
        boot.boot(host, 1099);
        boot.launchAgent(name, ControllerAgent.class, arguments);
        boot.waitAndShutdown();
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

        if (!user_pass.isEmpty() && !ip.isEmpty() && !name.isEmpty()) {
            try {
                startClient(ip, pass, name, capabilitiesList, true);
                setVisible(false);
            } catch (Exception e) {
                errorArea.setText("Error launching agent.\n" + e.getMessage());
            }
        } else {
            errorArea.setText("No field can be left blank");
        }
    }

    public static void main(String[] args) {
        System.out.println("Welcome to the domotic alerts system");
        Client c = new Client(args.length == 0, args);
    }

}
