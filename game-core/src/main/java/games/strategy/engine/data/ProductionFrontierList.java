package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProductionFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -7565214499087021809L;
  private final Map<String, ProductionFrontier> m_productionFrontiers;

  public ProductionFrontierList(final GameData data) {
    this(data, new HashMap<>());
  }

  private ProductionFrontierList(final GameData data, final Map<String, ProductionFrontier> productionFrontiers) {
    super(data);
    m_productionFrontiers = productionFrontiers;
  }

  protected void addProductionFrontier(final ProductionFrontier pf) {
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

  ProductionFrontierList clone(final GameData newData) {
    return new ProductionFrontierList(newData, new HashMap<>(m_productionFrontiers));
  }
}
