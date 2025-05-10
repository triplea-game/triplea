package games.strategy.triplea.ai;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.AbstractBasePlayer;
import games.strategy.triplea.settings.ClientSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Base class for AIs.
 *
 * <p>Control pausing with the AI pause menu option. AIs should note that any data that is stored in
 * the AI instance, will be lost when the game is restarted. We cannot save data with an AI, since
 * the player may choose to restart the game with a different AI, or with a human player.
 *
 * <p>If an AI finds itself starting in the middle of a move phase, or the middle of a purchase
 * phase, (as would happen if a player saved the game during the middle of an AI's move phase) it is
 * acceptable for the AI to play badly for a turn, but the AI should recover, and play correctly
 * when the next phase of the game starts.
 *
 * <p>As a rule, nothing that changes GameData should be in here (it should be in a delegate, and
 * done through an IDelegate using a change).
 */
@Slf4j
public abstract class AbstractAi extends AbstractBasePlayer {
  public AbstractAi(final String name, final String playerLabel) {
    super(name, playerLabel);
  }

  @Override
  public Territory selectBombardingTerritory(
      final Unit unit,
      final Territory unitTerritory,
      final Collection<Territory> territories,
      final boolean noneAvailable) {
    return CollectionUtils.getAny(territories);
  }

  @Override
  public boolean selectAttackSubs(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectAttackTransports(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectAttackUnits(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean selectShoreBombard(final Territory unitTerritory) {
    return true;
  }

  @Override
  public boolean confirmMoveKamikaze() {
    return false;
  }

  @Override
  public Territory whereShouldRocketsAttack(
      final Collection<Territory> candidates, final Territory from) {
    return CollectionUtils.getAny(candidates);
  }

  @Override
  public CasualtyDetails selectCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final Collection<Unit> friendlyUnits,
      final Collection<Unit> enemyUnits,
      final boolean amphibious,
      final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties,
      final UUID battleId,
      final Territory battleSite,
      final boolean allowMultipleHitsPerUnit) {
    if (defaultCasualties.size() != count) {
      throw new IllegalStateException(
          "Select Casualties showing different numbers for number of hits to take ("
              + count
              + ") vs total size of default casualty selections ("
              + defaultCasualties.size()
              + ")");
    }
    if (defaultCasualties.getKilled().isEmpty()) {
      return new CasualtyDetails(defaultCasualties, false);
    }
    final CasualtyDetails myCasualties = new CasualtyDetails(false);
    myCasualties.addToDamaged(defaultCasualties.getDamaged());
    final List<Unit> selectFromSorted = new ArrayList<>(selectFrom);
    final List<Unit> interleavedTargetList = AiUtils.interleaveCarriersAndPlanes(selectFromSorted);
    for (int i = 0; i < defaultCasualties.getKilled().size(); ++i) {
      myCasualties.addToKilled(interleavedTargetList.get(i));
    }
    if (count != myCasualties.size()) {
      throw new IllegalStateException("AI chose wrong number of casualties");
    }
    return myCasualties;
  }

  @Override
  public Unit whatShouldBomberBomb(
      final Territory territory,
      final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    final Collection<Unit> factories =
        CollectionUtils.getMatches(potentialTargets, Matches.unitCanProduceUnitsAndCanBeDamaged());
    if (factories.isEmpty()) {
      return CollectionUtils.getAny(potentialTargets);
    }
    return CollectionUtils.getAny(factories);
  }

  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      final Collection<Unit> fightersThatCanBeMoved, final Territory from) {
    final List<Unit> fighters = new ArrayList<>();
    for (final Unit fighter : fightersThatCanBeMoved) {
      if (Math.random() < 0.8) {
        fighters.add(fighter);
      }
    }
    return fighters;
  }

  @Override
  public Territory selectTerritoryForAirToLand(
      final Collection<Territory> candidates,
      final Territory currentTerritory,
      final String unitMessage) {
    return CollectionUtils.getAny(candidates);
  }

  @Override
  public boolean confirmMoveInFaceOfAa(final Collection<Territory> aaFiringTerritories) {
    return true;
  }

  @Override
  public Territory retreatQuery(
      final UUID battleId,
      final boolean submerge,
      final Territory battleTerritory,
      final Collection<Territory> possibleTerritories,
      final String message) {
    return null;
  }

  @Override
  public Map<Territory, Collection<Unit>> scrambleUnitsQuery(
      final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    return null;
  }

  @Override
  public Collection<Unit> selectUnitsQuery(
      final Territory current, final Collection<Unit> possible, final String message) {
    return null;
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    return false;
  }

  // TODO: This really needs to be rewritten with some basic logic
  @Override
  public boolean acceptAction(
      final GamePlayer playerSendingProposal,
      final String acceptanceQuestion,
      final boolean politics) {
    // we are dead, just accept
    if (!this.getGamePlayer().amNotDeadYet()) {
      return true;
    }
    // not related to politics? just accept i guess
    if (!politics) {
      return true;
    }
    // politics from ally? accept
    if (Matches.isAllied(this.getGamePlayer()).test(playerSendingProposal)) {
      return true;
    }
    // would we normally be allies?
    final List<String> allies =
        List.of(
            Constants.PLAYER_NAME_AMERICANS,
            Constants.PLAYER_NAME_AUSTRALIANS,
            Constants.PLAYER_NAME_BRITISH,
            Constants.PLAYER_NAME_CANADIANS,
            Constants.PLAYER_NAME_CHINESE,
            Constants.PLAYER_NAME_FRENCH,
            Constants.PLAYER_NAME_RUSSIANS);
    if (allies.contains(this.getGamePlayer().getName())
        && allies.contains(playerSendingProposal.getName())) {
      return true;
    }
    final List<String> axis =
        List.of(
            Constants.PLAYER_NAME_GERMANS,
            Constants.PLAYER_NAME_ITALIANS,
            Constants.PLAYER_NAME_JAPANESE,
            Constants.PLAYER_NAME_PUPPET_STATES);
    if (axis.contains(this.getGamePlayer().getName())
        && axis.contains(playerSendingProposal.getName())) {
      return true;
    }
    final Collection<String> myAlliances =
        new HashSet<>(
            getGameData().getAllianceTracker().getAlliancesPlayerIsIn(this.getGamePlayer()));
    myAlliances.retainAll(
        getGameData().getAllianceTracker().getAlliancesPlayerIsIn(playerSendingProposal));
    return !myAlliances.isEmpty() || Math.random() < .5;
  }

  @Override
  public @Nullable Map<Territory, Map<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      final Map<Territory, Collection<Unit>> possibleUnitsToAttack) {
    final GamePlayer gamePlayer = this.getGamePlayer();
    // we are going to just assign random attacks to each unit randomly, til we run out of tokens to
    // attack with.
    final PlayerAttachment pa = PlayerAttachment.get(gamePlayer);
    if (pa == null) {
      return null;
    }
    final IntegerMap<Resource> resourcesAndAttackValues = pa.getSuicideAttackResources();
    if (resourcesAndAttackValues.isEmpty()) {
      return null;
    }
    final IntegerMap<Resource> playerResourceCollection =
        gamePlayer.getResources().getResourcesCopy();
    final IntegerMap<Resource> attackTokens = new IntegerMap<>();
    for (final Resource possible : resourcesAndAttackValues.keySet()) {
      final int amount = playerResourceCollection.getInt(possible);
      if (amount > 0) {
        attackTokens.put(possible, amount);
      }
    }
    if (attackTokens.isEmpty()) {
      return null;
    }
    final Map<Territory, Map<Unit, IntegerMap<Resource>>> kamikazeSuicideAttacks = new HashMap<>();
    for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet()) {
      if (attackTokens.isEmpty()) {
        continue;
      }
      final Territory t = entry.getKey();
      final List<Unit> targets = new ArrayList<>(entry.getValue());
      Collections.shuffle(targets);
      for (final Unit u : targets) {
        if (attackTokens.isEmpty()) {
          continue;
        }
        final IntegerMap<Resource> resourceMap = new IntegerMap<>();
        final Resource resource = CollectionUtils.getAny(attackTokens.keySet());
        final int num =
            Math.min(
                attackTokens.getInt(resource),
                (u.getUnitAttachment().getHitPoints()
                    * (Math.random() < .3 ? 1 : (Math.random() < .5 ? 2 : 3))));
        resourceMap.put(resource, num);
        kamikazeSuicideAttacks.computeIfAbsent(t, key -> new HashMap<>()).put(u, resourceMap);
        attackTokens.add(resource, -num);
        if (attackTokens.getInt(resource) <= 0) {
          attackTokens.removeKey(resource);
        }
      }
    }
    return kamikazeSuicideAttacks;
  }

  @Override
  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(
      final List<Territory> territoryChoices,
      final List<Unit> unitChoices,
      final int unitsPerPick) {
    final GameState data = getGameData();
    final GamePlayer me = this.getGamePlayer();
    final Territory picked;
    if (territoryChoices == null || territoryChoices.isEmpty()) {
      picked = null;
    } else if (territoryChoices.size() == 1) {
      picked = territoryChoices.get(0);
    } else {
      Collections.shuffle(territoryChoices);
      final List<Territory> notOwned =
          CollectionUtils.getMatches(territoryChoices, Matches.isTerritoryOwnedBy(me).negate());
      if (notOwned.isEmpty()) {
        // only owned territories left
        final boolean nonFactoryUnitsLeft =
            unitChoices.stream().anyMatch(Matches.unitCanProduceUnits().negate());
        final Predicate<Unit> ownedFactories =
            Matches.unitCanProduceUnits().and(Matches.unitIsOwnedBy(me));
        final List<Territory> capitals = TerritoryAttachment.getAllCapitals(me, data.getMap());
        final List<Territory> test = new ArrayList<>(capitals);
        test.retainAll(territoryChoices);
        final List<Territory> territoriesWithFactories =
            CollectionUtils.getMatches(
                territoryChoices, Matches.territoryHasUnitsThatMatch(ownedFactories));
        if (!nonFactoryUnitsLeft) {
          test.retainAll(
              CollectionUtils.getMatches(
                  test, Matches.territoryHasUnitsThatMatch(ownedFactories).negate()));
          if (!test.isEmpty()) {
            picked = test.get(0);
          } else {
            if (capitals.isEmpty()) {
              capitals.addAll(
                  CollectionUtils.getMatches(
                      data.getMap().getTerritories(),
                      Matches.isTerritoryOwnedBy(me)
                          .and(Matches.territoryHasUnitsOwnedBy(me))
                          .and(Matches.territoryIsLand())));
            }
            final List<Territory> doesNotHaveFactoryYet =
                CollectionUtils.getMatches(
                    territoryChoices, Matches.territoryHasUnitsThatMatch(ownedFactories).negate());
            if (capitals.isEmpty() || doesNotHaveFactoryYet.isEmpty()) {
              picked = territoryChoices.get(0);
            } else {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(capitals.get(0), doesNotHaveFactoryYet, it -> true);
              picked = distanceMap.minKey();
            }
          }
        } else {
          final int maxTerritoriesToPopulate =
              Math.min(
                  territoryChoices.size(),
                  Math.max(
                      4, CollectionUtils.countMatches(unitChoices, Matches.unitCanProduceUnits())));
          test.addAll(territoriesWithFactories);
          if (!test.isEmpty()) {
            if (test.size() < maxTerritoriesToPopulate) {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(test.get(0), territoryChoices, it -> true);
              for (int i = 0; i < maxTerritoriesToPopulate; i++) {
                final Territory choice = distanceMap.minKey();
                distanceMap.removeKey(choice);
                test.add(choice);
              }
            }
            Collections.shuffle(test);
            picked = test.get(0);
          } else {
            if (capitals.isEmpty()) {
              capitals.addAll(
                  CollectionUtils.getMatches(
                      data.getMap().getTerritories(),
                      Matches.isTerritoryOwnedBy(me)
                          .and(Matches.territoryHasUnitsOwnedBy(me))
                          .and(Matches.territoryIsLand())));
            }
            if (capitals.isEmpty()) {
              picked = territoryChoices.get(0);
            } else {
              final IntegerMap<Territory> distanceMap =
                  data.getMap().getDistance(capitals.get(0), territoryChoices, it -> true);
              if (territoryChoices.contains(capitals.get(0))) {
                distanceMap.put(capitals.get(0), 0);
              }
              final List<Territory> choices = new ArrayList<>();
              for (int i = 0; i < maxTerritoriesToPopulate; i++) {
                final Territory choice = distanceMap.minKey();
                distanceMap.removeKey(choice);
                choices.add(choice);
              }
              Collections.shuffle(choices);
              picked = choices.get(0);
            }
          }
        }
      } else {
        // pick a not owned territory if possible
        final List<Territory> capitals = TerritoryAttachment.getAllCapitals(me, data.getMap());
        final List<Territory> test = new ArrayList<>(capitals);
        test.retainAll(notOwned);
        if (!test.isEmpty()) {
          picked = test.get(0);
        } else {
          if (capitals.isEmpty()) {
            capitals.addAll(
                CollectionUtils.getMatches(
                    data.getMap().getTerritories(),
                    Matches.isTerritoryOwnedBy(me)
                        .and(Matches.territoryHasUnitsOwnedBy(me))
                        .and(Matches.territoryIsLand())));
          }
          if (capitals.isEmpty()) {
            picked = territoryChoices.get(0);
          } else {
            final IntegerMap<Territory> distanceMap =
                data.getMap().getDistance(capitals.get(0), notOwned, it -> true);
            picked = distanceMap.minKey();
          }
        }
      }
    }
    final Set<Unit> unitsToPlace = new HashSet<>();
    if (!unitChoices.isEmpty() && unitsPerPick > 0) {
      Collections.shuffle(unitChoices);
      final List<Unit> nonFactory =
          CollectionUtils.getMatches(unitChoices, Matches.unitCanProduceUnits().negate());
      if (nonFactory.isEmpty()) {
        for (int i = 0; i < unitsPerPick && !unitChoices.isEmpty(); i++) {
          unitsToPlace.add(unitChoices.get(0));
        }
      } else {
        for (int i = 0; i < unitsPerPick; i++) {
          unitsToPlace.add(nonFactory.get(0));
        }
      }
    }
    return Tuple.of(picked, unitsToPlace);
  }

  @Override
  public void confirmEnemyCasualties(
      final UUID battleId, final String message, final GamePlayer hitPlayer) {}

  @Override
  public void reportError(final String error) {}

  @Override
  public void reportMessage(final String message, final String title) {}

  @Override
  public void confirmOwnCasualties(final UUID battleId, final String message) {
    combatStepPause();
  }

  @Override
  public int[] selectFixedDice(
      final int numRolls, final int hitAt, final String message, final int diceSides) {
    final int[] dice = new int[numRolls];
    for (int i = 0; i < numRolls; i++) {
      dice[i] = (int) Math.ceil(Math.random() * diceSides);
    }
    return dice;
  }

  @Override
  public final void start(final String name) {
    super.start(name);
    final GamePlayer gamePlayer = this.getGamePlayer();
    if (GameStep.isBidStepName(name)) {
      final IPurchaseDelegate purchaseDelegate =
          (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
      final String propertyName = gamePlayer.getName() + " bid";
      final int bidAmount = getGameData().getProperties().get(propertyName, 0);
      purchase(true, bidAmount, purchaseDelegate, getGameData(), gamePlayer);
    } else if (GameStep.isPurchaseStepName(name)) {
      final IPurchaseDelegate purchaseDelegate =
          (IPurchaseDelegate) getPlayerBridge().getRemoteDelegate();
      final Resource pus = getGameData().getResourceList().getResource(Constants.PUS);
      final int leftToSpend = gamePlayer.getResources().getQuantity(pus);
      purchase(false, leftToSpend, purchaseDelegate, getGameData(), gamePlayer);
    } else if (GameStep.isTechStepName(name)) {
      final ITechDelegate techDelegate = (ITechDelegate) getPlayerBridge().getRemoteDelegate();
      tech(techDelegate, getGameData(), gamePlayer);
    } else if (GameStep.isMoveStepName(name)) {
      final IMoveDelegate moveDel = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
      if (!GameStepPropertiesHelper.isAirborneMove(getGameData())) {
        move(GameStep.isNonCombatMoveStepName(name), moveDel, getGameData(), gamePlayer);
      }
    } else if (GameStep.isBattleStepName(name)) {
      battle((IBattleDelegate) getPlayerBridge().getRemoteDelegate());
    } else if (GameStep.isPoliticsStepName(name)) {
      politicalActions();
    } else if (GameStep.isPlaceStepName(name)) {
      final IAbstractPlaceDelegate placeDel =
          (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
      place(GameStep.isBidStepName(name), placeDel, getGameData(), gamePlayer);
    } else if (GameStep.isEndTurnStepName(name)) {
      endTurn((IAbstractForumPosterDelegate) getPlayerBridge().getRemoteDelegate(), gamePlayer);
    }
  }

  // The following methods are called when the AI starts a phase.
  /**
   * It is the AI's turn to purchase units.
   *
   * @param purchaseForBid Is this a bid purchase, or a normal purchase.
   * @param pusToSpend How many PUs we have to spend.
   * @param purchaseDelegate The purchase delegate to buy things with.
   * @param data The GameData.
   * @param player The player to buy for.
   */
  protected abstract void purchase(
      boolean purchaseForBid,
      int pusToSpend,
      IPurchaseDelegate purchaseDelegate,
      GameData data,
      GamePlayer player);

  /**
   * It is the AI's turn to roll for technology.
   *
   * @param techDelegate - the tech delegate to roll for
   * @param data - the game data
   * @param player - the player to roll tech for
   */
  protected abstract void tech(ITechDelegate techDelegate, GameData data, GamePlayer player);

  /**
   * It is the AI's turn to move. Make all moves before returning from this method.
   *
   * @param nonCombat - are we moving in combat, or non combat
   * @param moveDel - the move delegate to make moves with
   * @param data - the current game data
   * @param player - the player to move with
   */
  protected abstract void move(
      boolean nonCombat, IMoveDelegate moveDel, GameData data, GamePlayer player);

  /**
   * It is the AI's turn to place units. get the units available to place with
   * player.getUnitCollection()
   *
   * @param placeForBid - is this a placement for bid
   * @param placeDelegate - the place delegate to place with
   * @param data - the current Game Data
   * @param player - the player to place for
   */
  protected abstract void place(
      boolean placeForBid, IAbstractPlaceDelegate placeDelegate, GameState data, GamePlayer player);

  /**
   * No need to override this.
   *
   * @param endTurnForumPosterDelegate The delegate to end the turn with.
   * @param player The player whose turn is ending.
   */
  protected void endTurn(
      final IAbstractForumPosterDelegate endTurnForumPosterDelegate, final GamePlayer player) {
    // we should not override this...
  }

  /**
   * It is the AI's turn to fight. Subclasses may override this if they want, but generally the AI
   * does not need to worry about the order of fighting battles.
   *
   * @param battleDelegate the battle delegate to query for battles not fought and the
   */
  protected void battle(final IBattleDelegate battleDelegate) {
    // generally all AI's will follow the same logic.
    // loop until all battles are fought.
    // rather than try to analyze battles to figure out which must be fought before others
    // as in the case of a naval battle preceding an amphibious attack, keep trying to fight every
    // battle
    while (true) {
      final BattleListing listing = battleDelegate.getBattleListing();
      if (listing.isEmpty()) {
        return;
      }
      for (final Entry<BattleType, Collection<Territory>> entry :
          listing.getBattlesMap().entrySet()) {
        for (final Territory current : entry.getValue()) {
          final String error =
              battleDelegate.fightBattle(current, entry.getKey().isBombingRun(), entry.getKey());
          if (error != null && !BattleDelegate.isBattleDependencyErrorMessage(error)) {
            log.warn(error);
          }
        }
      }
    }
  }

  protected void politicalActions() {
    final IPoliticsDelegate remotePoliticsDelegate =
        (IPoliticsDelegate) getPlayerBridge().getRemoteDelegate();
    final GameData data = getGameData();
    final GamePlayer gamePlayer = this.getGamePlayer();
    final float numPlayers = data.getPlayerList().getPlayers().size();
    final PoliticsDelegate politicsDelegate = data.getPoliticsDelegate();
    // We want to test the conditions each time to make sure they are still valid
    if (Math.random() < .5) {
      final List<PoliticalActionAttachment> actionChoicesTowardsWar =
          AiPoliticalUtils.getPoliticalActionsTowardsWar(
              gamePlayer, politicsDelegate.getTestedConditions(), data);
      if (!actionChoicesTowardsWar.isEmpty()) {
        Collections.shuffle(actionChoicesTowardsWar);
        int i = 0;
        // should we use bridge's random source here?
        final double random = Math.random();
        int maxWarActionsPerTurn =
            (random < .5 ? 0 : (random < .9 ? 1 : (random < .99 ? 2 : (int) numPlayers / 2)));
        if ((maxWarActionsPerTurn > 0)
            && CollectionUtils.countMatches(
                        data.getRelationshipTracker().getRelationships(gamePlayer),
                        Matches.relationshipIsAtWar())
                    / numPlayers
                < 0.4) {
          if (Math.random() < .9) {
            maxWarActionsPerTurn = 0;
          } else {
            maxWarActionsPerTurn = 1;
          }
        }
        final Iterator<PoliticalActionAttachment> actionWarIter =
            actionChoicesTowardsWar.iterator();
        while (actionWarIter.hasNext() && maxWarActionsPerTurn > 0) {
          final PoliticalActionAttachment action = actionWarIter.next();
          if (!Matches.abstractUserActionAttachmentCanBeAttempted(
                  politicsDelegate.getTestedConditions())
              .test(action)) {
            continue;
          }
          i++;
          if (i > maxWarActionsPerTurn) {
            break;
          }
          remotePoliticsDelegate.attemptAction(action);
        }
      }
    } else {
      final List<PoliticalActionAttachment> actionChoicesOther =
          AiPoliticalUtils.getPoliticalActionsOther(
              gamePlayer, politicsDelegate.getTestedConditions(), data);
      if (!actionChoicesOther.isEmpty()) {
        Collections.shuffle(actionChoicesOther);
        int i = 0;
        // should we use bridge's random source here?
        final double random = Math.random();
        final int maxOtherActionsPerTurn =
            (random < .3
                ? 0
                : (random < .6 ? 1 : (random < .9 ? 2 : (random < .99 ? 3 : (int) numPlayers))));
        final Iterator<PoliticalActionAttachment> actionOtherIter = actionChoicesOther.iterator();
        while (actionOtherIter.hasNext() && maxOtherActionsPerTurn > 0) {
          final PoliticalActionAttachment action = actionOtherIter.next();
          if (!Matches.abstractUserActionAttachmentCanBeAttempted(
                  politicsDelegate.getTestedConditions())
              .test(action)) {
            continue;
          }
          if (!gamePlayer.getResources().has(action.getCostResources())) {
            continue;
          }
          i++;
          if (i > maxOtherActionsPerTurn) {
            break;
          }
          remotePoliticsDelegate.attemptAction(action);
        }
      }
    }
  }

  /** Pause the game to allow the human player to see what is going on. */
  public static void movePause() {
    Interruptibles.sleep(ClientSetting.aiMovePauseDuration.getValueOrThrow());
  }

  /** Pause the combat to allow the human player to see what is going on. */
  public static void combatStepPause() {
    Interruptibles.sleep(ClientSetting.aiCombatStepPauseDuration.getValueOrThrow());
  }

  @Override
  public boolean isAi() {
    return true;
  }
}
