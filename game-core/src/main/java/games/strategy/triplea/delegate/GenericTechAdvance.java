package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

public class GenericTechAdvance extends TechAdvance {
  private static final long serialVersionUID = -5985281030083508185L;
  private final TechAdvance m_advance;

  public GenericTechAdvance(final String name, final TechAdvance techAdvance, final GameData data) {
    super(name, data);
    m_advance = techAdvance;
  }

  @Override
  public String getProperty() {
    return (m_advance != null) ? m_advance.getProperty() : getName();
  }

  @Override
  public void perform(final PlayerID id, final IDelegateBridge bridge) {
    if (m_advance != null) {
      m_advance.perform(id, bridge);
    }
  }

  public TechAdvance getAdvance() {
    return m_advance;
  }

  @Override
  public boolean hasTech(final TechAttachment ta) {
    if (m_advance != null) {
      return m_advance.hasTech(ta);
    }
    // this can be null!!!
    final Boolean has = ta.hasGenericTech(getName());
    return ((has == null) ? false : has);
  }
}
