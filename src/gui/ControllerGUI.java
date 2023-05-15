package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.function.Consumer;

public class ControllerGUI {
    private static ControllerGUI instance;
    private JLabel titleLabel;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JButton stopButton;
    private JTextArea textArea;

    private JFrame frame;

    Consumer<ComponentEvent> onExit;

    public ControllerGUI(String name, Consumer<ComponentEvent> onExit) {
        this.onExit = onExit;
        this.titleLabel.setText(name);
        frame = new JFrame("Controller");
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.pack();
        frame.setSize(400, 300);
        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                onExit.accept(e);
                ((JFrame) (e.getComponent())).dispose();
            }
        });
        stopButton.addActionListener((ActionEvent ev) -> {
            onExit.accept(null);
            frame.dispose();
        });
    }


    public void show() {
        frame.setVisible(true);
    }

    public void setStatus(String newStatus, Color c) {
        this.statusLabel.setForeground(c);
        this.statusLabel.setText(newStatus);
    }

    public void setTextArea(String text, Color c) {
        this.textArea.setForeground(c);
        this.textArea.setText(text);
    }

}
