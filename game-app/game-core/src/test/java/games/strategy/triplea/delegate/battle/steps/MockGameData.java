package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.Constants.ALLIED_AIR_INDEPENDENT;
import static games.strategy.triplea.Constants.ATTACKER_RETREAT_PLANES;
import static games.strategy.triplea.Constants.CAPTURE_UNITS_ON_ENTERING_TERRITORY;
import static games.strategy.triplea.Constants.CHOOSE_AA;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE;
import static games.strategy.triplea.Constants.EDIT_MODE;
import static games.strategy.triplea.Constants.LHTR_HEAVY_BOMBERS;
import static games.strategy.triplea.Constants.LOW_LUCK;
import static games.strategy.triplea.Constants.NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE;
import static games.strategy.triplea.Constants.PARTIAL_AMPHIBIOUS_RETREAT;
import static games.strategy.triplea.Constants.SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.WW2V2;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.delegate.TechTracker;
import java.util.List;
import java.util.Set;

public class MockGameData {
  private final GameData gameData = mock(GameData.class);
  private final GameProperties gameProperties = mock(GameProperties.class);
  private final RelationshipTracker relationshipTracker = mock(RelationshipTracker.class);
  private final GameMap gameMap = mock(GameMap.class);
  private final UnitTypeList unitTypeList = mock(UnitTypeList.class);
  private final GameSequence gameSequence = mock(GameSequence.class);
  private final TechTracker techTracker = mock(TechTracker.class);
  private final ResourceList resourceList = mock(ResourceList.class);
  private final PlayerList playerList = mock(PlayerList.class);
  private final TechnologyFrontier technologyFrontier = mock(TechnologyFrontier.class);

  private MockGameData() {
    lenient().when(gameData.getProperties()).thenReturn(gameProperties);
    lenient().when(gameData.getRelationshipTracker()).thenReturn(relationshipTracker);
    lenient().when(gameData.getMap()).thenReturn(gameMap);
    lenient().when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
    lenient().when(gameData.getSequence()).thenReturn(gameSequence);
    lenient().when(gameData.getTechTracker()).thenReturn(techTracker);
    lenient().when(gameData.getResourceList()).thenReturn(resourceList);
    lenient().when(gameData.getPlayerList()).thenReturn(playerList);
    lenient().when(gameData.getTechnologyFrontier()).thenReturn(technologyFrontier);
    lenient().when(playerList.getNullPlayer()).thenCallRealMethod();
  }

  public static MockGameData givenGameData() {
    return new MockGameData();
  }

  public GameData build() {
    return gameData;
  }

  public MockGameData withDiceSides(final int diceSides) {
    when(gameData.getDiceSides()).thenReturn(diceSides);
    return this;
  }

  public MockGameData withRound(final int round, final GamePlayer player) {
    when(gameSequence.getRound()).thenReturn(round);
    final GameStep gameStep = mock(GameStep.class);
    when(gameStep.getPlayerId()).thenReturn(player);
    when(gameSequence.getStep()).thenReturn(gameStep);
    return this;
  }

  public MockGameData withAlliedRelationship(
      final GamePlayer player1, final GamePlayer player2, final boolean value) {
    lenient().when(player1.getData()).thenReturn(gameData);
    lenient().when(player2.getData()).thenReturn(gameData);
    when(relationshipTracker.isAllied(player1, player2)).thenReturn(value);
    return this;
  }

  public MockGameData withWarRelationship(
      final GamePlayer player1, final GamePlayer player2, final boolean value) {
    lenient().when(player1.getData()).thenReturn(gameData);
    lenient().when(player2.getData()).thenReturn(gameData);
    when(relationshipTracker.isAtWar(player1, player2)).thenReturn(value);
    return this;
  }

  public MockGameData withTechnologyFrontier() {
    when(gameData.getTechnologyFrontier()).thenReturn(mock(TechnologyFrontier.class));
    return this;
  }

  public MockGameData withLenientProperties() {
    lenient().when(gameProperties.get(anyString(), anyBoolean())).thenReturn(false);
    return this;
  }

  public MockGameData withTransportCasualtiesRestricted(final boolean value) {
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(value);
    return this;
  }

  public MockGameData withSubmersibleSubs(final boolean value) {
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(value);
    return this;
  }

  public MockGameData withSubmarinesDefendingMaySubmergeOrRetreat(final boolean value) {
    when(gameProperties.get(SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT, false)).thenReturn(value);
    return this;
  }

  public MockGameData withSubRetreatBeforeBattle(final boolean value) {
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(value);
    return this;
  }

  public MockGameData withWW2V2(final boolean value) {
    lenient().when(gameProperties.get(WW2V2, false)).thenReturn(value);
    return this;
  }

  public MockGameData withDefendingSubsSneakAttack(final boolean value) {
    lenient().when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(value);
    return this;
  }

  public MockGameData withDefendingSuicideAndMunitionUnitsDoNotFire(final boolean value) {
    lenient()
        .when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(value);
    return this;
  }

  public MockGameData withPartialAmphibiousRetreat(final boolean value) {
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(value);
    return this;
  }

  public MockGameData withAttackerRetreatPlanes(final boolean value) {
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(value);
    return this;
  }

  public MockGameData withLhtrHeavyBombers(final boolean value) {
    when(gameProperties.get(LHTR_HEAVY_BOMBERS, false)).thenReturn(value);
    return this;
  }

  public MockGameData withTerritoryHasNeighbors(
      final Territory territory, final Set<Territory> neighbors) {
    when(gameMap.getNeighbors(territory)).thenReturn(neighbors);
    return this;
  }

  public MockGameData withAlliedAirIndependent(final boolean value) {
    when(gameProperties.get(ALLIED_AIR_INDEPENDENT, false)).thenReturn(value);
    return this;
  }

  public MockGameData withNavalBombardCasualtiesReturnFire(final boolean value) {
    when(gameProperties.get(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, false)).thenReturn(value);
    return this;
  }

  public MockGameData withCaptureUnitsOnEnteringTerritory(final boolean value) {
    when(gameProperties.get(CAPTURE_UNITS_ON_ENTERING_TERRITORY, false)).thenReturn(value);
    return this;
  }

  public MockGameData withEditMode(final boolean value) {
    when(gameProperties.get(EDIT_MODE)).thenReturn(value);
    return this;
  }

  public MockGameData withChooseAaCasualties(final boolean value) {
    when(gameProperties.get(CHOOSE_AA, false)).thenReturn(value);
    return this;
  }

  public MockGameData withLowLuck(final boolean value) {
    when(gameProperties.get(LOW_LUCK, false)).thenReturn(value);
    return this;
  }

  public MockGameData withUnitTypeList(final List<UnitType> types) {
    UnitTypeList unitTypeList = new UnitTypeList(gameData);
    for (var unitType : types) {
      lenient().when(unitType.getData()).thenReturn(gameData);
      unitTypeList.addUnitType(unitType);
    }
    lenient().when(gameData.getUnitTypeList()).thenReturn(unitTypeList);
    return this;
  }
}
