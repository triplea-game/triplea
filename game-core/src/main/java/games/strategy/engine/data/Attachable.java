package games.strategy.engine.data;

import java.util.Map;

/**
 * An entity that can host attachments.
 */
public interface Attachable {
  void addAttachment(String key, IAttachment value);

  void removeAttachment(String keyString);

  IAttachment getAttachment(String key);

  Map<String, IAttachment> getAttachments();
}
