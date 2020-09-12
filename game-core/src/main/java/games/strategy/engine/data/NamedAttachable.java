package games.strategy.engine.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Superclass for named game data components that can host attachments. */
public class NamedAttachable extends DefaultNamed implements Attachable {
  private static final long serialVersionUID = 8597712929519099255L;

  private final Map<String, IAttachment> attachments = new HashMap<>();

  public NamedAttachable(final String name, final GameData data) {
    super(name, data);
  }

  @Override
  public IAttachment getAttachment(final String key) {
    return attachments.get(key);
  }

  @Override
  public Map<String, IAttachment> getAttachments() {
    return Collections.unmodifiableMap(attachments);
  }

  @Override
  public void addAttachment(final String key, final IAttachment value) {
    attachments.put(key.replaceAll("ttatchment", "ttachment"), value);
  }

  @Override
  public void removeAttachment(final String keyString) {
    attachments.remove(keyString);
  }
}
