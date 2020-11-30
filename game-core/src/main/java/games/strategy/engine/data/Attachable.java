package games.strategy.engine.data;

import games.strategy.engine.data.changefactory.serializers.PrimitiveNamedAttachable;
import java.util.Map;

/**
 * An entity that can host attachments.
 *
 * <p>Each attachment must be uniquely identified among all other attachments hosted by the entity
 * using a {@code String} key.
 */
public interface Attachable {
  void addAttachment(String key, IAttachment value);

  void removeAttachment(String keyString);

  IAttachment getAttachment(String key);

  Map<String, IAttachment> getAttachments();

  PrimitiveNamedAttachable getPrimitiveForm();
}
