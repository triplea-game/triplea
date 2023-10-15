package games.strategy.triplea.delegate;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.formatter.MyFormatter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * This delegate sets up the game according to Risk rules, with a few allowed customizations. Either
 * divide all neutral territories between players randomly, or let them pick one by one. After that,
 * any remaining units get placed one by one. (Note that player may not be used here, because this
 * delegate is not run by any player [it is null])
 */
public class RandomStartDelegate extends BaseTripleADelegate {
  private static final int UNITS_PER_PICK = 1;
  private GamePlayer currentPickingPlayer = null;

  @Override
  public void start() {
    super.start();
    setupBoard();
  }

  @Override
  public void end() {
    super.end();
    currentPickingPlayer = null;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return !(getData().getMap().getTerritories().stream().noneMatch(getTerritoryPickableMatch())
        && getData().getPlayerList().getPlayers().stream().noneMatch(getPlayerCanPickMatch()));
  }

  @Override
  public Serializable saveState() {
    final RandomStartExtendedDelegateState state = new RandomStartExtendedDelegateState();
    state.superState = super.saveState();
    state.currentPickingPlayer = this.currentPickingPlayer;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final RandomStartExtendedDelegateState s = (RandomStartExtendedDelegateState) state;
    super.loadState(s.superState);
    this.currentPickingPlayer = s.currentPickingPlayer;
  }

  private void setupBoard() {
    final GameData data = getData();
    final boolean randomTerritories =
        Properties.getTerritoriesAreAssignedRandomly(data.getProperties());
    final Predicate<Territory> pickableTerritoryMatch = getTerritoryPickableMatch();
    final Predicate<GamePlayer> playerCanPickMatch = getPlayerCanPickMatch();
    final List<Territory> allPickableTerritories =
        CollectionUtils.getMatches(data.getMap().getTerritories(), pickableTerritoryMatch);
    final List<GamePlayer> playersCanPick =
        CollectionUtils.getMatches(data.getPlayerList().getPlayers(), playerCanPickMatch);
    // we need a main event
    if (!playersCanPick.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Assigning Territories");
    }
    // for random:
    final int[] hitRandom =
        (!randomTerritories
            ? new int[0]
            : bridge.getRandom(
                allPickableTerritories.size(),
                allPickableTerritories.size(),
                null,
                DiceType.ENGINE,
                "Picking random territories"));
    int i = 0;
    int pos = 0;
    // divvy up territories
    while (!allPickableTerritories.isEmpty() && !playersCanPick.isEmpty()) {
      if (currentPickingPlayer == null || !playersCanPick.contains(currentPickingPlayer)) {
        currentPickingPlayer = playersCanPick.get(0);
      }
      Territory picked;
      if (randomTerritories) {
        pos += hitRandom[i];
        i++;
        final IntegerMap<UnitType> costs = bridge.getCostsForTuv(currentPickingPlayer);
        final List<Unit> units = new ArrayList<>(currentPickingPlayer.getUnits());

        units.sort(Comparator.comparingInt(unit -> costs.getInt(unit.getType())));
        final Set<Unit> unitsToPlace = new HashSet<>();
        unitsToPlace.add(units.get(0));
        picked = allPickableTerritories.get(pos % allPickableTerritories.size());
        final CompositeChange change = new CompositeChange();
        change.add(ChangeFactory.changeOwner(picked, currentPickingPlayer));
        final Collection<Unit> factoryAndInfrastructure =
            CollectionUtils.getMatches(unitsToPlace, Matches.unitIsInfrastructure());
        if (!factoryAndInfrastructure.isEmpty()) {
          change.add(
              OriginalOwnerTracker.addOriginalOwnerChange(
                  factoryAndInfrastructure, currentPickingPlayer));
        }
        change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
        change.add(ChangeFactory.addUnits(picked, unitsToPlace));
        bridge
            .getHistoryWriter()
            .addChildToEvent(
                currentPickingPlayer.getName()
                    + " receives territory "
                    + picked.getName()
                    + " with units "
                    + MyFormatter.unitsToTextNoOwner(unitsToPlace),
                picked);
        bridge.addChange(change);
      } else {
        Set<Unit> unitsToPlace;
        while (true) {
          final Tuple<Territory, Set<Unit>> pick =
              getRemotePlayer(currentPickingPlayer)
                  .pickTerritoryAndUnits(
                      new ArrayList<>(allPickableTerritories),
                      new ArrayList<>(currentPickingPlayer.getUnits()),
                      UNITS_PER_PICK);
          picked = pick.getFirst();
          unitsToPlace = pick.getSecond();
          if (!allPickableTerritories.contains(picked)
              || !currentPickingPlayer.getUnits().containsAll(unitsToPlace)
              || unitsToPlace.size() > UNITS_PER_PICK
              || (unitsToPlace.size() < UNITS_PER_PICK
                  && unitsToPlace.size() < currentPickingPlayer.getUnits().size())) {
            getRemotePlayer(currentPickingPlayer)
                .reportMessage(
                    "Chosen territory or units invalid!", "Chosen territory or units invalid!");
          } else {
            break;
          }
        }
        final CompositeChange change = new CompositeChange();
        change.add(ChangeFactory.changeOwner(picked, currentPickingPlayer));
        final Collection<Unit> factoryAndInfrastructure =
            CollectionUtils.getMatches(unitsToPlace, Matches.unitIsInfrastructure());
        if (!factoryAndInfrastructure.isEmpty()) {
          change.add(
              OriginalOwnerTracker.addOriginalOwnerChange(
                  factoryAndInfrastructure, currentPickingPlayer));
        }
        change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
        change.add(ChangeFactory.addUnits(picked, unitsToPlace));
        bridge
            .getHistoryWriter()
            .addChildToEvent(
                currentPickingPlayer.getName()
                    + " picks territory "
                    + picked.getName()
                    + " and places in it "
                    + MyFormatter.unitsToTextNoOwner(unitsToPlace),
                unitsToPlace);
        bridge.addChange(change);
      }
      allPickableTerritories.remove(picked);
      final GamePlayer lastPlayer = currentPickingPlayer;
      currentPickingPlayer = getNextPlayer(playersCanPick, currentPickingPlayer);
      if (!playerCanPickMatch.test(lastPlayer)) {
        playersCanPick.remove(lastPlayer);
      }
      if (playersCanPick.isEmpty()) {
        currentPickingPlayer = null;
      }
    }
    // place any remaining units
    while (!playersCanPick.isEmpty()) {
      if (currentPickingPlayer == null || !playersCanPick.contains(currentPickingPlayer)) {
        currentPickingPlayer = playersCanPick.get(0);
      }
      final List<Territory> territoriesToPickFrom =
          data.getMap().getTerritoriesOwnedBy(currentPickingPlayer);
      Territory picked;
      Set<Unit> unitsToPlace;
      while (true) {
        final Tuple<Territory, Set<Unit>> pick =
            getRemotePlayer(currentPickingPlayer)
                .pickTerritoryAndUnits(
                    new ArrayList<>(territoriesToPickFrom),
                    new ArrayList<>(currentPickingPlayer.getUnits()),
                    UNITS_PER_PICK);
        picked = pick.getFirst();
        unitsToPlace = pick.getSecond();
        if (!territoriesToPickFrom.contains(picked)
            || !currentPickingPlayer.getUnits().containsAll(unitsToPlace)
            || unitsToPlace.size() > UNITS_PER_PICK
            || (unitsToPlace.size() < UNITS_PER_PICK
                && unitsToPlace.size() < currentPickingPlayer.getUnits().size())) {
          getRemotePlayer(currentPickingPlayer)
              .reportMessage(
                  "Chosen territory or units invalid!", "Chosen territory or units invalid!");
        } else {
          break;
        }
      }
      final CompositeChange change = new CompositeChange();
      final Collection<Unit> factoryAndInfrastructure =
          CollectionUtils.getMatches(unitsToPlace, Matches.unitIsInfrastructure());
      if (!factoryAndInfrastructure.isEmpty()) {
        change.add(
            OriginalOwnerTracker.addOriginalOwnerChange(
                factoryAndInfrastructure, currentPickingPlayer));
      }
      change.add(ChangeFactory.removeUnits(currentPickingPlayer, unitsToPlace));
      change.add(ChangeFactory.addUnits(picked, unitsToPlace));
      bridge
          .getHistoryWriter()
          .addChildToEvent(
              currentPickingPlayer.getName()
                  + " places "
                  + MyFormatter.unitsToTextNoOwner(unitsToPlace)
                  + " in territory "
                  + picked.getName(),
              unitsToPlace);
      bridge.addChange(change);
      final GamePlayer lastPlayer = currentPickingPlayer;
      currentPickingPlayer = getNextPlayer(playersCanPick, currentPickingPlayer);
      if (!playerCanPickMatch.test(lastPlayer)) {
        playersCanPick.remove(lastPlayer);
      }
      if (playersCanPick.isEmpty()) {
        currentPickingPlayer = null;
      }
    }
  }

  private static GamePlayer getNextPlayer(
      final List<GamePlayer> playersCanPick, final GamePlayer currentPlayer) {
    int index = playersCanPick.indexOf(currentPlayer);
    if (index == -1) {
      return null;
    }
    index++;
    if (index >= playersCanPick.size()) {
      index = 0;
    }
    return playersCanPick.get(index);
  }

  /** Returns a new Predicate returning true for all pickable territories. */
  private static Predicate<Territory> getTerritoryPickableMatch() {
    return Matches.territoryIsLand()
        .and(Matches.territoryIsNotImpassable())
        .and(Matches.isTerritoryNeutral())
        .and(Matches.territoryIsEmpty());
  }

  private static Predicate<GamePlayer> getPlayerCanPickMatch() {
    return player ->
        player != null
            && !player.isNull()
            && !player.getUnitCollection().isEmpty()
            && !player.getIsDisabled();
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return null;
  }
}
