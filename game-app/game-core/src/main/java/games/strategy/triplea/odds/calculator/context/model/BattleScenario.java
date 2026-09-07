package games.strategy.triplea.odds.calculator.context.model;

import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import java.util.List;
import java.util.Map;

/**
 * A fully self-contained battle input: raw forces, rules, support, dependents, per-type cost, the
 * dice model, and the per-side preference seams. Nothing downstream reaches back into GameData.
 *
 * <p>{@code diceSides} and {@code lowLuck} are the battle-global dice model the {@link
 * games.strategy.triplea.odds.calculator.context.seam.HitRoller} reads through its {@code
 * FireContext}; they are first-class here rather than in {@link RulesProfile} because dice sides is
 * a numeric parameter, not a flag, and both must reach the roller as typed values in the hot path.
 */
public record BattleScenario(
    Force attackers,
    Force defenders,
    Force bombarding,
    Dependents dependents,
    RulesProfile rules,
    List<SupportRule> support,
    Map<UnitTypeId, Integer> cost,
    boolean amphibious,
    int diceSides,
    boolean lowLuck,
    RetreatPolicy attackerRetreat,
    RetreatPolicy defenderRetreat,
    CasualtyOrder attackerOrder,
    CasualtyOrder defenderOrder) {}
