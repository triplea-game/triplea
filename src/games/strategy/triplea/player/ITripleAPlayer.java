package games.strategy.triplea.player;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Interface the TriplePlayer presents to Delegates through IRemoteMessenger
 */
public interface ITripleAPlayer extends IRemotePlayer {
  /**
   * Select casualties
   *
   * @param selectFrom
   *        - the units to select casualties from
   * @param dependents
   *        - dependents of the units to select from
   * @param count
   *        - the number of casualties to select
   * @param message
   *        - ui message to display
   * @param dice
   *        - the dice rolled for the casualties
   * @param hit
   *        - the player hit
   * @param friendlyUnits
   *        - all friendly units in the battle (or moving)
   * @param enemyPlayer
   *        - the player who has hit you
   * @param enemyUnits
   *        - all enemy units in the battle (or defending aa)
   * @param amphibious
   *        - is the battle amphibious?
   * @param amphibiousLandAttackers
   *        - can be null
   * @param defaultCasualties
   *        - default casualties as selected by the game
   * @param battleID
   *        - the battle we are fighting in, may be null if this is an aa casualty selection during a move
   * @param battlesite
   *        - the territory where this happened
   * @param allowMultipleHitsPerUnit
   *        - can units be hit more than one time if they have more than one hitpoints left?
   * @return CasualtyDetails
   */
  CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count,
      String message, DiceRoll dice, PlayerID hit, Collection<Unit> friendlyUnits, PlayerID enemyPlayer,
      Collection<Unit> enemyUnits, boolean amphibious, Collection<Unit> amphibiousLandAttackers,
      CasualtyList defaultCasualties, GUID battleID, Territory battlesite, boolean allowMultipleHitsPerUnit);

  /**
   * Select a fixed dice roll
   *
   * @param numDice
   *        - the number of dice rolls
   * @param hitAt
   *        - the lowest roll that constitutes a hit (0 for none)
   * @param hitOnlyIfEquals
   *        - whether to count rolls greater than hitAt as hits
   * @param title
   *        - the title for the DiceChooser
   * @param diceSides
   *        - the number of sides on the die, found by data.getDiceSides()
   * @return the resulting dice array
   */
  int[] selectFixedDice(int numDice, int hitAt, boolean hitOnlyIfEquals, String title, int diceSides);

  // TODO: Remove noneAvailable as it is always passed as 'true'
  /**
   * Select the territory to bombard with the bombarding capable unit (eg battleship)
   *
   * @param unit
   *        - the bombarding unit
   * @param unitTerritory
   *        - where the bombarding unit is
   * @param territories
   *        - territories where the unit can bombard
   * @return the Territory to bombard in, null if the unit should not bombard
   */
  Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection<Territory> territories,
      boolean noneAvailable);

  /**
   * Ask if the player wants to attack lone subs
   *
   * @param unitTerritory
   *        - where the potential battle is
   */
  boolean selectAttackSubs(Territory unitTerritory);

  /**
   * Ask if the player wants to attack lone transports
   *
   * @param unitTerritory
   *        - where the potential battle is
   */
  boolean selectAttackTransports(Territory unitTerritory);

  /**
   * Ask if the player wants to attack units
   *
   * @param unitTerritory
   *        - where the potential battle is
   */
  boolean selectAttackUnits(Territory unitTerritory);

  /**
   * Ask if the player wants to shore bombard
   *
   * @param unitTerritory
   *        - where the potential battle is
   */
  boolean selectShoreBombard(Territory unitTerritory);

  // TODO: this is only called from BattleCalculator.selectCasualties() and should probably be removed
  /**
   * Report an error to the user.
   *
   * @param report
   *        that an error occurred
   */
  void reportError(String error);

  /**
   * report a message to the user
   *
   * @param message
   */
  void reportMessage(String message, String title);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing
   * raid can be conducted, should the bomber bomb?
   */
  boolean shouldBomberBomb(Territory territory);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing
   * raid can be conducted, what should the bomber bomb?
   */
  Unit whatShouldBomberBomb(Territory territory, final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers);

  /**
   * Choose where my rockets should fire
   *
   * @param candidates
   *        - a collection of Territories, the possible territories to attack
   * @param from
   *        - where the rockets are launched from, null for WW2V1 rules
   * @return the territory to attack, null if no territory should be attacked
   */
  Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from);

  /**
   * Get the fighters to move to a newly produced carrier
   *
   * @param fightersThatCanBeMoved
   *        - the fighters that can be moved
   * @param from
   *        - the territory containing the factory
   * @return - the fighters to move
   */
  Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from);

  /**
   * Some carriers were lost while defending. We must select where to land
   * some air units.
   *
   * @param candidates
   *        - a list of territories - these are the places where air units can land
   * @return - the territory to land the fighters in, must be non null
   */
  Territory selectTerritoryForAirToLand(Collection<Territory> candidates, final Territory currentTerritory,
      String unitMessage);

  /**
   * The attempted move will incur aa fire, confirm that you still want to move
   *
   * @param aaFiringTerritories
   *        - the territories where aa will fire
   */
  boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories);

  /**
   * The attempted move will kill some air units
   */
  boolean confirmMoveKamikaze();

  /**
   * The attempted move will kill some units
   */
  boolean confirmMoveHariKari();

  /**
   * Ask the player if he wishes to retreat.
   *
   * @param battleID
   *        - the battle
   * @param submerge
   *        - is submerging possible (means the retreat territory CAN be the current battle territory)
   * @param possibleTerritories
   *        - where the player can retreat to
   * @param message
   *        - user displayable message
   * @return the territory to retreat to, or null if the player doesnt wish to retreat
   */
  Territory retreatQuery(GUID battleID, boolean submerge, Territory battleTerritory,
      Collection<Territory> possibleTerritories, String message);

  /**
   * Ask the player which units, if any, they want to scramble to defend against the attacker.
   *
   * @param scrambleTo
   *        - the territory we are scrambling to defend in, where the units will end up if scrambled
   * @param possibleScramblers
   *        - possible units which we could scramble, with where they are from and how many allowed from that location
   * @return a list of units to scramble mapped to where they are coming from
   */
  HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(Territory scrambleTo,
      Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers);

  /**
   * Ask the player which if any units they want to select.
   *
   * @param current
   * @param possible
   * @param message
   */
  Collection<Unit> selectUnitsQuery(Territory current, Collection<Unit> possible, String message);

  /**
   * Allows the user to pause and confirm enemy casualties
   *
   * @param battleId
   * @param message
   */
  void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer);

  void confirmOwnCasualties(GUID battleId, String message);

  /**
   * Does the player accept the proposed action?
   *
   * @param acceptanceQuestion
   *        the question that should be asked to this player
   * @param politics
   *        is this from politics delegate?
   * @return whether the player accepts the actionproposal
   */
  boolean acceptAction(PlayerID playerSendingProposal, String acceptanceQuestion, boolean politics);

  /**
   * Asks the player if they wish to perform any kamikaze suicide attacks
   *
   * @param possibleUnitsToAttack
   */
  HashMap<Territory, HashMap<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      HashMap<Territory, Collection<Unit>> possibleUnitsToAttack);

  /**
   * Used during the RandomStartDelegate for assigning territories to players, and units to territories.
   */
  Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(List<Territory> territoryChoices, List<Unit> unitChoices,
      int unitsPerPick);
}
