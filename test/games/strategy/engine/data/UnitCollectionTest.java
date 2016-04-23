package games.strategy.engine.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;

public class UnitCollectionTest {

  @Mock
  private final GameData gameData = Mockito.mock(GameData.class);

  @Before
  public void setup() {
    final UnitTypeList unitTypeList = new UnitTypeList(gameData);
    unitTypeList.addUnitType(unitTypeOne);
    unitTypeList.addUnitType(unitTypeTwo);
    Mockito.when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
  }

  final private UnitType unitTypeOne = new UnitType("Unit Type 1", gameData);
  final private UnitType unitTypeTwo = new UnitType("Unit Type 2", gameData);
  @Mock
  final private MyPlayerID defaultPlayerID = new MyPlayerID();
  final private PlayerID otherPlayerID = PlayerID.NULL_PLAYERID;
  final private UnitCollection unitCollection = new UnitCollection(defaultPlayerID, gameData);

  final private int unitCountDefaultPlayerUnitTypeOne = getUnitsOfDefaultPlayerOfUnitTypeOne().size();

  private Collection<Unit> getUnitsOfDefaultPlayerOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitDefaultPlayer1);
    return units;
  }

  final private int unitCountDefaultPlayerUnitTypeTwo = getUnitsOfDefaultPlayerOfUnitTypeTwo().size();

  private Collection<Unit> getUnitsOfDefaultPlayerOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitDefaultPlayer2);
    units.add(unitDefaultPlayer3);
    return units;
  }

  final private int unitCountDefaultPlayer = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
  final private Unit unitDefaultPlayer1 = new Unit(unitTypeOne, defaultPlayerID, gameData);
  final private Unit unitDefaultPlayer2 = new Unit(unitTypeTwo, defaultPlayerID, gameData);
  final private Unit unitDefaultPlayer3 = new Unit(unitTypeTwo, defaultPlayerID, gameData);

  final private int unitCountOtherPlayerUnitTypeOne = 3;
  final private int unitCountOtherPlayerUnitTypeTwo = 4;
  final int unitCountOtherPlayer = unitCountOtherPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeTwo;

  final int unitCountUnitTypeOne = unitCountDefaultPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeOne;
  final int unitCountUnitTypeTwo = unitCountDefaultPlayerUnitTypeTwo + unitCountOtherPlayerUnitTypeTwo;

  final Unit unitOtherPlayer1 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer2 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer3 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer4 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer5 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer6 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer7 = new Unit(unitTypeTwo, otherPlayerID, gameData);

  @Test
  public void unitCollection() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultPlayerID)));
    assertThat(unitCollection.getData(), is(equalTo(gameData)));
  }

  @Test
  public void addUnit() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    unitCollection.addUnit(unitDefaultPlayer1);

    assertThat(unitCollection.getUnitCount(), is(equalTo(1)));
    assertThat(unitCollection.getUnits().iterator().next(), is(equalTo(unitDefaultPlayer1)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void addAllUnitsFromCollection() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  private Collection<Unit> getUnitsOfOtherPlayerOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitOtherPlayer1);
    units.add(unitOtherPlayer2);
    units.add(unitOtherPlayer3);
    return units;
  }

  @Test
  public void addAllUnitsFromUnitCollection() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    final UnitCollection unitCollectionTwo = new UnitCollection(otherPlayerID, gameData);
    unitCollectionTwo.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    unitCollection.addAllUnits(unitCollectionTwo);

    assertThat(unitCollection.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void removeAllUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    unitCollection.removeAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void getUnitCount() {
    assertThat(unitCollection.getUnitCount(), is(equalTo(0)));
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.getUnitCount(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection();
    final int expUnitCount = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
    assertThat(unitCollection.getUnitCount(), is(equalTo(expUnitCount)));
  }

  private void addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection() {
    unitCollection.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
  }

  private void addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection() {
    unitCollection.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
  }

  @Test
  public void getUnitCountByUnitType() {
    assertThat(unitCollection.getUnitCount(unitTypeOne), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo), is(equalTo(0)));

    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));

    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(unitTypeOne), is(equalTo(unitCountUnitTypeOne)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo), is(equalTo(unitCountUnitTypeTwo)));
  }

  private void addAllOtherPlayerUnitsToUnitCollection() {
    unitCollection.addAllUnits(getUnitsOfOtherPlayerOfUnitTypeOne());
    unitCollection.addAllUnits(getUnitsOfOtherPlayerOfUnitTypeTwo());
  }

  private Collection<Unit> getUnitsOfOtherPlayerOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitOtherPlayer4);
    units.add(unitOtherPlayer5);
    units.add(unitOtherPlayer6);
    units.add(unitOtherPlayer7);
    return units;
  }

  private void addAllPlayerUnitsToUnitCollection() {
    addAllDefaultPlayerUnitsToUnitCollection();
    addAllOtherPlayerUnitsToUnitCollection();
  }

  private void addAllDefaultPlayerUnitsToUnitCollection() {
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection();
  }

  @Test
  public void getUnitCountByUnitTypeAndPlayerID() {
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(unitTypeOne, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, defaultPlayerID),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(unitCollection.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(unitCountOtherPlayerUnitTypeOne)));
    assertThat(unitCollection.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(unitCountOtherPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitCountByPlayerID() {
    assertThat(unitCollection.getUnitCount(defaultPlayerID), is(equalTo(0)));
    assertThat(unitCollection.getUnitCount(otherPlayerID), is(equalTo(0)));

    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(unitCollection.getUnitCount(otherPlayerID), is(equalTo(0)));

    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(unitCollection.getUnitCount(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void containsAll() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeOne = getUnitsOfDefaultPlayerOfUnitTypeOne();
    assertThat(unitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(false)));
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getUnitsByUnitTypeAndMaxValue() {
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getUnits(unitTypeTwo, Integer.MAX_VALUE).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(unitCollection.getUnits(unitTypeTwo, unitCountDefaultPlayerUnitTypeTwo).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByType() {
    addAllDefaultPlayerUnitsToUnitCollection();
    final IntegerMap<UnitType> unitsByType = unitCollection.getUnitsByType();
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByTypeWithPlayerID() {
    addAllDefaultPlayerUnitsToUnitCollection();
    addAllOtherPlayerUnitsToUnitCollection();
    final IntegerMap<UnitType> unitsByType = unitCollection.getUnitsByType(defaultPlayerID);
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsWithUnityByTypeIntegerMap() {
    addAllDefaultPlayerUnitsToUnitCollection();
    final IntegerMap<UnitType> unitsByType = unitCollection.getUnitsByType();
    final Collection<Unit> expAllUnitsOfDefaultPlayer = unitCollection.getUnits(unitsByType);
    assertThat(expAllUnitsOfDefaultPlayer.size(), is(equalTo(unitCountDefaultPlayer)));
  }

  @Test
  public void size() {
    assertThat(unitCollection.size(), is(equalTo(0)));
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.size(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void isEmpty() {
    assertThat(unitCollection.isEmpty(), is(equalTo(true)));
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.isEmpty(), is(equalTo(false)));
  }

  @Test
  public void getUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    unitCollection.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    assertThat(unitCollection.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getPlayersWithUnits() {
    assertThat(unitCollection.getPlayersWithUnits().size(), is(equalTo(0)));
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getPlayersWithUnits().size(), is(equalTo(1)));
    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getPlayersWithUnits().size(), is(equalTo(2)));
  }

  @Test
  public void getPlayerUnitCounts() {
    addAllPlayerUnitsToUnitCollection();
    final IntegerMap<PlayerID> playerUnitCounts = unitCollection.getPlayerUnitCounts();
    assertThat(playerUnitCounts.getInt(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(playerUnitCounts.getInt(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void hasUnitsFromMultiplePlayers() {
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(false)));
    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.hasUnitsFromMultiplePlayers(), is(equalTo(true)));
  }

  @Test
  public void getHolder() {
    assertThat(unitCollection.getHolder(), is(equalTo(defaultPlayerID)));
  }

  @Test
  public void allMatch() {
    assertThat(unitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(false)));
  }

  @Test
  public void someMatch() {
    assertThat(unitCollection.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(false)));
    addAllUnitTypeOneUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    assertThat(unitCollection.someMatch(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(false)));
    addAllUnitTypeTwoUnitsOfDefaultPlayerToUnitCollection();
    assertThat(unitCollection.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    assertThat(unitCollection.someMatch(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(true)));
  }

  @Test
  public void countMatches() {
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(0)));
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeOne)),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitCollection.countMatches(Matches.unitIsOfType(unitTypeTwo)),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getMatches() {
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(0)));
    addAllDefaultPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    addAllOtherPlayerUnitsToUnitCollection();
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeOne)).size(), is(equalTo(unitCountUnitTypeOne)));
    assertThat(unitCollection.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(), is(equalTo(unitCountUnitTypeTwo)));
  }

  @Test
  public void iterator() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeTwo = getUnitsOfDefaultPlayerOfUnitTypeTwo();
    unitCollection.addAllUnits(unitsOfDefaultPlayerOfUnitTypeTwo);
    final Iterator<Unit> collectionIterator = unitsOfDefaultPlayerOfUnitTypeTwo.iterator();
    final Iterator<Unit> unitCollectionIterator = unitCollection.iterator();
    unitCollectionIterator.forEachRemaining(u -> assertThat(u, is(collectionIterator.next())));
  }

  private class MyPlayerID extends PlayerID {
    private static final long serialVersionUID = 7248994336755125604L;

    MyPlayerID() {
      super("Other Player", true, false, null);
    }

    int notifyChangedCounter = 0;

    public final int getNotifyChangedCounter() {
      return notifyChangedCounter;
    }

    public final void incrementNotifyChangedCounter() {
      ++this.notifyChangedCounter;
    }

    @Override
    public void notifyChanged() {
      incrementNotifyChangedCounter();
    }

    @Override
    public boolean isNull() {
      return true;
    }
  };

}
