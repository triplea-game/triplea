package games.strategy.triplea.delegate.battle;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryEffectAttachment;
import java.util.Collection;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;

/** Resolves per-terrain battle round limits with existing game properties as fallbacks. */
public final class BattleRoundResolver {
  private BattleRoundResolver() {}

  /**
   * Resolves the round limit for a normal land battle.
   *
   * <p>Water battles retain the existing global sea-battle setting. When several territory effects
   * configure a land battle limit, the shortest finite limit wins. If every configured effect uses
   * {@code -1}, the battle is unlimited.
   */
  public static int resolveGroundBattleRounds(
      final Territory battleSite,
      final Collection<TerritoryEffect> territoryEffects,
      final GameData gameData) {
    Objects.requireNonNull(battleSite);
    Objects.requireNonNull(territoryEffects);
    Objects.requireNonNull(gameData);

    if (battleSite.isWater()) {
      return Properties.getSeaBattleRounds(gameData.getProperties());
    }
    return resolveOverride(
        territoryEffects,
        TerritoryEffectAttachment::getMaxGroundBattleRounds,
        Properties.getLandBattleRounds(gameData.getProperties()));
  }

  /** Resolves the round limit for an air battle at any territory. */
  public static int resolveAirBattleRounds(
      final Collection<TerritoryEffect> territoryEffects, final GameData gameData) {
    Objects.requireNonNull(territoryEffects);
    Objects.requireNonNull(gameData);

    return resolveOverride(
        territoryEffects,
        TerritoryEffectAttachment::getMaxAirBattleRounds,
        Properties.getAirBattleRounds(gameData.getProperties()));
  }

  private static int resolveOverride(
      final Collection<TerritoryEffect> territoryEffects,
      final Function<TerritoryEffectAttachment, OptionalInt> roundLimit,
      final int fallback) {
    boolean configured = false;
    int resolved = -1;
    for (final TerritoryEffect effect : territoryEffects) {
      final OptionalInt configuredRounds = roundLimit.apply(TerritoryEffectAttachment.get(effect));
      if (configuredRounds.isEmpty()) {
        continue;
      }
      configured = true;
      final int rounds = configuredRounds.getAsInt();
      if (rounds > 0 && (resolved < 0 || rounds < resolved)) {
        resolved = rounds;
      }
    }
    return configured ? resolved : fallback;
  }
}
