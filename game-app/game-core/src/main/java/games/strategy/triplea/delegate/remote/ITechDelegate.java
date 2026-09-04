package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.Messengers;
import games.strategy.triplea.delegate.data.TechResults;
import java.io.Serial;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.IntegerMap;

/** Logic for spending tech tokens. */
public interface ITechDelegate extends IRemote, IDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods, guarded for idempotency.
   * The tech frontier and the per-player payment map ride the Java wire as they did under the
   * reflective path, so these messages carry no Gson fixture.
   */
  static void registerHandlers(final Messengers messengers) {
    if (!messengers.hasTypedMessageHandler(RollTechRequest.TYPE)) {
      messengers.registerMessageHandler(
          RollTechRequest.TYPE,
          (request, implementor) ->
              new RollTechResponse(
                  ((ITechDelegate) implementor)
                      .rollTech(
                          request.getRollCount(),
                          request.getTechToRollFor(),
                          request.getNewTokens(),
                          request.getWhoPaysHowMuch())));
    }
  }

  /**
   * Rolls for the specified tech.
   *
   * @param rollCount the number of tech rolls
   * @param techToRollFor the tech category to roll for, should be null if the game does not support
   *     rolling for certain techs
   * @param newTokens if WW2V3TechModel is used it set rollCount
   * @return TechResults. If the tech could not be rolled, then a message saying why.
   */
  @RemoteActionCode(8)
  TechResults rollTech(
      int rollCount,
      TechnologyFrontier techToRollFor,
      int newTokens,
      IntegerMap<GamePlayer> whoPaysHowMuch);

  /** Typed request to roll for tech. */
  @AllArgsConstructor
  class RollTechRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544674221L;

    public static final MessageType<RollTechRequest> TYPE = MessageType.of(RollTechRequest.class);

    @Getter private final int rollCount;
    @Getter @Nullable private final TechnologyFrontier techToRollFor;
    @Getter private final int newTokens;
    @Getter private final IntegerMap<GamePlayer> whoPaysHowMuch;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply carrying the tech-roll results. */
  @AllArgsConstructor
  class RollTechResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544674222L;

    public static final MessageType<RollTechResponse> TYPE = MessageType.of(RollTechResponse.class);

    @Getter private final TechResults techResults;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(6)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(10)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(11)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(4)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(9)
  @Override
  Serializable saveState();

  @RemoteActionCode(7)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(5)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
