package org.triplea.http.client.remote.actions.messages.server;

import java.net.InetAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import org.triplea.http.client.remote.actions.RemoteActionListeners;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;
import org.triplea.http.client.web.socket.messages.WebsocketMessageWrapper;

/** Types of messages that can be sent from server to client indicating a 'remote action' */
@SuppressWarnings("ImmutableEnumChecker")
public enum ServerRemoteActionMessageType implements WebsocketMessageType<RemoteActionListeners> {
  /** Requests that the server receiving this message to disconnect and shutdown. */
  SHUTDOWN(String.class, RemoteActionListeners::getShutdownListener),

  /** Indicates a player has been banned and they should be disconnected if present. */
  PLAYER_BANNED(InetAddress.class, RemoteActionListeners::getBannedPlayerListener);

  private final WebsocketMessageWrapper<RemoteActionListeners, ?> websocketMessageWrapper;

  <X> ServerRemoteActionMessageType(
      final Class<X> classType, final Function<RemoteActionListeners, Consumer<X>> listenerMethod) {
    this.websocketMessageWrapper =
        new WebsocketMessageWrapper<>(classType, listenerMethod, toString());
  }

  @Override
  public void sendPayloadToListener(
      final ServerMessageEnvelope serverMessageEnvelope, final RemoteActionListeners listener) {
    websocketMessageWrapper.sendPayloadToListener(serverMessageEnvelope, listener);
  }
}
