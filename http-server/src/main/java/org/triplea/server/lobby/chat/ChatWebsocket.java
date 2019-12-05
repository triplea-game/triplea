package org.triplea.server.lobby.chat;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.chat.LobbyChatClient;

/**
 * Handles chat connections. Largely delegates to {@see MessagingService}. A shared {@code
 * MessagingService} is injected into each user session and is available from {@code Session}
 * objects.
 */
@Slf4j
@ServerEndpoint(LobbyChatClient.LOBBY_CHAT_WEBSOCKET_PATH)
public class ChatWebsocket {
  public static final String MESSAGING_SERVICE_KEY = "messaging_service";

  @OnOpen
  public void open(final Session session) {
    log.info(
        "New websocket connection from IP: " + InetExtractor.extract(session.getUserProperties()));
    // TODO: Project#12 do filtering for banned IPs (check if filter can kick in first)
    // TODO: Project#12 make sure failed api key validation attempts become blacklisted
  }

  @OnMessage
  public void message(final Session session, final String message) {
    log.info("Chat message received: " + message);
    ((MessagingService) session.getUserProperties().get(MESSAGING_SERVICE_KEY))
        .handleMessage(session, message);
  }

  @OnClose
  public void close(final Session session, final CloseReason closeReason) {
    log.info(
        "IP disconnected: {}, {}", InetExtractor.extract(session.getUserProperties()), closeReason);
    ((MessagingService) session.getUserProperties().get(MESSAGING_SERVICE_KEY))
        .handleDisconnect(session);
  }

  /**
   * This error handler is called automatically when server processing encounters an uncaught
   * exception. We use it to notify the user that an error occurred.
   */
  @OnError
  public void handleError(final Session session, final Throwable throwable) {
    log.warn("Notifying user of an error", throwable);
    ((MessagingService) session.getUserProperties().get(MESSAGING_SERVICE_KEY))
        .handleError(session, throwable);
  }
}
