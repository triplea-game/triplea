package org.triplea.ai.benchmark.tabulator;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import org.triplea.ai.benchmark.tabulator.Scorecard.Box;
import org.triplea.ai.benchmark.tabulator.Scorecard.Point;
import org.triplea.ai.benchmark.tabulator.Scorecard.StepDetail;
import org.triplea.ai.benchmark.tabulator.Scorecard.Tally;
import org.triplea.ai.benchmark.tabulator.Scorecard.Team;
import org.triplea.ai.benchmark.tabulator.Scorecard.TerritoryRow;

/** A single self-contained HTML page: box scores plus a gap-over-turns chart per metric. */
final class HtmlRenderer {
  private static final int WIDTH = 340;
  private static final int HEIGHT = 190;
  private static final int LEFT = 48;
  private static final int RIGHT = 12;
  private static final int TOP = 12;
  private static final int BOTTOM = 30;

  private static final String STYLE =
      """
      .viz-root { color-scheme: light; --surface-1:#fcfcfb; --surface-2:#f1f0ed;
        --text-primary:#0b0b0b; --text-secondary:#52514e; --text-muted:#7a7974;
        --grid:#e4e3df; --axis:#b9b8b2; --series-1:#2a78d6; --series-2:#eb6834;
        --series-3:#2a9d6f; --series-4:#8e5bd1; }
      @media (prefers-color-scheme: dark) { :root:where(:not([data-theme="light"])) .viz-root {
        color-scheme: dark; --surface-1:#1a1a19; --surface-2:#252524; --text-primary:#f0efec;
        --text-secondary:#c3c2b7; --text-muted:#929189; --grid:#33332f; --axis:#55544f;
        --series-1:#3987e5; --series-2:#d95926; --series-3:#34b585; --series-4:#a57be0; } }
      :root[data-theme="dark"] .viz-root { color-scheme: dark; --surface-1:#1a1a19;
        --surface-2:#252524; --text-primary:#f0efec; --text-secondary:#c3c2b7;
        --text-muted:#929189; --grid:#33332f; --axis:#55544f; --series-1:#3987e5;
        --series-2:#d95926; --series-3:#34b585; --series-4:#a57be0; }
      body { margin:0; }
      .viz-root { background:var(--surface-1); color:var(--text-primary); min-height:100vh;
        font:14px/1.45 system-ui, sans-serif; padding:24px 32px; }
      h1 { font-size:20px; margin:0 0 4px; } h3 { font-size:14px; margin:20px 0 6px; } h2 { font-size:16px; margin:32px 0 8px; }
      .muted { color:var(--text-secondary); }
      table { border-collapse:collapse; font-variant-numeric:tabular-nums; }
      th, td { padding:4px 12px; text-align:right; border-bottom:1px solid var(--grid); }
      th:first-child, td:first-child { text-align:left; }
      td small { display:block; color:var(--text-muted); }
      tr.diff td { font-weight:600; }
      .swatch { display:inline-block; width:10px; height:10px; border-radius:2px;
        margin-right:6px; vertical-align:baseline; }
      .legend { margin:12px 0 4px; color:var(--text-secondary); }
      .legend span { margin-right:16px; }
      .charts { display:flex; flex-wrap:wrap; gap:16px; }
      figure { margin:0; } figcaption { font-weight:600; margin-bottom:2px; }
      svg text { fill:var(--text-secondary); font-size:11px; }
      .grid { stroke:var(--grid); } .zero { stroke:var(--axis); }
      .c1 { stroke:var(--series-1); fill:var(--series-1); }
      .c2 { stroke:var(--series-2); fill:var(--series-2); }
      .c3 { stroke:var(--series-3); fill:var(--series-3); }
      .c4 { stroke:var(--series-4); fill:var(--series-4); }
      .line { fill:none; stroke-width:2; } .band { stroke:none; opacity:.15; }
      .dot { stroke:var(--surface-1); stroke-width:2; }
      .hit { fill:transparent; stroke:none; cursor:default; }
      details { margin-top:4px; color:var(--text-secondary); font-size:12px; }
      #tip { position:fixed; pointer-events:none; display:none; background:var(--surface-2);
        color:var(--text-primary); border:1px solid var(--grid); border-radius:4px;
        padding:4px 8px; font-size:12px; white-space:pre; }
      """;

  private static final String SCRIPT =
      """
      const tip = document.getElementById('tip');
      document.querySelectorAll('[data-tip]').forEach(el => {
        el.addEventListener('mousemove', e => {
          tip.textContent = el.dataset.tip; tip.style.display = 'block';
          tip.style.left = (e.clientX + 12) + 'px'; tip.style.top = (e.clientY + 12) + 'px';
        });
        el.addEventListener('mouseleave', () => tip.style.display = 'none');
      });
      """;

  private HtmlRenderer() {}

  static String render(final Scorecard card) {
    final StringBuilder out = new StringBuilder();
    out.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">")
        .append("<title>AI benchmark")
        .append("</title><style>")
        .append(STYLE)
        .append("</style></head><body><div class=\"viz-root\">");
    out.append("<h1>AI benchmark scorecard</h1><div class=\"muted\">");
    for (int i = 0; i < card.runs().size(); i++) {
      final Scorecard.RunInfo run = card.runs().get(i);
      out.append("run ")
          .append(i + 1)
          .append(": ")
          .append(esc(run.label()))
          .append(" · started ")
          .append(esc(run.startedAt()))
          .append("<br>");
    }
    out.append(
        "Each cell is the mean gap (seat minus enemy) at the end of the game, ±95% CI, with"
            + " seat / enemy means below it. Diff rows are against run 1; * marks a difference"
            + " whose CI excludes 0. Record is games, then wins-losses-capped.</div>");
    card.series().forEach(box -> out.append(box(box)));
    out.append("<div id=\"tip\"></div><script>")
        .append(SCRIPT)
        .append("</script></div></body></html>\n");
    return out.toString();
  }

  private static String box(final Box box) {
    final StringBuilder out = new StringBuilder();
    out.append("<h2>")
        .append(esc(box.kata()))
        .append(" · seat ")
        .append(esc(box.seat()))
        .append(" <span class=\"muted\">· reference ")
        .append(esc(box.referenceAi()))
        .append("</span></h2>");
    out.append("<table><thead><tr><th>team</th><th>record</th>");
    box.metrics()
        .forEach(
            m ->
                out.append("<th>")
                    .append(esc(m))
                    .append(m.equals(box.primaryMetric()) ? " (primary)" : "")
                    .append("</th>"));
    out.append("</tr></thead><tbody>");
    IntStream.range(0, box.teams().size())
        .forEach(i -> teamRow(out, color(i), box.teams().get(i), box));
    box.teams().stream().skip(1).forEach(team -> differenceRow(out, team, box));
    out.append("</tbody></table>");
    out.append("<div class=\"legend\">");
    IntStream.range(0, box.teams().size())
        .forEach(i -> out.append(legendItem(color(i), box.teams().get(i).label())));
    out.append("<span>mean gap by turn, band = 95% CI</span></div><div class=\"charts\">");
    box.metrics().forEach(m -> out.append(chart(m, box.teams())));
    out.append("</div>");
    box.details().forEach(detail -> out.append(detail(detail, box)));
    return out.toString();
  }

  /** Four series colors, reused beyond that. */
  private static String color(final int team) {
    return "series-" + (team % 4 + 1);
  }

  private static String detail(final StepDetail detail, final Box box) {
    final StringBuilder out = new StringBuilder();
    out.append("<h3>")
        .append(esc(detail.step()))
        .append("</h3><table><thead><tr><th>territory</th>");
    box.teams().forEach(team -> out.append("<th>").append(esc(team.label())).append("</th>"));
    out.append("</tr></thead><tbody>");
    detail.territories().forEach(row -> out.append(detailRow(row)));
    out.append("</tbody></table>");
    return out.toString();
  }

  private static String detailRow(final TerritoryRow row) {
    final StringBuilder out = new StringBuilder("<tr><td>" + esc(row.territory()) + "</td>");
    row.tallies().forEach(tally -> out.append(detailCell(tally)));
    return out.append("</tr>").toString();
  }

  private static String detailCell(final Tally tally) {
    final String types =
        tally.seatUnitsByType().entrySet().stream()
            .map(e -> String.format(Locale.ROOT, "%s %.1f", e.getKey(), e.getValue()))
            .collect(java.util.stream.Collectors.joining(", "));
    return "<td>" + esc(TerminalRenderer.tally(tally)) + "<small>" + esc(types) + "</small></td>";
  }

  private static void teamRow(
      final StringBuilder out, final String color, final Team team, final Box box) {
    out.append("<tr><td><span class=\"swatch\" style=\"background:var(--")
        .append(color)
        .append(")\"></span>")
        .append(esc(team.label()))
        .append("</td><td>")
        .append(esc(TerminalRenderer.record(team)))
        .append("</td>");
    box.metrics()
        .forEach(
            m -> {
              final var cell = team.cells().get(m);
              out.append("<td>")
                  .append(esc(TerminalRenderer.summary(cell.gap())))
                  .append("<small>")
                  .append(TerminalRenderer.number(cell.seat().mean()))
                  .append(" / ")
                  .append(TerminalRenderer.number(cell.enemy().mean()))
                  .append("</small></td>");
            });
    out.append("</tr>");
  }

  private static void differenceRow(final StringBuilder out, final Team team, final Box box) {
    out.append("<tr class=\"diff\"><td>diff ").append(esc(team.label())).append("</td><td></td>");
    box.metrics()
        .forEach(
            m ->
                out.append("<td>")
                    .append(esc(TerminalRenderer.difference(team.difference().get(m))))
                    .append("</td>"));
    out.append("</tr>");
  }

  private static String legendItem(final String color, final String label) {
    return "<span><span class=\"swatch\" style=\"background:var(--"
        + color
        + ")\"></span>"
        + esc(label)
        + "</span>";
  }

  private static String chart(final String metric, final List<Team> teams) {
    final List<Point> all =
        teams.stream().flatMap(team -> team.trajectories().get(metric).stream()).toList();
    if (all.isEmpty()) {
      return "<figure><figcaption>" + esc(metric) + "</figcaption>no turn data</figure>";
    }
    final int maxTurn = all.stream().mapToInt(Point::turn).max().orElse(0);
    double lo = 0;
    double hi = 0;
    for (final Point p : all) {
      final double ci = p.gap().ci() == null ? 0 : p.gap().ci();
      lo = Math.min(lo, p.gap().mean() - ci);
      hi = Math.max(hi, p.gap().mean() + ci);
    }
    if (hi == lo) {
      hi = lo + 1;
    }
    final Scale scale = new Scale(maxTurn, lo, hi);
    final StringBuilder svg = new StringBuilder();
    svg.append("<figure><figcaption>")
        .append(esc(metric))
        .append(" gap</figcaption><svg width=\"")
        .append(WIDTH)
        .append("\" height=\"")
        .append(HEIGHT)
        .append("\" role=\"img\" aria-label=\"")
        .append(esc(metric))
        .append(" gap by turn\">");
    for (final double tick : new double[] {lo, (lo + hi) / 2, hi}) {
      svg.append(
          String.format(
              Locale.ROOT,
              "<line class=\"grid\" x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\"/>"
                  + "<text x=\"%d\" y=\"%.1f\" text-anchor=\"end\">%s</text>",
              LEFT,
              WIDTH - RIGHT,
              scale.y(tick),
              scale.y(tick),
              LEFT - 6,
              scale.y(tick) + 4,
              TerminalRenderer.number(tick)));
    }
    svg.append(
        String.format(
            Locale.ROOT,
            "<line class=\"zero\" x1=\"%d\" x2=\"%d\" y1=\"%.1f\" y2=\"%.1f\"/>",
            LEFT,
            WIDTH - RIGHT,
            scale.y(0),
            scale.y(0)));
    final int step = Math.max(1, (maxTurn + 1) / 8);
    for (int t = 0; t <= maxTurn; t += step) {
      svg.append(
          String.format(
              Locale.ROOT,
              "<text x=\"%.1f\" y=\"%d\" text-anchor=\"middle\">%d</text>",
              scale.x(t),
              HEIGHT - BOTTOM + 14,
              t));
    }
    svg.append(
        String.format(
            Locale.ROOT,
            "<text x=\"%d\" y=\"%d\" text-anchor=\"middle\">turn</text>",
            LEFT + (WIDTH - LEFT - RIGHT) / 2,
            HEIGHT - 2));
    for (int i = 0; i < teams.size(); i++) {
      series(
          svg,
          "c" + (i % 4 + 1),
          teams.get(i).label(),
          teams.get(i).trajectories().get(metric),
          scale);
    }
    svg.append("</svg>").append(table(metric, teams)).append("</figure>");
    return svg.toString();
  }

  private static void series(
      final StringBuilder svg,
      final String css,
      final String arm,
      final List<Point> points,
      final Scale scale) {
    if (points.isEmpty()) {
      return;
    }
    final StringBuilder upper = new StringBuilder();
    final StringBuilder lower = new StringBuilder();
    final StringBuilder line = new StringBuilder();
    for (final Point p : points) {
      final double ci = p.gap().ci() == null ? 0 : p.gap().ci();
      upper.append(xy(scale.x(p.turn()), scale.y(p.gap().mean() + ci)));
      lower.insert(0, xy(scale.x(p.turn()), scale.y(p.gap().mean() - ci)));
      line.append(xy(scale.x(p.turn()), scale.y(p.gap().mean())));
    }
    svg.append("<polygon class=\"band ")
        .append(css)
        .append("\" points=\"")
        .append(upper)
        .append(lower)
        .append("\"/>");
    svg.append("<polyline class=\"line ")
        .append(css)
        .append("\" points=\"")
        .append(line)
        .append("\"/>");
    for (final Point p : points) {
      final double x = scale.x(p.turn());
      final double y = scale.y(p.gap().mean());
      final String tip =
          arm
              + "  turn "
              + p.turn()
              + "\ngap "
              + TerminalRenderer.summary(p.gap())
              + "  (n="
              + p.gap().n()
              + ")";
      svg.append(
          String.format(
              Locale.ROOT,
              "<circle class=\"dot %s\" cx=\"%.1f\" cy=\"%.1f\" r=\"4\"/>"
                  + "<circle class=\"hit\" cx=\"%.1f\" cy=\"%.1f\" r=\"10\" data-tip=\"%s\"/>",
              css,
              x,
              y,
              x,
              y,
              esc(tip)));
    }
  }

  private static String table(final String metric, final List<Team> teams) {
    final StringBuilder out =
        new StringBuilder("<details><summary>table</summary><table><thead><tr><th>turn</th>");
    teams.forEach(team -> out.append("<th>").append(esc(team.label())).append("</th><th>n</th>"));
    out.append("</tr></thead><tbody>");
    final int maxTurn =
        teams.stream()
            .flatMap(team -> team.trajectories().get(metric).stream())
            .mapToInt(Point::turn)
            .max()
            .orElse(-1);
    for (int t = 0; t <= maxTurn; t++) {
      out.append("<tr><td>").append(t).append("</td>");
      for (final Team team : teams) {
        cell(out, team.trajectories().get(metric), t);
      }
      out.append("</tr>");
    }
    return out.append("</tbody></table></details>").toString();
  }

  private static void cell(final StringBuilder out, final List<Point> points, final int turn) {
    points.stream()
        .filter(p -> p.turn() == turn)
        .findFirst()
        .ifPresentOrElse(
            p ->
                out.append("<td>")
                    .append(esc(TerminalRenderer.summary(p.gap())))
                    .append("</td><td>")
                    .append(p.gap().n())
                    .append("</td>"),
            () -> out.append("<td>-</td><td>0</td>"));
  }

  private static String xy(final double x, final double y) {
    return String.format(Locale.ROOT, "%.1f,%.1f ", x, y);
  }

  private static String esc(final String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("\n", "&#10;");
  }

  private record Scale(int maxTurn, double lo, double hi) {
    double x(final int turn) {
      final int span = Math.max(1, maxTurn);
      return LEFT + (double) turn / span * (WIDTH - LEFT - RIGHT);
    }

    double y(final double value) {
      return TOP + (hi - value) / (hi - lo) * (HEIGHT - TOP - BOTTOM);
    }
  }
}
