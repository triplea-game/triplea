package org.triplea.ai.flowfield.diffusion;

import games.strategy.engine.data.Territory;
import lombok.Data;

@Data
public class FieldTerritory {
  private Territory territory;
  private long value;

  FieldTerritory(final Territory territory) {
    this.territory = territory;
    this.value = 0;
  }

  void addValue(final long value) {
    this.value += value;
  }
}
