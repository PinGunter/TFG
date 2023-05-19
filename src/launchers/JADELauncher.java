package launchers;

// constructor sin paramentros
// boot hostname, puerto
// launchAgent con argumnentos

import jade.core.MicroRuntime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import utils.Logger;
import utils.Timeout;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

public class JADELauncher {

    private Logger logger;

    private boolean isMicroboot;

    private AgentContainer container;
    private String containerName;

    private List<String> agentNames;

    private Timeout timer;

    public JADELauncher() {
        logger = new Logger();
        logger.setAgentName("JADELauncher");
        agentNames = new ArrayList<>();
        timer = new Timeout();
    }

    public JADELauncher boot(String host, int port) {
        isMicroboot = !isLocalIP(host);
        writeToFile("data/settings/micro.txt", String.valueOf(isMicroboot));
        return selectConnection(host, port);
    }

    private JADELauncher selectConnection(String host, int port) {
        return isMicroboot ? setupMicroJadeConnection(host, port) : setupJadeConnection(host, port);
    }

    private boolean isLocalIP(String ip) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(ip);
            return address.isSiteLocalAddress() ||
                    address.isAnyLocalAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isLoopbackAddress() ||
                    address.isMulticastAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private JADELauncher setupJadeConnection(String host, int port) {
        logger.info("Connecting to Jade");
        try {
            Runtime runtime = Runtime.instance();
            ProfileImpl profile = new ProfileImpl();
            if (!host.equals("")) {
                profile.setParameter(Profile.MAIN_HOST, host);
            }
            if (port != -1) {
                profile.setParameter(Profile.MAIN_PORT, "" + port);
            }
            container = runtime.createAgentContainer(profile);

        } catch (Exception e) {
            logger.error("Error connecting to JADE");
            System.exit(1);
        }
        return this;
    }

    public void launchAgent(String name, Class c, Object[] args) throws Exception {
        logger.info("Launching agent: " + name);
        agentNames.add(name);
        AgentController ag;
        if (isMicroboot) {
            MicroRuntime.startAgent(name, c.getName(), args);
            ag = MicroRuntime.getAgent(name);
        } else {
            ag = container.createNewAgent(name, c.getName(), args);
            ag.start();
        }
    }

    private JADELauncher setupMicroJadeConnection(String host, int port) {
        logger.info("Connecting to JADE (Micro)");
        Properties properties = new Properties();
        if (!host.equals("")) {
            properties.setProperty(Profile.MAIN_HOST, host);
        }
        if (port != -1) {
            properties.setProperty(Profile.MAIN_PORT, "" + port);
        }
        MicroRuntime.startJADE(properties, null);
        containerName = MicroRuntime.getContainerName();
        return this;
    }

    public void writeToFile(String path, String output) {
        File file = new File(path);
        FileWriter fr = null;
        try {
            fr = new FileWriter(file);
            fr.write(output);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                fr.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }


    private void purge() {
        logger.info("Purging agents");
        AgentController agentController;
        for (String name : agentNames) {
            try {
                if (isMicroboot) {
                    agentController = MicroRuntime.getAgent(name);
                } else {
                    agentController = container.getAgent(name);
                }
                agentController.kill();
            } catch (Exception e) {
                logger.error("Error purging remaining agents");
            }
        }
    }

    private void stopService() {
        logger.info("Stopping service");
        try {
            if (isMicroboot) {
                MicroRuntime.stopJADE();
            } else {
                container.kill();
            }
        } catch (StaleProxyException e) {
            logger.error("Error killing container");
            System.exit(1);
        }
        System.exit(0);
    }

    private void lifePoll() {
        timer.setInterval(new TimerTask() {
            @Override
            public void run() {
                logger.info("Polling agents");
                boolean isAlive = false;
                for (String name : agentNames) {
                    try {
                        if (isMicroboot) {
                            isAlive = MicroRuntime.size() > 0;
                        } else {
                            container.getAgent(name);
                            isAlive = true;
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
                if (!isAlive) {
                    purge();
                    stopService();
                }
            }
        }, 2500);
    }

    public void waitAndShutdown() {
        lifePoll();
    }
}
