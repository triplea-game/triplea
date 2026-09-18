package games.strategy.engine.data.unit.ability;

import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.battle.phase.BattlePhaseList;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.steps.MockGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnitAbilityFactoryTest {

  private GameData gameData;
  private PlayerList playerList;
  private GamePlayer player;
  private UnitTypeList unitTypeList;
  private UnitType unitType;
  private UnitAttachment unitAttachment;
  private BattlePhaseList battlePhaseList;

  @BeforeEach
  void setup() {
    gameData = MockGameData.givenGameData().withDiceSides(6).build();

    playerList = new PlayerList(gameData);
    player = new GamePlayer("player", gameData);
    playerList.addPlayerId(new GamePlayer("player", gameData));

    unitType = new UnitType("basic", gameData);
    unitAttachment = new UnitAttachment("basic", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);

    unitTypeList = new UnitTypeList(gameData);
    unitTypeList.addUnitType(unitType);

    battlePhaseList = new BattlePhaseList();
  }

  @Test
  void unitWithNoAbilities() {

    UnitAbilityFactory.generate(
        playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

    assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
        .as("Unit has no AA abilities")
        .isEmpty();
    assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
        .as("Unit has no Bombard abilities")
        .isEmpty();
    assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
        .as("Unit has no First Strike abilities")
        .isEmpty();
    assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
        .as("Unit has no general fight abilities")
        .isEmpty();
  }

  private Collection<CombatUnitAbility> getAbilities(final String phaseName) {
    return battlePhaseList.getPhase(phaseName).get().getAbilities(player);
  }

  @Nested
  class Normal {

    @Nested
    class NonFirstStrike {

      @Test
      void unitWithOnlyNormalOffenseAbilities() {
        unitAttachment.setAttack(1);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
            .as("Unit has no AA abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
            .as("Unit has no Bombard abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has no First Strike abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has attack abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getAttachedUnitTypes())
            .as("unitAbility is attached to the unit type")
            .isEqualTo(List.of(unitType));
        assertThat(unitAbility.getSides())
            .as("unitAbility is only for attacking")
            .isEqualTo(List.of(BattleState.Side.OFFENSE));
      }

      @Test
      void unitWithOnlyNormalDefenseAbilities() {
        unitAttachment.setDefense(1);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
            .as("Unit has no AA abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
            .as("Unit has no Bombard abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has no First Strike abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has defense abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getAttachedUnitTypes())
            .as("unitAbility is attached to the unit type")
            .isEqualTo(List.of(unitType));
        assertThat(unitAbility.getSides())
            .as("unitAbility is only for defending")
            .isEqualTo(List.of(BattleState.Side.DEFENSE));
      }

      @Test
      void unitWithSuicideOnHit() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicideOnHit(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has General abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getSuicide(BattleState.Side.OFFENSE))
            .as("isSuicideOnHit translates ONLY_ON_HIT for offense")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_HIT);
        assertThat(unitAbility.getSuicide(BattleState.Side.DEFENSE))
            .as("isSuicideOnHit translates ONLY_ON_HIT for defense")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_HIT);
      }

      @Test
      void unitWithIsSuicideOnAttack() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicideOnAttack(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has General abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getSuicide(BattleState.Side.OFFENSE))
            .as("isSuicideOnAttack translates into suicide on offense ALWAYS")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_FIRE);
        assertThat(unitAbility.getSuicide(BattleState.Side.DEFENSE))
            .as("isSuicideOnAttack should not cause suicide on defense to have anything")
            .isEqualTo(CombatUnitAbility.Suicide.NONE);
      }

      @Test
      void unitWithIsSuicideOnDefense() {
        unitAttachment.setDefense(1);
        unitAttachment.setIsSuicideOnDefense(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has General abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getSuicide(BattleState.Side.DEFENSE))
            .as("isSuicideOnDefense translates into suicide on defense ALWAYS")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_FIRE);
        assertThat(unitAbility.getSuicide(BattleState.Side.OFFENSE))
            .as("isSuicideOnDefense should not cause suicide on offense to have anything")
            .isEqualTo(CombatUnitAbility.Suicide.NONE);
      }

      @Test
      void unitWithSuicideOnHitAndSuicideOnAttackAndDefense() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicideOnHit(true);
        unitAttachment.setIsSuicideOnAttack(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getSuicide(BattleState.Side.OFFENSE))
            .as("isSuicideOnAttack supersedes isSuicideOnHit so offense is ALWAYS")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_FIRE);
        assertThat(unitAbility.getSuicide(BattleState.Side.DEFENSE))
            .as("isSuicideOnHit is by itself on defense so it is ONLY_ON_HIT")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_HIT);
      }

      @Test
      void twoUnitTypesWithSamePropertiesHaveCommonAbility() {
        unitAttachment.setAttack(1);

        final UnitType otherUnitType = new UnitType("other", gameData);
        final UnitAttachment otherUnitAttachment =
            new UnitAttachment("other", otherUnitType, gameData);
        otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
        otherUnitAttachment.setAttack(1);
        unitTypeList.addUnitType(otherUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Both unitTypes should have the same ability")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getAttachedUnitTypes())
            .as("unitAbility is attached to both of the unit types")
            .isEqualTo(List.of(unitType, otherUnitType));
      }

      @Test
      void unitTargetsAllButInfrastructure() {
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final UnitType infrastructureUnitType = new UnitType("infrastructure", gameData);
        final UnitAttachment infrastructureUnitAttachment =
            new UnitAttachment("infrastructure", infrastructureUnitType, gameData);
        infrastructureUnitType.addAttachment(UNIT_ATTACHMENT_NAME, infrastructureUnitAttachment);
        infrastructureUnitAttachment.setIsInfrastructure(true);
        unitTypeList.addUnitType(infrastructureUnitType);

        final UnitType otherUnitType = new UnitType("other", gameData);
        final UnitAttachment otherUnitAttachment =
            new UnitAttachment("other", otherUnitType, gameData);
        otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
        unitTypeList.addUnitType(otherUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getTargets())
            .as("All non infrastructure unit types, including itself, are possible targets")
            .isEqualTo(List.of(unitType, otherUnitType));
      }

      @Test
      void unitCanNotTarget() {
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final UnitType canNotTargetUnitType = new UnitType("canNotTarget", gameData);
        final UnitAttachment canNotTargetUnitAttachment =
            new UnitAttachment("canNotTarget", canNotTargetUnitType, gameData);
        canNotTargetUnitType.addAttachment(UNIT_ATTACHMENT_NAME, canNotTargetUnitAttachment);
        unitTypeList.addUnitType(canNotTargetUnitType);

        unitAttachment.setCanNotTarget(Set.of(canNotTargetUnitType));

        final UnitType otherUnitType = new UnitType("other", gameData);
        final UnitAttachment otherUnitAttachment =
            new UnitAttachment("other", otherUnitType, gameData);
        otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
        unitTypeList.addUnitType(otherUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getTargets())
            .as(
                "The canNotTargetUnitType is listed in the units canNotTarget property so it should "
                    + "not be in the target list")
            .isEqualTo(List.of(unitType, otherUnitType));
      }

      @Test
      void twoUnitTypesWithSameCanNotTargetHaveCommonAbility() {
        final UnitType canNotTarget1 = new UnitType("canNotTarget1", gameData);
        final UnitAttachment canNotTarget1Attachment =
            new UnitAttachment("canNotTarget1", canNotTarget1, gameData);
        canNotTarget1.addAttachment(UNIT_ATTACHMENT_NAME, canNotTarget1Attachment);
        unitTypeList.addUnitType(canNotTarget1);

        unitAttachment.setAttack(1);
        unitAttachment.setCanNotTarget(Set.of(canNotTarget1));

        final UnitType otherUnitType = new UnitType("other", gameData);
        final UnitAttachment otherUnitAttachment =
            new UnitAttachment("other", otherUnitType, gameData);
        otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
        otherUnitAttachment.setAttack(1);
        otherUnitAttachment.setCanNotTarget(Set.of(canNotTarget1));
        unitTypeList.addUnitType(otherUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Both unitTypes have the same ability")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(unitAbility.getAttachedUnitTypes())
            .as("The ability should be attached to both unit types")
            .isEqualTo(List.of(unitType, otherUnitType));
      }

      @Test
      void twoUnitTypesWithDifferentCanNotTargetGetDifferentAbilities() {
        final UnitType canNotTarget1 = new UnitType("canNotTarget1", gameData);
        final UnitAttachment canNotTarget1Attachment =
            new UnitAttachment("canNotTarget1", canNotTarget1, gameData);
        canNotTarget1.addAttachment(UNIT_ATTACHMENT_NAME, canNotTarget1Attachment);
        unitTypeList.addUnitType(canNotTarget1);

        final UnitType canNotTarget2 = new UnitType("canNotTarget2", gameData);
        final UnitAttachment canNotTarget2Attachment =
            new UnitAttachment("canNotTarget2", canNotTarget2, gameData);
        canNotTarget2.addAttachment(UNIT_ATTACHMENT_NAME, canNotTarget2Attachment);
        unitTypeList.addUnitType(canNotTarget2);

        unitAttachment.setAttack(1);
        unitAttachment.setCanNotTarget(Set.of(canNotTarget1));

        final UnitType otherUnitType = new UnitType("other", gameData);
        final UnitAttachment otherUnitAttachment =
            new UnitAttachment("other", otherUnitType, gameData);
        otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
        otherUnitAttachment.setAttack(1);
        otherUnitAttachment.setCanNotTarget(Set.of(canNotTarget2));
        unitTypeList.addUnitType(otherUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Both unitTypes should have their own ability")
            .hasSize(2);

        assertThat(
                getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                    .filter(
                        combatUnitAbility -> combatUnitAbility.getAttachedUnitTypes().size() == 1)
                    .collect(Collectors.toList()))
            .as("Both unit abilities should have only 1 attached unit type")
            .hasSize(2);

        assertThat(
                getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                    .filter(
                        combatUnitAbility ->
                            combatUnitAbility.getAttachedUnitTypes().contains(unitType))
                    .collect(Collectors.toList()))
            .as("One unit ability should be attached to unitType")
            .hasSize(1);

        assertThat(
                getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                    .filter(
                        combatUnitAbility ->
                            combatUnitAbility.getAttachedUnitTypes().contains(otherUnitType))
                    .collect(Collectors.toList()))
            .as("One unit ability should be attached to otherUnitType")
            .hasSize(1);
      }

      @Test
      void unitIsListedInCanNotBeTargetedBy() {
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final UnitType canNotTargetUnitType = new UnitType("canNotTarget", gameData);
        final UnitAttachment canNotTargetUnitAttachment =
            new UnitAttachment("canNotTarget", canNotTargetUnitType, gameData);
        canNotTargetUnitType.addAttachment(UNIT_ATTACHMENT_NAME, canNotTargetUnitAttachment);
        canNotTargetUnitAttachment.setCanNotBeTargetedBy(Set.of(unitType));
        unitTypeList.addUnitType(canNotTargetUnitType);

        final UnitType destroyerUnitType = new UnitType("destroyer", gameData);
        final UnitAttachment destroyerUnitAttachment =
            new UnitAttachment("destroyer", destroyerUnitType, gameData);
        destroyerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, destroyerUnitAttachment);
        destroyerUnitAttachment.setIsDestroyer(true);
        unitTypeList.addUnitType(destroyerUnitType);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as(
                "Unit has an ability without a destroyer present and another ability with the "
                    + "destroyer present to allow it to hit the canNotTargetUnitType")
            .hasSize(2);

        assertThat(battlePhaseList.getConvertAbilities().get(player))
            .as("Destroyer has convertUnitType")
            .hasSize(1);

        final ConvertUnitAbility convertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).iterator().next();

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Both of the abilities in the convertUnitAbility should be in the general phase")
            .contains(convertUnitAbility.getFrom(), convertUnitAbility.getTo());

        final CombatUnitAbility initialAbility = convertUnitAbility.getFrom();

        assertThat(initialAbility.getAttachedUnitTypes())
            .as("The initial ability should be attached to the unit type")
            .isEqualTo(List.of(unitType));
        assertThat(initialAbility.getTargets())
            .as("The initial ability doesn't allow targeting the canNotTargetUnitType")
            .isEqualTo(List.of(unitType, destroyerUnitType));

        final CombatUnitAbility finalAbility = convertUnitAbility.getTo();
        assertThat(finalAbility.getAttachedUnitTypes())
            .as("The final ability is not initially attached to the unit type")
            .isEmpty();
        assertThat(finalAbility.getTargets())
            .as("The final ability does allow targeting the canNotTargetUnitType")
            .isEqualTo(List.of(unitType, canNotTargetUnitType, destroyerUnitType));
      }

      @Test
      void unitIsListedInCanNotBeTargetedByAndHasCanNotTarget() {
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final UnitType canNotTargetUnitType = new UnitType("canNotTarget", gameData);
        final UnitAttachment canNotTargetUnitAttachment =
            new UnitAttachment("canNotTarget", canNotTargetUnitType, gameData);
        canNotTargetUnitType.addAttachment(UNIT_ATTACHMENT_NAME, canNotTargetUnitAttachment);
        canNotTargetUnitAttachment.setCanNotBeTargetedBy(Set.of(unitType));
        unitTypeList.addUnitType(canNotTargetUnitType);

        final UnitType destroyerUnitType = new UnitType("destroyer", gameData);
        final UnitAttachment destroyerUnitAttachment =
            new UnitAttachment("destroyer", destroyerUnitType, gameData);
        destroyerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, destroyerUnitAttachment);
        destroyerUnitAttachment.setIsDestroyer(true);
        unitTypeList.addUnitType(destroyerUnitType);
        unitAttachment.setCanNotTarget(Set.of(destroyerUnitType));

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        final ConvertUnitAbility convertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).iterator().next();

        final CombatUnitAbility initialAbility = convertUnitAbility.getFrom();

        assertThat(initialAbility.getTargets())
            .as(
                "The initial ability doesn't allow targeting the canNotTargetUnitType and the "
                    + "destroyerUnitType")
            .isEqualTo(List.of(unitType));

        final CombatUnitAbility finalAbility = convertUnitAbility.getTo();
        assertThat(finalAbility.getTargets())
            .as(
                "The final ability does allow targeting the canNotTargetUnitType but the "
                    + "destroyerUnitType is still not allowed")
            .isEqualTo(List.of(unitType, canNotTargetUnitType));
      }
    }

    @Nested
    class FirstStrike {

      @Test
      void unitWithIsFirstStrike() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsFirstStrike(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities")
            .hasSize(1);
      }

      @Test
      void unitWithIsSuicide() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicide(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("isSuicide Unit has First Strike abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(unitAbility.getSuicide(BattleState.Side.OFFENSE))
            .as("isSuicideOnHit translates ALWAYS for offense")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_FIRE);
        assertThat(unitAbility.getSuicide(BattleState.Side.DEFENSE))
            .as("isSuicideOnHit translates ALWAYS for defense")
            .isEqualTo(CombatUnitAbility.Suicide.AFTER_FIRE);
      }

      @Test
      void unitWithIsSub() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
            .as("Unit has no AA abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
            .as("Unit has no Bombard abilities")
            .isEmpty();
        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities")
            .hasSize(1);
        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has no General abilities")
            .isEmpty();
      }

      @Test
      void unitWithIsSubWithDefendingSubsSneakAttackTrue() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities")
            .hasSize(1);

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(unitAbility.isReturnFire()).as("isSub doesn't allow return fire").isFalse();
        assertThat(unitAbility.getSides())
            .as("isSub's ability is on both sides because DEFENDING_SUBS_SNEAK_ATTACK is true")
            .isEqualTo(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE));
        assertThat(unitAbility.getAttachedUnitTypes())
            .as("isSub's ability is attached to it")
            .isEqualTo(List.of(unitType));
      }

      @Test
      void unitWithIsSubWithDefendingSubsSneakAttackFalse() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, false);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities on offense")
            .hasSize(1);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has General abilities on defense")
            .hasSize(1);

        final CombatUnitAbility offenseUnitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(offenseUnitAbility.isReturnFire())
            .as("isSub's first strike ability doesn't allow return fire")
            .isFalse();
        assertThat(offenseUnitAbility.getSides())
            .as(
                "isSub's first strike ability is only on offense side because "
                    + "DEFENDING_SUBS_SNEAK_ATTACK is false")
            .isEqualTo(List.of(BattleState.Side.OFFENSE));
        assertThat(offenseUnitAbility.getAttachedUnitTypes())
            .as("isSub's first strike ability is attached to it")
            .isEqualTo(List.of(unitType));

        final CombatUnitAbility defenseUnitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(defenseUnitAbility.isReturnFire())
            .as("isSub's general ability does allow return fire")
            .isTrue();
        assertThat(defenseUnitAbility.getSides())
            .as("isSub's general ability is only on defense sides")
            .isEqualTo(List.of(BattleState.Side.DEFENSE));
        assertThat(defenseUnitAbility.getAttachedUnitTypes())
            .as("isSub's general ability is attached to it")
            .isEqualTo(List.of(unitType));
      }

      @Test
      void unitWithIsSubButIsDestroyerMissing() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities")
            .hasSize(1);
        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("Unit has no General abilities because isDestroyer is not present")
            .isEmpty();

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(unitAbility.isReturnFire()).as("isSub doesn't allow return fire").isFalse();
        assertThat(unitAbility.getSides())
            .as("isSub's ability is on both sides because DEFENDING_SUBS_SNEAK_ATTACK is true")
            .isEqualTo(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE));

        assertThat(battlePhaseList.getConvertAbilities().get(player))
            .as(
                "isSub triggers isFirstStrike which generally needs a convert ability but there "
                    + "isn't an isDestroyer unit type so the convert ability is not created.")
            .isNull();
      }

      @Test
      void unitWithIsSubAndIsDestroyerExists() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final UnitType destroyerUnitType = new UnitType("destroyer", gameData);
        final UnitAttachment destroyerUnitAttachment =
            new UnitAttachment("destroyer", destroyerUnitType, gameData);
        destroyerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, destroyerUnitAttachment);
        destroyerUnitAttachment.setIsDestroyer(true);
        unitTypeList.addUnitType(destroyerUnitType);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("Unit has First Strike abilities")
            .hasSize(1);
        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as(
                "Unit has General abilities because the isDestroyer can convert its first strike "
                    + "unitAbility")
            .hasSize(1);

        final CombatUnitAbility firstStrikeAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        final CombatUnitAbility generalAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(firstStrikeAbility.getAttachedUnitTypes())
            .as("The first strike ability is attached to the isSub")
            .isEqualTo(List.of(unitType));
        assertThat(generalAbility.getAttachedUnitTypes())
            .as(
                "The general ability is not attached to anything yet since it is attached when "
                    + "the convert ability runs during the battle.")
            .isEmpty();

        assertThat(battlePhaseList.getConvertAbilities().get(player))
            .as("A convert ability needs to exist to convert the first strike to general")
            .hasSize(1);
        final ConvertUnitAbility convertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).iterator().next();
        assertThat(convertUnitAbility.getFrom()).isEqualTo(firstStrikeAbility);
        assertThat(convertUnitAbility.getTo()).isEqualTo(generalAbility);
      }

      @Test
      void unitIsSubAndIsListedInCanNotBeTargetedBy() {
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);
        unitAttachment.setIsSub(true);

        final UnitType canNotTargetUnitType = new UnitType("canNotTarget", gameData);
        final UnitAttachment canNotTargetUnitAttachment =
            new UnitAttachment("canNotTarget", canNotTargetUnitType, gameData);
        canNotTargetUnitType.addAttachment(UNIT_ATTACHMENT_NAME, canNotTargetUnitAttachment);
        canNotTargetUnitAttachment.setCanNotBeTargetedBy(Set.of(unitType));
        unitTypeList.addUnitType(canNotTargetUnitType);

        final UnitType destroyerUnitType = new UnitType("destroyer", gameData);
        final UnitAttachment destroyerUnitAttachment =
            new UnitAttachment("destroyer", destroyerUnitType, gameData);
        destroyerUnitType.addAttachment(UNIT_ATTACHMENT_NAME, destroyerUnitAttachment);
        destroyerUnitAttachment.setIsDestroyer(true);
        unitTypeList.addUnitType(destroyerUnitType);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as(
                "Unit has a first strike ability without a friendly destroyer present and another "
                    + "ability with a friendly destroyer present to allow it to hit the "
                    + "canNotTargetUnitType")
            .hasSize(2);

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as(
                "When an enemy destroyer is present, the unit has a general ability without a "
                    + "friendly destroyer present and another ability with a friendly destroyer "
                    + "present to allow it to hit the canNotTargetUnitType")
            .hasSize(2);

        assertThat(battlePhaseList.getConvertAbilities().get(player))
            .as(
                "Friendly destroyer has a convert ability to allow the sub to hit the canNotTarget "
                    + "unit. Enemy destroyers have two convert abilities to negate the sub in either "
                    + "case")
            .hasSize(3);

        final ConvertUnitAbility friendlyConvertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).stream()
                .filter(
                    unitAbility ->
                        unitAbility.getTeams().contains(ConvertUnitAbility.Team.FRIENDLY))
                .findFirst()
                .orElseThrow();

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as(
                "Both of the abilities in the friendlyConvertUnitAbility should be in the first "
                    + "strike phase since the friendly isDestroyer doesn't negate the first strike")
            .contains(friendlyConvertUnitAbility.getFrom(), friendlyConvertUnitAbility.getTo());

        final List<ConvertUnitAbility> enemyUnitAbilities =
            battlePhaseList.getConvertAbilities().get(player).stream()
                .filter(unitAbility -> unitAbility.getTeams().contains(ConvertUnitAbility.Team.FOE))
                .collect(Collectors.toList());

        final ConvertUnitAbility enemyConvertUnitAbilityWhenFriendlyDestroyerPresent =
            enemyUnitAbilities.stream()
                .filter(
                    unitAbility ->
                        unitAbility.getFrom().getTargets().contains(canNotTargetUnitType))
                .findFirst()
                .orElseThrow();

        final ConvertUnitAbility enemyConvertUnitAbilityWhenFriendlyDestroyerNotPresent =
            enemyUnitAbilities.stream()
                .filter(
                    Predicate.not(
                        unitAbility ->
                            unitAbility.getFrom().getTargets().contains(canNotTargetUnitType)))
                .findFirst()
                .orElseThrow();

        assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
            .as("The to unitAbilities in both of the convert are in the general phase")
            .contains(
                enemyConvertUnitAbilityWhenFriendlyDestroyerNotPresent.getTo(),
                enemyConvertUnitAbilityWhenFriendlyDestroyerPresent.getTo());

        assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
            .as("The from unitAbilities in both of the convert are in the first strike phase")
            .contains(
                enemyConvertUnitAbilityWhenFriendlyDestroyerNotPresent.getFrom(),
                enemyConvertUnitAbilityWhenFriendlyDestroyerPresent.getFrom());
      }
    }
  }

  @Nested
  class Aa {

    @Test
    void unitWithOnlyAaOffenseAbilities() {
      unitAttachment.setOffensiveAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has offense AA abilities")
          .hasSize(1);
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has no Bombard abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void unitWithOnlyAaDefenseAbilities() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has defense AA abilities")
          .hasSize(1);
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has no Bombard abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void unitWithOnlyAaAbilitiesButNotForCombat() {
      unitAttachment.setOffensiveAttackAa(1);
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(false);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has not for AA combat so has no AA abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has no Bombard abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void unitWithOnlyAaOffenseAbilitiesButNoRolls() {
      unitAttachment.setOffensiveAttackAa(1);
      unitAttachment.setMaxAaAttacks(0);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has no AA rolls so doesn't have an AA ability")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has no Bombard abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void unitWithOnlyAaDefenseAbilitiesButNoRolls() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setMaxAaAttacks(0);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has no AA rolls so doesn't have an AA ability")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has no Bombard abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void unitWithMaxRounds() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setMaxRoundsAa(10);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(ability.getRound()).isEqualTo(10);
    }

    @Test
    void unitTargetsAa() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setTargetsAa(Set.of(mock(UnitType.class), mock(UnitType.class)));

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(ability.getTargets()).isEqualTo(unitAttachment.getTargetsAa(unitTypeList));
    }

    @Test
    void twoUnitsWithSameTargetsAa() {
      final Set<UnitType> targets = Set.of(mock(UnitType.class), mock(UnitType.class));
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setTargetsAa(targets);
      unitAttachment.setTypeAa("test");

      final UnitType otherUnitType = new UnitType("other", gameData);
      final UnitAttachment otherUnitAttachment =
          new UnitAttachment("other", otherUnitType, gameData);
      otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
      otherUnitAttachment.setAttackAa(1);
      otherUnitAttachment.setIsAaForCombatOnly(true);
      otherUnitAttachment.setTargetsAa(targets);
      otherUnitAttachment.setTypeAa("test");
      unitTypeList.addUnitType(otherUnitType);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Only one ability should be created since both units have the same typeAa")
          .hasSize(1);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(ability.getAttachedUnitTypes())
          .as("Both of the unit types in the typeAa should be on this ability")
          .isEqualTo(List.of(unitType, otherUnitType));
    }

    @Test
    void twoUnitsWithSameTypeAaButDifferentTargetsAa() {
      final String typeAa = "TypeAA";
      final Set<UnitType> targets1 = Set.of(mock(UnitType.class), mock(UnitType.class));
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setTargetsAa(targets1);
      unitAttachment.setTypeAa(typeAa);

      final UnitType otherUnitType = new UnitType("other", gameData);
      final UnitAttachment otherUnitAttachment =
          new UnitAttachment("other", otherUnitType, gameData);
      otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
      final Set<UnitType> targets2 = Set.of(mock(UnitType.class), mock(UnitType.class));
      otherUnitAttachment.setAttackAa(1);
      otherUnitAttachment.setIsAaForCombatOnly(true);
      otherUnitAttachment.setTargetsAa(targets2);
      otherUnitAttachment.setTypeAa(typeAa);
      unitTypeList.addUnitType(otherUnitType);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as(
              "Even though the targetsAa are different, both units have the same typeAa. So the engine "
                  + "assumes that it was a typo and will create one ability using the targetsAa from "
                  + "only one of the units. This targetsAa is generally the first one it sees.")
          .hasSize(1);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(ability.getAttachedUnitTypes())
          .as("Both of the unit types in the typeAa should be on this ability")
          .isEqualTo(List.of(unitType, otherUnitType));
      assertThat(ability.getTargets())
          .as(
              "The unit ability should have just one of the targetsAa. In this test, that happens "
                  + "to be the first set.")
          .isEqualTo(targets1);
    }

    @Test
    void unitWithWillNotFire() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);

      final UnitType preventsFiringUnitType = mock(UnitType.class);
      unitAttachment.setWillNotFireIfPresent(Set.of(preventsFiringUnitType));

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(battlePhaseList.getConvertAbilities().get(player))
          .as(
              "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
                  + "AA attack")
          .hasSize(1);
      final ConvertUnitAbility convertUnitAbility =
          battlePhaseList.getConvertAbilities().get(player).iterator().next();
      final CombatUnitAbility unitAbility =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(convertUnitAbility.getFrom())
          .as("The convert ability needs to reference the AA ability")
          .isEqualTo(unitAbility);
      assertThat(convertUnitAbility.getTo())
          .as("The convert ability is removing the AA ability so the To is EMPTY")
          .isEqualTo(CombatUnitAbility.EMPTY);
      assertThat(convertUnitAbility.getAttachedUnitTypes())
          .as("The convert ability should be on the preventsFiringUnitType")
          .isEqualTo(List.of(preventsFiringUnitType));
      assertThat(convertUnitAbility.getTeams())
          .as("The preventsFiringUnitType is preventing an enemy AA unit")
          .isEqualTo(List.of(ConvertUnitAbility.Team.FOE));
    }

    @Test
    void twoUnitWithWillNotFireInTheSameTypeAa() {

      final Set<UnitType> preventsFiringUnitTypes = Set.of(mock(UnitType.class));
      final Set<UnitType> targets = Set.of(mock(UnitType.class), mock(UnitType.class));

      unitAttachment.setWillNotFireIfPresent(preventsFiringUnitTypes);
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setTargetsAa(targets);
      unitAttachment.setTypeAa("test");

      final UnitType otherUnitType = new UnitType("other", gameData);
      final UnitAttachment otherUnitAttachment =
          new UnitAttachment("other", otherUnitType, gameData);
      otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
      otherUnitAttachment.setWillNotFireIfPresent(preventsFiringUnitTypes);
      otherUnitAttachment.setAttackAa(1);
      otherUnitAttachment.setIsAaForCombatOnly(true);
      otherUnitAttachment.setTargetsAa(targets);
      otherUnitAttachment.setTypeAa("test");
      unitTypeList.addUnitType(otherUnitType);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(battlePhaseList.getConvertAbilities().get(player))
          .as(
              "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
                  + "AA attack. Only one is needed as both of the firing unit types have the same "
                  + "typeAa")
          .hasSize(1);
      final ConvertUnitAbility convertUnitAbility =
          battlePhaseList.getConvertAbilities().get(player).iterator().next();
      final CombatUnitAbility unitAbility =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(convertUnitAbility.getFrom())
          .as("The convert ability needs to reference the AA ability")
          .isEqualTo(unitAbility);
    }

    @Test
    void twoUnitWithWillNotFireInDifferentTypeAa() {

      final Set<UnitType> preventsFiringUnitTypes = Set.of(mock(UnitType.class));

      unitAttachment.setWillNotFireIfPresent(preventsFiringUnitTypes);
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);
      unitAttachment.setTargetsAa(Set.of(mock(UnitType.class)));
      unitAttachment.setTypeAa("test");

      final UnitType otherUnitType = new UnitType("other", gameData);
      final UnitAttachment otherUnitAttachment =
          new UnitAttachment("other", otherUnitType, gameData);
      otherUnitType.addAttachment(UNIT_ATTACHMENT_NAME, otherUnitAttachment);
      otherUnitAttachment.setWillNotFireIfPresent(preventsFiringUnitTypes);
      otherUnitAttachment.setAttackAa(1);
      otherUnitAttachment.setIsAaForCombatOnly(true);
      otherUnitAttachment.setTargetsAa(Set.of(mock(UnitType.class)));
      otherUnitAttachment.setTypeAa("test2");
      unitTypeList.addUnitType(otherUnitType);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(battlePhaseList.getConvertAbilities().get(player))
          .as(
              "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
                  + "AA attack. Two are needed; one for each of the typeAa")
          .hasSize(2);
      final Collection<ConvertUnitAbility> convertUnitAbilities =
          battlePhaseList.getConvertAbilities().get(player);
      final List<CombatUnitAbility> unitAbilities =
          new ArrayList<>(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE));

      assertThat(
              convertUnitAbilities.stream()
                  .filter(
                      convertUnitAbility ->
                          convertUnitAbility.getFrom().equals(unitAbilities.get(0)))
                  .collect(Collectors.toList()))
          .as("One of the convert abilities needs to reference the first unit ability")
          .hasSize(1);
      assertThat(
              convertUnitAbilities.stream()
                  .filter(
                      convertUnitAbility ->
                          convertUnitAbility.getFrom().equals(unitAbilities.get(1)))
                  .collect(Collectors.toList()))
          .as("One of the convert abilities needs to reference the second unit ability")
          .hasSize(1);
    }
  }

  @Nested
  class Bombard {

    @Test
    void unitWithOnlyBombardAbilities() {
      unitAttachment.setCanBombard(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE))
          .as("Unit has no AA rolls so doesn't have an AA ability")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Unit has Bombard abilities")
          .hasSize(1);
      assertThat(getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE))
          .as("Unit has no First Strike abilities")
          .isEmpty();
      assertThat(getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE))
          .as("Unit has no General abilities")
          .isEmpty();
    }

    @Test
    void twoUnitsWithBombardMakeOnlyOneAbility() {
      unitAttachment.setCanBombard(true);

      final UnitType unitType2 = new UnitType("other", gameData);
      final UnitAttachment unitAttachment2 = new UnitAttachment("other", unitType2, gameData);
      unitType2.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment2);
      unitAttachment2.setCanBombard(true);

      unitTypeList.addUnitType(unitType2);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE))
          .as("Only one Bombard ability should be created")
          .hasSize(1);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(ability.getAttachedUnitTypes())
          .as("Both unit types should be on the ability")
          .isEqualTo(List.of(unitType, unitType2));
    }

    @Test
    void bombardReturnFireIsTrueIfPropertyIsTrue() {
      unitAttachment.setCanBombard(true);

      final GameProperties gameProperties = new GameProperties(gameData);
      gameProperties.set(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, true);

      UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, gameProperties);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(ability.isReturnFire()).isTrue();
    }

    @Test
    void bombardReturnFireIsFalseIfPropertyIsFalse() {
      unitAttachment.setCanBombard(true);

      final GameProperties gameProperties = new GameProperties(gameData);
      gameProperties.set(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, false);

      UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, gameProperties);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(ability.isReturnFire()).isFalse();
    }
  }
}
