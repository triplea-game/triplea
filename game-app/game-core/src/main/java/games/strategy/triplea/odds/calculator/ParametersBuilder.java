package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.casualty.CasualtyOrderOfLosses;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.triplea.java.collections.IntegerMap;

/** this class is a necessary bridge between Kotlin and Lombok */
public class ParametersBuilder {

  @Nullable
  public static CasualtyOrderOfLosses.Parameters build(
      @NotNull final Collection<Unit> targetsToPickFrom,
      @NotNull final GamePlayer player,
      @Nullable final CombatValue combatValue,
      @NotNull final Territory location,
      @Nullable final IntegerMap<UnitType> costsForTuv,
      @NotNull final GameData gameData) {
    return CasualtyOrderOfLosses.Parameters.builder()
        .targetsToPickFrom(targetsToPickFrom)
        .player(player)
        .combatValue(combatValue)
        .battlesite(location)
        .costs(costsForTuv)
        .data(gameData)
        .build();
  }

  public static List<Unit> sortUnitsForCasualtiesWithSupport(
      final CasualtyOrderOfLosses.Parameters parameters) {
    return CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(parameters);
  }
}
