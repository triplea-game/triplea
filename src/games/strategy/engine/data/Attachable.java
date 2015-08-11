package games.strategy.engine.data;

import java.util.Map;

public interface Attachable {
  public void addAttachment(String key, IAttachment value);

  public void removeAttachment(String keyString);

  public IAttachment getAttachment(String key);

  public Map<String, IAttachment> getAttachments();
}
