package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A collection of {@link Resource}s keyed on the resource name. */
public class ResourceList extends GameDataComponent {
  private static final long serialVersionUID = -8812702449627698253L;

  private final Map<String, Resource> resources = new LinkedHashMap<>();

  public ResourceList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addResource(final Resource resource) {
    resources.put(resource.getName(), resource);
  }

  public int size() {
    return resources.size();
  }

  public Resource getResource(final String name) {
    return resources.get(name);
  }

  public Collection<Resource> getResources() {
    return Collections.unmodifiableCollection(resources.values());
  }
}
