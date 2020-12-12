package org.triplea.ai.flowfield.map;

import games.strategy.engine.data.Territory;
import java.util.Map;
import lombok.Value;

@Value
public class DiffusionType {
  String name;
  double diffusion;
  Map<Territory, Long> territoryValuations;
}
