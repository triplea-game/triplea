package org.triplea.ai.benchmark.tabulator;

import javax.annotation.Nullable;
import org.triplea.ai.benchmark.tabulator.Scorecard.Box;
import org.triplea.ai.benchmark.tabulator.Scorecard.Cell;
import org.triplea.ai.benchmark.tabulator.Scorecard.Team;
import org.triplea.ai.benchmark.tabulator.Stats.Difference;
import org.triplea.ai.benchmark.tabulator.Stats.Summary;

/** Plain-text box scores, one per kata seat. */
final class TerminalRenderer {
  private static final int LABEL = 26;
  private static final int RECORD = 17;
  private static final int CELL = 18;

  private TerminalRenderer() {}

  static String render(final Scorecard card) {
    final StringBuilder out = new StringBuilder();
    out.append("Scorecard: ")
        .append(card.ai())
        .append(" @ ")
        .append(card.gitSha(), 0, Math.min(10, card.gitSha().length()))
        .append("  (cells: mean final gap ±95% CI; * CI excludes 0)\n");
    for (final Box box : card.series()) {
      out.append('\n').append(box(box));
    }
    return out.toString();
  }

  private static String box(final Box box) {
    final StringBuilder out = new StringBuilder();
    out.append(box.kata())
        .append(" · seat ")
        .append(box.seat())
        .append(" · reference ")
        .append(box.referenceAi())
        .append('\n');
    out.append(pad("", LABEL)).append(pad("games W-L-cap", RECORD));
    box.metrics()
        .forEach(m -> out.append(pad(m.equals(box.primaryMetric()) ? m + " (primary)" : m, CELL)));
    out.append('\n');
    teamRow(out, "control", box.control(), box);
    teamRow(out, "experimental", box.experimental(), box);
    out.append(pad("diff (exp - control)", LABEL)).append(pad("", RECORD));
    box.metrics().forEach(m -> out.append(pad(difference(box.difference().get(m)), CELL)));
    out.append('\n');
    out.append(pad("  seat / enemy", LABEL)).append('\n');
    absoluteRow(out, "control", box.control(), box);
    absoluteRow(out, "experimental", box.experimental(), box);
    return out.toString();
  }

  private static void teamRow(
      final StringBuilder out, final String arm, final Team team, final Box box) {
    out.append(pad(arm + " (" + team.ai() + ")", LABEL)).append(pad(record(team), RECORD));
    box.metrics().forEach(m -> out.append(pad(summary(team.cells().get(m).gap()), CELL)));
    out.append('\n');
  }

  private static void absoluteRow(
      final StringBuilder out, final String arm, final Team team, final Box box) {
    out.append(pad("  " + arm, LABEL)).append(pad("", RECORD));
    box.metrics()
        .forEach(
            m -> {
              final Cell cell = team.cells().get(m);
              out.append(
                  pad(number(cell.seat().mean()) + " / " + number(cell.enemy().mean()), CELL));
            });
    out.append('\n');
  }

  static String record(final Team team) {
    final String base =
        team.games() + "  " + team.wins() + "-" + team.losses() + "-" + team.capped();
    final int failed = team.crashes() + team.timeouts();
    return failed == 0 ? base : base + " +" + failed + "!";
  }

  static String summary(final Summary s) {
    if (s.mean() == null) {
      return "-";
    }
    return s.ci() == null ? number(s.mean()) : number(s.mean()) + " ±" + number(s.ci());
  }

  static String difference(final Difference d) {
    if (d.mean() == null) {
      return "-";
    }
    final String base =
        d.ci() == null ? number(d.mean()) : number(d.mean()) + " ±" + number(d.ci());
    return d.excludesZero() ? base + " *" : base;
  }

  static String number(@Nullable final Double value) {
    return value == null ? "-" : String.format("%.1f", value);
  }

  private static String pad(final String text, final int width) {
    return text.length() >= width ? text + " " : text + " ".repeat(width - text.length());
  }
}
