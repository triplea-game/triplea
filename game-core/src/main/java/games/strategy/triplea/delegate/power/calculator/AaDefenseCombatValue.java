package games.strategy.triplea.delegate.power.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import org.triplea.java.collections.IntegerMap;

/**
 * Calculates defense strength and roll for AA/Targeted dice
 *
 * <p>This takes into account friendly support, and enemy support
 */
@Builder
@Value
@Getter(AccessLevel.NONE)
class AaDefenseCombatValue implements CombatValue {

  @Getter(onMethod = @__({@Override}))
  @NonNull
  GameData gameData;

  @NonNull AvailableSupports supportFromFriends;
  @NonNull AvailableSupports supportFromEnemies;

  @Override
  public RollCalculator getRoll() {
    return new AaRoll(supportFromFriends, supportFromEnemies);
  }

  @Override
  public StrengthCalculator getStrength() {
    return new AaDefenseStrength(gameData, supportFromFriends, supportFromEnemies);
  }

  @Override
  public boolean isDefending() {
    return true;
  }

  static class AaDefenseStrength implements StrengthCalculator {

    private final GameData gameData;
    AvailableSupports supportFromFriends;
    AvailableSupports supportFromEnemies;
    UsedSupportTracker tracker = new UsedSupportTracker();

    AaDefenseStrength(
        final GameData gameData,
        final AvailableSupports supportFromFriends,
        final AvailableSupports supportFromEnemies) {
      this.gameData = gameData;
      this.supportFromFriends = supportFromFriends.filter(UnitSupportAttachment::getAaStrength);
      this.supportFromEnemies = supportFromEnemies.filter(UnitSupportAttachment::getAaStrength);
    }

    @Override
    public StrengthValue getStrength(final Unit unit) {
      return StrengthValue.of(
              gameData.getDiceSides(), unit.getUnitAttachment().getAttackAa(unit.getOwner()))
          .add(tracker.giveSupport(unit, supportFromFriends))
          .add(tracker.giveSupport(unit, supportFromEnemies));
    }

    @Override
    public Map<Unit, IntegerMap<Unit>> getSupportGiven() {
      return tracker.getSupportGiven();
    }
  }
}
