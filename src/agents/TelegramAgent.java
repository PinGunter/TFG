package agents;

import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import messages.Emergency;
import notifiers.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.Emoji;
import utils.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class TelegramAgent extends NotifierAgent {
    TelegramBot bot;
    boolean logout = false;

    // UI
    private InlineKeyboardButton returnMainMenuBtn;
    private InlineKeyboardMarkup mainMenu;
    private InlineKeyboardMarkup returnMainMenu;
    EditMessageText newTxt;
    EditMessageReplyMarkup newKb;
    // constants
    private final String welcomeMessage = "Hello and welcome to the Domotic Alerts Notifier " + Emoji.OWL;

    // pagination
    private int currentIndex;
    private int pageLimit = 6;

    private HashMap<String, List<Capabilities>> onlineDevices;

    private Set<Long> userIDs;

    private List<Emergency> emergencies;

    @Override
    public void setup() {
        // agent setup
        super.setup();
        status = AgentStatus.LOGIN;

        // bot registration
        bot = new TelegramBot(this::onReceiveTelegramMessage);
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage());
            exit = true;
        }

        // ui elements
        returnMainMenuBtn = InlineKeyboardButton.builder().text(Emoji.HOUSE + " Return to Main menu").callbackData("return").build();

        InlineKeyboardButton showDevices = InlineKeyboardButton.builder().text(Emoji.LAPTOP + " Show Devices").callbackData("devices/").build();
        InlineKeyboardButton settings = InlineKeyboardButton.builder().text(Emoji.GEAR + " Settings").callbackData("settings/").build();

        mainMenu = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(showDevices)).keyboardRow(List.of(settings)).build();
        returnMainMenu = InlineKeyboardMarkup.builder().keyboardRow(List.of(returnMainMenuBtn)).build();

        // pagination
        currentIndex = 0;


        onlineDevices = new HashMap<>();
        emergencies = new ArrayList<>();
        // TODO leer/escribir fichero
        userIDs = new HashSet<>();
    }

    @Override
    public void execute() {
        switch (status) {
            case LOGIN -> status = login();
            case IDLE -> status = running();
            case LOGOUT -> status = logout();
            case END -> exit = true;
        }
    }

    // checks the DF for the hubAgent
    public AgentStatus login() {
        if (isFirstTime) {
            this.DFAddMyServices(List.of("NOTIFIER", "TELEGRAM"));
            isFirstTime = false;
        }
        return this.lookForHub(Protocols.NOTIFIER_LOGIN.toString());
    }

    public AgentStatus running() {
        ACLMessage msg = receiveMsg();
        if (msg != null) {
            if (msg.getSender().getLocalName().equals(hub)) {
                Protocols p;
                try {
                    p = Protocols.valueOf(msg.getProtocol());
                } catch (IllegalArgumentException e) {
                    p = Protocols.NULL;
                    logger.error("Not a valid protocol" + msg.getProtocol());
                }

                switch (p) {
                    case WARNING -> {
                        Emergency em = null;
                        try {
                            em = (Emergency) msg.getContentObject();
                        } catch (UnreadableException e) {
                            logger.error("Error deserializing");
                        }
                        if (em != null) {
                            emergencies.add(em);
                            alertUsers(em.getMessage(), em.getOriginDevice().getLocalName());
                        }

                    }
                    case CONTROLLER_LOGIN -> {
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            ControllerID cId = new ControllerID("", null);
                            try {
                                cId = (ControllerID) msg.getContentObject();
                            } catch (UnreadableException e) {
                                logger.error("Error while deserializing");
                            }
                            onlineDevices.put(cId.getName(), cId.getCapabilities());
                            notifyUsers(cId.getName() + " has connected");
                        }
                    }
                    case CONTROLLER_LOGOUT -> {
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            onlineDevices.remove(msg.getContent());
                            notifyUsers(msg.getContent() + " has disconnected");
                        }
                    }
                    case CONTROLLER_DISCONNECT -> {
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            try {
                                ArrayList<String> removed = (ArrayList<String>) msg.getContentObject();
                                removed.forEach(device -> notifyUsers(device + " has disconnected without logging out"));
                                removed.forEach(onlineDevices::remove);
                            } catch (UnreadableException e) {
                                logger.error("Error deserializing");
                            }
                        }
                    }
                    case COMMAND -> notifyUsers(msg.getContent());
                }
            }
        }
        return logout ? AgentStatus.LOGOUT : AgentStatus.IDLE;
    }

    public AgentStatus logout() {
        return AgentStatus.END;
    }

    public void onReceiveTelegramMessage(Update update) {
        if (update.hasCallbackQuery()) {
            userIDs.add(update.getCallbackQuery().getFrom().getId());
            CallbackQuery callbackQuery = update.getCallbackQuery();
            long chatId = callbackQuery.getMessage().getChatId();
            int msgId = callbackQuery.getMessage().getMessageId();
            String data = callbackQuery.getData();
            processButtonPress(chatId, msgId, data, callbackQuery.getId());
        }

        long userId = update.getMessage().getFrom().getId();
        Message message = update.getMessage();
        String messageText = message.getText();

        if (update.hasMessage()) {
            userIDs.add(update.getMessage().getFrom().getId());
            logger.info("Received Telegram Message: " + messageText);
            if (message.isCommand()) {

                // first Message 
                if (messageText.equals("/start")) {

                    bot.sendWithReplyMenu(userId, welcomeMessage, mainMenu);
                }

                // stop JADE service
                else if (messageText.equals("/stopjade")) {
                    goodbye(userId);
                } else if (messageText.startsWith("/setpagelimit")) {
                    try {
                        int newLimit = Integer.parseInt(messageText.split(" ")[1]);
                        if (newLimit < 4 || newLimit > 12 || newLimit % 2 != 0) {
                            bot.sendText(userId, Emoji.NERD + " The limit cannot be under 4 or over 12.\n" + Emoji.NERD + " Remember that it has to be even");
                        } else {
                            pageLimit = newLimit;
                            bot.sendWithReplyMenu(userId, "Page limit changed to " + pageLimit, returnMainMenu);
                        }
                    } catch (Exception e) {
                        bot.sendText(userId, "That wasn't a valid number.\n" + Emoji.NERD + " This command is used liked /setpagelimit <newLimit>");
                    }
                } else {
                    notUnderstood(userId);
                }
            } else if (!message.hasText()) {
                notUnderstood(userId);
            } else {
                if (messageText.equalsIgnoreCase("Hello")) {
                    bot.sendWithReplyMenu(userId, "Hello! " + Emoji.HELLO, returnMainMenu);
                } else {
                    notUnderstood(userId);
                }
            }

        }

    }

    private void processButtonPress(long chatId, int msgId, String data, String queryId) {
        newTxt = EditMessageText.builder()
                .chatId(chatId)
                .messageId(msgId).text("").build();

        newKb = EditMessageReplyMarkup.builder()
                .chatId(chatId).messageId(msgId).build();


        if (data.startsWith("devices/")) {
            handleDevices(data);
        } else if (data.startsWith("settings/")) {
            handleSettings(data);
        } else if (data.startsWith("warning/")) {
            handleWarning(data);
        } else if (data.equals("return")) {
            newTxt.setText(welcomeMessage);
            newKb.setReplyMarkup(mainMenu);
            currentIndex = 0;
        } else {
            logger.error("Unknown callback data: " + data);
        }

        AnswerCallbackQuery close = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId).build();

        bot.myExecute(close);
        if (!newTxt.getText().isEmpty()) {
            bot.myExecute(newTxt);
        }
        bot.myExecute(newKb);
    }


    private void handleDevices(String path) {
        List<String> items = List.of(path.split("/"));
        // obtain the updated list
        if (onlineDevices != null) {
            // the root
            if (items.size() == 1) {
                newTxt.setText("Showing Online devices");
                showDevicePages();
            } else {
                if (items.get(1).equals("backPagination")) {
                    currentIndex = Math.max(0, currentIndex - 1);
                    showDevicePages();

                } else if (items.get(1).equals("nextPagination")) {
                    currentIndex += 1;
                    showDevicePages();

                } else if (onlineDevices.containsKey(items.get(1))) {
                    String newPath = String.join("/", items.subList(1, items.size()));
                    handleDeviceSpecific(newPath);
                }
            }

        } else {
            newTxt.setText(Emoji.ERROR + " Error getting device list " + Emoji.ERROR);
            newKb.setReplyMarkup(returnMainMenu);
        }


    }

    private void handleDeviceSpecific(String path) {
        List<String> items = List.of(path.split("/"));
        String device = items.get(0);
        List<Capabilities> c = onlineDevices.get(device);
        if (items.size() == 1) {
            newTxt.setText("Showing " + device + " capabilities");
            showDeviceCapabilities(device, c);
        } else {
            // FIXME maybe a bit over the top
            if (c.stream().map(Enum::toString).toList().contains(items.get(1))) {
                Command command = new Command("test command", device, items.get(1));
                sendCommand(command);
                newKb.setReplyMarkup(returnMainMenu);
                newTxt.setText("Interacting with " + items.get(1) + " in " + device);
            }
        }

    }

    private void handleSettings(String path) {
        List<String> items = List.of(path.split("/"));
        // the root
        if (items.size() == 1) {
            InlineKeyboardButton maxPerPageBtn = InlineKeyboardButton.builder().text("Page limit").callbackData("settings/pageLimit").build();
            InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder().keyboardRow(List.of(maxPerPageBtn)).keyboardRow(List.of(returnMainMenuBtn)).build();
            newKb.setReplyMarkup(keyboardMarkup);
        } else {
            switch (items.get(1)) {
                case "pageLimit" -> {
                    // ideally would be set using buttons, but it's too complex
                    newTxt.setText("Current page limit is " + pageLimit + "\n" + Emoji.NERD + Emoji.FINGER_UP + "Change it with /setpagelimit <newLimit>");
                    newKb.setReplyMarkup(returnMainMenu);
                }
            }
        }

    }

    private void handleWarning(String path) {
        List<String> items = List.of(path.split("/"));
        logger.info(items.toString());
        if (items.size() != 2) {
            logger.error("Unknown emergency callback received");
        } else {
            String emergency = items.get(1);
            newTxt.setText("Alert acknowledged");
            newKb.setReplyMarkup(returnMainMenu);
            Emergency em = Utils.findEmergencyByName(emergencies, emergency);
            if (em != null) {
                sendHub(ACLMessage.INFORM, em, Protocols.WARNING.toString());
                Utils.RemoveEmergency(emergencies, emergency);
            }

        }
    }

    private void notUnderstood(long userId) {
        InlineKeyboardMarkup returnToMainMenu = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(returnMainMenuBtn)).build();
        bot.sendWithReplyMenu(userId, Emoji.COLD_SWEAT + "Sorry, I didn't get that." + Emoji.COLD_SWEAT + "\n" + Emoji.NERD + Emoji.FINGER_UP + " Try using the inline buttons", returnToMainMenu);
    }

    private void goodbye(long userId) {
        bot.sendText(userId, "Good bye, JADE is stopping");
        logout = true;
    }

    private void showDevicePages() {
        InlineKeyboardMarkup deviceKeyboard = makeButtonList(onlineDevices.keySet().stream().toList(), onlineDevices.size() > pageLimit, currentIndex);
        newKb.setReplyMarkup(deviceKeyboard);
    }


    //TODO try to group up makeButtonList and showDeviceCapabilities
    // they both show a list of buttons but are built in slightly different ways
    private InlineKeyboardMarkup makeButtonList(List<String> devices, boolean usesPagination, int currentPage) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (String device : devices) {
            buttons.add(InlineKeyboardButton.builder().text(device).callbackData("devices/" + device).build());
        }
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

        if (buttons.size() > 0) {

            int maxPage = usesPagination ? Math.min(currentPage * pageLimit + pageLimit, buttons.size()) : buttons.size();

            for (int i = currentPage * pageLimit; i < maxPage && maxPage <= buttons.size(); i += 2) {
                if (i < buttons.size() - 1) {
                    keyboardBuilder.keyboardRow(List.of(buttons.get(i), buttons.get(i + 1)));
                } else {
                    keyboardBuilder.keyboardRow(List.of(buttons.get(i)));
                }

            }

            if (usesPagination) {
                InlineKeyboardButton back = InlineKeyboardButton.builder().text(Emoji.LEFT_ARROW.toString()).callbackData("devices/backPagination").build();
                InlineKeyboardButton next = InlineKeyboardButton.builder().text(Emoji.RIGHT_ARROW.toString()).callbackData("devices/nextPagination").build();

                if (currentPage == 0) { // no need for back button
                    keyboardBuilder.keyboardRow(List.of(next));
                } else if (maxPage == buttons.size()) { // no need for back button
                    keyboardBuilder.keyboardRow(List.of(back));
                } else {
                    keyboardBuilder.keyboardRow(List.of(back, next));
                }

            }
        } else {
            newTxt.setText(Emoji.COLD_SWEAT + " There are no devices connected");
        }

        keyboardBuilder.keyboardRow(List.of(returnMainMenuBtn));
        return keyboardBuilder.build();
    }

    private void showDeviceCapabilities(String device, List<Capabilities> capabilities) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (Capabilities cap : capabilities) {
            buttons.add(InlineKeyboardButton.builder().text(cap.toString()).callbackData("devices/" + device + "/" + cap).build());
        }
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();
        if (buttons.size() > 0) {
            for (int i = 0; i < buttons.size(); i += 2) {
                if (i < buttons.size() - 1) {
                    keyboardBuilder.keyboardRow(List.of(buttons.get(i), buttons.get(i + 1)));
                } else {
                    keyboardBuilder.keyboardRow(List.of(buttons.get(i)));
                }

            }

        } else {
            newTxt.setText(Emoji.COLD_SWEAT + " This device has no capabilities installed");
        }
        InlineKeyboardButton backButton = InlineKeyboardButton.builder().text(Emoji.LEFT_ARROW + "Go back").callbackData("devices/").build();
        keyboardBuilder.keyboardRow(List.of(backButton));
        newKb.setReplyMarkup(keyboardBuilder.build());

    }

    public void sendHub(int performative, String content, String protocol) {
        ACLMessage msg = new ACLMessage();
        msg.setSender(getAID());
        msg.addReceiver(new AID(hub, AID.ISLOCALNAME));
        msg.setContent(content);
        msg.setProtocol(protocol);
        msg.setPerformative(performative);
        sendMsg(msg);
    }

    public void sendHub(int performative, Serializable contentObject, String protocol) {
        try {
            ACLMessage msg = new ACLMessage();
            msg.setSender(getAID());
            msg.addReceiver(new AID(hub, AID.ISLOCALNAME));
            msg.setContentObject(contentObject);
            msg.setProtocol(protocol);
            msg.setPerformative(performative);
            sendMsg(msg);
        } catch (IOException e) {
            logger.error("Error serializing msg to hub");
        }
    }

    public void sendCommand(Command command) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setSender(getAID());
        msg.addReceiver(new AID(hub, AID.ISLOCALNAME));
        msg.setProtocol(Protocols.COMMAND.toString());
        try {
            msg.setContentObject(command);
        } catch (IOException e) {
            logger.error("Error serializing command");
        }
        sendMsg(msg);
    }

    public void notifyUsers(String msg) {
        userIDs.forEach(user -> bot.sendText(user, Emoji.NOTIFY + " " + msg));
    }

    public void alertUsers(String msg, String origin) {
        InlineKeyboardButton ack = InlineKeyboardButton.builder().text("Acknowledge emergency").callbackData("warning/" + msg).build();
        userIDs.forEach(user -> bot.sendWithKeyboard(user,
                Emoji.WARNING.toString() + Emoji.WARNING + Emoji.WARNING + "\n" +
                        msg.toUpperCase() + "\n " + Emoji.LOCATION_PIN + origin + "\n"
                        + Emoji.WARNING + Emoji.WARNING + Emoji.WARNING
                , InlineKeyboardMarkup.builder().keyboardRow(List.of(ack)).build()));
    }

    public void commandFinished(String msg) {
        userIDs.forEach(user -> bot.sendText(user, Emoji.CHECK + " " + msg));
    }
}
