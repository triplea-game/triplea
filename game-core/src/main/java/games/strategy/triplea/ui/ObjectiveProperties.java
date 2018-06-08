package games.strategy.triplea.ui;

import java.time.Instant;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Loads objective text from objectives.properties.
 */
public class ObjectiveProperties extends PropertyFile {

  private static final String PROPERTY_FILE = "objectives.properties";
  private static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";
  static final String GROUP_PROPERTY = "TABLEGROUP";


  private static ObjectiveProperties instance = null;
  private static Instant timestamp = Instant.EPOCH;

  protected ObjectiveProperties() {
    super(PROPERTY_FILE);
  }

  public static ObjectiveProperties getInstance() {
    // cache properties for 1 second
    if (instance == null || timestamp.plusSeconds(1).isBefore(Instant.now())) {
      instance = new ObjectiveProperties();
      timestamp = Instant.now();
    }
    return instance;
  }

  public String getName() {
    return properties.getProperty(ObjectiveProperties.OBJECTIVES_PANEL_NAME, "Objectives");
  }

  public Set<Entry<Object, Object>> entrySet() {
    return properties.entrySet();
  }
}
