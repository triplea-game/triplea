package org.triplea.ai.benchmark.tabulator;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.ai.benchmark.tabulator.Scorecard.StepDetail;

class DetailBuilderTest {
  @Test
  void talliesOwnershipAndUnitsPerArmAndKeepsOnlyTerritoriesThatDiffer() {
    final List<JsonObject> control =
        List.of(
            step("Ukraine", true, 3, "Poland", false, 0),
            step("Ukraine", false, 0, "Poland", false, 0));
    final List<JsonObject> experimental =
        List.of(
            step("Ukraine", true, 3, "Poland", false, 0),
            step("Ukraine", true, 5, "Poland", false, 0));

    final List<StepDetail> details = DetailBuilder.build(control, experimental);

    assertThat(details).hasSize(1);
    assertThat(details.get(0).step()).isEqualTo("round 1 russiansCombatMove");
    final var rows = details.get(0).territories();
    assertThat(rows).extracting(Scorecard.TerritoryRow::territory).containsExactly("Ukraine");
    assertThat(rows.get(0).control().seatOwned()).isEqualTo(0.5);
    assertThat(rows.get(0).control().seatUnits()).isEqualTo(1.5);
    assertThat(rows.get(0).experimental().seatOwned()).isEqualTo(1.0);
    assertThat(rows.get(0).experimental().seatUnitsByType()).containsEntry("infantry", 4.0);
  }

  private static JsonObject step(
      final String a,
      final boolean aOwned,
      final int aUnits,
      final String b,
      final boolean bOwned,
      final int bUnits) {
    return JsonParser.parseString(
            String.format(
                "{\"round\": 1, \"step\": \"russiansCombatMove\", \"territories\": {"
                    + "\"%s\": {\"o\": \"x\", \"s\": %b, \"u\": {\"infantry\": %d}, \"e\": {}},"
                    + "\"%s\": {\"o\": \"y\", \"s\": %b, \"u\": {\"infantry\": %d}, \"e\": {}}}}",
                a, aOwned, aUnits, b, bOwned, bUnits))
        .getAsJsonObject();
  }
}
