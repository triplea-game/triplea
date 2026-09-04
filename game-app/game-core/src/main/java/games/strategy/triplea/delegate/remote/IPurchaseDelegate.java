package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.Messengers;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.collections.IntegerMap;

/** Logic for purchasing and repairing units. */
public interface IPurchaseDelegate extends IAbstractForumPosterDelegate {
  /**
   * Registers the typed handlers for this delegate's converted methods. Registration is
   * unconditional: the registry overwrites any prior handler, refreshing per-game captures on a
   * later game. The production/repair maps ride the Java wire (as they did under the reflective
   * path), so these messages carry no Gson fixture.
   */
  static void registerHandlers(final Messengers messengers) {
    messengers.registerMessageHandler(
        PurchaseRequest.TYPE,
        (request, implementor) ->
            new PurchaseResponse(
                ((IPurchaseDelegate) implementor).purchase(request.getProductionRules())));
    messengers.registerMessageHandler(
        PurchaseRepairRequest.TYPE,
        (request, implementor) ->
            new PurchaseRepairResponse(
                ((IPurchaseDelegate) implementor).purchaseRepair(request.getProductionRules())));
  }

  /**
   * Purchases the specified units.
   *
   * @param productionRules - units maps ProductionRule -> count.
   * @return null if units bought, otherwise an error message
   */
  @RemoteActionCode(10)
  @Nullable
  String purchase(IntegerMap<ProductionRule> productionRules);

  /** Typed request to purchase units (the production map rides the Java wire). */
  @AllArgsConstructor
  class PurchaseRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544680221L;

    public static final MessageType<PurchaseRequest> TYPE = MessageType.of(PurchaseRequest.class);

    @Getter private final IntegerMap<ProductionRule> productionRules;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the purchase succeeded. */
  @AllArgsConstructor
  class PurchaseResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544680222L;

    public static final MessageType<PurchaseResponse> TYPE = MessageType.of(PurchaseResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Returns an error code, or null if all is good. */
  @RemoteActionCode(11)
  @Nullable
  String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> productionRules);

  /** Typed request to purchase repairs (the repair map rides the Java wire). */
  @AllArgsConstructor
  class PurchaseRepairRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544680223L;

    public static final MessageType<PurchaseRepairRequest> TYPE =
        MessageType.of(PurchaseRepairRequest.class);

    @Getter private final Map<Unit, IntegerMap<RepairRule>> productionRules;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed reply with an error string, or null when the repair purchase succeeded. */
  @AllArgsConstructor
  class PurchaseRepairResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5320048576544680224L;

    public static final MessageType<PurchaseRepairResponse> TYPE =
        MessageType.of(PurchaseRepairResponse.class);

    @Getter @Nullable private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @RemoteActionCode(14)
  @Override
  void setHasPostedTurnSummary(boolean hasPostedTurnSummary);

  @RemoteActionCode(7)
  @Override
  void initialize(String name, String displayName);

  @RemoteActionCode(13)
  @Override
  void setDelegateBridgeAndPlayer(IDelegateBridge delegateBridge);

  @RemoteActionCode(15)
  @Override
  void start();

  @RemoteActionCode(1)
  @Override
  void end();

  @RemoteActionCode(5)
  @Override
  String getName();

  @RemoteActionCode(3)
  @Override
  String getDisplayName();

  @RemoteActionCode(2)
  @Override
  IDelegateBridge getBridge();

  @RemoteActionCode(12)
  @Override
  Serializable saveState();

  @RemoteActionCode(8)
  @Override
  void loadState(Serializable state);

  @RemoteActionCode(6)
  @Override
  Class<? extends IRemote> getRemoteType();

  @RemoteActionCode(0)
  @Override
  boolean delegateCurrentlyRequiresUserInput();
}
