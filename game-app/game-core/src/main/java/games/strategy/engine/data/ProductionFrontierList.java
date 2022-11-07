package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** A keyed collection of {@link ProductionFrontier}s. */
public class ProductionFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -7565214499087021809L;

  private final Map<String, ProductionFrontier> productionFrontiers = new HashMap<>();

  public ProductionFrontierList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addProductionFrontier(final ProductionFrontier pf) {
    productionFrontiers.put(pf.getName(), pf);
  }

  public int size() {
    return productionFrontiers.size();
  }

  public ProductionFrontier getProductionFrontier(final String name) {
    return productionFrontiers.get(name);
  }

  public Set<String> getProductionFrontierNames() {
    return productionFrontiers.keySet();
  }
}
