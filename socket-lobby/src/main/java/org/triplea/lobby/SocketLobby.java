package org.triplea.lobby;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/game")
public class SocketLobby {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @OnOpen
  public void onOpen(final Session session) {
    logger.info("Lobby connected with Connected ... " + session.getId());
  }

  @OnMessage
  public String onMessage(final String message, final Session session) {
    logger.info("Received message: " + message);
    switch (message) {
      case "quit":
        try {
          session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Game ended"));
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
        break;
    }
    return message;
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    logger.info(String.format("Session %s closed because of %s", session.getId(), closeReason));
  }
}
