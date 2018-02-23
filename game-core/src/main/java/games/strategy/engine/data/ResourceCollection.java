package games.strategy.engine.data;

import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

public class ResourceCollection extends GameDataComponent {
  private static final long serialVersionUID = -1247795977888113757L;
  private final IntegerMap<Resource> m_resources = new IntegerMap<>();

  /**
   * Creates new ResourceCollection.
   *
   * @param data
   *        game data
   */
  public ResourceCollection(final GameData data) {
    super(data);
  }

  public ResourceCollection(final ResourceCollection other) {
    super(other.getData());
    m_resources.add(other.m_resources);
  }

  public ResourceCollection(final ResourceCollection[] others, final GameData data) {
    super(data);
    for (final ResourceCollection other : others) {
      m_resources.add(other.m_resources);
    }
  }

  public ResourceCollection(final GameData data, final IntegerMap<Resource> resources) {
    this(data);
    m_resources.add(resources);
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
   * @param resource
   *        referring resource
   * @param quantity
   *        quantity of the resource that should be removed
   */
  public void removeResource(final Resource resource, final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    final int current = getQuantity(resource);
    if ((current - quantity) < 0) {
      throw new IllegalArgumentException("Cant remove more than player has of resource: " + resource.getName()
          + ". current:" + current + " toRemove: " + quantity);
    }
    change(resource, -quantity);
  }

  public void removeAllOfResource(final Resource resource) {
    m_resources.removeKey(resource);
  }

  private void change(final Resource resource, final int quantity) {
    m_resources.add(resource, quantity);
  }

  /**
   * Overwrites any current resource with the same name.
   */
  public void putResource(final Resource resource, final int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    m_resources.put(resource, quantity);
  }

  public IntegerMap<Resource> getResourcesCopy() {
    return new IntegerMap<>(m_resources);
  }

  public int getQuantity(final Resource resource) {
    return m_resources.getInt(resource);
  }

  public int getQuantity(final String name) {
    getData().acquireReadLock();
    try {
      final Resource resource = getData().getResourceList().getResource(name);
      if (resource == null) {
        throw new IllegalArgumentException("No resource named:" + name);
      }
      return getQuantity(resource);
    } finally {
      getData().releaseReadLock();
    }
  }

  public boolean has(final IntegerMap<Resource> map) {
    return m_resources.greaterThanOrEqualTo(map);
  }

  /**
   * @return new ResourceCollection containing the difference between both collections.
   */
  public ResourceCollection difference(final ResourceCollection otherCollection) {
    final ResourceCollection returnCollection = new ResourceCollection(getData(), m_resources);
    returnCollection.subtract(otherCollection);
    return returnCollection;
  }

  private void subtract(final ResourceCollection resourceCollection) {
    subtract(resourceCollection.m_resources);
  }

  private void subtract(final IntegerMap<Resource> cost) {
    for (final Resource resource : cost.keySet()) {
      removeResource(resource, cost.getInt(resource));
    }
  }

  public void add(final ResourceCollection otherResources) {
    m_resources.add(otherResources.m_resources);
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
   * Will apply a discount if giving a fractional double (ie: 0.5 = 50% discount). Will round up remainder.
   */
  public void discount(final double discount) {
    multiplyAllValuesBy(discount, 3);
  }

  /**
   * Will multiply all values by a given double. Can be used to divide all numbers, if given a fractional double (ie: to
   * divide by 2, use
   * 0.5 as the double)
   *
   * @param roundType
   *        (1 = floor, 2 = round, 3 = ceil)
   */
  public void multiplyAllValuesBy(final double multiplyBy, final int roundType) {
    m_resources.multiplyAllValuesBy(multiplyBy, roundType);
  }

  /**
   * @return will return 10000 if it can fit more times than 10000. will return Integer MaxValue if cost is zero.
   */
  public int fitsHowOften(final IntegerMap<Resource> cost) {
    if ((cost.size() == 0) || ((cost.totalValues() <= 0) && cost.isPositive())) {
      return Integer.MAX_VALUE;
    }
    final ResourceCollection resources = new ResourceCollection(getData(), m_resources);
    for (int i = 0; i <= 10000; i++) {
      try {
        resources.subtract(cost);
      } catch (final IllegalArgumentException iae) {
        // when the subtraction isn't possible it will throw an exception, which means we can return i;
        return i;
      }
    }
    // throw new IllegalArgumentException("Unlimited purchases shouldn't be possible");
    // System.out.println("Can purchase more than 10,000 of unit - Unlimited purchases shouldn't be possible");
    return 10000;
  }

  @Override
  public String toString() {
    return toString(m_resources, getData(), ", ");
  }

  private static String toString(final IntegerMap<Resource> resources, final GameData data,
      final String lineSeparator) {
    if ((resources == null) || resources.isEmpty() || resources.allValuesEqual(0)) {
      return "nothing";
    }
    final StringBuilder sb = new StringBuilder();
    Resource pus = null;
    data.acquireReadLock();
    try {
      pus = data.getResourceList().getResource(Constants.PUS);
    } catch (final NullPointerException e) {
      // we are getting null pointers here occasionally on deserializing gamesaves, because data.getResourceList() is
      // still null at this
      // point
      for (final Resource r : resources.keySet()) {
        if (r.getName().equals(Constants.PUS)) {
          pus = r;
          break;
        }
      }
    } finally {
      data.releaseReadLock();
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
    return toStringForHtml(m_resources, getData());
  }

  public static String toStringForHtml(final IntegerMap<Resource> resources, final GameData data) {
    return toString(resources, data, "<br />");
  }

  /**
   * @param times
   *        multiply this Collection times times.
   */
  public void multiply(final int times) {
    final IntegerMap<Resource> base = new IntegerMap<>(m_resources);
    add(base, times - 1);
  }

  public boolean isEmpty() {
    return m_resources.isEmpty();
  }
}
