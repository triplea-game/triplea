package org.triplea.http.client.remote.actions;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.http.client.remote.actions.messages.server.ServerRemoteActionMessageType;
import org.triplea.http.client.web.socket.GenericWebSocketClient;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Listens to a websocket for remote actions, eg: player banned. */
@Log
public class RemoteActionsWebsocketListener implements Consumer<ServerMessageEnvelope> {
  public static final String NOTIFICATIONS_WEBSOCKET_PATH = "/remote/actions/ws";

  private final Collection<Runnable> shutdownListeners = new HashSet<>();
  private final Collection<Consumer<InetAddress>> bannedPlayerListeners = new HashSet<>();

  private final GenericWebSocketClient remoteActionsWebsocket;

  public RemoteActionsWebsocketListener(final URI serverUri) {
    this(
        new GenericWebSocketClient(
            URI.create(serverUri + NOTIFICATIONS_WEBSOCKET_PATH), "Failed to connect to lobby."));
  }

  @VisibleForTesting
  RemoteActionsWebsocketListener(final GenericWebSocketClient genericWebSocketClient) {
    this.remoteActionsWebsocket = genericWebSocketClient;
    this.remoteActionsWebsocket.addMessageListener(this);
  }

  public void addShutdownRequestListener(final Runnable listener) {
    shutdownListeners.add(listener);
  }

  public void addPlayerBannedListener(final Consumer<InetAddress> listener) {
    bannedPlayerListeners.add(listener);
  }

  public void stopListening() {
    remoteActionsWebsocket.close();
  }

  @Override
  public void accept(final ServerMessageEnvelope serverMessageEnvelope) {
    extractMessageType(serverMessageEnvelope)
        .ifPresent(
            messageType -> {
              switch (messageType) {
                case SHUTDOWN:
                  shutdownListeners.forEach(Runnable::run);
                  break;
                case PLAYER_BANNED:
                  final InetAddress bannedIp = serverMessageEnvelope.getPayload(InetAddress.class);
                  bannedPlayerListeners.forEach(listener -> listener.accept(bannedIp));
                  break;
                default:
                  log.severe(
                      "Unhandled remote action message type: "
                          + serverMessageEnvelope.getMessageType());
              }
            });
  }

  private Optional<ServerRemoteActionMessageType> extractMessageType(
      final ServerMessageEnvelope serverMessageEnvelope) {
    try {
      return Optional.of(
          ServerRemoteActionMessageType.valueOf(serverMessageEnvelope.getMessageType()));
    } catch (final IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }
}
