package org.triplea.ai.benchmark.tabulator;

import javax.annotation.Nullable;
import org.triplea.ai.benchmark.tabulator.Scorecard.Box;
import org.triplea.ai.benchmark.tabulator.Scorecard.Cell;
import org.triplea.ai.benchmark.tabulator.Scorecard.StepDetail;
import org.triplea.ai.benchmark.tabulator.Scorecard.Tally;
import org.triplea.ai.benchmark.tabulator.Scorecard.Team;
import org.triplea.ai.benchmark.tabulator.Stats.Difference;
import org.triplea.ai.benchmark.tabulator.Stats.Summary;

/** Plain-text box scores, one per kata seat. */
final class TerminalRenderer {
  private static final int LABEL = 30;
  private static final int RECORD = 17;
  private static final int CELL = 18;
  private static final int DETAIL_ROWS = 15;

  private TerminalRenderer() {}

  static String render(final Scorecard card) {
    final StringBuilder out = new StringBuilder();
    out.append("Scorecard (cells: mean final gap ±95% CI; diff rows are against run 1;")
        .append(" * CI excludes 0)\n");
    for (int i = 0; i < card.runs().size(); i++) {
      final Scorecard.RunInfo run = card.runs().get(i);
      out.append(String.format("  run %d: %s, started %s%n", i + 1, run.label(), run.startedAt()));
    }
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
    box.teams().forEach(team -> teamRow(out, team, box));
    box.teams().stream().skip(1).forEach(team -> differenceRow(out, team, box));
    out.append(pad("  seat / enemy", LABEL)).append('\n');
    box.teams().forEach(team -> absoluteRow(out, team, box));
    box.details().forEach(detail -> detail(out, detail, box));
    return out.toString();
  }

  private static void detail(final StringBuilder out, final StepDetail detail, final Box box) {
    out.append("\n  ")
        .append(detail.step())
        .append(" (seat-owned %, mean seat units; top ")
        .append(DETAIL_ROWS)
        .append(" by difference, full list in scorecard.json)\n");
    out.append("    ").append(pad("territory", LABEL));
    box.teams().forEach(team -> out.append(pad(team.label(), 34)));
    out.append('\n');
    detail.territories().stream()
        .limit(DETAIL_ROWS)
        .forEach(
            row -> {
              out.append("    ").append(pad(row.territory(), LABEL));
              row.tallies().forEach(tally -> out.append(pad(tally(tally), 34)));
              out.append('\n');
            });
  }

  static String tally(final Tally tally) {
    return String.format(
        "%3.0f%% %5.1f  n=%d", tally.seatOwned() * 100, tally.seatUnits(), tally.n());
  }

  private static void teamRow(final StringBuilder out, final Team team, final Box box) {
    out.append(pad(team.label(), LABEL)).append(pad(record(team), RECORD));
    box.metrics().forEach(m -> out.append(pad(summary(team.cells().get(m).gap()), CELL)));
    out.append('\n');
  }

  private static void differenceRow(final StringBuilder out, final Team team, final Box box) {
    out.append(pad("diff " + team.label(), LABEL)).append(pad("", RECORD));
    box.metrics().forEach(m -> out.append(pad(difference(team.difference().get(m)), CELL)));
    out.append('\n');
  }

  private static void absoluteRow(final StringBuilder out, final Team team, final Box box) {
    out.append(pad("  " + team.label(), LABEL)).append(pad("", RECORD));
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
