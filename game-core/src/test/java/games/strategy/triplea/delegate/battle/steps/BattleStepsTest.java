package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.Constants.ATTACKER_RETREAT_PLANES;
import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.PARTIAL_AMPHIBIOUS_RETREAT;
import static games.strategy.triplea.Constants.SUBMERSIBLE_SUBS;
import static games.strategy.triplea.Constants.SUB_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.Constants.TRANSPORT_CASUALTIES_RESTRICTED;
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
import static games.strategy.triplea.delegate.battle.steps.BattleSteps.FIRE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleStepsTest {

  @Mock GameData gameData;
  @Mock GameProperties gameProperties;
  @Mock Function<Collection<Unit>, Collection<Unit>> getDependentUnits;
  @Mock Supplier<Collection<Territory>> getAttackerRetreatTerritories;

  @Mock
  BiFunction<GamePlayer, Collection<Unit>, Collection<Territory>> getEmptyOrFriendlySeaNeighbors;

  @Mock Territory battleSite;
  @Mock GamePlayer attacker;
  @Mock GamePlayer defender;
  @Mock TechAttachment techAttachment;
  @Mock Unit unit1;
  @Mock UnitType unit1Type;
  @Mock UnitAttachment unit1Attachment;
  @Mock Unit unit2;
  @Mock UnitType unit2Type;
  @Mock UnitAttachment unit2Attachment;
  @Mock Unit unit3;
  @Mock UnitType unit3Type;
  @Mock UnitAttachment unit3Attachment;
  @Mock Unit unit4;
  @Mock UnitType unit4Type;
  @Mock UnitAttachment unit4Attachment;

  @BeforeEach
  void setupMocks() {
    when(gameData.getProperties()).thenReturn(gameProperties);
  }

  private void players() {
    when(attacker.getName()).thenReturn("mockAttacker");
    when(defender.getName()).thenReturn("mockDefender");
  }

  private void unit1() {
    when(unit1.getType()).thenReturn(unit1Type);
    when(unit1Type.getAttachment(anyString())).thenReturn(unit1Attachment);
  }

  private void unit2() {
    when(unit2.getType()).thenReturn(unit2Type);
    when(unit2Type.getAttachment(anyString())).thenReturn(unit2Attachment);
  }

  private void unit3() {
    when(unit3.getType()).thenReturn(unit3Type);
    when(unit3Type.getAttachment(anyString())).thenReturn(unit3Attachment);
  }

  private void unit4() {
    when(unit4.getType()).thenReturn(unit4Type);
    when(unit4Type.getAttachment(anyString())).thenReturn(unit4Attachment);
  }

  private void attackerRetreat() {
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of());
  }

  private void defenderRetreat() {
    when(getEmptyOrFriendlySeaNeighbors.apply(any(), any())).thenReturn(List.of());
  }

  private void makeAttackerFirstStrike(final UnitAttachment attachment) {
    when(attachment.getIsFirstStrike()).thenReturn(true);
    when(attachment.getCanEvade()).thenReturn(true);
  }

  private void makeDefenderFirstStrike(final UnitAttachment attachment) {
    when(attachment.getIsFirstStrike()).thenReturn(true);
    when(attachment.getCanEvade()).thenReturn(true);
    when(attachment.getCanNotTarget()).thenReturn(Set.of(unit3Type));
  }

  private void makeDestroyer(final UnitAttachment attachment) {
    when(attachment.getIsDestroyer()).thenReturn(true);
  }

  private void makeTransport(final UnitAttachment attachment) {
    when(attachment.getTransportCapacity()).thenReturn(2);
    when(attachment.getIsSea()).thenReturn(true);
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

  private BattleSteps.BattleStepsBuilder getStepBuilder() {
    return BattleSteps.builder()
        .canFireOffensiveAa(false)
        .canFireDefendingAa(false)
        .showFirstRun(true)
        .attacker(attacker)
        .defender(defender)
        .offensiveAa(List.of())
        .defendingAa(List.of())
        .attackingUnits(List.of())
        .defendingUnits(List.of())
        .attackingWaitingToDie(List.of())
        .defendingWaitingToDie(List.of())
        .battleSite(battleSite)
        .gameData(gameData)
        .bombardingUnits(List.of())
        .getDependentUnits(getDependentUnits)
        .isBattleSiteWater(true)
        .getAttackerRetreatTerritories(getAttackerRetreatTerritories)
        .getEmptyOrFriendlySeaNeighbors(getEmptyOrFriendlySeaNeighbors)
        .isAmphibious(false);
  }

  @Test
  @DisplayName("Verify what an empty battle looks like")
  void emptyBattle() {
    attackerRetreat();
    final List<String> steps = getStepBuilder().build().get();

    assertThat(steps, is(List.of(REMOVE_CASUALTIES)));

    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle (no aa, air, bombard, etc)")
  void basicLandBattle() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on first run")
  void bombardOnFirstRun() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .bombardingUnits(List.of(unit3))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(NAVAL_BOMBARDMENT, SELECT_NAVAL_BOMBARDMENT_CASUALTIES),
                basicFightSteps())));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with bombard on subsequent run")
  void bombardOnSubsequentRun() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    final List<String> steps =
        getStepBuilder()
            .showFirstRun(false)
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on first run")
  void paratroopersFirstRun() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit3.getOwner()).thenReturn(attacker);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);
    when(unit3Attachment.getIsAirTransport()).thenReturn(true);
    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    when(getDependentUnits.apply(any())).thenReturn(List.of(unit4));
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(mergeSteps(List.of(LAND_PARATROOPS), basicFightSteps())));
    verify(getDependentUnits, times(1)).apply(any());
    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with no AirTransport tech on first run")
  void noAirTransportTech() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(false);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with paratroopers on subsequent run")
  void paratroopersSubsequentRun() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    final List<String> steps =
        getStepBuilder()
            .showFirstRun(false)
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(attacker, times(0)).getAttachment(Constants.TECH_ATTACHMENT_NAME);
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with empty paratroopers on first run")
  void emptyParatroopersFirstRun() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    when(unit1.getOwner()).thenReturn(attacker);
    when(unit3.getOwner()).thenReturn(attacker);
    when(attacker.getAttachment(Constants.TECH_ATTACHMENT_NAME)).thenReturn(techAttachment);
    when(attacker.getTechAttachment()).thenReturn(techAttachment);
    when(techAttachment.getParatroopers()).thenReturn(true);
    when(unit3Attachment.getIsAirTransport()).thenReturn(true);
    when(battleSite.getUnits()).thenReturn(List.of(unit1, unit3));
    when(getDependentUnits.apply(any())).thenReturn(List.of());
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));

    verify(getDependentUnits, times(1)).apply(any());
    verify(battleSite, times(1)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with offensive Aa")
  void offensiveAaFire() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(unit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    final List<String> steps =
        getStepBuilder()
            .canFireOffensiveAa(true)
            .offensiveAa(List.of(unit1))
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(
                    attacker.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    defender.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    defender.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX),
                basicFightSteps())));

    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with defensive Aa")
  void defensiveAaFire() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(unit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    final List<String> steps =
        getStepBuilder()
            .canFireDefendingAa(true)
            .defendingAa(List.of(unit2))
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

    assertThat(
        steps,
        is(
            mergeSteps(
                List.of(
                    defender.getName() + " " + "AntiAirGun" + AA_GUNS_FIRE_SUFFIX,
                    attacker.getName() + SELECT_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX,
                    attacker.getName() + REMOVE_PREFIX + "AntiAirGun" + CASUALTIES_SUFFIX),
                basicFightSteps())));

    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify basic land battle with offensive and defensive Aa")
  void offensiveAndDefensiveAaFire() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(unit1Attachment.getTypeAa()).thenReturn("AntiAirGun");
    when(unit2Attachment.getTypeAa()).thenReturn("AntiAirGun");
    final List<String> steps =
        getStepBuilder()
            .canFireOffensiveAa(true)
            .canFireDefendingAa(true)
            .offensiveAa(List.of(unit1))
            .defendingAa(List.of(unit2))
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .build()
            .get();

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

    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName(
      "Verify impossible sea battle with bombarding and paratroopers "
          + "will not add a bombarding or paratrooper step")
  void impossibleSeaBattleWithBombardingAndParatroopers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .bombardingUnits(List.of(unit3))
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));

    verify(getDependentUnits, times(0)).apply(any());
    verify(attacker, times(0)).getAttachment(Constants.TECH_ATTACHMENT_NAME);
  }

  @Test
  @DisplayName(
      "Verify attacking canEvade units retreating if SUB_RETREAT_BEFORE_BATTLE and no destroyers")
  void attackingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    players();
    attackerRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(unit1Attachment.getCanEvade()).thenReturn(true);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(List.of(attacker.getName() + SUBS_SUBMERGE), basicFightSteps())));

    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName(
      "Verify defending canEvade units retreating if SUB_RETREAT_BEFORE_BATTLE and no destroyers")
  void defendingSubsRetreatIfNoDestroyersAndCanRetreatBeforeBattle() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(unit2Attachment.getCanEvade()).thenReturn(true);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(List.of(defender.getName() + SUBS_SUBMERGE), basicFightSteps())));
  }

  @Test
  @DisplayName("Verify attacking transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  void attackingTransportsAreRemovedIfTransportCasualtiesRestricted() {
    players();
    attackerRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    makeTransport(unit1Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
  }

  @Test
  @DisplayName("Verify defending transports are removed if TRANSPORT_CASUALTIES_RESTRICTED is true")
  void defendingTransportsAreRemovedIfTransportCasualtiesRestricted() {
    players();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    makeTransport(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
  }

  @Test
  @DisplayName(
      "Verify basic attacker firstStrike "
          + "(no other attackers, no special defenders, all options false)")
  void attackingFirstStrikeBasic() {
    players();
    attackerRetreat();
    unit1();
    unit2();

    makeAttackerFirstStrike(unit1Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();

    makeAttackerFirstStrike(unit1Attachment);
    makeDestroyer(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName(
      "Verify defender firstStrike with DEFENDING_SUBS_SNEAK_ATTACK true "
          + "and attacker destroyers")
  void defendingFirstStrikeWithSneakAttackAllowedAndDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit1Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName("Verify defender firstStrike with WW2v2 true")
  void defendingFirstStrikeWithWW2v2() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit1Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName(
      "Verify basic attacker and defender firstStrikes "
          + "(no other attackers, no special defenders, all options false)")
  void attackingDefendingFirstStrikeBasic() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true "
          + "and attacker/defender destroyers")
  void attackingDefendingFirstStrikeWithSneakAttackAllowedAndDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    unit4();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);
    makeDestroyer(unit4Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2, unit4))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true")
  void attackingDefendingFirstStrikeWithWW2v2() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify attacking/defender firstStrikes with WW2v2 true "
          + "and attacker/defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    unit4();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);
    makeDestroyer(unit4Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2, unit4))
            .build()
            .get();

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
      "Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true "
          + "and defender destroyers")
  void attackingDefendingFirstStrikeWithSneakAttackAllowedAndDefendingDestroyers() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2, unit3))
            .build()
            .get();

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
  @DisplayName(
      "Verify attacking/defender firstStrikes with DEFENDING_SUBS_SNEAK_ATTACK true "
          + "and attacking destroyers")
  void attackingDefendingFirstStrikeWithSneakAttackAllowedAndAttackingDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify attacking/defender firstStrikes with WW2v2 true and defender destroyers")
  void attackingDefendingFirstStrikeWithWW2v2AndDefendingDestroyers() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2, unit3))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    when(unit1Attachment.getCanNotBeTargetedBy()).thenReturn(Set.of(unit2Type));
    when(unit2Attachment.getIsAir()).thenReturn(true);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
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
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    when(unit1Attachment.getCanNotBeTargetedBy()).thenReturn(Set.of(unit2Type));
    when(unit2Attachment.getIsAir()).thenReturn(true);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2, unit3))
            .build()
            .get();

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
  @DisplayName("Verify defending firstStrikes against air")
  void defendingFirstStrikeVsAir() {
    players();
    attackerRetreat();
    defenderRetreat();
    unit1();
    unit2();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    when(unit2Attachment.getCanNotBeTargetedBy()).thenReturn(Set.of(unit3Type));

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                defender.getName() + FIRST_STRIKE_UNITS_FIRE,
                attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                SUBMERGE_SUBS_VS_AIR_ONLY,
                AIR_ATTACK_NON_SUBS,
                attacker.getName() + FIRE,
                defender.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName("Verify defending firstStrikes against air with destroyer")
  void defendingFirstStrikeVsAirAndDestroyer() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();

    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    makeDestroyer(unit3Attachment);

    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName("Verify unescorted attacking transports are removed if casualities are restricted")
  void unescortedAttackingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    makeTransport(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName("Verify unescorted defending transports are removed if casualities are restricted")
  void unescortedDefendingTransportsAreRemovedWhenCasualtiesAreRestricted() {
    players();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    makeTransport(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(mergeSteps(List.of(REMOVE_UNESCORTED_TRANSPORTS), basicFightSteps())));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
  }

  @Test
  @DisplayName(
      "Verify unescorted attacking transports are not removed if casualties are not restricted")
  void unescortedAttackingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
    // it shouldn't even ask for the transportCapacity
    verify(unit1Attachment, times(0)).getTransportCapacity();
  }

  @Test
  @DisplayName(
      "Verify unescorted defending transports are removed if casualities are not restricted")
  void unescortedDefendingTransportsAreNotRemovedWhenCasualtiesAreNotRestricted() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    verify(getDependentUnits, times(0)).apply(any());
    verify(battleSite, times(0)).getUnits();
    // it shouldn't even ask for the transportCapacity
    verify(unit2Attachment, times(0)).getTransportCapacity();
    verify(unit2Attachment, times(0)).getIsSea();
  }

  @Test
  @DisplayName("Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void attackingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName(
      "Verify attacking firstStrike can submerge if SUBMERSIBLE_SUBS is true even with destroyers")
  void attackingFirstStrikeCanSubmergeIfSubmersibleSubsAndDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeDestroyer(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES,
                attacker.getName() + SUBS_SUBMERGE)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike submerge before battle "
          + "if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true")
  void attackingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                attacker.getName() + SUBS_SUBMERGE,
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName("Verify defending firstStrike can submerge if SUBMERSIBLE_SUBS is true")
  void defendingFirstStrikeCanSubmergeIfSubmersibleSubs() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeDestroyer(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName(
      "Verify defending firstStrike submerge before battle "
          + "if SUB_RETREAT_BEFORE_BATTLE and SUBMERSIBLE_SUBS are true")
  void defendingFirstStrikeSubmergeBeforeBattleIfSubmersibleSubsAndRetreatBeforeBattle() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(true);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(true);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(SUBMERSIBLE_SUBS, false)).thenReturn(true);
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
  @DisplayName("Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void attackingFirstStrikeWithdrawIfAble() {
    players();
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of(battleSite));
    unit1();
    unit2();
    makeAttackerFirstStrike(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify attacking firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and no retreat territories")
  void attackingFirstStrikeNoWithdrawIfEmptyTerritories() {
    players();
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of());
    unit1();
    unit2();
    makeAttackerFirstStrike(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify attacking firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and destroyers present")
  void attackingFirstStrikeNoWithdrawIfDestroyers() {
    players();
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of(battleSite));
    unit1();
    unit2();
    makeAttackerFirstStrike(unit1Attachment);
    makeDestroyer(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify attacking firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and destroyers waiting to die")
  void attackingFirstStrikeNoWithdrawIfDestroyersWaitingToDie() {
    players();
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of(battleSite));
    unit1();
    unit2();
    unit3();
    makeAttackerFirstStrike(unit1Attachment);
    makeDestroyer(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit3))
            .defendingWaitingToDie(List.of(unit2))
            .build()
            .get();

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
                attacker.getName() + ATTACKER_WITHDRAW)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and defenseless transports with restricted casualties")
  void attackingFirstStrikeNoWithdrawIfDefenselessTransports() {
    players();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(true);
    makeAttackerFirstStrike(unit1Attachment);
    makeTransport(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps,
        is(
            List.of(
                REMOVE_UNESCORTED_TRANSPORTS,
                attacker.getName() + FIRST_STRIKE_UNITS_FIRE,
                defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES,
                REMOVE_SNEAK_ATTACK_CASUALTIES,
                defender.getName() + FIRE,
                attacker.getName() + SELECT_CASUALTIES,
                REMOVE_CASUALTIES)));
  }

  @Test
  @DisplayName(
      "Verify attacking firstStrike can withdraw when SUBMERSIBLE_SUBS is false "
          + "and defenseless transports with non restricted casualties")
  void attackingFirstStrikeWithdrawIfNonRestrictedDefenselessTransports() {
    players();
    when(getAttackerRetreatTerritories.get()).thenReturn(List.of(battleSite));
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    makeAttackerFirstStrike(unit1Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
    // it shouldn't even ask for the transportCapacity
    verify(unit1Attachment, times(0)).getTransportCapacity();
  }

  @Test
  @DisplayName("Verify defending firstStrike can withdraw when SUBMERSIBLE_SUBS is false")
  void defendingFirstStrikeWithdrawIfAble() {
    players();
    attackerRetreat();
    when(getEmptyOrFriendlySeaNeighbors.apply(any(), any())).thenReturn(List.of(battleSite));
    unit1();
    unit2();
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify defending firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and no retreat territories")
  void defendingFirstStrikeNoWithdrawIfEmptyTerritories() {
    players();
    attackerRetreat();
    when(getEmptyOrFriendlySeaNeighbors.apply(any(), any())).thenReturn(List.of());
    unit1();
    unit2();
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify defending firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and destroyers present")
  void defendingFirstStrikeNoWithdrawIfDestroyers() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    makeDestroyer(unit1Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

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
      "Verify defending firstStrike can't withdraw "
          + "when SUBMERSIBLE_SUBS is false and destroyers waiting to die")
  void defendingFirstStrikeNoWithdrawIfDestroyersWaitingToDie() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    makeDestroyer(unit3Attachment);
    makeDefenderFirstStrike(unit2Attachment);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .attackingWaitingToDie(List.of(unit3))
            .build()
            .get();

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
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can withdraw if a unit was not amphibious")
  void partialAmphibiousAttackCanWithdrawIfHasNonAmphibious() {
    players();
    unit1();
    unit2();
    unit3();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(false);
    when(unit1Attachment.getIsSea()).thenReturn(false);
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3Attachment.getIsAir()).thenReturn(false);
    when(unit3Attachment.getIsSea()).thenReturn(false);
    when(unit3.getWasAmphibious()).thenReturn(false);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify partial amphibious attack can not withdraw if all units were amphibious")
  void partialAmphibiousAttackCanNotWithdrawIfHasAllAmphibious() {
    players();
    unit1();
    unit2();
    unit3();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(false);
    when(unit1Attachment.getIsSea()).thenReturn(false);
    when(unit1.getWasAmphibious()).thenReturn(true);
    when(unit3Attachment.getIsAir()).thenReturn(false);
    when(unit3Attachment.getIsSea()).thenReturn(false);
    when(unit3.getWasAmphibious()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify partial amphibious attack can not withdraw "
          + "if partial amphibious withdrawal not allowed")
  void partialAmphibiousAttackCanNotWithdrawIfNotAllowed() {
    players();
    unit1();
    unit2();
    unit3();
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
    // should never check for amphibious units
    verify(unit1, times(0)).getWasAmphibious();
  }

  @Test
  @DisplayName("Verify partial amphibious attack can not withdraw if not amphibious")
  void partialAmphibiousAttackCanNotWithdrawIfNotAmphibious() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(false);
    when(unit3Attachment.getIsAir()).thenReturn(false);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if ww2v2 and amphibious")
  void attackingPlanesCanWithdrawWW2v2AndAmphibious() {
    players();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can withdraw "
          + "if attacker can partial amphibious retreat and amphibious")
  void attackingPlanesCanWithdrawPartialAmphibiousAndAmphibious() {
    players();
    unit1();
    unit2();
    unit3();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    when(unit3Attachment.getIsAir()).thenReturn(false);
    when(unit3Attachment.getIsSea()).thenReturn(false);
    when(unit3.getWasAmphibious()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can withdraw if attacker can retreat planes and amphibious")
  void attackingPlanesCanWithdrawPlanesRetreatAndAmphibious() {
    players();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(true)
            .build()
            .get();

    assertThat(
        steps, is(mergeSteps(basicFightSteps(), List.of(attacker.getName() + ATTACKER_WITHDRAW))));
  }

  @Test
  @DisplayName("Verify attacker planes can not withdraw if ww2v2 and not amphibious")
  void attackingPlanesCanNotWithdrawWW2v2AndNotAmphibious() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can not withdraw "
          + "if attacker can partial amphibious retreat and not amphibious")
  void attackingPlanesCanNotWithdrawPartialAmphibiousAndNotAmphibious() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    unit3();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(false);
    when(gameProperties.get(PARTIAL_AMPHIBIOUS_RETREAT, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1, unit3))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
  }

  @Test
  @DisplayName(
      "Verify attacker planes can not withdraw if attacker can retreat planes and not amphibious")
  void attackingPlanesCanNotWithdrawPlanesRetreatAndNotAmphibious() {
    players();
    attackerRetreat();
    unit1();
    unit2();
    when(gameProperties.get(SUB_RETREAT_BEFORE_BATTLE, false)).thenReturn(false);
    when(gameProperties.get(WW2V2, false)).thenReturn(false);
    when(gameProperties.get(DEFENDING_SUBS_SNEAK_ATTACK, false)).thenReturn(false);
    when(gameProperties.get(TRANSPORT_CASUALTIES_RESTRICTED, false)).thenReturn(false);
    when(gameProperties.get(ATTACKER_RETREAT_PLANES, false)).thenReturn(true);
    when(unit1Attachment.getIsAir()).thenReturn(true);
    final List<String> steps =
        getStepBuilder()
            .attackingUnits(List.of(unit1))
            .defendingUnits(List.of(unit2))
            .isBattleSiteWater(false)
            .isAmphibious(false)
            .build()
            .get();

    assertThat(steps, is(basicFightSteps()));
  }
}
