package games.strategy.triplea.oddscalc;

import java.util.Collection;

import com.google.common.base.Preconditions;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.TerritoryEffectHelper;

/**
 * Essentially a data class to wrap a large number of parameters and provide a builder interface.
 */
public class OddsCalculatorParameters {
  public final PlayerID attacker;
  public final PlayerID defender;
  public final Territory location;
  public final Collection<Unit> attacking;
  public final Collection<Unit> defending;
  public final Collection<Unit> bombarding;
  public final Collection<TerritoryEffect> territoryEffects;
  public int runCount;
  public final GameData gameData;
  public final boolean keepOneAttackingLandUnit;
  public final boolean amphibious;
  public final int retreatAfterRound;
  public final int retreatAfterXUnitsLeft;
  public final boolean retreatWhenOnlyAirLeft;
  public final String attackerOrderOfLosses;
  public final String defenderOrderOfLosses;

  private OddsCalculatorParameters(final Builder builder) {
    this.attacker = builder.attacker;
    this.defender = builder.defender;
    this.location = builder.location;
    this.attacking = builder.attacking;
    this.defending = builder.defending;
    this.bombarding = builder.bombarding;
    this.territoryEffects = builder.territoryEffects;
    this.runCount = builder.runCount;
    this.gameData = builder.gameData;
    this.keepOneAttackingLandUnit = builder.keepOneAttackingLandUnit;
    this.amphibious = builder.amphibious;
    this.retreatAfterRound = builder.retreatAfterRound;
    this.retreatAfterXUnitsLeft = builder.retreatAfterXUnitsLeft;
    this.retreatWhenOnlyAirLeft = builder.retreatWhenOnlyAirLeft;
    this.attackerOrderOfLosses = builder.attackerOrderOfLosses;
    this.defenderOrderOfLosses = builder.defenderOrderOfLosses;
  }

  public void setRunCount(final int runCount) {
    this.runCount = runCount;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns a value object builder.
   */
  public static final class Builder {
    private PlayerID attacker;
    private PlayerID defender;
    private Territory location;
    private Collection<Unit> attacking;
    private Collection<Unit> defending;
    private Collection<Unit> bombarding;
    private Collection<TerritoryEffect> territoryEffects;
    private int runCount;
    private GameData gameData;
    private boolean keepOneAttackingLandUnit;
    private boolean amphibious;
    private int retreatAfterRound;
    private int retreatAfterXUnitsLeft;
    private boolean retreatWhenOnlyAirLeft;
    private String attackerOrderOfLosses;
    private String defenderOrderOfLosses;

    private Builder() {}

    /**
     * Standard builder build method. Verifies that required fields were set.
     */
    public OddsCalculatorParameters build() {
      if (this.territoryEffects == null) {
        this.territoryEffects = TerritoryEffectHelper.getEffects(location);
      }
      Preconditions.checkNotNull(this.location);
      Preconditions.checkNotNull(this.attacker);
      Preconditions.checkNotNull(this.attacking);
      Preconditions.checkNotNull(this.defender);
      Preconditions.checkNotNull(this.defending);
      Preconditions.checkNotNull(this.gameData);

      return new OddsCalculatorParameters(this);
    }

    public Builder attacker(final PlayerID attacker) {
      this.attacker = attacker;
      return this;
    }

    public Builder defender(final PlayerID defender) {
      this.defender = defender;
      return this;
    }

    public Builder location(final Territory location) {
      this.location = location;
      return this;
    }

    public Builder attacking(final Collection<Unit> attacking) {
      this.attacking = attacking;
      return this;
    }

    public Builder defending(final Collection<Unit> defending) {
      this.defending = defending;
      return this;
    }

    public Builder bombarding(final Collection<Unit> bombarding) {
      this.bombarding = bombarding;
      return this;
    }

    public Builder territoryEffects(final Collection<TerritoryEffect> territoryEffects) {
      this.territoryEffects = territoryEffects;
      return this;
    }

    public Builder runCount(final int runCount) {
      this.runCount = runCount;
      return this;
    }

    public Builder gameData(final GameData gameData) {
      this.gameData = gameData;
      return this;
    }

    public Builder keepOneAttackingLandUnit(final boolean keepOneAttackingLandUnit) {
      this.keepOneAttackingLandUnit = keepOneAttackingLandUnit;
      return this;
    }

    public Builder amphibious(final boolean amphibious) {
      this.amphibious = amphibious;
      return this;
    }

    public Builder retreatAfterRound(final int retreatAfterRound) {
      this.retreatAfterRound = retreatAfterRound;
      return this;
    }

    public Builder retreatAfterXUnitsLeft(final int retreatAfterXUnitsLeft) {
      this.retreatAfterXUnitsLeft = retreatAfterXUnitsLeft;
      return this;
    }

    public Builder retreatWhenOnlyAirLeft(final boolean retreatWhenOnlyAirLeft) {
      this.retreatWhenOnlyAirLeft = retreatWhenOnlyAirLeft;
      return this;
    }

    public Builder attackerOrderOfLosses(final String attackerOrderOfLosses) {
      this.attackerOrderOfLosses = attackerOrderOfLosses;
      return this;
    }

    public Builder defenderOrderOfLosses(final String defenderOrderOfLosses) {
      this.defenderOrderOfLosses = defenderOrderOfLosses;
      return this;
    }
  }
}
