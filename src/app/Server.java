package app;

import agents.HubAgent;
import agents.notifiers.TelegramAgent;
import launchers.JADELauncher;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Server extends JFrame {
    private JPanel mainPanel;
    private JTextField sipField;
    private JPasswordField passField;
    private JButton startButton;
    private JLabel sipLabel;
    private JLabel passLabel;
    private JTextArea errorArea;


    public Server() {
        super("HUB");
        setContentPane(mainPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setSize(500, 500);
        errorArea.setLineWrap(true);
        errorArea.setEditable(false);
        setVisible(true);
        startButton.addActionListener(this::onStartButton);
    }

    private void onStartButton(ActionEvent actionEvent) {
        String ip = sipField.getText();
        StringBuilder user_pass = new StringBuilder();
        for (char c : passField.getPassword()) {
            user_pass.append(c);
        }
        String pass = Base64.getEncoder().encodeToString(user_pass.toString().getBytes(StandardCharsets.UTF_8));
        Object[] arguments = new Object[1];
        arguments[0] = pass;

        try {
            if (!user_pass.isEmpty() && !ip.isEmpty()) {
                JADELauncher boot = new JADELauncher();
                boot.boot(ip, 1099);
                boot.launchAgent("HUB", HubAgent.class, arguments);
                boot.launchAgent("telegramAgent", TelegramAgent.class, arguments);
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
        Server s = new Server();

    }
}
