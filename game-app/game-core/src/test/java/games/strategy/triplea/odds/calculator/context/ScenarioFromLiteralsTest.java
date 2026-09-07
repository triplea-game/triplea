package games.strategy.triplea.odds.calculator.context;

import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.gives;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.land;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.multiHp;
import static games.strategy.triplea.odds.calculator.context.CombatProfileFixtures.receives;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.model.SupportCategory;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.reference.OolCasualtyOrder;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceRetreatPolicy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * North-star acceptance test (design §9): a battle described entirely by hand-written value
 * literals — no {@code GameData}, no map XML, no adapter — driven through the {@link
 * ReferenceBattleSimulator} to odds out. When this passes, the calc is proven isolated from the
 * engine: nothing here imports {@code games.strategy.engine.*} — even the dice come from the core's
 * own {@link FakeRandomSource}.
 *
 * <p>RED until Phase 1/2: every seam the simulator drives is still a throwing stub, so {@code
 * simulate} raises {@code UnsupportedOperationException}. The scenario construction itself is the
 * deliverable — it must stay compilable so the impl session has an executable target.
 */
class ScenarioFromLiteralsTest {

  private static final SupportCategory ARTILLERY_GIVES = new SupportCategory("gives:artillery");
  private static final SupportCategory ARTILLERY_RECEIVES =
      new SupportCategory("receives:artillery");

  /**
   * Under {@code alwaysHits} the attacker fields five firing units against two defenders and cannot
   * be annihilated (six hit-points, two incoming hits), so a correct simulator returns
   * ATTACKER_WINS with the defenders wiped — regardless of how the two casualties are ordered. The
   * assertion is deliberately allocation-robust; exact per-type survivors are pinned by the
   * differential harness, not here.
   *
   * <pre>
   * (1) build attackers = infantry x3 + artillery(gives support) + a 2-HP tank, all from literals
   * (2) build defenders = infantry x2 with a baked +1 defense (no Territory ref)
   * (3) drive one alwaysHits run through the reference simulator
   * (4) validate: attacker wins, defenders annihilated, attacker retains units
   * </pre>
   */
  @Test
  void handWrittenScenarioYieldsAnAttackerWinWithNoDefendersLeft() {
    final CombatProfile infantry = receives(land("infantry", 1, 2, 1), ARTILLERY_RECEIVES);
    final CombatProfile artillery = gives(land("artillery", 2, 2, 1), ARTILLERY_GIVES);

    // A 2-HP unit as a next-chain: full tank --hit--> damaged tank --hit--> dead (null).
    final CombatProfile tank = multiHp("tank", 3, 3, 2, Domain.LAND);

    final Force attackers =
        new Force(
            Map.of(
                new Key(infantry, Lifecycle.ACTIVE), 3,
                new Key(artillery, Lifecycle.ACTIVE), 1,
                new Key(tank, Lifecycle.ACTIVE), 1));

    // Defense 3 bakes a +1 territory bonus straight into the stat — the whole point of isolation.
    final CombatProfile defendingInfantry = land("infantry", 1, 3, 1);
    final Force defenders = new Force(Map.of(new Key(defendingInfantry, Lifecycle.ACTIVE), 2));

    final BattleScenario scenario =
        new BattleScenario(
            attackers,
            defenders,
            new Force(Map.of()),
            new Dependents(Map.of()),
            new RulesProfile(Map.of()),
            List.of(
                new SupportRule(
                    ARTILLERY_GIVES, ARTILLERY_RECEIVES, 1, true, 1, Side.OFFENSE, false)),
            Map.of(
                new UnitTypeId("infantry"), 3,
                new UnitTypeId("artillery"), 4,
                new UnitTypeId("tank"), 5),
            false,
            new ReferenceRetreatPolicy(-1, -1, false),
            new ReferenceRetreatPolicy(-1, -1, false),
            new OolCasualtyOrder(List.of()),
            new OolCasualtyOrder(List.of()));

    final SimulationResults results =
        new ReferenceBattleSimulator().simulate(scenario, 1, FakeRandomSource.alwaysHits());

    assertThat(results.results()).hasSize(1);
    final BattleResult only = results.results().get(0);
    assertThat(only.outcome()).isEqualTo(Outcome.ATTACKER_WINS);
    assertThat(totalUnits(only.defenderSurvivors())).isZero();
    assertThat(totalUnits(only.attackerSurvivors())).isPositive();
  }

  private static int totalUnits(final Force force) {
    return force.counts().values().stream().mapToInt(Integer::intValue).sum();
  }
}
