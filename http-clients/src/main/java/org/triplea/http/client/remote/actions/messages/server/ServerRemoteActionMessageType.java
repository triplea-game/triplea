package org.triplea.http.client.remote.actions.messages.server;

import java.net.InetAddress;
import java.util.function.Consumer;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.remote.actions.RemoteActionListeners;
import org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Types of messages that can be sent from server to client indicating a 'remote action' */
@Getter(onMethod_ = @Override)
@AllArgsConstructor
public enum ServerRemoteActionMessageType implements WebsocketMessageType<RemoteActionListeners> {
  /** Requests that the server receiving this message to disconnect and shutdown. */
  SHUTDOWN(MessageTypeListenerBinding.of(String.class, RemoteActionListeners::getShutdownListener)),

  /** Indicates a player has been banned and they should be disconnected if present. */
  PLAYER_BANNED(MessageTypeListenerBinding.of(InetAddress.class, RemoteActionListeners::getBannedPlayerListener));

  private final MessageTypeListenerBinding<RemoteActionListeners, ?> messageTypeListenerBinding;
}
