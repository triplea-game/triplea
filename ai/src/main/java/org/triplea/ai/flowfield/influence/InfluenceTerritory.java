package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.Territory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.triplea.ai.flowfield.odds.BattleDetails;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InfluenceTerritory {
  final Territory territory;
  long influence = 0;
  @Setter BattleDetails battleDetails;
  int distanceFromInitialTerritory;
  /** Tracks battle details that were diffused to this territory and their distance */
  final Map<BattleDetails, Integer> battleDetailsByDistance = new HashMap<>();
  /**
   * Tracks territories that this territory diffused the battle details to. Prevents diffusing the
   * battle details back towards the initial territory.
   */
  final Set<InfluenceTerritory> territoriesThatWereDiffusedTo = new HashSet<>();

  InfluenceTerritory(final Territory territory) {
    this.territory = territory;
  }

  long getInfluence() {
    return influence;
  }

  void addDiffusedInfluence(final long value) {
    this.influence += value;
  }

  void setDistanceFromInitialTerritory(final int distanceFromInitialTerritory) {
    this.distanceFromInitialTerritory = distanceFromInitialTerritory;
  }

  void updateDiffusedBattleDetails(final InfluenceTerritory otherTerritory) {
    otherTerritory.diffusedToTerritory(this);
    this.battleDetailsByDistance.clear();
    this.battleDetailsByDistance.putAll(otherTerritory.getBattleDetailsByDistance());
    if (!otherTerritory.getBattleDetails().isEmpty()) {
      this.battleDetailsByDistance.put(
          otherTerritory.getBattleDetails(), otherTerritory.getDistanceFromInitialTerritory());
    }
  }

  void diffusedToTerritory(final InfluenceTerritory otherTerritory) {
    territoriesThatWereDiffusedTo.add(otherTerritory);
  }

  /** Detects whether the new set of battle details is easier to defeat than the current set */
  boolean shouldBattleDetailsByUpdated(final InfluenceTerritory otherTerritory) {
    if (territoriesThatWereDiffusedTo.contains(otherTerritory)) {
      return false;
    }
    final double currentBattleDetailsValue =
        battleDetailsByDistance.keySet().stream().mapToDouble(BattleDetails::getValue).sum();
    final double otherBattleDetailsValue =
        otherTerritory.getBattleDetailsByDistance().keySet().stream()
                .mapToDouble(BattleDetails::getValue)
                .sum()
            + otherTerritory.getBattleDetails().getValue();
    return currentBattleDetailsValue > otherBattleDetailsValue;
  }
}
