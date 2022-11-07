package games.strategy.engine.chat;

import lombok.Value;
import org.triplea.domain.data.UserName;

@Value
public class ChatMessage {
  UserName from;
  String message;
}
