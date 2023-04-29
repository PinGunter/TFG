package notifiers;

import io.github.cdimascio.dotenv.Dotenv;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.function.Consumer;

public class TelegramBot extends TelegramLongPollingBot {
    Consumer<Update> onUpdate;


    String name, token;

    public TelegramBot(Consumer<Update> onUpdate) {
        this.onUpdate = onUpdate;
        Dotenv dotenv = Dotenv.load();
        name = dotenv.get("TELEGRAM_BOT_NAME");
        token = dotenv.get("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        onUpdate.accept(update);
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what)
                .build();
        myExecute(sm);
    }

    public void sendVoiceMsg(Long who, InputFile audio) {
        SendVoice sendVoice = new SendVoice(who.toString(), audio);
        myExecute(sendVoice);
    }

    public void sendPhoto(Long who, InputFile photo) {
        SendPhoto sendPhoto = new SendPhoto(who.toString(), photo);
        myExecute(sendPhoto);
    }

    public void sendAnimation(Long who, InputFile animation) {
        SendAnimation sendAnimation = new SendAnimation(who.toString(), animation);
        myExecute(sendAnimation);
    }

    public void sendMediaGroup(Long who, List<InputMedia> media) {
        SendMediaGroup sendMediaGroup = new SendMediaGroup(who.toString(), media);
        myExecute(sendMediaGroup);
    }

    public void sendVoiceMsgKbMarkup(Long who, InputFile audio, InlineKeyboardMarkup kb) {
        SendVoice sendVoice = new SendVoice(who.toString(), audio);
        sendVoice.setReplyMarkup(kb);
        myExecute(sendVoice);
    }

    public void sendPhotoKbMarkup(Long who, InputFile photo, InlineKeyboardMarkup kb) {
        SendPhoto sendPhoto = new SendPhoto(who.toString(), photo);
        sendPhoto.setReplyMarkup(kb);
        myExecute(sendPhoto);
    }

    public void sendWithReplyMenu(Long who, String what, InlineKeyboardMarkup kb) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .parseMode("HTML")
                .text(what)
                .replyMarkup(kb)
                .build();
        myExecute(sm);
    }

    public void sendWithKeyboard(Long who, String what, ReplyKeyboard kb) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .parseMode("HTML")
                .text(what)
                .replyMarkup(kb)
                .build();
        myExecute(sm);
    }


    public void myExecute(BotApiMethod method) {
        try {
            this.execute(method);
        } catch (TelegramApiException e) {
            System.err.println(e.getMessage());
        }
    }

    public void myExecute(SendVoice method) {
        try {
            this.execute(method);
        } catch (TelegramApiException e) {
            System.err.println(e.getMessage());
        }
    }

    public void myExecute(SendAnimation method) {
        try {
            this.execute(method);
        } catch (TelegramApiException e) {
            System.err.println(e.getMessage());
        }
    }

    public void myExecute(SendPhoto method) {
        try {
            this.execute(method);
        } catch (TelegramApiException e) {
            System.err.println(e.getMessage());
        }
    }

    public void myExecute(SendMediaGroup method) {
        try {
            this.execute(method);
        } catch (TelegramApiException e) {
            System.err.println(e.getMessage());
        }
    }

}
