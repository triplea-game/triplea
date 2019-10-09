package games.strategy.engine.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;

@AllArgsConstructor
@Getter
class ChatMessage {
  private final String message;
  private final PlayerName from;
}
