package utils;

public class Logger {

    private String agentName;
    private String fileName;

    private boolean writeToFile;

    public Logger(String fileName){
        if (fileName != "" && fileName != null){
            this.fileName = fileName;
            writeToFile = true;
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
            // code to write to file :)
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
