package org.triplea.ai.benchmark.tabulator;

import java.util.List;
import java.util.Map;
import org.triplea.ai.benchmark.tabulator.Stats.Difference;
import org.triplea.ai.benchmark.tabulator.Stats.Summary;

/**
 * The comparison of one or more runs, laid out like a baseball scoreboard: each kata seat is a game
 * ("box score"), each metric an inning, each run a team. The first run is the reference the others
 * are measured against.
 */
record Scorecard(int schema, List<RunInfo> runs, List<Box> series) {

  /** Where a run came from. */
  record RunInfo(String dir, String ai, String gitSha, String startedAt) {
    /** The AI and the commit it was built from, enough to tell runs of the same AI apart. */
    String label() {
      return ai + " @ " + gitSha.substring(0, Math.min(7, gitSha.length()));
    }
  }

  /** One kata seat, with one team per run in the order the runs were given. */
  record Box(
      String kata,
      String seat,
      String referenceAi,
      String primaryMetric,
      List<String> metrics,
      List<Team> teams,
      List<StepDetail> details) {}

  /**
   * One run's games on a kata seat. {@code games} counts only games that finished without a crash
   * or timeout. {@code difference} is this team minus the first team and empty for the first team.
   * Gaps are at each game's final snapshot.
   */
  record Team(
      String label,
      int games,
      int wins,
      int losses,
      int capped,
      int crashes,
      int timeouts,
      Map<String, Cell> cells,
      Map<String, Difference> difference,
      Map<String, List<Point>> trajectories) {}

  record Cell(Summary gap, Summary seat, Summary enemy) {}

  /** Mean gap at a turn index; later turns cover fewer games, so each point carries its n. */
  record Point(int turn, Summary gap) {}

  /**
   * What the seat's players did in one step, across games: the territories where the runs differ or
   * the outcome varied. Only present for kata that record detail.
   */
  record StepDetail(String step, List<TerritoryRow> territories) {}

  /** One territory's tally per run, in the order of {@link Box#teams()}. */
  record TerritoryRow(String territory, List<Tally> tallies) {}

  /**
   * One run's games at a territory after a step: the share ending seat-owned, and mean units on the
   * seat's side (total and by type) and on the enemy side.
   */
  record Tally(
      int n,
      double seatOwned,
      double seatUnits,
      double enemyUnits,
      Map<String, Double> seatUnitsByType) {}
}
