package org.triplea.ai.benchmark.tabulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScorecardBuilderTest {
  private static final JsonObject RUN =
      json(
          """
          {"schema": 1, "ai": "Hard Beta", "git_sha": "abc", "started_at": "t0",
           "kata": [{"name": "k", "seats": ["Axis"], "reference_ai": "Hard", "control_ai": "Hard",
                     "primary_metric": "tuv", "metrics": ["tuv"]}]}
          """);

  @Test
  void boxScoreCountsFinishedGamesAndUsesFinalSnapshots() {
    final List<JsonObject> games =
        List.of(
            game("k/Axis/control/0", "control", "ok", "Axis", false),
            game("k/Axis/control/1", "control", "ok", null, true),
            game("k/Axis/experimental/0", "experimental", "ok", "Allies", false),
            game("k/Axis/experimental/1", "experimental", "crash", null, false));
    final List<JsonObject> snapshots =
        List.of(
            snapshot("k/Axis/control/0", 0, false, 5),
            snapshot("k/Axis/control/0", 1, true, 10),
            snapshot("k/Axis/control/1", 0, false, 7),
            snapshot("k/Axis/control/1", 1, true, 20),
            snapshot("k/Axis/experimental/0", 0, false, -1),
            snapshot("k/Axis/experimental/0", 1, true, -4),
            snapshot("k/Axis/experimental/1", 0, false, 999));

    final Scorecard card = new ScorecardBuilder(RUN, games, snapshots).build();

    final Scorecard.Box box = card.series().get(0);
    assertThat(box.control().games()).isEqualTo(2);
    assertThat(box.control().wins()).isEqualTo(1);
    assertThat(box.control().capped()).isEqualTo(1);
    assertThat(box.control().cells().get("tuv").gap().mean()).isEqualTo(15.0);
    assertThat(box.experimental().games()).isEqualTo(1);
    assertThat(box.experimental().losses()).isEqualTo(1);
    assertThat(box.experimental().crashes()).isEqualTo(1);
    assertThat(box.experimental().cells().get("tuv").gap().mean()).isEqualTo(-4.0);
    assertThat(box.difference().get("tuv").mean()).isEqualTo(-19.0);
  }

  @Test
  void trajectoryPointsCarryHowManyGamesReachedThem() {
    final List<JsonObject> games =
        List.of(
            game("k/Axis/control/0", "control", "ok", null, true),
            game("k/Axis/control/1", "control", "ok", "Axis", false));
    final List<JsonObject> snapshots =
        List.of(
            snapshot("k/Axis/control/0", 0, false, 2),
            snapshot("k/Axis/control/0", 1, false, 4),
            snapshot("k/Axis/control/0", 2, true, 4),
            snapshot("k/Axis/control/1", 0, false, 6),
            snapshot("k/Axis/control/1", 1, true, 6));

    final Scorecard card = new ScorecardBuilder(RUN, games, snapshots).build();

    final List<Scorecard.Point> control = card.series().get(0).trajectories().get("tuv").control();
    assertThat(control).extracting(Scorecard.Point::turn).containsExactly(0, 1);
    assertThat(control.get(0).gap().n()).isEqualTo(2);
    assertThat(control.get(0).gap().mean()).isEqualTo(4.0);
    assertThat(control.get(1).gap().n()).isEqualTo(1);
  }

  private static JsonObject game(
      final String id,
      final String arm,
      final String status,
      final String winner,
      final boolean capped) {
    final JsonObject game = new JsonObject();
    game.addProperty("kata", "k");
    game.addProperty("seat", "Axis");
    game.addProperty("arm", arm);
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
