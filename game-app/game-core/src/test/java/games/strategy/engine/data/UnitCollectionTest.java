package games.strategy.engine.data;

import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
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
    assertThat(unitCollection.getHolder()).isEqualTo(defaultGamePlayer);
    assertThat(unitCollection.getData()).isEqualTo(mockGameData);
  }

  @Test
  void add() {
    final Unit unitDefaultPlayer = new Unit(unitTypeOne, defaultGamePlayer, mockGameData);
    unitCollection.add(unitDefaultPlayer);

    assertThat(unitCollection.getUnitCount()).isEqualTo(1);
    assertThat(unitCollection.getUnits().iterator().next()).isEqualTo(unitDefaultPlayer);
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void addAllFromCollection() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount()).isEqualTo(unitsOfOtherPlayerOfUnitTypeOne.size());
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void addAllFromUnitCollection() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    final UnitCollection unitCollectionTwo = new UnitCollection(otherGamePlayer, mockGameData);
    unitCollectionTwo.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    unitCollection.addAll(unitCollectionTwo);

    assertThat(unitCollection.getUnitCount()).isEqualTo(unitsOfOtherPlayerOfUnitTypeOne.size());
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void removeAll() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    reset(defaultGamePlayer);
    unitCollection.removeAll(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount()).isEqualTo(0);
    verify(defaultGamePlayer).notifyChanged();
  }

  @Test
  void getUnitCount() {
    assertThat(unitCollection.getUnitCount()).isEqualTo(0);
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.getUnitCount())
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    final int expUnitCount = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
    assertThat(allDefaultPlayerUnitCollection.getUnitCount()).isEqualTo(expUnitCount);
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
    assertThat(unitCollection.getUnitCount(unitTypeOne)).isEqualTo(0);
    assertThat(unitCollection.getUnitCount(unitTypeTwo)).isEqualTo(0);

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);

    final UnitCollection allUnitsUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeOne)).isEqualTo(unitCountUnitTypeOne);
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeTwo)).isEqualTo(unitCountUnitTypeTwo);
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
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultGamePlayer)).isEqualTo(0);
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer)).isEqualTo(0);
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherGamePlayer)).isEqualTo(0);
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherGamePlayer)).isEqualTo(0);

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, otherGamePlayer))
        .isEqualTo(0);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, otherGamePlayer))
        .isEqualTo(0);

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeOne, defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeTwo, defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeOne, otherGamePlayer))
        .isEqualTo(unitCountOtherPlayerUnitTypeOne);
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeTwo, otherGamePlayer))
        .isEqualTo(unitCountOtherPlayerUnitTypeTwo);
  }

  @Test
  void getUnitCountByPlayerId() {
    assertThat(unitCollection.getUnitCount(defaultGamePlayer)).isEqualTo(0);
    assertThat(unitCollection.getUnitCount(otherGamePlayer)).isEqualTo(0);

    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayer);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(otherGamePlayer)).isEqualTo(0);

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getUnitCount(defaultGamePlayer))
        .isEqualTo(unitCountDefaultPlayer);
    assertThat(allPlayersUnitCollection.getUnitCount(otherGamePlayer))
        .isEqualTo(unitCountOtherPlayer);
  }

  @Test
  void containsAll() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeOne = getDefaultPlayerUnitsOfUnitTypeOne();
    assertThat(unitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne)).isEqualTo(false);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne))
        .isEqualTo(true);
  }

  @Test
  void getUnitsByUnitTypeAndMaxValue() {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnits(unitTypeTwo, Integer.MAX_VALUE).size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
    assertThat(
            allDefaultPlayerUnitCollection
                .getUnits(unitTypeTwo, unitCountDefaultPlayerUnitTypeTwo)
                .size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
  }

  @Test
  void getUnitsByType() {
    givenUnitTypeList();
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    assertThat(unitsByType.getInt(unitTypeOne)).isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(unitsByType.getInt(unitTypeTwo)).isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
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
    assertThat(unitsByTypeOnlyDefaultPlayer.getInt(unitTypeOne))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(unitsByTypeOnlyDefaultPlayer.getInt(unitTypeTwo))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    final IntegerMap<UnitType> unitsByTypeBothPlayers =
        allPlayersUnitCollection.getUnitsByType(defaultGamePlayer);
    assertThat(unitsByTypeBothPlayers.getInt(unitTypeOne))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(unitsByTypeBothPlayers.getInt(unitTypeTwo))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
  }

  @Test
  void getUnitsWithUnityByTypeIntegerMap() {
    givenUnitTypeList();
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    final Collection<Unit> expAllUnitsOfDefaultPlayer =
        allDefaultPlayerUnitCollection.getUnits(unitsByType);
    assertThat(expAllUnitsOfDefaultPlayer.size()).isEqualTo(unitCountDefaultPlayer);
  }

  @Test
  void size() {
    assertThat(unitCollection.size()).isEqualTo(0);
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo);
  }

  @Test
  void isEmpty() {
    assertThat(unitCollection.isEmpty()).isEqualTo(true);
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.isEmpty()).isEqualTo(false);
  }

  @Test
  void getUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    assertThat(unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne))
        .isEqualTo(false);
    unitCollection.addAll(unitsOfOtherPlayerOfUnitTypeOne);
    assertThat(unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne))
        .isEqualTo(true);
  }

  @Test
  void getPlayersWithUnits() {
    assertThat(unitCollection.getPlayersWithUnits().size()).isEqualTo(0);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getPlayersWithUnits().size()).isEqualTo(1);
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getPlayersWithUnits().size()).isEqualTo(2);
  }

  @Test
  void getPlayerUnitCounts() {
    final UnitCollection allPlayerUnitCollection =
        addAllPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<GamePlayer> playerUnitCounts = allPlayerUnitCollection.getPlayerUnitCounts();
    assertThat(playerUnitCounts.getInt(defaultGamePlayer)).isEqualTo(unitCountDefaultPlayer);
    assertThat(playerUnitCounts.getInt(otherGamePlayer)).isEqualTo(unitCountOtherPlayer);
  }

  @Test
  void hasUnitsFromMultiplePlayers() {
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.hasUnitsFromMultiplePlayers()).isEqualTo(false);
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.hasUnitsFromMultiplePlayers()).isEqualTo(true);
  }

  @Test
  void getHolder() {
    assertThat(unitCollection.getHolder()).isEqualTo(defaultGamePlayer);
  }

  @Test
  void allMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection.allMatch(
                Matches.unitIsOfType(unitTypeOne)))
        .isEqualTo(true);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)))
        .isEqualTo(false);
  }

  @Test
  void anyMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection.anyMatch(
                Matches.unitIsOfType(unitTypeOne)))
        .isEqualTo(true);
    assertThat(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection.anyMatch(
                Matches.unitIsOfType(unitTypeTwo)))
        .isEqualTo(false);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(
            defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeOne)))
        .isEqualTo(true);
    assertThat(allDefaultPlayerUnitCollection.anyMatch(Matches.unitIsOfType(unitTypeTwo)))
        .isEqualTo(true);
  }

  @Test
  void countMatches() {
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeOne))).isEqualTo(0);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)))
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeTwo)))
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
  }

  @Test
  void getMatches() {
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size()).isEqualTo(0);
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeOne);
    assertThat(allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size())
        .isEqualTo(unitCountDefaultPlayerUnitTypeTwo);
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size())
        .isEqualTo(unitCountUnitTypeOne);
    assertThat(allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size())
        .isEqualTo(unitCountUnitTypeTwo);
  }

  @Test
  void iterator() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeTwo = getDefaultPlayerUnitsOfUnitTypeTwo();
    unitCollection.addAll(unitsOfDefaultPlayerOfUnitTypeTwo);
    final Iterator<Unit> collectionIterator = unitsOfDefaultPlayerOfUnitTypeTwo.iterator();
    final Iterator<Unit> unitCollectionIterator = unitCollection.iterator();
    unitCollectionIterator.forEachRemaining(
        u -> assertThat(u).isEqualTo(collectionIterator.next()));
  }
}
