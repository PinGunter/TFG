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

    public void log(String msg){
        if (writeToFile){
            // code to write to file :)
        }
        String output = agentName + ": " + msg;
        System.out.println(output);
    }
}
