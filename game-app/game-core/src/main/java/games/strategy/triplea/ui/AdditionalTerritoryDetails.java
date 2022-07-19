package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdditionalTerritoryDetails {
  private final List<Function<Territory, String>> detailProviders = new ArrayList<>();

  public void addAdditionalTerritoryDetailsFunction(final Function<Territory, String> method) {
    this.detailProviders.add(method);
  }

  public void removeAdditionalTerritoryDetailsFunction(final Function<Territory, String> method) {
    this.detailProviders.remove(method);
  }

  public String computeAdditionalText(Territory territory) {
    return this.detailProviders.stream()
        .map(method -> method.apply(territory))
        .collect(Collectors.joining("<br />"));
  }
}
