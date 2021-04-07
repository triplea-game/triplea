package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AA_GUNS_FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_WITHOUT_SPACE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.LAND_PARATROOPS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARD;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NOTIFY_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_BOMBARDMENT_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_SNEAK_ATTACK_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_UNESCORTED_TRANSPORTS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BattleStepsTest {

  @Mock BattleActions battleActions;

  @Mock Territory battleSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;
  @Mock TechAttachment techAttachment;

  @BeforeEach
  public void givenPlayers() {
    lenient().when(attacker.getName()).thenReturn("mockAttacker");
    lenient().when(defender.getName()).thenReturn("mockDefender");
  }

  public static Territory givenSeaBattleSite() {
    final Territory battleSite = mock(Territory.class);
    when(battleSite.isWater()).thenReturn(true);
    return battleSite;
  }

  @Value
  public static class UnitAndAttachment {
    private Unit unit;
    private UnitAttachment unitAttachment;
  }

  public static UnitAndAttachment newUnitAndAttachment() {
    final Unit unit = mock(Unit.class);
    final UnitType unitType = mock(UnitType.class);
    final UnitAttachment unitAttachment = mock(UnitAttachment.class);
    lenient().when(unit.getType()).thenReturn(unitType);
    lenient().when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
    lenient().when(unit.getUnitAttachment()).thenReturn(unitAttachment);
    return new UnitAndAttachment(unit, unitAttachment);
  }

  public static Unit givenAnyUnit() {
    return newUnitAndAttachment().unit;
  }

  public static Unit givenUnitCanEvade() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getCanEvade()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitFirstStrike() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    lenient().when(unitAndAttachment.unitAttachment.getIsFirstStrike()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitFirstStrikeAndEvade() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsFirstStrike()).thenReturn(true);
    when(unitAndAttachment.unitAttachment.getCanEvade()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(final UnitType otherType) {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsFirstStrike()).thenReturn(true);
    when(unitAndAttachment.unitAttachment.getCanEvade()).thenReturn(true);
    when(unitAndAttachment.unitAttachment.getCanNotBeTargetedBy()).thenReturn(Set.of(otherType));
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitCanEvadeAndCanNotBeTargetedByRandomUnit() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getCanEvade()).thenReturn(true);
    when(unitAndAttachment.unitAttachment.getCanNotBeTargetedBy())
        .thenReturn(Set.of(mock(UnitType.class)));
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitCanNotBeTargetedBy(final UnitType otherType) {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getCanNotBeTargetedBy()).thenReturn(Set.of(otherType));
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitDestroyer() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    lenient().when(unitAndAttachment.unitAttachment.getIsDestroyer()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitSeaTransport() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getTransportCapacity()).thenReturn(2);
    when(unitAndAttachment.unitAttachment.getIsSea()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitAirTransport() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsAirTransport()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitIsCombatAa() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unitAndAttachment.unitAttachment.getIsAaForCombatOnly()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitIsCombatAa(
      final Set<UnitType> aaTarget, final GamePlayer player, final BattleState.Side side) {
    return givenUnitIsCombatAa(aaTarget, player, side, "AntiAirGun");
  }

  public static Unit givenUnitIsCombatAa(
      final Set<UnitType> aaTarget,
      final GamePlayer player,
      final BattleState.Side side,
      final String aaType) {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unit.getData()).thenReturn(mock(GameData.class));
    when(unitAndAttachment.unitAttachment.getTypeAa()).thenReturn(aaType);
    when(unitAndAttachment.unitAttachment.getTargetsAa(any())).thenReturn(aaTarget);
    when(unitAndAttachment.unitAttachment.getIsAaForCombatOnly()).thenReturn(true);
    when(unitAndAttachment.unitAttachment.getMaxRoundsAa()).thenReturn(-1);
    when(unitAndAttachment.unitAttachment.getMaxAaAttacks()).thenReturn(1);
    if (side == BattleState.Side.OFFENSE) {
      when(unitAndAttachment.unitAttachment.getOffensiveAttackAa(player)).thenReturn(1);
    } else {
      when(unitAndAttachment.unitAttachment.getAttackAa(player)).thenReturn(1);
    }
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitIsAir() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsAir()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitIsSea() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsSea()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitWasAmphibious() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unit.getWasAmphibious()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  public static Unit givenUnitIsInfrastructure() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getIsInfrastructure()).thenReturn(true);
    return unitAndAttachment.unit;
  }

  @SafeVarargs
  public static List<String> mergeSteps(final List<String>... steps) {
    return Stream.of(steps).flatMap(Collection::stream).collect(Collectors.toList());
  }

  private List<String> basicFightStepStrings() {
    return mergeSteps(
        generalFightStepStrings(attacker, defender),
        generalFightStepStrings(defender, attacker),
        List.of(REMOVE_CASUALTIES));
  }

  public static List<String> generalFightStepStrings(
      final GamePlayer firingPlayer, final GamePlayer hitPlayer) {
    return List.of(
        firingPlayer.getName() + FIRE_SUFFIX,
        hitPlayer.getName() + SELECT_PREFIX + CASUALTIES_WITHOUT_SPACE_SUFFIX,
        hitPlayer.getName() + NOTIFY_PREFIX + CASUALTIES_WITHOUT_SPACE_SUFFIX);
  }

  public static List<String> generalFightStepStrings(
      final GamePlayer firingPlayer, final GamePlayer hitPlayer, final String groupName) {
    return List.of(
        firingPlayer.getName() + " " + groupName + FIRE_SUFFIX,
        hitPlayer.getName() + SELECT_PREFIX + groupName + CASUALTIES_SUFFIX,
        hitPlayer.getName() + NOTIFY_PREFIX + groupName + CASUALTIES_SUFFIX);
  }

  public static List<String> firstStrikeFightStepStrings(
      final GamePlayer firingPlayer, final GamePlayer hitPlayer) {
    return List.of(
        firingPlayer.getName() + " " + FIRST_STRIKE_UNITS + FIRE_SUFFIX,
        hitPlayer.getName() + SELECT_PREFIX + FIRST_STRIKE_UNITS + CASUALTIES_SUFFIX,
        hitPlayer.getName() + NOTIFY_PREFIX + FIRST_STRIKE_UNITS + CASUALTIES_SUFFIX);
  }

  private List<String> navalBombardmentFightStepStrings(
      final GamePlayer firingPlayer, final GamePlayer hitPlayer) {
    return List.of(
        firingPlayer.getName() + " " + NAVAL_BOMBARD + FIRE_SUFFIX,
        hitPlayer.getName() + SELECT_PREFIX + NAVAL_BOMBARD + CASUALTIES_SUFFIX,
        hitPlayer.getName() + NOTIFY_PREFIX + NAVAL_BOMBARD + CASUALTIES_SUFFIX,
        REMOVE_BOMBARDMENT_CASUALTIES);
  }

  private List<String> aaFightStepStrings(
      final String name, final GamePlayer firingPlayer, final GamePlayer hitPlayer) {
    return List.of(
        firingPlayer.getName() + " " + name + AA_GUNS_FIRE_SUFFIX,
        hitPlayer.getName() + SELECT_PREFIX + name + CASUALTIES_SUFFIX,
        hitPlayer.getName() + NOTIFY_PREFIX + name + CASUALTIES_SUFFIX);
  }

  private List<String> givenBattleSteps(final BattleState battleState) {
    return BattleSteps.builder()
        .battleActions(battleActions)
        .battleState(battleState)
        .build()
        .get();
  }

  @Test
  @DisplayName("Verify what an empty battle looks like")
  void emptyBattle() {
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(givenGameData().build())
                .attacker(attacker)
                .defender(defender)
                .build());

    assertThat(steps, is(List.of(REMOVE_CASUALTIES)));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle (no aa, air, bombard, etc)")
  void basicLandBattle() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on first run")
  void bombardOnFirstRun() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withNavalBombardCasualtiesReturnFire(false)
                        .withCaptureUnitsOnEnteringTerritory(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .bombardingUnits(List.of(givenAnyUnit()))
                .battleSite(battleSite)
                .battleRound(1)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                navalBombardmentFightStepStrings(attacker, defender), basicFightStepStrings())));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on subsequent run")
  void bombardOnSubsequentRun() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .battleRound(2)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify impossible sea battle with bombarding will not add a bombarding step")
  void impossibleSeaBattleWithBombarding() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .bombardingUnits(List.of(mock(Unit.class)))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightStepStrings()));

    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on first run")
  void paratroopersFirstRun() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenUnitAirTransport();

    when(unit1.getOwner()).thenReturn(attacker);
    when(unit3.getOwner()).thenReturn(attacker);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);
    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .battleRound(1)
                .gameData(givenGameData().build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .dependentUnits(List.of(mock(Unit.class)))
                .build());

    assertThat(steps, is(mergeSteps(List.of(LAND_PARATROOPS), basicFightStepStrings())));
    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with no AirTransport tech on first run")
  void noAirTransportTech() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on subsequent run")
  void paratroopersSubsequentRun() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .battleRound(2)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with empty paratroopers on first run")
  void emptyParatroopersFirstRun() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenUnitAirTransport();

    when(unit1.getOwner()).thenReturn(attacker);
    when(unit3.getOwner()).thenReturn(attacker);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);
    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(givenGameData().build())
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));

    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify impossible sea battle with paratroopers will not add a paratrooper step")
  void impossibleSeaBattleWithParatroopers() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightStepStrings()));

    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  @Test
  @DisplayName("Verify basic land battle with offensive Aa")
  void offensiveAaFire() {
    final Unit unit2 = givenAnyUnit();
    when(unit2.getOwner()).thenReturn(defender);
    final Unit unit1 =
        givenUnitIsCombatAa(Set.of(unit2.getType()), attacker, BattleState.Side.OFFENSE);
    when(unit1.getOwner()).thenReturn(attacker);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWarRelationship(attacker, defender, true)
                        .withWarRelationship(defender, attacker, true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                aaFightStepStrings("AntiAirGun", attacker, defender), basicFightStepStrings())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with defensive Aa")
  void defensiveAaFire() {
    final Unit unit1 = givenAnyUnit();
    when(unit1.getOwner()).thenReturn(attacker);
    final Unit unit2 =
        givenUnitIsCombatAa(Set.of(unit1.getType()), defender, BattleState.Side.DEFENSE);
    when(unit2.getOwner()).thenReturn(defender);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWarRelationship(attacker, defender, true)
                        .withWarRelationship(defender, attacker, true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                aaFightStepStrings("AntiAirGun", defender, attacker), basicFightStepStrings())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with offensive and defensive Aa")
  void offensiveAndDefensiveAaFire() {
    final Unit target1 = givenAnyUnit();
    final Unit target2 = givenAnyUnit();
    when(target1.getOwner()).thenReturn(attacker);
    when(target2.getOwner()).thenReturn(defender);

    final Unit unit1 =
        givenUnitIsCombatAa(Set.of(target2.getType()), attacker, BattleState.Side.OFFENSE);
    final Unit unit2 =
        givenUnitIsCombatAa(Set.of(target1.getType()), defender, BattleState.Side.DEFENSE);
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit2.getOwner()).thenReturn(defender);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWarRelationship(attacker, defender, true)
                        .withWarRelationship(defender, attacker, true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, target1))
                .defendingUnits(List.of(unit2, target2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                aaFightStepStrings("AntiAirGun", attacker, defender),
                aaFightStepStrings("AntiAirGun", defender, attacker),
                basicFightStepStrings())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can retreat if SUB_RETREAT_BEFORE_BATTLE and no destroyers")
  void defendingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitCanEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(true)
                        .withSubmersibleSubs(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(mergeSteps(List.of(defender.getName() + SUBS_SUBMERGE), basicFightStepStrings())));
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can retreat if SUB_RETREAT_BEFORE_BATTLE and destroyers")
  void defendingSubsNotRetreatIfDestroyersAndCanRetreatBeforeBattle() {
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitCanEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(true)
                        .withSubmersibleSubs(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(mergeSteps(List.of(defender.getName() + SUBS_SUBMERGE), basicFightStepStrings())));
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE is false")
  void defendingSubsRetreatIfCanNotRetreatBeforeBattle() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitCanEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike submerge before battle "
          + "if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true")
  void defendingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(true)
                        .withSubmersibleSubs(true)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(defender.getName() + SUBS_SUBMERGE),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify unescorted attacking transports are removed if casualties are restricted")
  void unescortedAttackingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    final Unit unit1 = givenUnitSeaTransport();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightStepStrings())));
  }

  @Test
  @DisplayName(
      "Verify unescorted attacking transports are not removed if casualties are not restricted")
  void unescortedAttackingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit1 = unitAndAttachment.unit;
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName("Verify unescorted defending transports are removed if casualties are restricted")
  void unescortedDefendingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitSeaTransport();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightStepStrings())));
  }

  @Test
  @DisplayName(
      "Verify unescorted defending transports are removed if casualties are not restricted")
  void unescortedDefendingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    final Unit unit1 = givenAnyUnit();
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment.unit;

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName(
      "Verify basic attacker firstStrike "
          + "(no other attackers, no special defenders, all options false)")
  void attackingFirstStrikeBasic() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacker firstStrike with destroyers")
  void attackingFirstStrikeWithDestroyers() {
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify basic defender firstStrike "
          + "(no other attackers, no special defenders, all options false)")
  void defendingFirstStrikeBasic() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify defender firstStrike with DEFENDING_SUBS_SNEAK_ATTACK true")
  void defendingFirstStrikeWithSneakAttackAllowed() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify defender firstStrike with WW2v2 true")
  void defendingFirstStrikeWithWW2v2() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify defender firstStrike with WW2v2 true and attacker destroyers")
  void defendingFirstStrikeWithWW2v2AndDestroyers() {
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify basic attacker and defender firstStrikes "
          + "(no other attackers, no special defenders, all options false)")
  void attackingDefendingFirstStrikeBasic() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withDefendingSubsSneakAttack(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true")
  void attackingDefendingFirstStrikeWithSneakAttackAllowed() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true")
  void attackingDefendingFirstStrikeWithWW2v2() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify attacking/defender firstStrikes with WW2v2 true and attacker/defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDestroyers() {
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();
    final Unit unit4 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2, unit4))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                generalFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify attacking/defender firstStrikes with "
          + "DEFENDING_SUBS_SNEAK_ATTACK true and defender destroyers")
  void attackingDefendingFirstStrikeWithSneakAttackAllowedAndDefendingDestroyers() {
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                firstStrikeFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDefendingDestroyers() {
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and attacking destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndAttackingDestroyers() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking firstStrikes against air")
  void attackingFirstStrikeVsAir() {
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(unit2.getType());

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(SUBMERGE_SUBS_VS_AIR_ONLY),
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking firstStrikes against air with other units on both sides")
  void attackingFirstStrikeVsAirWithOtherUnits() {
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(unit2.getType());
    final Unit unit3 = givenAnyUnit();
    final Unit unit4 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit4))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker, "air vs non subs"),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking firstStrikes against air with destroyer")
  void attackingFirstStrikeVsAirAndDestroyer() {
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit3 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air")
  void defendingFirstStrikeVsAir() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(unit1.getType());

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(SUBMERGE_SUBS_VS_AIR_ONLY),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                List.of(REMOVE_CASUALTIES, attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air with destroyer")
  void defendingFirstStrikeVsAirAndDestroyer() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES, attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air with other units on both sides")
  void defendingFirstStrikeVsAirWithOtherUnits() {
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(unit2.getType());
    final Unit unit3 = givenAnyUnit();
    final Unit unit4 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(true)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit2, unit3))
                .defendingUnits(List.of(unit1, unit4))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender, "air vs non subs"),
                generalFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES, attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void attackingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withSubmersibleSubs(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES, attacker.getName() + SUBS_SUBMERGE))));
  }

  @Test
  @DisplayName("Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void defendingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(true)
                        .withSubmersibleSubs(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(attacker, defender),
                List.of(REMOVE_CASUALTIES, defender.getName() + SUBS_SUBMERGE))));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true even with destroyers")
  void defendingFirstStrikeCanSubmergeIfSubmersibleSubsAndDestroyers() {
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(false)
                        .withWW2V2(false)
                        .withSubmersibleSubs(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES, defender.getName() + SUBS_SUBMERGE))));
  }

  @Test
  @DisplayName("Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void attackingFirstStrikeWithdrawIfAble() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .attackerRetreatTerritories(List.of(battleSite))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker.getName() + SUBS_WITHDRAW,
                    attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and no retreat territories")
  void attackingFirstStrikeNoWithdrawIfEmptyTerritories() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and destroyers present")
  void attackingFirstStrikeNoWithdrawIfDestroyers() {
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .attackerRetreatTerritories(List.of(battleSite))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                generalFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES, attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can withdraw when "
          + "SUBMERSIBLE_SUBS is false and defenseless transports with non restricted casualties")
  void attackingFirstStrikeWithdrawIfNonRestrictedDefenselessTransports() {
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment.unit;

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .attackerRetreatTerritories(List.of(battleSite))
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                firstStrikeFightStepStrings(attacker, defender),
                List.of(REMOVE_SNEAK_ATTACK_CASUALTIES),
                generalFightStepStrings(defender, attacker),
                List.of(
                    REMOVE_CASUALTIES,
                    attacker.getName() + SUBS_WITHDRAW,
                    attacker.getName() + ATTACKER_WITHDRAW))));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName("Verify defending firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void defendingFirstStrikeWithdrawIfAble() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final Territory retreatTerritory = mock(Territory.class);
    when(retreatTerritory.isWater()).thenReturn(true);
    when(retreatTerritory.getUnitCollection()).thenReturn(mock(UnitCollection.class));

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withTerritoryHasNeighbors(battleSite, Set.of(retreatTerritory))
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES, defender.getName() + SUBS_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and no retreat territories")
  void defendingFirstStrikeNoWithdrawIfEmptyTerritories() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withWW2V2(false)
                        .withDefendingSubsSneakAttack(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and destroyers present")
  void defendingFirstStrikeNoWithdrawIfDestroyers() {
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withWW2V2(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                generalFightStepStrings(attacker, defender),
                firstStrikeFightStepStrings(defender, attacker),
                List.of(REMOVE_CASUALTIES))));
  }

  @Test
  @DisplayName("Verify attacking air units at sea can withdraw")
  void attackingAirUnitsAtSeaCanWithdraw() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withTransportCasualtiesRestricted(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(mergeSteps(basicFightStepStrings(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can withdraw if a unit was not amphibious")
  void partialAmphibiousAttackCanWithdrawIfHasNonAmphibious() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3.getWasAmphibious()).thenReturn(false);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withPartialAmphibiousRetreat(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps,
        is(mergeSteps(basicFightStepStrings(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can not withdraw if all units were amphibious")
  void partialAmphibiousAttackCanNotWithdrawIfHasAllAmphibious() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3.getWasAmphibious()).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withWW2V2(false)
                        .withAlliedAirIndependent(true)
                        .withAttackerRetreatPlanes(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withPartialAmphibiousRetreat(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
  }

  @Test
  @DisplayName(
      "Verify partial amphibious attack can not withdraw if "
          + "partial amphibious withdrawal not allowed")
  void partialAmphibiousAttackCanNotWithdrawIfNotAllowed() {
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
    // should never check for amphibious units
    verify(unit1, never()).getWasAmphibious();
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if ww2v2 and amphibious")
  void attackingPlanesCanWithdrawWW2v2AndAmphibious() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .withPartialAmphibiousRetreat(false)
                        .withWW2V2(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps,
        is(mergeSteps(basicFightStepStrings(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can withdraw if "
          + "attacker can partial amphibious retreat and amphibious")
  void attackingPlanesCanWithdrawPartialAmphibiousAndAmphibious() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(unit3.getWasAmphibious()).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withWW2V2(false)
                        .withAttackerRetreatPlanes(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withPartialAmphibiousRetreat(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps,
        is(mergeSteps(basicFightStepStrings(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if attacker can retreat planes and amphibious")
  void attackingPlanesCanWithdrawPlanesRetreatAndAmphibious() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withSubRetreatBeforeBattle(false)
                        .withWW2V2(false)
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withAlliedAirIndependent(true)
                        .withPartialAmphibiousRetreat(false)
                        .withAttackerRetreatPlanes(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps,
        is(mergeSteps(basicFightStepStrings(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can not withdraw if ww2v2 and not amphibious")
  void attackingPlanesCanNotWithdrawWW2v2AndNotAmphibious() {
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(
                    givenGameData()
                        .withDefendingSuicideAndMunitionUnitsDoNotFire(false)
                        .withSubRetreatBeforeBattle(false)
                        .withAlliedAirIndependent(true)
                        .withTransportCasualtiesRestricted(true)
                        .build())
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(false)
                .build());

    assertThat(steps, is(basicFightStepStrings()));
  }
}
