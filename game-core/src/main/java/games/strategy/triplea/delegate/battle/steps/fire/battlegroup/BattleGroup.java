package games.strategy.triplea.delegate.battle.steps.fire.battlegroup;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.battle.BattleState;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Value;

/**
 * TODO: In a battle group, the suicide/non suicide units need to be separated during the actual
 * battle, and not before
 */
@Value
@Builder
public class BattleGroup {

  /** What type of fire type will be used (AA, Normal, etc) */
  @Builder.Default BattleState.FireType fireType = BattleState.FireType.NORMAL;

  /** Can the offensive casualties return fire? */
  boolean casualtiesOnOffenseReturnFire;
  /**
   * Check the offensive units to determine if the offensive casualties return fire?
   *
   * <p>Uses the value of casualtiesOnOffenseReturnFire unless overridden
   */
  Predicate<Collection<Unit>> casualtiesOnOffenseReturnFirePredicate;
  /** Can the defensive casualties return fire? */
  boolean casualtiesOnDefenseReturnFire;
  /**
   * Check the defensive units to determine if the defensive casualties return fire
   *
   * <p>Uses the value of casualtiesOnDefenseReturnFire unless overridden
   */
  Predicate<Collection<Unit>> casualtiesOnDefenseReturnFirePredicate;
  /** Squadrons on the offense side */
  @Builder.Default Collection<FiringSquadron> offenseSquadrons = List.of();
  /** Squadrons on the defense side */
  @Builder.Default Collection<FiringSquadron> defenseSquadrons = List.of();

  public static List<BattleGroup> createBattleGroups(final GameData gameData) {
    return List.of(
        AaBattleGroups.create(gameData.getUnitTypeList(), gameData.getTechnologyFrontier()),
        BombardmentBattleGroups.create(gameData.getProperties()),
        createFirstStrikeBattleGroups(
            gameData.getUnitTypeList().getAllUnitTypes(), gameData.getProperties()),
        createGeneralBattleGroups(
            gameData.getUnitTypeList().getAllUnitTypes(), gameData.getProperties()));
  }

  private static BattleGroup createFirstStrikeBattleGroups(
      final Collection<UnitType> unitTypes, final GameProperties properties) {
    if (Properties.getWW2V2(properties)) {
      return NormalWW2V2BattleGroups.createFirstStrike(unitTypes, properties);
    } else {
      return NormalBattleGroups.createFirstStrike(unitTypes, properties);
    }
  }

  private static BattleGroup createGeneralBattleGroups(
      final Collection<UnitType> unitTypes, final GameProperties properties) {
    if (Properties.getWW2V2(properties)) {
      return NormalWW2V2BattleGroups.createGeneral(unitTypes, properties);
    } else {
      return NormalBattleGroups.createGeneral(unitTypes, properties);
    }
  }

  /**
   * Override the Lombok created Builder to ensure the casualtiesOn*ReturnFirePredicate return the
   * casualtiesOn*ReturnFire boolean by default.
   */
  public static class BattleGroupBuilder {
    private boolean casualtiesOnOffenseReturnFire = true;
    private Predicate<Collection<Unit>> casualtiesOnOffenseReturnFirePredicate = (units) -> true;
    private boolean casualtiesOnDefenseReturnFire = true;
    private Predicate<Collection<Unit>> casualtiesOnDefenseReturnFirePredicate = (units) -> true;

    public BattleGroupBuilder casualtiesOnOffenseReturnFire(
        final boolean casualtiesOnOffenseReturnFire) {
      this.casualtiesOnOffenseReturnFire = casualtiesOnOffenseReturnFire;
      this.casualtiesOnOffenseReturnFirePredicate = (units) -> casualtiesOnOffenseReturnFire;
      return this;
    }

    public BattleGroupBuilder casualtiesOnDefenseReturnFire(
        final boolean casualtiesOnDefenseReturnFire) {
      this.casualtiesOnDefenseReturnFire = casualtiesOnDefenseReturnFire;
      this.casualtiesOnDefenseReturnFirePredicate = (units) -> casualtiesOnDefenseReturnFire;
      return this;
    }

    public BattleGroupBuilder casualtiesOnOffenseReturnFirePredicate(
        final Predicate<Collection<Unit>> casualtiesOnOffenseReturnFirePredicate) {
      this.casualtiesOnOffenseReturnFirePredicate = casualtiesOnOffenseReturnFirePredicate;
      return this;
    }

    public BattleGroupBuilder casualtiesOnDefenseReturnFirePredicate(
        final Predicate<Collection<Unit>> casualtiesOnDefenseReturnFirePredicate) {
      this.casualtiesOnDefenseReturnFirePredicate = casualtiesOnDefenseReturnFirePredicate;
      return this;
    }
  }
}
