package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;

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

  public Optional<Resource> getResourceOptional(final String name) {
    return Optional.ofNullable(resources.get(name));
  }

  public Resource getResourceOrThrow(final @NonNls String name) {
    return getResourceOptional(name)
        .orElseThrow(() -> new IllegalArgumentException("No resource named: " + name));
  }

  public Collection<Resource> getResources() {
    return Collections.unmodifiableCollection(resources.values());
  }
}
