package games.strategy.engine.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.net.Messengers;
import games.strategy.triplea.ai.weak.WeakAi;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Central registration point and dispatcher for the typed {@link Player} remote messages, mirroring
 * {@code DelegateRemoteMessageHandlers}. Called from {@code AbstractGame} as each local player
 * endpoint is registered; the registration is guarded so re-running it across players and across
 * games is a harmless no-op.
 *
 * <p>Handlers are keyed by message type on the session-scoped registry and dispatch to whichever
 * player the addressed per-name endpoint currently holds, so a single registration per type serves
 * every player of that name. The call sites reach a player through {@link
 * IDelegateBridge#invokeRemotePlayer}, so the same typed request is applied locally under the
 * simulation/odds bridges and sent over the wire under the networked bridge.
 */
@UtilityClass
public class PlayerRemoteMessageHandlers {

  /** Registers every converted {@link Player} message handler, guarded for idempotency. */
  public static void registerAll(final Messengers messengers, final GameData gameData) {
    register(messengers, Player.AcceptActionRequest.TYPE, gameData);
    register(messengers, Player.SelectAttackSubsRequest.TYPE, gameData);
    register(messengers, Player.SelectAttackTransportsRequest.TYPE, gameData);
    register(messengers, Player.SelectAttackUnitsRequest.TYPE, gameData);
    register(messengers, Player.SelectShoreBombardRequest.TYPE, gameData);
    register(messengers, Player.ShouldBomberBombRequest.TYPE, gameData);
    register(messengers, Player.ConfirmMoveInFaceOfAaRequest.TYPE, gameData);
    register(messengers, Player.ConfirmMoveKamikazeRequest.TYPE, gameData);
    register(messengers, Player.WhereShouldRocketsAttackRequest.TYPE, gameData);
    register(messengers, Player.WhatShouldBomberBombRequest.TYPE, gameData);
    register(messengers, Player.GetNumberOfFightersToMoveToNewCarrierRequest.TYPE, gameData);
    register(messengers, Player.SelectTerritoryForAirToLandRequest.TYPE, gameData);
    register(messengers, Player.SelectBombardingTerritoryRequest.TYPE, gameData);
    register(messengers, Player.SelectUnitsQueryRequest.TYPE, gameData);
    register(messengers, Player.SelectFixedDiceRequest.TYPE, gameData);
    register(messengers, Player.GetGamePlayerRequest.TYPE, gameData);
    register(messengers, Player.ReportErrorRequest.TYPE, gameData);
    register(messengers, Player.ReportMessageRequest.TYPE, gameData);
    register(messengers, Player.ConfirmEnemyCasualtiesRequest.TYPE, gameData);
    register(messengers, Player.ConfirmOwnCasualtiesRequest.TYPE, gameData);
  }

  private static <T extends WebSocketMessage> void register(
      final Messengers messengers, final MessageType<T> type, final GameData gameData) {
    if (!messengers.hasTypedMessageHandler(type)) {
      messengers.registerMessageHandler(
          type, (message, implementor) -> handle(message, (Player) implementor, gameData));
    }
  }

  /**
   * Sends a typed request to a player and blocks for its reply, choosing the null player's
   * do-nothing behavior locally (it has no registered remote), otherwise the networked endpoint.
   * Used by the networked {@link IDelegateBridge}; the local/simulation bridges apply the request
   * in process via {@link #applyLocally}.
   */
  public static <R extends WebSocketMessage> R invokeRemotePlayer(
      final Messengers messengers,
      final GameData gameData,
      final GamePlayer player,
      final WebSocketMessage request,
      final MessageType<R> responseType) {
    if (player.isNull()) {
      return applyLocally(request, new WeakAi(player.getName()), gameData, responseType);
    }
    return messengers.invokeRemoteMessage(ServerGame.getRemoteName(player), request, responseType);
  }

  /** Applies a typed request to an in-process player, returning the typed reply. */
  public static <R extends WebSocketMessage> R applyLocally(
      final WebSocketMessage request,
      final Player player,
      final GameData gameData,
      final MessageType<R> responseType) {
    return responseType.getPayloadType().cast(handle(request, player, gameData));
  }

  /** Resolves a typed request against a player, returning the typed reply. */
  private static WebSocketMessage handle(
      final WebSocketMessage request, final Player player, final GameData data) {
    return switch (request) {
      case Player.AcceptActionRequest r ->
          new Player.BooleanResponse(
              player.acceptAction(
                  r.getPlayerSendingProposal().resolvePlayer(data),
                  r.getAcceptanceQuestion(),
                  r.isPolitics()));
      case Player.SelectAttackSubsRequest r ->
          new Player.BooleanResponse(
              player.selectAttackSubs(r.getUnitTerritory().resolveTerritory(data)));
      case Player.SelectAttackTransportsRequest r ->
          new Player.BooleanResponse(
              player.selectAttackTransports(r.getUnitTerritory().resolveTerritory(data)));
      case Player.SelectAttackUnitsRequest r ->
          new Player.BooleanResponse(
              player.selectAttackUnits(r.getUnitTerritory().resolveTerritory(data)));
      case Player.SelectShoreBombardRequest r ->
          new Player.BooleanResponse(
              player.selectShoreBombard(r.getUnitTerritory().resolveTerritory(data)));
      case Player.ShouldBomberBombRequest r ->
          new Player.BooleanResponse(
              player.shouldBomberBomb(r.getTerritory().resolveTerritory(data)));
      case Player.ConfirmMoveInFaceOfAaRequest r ->
          new Player.BooleanResponse(
              player.confirmMoveInFaceOfAa(resolveTerritories(r.getAaFiringTerritories(), data)));
      case Player.ConfirmMoveKamikazeRequest ignored ->
          new Player.BooleanResponse(player.confirmMoveKamikaze());
      case Player.WhereShouldRocketsAttackRequest r ->
          territoryResponse(
              player.whereShouldRocketsAttack(
                  resolveTerritories(r.getCandidates(), data), resolveNullable(r.getFrom(), data)));
      case Player.WhatShouldBomberBombRequest r ->
          unitResponse(
              player.whatShouldBomberBomb(
                  r.getTerritory().resolveTerritory(data),
                  resolveUnits(r.getPotentialTargets(), data),
                  resolveUnits(r.getBombers(), data)));
      case Player.GetNumberOfFightersToMoveToNewCarrierRequest r ->
          new Player.UnitsResponse(
              unitRefs(
                  player.getNumberOfFightersToMoveToNewCarrier(
                      resolveUnits(r.getFightersThatCanBeMoved(), data),
                      r.getFrom().resolveTerritory(data))));
      case Player.SelectTerritoryForAirToLandRequest r ->
          territoryResponse(
              player.selectTerritoryForAirToLand(
                  resolveTerritories(r.getCandidates(), data),
                  r.getCurrentTerritory().resolveTerritory(data),
                  r.getUnitMessage()));
      case Player.SelectBombardingTerritoryRequest r ->
          territoryResponse(
              player.selectBombardingTerritory(
                  r.getUnit().resolveUnit(data),
                  r.getUnitTerritory().resolveTerritory(data),
                  resolveTerritories(r.getTerritories(), data),
                  r.isNoneAvailable()));
      case Player.SelectUnitsQueryRequest r ->
          new Player.UnitsResponse(
              unitRefs(
                  player.selectUnitsQuery(
                      r.getCurrent().resolveTerritory(data),
                      resolveUnits(r.getPossible(), data),
                      r.getMessage())));
      case Player.SelectFixedDiceRequest r ->
          new Player.IntArrayResponse(
              player.selectFixedDice(r.getNumDice(), r.getHitAt(), r.getTitle(), r.getDiceSides()));
      case Player.GetGamePlayerRequest ignored -> {
        final GamePlayer gamePlayer = player.getGamePlayer();
        yield new Player.PlayerResponse(gamePlayer == null ? null : EntityRef.of(gamePlayer));
      }
      case Player.ReportErrorRequest r -> {
        player.reportError(r.getError());
        yield new Player.VoidAck();
      }
      case Player.ReportMessageRequest r -> {
        player.reportMessage(r.getMessage(), r.getTitle());
        yield new Player.VoidAck();
      }
      case Player.ConfirmEnemyCasualtiesRequest r -> {
        player.confirmEnemyCasualties(
            r.getBattleId() == null ? null : UUID.fromString(r.getBattleId()),
            r.getMessage(),
            r.getHitPlayer().resolvePlayer(data));
        yield new Player.VoidAck();
      }
      case Player.ConfirmOwnCasualtiesRequest r -> {
        player.confirmOwnCasualties(
            r.getBattleId() == null ? null : UUID.fromString(r.getBattleId()), r.getMessage());
        yield new Player.VoidAck();
      }
      default ->
          throw new IllegalArgumentException(
              "Unhandled player message: " + request.getClass().getName());
    };
  }

  // --- Call-site facades: build the request, dispatch through the bridge, decode the reply. ---

  public static boolean acceptAction(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final GamePlayer playerSendingProposal,
      final String acceptanceQuestion,
      final boolean politics) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.AcceptActionRequest(
                EntityRef.of(playerSendingProposal), acceptanceQuestion, politics),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean selectAttackSubs(
      final IDelegateBridge bridge, final GamePlayer player, final Territory unitTerritory) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.SelectAttackSubsRequest(EntityRef.of(unitTerritory)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean selectAttackTransports(
      final IDelegateBridge bridge, final GamePlayer player, final Territory unitTerritory) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.SelectAttackTransportsRequest(EntityRef.of(unitTerritory)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean selectAttackUnits(
      final IDelegateBridge bridge, final GamePlayer player, final Territory unitTerritory) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.SelectAttackUnitsRequest(EntityRef.of(unitTerritory)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean selectShoreBombard(
      final IDelegateBridge bridge, final GamePlayer player, final Territory unitTerritory) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.SelectShoreBombardRequest(EntityRef.of(unitTerritory)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean shouldBomberBomb(
      final IDelegateBridge bridge, final GamePlayer player, final Territory territory) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.ShouldBomberBombRequest(EntityRef.of(territory)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean confirmMoveInFaceOfAa(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Collection<Territory> aaFiringTerritories) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.ConfirmMoveInFaceOfAaRequest(territoryRefs(aaFiringTerritories)),
            Player.BooleanResponse.TYPE)
        .isValue();
  }

  public static boolean confirmMoveKamikaze(final IDelegateBridge bridge, final GamePlayer player) {
    return bridge
        .invokeRemotePlayer(
            player, new Player.ConfirmMoveKamikazeRequest(), Player.BooleanResponse.TYPE)
        .isValue();
  }

  @Nullable
  public static Territory whereShouldRocketsAttack(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Collection<Territory> candidates,
      final @Nullable Territory from) {
    return decodeTerritory(
        bridge.invokeRemotePlayer(
            player,
            new Player.WhereShouldRocketsAttackRequest(
                territoryRefs(candidates), from == null ? null : EntityRef.of(from)),
            Player.TerritoryResponse.TYPE),
        bridge.getData());
  }

  @Nullable
  public static Unit whatShouldBomberBomb(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Territory territory,
      final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    final Player.UnitResponse response =
        bridge.invokeRemotePlayer(
            player,
            new Player.WhatShouldBomberBombRequest(
                EntityRef.of(territory), unitRefs(potentialTargets), unitRefs(bombers)),
            Player.UnitResponse.TYPE);
    return response.getUnit() == null ? null : response.getUnit().resolveUnit(bridge.getData());
  }

  public static Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Collection<Unit> fightersThatCanBeMoved,
      final Territory from) {
    return resolveUnits(
        bridge
            .invokeRemotePlayer(
                player,
                new Player.GetNumberOfFightersToMoveToNewCarrierRequest(
                    unitRefs(fightersThatCanBeMoved), EntityRef.of(from)),
                Player.UnitsResponse.TYPE)
            .getUnits(),
        bridge.getData());
  }

  @Nullable
  public static Territory selectTerritoryForAirToLand(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Collection<Territory> candidates,
      final Territory currentTerritory,
      final String unitMessage) {
    return decodeTerritory(
        bridge.invokeRemotePlayer(
            player,
            new Player.SelectTerritoryForAirToLandRequest(
                territoryRefs(candidates), EntityRef.of(currentTerritory), unitMessage),
            Player.TerritoryResponse.TYPE),
        bridge.getData());
  }

  @Nullable
  public static Territory selectBombardingTerritory(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Unit unit,
      final Territory unitTerritory,
      final Collection<Territory> territories,
      final boolean noneAvailable) {
    return decodeTerritory(
        bridge.invokeRemotePlayer(
            player,
            new Player.SelectBombardingTerritoryRequest(
                EntityRef.of(unit),
                EntityRef.of(unitTerritory),
                territoryRefs(territories),
                noneAvailable),
            Player.TerritoryResponse.TYPE),
        bridge.getData());
  }

  public static Collection<Unit> selectUnitsQuery(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final Territory current,
      final Collection<Unit> possible,
      final String message) {
    return resolveUnits(
        bridge
            .invokeRemotePlayer(
                player,
                new Player.SelectUnitsQueryRequest(
                    EntityRef.of(current), unitRefs(possible), message),
                Player.UnitsResponse.TYPE)
            .getUnits(),
        bridge.getData());
  }

  public static int[] selectFixedDice(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final int numDice,
      final int hitAt,
      final String title,
      final int diceSides) {
    return bridge
        .invokeRemotePlayer(
            player,
            new Player.SelectFixedDiceRequest(numDice, hitAt, title, diceSides),
            Player.IntArrayResponse.TYPE)
        .getValues();
  }

  @Nullable
  public static GamePlayer getGamePlayer(final IDelegateBridge bridge, final GamePlayer player) {
    final Player.PlayerResponse response =
        bridge.invokeRemotePlayer(
            player, new Player.GetGamePlayerRequest(), Player.PlayerResponse.TYPE);
    return response.getPlayer() == null
        ? null
        : response.getPlayer().resolvePlayer(bridge.getData());
  }

  public static void reportError(
      final IDelegateBridge bridge, final GamePlayer player, final String error) {
    bridge.invokeRemotePlayer(player, new Player.ReportErrorRequest(error), Player.VoidAck.TYPE);
  }

  public static void reportMessage(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final String message,
      final String title) {
    bridge.invokeRemotePlayer(
        player, new Player.ReportMessageRequest(message, title), Player.VoidAck.TYPE);
  }

  public static void confirmEnemyCasualties(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final @Nullable UUID battleId,
      final String message,
      final GamePlayer hitPlayer) {
    bridge.invokeRemotePlayer(
        player,
        new Player.ConfirmEnemyCasualtiesRequest(
            battleId == null ? null : battleId.toString(), message, EntityRef.of(hitPlayer)),
        Player.VoidAck.TYPE);
  }

  public static void confirmOwnCasualties(
      final IDelegateBridge bridge,
      final GamePlayer player,
      final @Nullable UUID battleId,
      final String message) {
    bridge.invokeRemotePlayer(
        player,
        new Player.ConfirmOwnCasualtiesRequest(
            battleId == null ? null : battleId.toString(), message),
        Player.VoidAck.TYPE);
  }

  // --- Encoding / decoding helpers. ---

  private static List<EntityRef> unitRefs(final @Nullable Collection<Unit> units) {
    return units == null ? List.of() : units.stream().map(EntityRef::of).toList();
  }

  private static List<EntityRef> territoryRefs(final Collection<Territory> territories) {
    return territories.stream().map(EntityRef::of).toList();
  }

  private static List<Unit> resolveUnits(final List<EntityRef> refs, final GameData data) {
    return refs.stream().map(ref -> ref.resolveUnit(data)).toList();
  }

  private static List<Territory> resolveTerritories(
      final List<EntityRef> refs, final GameData data) {
    return refs.stream().map(ref -> ref.resolveTerritory(data)).toList();
  }

  @Nullable
  private static Territory resolveNullable(final @Nullable EntityRef ref, final GameData data) {
    return ref == null ? null : ref.resolveTerritory(data);
  }

  private static Player.TerritoryResponse territoryResponse(final @Nullable Territory territory) {
    return new Player.TerritoryResponse(territory == null ? null : EntityRef.of(territory));
  }

  private static Player.UnitResponse unitResponse(final @Nullable Unit unit) {
    return new Player.UnitResponse(unit == null ? null : EntityRef.of(unit));
  }

  @Nullable
  private static Territory decodeTerritory(
      final Player.TerritoryResponse response, final GameData data) {
    return response.getTerritory() == null ? null : response.getTerritory().resolveTerritory(data);
  }
}
