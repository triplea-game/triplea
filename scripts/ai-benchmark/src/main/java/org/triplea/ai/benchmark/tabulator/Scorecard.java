package org.triplea.ai.benchmark.tabulator;

import java.util.List;
import java.util.Map;
import org.triplea.ai.benchmark.tabulator.Stats.Difference;
import org.triplea.ai.benchmark.tabulator.Stats.Summary;

/**
 * The analysis of one run, laid out like a baseball scoreboard: each kata seat is a game ("box
 * score"), each metric an inning, control and experimental the two teams; the run is the series.
 */
record Scorecard(int schema, String ai, String gitSha, String startedAt, List<Box> series) {

  /** One kata seat. Gaps are at each game's final snapshot. */
  record Box(
      String kata,
      String seat,
      String referenceAi,
      String primaryMetric,
      List<String> metrics,
      Team control,
      Team experimental,
      Map<String, Difference> difference,
      Map<String, Trajectory> trajectories,
      List<StepDetail> details) {}

  /** One arm. {@code games} counts only games that finished without a crash or timeout. */
  record Team(
      String ai,
      int games,
      int wins,
      int losses,
      int capped,
      int crashes,
      int timeouts,
      Map<String, Cell> cells) {}

  record Cell(Summary gap, Summary seat, Summary enemy) {}

  /** Mean gap per turn index; later turns cover fewer games, so each point carries its n. */
  record Trajectory(List<Point> control, List<Point> experimental) {}

  record Point(int turn, Summary gap) {}

  /**
   * What the seat's players did in one step, across games: the territories where the arms differ or
   * the outcome varied. Only present for kata that record detail.
   */
  record StepDetail(String step, List<TerritoryRow> territories) {}

  record TerritoryRow(String territory, ArmTally control, ArmTally experimental) {}

  /**
   * One arm's games at a territory after a step: the share ending seat-owned, and mean units on the
   * seat's side (total and by type) and on the enemy side.
   */
  record ArmTally(
      int n,
      double seatOwned,
      double seatUnits,
      double enemyUnits,
      Map<String, Double> seatUnitsByType) {}
}
