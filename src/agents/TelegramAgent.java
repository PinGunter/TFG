package agents;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
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

import java.util.ArrayList;
import java.util.List;

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

    // this is currently a mockup
    private List<String> onlineDevices;

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

        // currently a mockup
        onlineDevices = new ArrayList<>();
        for (int i = 0; i < 23; i++) {
            onlineDevices.add("D" + i);
        }
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
        this.DFAddMyServices(List.of("NOTIFIER", "TELEGRAM"));
        List<String> hubProvider = DFGetAllProvidersOf("HUB");
        if (!hubProvider.isEmpty()){
            hub = hubProvider.get(0);
        }
        ACLMessage helloHub = new ACLMessage();
        helloHub.addReceiver(new AID(hub, AID.ISLOCALNAME));
        helloHub.setContent("Hello");
        helloHub.setPerformative(ACLMessage.REQUEST);
        helloHub.setInReplyTo("helloTelegram");
        sendMsg(helloHub);

        logger.info("Waiting for hub");
        helloHub = blockingReceive();
        if (helloHub != null){
            if (helloHub.getPerformative() == ACLMessage.INFORM){
                logger.info("Greeted Hub");
                return AgentStatus.IDLE;
            }
        }

        return  AgentStatus.LOGIN;
    }

    public AgentStatus running() {
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

                    bot.sendWithReplyMenu(userId, welcomeMessage, mainMenu);
                }

                // stop JADE service
                else if (messageText.equals("/stopjade")) {
                    goodbye(userId);
                }

                else if (messageText.startsWith("/setpagelimit")){
                    try {
                        int newLimit = Integer.parseInt(messageText.split(" ")[1]);
                        if (newLimit < 4 || newLimit > 12 || newLimit % 2 != 0){
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


        if (data.startsWith("devices/")){
            handleDevices(data);
        } else if (data.startsWith("settings/")) {
            handleSettings(data);
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

    // this looks really ugly :(

    private void handleDevices(String path) {
        List<String> items = List.of(path.split("/"));
        // the root
        if (items.size() == 1) {
            newTxt.setText("Showing Online devices");
            showDevicePages();
        } else {
            // this looks really ugly :(
            if (items.get(1).equals("backPagination")) {
                currentIndex = Math.max(0, currentIndex - 1);
                showDevicePages();

            } else if (items.get(1).equals("nextPagination")) {
                currentIndex += 1;
                showDevicePages();

            } else if (onlineDevices.contains(items.get(1))){
                sendHub(ACLMessage.REQUEST, "button pressed", items.get(1));
                newKb.setReplyMarkup(returnMainMenu);
                newTxt.setText("Sending msg to " + items.get(1));
            }
        }


    }
    // this looks really ugly :(

    private void handleSettings(String path){
        List<String> items = List.of(path.split("/"));
        // the root
        if (items.size() == 1) {
            InlineKeyboardButton maxPerPageBtn = InlineKeyboardButton.builder().text("Page limit").callbackData("settings/pageLimit").build();
            InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder().keyboardRow(List.of(maxPerPageBtn)).keyboardRow(List.of(returnMainMenuBtn)).build();
            newKb.setReplyMarkup(keyboardMarkup);
        } else {
            switch (items.get(1)) {
                case "pageLimit" ->{
                    // ideally would be set using buttons, but it's too complex
                    newTxt.setText("Current page limit is " + pageLimit +  "\n" +Emoji.NERD + Emoji.FINGER_UP +  "Change it with /setpagelimit <newLimit>");
                    newKb.setReplyMarkup(returnMainMenu);
                }
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

    private InlineKeyboardMarkup makeButtonList(List<String> devices, boolean usesPagination, int currentPage) {
        List<InlineKeyboardButton> buttons = new ArrayList<>();
        for (String device : devices) {
            buttons.add(InlineKeyboardButton.builder().text(device).callbackData("devices/"+device).build());
        }
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder keyboardBuilder = InlineKeyboardMarkup.builder();

        if (buttons.size() > 0) {

            int maxPage = usesPagination ? Math.min(currentPage* pageLimit + pageLimit, buttons.size()) : buttons.size();

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
        }

        keyboardBuilder.keyboardRow(List.of(returnMainMenuBtn));
        return keyboardBuilder.build();
    }

    private void showDevicePages(){
        InlineKeyboardMarkup deviceKeyboard = makeButtonList(onlineDevices, onlineDevices.size() > pageLimit, currentIndex);
        newKb.setReplyMarkup(deviceKeyboard);
    }

    public void sendHub(int performative, String content, String device){
        ACLMessage msg = new ACLMessage();
        msg.setSender(new AID(getLocalName(), AID.ISLOCALNAME));
        msg.addReceiver(new AID(hub, AID.ISLOCALNAME));
        msg.setContent("COMMAND "+ content + " | " + device);
        msg.setPerformative(ACLMessage.REQUEST);
        sendMsg(msg);
    }
}
