package games.strategy.triplea.ui;

import games.strategy.triplea.ResourceLoader;
import java.util.Map.Entry;
import java.util.Set;

/** Loads objective text from objectives.properties. */
public class ObjectiveProperties extends PropertyFile {
  static final String GROUP_PROPERTY = "TABLEGROUP";
  private static final String PROPERTY_FILE = "objectives.properties";
  private static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";

  protected ObjectiveProperties(final ResourceLoader resourceLoader) {
    super(PROPERTY_FILE, resourceLoader);
  }

  public static ObjectiveProperties getInstance(final ResourceLoader resourceLoader) {
    return PropertyFile.getInstance(
        ObjectiveProperties.class, () -> new ObjectiveProperties(resourceLoader));
  }

  public String getName() {
    return properties.getProperty(ObjectiveProperties.OBJECTIVES_PANEL_NAME, "Objectives");
  }

  public Set<Entry<Object, Object>> entrySet() {
    return properties.entrySet();
  }
}
