package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.ArrayList;
import java.util.List;

public class ProPlaceTerritory {
  private Territory territory;
  private List<Unit> defendingUnits;
  private ProBattleResult minBattleResult;
  private double defenseValue;
  private double strategicValue;
  private List<Unit> placeUnits;
  private boolean canHold;

  public ProPlaceTerritory(final Territory territory) {
    this.territory = territory;
    defendingUnits = new ArrayList<>();
    minBattleResult = new ProBattleResult();
    defenseValue = 0;
    strategicValue = 0;
    placeUnits = new ArrayList<>();
    canHold = true;
  }

  @Override
  public String toString() {
    return territory.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (o instanceof ProPlaceTerritory) {
      return ((ProPlaceTerritory) o).getTerritory().equals(territory);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return territory.hashCode();
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setTerritory(final Territory territory) {
    this.territory = territory;
  }

  public List<Unit> getDefendingUnits() {
    return defendingUnits;
  }

  public void setDefendingUnits(final List<Unit> defendingUnits) {
    this.defendingUnits = defendingUnits;
  }

  public double getDefenseValue() {
    return defenseValue;
  }

  public void setDefenseValue(final double defenseValue) {
    this.defenseValue = defenseValue;
  }

  public double getStrategicValue() {
    return strategicValue;
  }

  public void setStrategicValue(final double strategicValue) {
    this.strategicValue = strategicValue;
  }

  public List<Unit> getPlaceUnits() {
    return placeUnits;
  }

  public void setPlaceUnits(final List<Unit> placeUnits) {
    this.placeUnits = placeUnits;
  }

  public void setMinBattleResult(final ProBattleResult minBattleResult) {
    this.minBattleResult = minBattleResult;
  }

  public ProBattleResult getMinBattleResult() {
    return minBattleResult;
  }

  public void setCanHold(final boolean canHold) {
    this.canHold = canHold;
  }

  public boolean isCanHold() {
    return canHold;
  }
}
