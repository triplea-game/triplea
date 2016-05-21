package games.strategy.engine.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;

@RunWith(MockitoJUnitRunner.class)
public class UnitCollectionTest {

  @Mock
  private GameData mockGameData;
  private UnitType unitTypeOne;
  private UnitType unitTypeTwo;
  @Mock
  private final PlayerID defaultPlayerID = Mockito.spy(new PlayerID("Default Player", true, false, null));
  @Mock
  private PlayerID otherPlayerID;
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

  int defaultPlayerNotifyChangedCounter = 0;

  public final int getDefaultPlayerNotifyChangedCounter() {
    return defaultPlayerNotifyChangedCounter;
  }

  @Before
  public void setup() {
    unitTypeOne = new UnitType("Unit Type 1", mockGameData);
    unitTypeTwo = new UnitType("Unit Type 2", mockGameData);
    final UnitTypeList unitTypeList = new UnitTypeList(mockGameData);
    unitTypeList.addUnitType(unitTypeOne);
    unitTypeList.addUnitType(unitTypeTwo);
    Mockito.when(mockGameData.getUnitTypeList()).thenReturn(unitTypeList);

    Mockito.when(defaultPlayerID.isNull()).thenReturn(true);
    Mockito.doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        ++defaultPlayerNotifyChangedCounter;
        return null;
      }
    }).when(defaultPlayerID).notifyChanged();

    Mockito.when(otherPlayerID.isNull()).thenReturn(true);

    unitCollection = new UnitCollection(defaultPlayerID, mockGameData);

    unitDefaultPlayer1 = new Unit(unitTypeOne, defaultPlayerID, mockGameData);
    unitDefaultPlayer2 = new Unit(unitTypeTwo, defaultPlayerID, mockGameData);
    unitDefaultPlayer3 = new Unit(unitTypeTwo, defaultPlayerID, mockGameData);
    unitCountDefaultPlayerUnitTypeOne = getDefaultPlayerUnitsOfUnitTypeOne().size();
    unitCountDefaultPlayerUnitTypeTwo = getDefaultPlayerUnitsOfUnitTypeTwo().size();
    unitCountDefaultPlayer = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;

    unitOtherPlayer1 = new Unit(unitTypeOne, otherPlayerID, mockGameData);
    unitOtherPlayer2 = new Unit(unitTypeOne, otherPlayerID, mockGameData);
    unitOtherPlayer3 = new Unit(unitTypeOne, otherPlayerID, mockGameData);
    unitOtherPlayer4 = new Unit(unitTypeTwo, otherPlayerID, mockGameData);
    unitOtherPlayer5 = new Unit(unitTypeTwo, otherPlayerID, mockGameData);
    unitOtherPlayer6 = new Unit(unitTypeTwo, otherPlayerID, mockGameData);
    unitOtherPlayer7 = new Unit(unitTypeTwo, otherPlayerID, mockGameData);

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
  public void unitCollection() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultPlayerID)));
    assertThat(unitCollection.getData(), is(equalTo(mockGameData)));
  }

  @Test
  public void addUnit() {
    final int notifyChangedCountBefore = getDefaultPlayerNotifyChangedCounter();
    final Unit unitDefaultPlayer = new Unit(unitTypeOne, defaultPlayerID, mockGameData);
    unitCollection.addUnit(unitDefaultPlayer);

    assertThat(unitCollection.getUnitCount(), is(equalTo(1)));
    assertThat(unitCollection.getUnits().iterator().next(), is(equalTo(unitDefaultPlayer)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(getDefaultPlayerNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void addAllUnitsFromCollection() {
    final int notifyChangedCountBefore = getDefaultPlayerNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(getDefaultPlayerNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void addAllUnitsFromUnitCollection() {
    final int notifyChangedCountBefore = getDefaultPlayerNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    final UnitCollection unitCollectionTwo = new UnitCollection(otherPlayerID, mockGameData);
    unitCollectionTwo.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    unitCollection.addAllUnits(unitCollectionTwo);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(getDefaultPlayerNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void removeAllUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    final int notifyChangedCountBefore = getDefaultPlayerNotifyChangedCounter();
    unitCollection.removeAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(getDefaultPlayerNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void getUnitCount() {
    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.getUnitCount(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    final int expUnitCount = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(), is(equalTo(expUnitCount)));
  }

  private UnitCollection addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(final UnitCollection unitCollection) {
    unitCollection.addAllUnits(getDefaultPlayerUnitsOfUnitTypeTwo());
    return unitCollection;
  }

  private UnitCollection addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(final UnitCollection unitCollection) {
    unitCollection.addAllUnits(getDefaultPlayerUnitsOfUnitTypeOne());
    return unitCollection;
  }

  @Test
  public void getUnitCountByUnitType() {
    assertThat(unitCollection.getUnitCount(unitTypeOne), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));

    final UnitCollection allUnitsUnitCollection = addAllOtherPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeOne), is(equalTo(unitCountUnitTypeOne)));
    assertThat(allUnitsUnitCollection.getUnitCount(unitTypeTwo), is(equalTo(unitCountUnitTypeTwo)));
  }

  private UnitCollection addAllOtherPlayerUnitsToUnitCollection(final UnitCollection unitCollection) {
    unitCollection.addAllUnits(getOtherPlayerUnitsOfUnitTypeOne());
    unitCollection.addAllUnits(getOtherPlayerUnitsOfUnitTypeTwo());
    return unitCollection;
  }

  private UnitCollection addAllPlayerUnitsToUnitCollection(final UnitCollection unitCollection) {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final UnitCollection allPlayerUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    return allPlayerUnitCollection;
  }

  private UnitCollection addAllDefaultPlayerUnitsToUnitCollection(final UnitCollection unitCollection) {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    final UnitCollection allUnitsOfDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    return allUnitsOfDefaultPlayerUnitCollection;
  }

  @Test
  public void getUnitCountByUnitTypeAndPlayerID() {
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeOne, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeTwo, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeOne, otherPlayerID),
        is(equalTo(unitCountOtherPlayerUnitTypeOne)));
    assertThat(allPlayersUnitCollection.getUnitCount(unitTypeTwo, otherPlayerID),
        is(equalTo(unitCountOtherPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitCountByPlayerID() {
    assertThat(unitCollection.getUnitCount(defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(otherPlayerID), is(equalTo(0)));

    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(allDefaultPlayerUnitCollection.getUnitCount(otherPlayerID), is(equalTo(0)));

    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(allPlayersUnitCollection.getUnitCount(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void containsAll() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeOne = getDefaultPlayerUnitsOfUnitTypeOne();
    assertThat(unitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(false)));
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getUnitsByUnitTypeAndMaxValue() {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getUnits(unitTypeTwo, Integer.MAX_VALUE).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(allDefaultPlayerUnitCollection.getUnits(unitTypeTwo, unitCountDefaultPlayerUnitTypeTwo).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByType() {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByTypeWithPlayerID() {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByTypeOnlyDefaultPlayer =
        allDefaultPlayerUnitCollection.getUnitsByType(defaultPlayerID);
    assertThat(unitsByTypeOnlyDefaultPlayer.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByTypeOnlyDefaultPlayer.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    final IntegerMap<UnitType> unitsByTypeBothPlayers = allPlayersUnitCollection.getUnitsByType(defaultPlayerID);
    assertThat(unitsByTypeBothPlayers.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByTypeBothPlayers.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsWithUnityByTypeIntegerMap() {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<UnitType> unitsByType = allDefaultPlayerUnitCollection.getUnitsByType();
    final Collection<Unit> expAllUnitsOfDefaultPlayer = allDefaultPlayerUnitCollection.getUnits(unitsByType);
    assertThat(expAllUnitsOfDefaultPlayer.size(), is(equalTo(unitCountDefaultPlayer)));
  }

  @Test
  public void size() {
    assertThat(unitCollection.size(), is(equalTo(0)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.size(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void isEmpty() {
    assertThat(unitCollection.isEmpty(), is(equalTo(true)));
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.isEmpty(), is(equalTo(false)));
  }

  @Test
  public void getUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getOtherPlayerUnitsOfUnitTypeOne();
    assertThat(unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(false)));
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    assertThat(unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getPlayersWithUnits() {
    assertThat(unitCollection.getPlayersWithUnits().size(), is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getPlayersWithUnits().size(), is(equalTo(1)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getPlayersWithUnits().size(), is(equalTo(2)));
  }

  @Test
  public void getPlayerUnitCounts() {
    final UnitCollection allPlayerUnitCollection = addAllPlayerUnitsToUnitCollection(unitCollection);
    final IntegerMap<PlayerID> playerUnitCounts = allPlayerUnitCollection.getPlayerUnitCounts();
    assertThat(playerUnitCounts.getInt(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(playerUnitCounts.getInt(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void hasUnitsFromMultiplePlayers() {
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(false)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(true)));
  }

  @Test
  public void getHolder() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultPlayerID)));
  }

  @Test
  public void allMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(true)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(false)));
  }

  @Test
  public void someMatch() {
    final UnitCollection defaultPlayerUnitsOfUnitTypeOneUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeOneToUnitCollection(unitCollection);
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.someMatch(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(true)));
    assertThat(defaultPlayerUnitsOfUnitTypeOneUnitCollection.someMatch(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(false)));
    final UnitCollection allDefaultPlayerUnitCollection =
        addAllDefaultPlayerUnitsOfUnitTypeTwoToUnitCollection(defaultPlayerUnitsOfUnitTypeOneUnitCollection);
    assertThat(allDefaultPlayerUnitCollection.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    assertThat(allDefaultPlayerUnitCollection.someMatch(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(true)));
  }

  @Test
  public void countMatches() {
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(allDefaultPlayerUnitCollection.countMatches(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getMatches() {
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(0)));
    final UnitCollection allDefaultPlayerUnitCollection = addAllDefaultPlayerUnitsToUnitCollection(unitCollection);
    assertThat(allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(allDefaultPlayerUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    final UnitCollection allPlayersUnitCollection =
        addAllOtherPlayerUnitsToUnitCollection(allDefaultPlayerUnitCollection);
    assertThat(allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(unitCountUnitTypeOne)));
    assertThat(allPlayersUnitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(),
        is(equalTo(unitCountUnitTypeTwo)));
  }

  @Test
  public void iterator() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeTwo = getDefaultPlayerUnitsOfUnitTypeTwo();
    unitCollection.addAllUnits(unitsOfDefaultPlayerOfUnitTypeTwo);
    final Iterator<Unit> collectionIterator = unitsOfDefaultPlayerOfUnitTypeTwo.iterator();
    final Iterator<Unit> unitCollectionIterator = unitCollection.iterator();
    unitCollectionIterator.forEachRemaining(u -> assertThat(u, is(collectionIterator.next())));
  }

}
