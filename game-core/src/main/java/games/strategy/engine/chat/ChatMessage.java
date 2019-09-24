package games.strategy.engine.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class ChatMessage {
  private final String message;
  private final String from;
}
