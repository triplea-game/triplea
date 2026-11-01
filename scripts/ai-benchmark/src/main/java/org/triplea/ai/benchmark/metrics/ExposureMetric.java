package org.triplea.ai.benchmark.metrics;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritoryManager;
import games.strategy.triplea.ai.pro.util.ProBattleUtils;
import games.strategy.triplea.delegate.Matches;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Enemy attack power that can reach a target it would likely win: a territory of {@code players}
 * where the strongest enemy attack scores at least {@link #TARGET_STRENGTH} on Pro AI's quick
 * estimate. Each enemy unit counts once, however many targets it reaches. Lower is better: the
 * enemy has fewer units with something worth doing.
 *
 * <p>Reach comes from Pro AI's own model, which takes the strongest single enemy player per
 * territory, not combined alliance attacks.
 */
final class ExposureMetric implements Metric {
  /** On {@link ProBattleUtils#estimateStrengthDifference}, 50 is an even fight. */
  private static final double TARGET_STRENGTH = 60;

  private final String id;
  private final Predicate<Unit> counted;

  private ExposureMetric(final String id, final Predicate<Unit> counted) {
    this.id = id;
    this.counted = counted;
  }

  static ExposureMetric air() {
    return new ExposureMetric("exposure_air", Matches.unitIsAir());
  }

  static ExposureMetric land() {
    return new ExposureMetric("exposure_land", Matches.unitIsAir().negate());
  }

  @Override
  public String id() {
    return id;
  }

  @Override
  public double value(final GameData data, final Collection<GamePlayer> players) {
    final Set<Unit> attackers = new HashSet<>();
    for (final GamePlayer player : players) {
      attackers.addAll(attackersWithTargets(data, player));
    }
    return attackers.stream().filter(counted).mapToInt(ExposureMetric::attackPower).sum();
  }

  private static Set<Unit> attackersWithTargets(final GameData data, final GamePlayer player) {
    final var proData = new ProData();
    // The reach model never calls back into the AI, so no live ProAi is needed.
    proData.initializeSimulation(null, data, player);
    final var territoryManager = new ProTerritoryManager(null, proData);
    final List<Territory> held =
        data.getMap().getTerritories().stream()
            .filter(Matches.isTerritoryOwnedBy(player).or(Matches.territoryHasUnitsOwnedBy(player)))
            .toList();
    territoryManager.populateEnemyAttackOptions(List.of(), held);
    final Set<Unit> attackers = new HashSet<>();
    for (final Territory t : held) {
      final ProTerritory maxAttack = territoryManager.getEnemyAttackOptions().getMax(t);
      if (maxAttack == null) {
        continue;
      }
      final Set<Unit> units = new HashSet<>(maxAttack.getMaxUnits());
      units.addAll(maxAttack.getMaxAmphibUnits());
      final List<Unit> defenders = t.getUnitCollection().getMatches(Matches.alliedUnit(player));
      if (ProBattleUtils.estimateStrengthDifference(t, units, defenders) >= TARGET_STRENGTH) {
        attackers.addAll(units);
      }
    }
    return attackers;
  }

  private static int attackPower(final Unit unit) {
    final var attachment = unit.getUnitAttachment();
    return attachment.getAttack(unit.getOwner()) * attachment.getAttackRolls(unit.getOwner());
  }
}
