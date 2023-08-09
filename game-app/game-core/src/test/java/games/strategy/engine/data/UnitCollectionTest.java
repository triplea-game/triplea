package games.strategy.engine.data;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.IntegerMap;

@ExtendWith(MockitoExtension.class)
class UnitCollectionTest {

  private final GameData mockGameData = givenGameData().build();
  private UnitType unitTypeOne;
  private UnitType unitTypeTwo;
  private final GamePlayer defaultGamePlayer =
      Mockito.spy(new GamePlayer("Default Player", true, false, null, false, null));
  @Mock private GamePlayer otherGamePlayer;
  private UnitCollection unitCollection;

  private Unit unitDefaultPlayer1;
  private Unit unitDefaultPlayer2;
  private Unit unitDefaultPlayer3;
  private int unitCountDefaultPlayerUnitTypeOne;
  private int unitCountDefaultPlayerUnitTypeTwo;
  private int unitCountDefaultPlayer;

  private Unit unitOtherPlayer1;
  private Unit unitOtherPlayer2;
  private Unit unitOtherPlayer3;
  private Unit unitOtherPlayer4;
  private Unit unitOtherPlayer5;
  private Unit unitOtherPlayer6;
  private Unit unitOtherPlayer7;
  private int unitCountOtherPlayerUnitTypeOne;
  private int unitCountOtherPlayerUnitTypeTwo;
  private int unitCountOtherPlayer;

  private int unitCountUnitTypeOne;
  private int unitCountUnitTypeTwo;

  @BeforeEach
  void setUp() {
    unitTypeOne = new UnitType("Unit Type 1", mockGameData);
    unitTypeTwo = new UnitType("Unit Type 2", mockGameData);

    unitCollection = new UnitCollection(defaultGamePlayer, mockGameData);

    unitDefaultPlayer1 = new Unit(unitTypeOne, defaultGamePlayer, mockGameData);
    unitDefaultPlayer2 = new Unit(unitTypeTwo, defaultGamePlayer, mockGameData);
    unitDefaultPlayer3 = new Unit(unitTypeTwo, defaultGamePlayer, mockGameData);
    unitCountDefaultPlayerUnitTypeOne = getDefaultPlayerUnitsOfUnitTypeOne().size();
    unitCountDefaultPlayerUnitTypeTwo = getDefaultPlayerUnitsOfUnitTypeTwo().size();
    unitCountDefaultPlayer = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;

    unitOtherPlayer1 = new Unit(unitTypeOne, otherGamePlayer, mockGameData);
    unitOtherPlayer2 = new Unit(unitTypeOne, otherGamePlayer, mockGameData);
    unitOtherPlayer3 = new Unit(unitTypeOne, otherGamePlayer, mockGameData);
    unitOtherPlayer4 = new Unit(unitTypeTwo, otherGamePlayer, mockGameData);
    unitOtherPlayer5 = new Unit(unitTypeTwo, otherGamePlayer, mockGameData);
    unitOtherPlayer6 = new Unit(unitTypeTwo, otherGamePlayer, mockGameData);
    unitOtherPlayer7 = new Unit(unitTypeTwo, otherGamePlayer, mockGameData);

    unitCountOtherPlayerUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne().size();
    unitCountOtherPlayerUnitTypeTwo = getOtherPlayerUnitsOfUnitTypeTwo().size();
    unitCountOtherPlayer = unitCountOtherPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeTwo;

    unitCountUnitTypeOne = unitCountDefaultPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeOne;
    unitCountUnitTypeTwo = unitCountDefaultPlayerUnitTypeTwo + unitCountOtherPlayerUnitTypeTwo;
  }

  private Collection<Unit> getDefaultPlayerUnitsOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<>();
    units.add(unitDefaultPlayer1);
    return units;
  }

  private Collection<Unit> getDefaultPlayerUnitsOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<>();
    units.add(unitDefaultPlayer2);
    units.add(unitDefaultPlayer3);
    return units;
  }

  private Collection<Unit> getOtherPlayerUnitsOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<>();
    units.add(unitOtherPlayer1);
    units.add(unitOtherPlayer2);
    units.add(unitOtherPlayer3);
    return units;
  }

  private Collection<Unit> getOtherPlayerUnitsOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<>();
    units.add(unitOtherPlayer4);
    units.add(unitOtherPlayer5);
    units.add(unitOtherPlayer6);
    units.add(unitOtherPlayer7);
    return units;
  }

  @Test
  void unitCollection() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultGamePlayer)));
    assertThat(unitCollection.getData(), is(equalTo(mockGameData)));
  }

  @Test
  void add() {
    final Unit unitDefaultPlayer = new Unit(unitTypeOne, defaultGamePlayer, mockGameData);
    unitCollection.add(unitDefaultPlayer);

    assertThat(unitCollection.getUnitCount(), is(equalTo(1)));
    assertThat(unitCollection.getUnits().iterator().next(), is(equalTo(unitDefaultPlayer)));
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void addAllFromCollection() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void addAllFromUnitCollection() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    final UnitCollection unitCollectionTwo = new UnitCollection(otherGamePlayer, mockGameData);
    unitCollectionTwo.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    unitCollection.addAll(unitCollectionTwo);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void removeAll() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    reset(defaultGamePlayer);
    unitCollection.removeAll(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void getUnitCount() {
    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
        defaultPlayerUnitsOfUnitTypeOneUnitCollection.getUnitCount(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    final int expUnitCount = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(), is(equalTo(expUnitCount)));
  }

  private UnitCollection addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
      final UnitCollection unitCollection) {
    unitCollection.addAll(getDefaultPlayerUnitsOfUnitTypeTwo());
    return unitCollection;
  }

  private UnitCollection addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(
      final UnitCollection unitCollection) {
    unitCollection.addAll(getDefaultPlayerUnitsOfUnitTypeOne());
    return unitCollection;
  }

  @Test
  void getUnitCountByUnitType() {
    assertThat(unitCollection.getUnitCount(unitTypeOne), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));

    final UnitCollection allUnitsUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeOne), is(equalTo(unitCountUnitTypeOne)));
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeTwo), is(equalTo(unitCountUnitTypeTwo)));
  }

  private UnitCollection addAllOtherPlayerUnitsToUnitCollection(
      final UnitCollection unitCollection) {
    unitCollection.addAll(getOtherPlayerUnitsOfUnitTypeOne());
    unitCollection.addAll(getOtherPlayerUnitsOfUnitTypeTwo());
    return unitCollection;
  }

  private UnitCollection addAllPlayerUnitsToUnitCollection(final UnitCollection unitCollection) {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final UnitCollection allPlayerUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    return allPlayerUnitCollection;
  }

  private UnitCollection addAllDefaultPlayerUnitsToUnitCollection(
      final UnitCollection unitCollection) {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    final UnitCollection allUnitsOfDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    return allUnitsOfDefaultPlayerUnitCollection;
  }

  @Test
  void getUnitCountByUnitTypeAndPlayerId() {
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultGamePlayer), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherGamePlayer), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherGamePlayer), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, otherGamePlayer), is(equalTo(0)));
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, otherGamePlayer), is(equalTo(0)));

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(
        allPlayersUnitCollection.getUnitCount(unitTypeOne, defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        allPlayersUnitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(
        allPlayersUnitCollection.getUnitCount(unitTypeOne, otherGamePlayer),
        is(equalTo(unitCountOtherPlayerUnitTypeOne)));
    assertThat(
        allPlayersUnitCollection.getUnitCount(unitTypeTwo, otherGamePlayer),
        is(equalTo(unitCountOtherPlayerUnitTypeTwo)));
  }

  @Test
  void getUnitCountByPlayerId() {
    assertThat(unitCollection.getUnitCount(defaultGamePlayer), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(otherGamePlayer), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.getUnitCount(defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayer)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(otherGamePlayer), is(equalTo(0)));

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(
        allPlayersUnitCollection.getUnitCount(defaultGamePlayer),
        is(equalTo(unitCountDefaultPlayer)));
    assertThat(
        allPlayersUnitCollection.getUnitCount(otherGamePlayer), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  void containsAll() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeOne = getDefaultPlayerUnitsOfUnitTypeOne();
    assertThat(unitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(false)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne),
        is(equalTo(true)));
  }

  @Test
  void getUnitsByUnitTypeAndMaxValue() {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.getUnits(unitTypeTwo, Integer.MAX_VALUE).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(
        allDefaultPlayerUnitCollection
            .getUnits(unitTypeTwo, unitCountDefaultPlayerUnitTypeTwo)
            .size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  void getUnitsByType() {
    givenUnitTypeList();
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  private void givenUnitTypeList() {
    final UnitTypeList unitTypeList = new UnitTypeList(mockGameData);
    unitTypeList.addUnitType(unitTypeOne);
    unitTypeList.addUnitType(unitTypeTwo);
    when(mockGameData.getUnitTypeList()).thenReturn(unitTypeList);
  }

  @Test
  void getUnitsByTypeWithPlayerId() {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByTypeOnlyDefaultPlayer =
        allDefaultPlayerUnitCollection.getUnitsByType(defaultGamePlayer);
    assertThat(
        unitsByTypeOnlyDefaultPlayer.getInt(unitTypeOne),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        unitsByTypeOnlyDefaultPlayer.getInt(unitTypeTwo),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    final IntegerMap<UnitType> unitsByTypeBothPlayers =
        allPlayersUnitCollection.getUnitsByType(defaultGamePlayer);
    assertThat(
        unitsByTypeBothPlayers.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        unitsByTypeBothPlayers.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  void getUnitsWithUnityByTypeIntegerMap() {
    givenUnitTypeList();
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    final Collection<Unit> expAllUnitsOfDefaultPlayer =
        allDefaultPlayerUnitCollection.getUnits(unitsByType);
    assertThat(expAllUnitsOfDefaultPlayer.size(), is(equalTo(unitCountDefaultPlayer)));
  }

  @Test
  void size() {
    assertThat(unitCollection.size(), is(equalTo(0)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
        defaultPlayerUnitsOfUnitTypeOneUnitCollection.size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  void isEmpty() {
    assertThat(unitCollection.isEmpty(), is(equalTo(true)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.isEmpty(), is(equalTo(false)));
  }

  @Test
  void getUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    assertThat(
        unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(false)));
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    assertThat(
        unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  void getPlayersWithUnits() {
    assertThat(unitCollection.getPlayersWithUnits().size(), is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getPlayersWithUnits().size(), is(equalTo(1)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getPlayersWithUnits().size(), is(equalTo(2)));
  }

  @Test
  void getPlayerUnitCounts() {
    final UnitCollection allPlayerUnitCollection =
        addAllPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<GamePlayer> playerUnitCounts = allPlayerUnitCollection.getPlayerUnitCounts();
    assertThat(playerUnitCounts.getInt(defaultGamePlayer), is(equalTo(unitCountDefaultPlayer)));
    assertThat(playerUnitCounts.getInt(otherGamePlayer), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  void hasUnitsFromMultiplePlayers() {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(false)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(true)));
  }

  @Test
  void getHolder() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultGamePlayer)));
  }

  @Test
  void allMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
        defaultPlayerUnitsOfUnitTypeOneUnitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(true)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(false)));
  }

  @Test
  void anyMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
        defaultPlayerUnitsOfUnitTypeOneUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(true)));
    assertThat(
        defaultPlayerUnitsOfUnitTypeOneUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(false)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(true)));
    assertThat(
        allDefaultPlayerUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(true)));
  }

  @Test
  void countMatches() {
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)), is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  void getMatches() {
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(), is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(
        allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(
        allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(
        allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(unitCountUnitTypeOne)));
    assertThat(
        allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(),
        is(equalTo(unitCountUnitTypeTwo)));
  }

  @Test
  void iterator() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeTwo = getDefaultPlayerUnitsOfUnitTypeTwo();
    unitCollection.addAll(unitsOfDefaultPlayerOfUnitTypeTwo);
    final Iterator<Unit> collectionIterator = unitsOfDefaultPlayerOfUnitTypeTwo.iterator();
    final Iterator<Unit> unitCollectionIterator = unitCollection.iterator();
    unitCollectionIterator.forEachRemaining(u -> assertThat(u, is(collectionIterator.next())));
  }
}
