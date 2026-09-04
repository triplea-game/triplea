package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.net.Messengers;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** Logic for performing political actions. */
public interface IPoliticsDelegate extends IRemote, IDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods. Registration is
   * unconditional: the registry overwrites any prior handler, refreshing per-game captures on a
   * later game.
   */
  static void registerHandlers(final Messengers messengers) {
    messengers.registerMessageHandler(
        AttemptActionRequest.TYPE,
        (request, implementor) -> {
          ((IPoliticsDelegate) implementor).attemptAction(request.getActionChoice());
          return new AttemptActionResponse();
        });
    messengers.registerMessageHandler(
        GetValidActionsRequest.TYPE,
        (request, implementor) ->
            new GetValidActionsResponse(
                List.copyOf(((IPoliticsDelegate) implementor).getValidActions())));
  }

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

  Collection<PoliticalActionAttachment> getValidActions();

  /** Typed request for the currently valid political actions. */
  class GetValidActionsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544671223L;

    public static final MessageType<GetValidActionsRequest> TYPE =
        MessageType.of(GetValidActionsRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed reply carrying the valid political actions. The attachments ride the Java wire as they
   * did under the reflective path, so this has no Gson fixture.
   */
  @AllArgsConstructor
  class GetValidActionsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544671224L;

    public static final MessageType<GetValidActionsResponse> TYPE =
        MessageType.of(GetValidActionsResponse.class);

    @Getter private final List<PoliticalActionAttachment> validActions;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Override
  void initialize(String name, String displayName);

  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @Override
  void start();

  @Override
  void end();

  @Override
  String getName();

  @Override
  String getDisplayName();

  @Override
  IDelegateBridge getBridge();

  @Override
  Serializable saveState();

  @Override
  void loadState(Serializable state);

  @Override
  Class<? extends IRemote> getRemoteType();

  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
