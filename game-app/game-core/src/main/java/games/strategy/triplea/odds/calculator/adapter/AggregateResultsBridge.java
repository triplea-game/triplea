package games.strategy.triplea.odds.calculator.adapter;

import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.IBattle.WhoWon;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;

/**
 * The out-bound half of the anti-corruption layer: maps the calc core's {@link SimulationResults}
 * back onto the engine's {@link AggregateResults} so the existing win%/TUV accessors keep working.
 * Each core {@link BattleResult} becomes one {@link BattleResults}, its survivor {@code Force}s
 * rehydrated into representative {@code Unit}s via {@link GameDataBattleAdapter}. Lives outside the
 * engine-free core, alongside the in-bound {@link GameDataBattleAdapter}.
 */
public class AggregateResultsBridge {

  private final GameDataBattleAdapter adapter;

  public AggregateResultsBridge(final GameDataBattleAdapter adapter) {
    this.adapter = adapter;
  }

  public AggregateResults toAggregateResults(final SimulationResults results, final GameData data) {
    final AggregateResults aggregate = new AggregateResults(results.results().size());
    for (final BattleResult result : results.results()) {
      aggregate.addResult(
          new BattleResults(
              result.roundsFought(),
              adapter.toRepresentativeUnits(result.attackerSurvivors(), data),
              adapter.toRepresentativeUnits(result.defenderSurvivors(), data),
              whoWon(result.outcome()),
              data));
    }
    return aggregate;
  }

  private static WhoWon whoWon(final Outcome outcome) {
    return switch (outcome) {
      case ATTACKER_WINS -> WhoWon.ATTACKER;
      case DEFENDER_WINS -> WhoWon.DEFENDER;
      case DRAW -> WhoWon.DRAW;
    };
  }
}
