package games.strategy.triplea.delegate;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.RelationshipTracker.Relationship;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.player.Player;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.BonusIncomeUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/** At the end of the turn collect income. */
public abstract class AbstractEndTurnDelegate extends BaseTripleADelegate
    implements IAbstractForumPosterDelegate {
  public static final String END_TURN_REPORT_STRING = "Income Summary for ";
  private static final int CONVOY_BLOCKADE_DICE_SIDES = 6;
  private boolean needToInitialize = true;
  private boolean hasPostedTurnSummary = false;

  private static boolean canPlayerCollectIncome(final GamePlayer player, final GameMap gameMap) {
    return TerritoryAttachment.doWeHaveEnoughCapitalsToProduce(player, gameMap);
  }

  /**
   * Find estimated income for given player. This only takes into account income from territories,
   * units, NOs, and triggers. It ignores blockades, war bonds, relationship upkeep, and bonus
   * income.
   */
  public static IntegerMap<Resource> findEstimatedIncome(
      final GamePlayer player, final GameData data) {
    final IntegerMap<Resource> resources = new IntegerMap<>();

    // Only add territory resources if endTurn not endTurnNoPU
    for (final GameStep step : data.getSequence()) {
      if (player.equals(step.getPlayerId())
          && step.getDelegate() != null
          && step.getDelegate().getName().equals("endTurn")) {
        final List<Territory> territories = data.getMap().getTerritoriesOwnedBy(player);
        final int pusFromTerritories =
            getProduction(territories, data) * Properties.getPuMultiplier(data.getProperties());
        resources.add(new Resource(Constants.PUS, data), pusFromTerritories);
        resources.add(EndTurnDelegate.getResourceProduction(territories, data));
      }
    }

    // Add unit generated resources, NOs, and triggers
    resources.add(EndTurnDelegate.findUnitCreatedResources(player, data));
    resources.add(EndTurnDelegate.findNationalObjectiveAndTriggerResources(player, data));

    return resources;
  }

  @Override
  public void start() {
    // figure out our current PUs before we do anything else, including super methods
    final GameData data = bridge.getData();
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int leftOverPUs = bridge.getGamePlayer().getResources().getQuantity(pus);
    final IntegerMap<Resource> leftOverResources =
        bridge.getGamePlayer().getResources().getResourcesCopy();
    super.start();
    if (!needToInitialize) {
      return;
    }
    final StringBuilder endTurnReport = new StringBuilder();
    hasPostedTurnSummary = false;
    final PlayerAttachment pa = PlayerAttachment.get(player);
    // can't collect unless you own your own capital
    if (!canPlayerCollectIncome(player, data.getMap())) {
      endTurnReport.append(
          rollWarBondsForFriends(
              bridge, player, data.getTechnologyFrontier(), data.getMap(), data.getResourceList()));
      // we do not collect any income this turn
    } else {
      // just collect resources
      final Collection<Territory> territories = data.getMap().getTerritoriesOwnedBy(player);
      int toAdd = getProduction(territories);
      final int blockadeLoss = getBlockadeProductionLoss(player, data, bridge, endTurnReport);
      toAdd -= blockadeLoss;
      toAdd *= Properties.getPuMultiplier(data.getProperties());
      int total = player.getResources().getQuantity(pus) + toAdd;
      final String transcriptText;
      if (blockadeLoss == 0) {
        transcriptText =
            player.getName()
                + " collect "
                + toAdd
                + MyFormatter.pluralize(" PU", toAdd)
                + "; end with "
                + total
                + MyFormatter.pluralize(" PU", total);
      } else {
        transcriptText =
            player.getName()
                + " collect "
                + toAdd
                + MyFormatter.pluralize(" PU", toAdd)
                + " ("
                + blockadeLoss
                + " lost to blockades)"
                + "; end with "
                + total
                + MyFormatter.pluralize(" PU", total);
      }
      bridge.getHistoryWriter().startEvent(transcriptText);
      endTurnReport.append(transcriptText).append("<br />");
      // do war bonds
      final int bonds = rollWarBonds(bridge, player, data.getTechnologyFrontier());
      if (bonds > 0) {
        total += bonds;
        toAdd += bonds;
        final String bondText =
            player.getName()
                + " collect "
                + bonds
                + MyFormatter.pluralize(" PU", bonds)
                + " from War Bonds; end with "
                + total
                + MyFormatter.pluralize(" PU", total);
        bridge.getHistoryWriter().startEvent(bondText);
        endTurnReport.append("<br />").append(bondText).append("<br />");
      }
      if (total < 0) {
        toAdd -= total;
      }
      final Change change = ChangeFactory.changeResourcesChange(player, pus, toAdd);
      bridge.addChange(change);

      if (Properties.getPacificTheater(data.getProperties()) && pa != null) {
        final Change changeVp =
            (ChangeFactory.attachmentPropertyChange(
                pa, (pa.getVps() + (toAdd / 10) + (pa.getCaptureVps() / 10)), "vps"));
        final Change changeCaptureVp =
            ChangeFactory.attachmentPropertyChange(pa, "0", "captureVps");
        final CompositeChange ccVp = new CompositeChange(changeVp, changeCaptureVp);
        bridge.addChange(ccVp);
      }

      endTurnReport.append("<br />").append(addOtherResources(bridge));
      endTurnReport.append("<br />").append(doNationalObjectivesAndOtherEndTurnEffects(bridge));
      final IntegerMap<Resource> income = player.getResources().getResourcesCopy();
      income.subtract(leftOverResources);
      endTurnReport
          .append("<br />")
          .append(BonusIncomeUtils.addBonusIncome(income, bridge, player));

      // now we do upkeep costs, including upkeep cost as a percentage of our entire income for this
      // turn (including
      // NOs)
      final int currentPUs = player.getResources().getQuantity(pus);
      int relationshipUpkeepCostFlat = 0;
      int relationshipUpkeepCostPercentage = 0;
      for (final Relationship r : data.getRelationshipTracker().getRelationships(player)) {
        final String[] upkeep =
            r.getRelationshipType().getRelationshipTypeAttachment().getUpkeepCost().split(":", 2);
        if (upkeep.length == 1 || upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_FLAT)) {
          relationshipUpkeepCostFlat += Integer.parseInt(upkeep[0]);
        } else if (upkeep[1].equals(RelationshipTypeAttachment.UPKEEP_PERCENTAGE)) {
          relationshipUpkeepCostPercentage += Integer.parseInt(upkeep[0]);
        }
      }
      relationshipUpkeepCostPercentage = Math.min(100, relationshipUpkeepCostPercentage);
      int relationshipUpkeepTotalCost = 0;
      if (relationshipUpkeepCostPercentage != 0) {
        final float gainedPus = Math.max(0, currentPUs - leftOverPUs);
        relationshipUpkeepTotalCost +=
            Math.round(gainedPus * relationshipUpkeepCostPercentage / 100f);
      }
      if (relationshipUpkeepCostFlat != 0) {
        relationshipUpkeepTotalCost += relationshipUpkeepCostFlat;
      }
      // we can't remove more than we have, and we also must flip the sign
      relationshipUpkeepTotalCost = Math.min(currentPUs, relationshipUpkeepTotalCost);
      relationshipUpkeepTotalCost = -1 * relationshipUpkeepTotalCost;
      if (relationshipUpkeepTotalCost != 0) {
        final int newTotal = currentPUs + relationshipUpkeepTotalCost;
        final String transcriptText2 =
            player.getName()
                + (relationshipUpkeepTotalCost < 0 ? " pays " : " taxes ")
                + (-1 * relationshipUpkeepTotalCost)
                + MyFormatter.pluralize(" PU", relationshipUpkeepTotalCost)
                + " in order to maintain current relationships with other players, "
                + "and ends the turn with "
                + newTotal
                + MyFormatter.pluralize(" PU", newTotal);
        bridge.getHistoryWriter().startEvent(transcriptText2);
        endTurnReport.append("<br />").append(transcriptText2).append("<br />");
        final Change upkeep =
            ChangeFactory.changeResourcesChange(player, pus, relationshipUpkeepTotalCost);
        bridge.addChange(upkeep);
      }
    }
    if (GameStepPropertiesHelper.isRepairUnits(data)) {
      MoveDelegate.repairMultipleHitPointUnits(bridge, bridge.getGamePlayer());
    }
    if (Properties.getGiveUnitsByTerritory(getData().getProperties())
        && pa != null
        && !pa.getGiveUnitControl().isEmpty()) {
      changeUnitOwnership(bridge);
    }
    needToInitialize = false;
    showEndTurnReport(endTurnReport.toString());
  }

  protected void showEndTurnReport(final String endTurnReport) {
    if (endTurnReport != null && endTurnReport.trim().length() > 6 && !player.isAi()) {
      final Player currentPlayer = getRemotePlayer(player);
      final String playerName = player.getName();
      currentPlayer.reportMessage(
          "<html><b style=\"font-size:120%\" >"
              + END_TURN_REPORT_STRING
              + playerName
              + "</b><br /><br />"
              + endTurnReport
              + "</html>",
          END_TURN_REPORT_STRING + playerName);
    }
  }

  @Override
  public void end() {
    super.end();
    needToInitialize = true;
    getData().getBattleDelegate().getBattleTracker().clear();
  }

  @Override
  public Serializable saveState() {
    final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    state.needToInitialize = needToInitialize;
    state.hasPostedTurnSummary = hasPostedTurnSummary;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final EndTurnExtendedDelegateState s = (EndTurnExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.needToInitialize;
    hasPostedTurnSummary = s.hasPostedTurnSummary;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    // currently we need to call this regardless, because it resets player sounds for the turn.
    return true;
  }

  private int rollWarBonds(
      final IDelegateBridge delegateBridge,
      final GamePlayer player,
      final TechnologyFrontier technologyFrontier) {

    final int count =
        TechAbilityAttachment.getWarBondDiceNumber(
            TechTracker.getCurrentTechAdvances(player, technologyFrontier));
    final int sides =
        TechAbilityAttachment.getWarBondDiceSides(
            TechTracker.getCurrentTechAdvances(player, technologyFrontier));
    if (sides <= 0 || count <= 0) {
      return 0;
    }
    final String annotation = player.getName() + " rolling to resolve War Bonds: ";
    final DiceRoll dice =
        RollDiceFactory.rollNSidedDiceXTimes(
            delegateBridge, count, sides, player, DiceType.NONCOMBAT, annotation);
    int total = 0;
    for (int i = 0; i < dice.size(); i++) {
      total += dice.getDie(i).getValue() + 1;
    }
    getRemotePlayer(player)
        .reportMessage(
            annotation + MyFormatter.asDice(dice), annotation + MyFormatter.asDice(dice));
    return total;
  }

  private String rollWarBondsForFriends(
      final IDelegateBridge delegateBridge,
      final GamePlayer player,
      final TechnologyFrontier technologyFrontier,
      final GameMap gameMap,
      final ResourceList resourceList) {

    final int count =
        TechAbilityAttachment.getWarBondDiceNumber(
            TechTracker.getCurrentTechAdvances(player, technologyFrontier));
    final int sides =
        TechAbilityAttachment.getWarBondDiceSides(
            TechTracker.getCurrentTechAdvances(player, technologyFrontier));
    if (sides <= 0 || count <= 0) {
      return "";
    }
    // basically, if we are sharing our technology with someone, and we have warbonds but they do
    // not, then we roll our
    // warbonds and give them the proceeds (Global 1940)
    final PlayerAttachment playerattachment = PlayerAttachment.get(player);
    if (playerattachment == null) {
      return "";
    }
    final Collection<GamePlayer> shareWith = playerattachment.getShareTechnology();
    if (shareWith == null || shareWith.isEmpty()) {
      return "";
    }
    // take first one
    GamePlayer giveWarBondsTo = null;
    for (final GamePlayer p : shareWith) {

      final int diceCount =
          TechAbilityAttachment.getWarBondDiceNumber(
              TechTracker.getCurrentTechAdvances(p, technologyFrontier));
      final int diceSides =
          TechAbilityAttachment.getWarBondDiceSides(
              TechTracker.getCurrentTechAdvances(p, technologyFrontier));
      // if both are zero, then it must mean we did not share our war bonds tech with them, even
      // though we are sharing
      // all tech (because they cannot have this tech)
      if (diceSides <= 0 && diceCount <= 0 && canPlayerCollectIncome(p, gameMap)) {
        giveWarBondsTo = p;
        break;
      }
    }
    if (giveWarBondsTo == null) {
      return "";
    }
    final String annotation =
        player.getName()
            + " rolling to resolve War Bonds, and giving results to "
            + giveWarBondsTo.getName()
            + ": ";
    final DiceRoll dice =
        RollDiceFactory.rollNSidedDiceXTimes(
            delegateBridge, count, sides, player, DiceType.NONCOMBAT, annotation);
    int totalWarBonds = 0;
    for (int i = 0; i < dice.size(); i++) {
      totalWarBonds += dice.getDie(i).getValue() + 1;
    }
    final Resource pus = resourceList.getResource(Constants.PUS);
    final int currentPUs = giveWarBondsTo.getResources().getQuantity(pus);
    final String transcriptText =
        player.getName()
            + " rolls "
            + totalWarBonds
            + MyFormatter.pluralize(" PU", totalWarBonds)
            + " from War Bonds, giving the total to "
            + giveWarBondsTo.getName()
            + ", who ends with "
            + (currentPUs + totalWarBonds)
            + MyFormatter.pluralize(" PU", (currentPUs + totalWarBonds))
            + " total";
    delegateBridge.getHistoryWriter().startEvent(transcriptText);
    final Change change = ChangeFactory.changeResourcesChange(giveWarBondsTo, pus, totalWarBonds);
    delegateBridge.addChange(change);
    getRemotePlayer(player)
        .reportMessage(
            annotation + MyFormatter.asDice(dice), annotation + MyFormatter.asDice(dice));
    return transcriptText + "<br />";
  }

  private static void changeUnitOwnership(final IDelegateBridge bridge) {
    final GamePlayer player = bridge.getGamePlayer();
    final PlayerAttachment pa = PlayerAttachment.get(player);
    final Collection<GamePlayer> possibleNewOwners = pa.getGiveUnitControl();
    final boolean inAllTerritories = pa.getGiveUnitControlInAllTerritories();
    final CompositeChange change = new CompositeChange();
    final Collection<Tuple<Territory, Collection<Unit>>> changeList = new ArrayList<>();
    for (final Territory currTerritory : bridge.getData().getMap().getTerritories()) {
      final TerritoryAttachment ta = TerritoryAttachment.get(currTerritory);
      // if ownership should change in this territory
      if (inAllTerritories || (ta != null && !ta.getChangeUnitOwners().isEmpty())) {
        final List<GamePlayer> newOwners =
            new ArrayList<>(
                (ta != null && !ta.getChangeUnitOwners().isEmpty())
                    ? ta.getChangeUnitOwners()
                    : bridge.getData().getPlayerList().getPlayers());
        newOwners.retainAll(possibleNewOwners);
        for (final GamePlayer newOwner : newOwners) {
          final Collection<Unit> units =
              currTerritory.getMatches(
                  Matches.unitIsOwnedBy(player).and(Matches.unitCanBeGivenByTerritoryTo(newOwner)));
          if (!units.isEmpty()) {
            change.add(ChangeFactory.changeOwner(units, newOwner, currTerritory));
            changeList.add(Tuple.of(currTerritory, units));
          }
        }
      }
    }
    if (!change.isEmpty() && !changeList.isEmpty()) {
      if (changeList.size() == 1) {
        final Tuple<Territory, Collection<Unit>> tuple = CollectionUtils.getAny(changeList);
        bridge
            .getHistoryWriter()
            .startEvent(
                "Some Units in "
                    + tuple.getFirst().getName()
                    + " change ownership: "
                    + MyFormatter.unitsToTextNoOwner(tuple.getSecond()),
                tuple.getSecond());
      } else {
        bridge.getHistoryWriter().startEvent("Units Change Ownership");
        for (final Tuple<Territory, Collection<Unit>> tuple : changeList) {
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  "Some Units in "
                      + tuple.getFirst().getName()
                      + " change ownership: "
                      + MyFormatter.unitsToTextNoOwner(tuple.getSecond()),
                  tuple.getSecond());
        }
      }
      bridge.addChange(change);
    }
  }

  protected abstract String addOtherResources(IDelegateBridge bridge);

  protected abstract String doNationalObjectivesAndOtherEndTurnEffects(IDelegateBridge bridge);

  protected int getProduction(final Collection<Territory> territories) {
    return getProduction(territories, getData());
  }

  /** Returns the total production value of the specified territories. */
  public static int getProduction(final Collection<Territory> territories, final GameState data) {
    int value = 0;
    for (final Territory current : territories) {
      final TerritoryAttachment attachment = TerritoryAttachment.get(current);
      if (attachment == null) {
        throw new IllegalStateException("No attachment for owned territory:" + current.getName());
      }
      // Match will Check if territory is originally owned convoy center, or if it is contested
      if (Matches.territoryCanCollectIncomeFrom(current.getOwner()).test(current)) {
        value += attachment.getProduction();
      }
    }
    return value;
  }

  // finds losses due to blockades, positive value returned.
  private int getBlockadeProductionLoss(
      final GamePlayer player,
      final GameState data,
      final IDelegateBridge bridge,
      final StringBuilder endTurnReport) {
    final PlayerAttachment playerRules = PlayerAttachment.get(player);
    if (playerRules != null && playerRules.getImmuneToBlockade()) {
      return 0;
    }
    final GameMap map = data.getMap();
    final Collection<Territory> blockable =
        CollectionUtils.getMatches(map.getTerritories(), Matches.territoryIsBlockadeZone());
    if (blockable.isEmpty()) {
      return 0;
    }
    final Predicate<Unit> enemyUnits = Matches.enemyUnit(player);
    int totalLoss = 0;
    final boolean rollDiceForBlockadeDamage =
        Properties.getConvoyBlockadesRollDiceForCost(data.getProperties());
    final Collection<String> transcripts = new ArrayList<>();
    final Map<Territory, Tuple<Integer, List<Territory>>> damagePerBlockadeZone = new HashMap<>();
    boolean rolledDice = false;
    for (final Territory b : blockable) {
      // match will check for land, convoy zones, and also contested territories
      final List<Territory> viableNeighbors =
          CollectionUtils.getMatches(
              map.getNeighbors(b),
              Matches.isTerritoryOwnedBy(player)
                  .and(Matches.territoryCanCollectIncomeFrom(player)));
      final int maxLoss = getProduction(viableNeighbors);
      if (maxLoss <= 0) {
        continue;
      }
      final Collection<Unit> enemies = CollectionUtils.getMatches(b.getUnits(), enemyUnits);
      if (enemies.isEmpty()) {
        continue;
      }
      int loss = 0;
      if (rollDiceForBlockadeDamage) {
        int numberOfDice = 0;
        for (final Unit u : enemies) {
          numberOfDice += u.getUnitAttachment().getBlockade();
        }
        if (numberOfDice > 0) {
          // there is an issue with maps that have lots of rolls without any pause between them:
          // they are causing the crypted random source (ie: live and pbem games) to lock up or
          // error out so we need to slow them down a bit, until we come up with a better solution
          // (like aggregating all the chances together, then getting a ton of random numbers at
          // once instead of one at a time)
          if (PbemMessagePoster.gameDataHasPlayByEmailOrForumMessengers(data)) {
            Interruptibles.sleep(100);
          }
          final String transcript = "Rolling for Convoy Blockade Damage in " + b.getName();
          final int[] dice =
              bridge.getRandom(
                  CONVOY_BLOCKADE_DICE_SIDES,
                  numberOfDice,
                  CollectionUtils.getAny(enemies).getOwner(),
                  DiceType.BOMBING,
                  transcript);
          transcripts.add(transcript + ". Rolls: " + MyFormatter.asDice(dice));
          rolledDice = true;
          for (final int d : dice) {
            // we are zero based
            final int roll = d + 1;
            loss += (roll <= 3 ? roll : 0);
          }
        }
      } else {
        for (final Unit u : enemies) {
          loss += u.getUnitAttachment().getBlockade();
        }
      }
      if (loss <= 0) {
        continue;
      }
      final int lossForBlockade = Math.min(maxLoss, loss);
      damagePerBlockadeZone.put(b, Tuple.of(lossForBlockade, viableNeighbors));
      totalLoss += lossForBlockade;
    }
    if (totalLoss <= 0 && !rolledDice) {
      return 0;
    }
    // now we need to make sure that we didn't deal more damage than the territories are worth, in
    // the case of having
    // multiple sea zones touching the same land zone.
    final List<Territory> blockadeZonesSorted = new ArrayList<>(damagePerBlockadeZone.keySet());
    blockadeZonesSorted.sort(
        getSingleBlockadeThenHighestToLowestBlockadeDamage(damagePerBlockadeZone));
    // we want to match highest damage to largest producer first, that is why we sort twice
    final IntegerMap<Territory> totalDamageTracker = new IntegerMap<>();
    for (final Territory b : blockadeZonesSorted) {
      final Tuple<Integer, List<Territory>> tuple = damagePerBlockadeZone.get(b);
      int damageForZone = tuple.getFirst();
      final List<Territory> terrsLosingIncome = new ArrayList<>(tuple.getSecond());
      terrsLosingIncome.sort(
          getSingleNeighborBlockadesThenHighestToLowestProduction(blockadeZonesSorted, map));
      final Iterator<Territory> iter = terrsLosingIncome.iterator();
      while (damageForZone > 0 && iter.hasNext()) {
        final Territory t = iter.next();
        final int maxProductionLessPreviousDamage =
            TerritoryAttachment.getProduction(t) - totalDamageTracker.getInt(t);
        final int damageToTerr = Math.min(damageForZone, maxProductionLessPreviousDamage);
        damageForZone -= damageToTerr;
        totalDamageTracker.put(t, damageToTerr + totalDamageTracker.getInt(t));
      }
    }
    final int realTotalLoss = Math.max(0, totalDamageTracker.totalValues());
    if (rollDiceForBlockadeDamage && (realTotalLoss > 0 || !transcripts.isEmpty())) {
      final String mainline = "Total Cost from Convoy Blockades: " + realTotalLoss;
      bridge.getHistoryWriter().startEvent(mainline);
      endTurnReport.append(mainline).append("<br />");
      for (final String t : transcripts) {
        bridge.getHistoryWriter().addChildToEvent(t);
        endTurnReport.append("* ").append(t).append("<br />");
      }
      endTurnReport.append("<br />");
    }
    return realTotalLoss;
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    this.hasPostedTurnSummary = hasPostedTurnSummary;
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    return hasPostedTurnSummary;
  }

  @Override
  public boolean postTurnSummary(final PbemMessagePoster poster, final String title) {
    hasPostedTurnSummary = poster.post(bridge.getHistoryWriter(), title);
    return hasPostedTurnSummary;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return IAbstractForumPosterDelegate.class;
  }

  @VisibleForTesting
  static Comparator<Territory> getSingleNeighborBlockadesThenHighestToLowestProduction(
      final Collection<Territory> blockadeZones, final GameMap map) {
    return Comparator.nullsLast(
        (t1, t2) -> {
          if (Objects.equals(t1, t2)) {
            return 0;
          }

          // if a territory is only touching 1 blockadeZone, we must take it first
          final Collection<Territory> neighborBlockades1 = new ArrayList<>(map.getNeighbors(t1));
          neighborBlockades1.retainAll(blockadeZones);
          final int n1 = neighborBlockades1.size();
          final Collection<Territory> neighborBlockades2 = new ArrayList<>(map.getNeighbors(t2));
          neighborBlockades2.retainAll(blockadeZones);
          final int n2 = neighborBlockades2.size();
          if (n1 == 1 && n2 != 1) {
            return -1;
          }
          if (n2 == 1 && n1 != 1) {
            return 1;
          }
          return Comparator.comparing(
                  TerritoryAttachment::get,
                  Comparator.nullsFirst(
                      Comparator.comparingInt(TerritoryAttachment::getProduction)))
              .compare(t1, t2);
        });
  }

  @VisibleForTesting
  static Comparator<Territory> getSingleBlockadeThenHighestToLowestBlockadeDamage(
      final Map<Territory, Tuple<Integer, List<Territory>>> damagePerBlockadeZone) {
    return Comparator.nullsLast(
        (t1, t2) -> {
          if (Objects.equals(t1, t2)) {
            return 0;
          }

          final Tuple<Integer, List<Territory>> tuple1 = damagePerBlockadeZone.get(t1);
          final Tuple<Integer, List<Territory>> tuple2 = damagePerBlockadeZone.get(t2);
          final int num1 = tuple1.getSecond().size();
          final int num2 = tuple2.getSecond().size();
          if (num1 == 1 && num2 != 1) {
            return -1;
          }
          if (num2 == 1 && num1 != 1) {
            return 1;
          }
          final int d1 = tuple1.getFirst();
          final int d2 = tuple2.getFirst();
          return Integer.compare(d2, d1);
        });
  }
}
