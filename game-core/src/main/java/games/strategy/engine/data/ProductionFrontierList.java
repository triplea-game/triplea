package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

/**
 * A keyed collection of {@link ProductionFrontier}s.
 */
public class ProductionFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -7565214499087021809L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final Map<String, ProductionFrontier> m_productionFrontiers = new HashMap<>();

  public ProductionFrontierList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addProductionFrontier(final ProductionFrontier pf) {
    m_productionFrontiers.put(pf.getName(), pf);
  }

  public int size() {
    return m_productionFrontiers.size();
  }

  public ProductionFrontier getProductionFrontier(final String name) {
    return m_productionFrontiers.get(name);
  }

  public Set<String> getProductionFrontierNames() {
    return m_productionFrontiers.keySet();
  }
}
