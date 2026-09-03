package games.strategy.engine.chat;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.lobby.web.socket.messages.envelopes.chat.ChatParticipant;
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

  /** Typed request asking to join the chat. */
  class JoinChatRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6551002233118845990L;

    public static final MessageType<JoinChatRequest> TYPE = MessageType.of(JoinChatRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the chatters currently in the chat. */
  @AllArgsConstructor
  class JoinChatResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8890274416600335521L;

    public static final MessageType<JoinChatResponse> TYPE = MessageType.of(JoinChatResponse.class);

    @Getter private final List<ChatParticipant> chatParticipants;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(1)
  void leaveChat();

  /** Typed request asking to leave the chat. */
  class LeaveChatRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1225884430091771404L;

    public static final MessageType<LeaveChatRequest> TYPE = MessageType.of(LeaveChatRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the leave request was applied. */
  class LeaveChatResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5090441233188730022L;

    public static final MessageType<LeaveChatResponse> TYPE =
        MessageType.of(LeaveChatResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(2)
  void setStatus(String newStatus);

  @AllArgsConstructor
  class SetChatStatusMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3145992801994551277L;

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

  /** Typed acknowledgement that the status change was applied. */
  class SetStatusResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7712004553330019918L;

    public static final MessageType<SetStatusResponse> TYPE =
        MessageType.of(SetStatusResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** A tag associated with a chat participant indicating the participant's role. */
  enum Tag {
    MODERATOR,
    NONE
  }
}
