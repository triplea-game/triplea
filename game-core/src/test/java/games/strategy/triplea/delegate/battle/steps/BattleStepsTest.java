package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.Constants.ATTACKER_RETREAT_PLANES;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE;
import static games.strategy.triplea.Constants.PARTIAL_AMPHIBIOUS_RETREAT;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.Constants.WW2V2;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AA_GUNS_FIRE_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_ATTACK_NON_SUBS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.AIR_DEFEND_NON_SUBS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.CASUALTIES_SUFFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.FIRST_STRIKE_UNITS_FIRE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.LAND_PARATROOPS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.NAVAL_BOMBARDMENT;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_SNEAK_ATTACK_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.REMOVE_UNESCORTED_TRANSPORTS;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_FIRST_STRIKE_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_NAVAL_BOMBARDMENT_CASUALTIES;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SELECT_PREFIX;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBMERGE_SUBS_VS_AIR_ONLY;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_SUBMERGE;
import static games.strategy.triplea.delegate.battle.BattleStepStrings.SUBS_WITHDRAW;
import static games.strategy.triplea.delegate.battle.FakeBattleState.givenBattleStateBuilder;
import static games.strategy.triplea.delegate.battle.steps.BattleSteps.FIRE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
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

  @Mock GameData gameData;
  @Mock GameProperties gameProperties;
  @Mock BattleActions battleActions;

  @Mock Territory battleSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;
  @Mock TechAttachment techAttachment;

  @BeforeEach
  void setupMocks() {
    when(gameData.getProperties()).thenReturn(gameProperties);
  }

  private void givenPlayers() {
    when(attacker.getName()).thenReturn("mockAttacker");
    when(defender.getName()).thenReturn("mockDefender");
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

  public static Unit givenUnitTransport() {
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

  public static Unit givenUnitWithTypeAa() {
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    when(unitAndAttachment.unitAttachment.getTypeAa()).thenReturn("AntiAirGun");
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

  @SafeVarargs
  private List<String> mergeSteps(final List<String>... steps) {
    return Stream.of(steps).flatMap(Collection::stream).collect(Collectors.toList());
  }

  private List<String> basicFightSteps() {
    return List.of(
        attacker.getName() + FIRE,
        defender.getName() + SELECT_CASUALTIES,
        defender.getName() + FIRE,
        attacker.getName() + SELECT_CASUALTIES,
        REMOVE_CASUALTIES);
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
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .build());

    assertThat(steps, is(List.of(REMOVE_CASUALTIES)));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle (no aa, air, bombard, etc)")
  void basicLandBattle() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on first run")
  void bombardOnFirstRun() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .bombardingUnits(List.of(mock(Unit.class)))
                .battleSite(battleSite)
                .battleRound(1)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(NAVAL_BOMBARDMENT, SELECT_NAVAL_BOMBARDMENT_CASUALTIES),
                basicFightSteps())));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on subsequent run")
  void bombardOnSubsequentRun() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .battleRound(2)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify impossible sea battle with bombarding will not add a bombarding step")
  void impossibleSeaBattleWithBombarding() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .bombardingUnits(List.of(mock(Unit.class)))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightSteps()));

    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on first run")
  void paratroopersFirstRun() {
    givenPlayers();
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
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .dependentUnits(List.of(mock(Unit.class)))
                .build());

    assertThat(steps, is(mergeSteps(List.of(LAND_PARATROOPS), basicFightSteps())));
    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with no AirTransport tech on first run")
  void noAirTransportTech() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on subsequent run")
  void paratroopersSubsequentRun() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .battleRound(2)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with empty paratroopers on first run")
  void emptyParatroopersFirstRun() {
    givenPlayers();
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
                .gameData(gameData)
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));

    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify impossible sea battle with paratroopers will not add a paratrooper step")
  void impossibleSeaBattleWithParatroopers() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .battleRound(1)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightSteps()));

    verify(attacker, never()).getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  @Test
  @DisplayName("Verify basic land battle with offensive Aa")
  void offensiveAaFire() {
    givenPlayers();
    final Unit unit1 = givenUnitWithTypeAa();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .offensiveAa(List.of(unit1))
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(
                    attacker.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    defender.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    defender.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX),
                basicFightSteps())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with defensive Aa")
  void defensiveAaFire() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitWithTypeAa();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .defendingAa(List.of(unit2))
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(
                    defender.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    attacker.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    attacker.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX),
                basicFightSteps())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with offensive and defensive Aa")
  void offensiveAndDefensiveAaFire() {
    givenPlayers();
    final Unit unit1 = givenUnitWithTypeAa();
    final Unit unit2 = givenUnitWithTypeAa();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .offensiveAa(List.of(unit1))
                .defendingAa(List.of(unit2))
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(
                    attacker.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    defender.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    defender.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    defender.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    attacker.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    attacker.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX),
                basicFightSteps())));

    verify(battleSite, never()).getUnits();
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can retreat if SUB_RETREAT_BEFORE_BATTLE and no destroyers")
  void defendingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitCanEvade();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps, is(mergeSteps(List.of(defender.getName() + SUBS_SUBMERGE), basicFightSteps())));
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE and destroyers")
  void defendingSubsNotRetreatIfDestroyersAndCanRetreatBeforeBattle() {
    givenPlayers();
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitCanEvade();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units can not retreat if SUB_RETREAT_BEFORE_BATTLE is false")
  void defendingSubsRetreatIfCanNotRetreatBeforeBattle() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitCanEvade();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike submerge before battle "
          + "if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true")
  void defendingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + SUBS_SUBMERGE,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify unescorted attacking transports are removed if casualties are restricted")
  void unescortedAttackingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    givenPlayers();
    final Unit unit1 = givenUnitTransport();
    final Unit unit2 = givenAnyUnit();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
  }

  @Test
  @DisplayName(
      "Verify unescorted attacking transports are not removed if casualties are not restricted")
  void unescortedAttackingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    givenPlayers();
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit1 = unitAndAttachment.unit;
    final Unit unit2 = givenAnyUnit();
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName("Verify unescorted defending transports are removed if casualties are restricted")
  void unescortedDefendingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitTransport();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
  }

  @Test
  @DisplayName(
      "Verify unescorted defending transports are removed if casualties are not restricted")
  void unescortedDefendingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment.unit;
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(steps, is(basicFightSteps()));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName(
      "Verify basic attacker firstStrike "
          + "(no other attackers, no special defenders, all options false)")
  void attackingFirstStrikeBasic() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacker firstStrike with destroyers")
  void attackingFirstStrikeWithDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify basic defender firstStrike "
          + "(no other attackers, no special defenders, all options false)")
  void defendingFirstStrikeBasic() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify defender firstStrike with DEFENDING_SUBS_SNEAK_ATTACK true")
  void defendingFirstStrikeWithSneakAttackAllowed() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify defender firstStrike with WW2v2 true")
  void defendingFirstStrikeWithWW2v2() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify defender firstStrike with WW2v2 true and attacker destroyers")
  void defendingFirstStrikeWithWW2v2AndDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify basic attacker and defender firstStrikes "
          + "(no other attackers, no special defenders, all options false)")
  void attackingDefendingFirstStrikeBasic() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true")
  void attackingDefendingFirstStrikeWithSneakAttackAllowed() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                // TODO: BUG? should this have been skipped
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true")
  void attackingDefendingFirstStrikeWithWW2v2() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                // TODO: BUG? should this have been skipped
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify attacking/defender firstStrikes with WW2v2 true and attacker/defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();
    final Unit unit4 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2, unit4))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify attacking/defender firstStrikes with "
          + "DEFENDING_SUBS_SNEAK_ATTACK true and defender destroyers")
  void attackingDefendingFirstStrikeWithSneakAttackAllowedAndDefendingDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDefendingDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and attacking destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndAttackingDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking firstStrikes against air")
  void attackingFirstStrikeVsAir() {
    givenPlayers();
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(unit2.getType());

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                SUBMERGE_SUBS_VS_AIR_ONLY,
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                AIR_DEFEND_NON_SUBS,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking firstStrikes against air with destroyer")
  void attackingFirstStrikeVsAirAndDestroyer() {
    givenPlayers();
    final Unit unit2 = givenUnitIsAir();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit3 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2, unit3))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air")
  void defendingFirstStrikeVsAir() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenUnitFirstStrikeAndEvadeAndCanNotBeTargetedBy(mock(UnitType.class));

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                SUBMERGE_SUBS_VS_AIR_ONLY,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                AIR_ATTACK_NON_SUBS,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air with destroyer")
  void defendingFirstStrikeVsAirAndDestroyer() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();
    final Unit unit3 = givenUnitDestroyer();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName("Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void attackingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + SUBS_SUBMERGE)));
  }

  @Test
  @DisplayName("Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void defendingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                defender.getName() + SUBS_SUBMERGE)));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true even with destroyers")
  void defendingFirstStrikeCanSubmergeIfSubmersibleSubsAndDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES,
                defender.getName() + SUBS_SUBMERGE)));
  }

  @Test
  @DisplayName("Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void attackingFirstStrikeWithdrawIfAble() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
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
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + SUBS_WITHDRAW,
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and no retreat territories")
  void attackingFirstStrikeNoWithdrawIfEmptyTerritories() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and destroyers present")
  void attackingFirstStrikeNoWithdrawIfDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrike();
    final Unit unit2 = givenUnitDestroyer();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
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
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can withdraw when "
          + "SUBMERSIBLE_SUBS is false and defenseless transports with non restricted casualties")
  void attackingFirstStrikeWithdrawIfNonRestrictedDefenselessTransports() {
    givenPlayers();
    final Unit unit1 = givenUnitFirstStrikeAndEvade();
    final UnitAndAttachment unitAndAttachment = newUnitAndAttachment();
    final Unit unit2 = unitAndAttachment.unit;

    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
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
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + SUBS_WITHDRAW,
                attacker.getName() + ATTACKER_WITHDRAW)));
    verify(unitAndAttachment.unitAttachment, never()).getTransportCapacity();
  }

  @Test
  @DisplayName("Verify defending firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void defendingFirstStrikeWithdrawIfAble() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .emptyOrFriendlySeaNeighbors(List.of(battleSite))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES,
                defender.getName() + SUBS_WITHDRAW)));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and no retreat territories")
  void defendingFirstStrikeNoWithdrawIfEmptyTerritories() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify defending firstStrike can't withdraw when "
          + "SUBMERSIBLE_SUBS is false and destroyers present")
  void defendingFirstStrikeNoWithdrawIfDestroyers() {
    givenPlayers();
    final Unit unit1 = givenUnitDestroyer();
    final Unit unit2 = givenUnitFirstStrikeAndEvade();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .build());

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking air units at sea can withdraw")
  void attackingAirUnitsAtSeaCanWithdraw() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(givenSeaBattleSite())
                .build());

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can withdraw if a unit was not amphibious")
  void partialAmphibiousAttackCanWithdrawIfHasNonAmphibious() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3.getWasAmphibious()).thenReturn(false);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can not withdraw if all units were amphibious")
  void partialAmphibiousAttackCanNotWithdrawIfHasAllAmphibious() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3.getWasAmphibious()).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify partial amphibious attack can not withdraw if "
          + "partial amphibious withdrawal not allowed")
  void partialAmphibiousAttackCanNotWithdrawIfNotAllowed() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(steps, is(basicFightSteps()));
    // should never check for amphibious units
    verify(unit1, never()).getWasAmphibious();
  }

  @Test
  @DisplayName("Verify partial amphibious attack can not withdraw if not amphibious")
  void partialAmphibiousAttackCanNotWithdrawIfNotAmphibious() {
    givenPlayers();
    final Unit unit1 = givenAnyUnit();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(false)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if ww2v2 and amphibious")
  void attackingPlanesCanWithdrawWW2v2AndAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can withdraw if "
          + "attacker can partial amphibious retreat and amphibious")
  void attackingPlanesCanWithdrawPartialAmphibiousAndAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();
    when(unit3.getWasAmphibious()).thenReturn(true);

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if attacker can retreat planes and amphibious")
  void attackingPlanesCanWithdrawPlanesRetreatAndAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(true)
                .build());

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can not withdraw if ww2v2 and not amphibious")
  void attackingPlanesCanNotWithdrawWW2v2AndNotAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);

    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(false)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can not withdraw if "
          + "attacker can partial amphibious retreat and not amphibious")
  void attackingPlanesCanNotWithdrawPartialAmphibiousAndNotAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();
    final Unit unit3 = givenAnyUnit();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1, unit3))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(false)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can not withdraw if attacker can retreat planes and not amphibious")
  void attackingPlanesCanNotWithdrawPlanesRetreatAndNotAmphibious() {
    givenPlayers();
    final Unit unit1 = givenUnitIsAir();
    final Unit unit2 = givenAnyUnit();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false))
        .thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(true);
    final List<String> steps =
        givenBattleSteps(
            givenBattleStateBuilder()
                .gameData(gameData)
                .attacker(attacker)
                .defender(defender)
                .attackingUnits(List.of(unit1))
                .defendingUnits(List.of(unit2))
                .battleSite(battleSite)
                .amphibious(false)
                .build());

    assertThat(steps, is(basicFightSteps()));
  }
}
