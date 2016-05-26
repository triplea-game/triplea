package util.triplea.mapXmlCreator;

import java.util.Set;
import java.util.TreeSet;

import games.strategy.util.Tuple;

class CanalTerritoriesTuple {

  Tuple<Set<String>, Set<String>> canalTerritoriesTuple;

  public CanalTerritoriesTuple() {
    this(new TreeSet<>(), new TreeSet<>());
  }

  public CanalTerritoriesTuple(final Set<String> waterTerritories, final Set<String> landTerritories) {
    canalTerritoriesTuple = Tuple.of(waterTerritories, landTerritories);
  }

  public void clear() {
    canalTerritoriesTuple.getFirst().clear();
    canalTerritoriesTuple.getSecond().clear();
  }

  public void addLandTerritory(final String territory) {
    canalTerritoriesTuple.getSecond().add(territory);
  }

  public void addWaterTerritory(final String territory) {
    canalTerritoriesTuple.getFirst().add(territory);
  }

  public Set<String> getLandTerritories() {
    return canalTerritoriesTuple.getSecond();
  }

  public Set<String> getWaterTerritories() {
    return canalTerritoriesTuple.getFirst();
  }

}
