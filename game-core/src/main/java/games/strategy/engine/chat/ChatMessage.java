package games.strategy.engine.chat;

import games.strategy.engine.lobby.PlayerName;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class ChatMessage {
  private final String message;
  private final PlayerName from;
}
