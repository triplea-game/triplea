package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.AaPowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AaCasualtySelectorTest {

  @Mock GamePlayer hitPlayer;
  @Mock GamePlayer aaPlayer;
  private UnitType aaUnitType;
  private UnitType damageableAaUnitType;
  private UnitType planeUnitType;
  private UnitType planeMultiHpUnitType;
  private GameData gameData;

  private DiceRoll givenDiceRollWithHitSequence(final boolean... shots) {
    final int[] diceRolls = new int[shots.length];
    int hits = 0;
    for (int i = 0; i < shots.length; i++) {
      diceRolls[i] = shots[i] ? 0 : 2;
      hits += (shots[i] ? 1 : 0);
    }

    return new DiceRoll(diceRolls, hits, 1, false, "");
  }

  private CombatValue givenAaCombatValue() {
    return CombatValueBuilder.aaCombatValue()
        .friendlyUnits(List.of())
        .enemyUnits(List.of())
        .side(BattleState.Side.DEFENSE)
        .supportAttachments(List.of())
        .build();
  }

  @Test
  void noHitsReturnEmptyCasualties() {
    gameData = givenGameData().build();
    final IDelegateBridge bridge = mock(IDelegateBridge.class);
    when(bridge.getData()).thenReturn(gameData);

    aaUnitType = new UnitType("aaUnitType", gameData);
    final UnitAttachment aaUnitAttachment =
        new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
    aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

    final CasualtyDetails details =
        AaCasualtySelector.getAaCasualties(
            List.of(mock(Unit.class)),
            aaUnitType.createTemp(1, aaPlayer),
            mock(CombatValue.class),
            mock(CombatValue.class),
            "text",
            givenDiceRollWithHitSequence(),
            bridge,
            hitPlayer,
            UUID.randomUUID(),
            mock(Territory.class));

    assertThat("No hits so no kills or damaged", details.size(), is(0));
  }

  @Nested
  class RandomCasualties {
    private IDelegateBridge bridge;

    @BeforeEach
    void initializeGameData() {
      gameData = givenGameData().withDiceSides(6).build();
      bridge = mock(IDelegateBridge.class);
      when(bridge.getData()).thenReturn(gameData);

      aaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment aaUnitAttachment =
          new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
      aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

      damageableAaUnitType = new UnitType("damageableAaUnitType", gameData);
      final UnitAttachment damageableAaUnitAttachment =
          new UnitAttachment("damageableAaUnitAttachment", damageableAaUnitType, gameData);
      damageableAaUnitAttachment.setDamageableAa(true);
      damageableAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, damageableAaUnitAttachment);

      planeUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeUnitAttachment =
          new UnitAttachment("planeUnitAttachment", aaUnitType, gameData);
      planeUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeUnitAttachment);

      planeMultiHpUnitType = new UnitType("planeMultiHpUnitType", gameData);
      final UnitAttachment planeMultiHpUnitAttachment =
          new UnitAttachment("planeMultiHpUnitAttachment", aaUnitType, gameData);
      planeMultiHpUnitAttachment.setHitPoints(2);
      planeMultiHpUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeMultiHpUnitAttachment);
    }

    @Test
    void hitsEqualToPlanesKillsAll() {
      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeUnitType.createTemp(1, hitPlayer),
              aaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
    }

    @Test
    void hitsMoreThanPlanesKillsAll() {
      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeUnitType.createTemp(1, hitPlayer),
              aaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("The plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat("Plane only had 1 hit point so no damages", details.getDamaged(), is(empty()));
    }

    @Test
    void oneHitAgainstTwoPlanesOnlyKillsOne() {
      final UnitType aaNonInfiniteUnitType = new UnitType("aaNonInfiniteUnitType", gameData);
      final UnitAttachment aaNonInfiniteUnitAttachment =
          new UnitAttachment("aaNonInfiniteUnitAttachment", aaNonInfiniteUnitType, gameData);
      aaNonInfiniteUnitAttachment.setMaxAaAttacks(1);
      aaNonInfiniteUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaNonInfiniteUnitAttachment);

      whenGetRandom(bridge).thenAnswer(withValues(0));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeUnitType.createTemp(2, hitPlayer),
              aaNonInfiniteUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One of the two planes are killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
      verify(bridge, description("2 planes with only 1 hit"))
          .getRandom(eq(2), eq(1), any(), any(), anyString());
    }

    @Test
    void identicalDieRollsShouldStillKillPlanesEqualToHits() {
      final UnitType aaNonInfiniteUnitType = new UnitType("aaNonInfiniteUnitType", gameData);
      final UnitAttachment aaNonInfiniteUnitAttachment =
          new UnitAttachment("aaNonInfiniteUnitAttachment", aaNonInfiniteUnitType, gameData);
      aaNonInfiniteUnitAttachment.setMaxAaAttacks(1);
      aaNonInfiniteUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaNonInfiniteUnitAttachment);

      whenGetRandom(bridge).thenAnswer(withValues(9, 9, 9, 9, 9));

      final List<Unit> planes = planeUnitType.createTemp(10, hitPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaNonInfiniteUnitType.createTemp(5, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "5 planes are killed even though the dice were all 9s", details.getKilled(), hasSize(5));
      assertThat(details.getDamaged(), is(empty()));
      verify(bridge, description("10 planes with only 5 hits"))
          .getRandom(eq(10), eq(5), any(), any(), anyString());
    }

    @Test
    void hitsEqualToPlanesMultiHpDamagesAndKillsAll() {
      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeMultiHpUnitType.createTemp(1, hitPlayer),
              damageableAaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "The damage is equal to the plane hp so it is killed", details.getKilled(), hasSize(1));
      assertThat("The damage to the plane should be tracked", details.getDamaged(), hasSize(1));
    }

    @Test
    void hitsGreaterThanPlanesMultiHpDamagesAndKillsAll() {
      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeMultiHpUnitType.createTemp(1, hitPlayer),
              damageableAaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("More than enough damage to kill the plane", details.getKilled(), hasSize(1));
      assertThat("The damage to the plane should be tracked", details.getDamaged(), hasSize(1));
    }

    @Test
    void oneHitAgainstMultiHpPlaneOnlyDamagesIt() {
      whenGetRandom(bridge).thenAnswer(withValues(0));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeMultiHpUnitType.createTemp(1, hitPlayer),
              damageableAaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("Plane has extra hp so it isn't killed", details.getKilled(), is(empty()));
      assertThat("Plane is damaged", details.getDamaged(), hasSize(1));
    }

    @Test
    void threeHitsAgainstTwoMultiHpPlanesKillsOneAndDamagesTheOther() {
      whenGetRandom(bridge).thenAnswer(withValues(0, 1, 2));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeMultiHpUnitType.createTemp(2, hitPlayer),
              damageableAaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "3 hits against 2 planes with 2 hp each is guaranteed to kill one of them",
          details.getKilled(),
          hasSize(1));
      assertThat(
          "3 hits against 2 planes with 2 hp each will damage both of them.",
          details.getDamaged(),
          hasSize(2));
    }

    @Test
    void identicalDieRollsShouldStillKillAndDamagePlanesEqualToHits() {
      whenGetRandom(bridge).thenAnswer(withValues(6, 6, 6, 6, 6));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeMultiHpUnitType.createTemp(7, hitPlayer),
              damageableAaUnitType.createTemp(3, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("5 planes should be killed or damaged", details.size(), is(5));
    }
  }

  @Nested
  class IndividualRollCasualties {
    private IDelegateBridge bridge;

    @BeforeEach
    void initializeGameData() {
      gameData = givenGameData().withDiceSides(6).build();
      bridge = mock(IDelegateBridge.class);
      when(bridge.getData()).thenReturn(gameData);

      aaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment aaUnitAttachment =
          new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
      aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

      damageableAaUnitType = new UnitType("damageableAaUnitType", gameData);
      final UnitAttachment damageableAaUnitAttachment =
          new UnitAttachment("damageableAaUnitAttachment", damageableAaUnitType, gameData);
      damageableAaUnitAttachment.setDamageableAa(true);
      damageableAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, damageableAaUnitAttachment);

      planeUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeUnitAttachment =
          new UnitAttachment("planeUnitAttachment", aaUnitType, gameData);
      planeUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeUnitAttachment);

      planeMultiHpUnitType = new UnitType("planeMultiHpUnitType", gameData);
      final UnitAttachment planeMultiHpUnitAttachment =
          new UnitAttachment("planeMultiHpUnitAttachment", aaUnitType, gameData);
      planeMultiHpUnitAttachment.setHitPoints(2);
      planeMultiHpUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeMultiHpUnitAttachment);
    }

    @Test
    void hitsEqualToPlanesKillsAll() {
      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planeUnitType.createTemp(1, hitPlayer),
              aaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
    }

    @Test
    void hitsLessThanPlanesKillsAccordingToTheRolledDice() {
      final List<Unit> planes = planeUnitType.createTemp(5, hitPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnitType.createTemp(1, aaPlayer),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRollWithHitSequence(true, false, true, false, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "1st, 3rd, and 5th plane were killed",
          details.getKilled(),
          is(List.of(planes.get(0), planes.get(2), planes.get(4))));
    }
  }

  @Nested
  class LowLuckCasualties {
    private IDelegateBridge bridge;
    @Mock private UnitType otherPlaneUnitType;
    @Mock private UnitType otherPlaneMultiHpUnitType;
    @Mock private UnitType powerfulAaUnitType;

    @BeforeEach
    void setupPlayers() {
      when(hitPlayer.getName()).thenReturn("Hit Player");
    }

    @BeforeEach
    void initializeGameData() {
      gameData =
          givenGameData().withDiceSides(6).withChooseAaCasualties(false).withLowLuck(true).build();
      bridge = mock(IDelegateBridge.class);
      when(bridge.getData()).thenReturn(gameData);

      aaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment aaUnitAttachment =
          new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
      aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

      damageableAaUnitType = new UnitType("damageableAaUnitType", gameData);
      final UnitAttachment damageableAaUnitAttachment =
          new UnitAttachment("damageableAaUnitAttachment", damageableAaUnitType, gameData);
      damageableAaUnitAttachment.setDamageableAa(true);
      damageableAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, damageableAaUnitAttachment);

      powerfulAaUnitType = new UnitType("powerfulAaUnitType", gameData);
      final UnitAttachment powerfulAaUnitAttachment =
          new UnitAttachment("powerfulAaUnitAttachment", powerfulAaUnitType, gameData);
      powerfulAaUnitAttachment.setAttackAa(5);
      powerfulAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, powerfulAaUnitAttachment);

      planeUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeUnitAttachment =
          new UnitAttachment("planeUnitAttachment", aaUnitType, gameData);
      planeUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeUnitAttachment);

      otherPlaneUnitType = new UnitType("otherPlaneUnitType", gameData);
      final UnitAttachment otherPlaneUnitAttachment =
          new UnitAttachment("otherPlaneUnitAttachment", aaUnitType, gameData);
      otherPlaneUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherPlaneUnitAttachment);

      planeMultiHpUnitType = new UnitType("planeMultiHpUnitType", gameData);
      final UnitAttachment planeMultiHpUnitAttachment =
          new UnitAttachment("planeMultiHpUnitAttachment", aaUnitType, gameData);
      planeMultiHpUnitAttachment.setHitPoints(2);
      planeMultiHpUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeMultiHpUnitAttachment);

      otherPlaneMultiHpUnitType = new UnitType("otherPlaneMultiHpUnitType", gameData);
      final UnitAttachment otherPlaneMultiHpUnitAttachment =
          new UnitAttachment("otherPlaneMultiHpUnitAttachment", aaUnitType, gameData);
      otherPlaneMultiHpUnitAttachment.setHitPoints(2);
      otherPlaneMultiHpUnitType.addAttachment(
          UNIT_ATTACHMENT_NAME, otherPlaneMultiHpUnitAttachment);
    }

    @Test
    void oneTypeOfPlaneWithAmountEqualToDiceSides() {
      final List<Unit> planes = planeUnitType.createTemp(6, hitPlayer);
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    private DiceRoll givenLowLuckDiceRoll(final List<Unit> aaUnits, final List<Unit> planes) {
      final AaPowerStrengthAndRolls strengthAndRolls =
          AaPowerStrengthAndRolls.build(
              aaUnits,
              planes.size(),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.DEFENSE)
                  .supportAttachments(List.of())
                  .build());

      return new DiceRoll(
          new int[] {5},
          strengthAndRolls.calculateTotalPower() / strengthAndRolls.getDiceSides(),
          1,
          false,
          "");
    }

    private DiceRoll givenLowLuckDiceRollWithExtraHit(
        final List<Unit> aaUnits, final List<Unit> planes) {
      final AaPowerStrengthAndRolls strengthAndRolls =
          AaPowerStrengthAndRolls.build(
              aaUnits,
              planes.size(),
              CombatValueBuilder.aaCombatValue()
                  .enemyUnits(List.of())
                  .friendlyUnits(List.of())
                  .side(BattleState.Side.DEFENSE)
                  .supportAttachments(List.of())
                  .build());

      return new DiceRoll(
          new int[] {0},
          strengthAndRolls.calculateTotalPower() / strengthAndRolls.getDiceSides() + 1,
          1,
          false,
          "");
    }

    @Test
    void twoTypesOfPlanesAndBothHaveAmountEqualToDiceSides() {
      final List<Unit> planes = planeUnitType.createTemp(6, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(6, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "One of each plane type should be killed",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, otherPlaneUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndTogetherHaveAmountEqualToDiceSides() {
      // need to randomly pick a plane to kill
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeUnitType.createTemp(3, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(3, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The 2nd plane was randomly selected and it is a planeUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void oneTypeOfMultiHpPlaneWithAmountEqualToDiceSides() {
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeMultiHpUnitType.createTemp(6, hitPlayer);
      final List<Unit> aaUnits = damageableAaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "Only one hit and the plane has 2 hp so it can withstand it",
          details.getKilled(),
          is(empty()));
      assertThat("The planes have 2 hp so the hit damages it", details.getDamaged(), hasSize(1));
    }

    @Test
    void twoTypesOfMultiHpPlanesAndBothHaveAmountEqualToDiceSides() {
      whenGetRandom(bridge).thenAnswer(withValues(1, 1));

      final List<Unit> planes = planeMultiHpUnitType.createTemp(6, hitPlayer);
      planes.addAll(otherPlaneMultiHpUnitType.createTemp(6, hitPlayer));
      final List<Unit> aaUnits = damageableAaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "Only two hits and the planes have 2 hp so they can withstand it",
          details.getKilled(),
          is(empty()));

      assertThat(details.getDamaged(), hasSize(2));
      assertThat(
          "One of each plane type should be damaged",
          details.getDamaged().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeMultiHpUnitType, otherPlaneMultiHpUnitType));
    }

    @Test
    void oneTypeOfPlaneWithAmountLessThanDiceSidesButItHit() {
      // need to randomly pick a plane to kill
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeUnitType.createTemp(3, hitPlayer);
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndTogetherHaveAmountLessThanDiceSidesButItHit() {
      // need to randomly pick a plane to kill
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeUnitType.createTemp(2, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(2, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The 2nd plane was randomly selected and it is a planeUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void oneTypeOfPlaneWithRemainderOverDiceSidesButNotExtraHit() {
      final List<Unit> planes = planeUnitType.createTemp(8, hitPlayer);
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndBothHaveRemainderOverDiceSidesButNotExtraHit() {
      final List<Unit> planes = planeUnitType.createTemp(8, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(8, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "Both types of planes are killed because the first plane from each group is added "
              + "to the selection and then one extra plane is added from the remainder. Then the "
              + "'random' selects the 1st and 2nd plane which are the first from both groups.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, otherPlaneUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndTogetherHaveRemainderOverDiceSidesButNotExtraHit() {
      // need to randomly pick 1 planes out of the remainder to kill
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeUnitType.createTemp(4, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(4, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRoll(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(1));
      assertThat(
          "The 2nd plane was randomly selected and it is planeUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void oneTypeOfPlaneWithRemainderOverDiceSidesAndWithExtraHit() {
      // need to randomly pick a plane to kill
      whenGetRandom(bridge).thenAnswer(withValues(1));

      final List<Unit> planes = planeUnitType.createTemp(8, hitPlayer);
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void oneTypeOfPlaneWithRemainderOf1AndWithExtraHit() {
      final List<Unit> planes = planeUnitType.createTemp(7, hitPlayer);
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndBothHaveRemainderOverDiceSidesAndWithExtraHit() {
      // need to pick one plane out of the remainder list
      whenGetRandom(bridge).thenAnswer(withValues(0));

      final List<Unit> planes = planeUnitType.createTemp(8, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(8, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(3));
      assertThat(
          "The first of each unit type is selected and then the third hit is randomly "
              + "selected, which this test forces it to be the first one in the list and that is "
              + "the otherPlaneUnitType",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, otherPlaneUnitType, otherPlaneUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndOneHasRemainderOf1AndWithExtraHit() {
      final List<Unit> planes = planeUnitType.createTemp(6, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(7, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(3));
      assertThat(
          "The first of each unit type is selected and then the third hit is picked from "
              + "the remainder list which only contains 1 otherPlaneUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, otherPlaneUnitType, otherPlaneUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndTogetherHaveRemainderOverDiceSidesAndWithExtraHit() {
      // need to randomly pick 2 planes to kill
      whenGetRandom(bridge).thenAnswer(withValues(1, 1));

      final List<Unit> planes = planeUnitType.createTemp(4, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(4, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "The 2nd and 4th plane were randomly selected and they are both planeUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, planeUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }

    @Test
    void twoTypesOfPlanesAndTogetherHaveRemainderOf1AndWithExtraHit() {
      // need to randomly pick 2 planes to kill
      whenGetRandom(bridge).thenAnswer(withValues(1, 2));

      final List<Unit> planes = planeUnitType.createTemp(4, hitPlayer);
      planes.addAll(otherPlaneUnitType.createTemp(3, hitPlayer));
      final List<Unit> aaUnits = aaUnitType.createTemp(1, aaPlayer);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              aaUnits,
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenLowLuckDiceRollWithExtraHit(aaUnits, planes),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(details.getKilled(), hasSize(2));
      assertThat(
          "The 2nd and 4th plane were randomly selected and both are planeUnitType.",
          details.getKilled().stream().map(Unit::getType).collect(Collectors.toList()),
          containsInAnyOrder(planeUnitType, planeUnitType));

      assertThat(
          "The planes have 1 hp so there can't be damaged planes",
          details.getDamaged(),
          is(empty()));
    }
  }
}
