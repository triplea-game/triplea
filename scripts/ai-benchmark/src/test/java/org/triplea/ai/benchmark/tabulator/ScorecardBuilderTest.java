package org.triplea.ai.benchmark.tabulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScorecardBuilderTest {
  @Test
  void boxScoreCountsFinishedGamesAndUsesFinalSnapshots() {
    final RunData run =
        run(
            "Hard",
            List.of(
                game("k/Axis/0", "ok", "Axis", false),
                game("k/Axis/1", "ok", null, true),
                game("k/Axis/2", "crash", null, false)),
            List.of(
                snapshot("k/Axis/0", 0, false, 5),
                snapshot("k/Axis/0", 1, true, 10),
                snapshot("k/Axis/1", 0, false, 7),
                snapshot("k/Axis/1", 1, true, 20),
                snapshot("k/Axis/2", 0, false, 999)));

    final Scorecard card = new ScorecardBuilder(List.of(run)).build();

    final Scorecard.Team team = card.series().get(0).teams().get(0);
    assertThat(team.games()).isEqualTo(2);
    assertThat(team.wins()).isEqualTo(1);
    assertThat(team.capped()).isEqualTo(1);
    assertThat(team.crashes()).isEqualTo(1);
    assertThat(team.cells().get("tuv").gap().mean()).isEqualTo(15.0);
    assertThat(team.difference()).isEmpty();
  }

  @Test
  void laterRunsAreDifferencesFromTheFirst() {
    final RunData reference =
        run(
            "Hard",
            List.of(game("k/Axis/0", "ok", "Axis", false)),
            List.of(snapshot("k/Axis/0", 1, true, 10)));
    final RunData second =
        run(
            "Hard Beta",
            List.of(game("k/Axis/0", "ok", "Allies", false)),
            List.of(snapshot("k/Axis/0", 1, true, -4)));
    final RunData third =
        run(
            "Fast",
            List.of(game("k/Axis/0", "ok", "Allies", false)),
            List.of(snapshot("k/Axis/0", 1, true, 12)));

    final Scorecard card = new ScorecardBuilder(List.of(reference, second, third)).build();

    final List<Scorecard.Team> teams = card.series().get(0).teams();
    assertThat(teams)
        .extracting(Scorecard.Team::label)
        .containsExactly("Hard @ abcdef1", "Hard Beta @ abcdef1", "Fast @ abcdef1");
    assertThat(teams.get(1).difference().get("tuv").mean()).isEqualTo(-14.0);
    assertThat(teams.get(2).difference().get("tuv").mean()).isEqualTo(2.0);
    assertThat(teams.get(1).losses()).isEqualTo(1);
  }

  @Test
  void aRunThatDidNotPlayTheSeatShowsNoGames() {
    final RunData reference =
        run(
            "Hard",
            List.of(game("k/Axis/0", "ok", "Axis", false)),
            List.of(snapshot("k/Axis/0", 1, true, 10)));
    final RunData empty = run("Hard Beta", List.of(), List.of());

    final Scorecard card = new ScorecardBuilder(List.of(reference, empty)).build();

    final Scorecard.Team team = card.series().get(0).teams().get(1);
    assertThat(team.games()).isZero();
    assertThat(team.difference().get("tuv").mean()).isNull();
  }

  @Test
  void aControlArmInAnOlderRunFolderIsIgnored() {
    final JsonObject control = game("k/Axis/control/0", "ok", "Axis", false);
    control.addProperty("arm", "control");
    final JsonObject experimental = game("k/Axis/experimental/0", "ok", "Allies", false);
    experimental.addProperty("arm", "experimental");
    final RunData run =
        run(
            "Hard Beta",
            List.of(control, experimental),
            List.of(
                snapshot("k/Axis/control/0", 1, true, 10),
                snapshot("k/Axis/experimental/0", 1, true, -4)));

    final Scorecard.Team team =
        new ScorecardBuilder(List.of(run)).build().series().get(0).teams().get(0);

    assertThat(team.games()).isEqualTo(1);
    assertThat(team.cells().get("tuv").gap().mean()).isEqualTo(-4.0);
  }

  @Test
  void trajectoryPointsCarryHowManyGamesReachedThem() {
    final RunData run =
        run(
            "Hard",
            List.of(game("k/Axis/0", "ok", null, true), game("k/Axis/1", "ok", "Axis", false)),
            List.of(
                snapshot("k/Axis/0", 0, false, 2),
                snapshot("k/Axis/0", 1, false, 4),
                snapshot("k/Axis/0", 2, true, 4),
                snapshot("k/Axis/1", 0, false, 6),
                snapshot("k/Axis/1", 1, true, 6)));

    final Scorecard card = new ScorecardBuilder(List.of(run)).build();

    final List<Scorecard.Point> points =
        card.series().get(0).teams().get(0).trajectories().get("tuv");
    assertThat(points).extracting(Scorecard.Point::turn).containsExactly(0, 1);
    assertThat(points.get(0).gap().n()).isEqualTo(2);
    assertThat(points.get(0).gap().mean()).isEqualTo(4.0);
    assertThat(points.get(1).gap().n()).isEqualTo(1);
  }

  private static RunData run(
      final String ai, final List<JsonObject> games, final List<JsonObject> snapshots) {
    return new RunData(
        "dir",
        json(
            String.format(
                """
                {"schema": 1, "ai": "%s", "git_sha": "abcdef1234", "started_at": "t0",
                 "kata": [{"name": "k", "seats": ["Axis"], "reference_ai": "Hard",
                           "primary_metric": "tuv", "metrics": ["tuv"]}]}
                """,
                ai)),
        games,
        snapshots,
        List.of());
  }

  private static JsonObject game(
      final String id, final String status, final String winner, final boolean capped) {
    final JsonObject game = new JsonObject();
    game.addProperty("kata", "k");
    game.addProperty("seat", "Axis");
    game.addProperty("game", id);
    game.addProperty("status", status);
    game.addProperty("winner", winner);
    game.addProperty("capped", capped);
    return game;
  }

  private static JsonObject snapshot(
      final String id, final int turn, final boolean last, final double gap) {
    return json(
        String.format(
            "{\"game\": \"%s\", \"turn_index\": %d, \"last\": %b,"
                + " \"metrics\": {\"tuv\": {\"seat\": %f, \"enemy\": 0, \"gap\": %f}}}",
            id, turn, last, gap, gap));
  }

  private static JsonObject json(final String text) {
    return JsonParser.parseString(text).getAsJsonObject();
  }
}
