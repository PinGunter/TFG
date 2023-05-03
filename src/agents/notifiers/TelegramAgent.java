package agents.notifiers;

import agents.AgentStatus;
import agents.Protocols;
import device.Capabilities;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import messages.Command;
import messages.ControllerID;
import messages.Emergency;
import notifiers.TelegramBot;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import utils.Emoji;
import utils.Utils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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

    private HashSet<Pair<Long, String>> users;
    private HashSet<Long> chatIds;

    private List<Emergency> emergencies;

    private String fullInputPath;

    private boolean isRecordingAudio;

    final String chatIdsPath = "data/notifiers/chatIds.data";

    private String lastAudioPath;

    @Override
    public void setup() {
        // agent setup
        super.setup();
        status = AgentStatus.LOGIN;

        // we try to read a chatIds.data from the data/ directory
        // if we fail to open it or cant read its contents (it should be encrypted)
        // then the bot is not able to communicate to any user without registering it first
        // TODO once we have the ui we should be able to turn on/off the registering process
        // right now its gonna be open during the first 30 seconds

        users = new HashSet<>();
        chatIds = new HashSet<>();


        try {
            users = (HashSet<Pair<Long, String>>) Utils.ReadEncryptedFile(chatIdsPath, cryptKey);
        } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException | ClassNotFoundException e) {
            logger.error("ChatIds file appears to be written with a different cryptkey, overriding...");
        }

        for (Pair<Long, String> user : users) {
            chatIds.add(user.getKey());
        }
//         bot registration
        bot = new TelegramBot(this::onReceiveTelegramMessage, chatIds);
        if (users.size() == 0) {
            bot.setOpen(true);
            timer.setTimeout(() -> bot.setOpen(false), 30 * 1000);
        }
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            notifyUsers("Domotic System started");
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

        isRecordingAudio = false;


        onlineDevices = new HashMap<>();
        emergencies = new ArrayList<>();
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
                            if (em.getType().equals("Motion")) {
                                InputStream is = new ByteArrayInputStream((byte[]) em.getObject());
                                InputFile inputFile = new InputFile(is, "image");
                                alertImage(em.getMessage(), em.getOriginDevice().getLocalName(), inputFile);
                            } else {
                                alertUsers(em.getMessage(), em.getOriginDevice().getLocalName());
                            }
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
                    case COMMAND -> {
                        try {
                            Command c = (Command) msg.getContentObject();
                            switch (c.getStatus()) {
                                case DONE -> {
                                    switch (c.getResultType()) {
                                        case "msg" -> notifyUsers((String) c.getResult());
                                        case "img" -> {
                                            byte[] image = (byte[]) c.getResult();
                                            InputStream is = new ByteArrayInputStream(image);
                                            InputFile inputFile = new InputFile(is, "image");
                                            for (Pair<Long, String> user : users) {
                                                bot.sendPhoto(user.getKey(), inputFile);
                                            }
                                        }
                                        case "burst" -> {
                                            byte[] gif = (byte[]) c.getResult();
                                            InputFile photo = new InputFile();
                                            photo.setMedia(new ByteArrayInputStream(gif), "burst.gif");
//                                            for (int i = 0; i < burst.size(); i++) {
//                                                InputStream is = new ByteArrayInputStream(burst.get(i));
//                                                InputMedia inputMedia = new InputMediaPhoto();
//                                                inputMedia.setCaption("Burst captured");
//                                                inputMedia.setMedia(is, "burst" + i);
//                                                photos.add(inputMedia);
//                                            }

                                            for (Pair<Long, String> user : users) {
                                                bot.sendAnimation(user.getKey(), photo);
                                            }

                                        }
                                        case "audio" -> {
                                            byte[] byteArray = (byte[]) c.getResult();
                                            InputStream is = new ByteArrayInputStream(byteArray);
                                            InputFile inputFile = new InputFile(is, "audio_recording");
                                            for (Pair<Long, String> user : users) {
                                                bot.sendVoiceMsg(user.getKey(), inputFile);
                                            }
                                            notifyUsers("Stopped recording");
                                            isRecordingAudio = false;
                                        }
                                    }
                                }
                                case IN_PROGRESS -> {
                                    if (c.getResultType().equals("audio")) isRecordingAudio = true;
                                    notifyUsers((String) c.getResult());
                                }
                                case FAILURE -> {
                                    for (Pair<Long, String> user : users) {
                                        bot.sendText(user.getKey(), Emoji.ERROR.toString() + c.getResult());
                                    }
                                }
                            }
                        } catch (UnreadableException e) {
                            logger.error("Error deserializing command");
                        }
                    }
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
            logger.info("Received Telegram Message: " + messageText);
            if (message.isCommand()) {

                // first Message 
                if (messageText.equals("/start")) {
                    if (isUserRegistered(userId))
                        bot.sendWithReplyMenu(userId, welcomeMessage, mainMenu);
                }
                // stop JADE service
                else if (messageText.equals("/stop")) {
                    if (isUserRegistered(userId))
                        goodbye(userId);
                } else if (messageText.startsWith("/setpagelimit")) {
                    if (isUserRegistered(userId)) {
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
                    }

                } else if (messageText.startsWith("/register")) {
                    if (bot.isOpen() && !chatIds.contains(userId)) {
                        if (!messageText.equals("/register")) {
                            String key = messageText.split(" ")[1];
                            if (Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8)).equals(cryptKey)) {
                                addUserId(userId, message.getFrom().getFirstName() + " " + message.getFrom().getLastName());
                                bot.sendText(userId, Emoji.HELLO.toString() + " Welcome, " + message.getFrom().getFirstName() + "\nYou are now registered!");
                            } else {
                                bot.sendText(userId, Emoji.ERROR.toString() + " Sorry, but the key seems to be incorrect.\nRemember that the key is the same one as in the hub");
                            }
                        } else {
                            bot.sendText(userId, Emoji.NERD.toString() + " To use this command correctly add the key at the end.\nLike this: /register key");
                        }
                    } else if (chatIds.contains(userId)) {
                        bot.sendText(userId, Emoji.NERD.toString() + " You are already registered");
                    } else {
                        bot.sendText(userId, Emoji.NERD.toString() + " The bot is currently closed for new users. Restart the service or open the bot in the hub screen");
                    }
                } else {
                    notUnderstood(userId);
                }
            } else if (message.hasVoice() || message.hasAudio()) {
                GetFile uploadedFile = new GetFile();
                String filePath = null;
                try {
                    if (message.hasVoice()) {
                        logger.info("Nota de voz");
                        uploadedFile.setFileId(message.getVoice().getFileId());
                        filePath = bot.execute(uploadedFile).getFilePath();
                    } else {
                        logger.info("Audio");
                        uploadedFile.setFileId(message.getAudio().getFileId());
                        filePath = bot.execute(uploadedFile).getFilePath();
                    }
                    lastAudioPath = "temp/" + filePath.split("/")[1];
                    File output = new File(lastAudioPath);
                    bot.downloadFile(filePath, output);

                    InlineKeyboardButton singleDevice = InlineKeyboardButton.builder().text(Emoji.SPEAKER + " Single Device").callbackData("audio/singleDevice").build();
                    InlineKeyboardButton broadcast = InlineKeyboardButton.builder().text(Emoji.BROADCAST + " Broadcast").callbackData("audio/broadcast").build();
                    InlineKeyboardMarkup audioKb = InlineKeyboardMarkup.builder().keyboardRow(List.of(singleDevice)).keyboardRow(List.of(broadcast)).build();
                    bot.sendWithKeyboard(userId, "Select an option", audioKb);

                } catch (TelegramApiException e) {
                    logger.error("Error downloading audio");
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

        fullInputPath = data;

        if (isUserRegistered(chatId)) {
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
            } else if (data.startsWith("audio/")) {
                handleAudio(data);
            } else {
                logger.error("Unknown callback data: " + data);
            }
        }

        AnswerCallbackQuery close = AnswerCallbackQuery.builder()
                .callbackQueryId(queryId).build();

        bot.myExecute(close);
        if (!newTxt.getText().isEmpty()) {
            bot.myExecute(newTxt);
        }
        bot.myExecute(newKb);
    }


    private void handleAudio(String path) {
        List<String> items = List.of(path.split("/"));
        if (items.size() > 0) {
            if (items.get(1).equals("singleDevice")) {
                if (items.size() > 2) {
                    // play the sound in the corresponding device
                    Command c = new Command("play " + lastAudioPath, items.get(2), items.get(3));
                    sendCommand(c);
                } else {
                    // show a list of devices with speakers
                    List<String> devicesWithSpeakers = onlineDevices.entrySet().stream().filter(entry -> entry.getValue().contains(Capabilities.SPEAKERS)).map(Map.Entry::getKey).toList();
                    List<InlineKeyboardButton> buttons = devicesWithSpeakers.stream().map(d -> InlineKeyboardButton.builder().text(d).callbackData("audio/singleDevice/" + d + "/SPEAKERS").build()).toList();

                    InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();
                    if (buttons.size() > 0) {
                        for (int i = 0; i < buttons.size(); i += 2) {
                            if (i < buttons.size() - 1) {
                                keyboardBuilder.keyboardRow(List.of(buttons.get(i), buttons.get(i + 1)));
                            } else {
                                keyboardBuilder.keyboardRow(List.of(buttons.get(i)));
                            }
                        }
                        newKb.setReplyMarkup(keyboardBuilder.build());
                    } else {
                        newTxt.setText(Emoji.COLD_SWEAT + " There are no devices with speakers connected");
                        newKb.setReplyMarkup(returnMainMenu);
                    }
                }

            } else if (items.get(1).equals("broadcast")) {
                // send command to all speakers
                List<String> devicesWithSpeakers = onlineDevices.entrySet().stream().filter(entry -> entry.getValue().contains(Capabilities.SPEAKERS)).map(Map.Entry::getKey).toList();
                if (devicesWithSpeakers.size() > 0) {
                    devicesWithSpeakers.forEach(d -> {
                        sendCommand(new Command("play " + lastAudioPath, d, "SPEAKERS"));
                    });
                } else {
                    newTxt.setText(Emoji.COLD_SWEAT + " There are no devices with speakers connected");
                    newKb.setReplyMarkup(returnMainMenu);
                }
            }
        }
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
            if (c.stream().map(Enum::toString).toList().contains(items.get(1))) {
                switch (items.get(1)) {
                    case "CAMERA" -> {
                        if (items.size() < 3) {
                            // root of camera
                            InlineKeyboardButton takePictureBtn = InlineKeyboardButton.builder().text("Take Picture").callbackData(fullInputPath + "/single").build();
                            InlineKeyboardButton takeBurstBtn = InlineKeyboardButton.builder().text("Take Burst of 10 photos each second").callbackData(fullInputPath + "/burst").build(); // TODO this is just an example
                            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboardRow(List.of(takePictureBtn)).keyboardRow(List.of(takeBurstBtn)).keyboardRow(List.of(returnMainMenuBtn)).build();
                            newKb.setReplyMarkup(kb);
                            newTxt.setText("Choose an option");
                        } else {
                            Command command = new Command("ALARM", device, items.get(1));
                            if (items.get(2).equals("single")) {
                                command.setOrder("photo");
                            } else {
                                command.setOrder("burst 10 1");
                            }
                            sendCommand(command);
                        }
                    }
                    case "SCREEN" -> {
                        if (items.size() < 3) {
                            InlineKeyboardButton toggleScreen = InlineKeyboardButton.builder().text("Toggle Screen").callbackData(fullInputPath + "/toggle").build();
                            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder().keyboardRow(List.of(toggleScreen)).keyboardRow(List.of(returnMainMenuBtn)).build();
                            newKb.setReplyMarkup(kb);
                            newTxt.setText("Screen options");
                        } else {
                            Command command = new Command("toggle", device, items.get(1));
                            sendCommand(command);
                            newKb.setReplyMarkup(returnMainMenu);
                            newTxt.setText("Screen toggled");
                        }
                    }
                    case "MICROPHONE" -> {
                        if (items.size() < 3) {
                            InlineKeyboardMarkup.InlineKeyboardMarkupBuilder kb = InlineKeyboardMarkup.builder();
                            if (!isRecordingAudio) {
                                int[] seconds_template = {10, 15, 30, 60};
                                for (int i = 0; i < seconds_template.length; i += 2) {
                                    InlineKeyboardButton btn = InlineKeyboardButton.builder().text(seconds_template[i] + " s").callbackData(fullInputPath + "/record/" + seconds_template[i]).build();
                                    InlineKeyboardButton btn2 = InlineKeyboardButton.builder().text(seconds_template[i + 1] + " s").callbackData(fullInputPath + "/record/" + seconds_template[i + 1]).build();
                                    kb.keyboardRow(List.of(btn, btn2));
                                }
                            }
                            kb.keyboardRow(List.of(InlineKeyboardButton.builder().text(isRecordingAudio ? "Stop recording" : "Start recording (3min max)").callbackData(fullInputPath + "/record/startstop").build()));
                            kb.keyboardRow(List.of(returnMainMenuBtn));
                            newKb.setReplyMarkup(kb.build());
                            newTxt.setText("Select an option");
                        } else {
                            if (items.get(2).equals("record")) {
                                newKb.setReplyMarkup(returnMainMenu);
                                if (items.get(3).equals("startstop")) {
                                    sendCommand(new Command("startstop", device, items.get(1)));
                                } else {
                                    int seconds = -1;
                                    try {
                                        seconds = Integer.parseInt(items.get(3));
                                    } catch (NumberFormatException e) {
                                        seconds = -1;
                                    }
                                    if (seconds != -1) {
                                        sendCommand(new Command("record " + seconds, device, items.get(1)));
                                    }
                                }
                            }
                        }
                    }
                    default -> {
                        Command command = new Command("ALARM", device, items.get(1));
                        sendCommand(command);
                        newKb.setReplyMarkup(returnMainMenu);
                        newTxt.setText("Interacting with " + items.get(1) + " in " + device);
                    }
                }

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
            Emergency em = Utils.FindEmergencyByName(emergencies, emergency);
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
        users.forEach(user -> bot.sendText(user.getKey(), Emoji.NOTIFY + " " + msg));
    }

    public void alertUsers(String msg, String origin) {
        InlineKeyboardButton ack = InlineKeyboardButton.builder().text("Acknowledge emergency").callbackData("warning/" + msg).build();
        users.forEach(user -> bot.sendWithKeyboard(user.getKey(),
                Emoji.WARNING.toString() + Emoji.WARNING + Emoji.WARNING + "\n" +
                        msg.toUpperCase() + "\n " + Emoji.LOCATION_PIN + origin + "\n"
                        + Emoji.WARNING + Emoji.WARNING + Emoji.WARNING
                , InlineKeyboardMarkup.builder().keyboardRow(List.of(ack)).build()));
    }

    public void alertImage(String msg, String origin, InputFile photo) {
        InlineKeyboardButton ack = InlineKeyboardButton.builder().text("Acknowledge emergency").callbackData("warning/" + msg).build();
        users.forEach(user -> bot.sendText(user.getKey(),
                Emoji.WARNING.toString() + Emoji.WARNING + Emoji.WARNING + "\n" +
                        msg.toUpperCase() + "\n " + Emoji.LOCATION_PIN + origin + "\n"
                        + Emoji.WARNING + Emoji.WARNING + Emoji.WARNING
        ));
        users.forEach(user -> bot.sendPhotoKbMarkup(user.getKey(), photo, InlineKeyboardMarkup.builder().keyboardRow(List.of(ack)).build()));
    }

    public void commandFinished(String msg) {
        users.forEach(user -> bot.sendText(user.getKey(), Emoji.CHECK + " " + msg));
    }

    public void addUserId(long id, String name) {
        users.add(new MutablePair<>(id, name));
        chatIds.add(id);
        logger.info("new user: " + id + " - " + name);
        bot.setChatIds(chatIds);
        try {
            Utils.WriteEncryptedFile(users, chatIdsPath, cryptKey);
        } catch (IOException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 BadPaddingException | InvalidKeyException e) {
            logger.error("Error saving chatIds");
        }
    }

    public boolean isUserRegistered(Long id) {
        if (!chatIds.contains(id)) {
            bot.sendText(id, Emoji.NERD.toString() + " You need to be registered first");
        }
        return chatIds.contains(id);
    }
}
