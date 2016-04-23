package games.strategy.engine.data;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;

public class UnitCollectionTest {

  final GameData gameData = new MyGameData();
  final UnitType unitTypeOne = new UnitType("Unit Type 1", gameData);
  final UnitType unitTypeTwo = new UnitType("Unit Type 2", gameData);
  final MyPlayerID defaultPlayerID = new MyPlayerID();
  final PlayerID otherPlayerID = PlayerID.NULL_PLAYERID;
  final UnitCollection uc = new UnitCollection(defaultPlayerID, gameData);

  final int unitCountDefaultPlayerUnitTypeOne = getUnitsOfDefaultPlayerOfUnitTypeOne().size();
  final int unitCountDefaultPlayerUnitTypeTwo = getUnitsOfDefaultPlayerOfUnitTypeTwo().size();
  final int unitCountDefaultPlayer = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
  final Unit unitDefaultPlayer1 = new Unit(unitTypeOne, defaultPlayerID, gameData);
  final Unit unitDefaultPlayer2 = new Unit(unitTypeTwo, defaultPlayerID, gameData);
  final Unit unitDefaultPlayer3 = new Unit(unitTypeTwo, defaultPlayerID, gameData);

  private Collection<Unit> getUnitsOfDefaultPlayerOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitDefaultPlayer1);
    return units;
  }

  private Collection<Unit> getUnitsOfDefaultPlayerOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitDefaultPlayer2);
    units.add(unitDefaultPlayer3);
    return units;
  }

  final int unitCountOtherPlayerUnitTypeOne = 3;
  final int unitCountOtherPlayerUnitTypeTwo = 4;
  final int unitCountOtherPlayer = unitCountOtherPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeTwo;
  final Unit unitOtherPlayer1 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer2 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer3 = new Unit(unitTypeOne, otherPlayerID, gameData);
  final Unit unitOtherPlayer4 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer5 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer6 = new Unit(unitTypeTwo, otherPlayerID, gameData);
  final Unit unitOtherPlayer7 = new Unit(unitTypeTwo, otherPlayerID, gameData);


  final int unitCountUnitTypeOne = unitCountDefaultPlayerUnitTypeOne + unitCountOtherPlayerUnitTypeOne;
  final int unitCountUnitTypeTwo = unitCountDefaultPlayerUnitTypeTwo + unitCountOtherPlayerUnitTypeTwo;

  private Collection<Unit> getUnitsOfOtherPlayerOfUnitTypeOne() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitOtherPlayer1);
    units.add(unitOtherPlayer2);
    units.add(unitOtherPlayer3);
    return units;
  }

  private Collection<Unit> getUnitsOfOtherPlayerOfUnitTypeTwo() {
    final Collection<Unit> units = new ArrayList<Unit>();
    units.add(unitOtherPlayer4);
    units.add(unitOtherPlayer5);
    units.add(unitOtherPlayer6);
    units.add(unitOtherPlayer7);
    return units;
  }

  private void addAllPlayerUnits() {
    addAllDefaultPlayerUnits();
    addAllOtherPlayerUnits();
  }

  private void addAllDefaultPlayerUnits() {
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
  }

  private void addAllOtherPlayerUnits() {
    uc.addAllUnits(getUnitsOfOtherPlayerOfUnitTypeOne());
    uc.addAllUnits(getUnitsOfOtherPlayerOfUnitTypeTwo());
  }

  @Test
  public void unitCollection() {
    assertThat(uc.getHolder(), is(equalTo(defaultPlayerID)));
    assertThat(uc.getData(), is(equalTo(gameData)));
  }

  @Test
  public void addUnit() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    uc.addUnit(unitDefaultPlayer1);

    assertThat(uc.getUnitCount(), is(equalTo(1)));
    assertThat(uc.getUnits().iterator().next(), is(equalTo(unitDefaultPlayer1)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void addAllUnitsFromCollection() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    uc.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(uc.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void addAllUnitsFromUnitCollection() {
    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    final UnitCollection unitCollectionTwo = new UnitCollection(otherPlayerID, gameData);
    unitCollectionTwo.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    uc.addAllUnits(unitCollectionTwo);

    assertThat(uc.getUnitCount(), is(equalTo(unitsOfOtherPlayerOfUnitTypeOne.size())));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void removeAllUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    uc.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    final int notifyChangedCountBefore = defaultPlayerID.getNotifyChangedCounter();
    uc.removeAllUnits(unitsOfOtherPlayerOfUnitTypeOne);

    assertThat(uc.getUnitCount(), is(equalTo(0)));
    final int expNotifyChangedCountAfter = notifyChangedCountBefore + 1;
    assertThat(defaultPlayerID.getNotifyChangedCounter(), is(equalTo(expNotifyChangedCountAfter)));
  }

  @Test
  public void getUnitCount() {
    assertThat(uc.getUnitCount(), is(equalTo(0)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    assertThat(uc.getUnitCount(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
    final int expUnitCount = unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo;
    assertThat(uc.getUnitCount(), is(equalTo(expUnitCount)));
  }

  @Test
  public void getUnitCountByUnitType() {
    assertThat(uc.getUnitCount(unitTypeOne), is(equalTo(0)));
    assertThat(uc.getUnitCount(unitTypeTwo), is(equalTo(0)));

    addAllDefaultPlayerUnits();
    assertThat(uc.getUnitCount(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(uc.getUnitCount(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));

    addAllOtherPlayerUnits();
    assertThat(uc.getUnitCount(unitTypeOne), is(equalTo(unitCountUnitTypeOne)));
    assertThat(uc.getUnitCount(unitTypeTwo), is(equalTo(unitCountUnitTypeTwo)));
  }

  @Test
  public void getUnitCountByUnitTypeAndPlayerID() {
    assertThat(uc.getUnitCount(unitTypeOne, defaultPlayerID), is(equalTo(0)));
    assertThat(uc.getUnitCount(unitTypeTwo, defaultPlayerID), is(equalTo(0)));
    assertThat(uc.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(uc.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    addAllDefaultPlayerUnits();
    assertThat(uc.getUnitCount(unitTypeOne, defaultPlayerID), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(uc.getUnitCount(unitTypeTwo, defaultPlayerID), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(uc.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(0)));
    assertThat(uc.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(0)));

    addAllOtherPlayerUnits();
    assertThat(uc.getUnitCount(unitTypeOne, defaultPlayerID), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(uc.getUnitCount(unitTypeTwo, defaultPlayerID), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(uc.getUnitCount(unitTypeOne, otherPlayerID), is(equalTo(unitCountOtherPlayerUnitTypeOne)));
    assertThat(uc.getUnitCount(unitTypeTwo, otherPlayerID), is(equalTo(unitCountOtherPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitCountByPlayerID() {
    assertThat(uc.getUnitCount(defaultPlayerID), is(equalTo(0)));
    assertThat(uc.getUnitCount(otherPlayerID), is(equalTo(0)));

    addAllDefaultPlayerUnits();
    assertThat(uc.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(uc.getUnitCount(otherPlayerID), is(equalTo(0)));

    addAllOtherPlayerUnits();
    assertThat(uc.getUnitCount(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(uc.getUnitCount(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void containsAll() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeOne = getUnitsOfDefaultPlayerOfUnitTypeOne();
    assertThat(uc.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(false)));
    addAllDefaultPlayerUnits();
    assertThat(uc.containsAll(unitsOfDefaultPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getUnitsByUnitTypeAndMaxValue() {
    addAllDefaultPlayerUnits();
    assertThat(uc.getUnits(unitTypeTwo, Integer.MAX_VALUE).size(), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    assertThat(uc.getUnits(unitTypeTwo, unitCountDefaultPlayerUnitTypeTwo).size(),
        is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByType() {
    addAllDefaultPlayerUnits();
    final IntegerMap<UnitType> unitsByType = uc.getUnitsByType();
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsByTypeWithPlayerID() {
    addAllDefaultPlayerUnits();
    addAllOtherPlayerUnits();
    final IntegerMap<UnitType> unitsByType = uc.getUnitsByType(defaultPlayerID);
    assertThat(unitsByType.getInt(unitTypeOne), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(unitsByType.getInt(unitTypeTwo), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getUnitsWithUnityByTypeIntegerMap() {
    addAllDefaultPlayerUnits();
    final IntegerMap<UnitType> unitsByType = uc.getUnitsByType();
    final Collection<Unit> expAllUnitsOfDefaultPlayer = uc.getUnits(unitsByType);
    assertThat(expAllUnitsOfDefaultPlayer.size(), is(equalTo(unitCountDefaultPlayer)));
  }

  @Test
  public void size() {
    assertThat(uc.size(), is(equalTo(0)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    assertThat(uc.size(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
    assertThat(uc.size(), is(equalTo(unitCountDefaultPlayerUnitTypeOne + unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void isEmpty() {
    assertThat(uc.isEmpty(), is(equalTo(true)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    assertThat(uc.isEmpty(), is(equalTo(false)));
  }

  @Test
  public void getUnits() {
    final Collection<Unit> unitsOfOtherPlayerOfUnitTypeOne = getUnitsOfOtherPlayerOfUnitTypeOne();
    uc.addAllUnits(unitsOfOtherPlayerOfUnitTypeOne);
    assertThat(uc.getUnits().containsAll(unitsOfOtherPlayerOfUnitTypeOne), is(equalTo(true)));
  }

  @Test
  public void getPlayersWithUnits() {
    assertThat(uc.getPlayersWithUnits().size(), is(equalTo(0)));
    addAllDefaultPlayerUnits();
    assertThat(uc.getPlayersWithUnits().size(), is(equalTo(1)));
    addAllOtherPlayerUnits();
    assertThat(uc.getPlayersWithUnits().size(), is(equalTo(2)));
  }

  @Test
  public void getPlayerUnitCounts() {
    addAllPlayerUnits();
    final IntegerMap<PlayerID> playerUnitCounts = uc.getPlayerUnitCounts();
    assertThat(playerUnitCounts.getInt(defaultPlayerID), is(equalTo(unitCountDefaultPlayer)));
    assertThat(playerUnitCounts.getInt(otherPlayerID), is(equalTo(unitCountOtherPlayer)));
  }

  @Test
  public void hasUnitsFromMultiplePlayers() {
    addAllDefaultPlayerUnits();
    assertThat(uc.hasUnitsFromMultiplePlayers(), is(equalTo(false)));
    addAllOtherPlayerUnits();
    assertThat(uc.hasUnitsFromMultiplePlayers(), is(equalTo(true)));
  }

  @Test
  public void getHolder() {
    assertThat(uc.getHolder(), is(equalTo(defaultPlayerID)));
  }

  @Test
  public void allMatch() {
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    assertThat(uc.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
    assertThat(uc.allMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(false)));
  }

  @Test
  public void someMatch() {
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeOne());
    assertThat(uc.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    assertThat(uc.someMatch(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(false)));
    uc.addAllUnits(getUnitsOfDefaultPlayerOfUnitTypeTwo());
    assertThat(uc.someMatch(Matches.unitIsOfType(unitTypeOne)), is(equalTo(true)));
    assertThat(uc.someMatch(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(true)));
  }

  @Test
  public void countMatches() {
    addAllDefaultPlayerUnits();
    assertThat(uc.countMatches(Matches.unitIsOfType(unitTypeOne)), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(uc.countMatches(Matches.unitIsOfType(unitTypeTwo)), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
  }

  @Test
  public void getMatches() {
    addAllDefaultPlayerUnits();
    assertThat(uc.getMatches(Matches.unitIsOfType(unitTypeOne)).size(), is(equalTo(unitCountDefaultPlayerUnitTypeOne)));
    assertThat(uc.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(), is(equalTo(unitCountDefaultPlayerUnitTypeTwo)));
    addAllOtherPlayerUnits();
    assertThat(uc.getMatches(Matches.unitIsOfType(unitTypeOne)).size(), is(equalTo(unitCountUnitTypeOne)));
    assertThat(uc.getMatches(Matches.unitIsOfType(unitTypeTwo)).size(), is(equalTo(unitCountUnitTypeTwo)));
  }

  @Test
  public void iterator() {
    final Collection<Unit> unitsOfDefaultPlayerOfUnitTypeTwo = getUnitsOfDefaultPlayerOfUnitTypeTwo();
    uc.addAllUnits(unitsOfDefaultPlayerOfUnitTypeTwo);
    final Iterator<Unit> ucIterator = uc.iterator();
    final Iterator<Unit> collectionIterator = unitsOfDefaultPlayerOfUnitTypeTwo.iterator();
    while (ucIterator.hasNext()) {
      final Unit ucIteratorUnit = ucIterator.next();
      final Unit collectionIteratorIteratorUnit = collectionIterator.next();
      assertThat(ucIteratorUnit, is(equalTo(collectionIteratorIteratorUnit)));
    }
  }

  class MyGameData extends GameData {
    private static final long serialVersionUID = -1705899383092114236L;

    @Override
    public UnitTypeList getUnitTypeList() {
      final UnitTypeList unitTypeList = super.getUnitTypeList();
      unitTypeList.addUnitType(unitTypeOne);
      unitTypeList.addUnitType(unitTypeTwo);
      return super.getUnitTypeList();
    }
  }

  class MyPlayerID extends PlayerID {
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
