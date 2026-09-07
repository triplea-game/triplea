package games.strategy.triplea.odds.calculator.context.model;

import java.util.List;

// TODO(phase0-decision): context-local aggregate, deliberately NOT the engine-flavored
// games.strategy.triplea.odds.calculator.AggregateResults (whose API speaks Unit/GameData). Decide
// whether the boundary conversion to that type happens in the adapter or the caller. Simple-name
// shadow of the engine type is intentional but a rename is on the table.
/** The batch result: every run's {@link BattleResult}; win%/TUV are derived by the caller. */
public record AggregateResults(List<BattleResult> results) {}
