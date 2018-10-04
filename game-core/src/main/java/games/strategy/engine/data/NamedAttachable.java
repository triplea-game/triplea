package games.strategy.engine.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Superclass for named game data components that can host attachments.
 */
public class NamedAttachable extends DefaultNamed implements Attachable {
  private static final long serialVersionUID = 8597712929519099255L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final Map<String, IAttachment> m_attachments = new HashMap<>();

  /** Creates new NamedAttachable. */
  public NamedAttachable(final String name, final GameData data) {
    super(name, data);
  }

  @Override
  public IAttachment getAttachment(final String key) {
    return m_attachments.get(key);
  }

  @Override
  public Map<String, IAttachment> getAttachments() {
    return Collections.unmodifiableMap(m_attachments);
  }

  @Override
  public void addAttachment(final String key, final IAttachment value) {
    m_attachments.put(key, value);
  }

  @Override
  public void removeAttachment(final String keyString) {
    m_attachments.remove(keyString);
  }
}
