package org.triplea.ai.benchmark.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KataParserTest {
  private static final Path KATA_DIR = Path.of("/kata/tiny");
  private final KataParser parser = new KataParser(List.of("Hard", "Fast"), List.of("tuv", "pus"));

  @Test
  void minimalDescriptorGetsDefaults() {
    final Kata kata =
        parser.parse(
            KATA_DIR,
            """
            start: {map: game.xml}
            seats: [Axis]
            limits: {rounds: 3}
            """);

    assertThat(kata.name()).isEqualTo("tiny");
    assertThat(kata.start())
        .isEqualTo(new Kata.Start(Kata.StartKind.MAP, KATA_DIR.resolve("game.xml")));
    assertThat(kata.referenceAi()).isEqualTo("Hard");
    assertThat(kata.controlAi()).isEqualTo("Hard");
    assertThat(kata.limits()).isEqualTo(new Kata.Limits(3, 2, 20, 20, 30));
    assertThat(kata.metrics()).containsExactly("tuv", "pus");
    assertThat(kata.primaryMetric()).isEqualTo("tuv");
    assertThat(kata.props()).isEmpty();
  }

  @Test
  void fullDescriptorIsReadAsWritten() {
    final Kata kata =
        parser.parse(
            KATA_DIR,
            """
            name: renamed
            description: a save start
            start: {save: position.tsvg}
            seats: [Axis, Allies]
            reference_ai: Fast
            control_ai: Fast
            limits: {rounds: 5, min_games: 4, max_games: 8, budget_minutes: 10, game_timeout_minutes: 2}
            primary_metric: pus
            metrics: [pus]
            props: {Low Luck: true, Bid: 9}
            """);

    assertThat(kata.name()).isEqualTo("renamed");
    assertThat(kata.start().kind()).isEqualTo(Kata.StartKind.SAVE);
    assertThat(kata.seats()).containsExactly("Axis", "Allies");
    assertThat(kata.limits()).isEqualTo(new Kata.Limits(5, 4, 8, 10, 2));
    assertThat(kata.metrics()).containsExactly("pus");
    assertThat(kata.props()).isEqualTo(Map.of("Low Luck", "true", "Bid", "9"));
  }

  @Test
  void unknownKeyIsAnError() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: game.xml}
                    seats: [Axis]
                    limits: {rounds: 3, max_game: 4}
                    """))
        .hasMessageContaining("unknown key(s) [max_game] in limits");
  }

  @Test
  void unknownAiIsAnError() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: game.xml}
                    seats: [Axis]
                    control_ai: Hardest
                    limits: {rounds: 3}
                    """))
        .hasMessageContaining("control_ai: unknown 'Hardest'");
  }

  @Test
  void primaryMetricMustBeAmongTheKataMetrics() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: game.xml}
                    seats: [Axis]
                    limits: {rounds: 3}
                    metrics: [pus]
                    """))
        .hasMessageContaining("primary_metric: unknown 'tuv'");
  }

  @Test
  void startNeedsExactlyOneOfMapOrSave() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: game.xml, save: position.tsvg}
                    seats: [Axis]
                    limits: {rounds: 3}
                    """))
        .hasMessageContaining("exactly one of map or save");
  }

  @Test
  void startMustStayInsideTheKataFolder() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: ../other/game.xml}
                    seats: [Axis]
                    limits: {rounds: 3}
                    """))
        .hasMessageContaining("inside the kata folder");
  }

  @Test
  void minGamesAboveMaxGamesIsAnError() {
    assertThatThrownBy(
            () ->
                parser.parse(
                    KATA_DIR,
                    """
                    start: {map: game.xml}
                    seats: [Axis]
                    limits: {rounds: 3, min_games: 5, max_games: 4}
                    """))
        .hasMessageContaining("min_games is above limits.max_games");
  }
}
