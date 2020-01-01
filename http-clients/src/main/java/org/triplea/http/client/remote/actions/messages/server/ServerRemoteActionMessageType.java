package org.triplea.http.client.remote.actions.messages.server;

import static org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding.newBinding;

import java.net.InetAddress;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Types of messages that can be sent from server to client indicating a 'remote action' */
@Getter(onMethod_ = @Override)
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
public enum ServerRemoteActionMessageType implements WebsocketMessageType<RemoteActionListeners> {
  /** Requests that the server receiving this message to disconnect and shutdown. */
  SHUTDOWN(newBinding(String.class, RemoteActionListeners::getShutdownListener)),

  /** Indicates a player has been banned and they should be disconnected if present. */
  PLAYER_BANNED(newBinding(InetAddress.class, RemoteActionListeners::getBannedPlayerListener)),
  ;

  private final MessageTypeListenerBinding<RemoteActionListeners, ?> messageTypeListenerBinding;
}
