package agents;

import notifiers.TelegramBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramAgent extends NotifierAgent{
    TelegramBot bot;

    @Override
    public void setup(){
        super.setup();
        bot = new TelegramBot(this::onReceiveMessage);
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            logger.error(e.getMessage());
            exit = true;
        }
    }

    public void onReceiveMessage(Update update){
        System.out.println(update.getMessage().getText());
        if (update.getMessage().isCommand()){
            if (update.getMessage().getText().equals("/stopjade")){
                bot.sendText(update.getMessage().getFrom().getId(), "Good bye, JADE is stopping");
                exit = true;
            }
        }
    }
}
