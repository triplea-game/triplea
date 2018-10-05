package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A collection of {@link Resource}s keyed on the resource name.
 */
public class ResourceList extends GameDataComponent {
  private static final long serialVersionUID = -8812702449627698253L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final Map<String, Resource> m_resourceList = new LinkedHashMap<>();

  public ResourceList(final GameData data) {
    super(data);
  }

  protected void addResource(final Resource resource) {
    m_resourceList.put(resource.getName(), resource);
  }

  public int size() {
    return m_resourceList.size();
  }

  public Resource getResource(final String name) {
    return m_resourceList.get(name);
  }

  public List<Resource> getResources() {
    return new ArrayList<>(m_resourceList.values());
  }
}
