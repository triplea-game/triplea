package org.triplea.http.client.remote.actions.messages.server;

import java.net.InetAddress;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@UtilityClass
public class RemoteActionsEnvelopeFactory {

  public ServerMessageEnvelope newShutdownMessage() {
    return ServerMessageEnvelope.packageMessage(
        ServerRemoteActionMessageType.SHUTDOWN.toString(), "");
  }

  public ServerMessageEnvelope newBannedPlayer(final InetAddress bannedIp) {
    return ServerMessageEnvelope.packageMessage(
        ServerRemoteActionMessageType.PLAYER_BANNED.toString(), bannedIp);
  }
}
