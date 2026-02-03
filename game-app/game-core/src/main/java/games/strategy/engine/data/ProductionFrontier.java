package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NonNls;

/** A collection of {@link ProductionRule}s. */
public class ProductionFrontier extends DefaultNamed implements Iterable<ProductionRule> {
  public static final @NonNls String PRODUCTION = "production";
  public static final @NonNls String PRODUCTION_INDUSTRIAL_TECHNOLOGY =
      "productionIndustrialTechnology";
  public static final @NonNls String PRODUCTION_SHIPYARDS = "productionShipyards";
  @Serial private static final long serialVersionUID = -5967251608158552892L;

  private final List<ProductionRule> rules;
  private List<ProductionRule> cachedRules;

  public ProductionFrontier(final String name, final GameData data) {
    this(name, data, List.of());
  }

  public ProductionFrontier(
      final String name, final GameData data, final List<ProductionRule> rules) {
    super(name, data);

    checkNotNull(rules);

    this.rules = new ArrayList<>(rules);
  }

  public void addRule(final ProductionRule rule) {
    if (rules.contains(rule)) {
      throw new IllegalStateException("Rule already added: " + rule);
    }
    rules.add(rule);
    cachedRules = null;
  }

  public void removeRule(final ProductionRule rule) {
    if (!rules.contains(rule)) {
      throw new IllegalStateException("Rule not present: " + rule);
    }
    rules.remove(rule);
    cachedRules = null;
  }

  public List<ProductionRule> getRules() {
    if (cachedRules == null) {
      cachedRules = Collections.unmodifiableList(rules);
    }
    return cachedRules;
  }

  /**
   * @return Collection of <code>UnitType</code> that can be produced by this frontier
   */
  public Collection<UnitType> getProducibleUnitTypes() {
    Collection<UnitType> producibleUnitTypes = new ArrayList<>();
    for (final ProductionRule rule : this) {
      for (final NamedAttachable type : rule.getResults().keySet()) {
        if (type instanceof UnitType unitType) {
          producibleUnitTypes.add(unitType);
        }
      }
    }
    return producibleUnitTypes;
  }

  @Override
  public Iterator<ProductionRule> iterator() {
    return getRules().iterator();
  }
}
