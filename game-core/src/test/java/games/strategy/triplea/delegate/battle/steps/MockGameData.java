package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.Constants.ATTACKER_RETREAT_PLANES;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE;
import static games.strategy.triplea.Constants.PARTIAL_AMPHIBIOUS_RETREAT;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.WW2V2;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.properties.GameProperties;

public class MockGameData {
  private final GameData gameData = mock(GameData.class);
  private final GameProperties gameProperties = mock(GameProperties.class);
  private final RelationshipTracker relationshipTracker = mock(RelationshipTracker.class);

  private MockGameData() {
    lenient().when(gameData.getProperties()).thenReturn(gameProperties);
    lenient().when(gameData.getRelationshipTracker()).thenReturn(relationshipTracker);
  }

  public static MockGameData givenGameData() {
    return new MockGameData();
  }

  public GameData build() {
    return gameData;
  }

  public MockGameData withAlliedRelationship(
      final GamePlayer player1, final GamePlayer player2, final boolean value) {
    when(relationshipTracker.isAllied(player1, player2)).thenReturn(value);
    return this;
  }

  public MockGameData withWarRelationship(
      final GamePlayer player1, final GamePlayer player2, final boolean value) {
    when(relationshipTracker.isAtWar(player1, player2)).thenReturn(value);
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
}
