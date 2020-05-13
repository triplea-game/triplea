package org.triplea.modules.chat;

import java.net.InetAddress;
import javax.websocket.Session;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.domain.data.ChatParticipant;

@Getter
@Builder
@EqualsAndHashCode
public class ChatterSession {
  private final ChatParticipant chatParticipant;
  private final Session session;
  private final int apiKeyId;
  private final InetAddress ip;
}
