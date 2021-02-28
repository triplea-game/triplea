package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** A collection of {@link ProductionRule}s keyed on the production rule name. */
public class ProductionRuleList extends GameDataComponent {
  private static final long serialVersionUID = -5313215563006788188L;

  private final Map<String, ProductionRule> productionRules = new HashMap<>();

  public ProductionRuleList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addProductionRule(final ProductionRule pf) {
    productionRules.put(pf.getName(), pf);
  }

  public int size() {
    return productionRules.size();
  }

  public ProductionRule getProductionRule(final String name) {
    return productionRules.get(name);
  }

  public Collection<ProductionRule> getProductionRules() {
    return productionRules.values();
  }
}
