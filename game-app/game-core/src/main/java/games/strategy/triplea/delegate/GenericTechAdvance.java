package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;
import lombok.Getter;

/**
 * A proxy for another {@link TechAdvance}. Delegates all operations to the proxied instance. If no
 * instance to proxy is specified, an instance of this class will provide a suitable Null Object
 * implementation.
 */
@Getter
public class GenericTechAdvance extends TechAdvance {
  private static final long serialVersionUID = -5985281030083508185L;

  private final TechAdvance advance;

  public GenericTechAdvance(final String name, final TechAdvance techAdvance, final GameData data) {
    super(name, data);
    advance = techAdvance;
  }

  @Override
  public String getProperty() {
    return (advance != null) ? advance.getProperty() : getName();
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {
    if (advance != null) {
      advance.perform(gamePlayer, bridge);
    }
  }

  @Override
  public boolean hasTech(final TechAttachment ta) {
    if (advance != null) {
      return advance.hasTech(ta);
    }
    // this can be null!!!
    final Boolean has = ta.hasGenericTech(getName());
    return (has != null && has);
  }
}
