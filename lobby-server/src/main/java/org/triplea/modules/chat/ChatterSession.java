package org.triplea.modules.chat;

import java.net.InetAddress;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.web.socket.WebSocketSession;

@Getter
@Builder
@EqualsAndHashCode
public class ChatterSession {
  private final ChatParticipant chatParticipant;
  private final WebSocketSession session;
  private final int apiKeyId;
  private final InetAddress ip;
}
