package games.strategy.engine.player;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Used for both IRemotePlayer (used by the server, etc.) and specific game players such as
 * IRemotePlayer and IGridGamePlayer (used by delegates for communication, etc.).
 */
public interface Player extends IRemote {
  /**
   * Returns the id of this player. This id is initialized by the initialize method in
   * IRemotePlayer.
   */
  @RemoteActionCode(5)
  GamePlayer getGamePlayer();

  /** Called before the game starts. */
  void initialize(PlayerBridge bridge, GamePlayer gamePlayer);

  /** Returns the nation name. */
  @RemoteActionCode(6)
  String getName();

  @SuppressWarnings("unused")
  @RemoveOnNextMajorRelease
  @RemoteActionCode(8)
  default PlayerTypes.Type getPlayerType() {
    throw new UnsupportedOperationException("This method should not be called over the network");
  }

  String getPlayerLabel();

  boolean isAi();

  /**
   * Start the given step. stepName appears as it does in the game xml file. The game step will
   * finish executing when this method returns.
   */
  @RemoteActionCode(26)
  void start(String stepName);

  /** Called when the game is stopped (like if we are closing the window or leaving the game). */
  @RemoteActionCode(27)
  void stopGame();

  /**
   * Select casualties.
   *
   * @param selectFrom - the units to select casualties from
   * @param dependents - dependents of the units to select from
   * @param count - the number of casualties to select
   * @param message - ui message to display
   * @param dice - the dice rolled for the casualties
   * @param hit - the player hit
   * @param friendlyUnits - all friendly units in the battle (or moving)
   * @param enemyUnits - all enemy units in the battle (or defending aa)
   * @param amphibious - is the battle amphibious?
   * @param amphibiousLandAttackers - can be null
   * @param defaultCasualties - default casualties as selected by the game
   * @param battleId - the battle we are fighting in, may be null if this is an aa casualty
   *     selection during a move
   * @param battlesite - the territory where this happened
   * @param allowMultipleHitsPerUnit - can units be hit more than one time if they have more than
   *     one hitpoints left?
   * @return CasualtyDetails
   */
  @RemoveOnNextMajorRelease("amphibiousLandAttackers and amphibious isn't used anymore")
  @RemoteActionCode(19)
  CasualtyDetails selectCasualties(
      Collection<Unit> selectFrom,
      Map<Unit, Collection<Unit>> dependents,
      int count,
      String message,
      DiceRoll dice,
      GamePlayer hit,
      Collection<Unit> friendlyUnits,
      Collection<Unit> enemyUnits,
      boolean amphibious,
      Collection<Unit> amphibiousLandAttackers,
      CasualtyList defaultCasualties,
      UUID battleId,
      Territory battlesite,
      boolean allowMultipleHitsPerUnit);

  /**
   * Select a fixed dice roll.
   *
   * @param numDice - the number of dice rolls
   * @param hitAt - the roll value that constitutes a hit
   * @param title - the title for the DiceChooser
   * @param diceSides - the number of sides on the die, found by data.getDiceSides()
   * @return the resulting dice array
   */
  @RemoteActionCode(20)
  int[] selectFixedDice(int numDice, int hitAt, String title, int diceSides);

  /**
   * Select the territory to bombard with the bombarding capable unit (eg battleship).
   *
   * @param unit - the bombarding unit
   * @param unitTerritory - where the bombarding unit is
   * @param territories - territories where the unit can bombard
   * @return the Territory to bombard in, null if the unit should not bombard
   */
  @ChangeOnNextMajorRelease("Remove noneAvailable as it is always passed as 'true'")
  @RemoteActionCode(18)
  Territory selectBombardingTerritory(
      Unit unit, Territory unitTerritory, Collection<Territory> territories, boolean noneAvailable);

  /**
   * Ask if the player wants to attack lone subs.
   *
   * @param unitTerritory - where the potential battle is
   */
  @RemoteActionCode(15)
  boolean selectAttackSubs(Territory unitTerritory);

  /**
   * Ask if the player wants to attack lone transports.
   *
   * @param unitTerritory - where the potential battle is
   */
  @RemoteActionCode(16)
  boolean selectAttackTransports(Territory unitTerritory);

  /**
   * Ask if the player wants to attack units.
   *
   * @param unitTerritory - where the potential battle is
   */
  @RemoteActionCode(17)
  boolean selectAttackUnits(Territory unitTerritory);

  /**
   * Ask if the player wants to shore bombard.
   *
   * @param unitTerritory - where the potential battle is
   */
  @RemoteActionCode(22)
  boolean selectShoreBombard(Territory unitTerritory);

  /**
   * Report an error to the user.
   *
   * @param error that an error occurred
   */
  @RemoteActionCode(11)
  void reportError(String error);

  /** report a message to the user. */
  @RemoteActionCode(12)
  void reportMessage(String message, String title);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing raid can be
   * conducted, should the bomber bomb.
   */
  @RemoteActionCode(25)
  boolean shouldBomberBomb(Territory territory);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing raid can be
   * conducted, what should the bomber bomb.
   */
  @RemoteActionCode(28)
  Unit whatShouldBomberBomb(
      Territory territory, Collection<Unit> potentialTargets, Collection<Unit> bombers);

  /**
   * Choose where my rockets should fire.
   *
   * @param candidates - a collection of Territories, the possible territories to attack
   * @param from - where the rockets are launched from, null for WW2V1 rules
   * @return the territory to attack, null if no territory should be attacked
   */
  @RemoteActionCode(29)
  Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from);

  /**
   * Get the fighters to move to a newly produced carrier.
   *
   * @param fightersThatCanBeMoved - the fighters that can be moved
   * @param from - the territory containing the factory
   * @return - the fighters to move
   */
  @RemoteActionCode(7)
  Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      Collection<Unit> fightersThatCanBeMoved, Territory from);

  /**
   * Some carriers were lost while defending. We must select where to land some air units.
   *
   * @param candidates - a list of territories - these are the places where air units can land
   * @return - the territory to land the fighters in, must be non null
   */
  @RemoteActionCode(23)
  Territory selectTerritoryForAirToLand(
      Collection<Territory> candidates, Territory currentTerritory, String unitMessage);

  /**
   * The attempted move will incur aa fire, confirm that you still want to move.
   *
   * @param aaFiringTerritories - the territories where aa will fire
   */
  @RemoteActionCode(2)
  boolean confirmMoveInFaceOfAa(Collection<Territory> aaFiringTerritories);

  /** The attempted move will kill some air units. */
  @RemoteActionCode(3)
  boolean confirmMoveKamikaze();

  /**
   * Ask the player if he wishes to retreat.
   *
   * @param battleId - the battle
   * @param submerge - is submerging possible (means the retreat territory CAN be the current battle
   *     territory)
   * @param battleTerritory - where the battle is taking place
   * @param possibleTerritories - where the player can retreat to
   * @param message - user displayable message
   * @return the territory to retreat to, or null if the player doesnt wish to retreat
   */
  @RemoteActionCode(13)
  Optional<Territory> retreatQuery(
      UUID battleId,
      boolean submerge,
      Territory battleTerritory,
      Collection<Territory> possibleTerritories,
      String message);

  /**
   * Ask the player which units, if any, they want to scramble to defend against the attacker.
   *
   * @param scrambleTo - the territory we are scrambling to defend in, where the units will end up
   *     if scrambled
   * @param possibleScramblers possible units which we could scramble, with where they are from and
   *     how many allowed from that location
   * @return a list of units to scramble mapped to where they are coming from
   */
  @RemoteActionCode(14)
  Map<Territory, Collection<Unit>> scrambleUnitsQuery(
      Territory scrambleTo,
      Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers);

  /** Ask the player which if any units they want to select. */
  @RemoteActionCode(24)
  Collection<Unit> selectUnitsQuery(Territory current, Collection<Unit> possible, String message);

  /** Allows the user to pause and confirm enemy casualties. */
  @RemoteActionCode(1)
  void confirmEnemyCasualties(UUID battleId, String message, GamePlayer hitPlayer);

  @RemoteActionCode(4)
  void confirmOwnCasualties(UUID battleId, String message);

  /**
   * Indicates the player accepts the proposed action.
   *
   * @param acceptanceQuestion the question that should be asked to this player
   * @param politics is this from politics delegate?
   * @return whether the player accepts the action proposal
   */
  @RemoteActionCode(0)
  boolean acceptAction(
      GamePlayer playerSendingProposal, String acceptanceQuestion, boolean politics);

  /** Asks the player if they wish to perform any kamikaze suicide attacks. */
  @RemoteActionCode(21)
  @Nullable
  Map<Territory, Map<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      Map<Territory, Collection<Unit>> possibleUnitsToAttack);

  /**
   * Used during the RandomStartDelegate for assigning territories to players, and units to
   * territories.
   */
  @RemoteActionCode(10)
  Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(
      List<Territory> territoryChoices, List<Unit> unitChoices, int unitsPerPick);

  // --- Typed request/response records for the converted remote methods. ---
  // Entity arguments and returns ride as symbolic EntityRef references (see the wire package); the
  // receiver re-resolves them against its own GameData. Every remote Player call blocked for a
  // reply
  // under the reflective path, so void methods use a VoidAck to keep the caller blocking until the
  // player has handled the message. PlayerRemoteMessageHandlers registers the handlers and
  // dispatches
  // each request to the addressed player. The gnarly decision queries stay reflective for E3.

  /** Shared typed reply carrying a single boolean answer. */
  @AllArgsConstructor
  class BooleanResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600001L;

    public static final MessageType<BooleanResponse> TYPE = MessageType.of(BooleanResponse.class);

    @Getter private final boolean value;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Shared typed acknowledgement for a void method, so the caller still blocks on the reply. */
  class VoidAck implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600002L;

    public static final MessageType<VoidAck> TYPE = MessageType.of(VoidAck.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Shared typed reply carrying an optional territory reference (null when none was chosen). */
  @AllArgsConstructor
  class TerritoryResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600003L;

    public static final MessageType<TerritoryResponse> TYPE =
        MessageType.of(TerritoryResponse.class);

    @Getter @Nullable private final EntityRef territory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Shared typed reply carrying an optional unit reference (null when none was chosen). */
  @AllArgsConstructor
  class UnitResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600004L;

    public static final MessageType<UnitResponse> TYPE = MessageType.of(UnitResponse.class);

    @Getter @Nullable private final EntityRef unit;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Shared typed reply carrying a list of unit references. */
  @AllArgsConstructor
  class UnitsResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600005L;

    public static final MessageType<UnitsResponse> TYPE = MessageType.of(UnitsResponse.class);

    @Getter private final List<EntityRef> units;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Shared typed reply carrying an optional player reference. */
  @AllArgsConstructor
  class PlayerResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600006L;

    public static final MessageType<PlayerResponse> TYPE = MessageType.of(PlayerResponse.class);

    @Getter @Nullable private final EntityRef player;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed reply carrying a fixed dice array. The array rides the Java wire, so it has no fixture.
   */
  @AllArgsConstructor
  class IntArrayResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600007L;

    public static final MessageType<IntArrayResponse> TYPE = MessageType.of(IntArrayResponse.class);

    @Getter private final int[] values;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether the player accepts a proposed action. */
  @AllArgsConstructor
  class AcceptActionRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600010L;

    public static final MessageType<AcceptActionRequest> TYPE =
        MessageType.of(AcceptActionRequest.class);

    @Getter private final EntityRef playerSendingProposal;
    @Getter private final String acceptanceQuestion;
    @Getter private final boolean politics;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether the player wants to attack lone subs in a territory. */
  @AllArgsConstructor
  class SelectAttackSubsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600011L;

    public static final MessageType<SelectAttackSubsRequest> TYPE =
        MessageType.of(SelectAttackSubsRequest.class);

    @Getter private final EntityRef unitTerritory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether the player wants to attack lone transports in a territory. */
  @AllArgsConstructor
  class SelectAttackTransportsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600012L;

    public static final MessageType<SelectAttackTransportsRequest> TYPE =
        MessageType.of(SelectAttackTransportsRequest.class);

    @Getter private final EntityRef unitTerritory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether the player wants to attack units in a territory. */
  @AllArgsConstructor
  class SelectAttackUnitsRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600013L;

    public static final MessageType<SelectAttackUnitsRequest> TYPE =
        MessageType.of(SelectAttackUnitsRequest.class);

    @Getter private final EntityRef unitTerritory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether the player wants to shore bombard a territory. */
  @AllArgsConstructor
  class SelectShoreBombardRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600014L;

    public static final MessageType<SelectShoreBombardRequest> TYPE =
        MessageType.of(SelectShoreBombardRequest.class);

    @Getter private final EntityRef unitTerritory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking whether a bomber that moved into a territory should bomb. */
  @AllArgsConstructor
  class ShouldBomberBombRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600015L;

    public static final MessageType<ShouldBomberBombRequest> TYPE =
        MessageType.of(ShouldBomberBombRequest.class);

    @Getter private final EntityRef territory;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request confirming a move that will incur anti-aircraft fire. */
  @AllArgsConstructor
  class ConfirmMoveInFaceOfAaRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600016L;

    public static final MessageType<ConfirmMoveInFaceOfAaRequest> TYPE =
        MessageType.of(ConfirmMoveInFaceOfAaRequest.class);

    @Getter private final List<EntityRef> aaFiringTerritories;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request confirming a move that will kill some air units. */
  class ConfirmMoveKamikazeRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600017L;

    public static final MessageType<ConfirmMoveKamikazeRequest> TYPE =
        MessageType.of(ConfirmMoveKamikazeRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking which territory the player's rockets should attack. */
  @AllArgsConstructor
  class WhereShouldRocketsAttackRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600018L;

    public static final MessageType<WhereShouldRocketsAttackRequest> TYPE =
        MessageType.of(WhereShouldRocketsAttackRequest.class);

    @Getter private final List<EntityRef> candidates;
    @Getter @Nullable private final EntityRef from;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking which unit a bomber should bomb in a territory. */
  @AllArgsConstructor
  class WhatShouldBomberBombRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600019L;

    public static final MessageType<WhatShouldBomberBombRequest> TYPE =
        MessageType.of(WhatShouldBomberBombRequest.class);

    @Getter private final EntityRef territory;
    @Getter private final List<EntityRef> potentialTargets;
    @Getter private final List<EntityRef> bombers;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking which fighters to move to a newly produced carrier. */
  @AllArgsConstructor
  class GetNumberOfFightersToMoveToNewCarrierRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600020L;

    public static final MessageType<GetNumberOfFightersToMoveToNewCarrierRequest> TYPE =
        MessageType.of(GetNumberOfFightersToMoveToNewCarrierRequest.class);

    @Getter private final List<EntityRef> fightersThatCanBeMoved;
    @Getter private final EntityRef from;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking where stranded air units should try to land. */
  @AllArgsConstructor
  class SelectTerritoryForAirToLandRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600021L;

    public static final MessageType<SelectTerritoryForAirToLandRequest> TYPE =
        MessageType.of(SelectTerritoryForAirToLandRequest.class);

    @Getter private final List<EntityRef> candidates;
    @Getter private final EntityRef currentTerritory;
    @Getter private final String unitMessage;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking which territory the player's bombarding unit should bombard. */
  @AllArgsConstructor
  class SelectBombardingTerritoryRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600022L;

    public static final MessageType<SelectBombardingTerritoryRequest> TYPE =
        MessageType.of(SelectBombardingTerritoryRequest.class);

    @Getter private final EntityRef unit;
    @Getter private final EntityRef unitTerritory;
    @Getter private final List<EntityRef> territories;
    @Getter private final boolean noneAvailable;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking which units, if any, the player wants to select. */
  @AllArgsConstructor
  class SelectUnitsQueryRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600023L;

    public static final MessageType<SelectUnitsQueryRequest> TYPE =
        MessageType.of(SelectUnitsQueryRequest.class);

    @Getter private final EntityRef current;
    @Getter private final List<EntityRef> possible;
    @Getter private final String message;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking the player to select a fixed dice roll. */
  @AllArgsConstructor
  class SelectFixedDiceRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600024L;

    public static final MessageType<SelectFixedDiceRequest> TYPE =
        MessageType.of(SelectFixedDiceRequest.class);

    @Getter private final int numDice;
    @Getter private final int hitAt;
    @Getter private final String title;
    @Getter private final int diceSides;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request asking the player for its game player identity. */
  class GetGamePlayerRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600025L;

    public static final MessageType<GetGamePlayerRequest> TYPE =
        MessageType.of(GetGamePlayerRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed notification reporting an error to the player. */
  @AllArgsConstructor
  class ReportErrorRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600026L;

    public static final MessageType<ReportErrorRequest> TYPE =
        MessageType.of(ReportErrorRequest.class);

    @Getter private final String error;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed notification reporting a message to the player. */
  @AllArgsConstructor
  class ReportMessageRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600027L;

    public static final MessageType<ReportMessageRequest> TYPE =
        MessageType.of(ReportMessageRequest.class);

    @Getter private final String message;
    @Getter private final String title;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request pausing so the player can confirm enemy casualties. */
  @AllArgsConstructor
  class ConfirmEnemyCasualtiesRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600028L;

    public static final MessageType<ConfirmEnemyCasualtiesRequest> TYPE =
        MessageType.of(ConfirmEnemyCasualtiesRequest.class);

    @Getter @Nullable private final String battleId;
    @Getter private final String message;
    @Getter private final EntityRef hitPlayer;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request pausing so the player can confirm their own casualties. */
  @AllArgsConstructor
  class ConfirmOwnCasualtiesRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6320048576544600029L;

    public static final MessageType<ConfirmOwnCasualtiesRequest> TYPE =
        MessageType.of(ConfirmOwnCasualtiesRequest.class);

    @Getter @Nullable private final String battleId;
    @Getter private final String message;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
