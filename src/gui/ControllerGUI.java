package gui;

import javax.swing.*;
import java.awt.*;

public class ControllerGUI extends JFrame {
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JButton stopButton;


    private String name;

    public void setStatus(String status, Color c) {
        statusLabel.setForeground(c);
        statusLabel.setText(status);
    }

    public ControllerGUI(String name) {
        super("Domotic Alerts - " + name);
        this.name = name;
        this.titleLabel.setText(name);
        setContentPane(mainPanel);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        pack();
        setSize(200, 200);
        setVisible(true);
    }
}
