package games.strategy.triplea.odds.calculator.context.model;

import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import java.util.List;
import java.util.Map;

/**
 * A fully self-contained battle input: raw forces, rules, support, dependents, per-type cost, and
 * the per-side preference seams. Nothing downstream reaches back into GameData.
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
    RetreatPolicy attackerRetreat,
    RetreatPolicy defenderRetreat,
    CasualtyOrder attackerOrder,
    CasualtyOrder defenderOrder) {}
