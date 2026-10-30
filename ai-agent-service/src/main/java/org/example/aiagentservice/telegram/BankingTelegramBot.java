package org.example.aiagentservice.telegram;

import jakarta.annotation.PostConstruct;
import org.example.aiagentservice.agent.BankingAiAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@ConditionalOnExpression("'${telegram.bot-token:}' != ''")
public class BankingTelegramBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(BankingTelegramBot.class);
    private static final int TELEGRAM_MESSAGE_LIMIT = 4000;

    private final BankingAiAgent agent;
    private final String username;
    private final String token;
    private final TelegramClient telegramClient;

    public BankingTelegramBot(
            BankingAiAgent agent,
            @Value("${telegram.bot-username}") String username,
            @Value("${telegram.bot-token}") String token) {
        this.agent = agent;
        this.username = username;
        this.token = token;
        this.telegramClient = new OkHttpTelegramClient(token);
    }

    @PostConstruct
    public void register() {
        try {
            TelegramBotsLongPollingApplication bots = new TelegramBotsLongPollingApplication();
            bots.registerBot(token, this);
            log.info("Bot Telegram enregistré : {}", username);
        } catch (TelegramApiException exception) {
            log.error("Enregistrement Telegram impossible : {}", exception.getMessage());
        }
    }

    @Override
    public void consume(Update update) {
        if (update == null || !update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        try {
            telegramClient.execute(SendChatAction.builder()
                    .chatId(chatId)
                    .action(ActionType.TYPING.toString())
                    .build());
            sendText(chatId, agent.chat(text, "telegram:" + chatId));
        } catch (Exception exception) {
            log.warn("Message Telegram non traité : {}", exception.toString());
            try {
                sendText(chatId, "L'assistant est indisponible pour le moment.");
            } catch (TelegramApiException sendFailure) {
                log.warn("Réponse d'erreur Telegram non envoyée : {}", sendFailure.getMessage());
            }
        }
    }

    private void sendText(Long chatId, String text) throws TelegramApiException {
        String remaining = text == null || text.isBlank() ? "Je n'ai pas de réponse." : text;
        while (!remaining.isEmpty()) {
            int end = Math.min(TELEGRAM_MESSAGE_LIMIT, remaining.length());
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(remaining.substring(0, end))
                    .build());
            remaining = remaining.substring(end);
        }
    }
}
