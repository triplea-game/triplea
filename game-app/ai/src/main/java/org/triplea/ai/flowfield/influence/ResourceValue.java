package org.triplea.ai.flowfield.influence;

import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.util.Optional;
import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResourceValue {

  /**
   * Gets the value of the resource from the territory
   *
   * <p>The PUS resource is read from the `production` attribute instead of the `resources`. If the
   * territory has no resources or the resource doesn't exist, then 0 is returned.
   */
  public Function<Territory, Long> territoryToResourceValue(final Resource resource) {
    return territory -> {
      if (resource.getName().equals(Constants.PUS)) {
        return TerritoryAttachment.get(territory)
            .map(territoryAttachment -> (long) territoryAttachment.getProduction())
            .orElse(0L);
      } else {
        return TerritoryAttachment.get(territory)
            .map(
                territoryAttachment ->
                    Optional.ofNullable(territoryAttachment.getResources())
                        .map(resources -> (long) resources.getQuantity(resource))
                        .orElse(0L))
            .orElse(0L);
      }
    };
  }
}
