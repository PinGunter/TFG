package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {

    private String agentName;
    private String fileName;

    private PrintWriter printWriter;
    private boolean writeToFile;

    public Logger(String fileName) {
        agentName = "anonymousAgent";
        if (fileName != "" && fileName != null) {
            writeToFile = true;
            this.fileName = fileName;
        } else {
            this.fileName = "";
            writeToFile = false;
        }
    }

    public Logger() {
        this("");
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    private boolean openFile(String fileName) {
        try {
            FileWriter fw = new FileWriter(fileName, true);
            BufferedWriter bw = new BufferedWriter(fw);
            printWriter = new PrintWriter(bw);
            return true;
        } catch (IOException e) {
            this.fileName = "";
            writeToFile = false;
            error("Couldn't write to file " + fileName);
            return false;
        }
    }

    private void log(String msg) {
        if (writeToFile) {
            openFile(fileName);
            printWriter.println(msg);
            printWriter.close();
        }
        System.out.println(msg);
    }

    public void info(String msg) {
        String output = "[INFO]" + "\t" + agentName + ": " + msg;
        log(output);
    }

    public void message(String msg) {
        String output = "[COMMS]" + "\t" + agentName + ": " + msg;
        log(output);
    }

    public void error(String msg) {
        String output = "[ERROR]" + "\t" + agentName + ": " + msg;
        log(output);
    }
}
