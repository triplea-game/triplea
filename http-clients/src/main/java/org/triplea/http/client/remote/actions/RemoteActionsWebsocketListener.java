package org.triplea.http.client.remote.actions;

import java.net.URI;
import lombok.extern.java.Log;
import org.triplea.http.client.remote.actions.messages.server.ServerRemoteActionMessageType;
import org.triplea.http.client.web.socket.WebsocketListener;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Listens to a websocket for remote actions, eg: player banned. */
@Log
public class RemoteActionsWebsocketListener
    extends WebsocketListener<ServerRemoteActionMessageType, RemoteActionListeners> {

  public static final String NOTIFICATIONS_WEBSOCKET_PATH = "/remote/actions/ws";

  public RemoteActionsWebsocketListener(
      final URI serverUri, final RemoteActionListeners remoteActionListeners) {
    super(serverUri, NOTIFICATIONS_WEBSOCKET_PATH, remoteActionListeners);
  }

  @Override
  protected ServerRemoteActionMessageType readMessageType(
      final ServerMessageEnvelope serverMessageEnvelope) {
    return ServerRemoteActionMessageType.valueOf(serverMessageEnvelope.getMessageType());
  }
}
