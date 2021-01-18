package games.strategy.engine.data.unit.ability;

import static games.strategy.triplea.Constants.DEFENDING_SUBS_SNEAK_ATTACK;
import static games.strategy.triplea.Constants.NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

    assertThat(
        "Unit has no AA abilities", getAbilities(BattlePhaseList.DEFAULT_AA_PHASE), is(empty()));
    assertThat(
        "Unit has no Bombard abilities",
        getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
        is(empty()));
    assertThat(
        "Unit has no First Strike abilities",
        getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
        is(empty()));
    assertThat(
        "Unit has no general fight abilities",
        getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
        is(empty()));
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

        assertThat(
            "Unit has no AA abilities",
            getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
            is(empty()));
        assertThat(
            "Unit has no Bombard abilities",
            getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
            is(empty()));
        assertThat(
            "Unit has no First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            is(empty()));
        assertThat(
            "Unit has attack abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "unitAbility is attached to the unit type",
            unitAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
        assertThat(
            "unitAbility is only for attacking",
            unitAbility.getSides(),
            is(List.of(BattleState.Side.OFFENSE)));
      }

      @Test
      void unitWithOnlyNormalDefenseAbilities() {
        unitAttachment.setDefense(1);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(
            "Unit has no AA abilities",
            getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
            is(empty()));
        assertThat(
            "Unit has no Bombard abilities",
            getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
            is(empty()));
        assertThat(
            "Unit has no First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            is(empty()));
        assertThat(
            "Unit has defense abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "unitAbility is attached to the unit type",
            unitAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
        assertThat(
            "unitAbility is only for defending",
            unitAbility.getSides(),
            is(List.of(BattleState.Side.DEFENSE)));
      }

      @Test
      void unitWithSuicideOnHit() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicideOnHit(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(
            "Unit has General abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "isSuicideOnHit translates into commitSuicideAfterSuccesfulHit on both sides",
            unitAbility.getCommitSuicideAfterSuccessfulHit(),
            is(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE)));
      }

      @Test
      void unitWithIsSuicideOnAttack() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicideOnAttack(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(
            "Unit has General abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "isSuicideOnAttack translates into commitSuicide on only offense",
            unitAbility.getCommitSuicide(),
            is(List.of(BattleState.Side.OFFENSE)));
      }

      @Test
      void unitWithIsSuicideOnDefense() {
        unitAttachment.setDefense(1);
        unitAttachment.setIsSuicideOnDefense(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(
            "Unit has General abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "isSuicideOnDefense translates into commitSuicide on only defense",
            unitAbility.getCommitSuicide(),
            is(List.of(BattleState.Side.DEFENSE)));
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

        assertThat(
            "Both unitTypes should have the same ability",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "unitAbility is attached to both of the unit types",
            unitAbility.getAttachedUnitTypes(),
            is(List.of(unitType, otherUnitType)));
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
        assertThat(
            "All non infrastructure unit types, including itself, are possible targets",
            unitAbility.getTargets(),
            is(List.of(unitType, otherUnitType)));
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
        assertThat(
            "The canNotTargetUnitType is listed in the units canNotTarget property so it should "
                + "not be in the target list",
            unitAbility.getTargets(),
            is(List.of(unitType, otherUnitType)));
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

        assertThat(
            "Both unitTypes have the same ability",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "The ability should be attached to both unit types",
            unitAbility.getAttachedUnitTypes(),
            is(List.of(unitType, otherUnitType)));
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

        assertThat(
            "Both unitTypes should have their own ability",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(2));

        assertThat(
            "Both unit abilities should have only 1 attached unit type",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                .filter(combatUnitAbility -> combatUnitAbility.getAttachedUnitTypes().size() == 1)
                .collect(Collectors.toList()),
            hasSize(2));

        assertThat(
            "One unit ability should be attached to unitType",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                .filter(
                    combatUnitAbility ->
                        combatUnitAbility.getAttachedUnitTypes().contains(unitType))
                .collect(Collectors.toList()),
            hasSize(1));

        assertThat(
            "One unit ability should be attached to otherUnitType",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).stream()
                .filter(
                    combatUnitAbility ->
                        combatUnitAbility.getAttachedUnitTypes().contains(otherUnitType))
                .collect(Collectors.toList()),
            hasSize(1));
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

        assertThat(
            "Unit has an ability without a destroyer present and another ability with the "
                + "destroyer present to allow it to hit the canNotTargetUnitType",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(2));

        assertThat(
            "Destroyer has convertUnitType",
            battlePhaseList.getConvertAbilities().get(player),
            hasSize(1));

        final ConvertUnitAbility convertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).iterator().next();

        assertThat(
            "Both of the abilities in the convertUnitAbility should be in the general phase",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasItems(convertUnitAbility.getFrom(), convertUnitAbility.getTo()));

        final CombatUnitAbility initialAbility = convertUnitAbility.getFrom();

        assertThat(
            "The initial ability should be attached to the unit type",
            initialAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
        assertThat(
            "The initial ability doesn't allow targeting the canNotTargetUnitType",
            initialAbility.getTargets(),
            is(List.of(unitType, destroyerUnitType)));

        final CombatUnitAbility finalAbility = convertUnitAbility.getTo();
        assertThat(
            "The final ability is not initially attached to the unit type",
            finalAbility.getAttachedUnitTypes(),
            is(empty()));
        assertThat(
            "The final ability does allow targeting the canNotTargetUnitType",
            finalAbility.getTargets(),
            is(List.of(unitType, canNotTargetUnitType, destroyerUnitType)));
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

        assertThat(
            "The initial ability doesn't allow targeting the canNotTargetUnitType and the "
                + "destroyerUnitType",
            initialAbility.getTargets(),
            is(List.of(unitType)));

        final CombatUnitAbility finalAbility = convertUnitAbility.getTo();
        assertThat(
            "The final ability does allow targeting the canNotTargetUnitType but the "
                + "destroyerUnitType is still not allowed",
            finalAbility.getTargets(),
            is(List.of(unitType, canNotTargetUnitType)));
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

        assertThat(
            "Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));
      }

      @Test
      void unitWithIsSuicide() {
        unitAttachment.setAttack(1);
        unitAttachment.setIsSuicide(true);

        UnitAbilityFactory.generate(
            playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

        assertThat(
            "isSuicide Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(
            "isSuicide translates into commitSuicide on both sides",
            unitAbility.getCommitSuicide(),
            is(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE)));
      }

      @Test
      void unitWithIsSub() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(
            "Unit has no AA abilities",
            getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
            is(empty()));
        assertThat(
            "Unit has no Bombard abilities",
            getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
            is(empty()));
        assertThat(
            "Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));
        assertThat(
            "Unit has no General abilities",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            is(empty()));
      }

      @Test
      void unitWithIsSubWithDefendingSubsSneakAttackTrue() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(
            "Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat("isSub doesn't allow return fire", unitAbility.isReturnFire(), is(false));
        assertThat(
            "isSub's ability is on both sides because DEFENDING_SUBS_SNEAK_ATTACK is true",
            unitAbility.getSides(),
            is(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE)));
        assertThat(
            "isSub's ability is attached to it",
            unitAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
      }

      @Test
      void unitWithIsSubWithDefendingSubsSneakAttackFalse() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, false);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(
            "Unit has First Strike abilities on offense",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));

        assertThat(
            "Unit has General abilities on defense",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility offenseUnitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat(
            "isSub's first strike ability doesn't allow return fire",
            offenseUnitAbility.isReturnFire(),
            is(false));
        assertThat(
            "isSub's first strike ability is only on offense side because "
                + "DEFENDING_SUBS_SNEAK_ATTACK is false",
            offenseUnitAbility.getSides(),
            is(List.of(BattleState.Side.OFFENSE)));
        assertThat(
            "isSub's first strike ability is attached to it",
            offenseUnitAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));

        final CombatUnitAbility defenseUnitAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "isSub's general ability does allow return fire",
            defenseUnitAbility.isReturnFire(),
            is(true));
        assertThat(
            "isSub's general ability is only on defense sides",
            defenseUnitAbility.getSides(),
            is(List.of(BattleState.Side.DEFENSE)));
        assertThat(
            "isSub's general ability is attached to it",
            defenseUnitAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
      }

      @Test
      void unitWithIsSubButIsDestroyerMissing() {
        unitAttachment.setIsSub(true);
        unitAttachment.setAttack(1);
        unitAttachment.setDefense(1);

        final GameProperties properties = new GameProperties(gameData);
        properties.set(DEFENDING_SUBS_SNEAK_ATTACK, true);

        UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, properties);

        assertThat(
            "Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));
        assertThat(
            "Unit has no General abilities because isDestroyer is not present",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            is(empty()));

        final CombatUnitAbility unitAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        assertThat("isSub doesn't allow return fire", unitAbility.isReturnFire(), is(false));
        assertThat(
            "isSub's ability is on both sides because DEFENDING_SUBS_SNEAK_ATTACK is true",
            unitAbility.getSides(),
            is(List.of(BattleState.Side.OFFENSE, BattleState.Side.DEFENSE)));

        assertThat(
            "isSub triggers isFirstStrike which generally needs a convert ability but there "
                + "isn't an isDestroyer unit type so the convert ability is not created.",
            battlePhaseList.getConvertAbilities().get(player),
            is(nullValue()));
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

        assertThat(
            "Unit has First Strike abilities",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(1));
        assertThat(
            "Unit has General abilities because the isDestroyer can convert its first strike "
                + "unitAbility",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(1));

        final CombatUnitAbility firstStrikeAbility =
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE).iterator().next();
        final CombatUnitAbility generalAbility =
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE).iterator().next();
        assertThat(
            "The first strike ability is attached to the isSub",
            firstStrikeAbility.getAttachedUnitTypes(),
            is(List.of(unitType)));
        assertThat(
            "The general ability is not attached to anything yet since it is attached when "
                + "the convert ability runs during the battle.",
            generalAbility.getAttachedUnitTypes(),
            is(empty()));

        assertThat(
            "A convert ability needs to exist to convert the first strike to general",
            battlePhaseList.getConvertAbilities().get(player),
            hasSize(1));
        final ConvertUnitAbility convertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).iterator().next();
        assertThat(convertUnitAbility.getFrom(), is(firstStrikeAbility));
        assertThat(convertUnitAbility.getTo(), is(generalAbility));
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

        assertThat(
            "Unit has a first strike ability without a friendly destroyer present and another "
                + "ability with a friendly destroyer present to allow it to hit the "
                + "canNotTargetUnitType",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasSize(2));

        assertThat(
            "When an enemy destroyer is present, the unit has a general ability without a "
                + "friendly destroyer present and another ability with a friendly destroyer "
                + "present to allow it to hit the canNotTargetUnitType",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasSize(2));

        assertThat(
            "Friendly destroyer has a convert ability to allow the sub to hit the canNotTarget "
                + "unit. Enemy destroyers have two convert abilities to negate the sub in either "
                + "case",
            battlePhaseList.getConvertAbilities().get(player),
            hasSize(3));

        final ConvertUnitAbility friendlyConvertUnitAbility =
            battlePhaseList.getConvertAbilities().get(player).stream()
                .filter(
                    unitAbility ->
                        unitAbility.getFactions().contains(ConvertUnitAbility.Faction.ALLIED))
                .findFirst()
                .orElseThrow();

        assertThat(
            "Both of the abilities in the friendlyConvertUnitAbility should be in the first "
                + "strike phase since the friendly isDestroyer doesn't negate the first strike",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasItems(friendlyConvertUnitAbility.getFrom(), friendlyConvertUnitAbility.getTo()));

        final List<ConvertUnitAbility> enemyUnitAbilities =
            battlePhaseList.getConvertAbilities().get(player).stream()
                .filter(
                    unitAbility ->
                        unitAbility.getFactions().contains(ConvertUnitAbility.Faction.ENEMY))
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

        assertThat(
            "The to unitAbilities in both of the convert are in the general phase",
            getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
            hasItems(
                enemyConvertUnitAbilityWhenFriendlyDestroyerNotPresent.getTo(),
                enemyConvertUnitAbilityWhenFriendlyDestroyerPresent.getTo()));

        assertThat(
            "The from unitAbilities in both of the convert are in the first strike phase",
            getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
            hasItems(
                enemyConvertUnitAbilityWhenFriendlyDestroyerNotPresent.getFrom(),
                enemyConvertUnitAbilityWhenFriendlyDestroyerPresent.getFrom()));
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

      assertThat(
          "Unit has offense AA abilities",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          hasSize(1));
      assertThat(
          "Unit has no Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          is(empty()));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
    }

    @Test
    void unitWithOnlyAaDefenseAbilities() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "Unit has defense AA abilities",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          hasSize(1));
      assertThat(
          "Unit has no Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          is(empty()));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
    }

    @Test
    void unitWithOnlyAaAbilitiesButNotForCombat() {
      unitAttachment.setOffensiveAttackAa(1);
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(false);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "Unit has not for AA combat so has no AA abilities",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          is(empty()));
      assertThat(
          "Unit has no Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          is(empty()));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
    }

    @Test
    void unitWithOnlyAaOffenseAbilitiesButNoRolls() {
      unitAttachment.setOffensiveAttackAa(1);
      unitAttachment.setMaxAaAttacks(0);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "Unit has no AA rolls so doesn't have an AA ability",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          is(empty()));
      assertThat(
          "Unit has no Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          is(empty()));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
    }

    @Test
    void unitWithOnlyAaDefenseAbilitiesButNoRolls() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setMaxAaAttacks(0);
      unitAttachment.setIsAaForCombatOnly(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "Unit has no AA rolls so doesn't have an AA ability",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          is(empty()));
      assertThat(
          "Unit has no Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          is(empty()));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
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
      assertThat(ability.getRound(), is(10));
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
      assertThat(ability.getTargets(), is(unitAttachment.getTargetsAa(unitTypeList)));
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

      assertThat(
          "Only one ability should be created since both units have the same typeAa",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          hasSize(1));

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(
          "Both of the unit types in the typeAa should be on this ability",
          ability.getAttachedUnitTypes(),
          is(List.of(unitType, otherUnitType)));
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

      assertThat(
          "Even though the targetsAa are different, both units have the same typeAa. So the engine "
              + "assumes that it was a typo and will create one ability using the targetsAa from "
              + "only one of the units. This targetsAa is generally the first one it sees.",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          hasSize(1));

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(
          "Both of the unit types in the typeAa should be on this ability",
          ability.getAttachedUnitTypes(),
          is(List.of(unitType, otherUnitType)));
      assertThat(
          "The unit ability should have just one of the targetsAa. In this test, that happens "
              + "to be the first set.",
          ability.getTargets(),
          is(targets1));
    }

    @Test
    void unitWithWillNotFire() {
      unitAttachment.setAttackAa(1);
      unitAttachment.setIsAaForCombatOnly(true);

      final UnitType preventsFiringUnitType = mock(UnitType.class);
      unitAttachment.setWillNotFireIfPresent(Set.of(preventsFiringUnitType));

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
              + "AA attack",
          battlePhaseList.getConvertAbilities().get(player),
          hasSize(1));
      final ConvertUnitAbility convertUnitAbility =
          battlePhaseList.getConvertAbilities().get(player).iterator().next();
      final CombatUnitAbility unitAbility =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(
          "The convert ability needs to reference the AA ability",
          convertUnitAbility.getFrom(),
          is(unitAbility));
      assertThat(
          "The convert ability is removing the AA ability so the To is EMPTY",
          convertUnitAbility.getTo(),
          is(CombatUnitAbility.EMPTY));
      assertThat(
          "The convert ability should be on the preventsFiringUnitType",
          convertUnitAbility.getAttachedUnitTypes(),
          is(List.of(preventsFiringUnitType)));
      assertThat(
          "The preventsFiringUnitType is preventing an enemy AA unit",
          convertUnitAbility.getFactions(),
          is(List.of(ConvertUnitAbility.Faction.ENEMY)));
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

      assertThat(
          "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
              + "AA attack. Only one is needed as both of the firing unit types have the same "
              + "typeAa",
          battlePhaseList.getConvertAbilities().get(player),
          hasSize(1));
      final ConvertUnitAbility convertUnitAbility =
          battlePhaseList.getConvertAbilities().get(player).iterator().next();
      final CombatUnitAbility unitAbility =
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE).iterator().next();
      assertThat(
          "The convert ability needs to reference the AA ability",
          convertUnitAbility.getFrom(),
          is(unitAbility));
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

      assertThat(
          "A convert ability needs to exist so that the preventsFiringUnitType can negate the "
              + "AA attack. Two are needed; one for each of the typeAa",
          battlePhaseList.getConvertAbilities().get(player),
          hasSize(2));
      final Collection<ConvertUnitAbility> convertUnitAbilities =
          battlePhaseList.getConvertAbilities().get(player);
      final List<CombatUnitAbility> unitAbilities =
          new ArrayList<>(getAbilities(BattlePhaseList.DEFAULT_AA_PHASE));

      assertThat(
          "One of the convert abilities needs to reference the first unit ability",
          convertUnitAbilities.stream()
              .filter(
                  convertUnitAbility -> convertUnitAbility.getFrom().equals(unitAbilities.get(0)))
              .collect(Collectors.toList()),
          hasSize(1));
      assertThat(
          "One of the convert abilities needs to reference the second unit ability",
          convertUnitAbilities.stream()
              .filter(
                  convertUnitAbility -> convertUnitAbility.getFrom().equals(unitAbilities.get(1)))
              .collect(Collectors.toList()),
          hasSize(1));
    }
  }

  @Nested
  class Bombard {

    @Test
    void unitWithOnlyBombardAbilities() {
      unitAttachment.setCanBombard(true);

      UnitAbilityFactory.generate(
          playerList, unitTypeList, battlePhaseList, new GameProperties(gameData));

      assertThat(
          "Unit has no AA rolls so doesn't have an AA ability",
          getAbilities(BattlePhaseList.DEFAULT_AA_PHASE),
          is(empty()));
      assertThat(
          "Unit has Bombard abilities",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          hasSize(1));
      assertThat(
          "Unit has no First Strike abilities",
          getAbilities(BattlePhaseList.DEFAULT_FIRST_STRIKE_PHASE),
          is(empty()));
      assertThat(
          "Unit has no General abilities",
          getAbilities(BattlePhaseList.DEFAULT_GENERAL_PHASE),
          is(empty()));
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

      assertThat(
          "Only one Bombard ability should be created",
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE),
          hasSize(1));

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(
          "Both unit types should be on the ability",
          ability.getAttachedUnitTypes(),
          is(List.of(unitType, unitType2)));
    }

    @Test
    void bombardReturnFireIsTrueIfPropertyIsTrue() {
      unitAttachment.setCanBombard(true);

      final GameProperties gameProperties = new GameProperties(gameData);
      gameProperties.set(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, true);

      UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, gameProperties);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(ability.isReturnFire(), is(true));
    }

    @Test
    void bombardReturnFireIsFalseIfPropertyIsFalse() {
      unitAttachment.setCanBombard(true);

      final GameProperties gameProperties = new GameProperties(gameData);
      gameProperties.set(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, false);

      UnitAbilityFactory.generate(playerList, unitTypeList, battlePhaseList, gameProperties);

      final CombatUnitAbility ability =
          getAbilities(BattlePhaseList.DEFAULT_BOMBARD_PHASE).iterator().next();
      assertThat(ability.isReturnFire(), is(false));
    }
  }
}
