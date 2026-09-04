package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.Messengers;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Logic for performing political actions. */
public interface IPoliticsDelegate extends IRemote, IDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods, guarded for idempotency.
   */
  static void registerHandlers(final Messengers messengers) {
    if (!messengers.hasTypedMessageHandler(AttemptActionRequest.TYPE)) {
      messengers.registerMessageHandler(
          AttemptActionRequest.TYPE,
          (request, implementor) -> {
            ((IPoliticsDelegate) implementor).attemptAction(request.getActionChoice());
            return new AttemptActionResponse();
          });
    }
  }

  @RemoteActionCode(0)
  void attemptAction(PoliticalActionAttachment actionChoice);

  /** Typed request to perform a political action. */
  @AllArgsConstructor
  class AttemptActionRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544671221L;

    public static final MessageType<AttemptActionRequest> TYPE =
        MessageType.of(AttemptActionRequest.class);

    @Getter private final PoliticalActionAttachment actionChoice;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the political action was applied. */
  class AttemptActionResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544671222L;

    public static final MessageType<AttemptActionResponse> TYPE =
        MessageType.of(AttemptActionResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(7)
  Collection<PoliticalActionAttachment> getValidActions();

  @RemoteActionCode(8)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(11)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(12)
  @Override
  void start();

  @RemoteActionCode(2)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(4)
  @Override
  String getDisplayName();

  @RemoteActionCode(3)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(10)
  @Override
  Serializable saveState();

  @RemoteActionCode(9)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(1)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
