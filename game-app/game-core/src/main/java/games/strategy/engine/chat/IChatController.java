package games.strategy.engine.chat;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import java.util.Collection;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A central controller of who is in the chat.
 *
 * <p>When joining you get a list of all the players currently in the chat and their statuses.
 */
public interface IChatController extends IRemote {
  /** Join the chat, returns the chatters currently in the chat. */
  @RemoteActionCode(0)
  Collection<ChatParticipant> joinChat();

  @RemoteActionCode(1)
  void leaveChat();

  @RemoteActionCode(2)
  void setStatus(String newStatus);

  @AllArgsConstructor
  class SetChatStatusMessage implements WebSocketMessage {
    public static final MessageType<SetChatStatusMessage> TYPE =
        MessageType.of(SetChatStatusMessage.class);

    @Nonnull private final String status;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }

    public void invokeCallback(IChatController iChatController) {
      iChatController.setStatus(status);
    }
  }

  /** A tag associated with a chat participant indicating the participant's role. */
  enum Tag {
    MODERATOR,
    NONE
  }
}
