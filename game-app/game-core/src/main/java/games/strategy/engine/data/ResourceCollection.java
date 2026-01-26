package games.strategy.engine.data;

import games.strategy.triplea.Constants;
import java.text.MessageFormat;
import org.triplea.java.collections.IntegerMap;

/** A collection of {@link Resource}s. */
public class ResourceCollection extends GameDataComponent {
  private static final long serialVersionUID = -1247795977888113757L;
  public static final int MAX_FIT_VALUE = 10000; // how often costs can fit into resource collection

  private final IntegerMap<Resource> resources = new IntegerMap<>();

  public ResourceCollection(final GameData data) {
    super(data);
  }

  public ResourceCollection(final ResourceCollection other) {
    super(other.getData());
    resources.add(other.resources);
  }

  public ResourceCollection(final ResourceCollection[] others, final GameData data) {
    super(data);
    for (final ResourceCollection other : others) {
      resources.add(other.resources);
    }
  }

  public ResourceCollection(final GameData data, final IntegerMap<Resource> resources) {
    this(data);
    this.resources.add(resources);
  }

  public void addResource(final Resource resource, final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    change(resource, quantity);
  }

  /**
   * You cannot remove more than the collection contains.
   *
   * @param resource referring resource
   * @param quantity quantity of the resource that should be removed
   */
  public void removeResourceUpTo(final Resource resource, final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    final int current = getQuantity(resource);
    change(resource, -Math.min(current, quantity));
  }

  private void change(final Resource resource, final int quantity) {
    resources.add(resource, quantity);
  }

  /** Overwrites any current resource with the same name. */
  public void putResource(final Resource resource, final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    resources.put(resource, quantity);
  }

  public IntegerMap<Resource> getResourcesCopy() {
    return new IntegerMap<>(resources);
  }

  public int getQuantity(final Resource resource) {
    return resources.getInt(resource);
  }

  public int getQuantity(final String name) {
    try (GameData.Unlocker ignored = getData().acquireReadLock()) {
      final Resource resource = getData().getResourceList().getResourceOrThrow(name);
      return getQuantity(resource);
    }
  }

  public boolean has(final IntegerMap<Resource> map) {
    return resources.greaterThanOrEqualTo(map);
  }

  /** Returns new ResourceCollection containing the difference between both collections. */
  public ResourceCollection difference(final ResourceCollection otherCollection) {
    final ResourceCollection returnCollection = new ResourceCollection(getData(), resources);
    returnCollection.subtract(otherCollection.resources);
    return returnCollection;
  }

  private void subtract(final IntegerMap<Resource> cost) {
    for (final Resource resource : cost.keySet()) {
      int costValue = cost.getInt(resource);
      if (costValue < 0) {
        throw new IllegalArgumentException(
            MessageFormat.format(
                "Cost must be positive, but was {0} for resource {1}",
                costValue, resource.getName()));
      }
      change(resource, -costValue);
    }
  }

  public void add(final ResourceCollection otherResources) {
    resources.add(otherResources.resources);
  }

  public void add(final IntegerMap<Resource> resources) {
    for (final Resource resource : resources.keySet()) {
      addResource(resource, resources.getInt(resource));
    }
  }

  public void add(final IntegerMap<Resource> resources, final int quantity) {
    for (int i = 0; i < quantity; i++) {
      add(resources);
    }
  }

  /**
   * Will apply a discount if giving a fractional double (ie: 0.5 = 50% discount). Will round up
   * remainder.
   */
  public void discount(final double discount) {
    resources.multiplyAllValuesBy(discount);
  }

  /**
   * Returns {@link ResourceCollection#MAX_FIT_VALUE} if it can fit more times than {@link
   * ResourceCollection#MAX_FIT_VALUE} or if cost is zero.
   */
  public int fitsHowOften(final IntegerMap<Resource> cost) {
    return (cost.isEmpty() || (cost.totalValues() <= 0 && cost.isPositive()))
        ? MAX_FIT_VALUE
        : cost.entrySet().stream()
            .mapToInt(
                costEntry -> {
                  int resourceCost = costEntry.getValue();
                  if (resourceCost == 0) {
                    return MAX_FIT_VALUE;
                  } else {
                    return this.resources.getInt(costEntry.getKey()) / resourceCost;
                  }
                })
            .min()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        MessageFormat.format(
                            "Could not calculate how often cost of {0} can be paid with resources {1}",
                            cost, this.resources)));
  }

  @Override
  public String toString() {
    return toString(resources, getData());
  }

  public static String toString(final IntegerMap<Resource> resources, final GameData data) {
    return toString(resources, data, ", ");
  }

  private static String toString(
      final IntegerMap<Resource> resources, final GameData data, final String lineSeparator) {
    if (resources == null || resources.allValuesEqual(0)) {
      return "nothing";
    }
    final StringBuilder sb = new StringBuilder();
    Resource pus = null;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      pus = data.getResourceList().getResourceOrThrow(Constants.PUS);
    } catch (final NullPointerException e) {
      // we are getting null pointers here occasionally on deserializing game saves, because
      // data.getResourceList() is
      // still null at this point
      for (final Resource r : resources.keySet()) {
        if (r.getName().equals(Constants.PUS)) {
          pus = r;
          break;
        }
      }
    }
    if (pus == null) {
      throw new IllegalStateException("Possible deserialization error: PUs is null");
    }
    if (resources.getInt(pus) != 0) {
      sb.append(lineSeparator);
      sb.append(resources.getInt(pus));
      sb.append(" ");
      sb.append(pus.getName());
    }
    for (final Resource resource : resources.keySet()) {
      if (resource.equals(pus)) {
        continue;
      }
      sb.append(lineSeparator);
      sb.append(resources.getInt(resource));
      sb.append(" ");
      sb.append(resource.getName());
    }
    return sb.toString().replaceFirst(lineSeparator, "");
  }

  public String toStringForHtml() {
    return toStringForHtml(resources, getData());
  }

  public static String toStringForHtml(final IntegerMap<Resource> resources, final GameData data) {
    return toString(resources, data, "<br />");
  }

  /**
   * Adds {@code times - 1} copies of each resource in this collection.
   *
   * @param times multiply this Collection {@code times}.
   */
  public void multiply(final int times) {
    final IntegerMap<Resource> base = new IntegerMap<>(resources);
    add(base, times - 1);
  }

  public boolean isEmpty() {
    return resources.isEmpty();
  }
}
