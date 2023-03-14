package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class Logger {

    private String agentName;
    private String fileName;

    private PrintWriter printWriter;
    private boolean writeToFile;

    public Logger(String fileName){
        if (fileName != "" && fileName != null){
            this.fileName = fileName;
            writeToFile = true;
            try {
                FileWriter fw = new FileWriter(fileName, true);
                BufferedWriter bw = new BufferedWriter(fw);
                printWriter = new PrintWriter(bw);
            } catch (IOException e){
                this.fileName = "";
                writeToFile = false;
                error("Couldn't write to file " + fileName);
            }
        } else {
            this.fileName = "";
            writeToFile = false;
        }
    }
    public Logger(){
        this("");
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    private void log(String msg){
        if (writeToFile){
            printWriter.println(msg);
        }
        System.out.println(msg);
    }

    public void info(String msg){
        String output = "[INFO]" + "\t" + agentName + ": " + msg;
        log(output);
    }

    public void error(String msg){
        String output = "[ERROR]" + "\t" + agentName + ": " + msg;
        log(output);
    }
}
