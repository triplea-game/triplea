package games.strategy.engine.player;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
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
}
