package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/** A technology advance that provides the ability for destroyers to conduct bombardment. */
public final class DestroyerBombardTechAdvance extends TechAdvance {
  private static final long serialVersionUID = -4977423636387126617L;

  public DestroyerBombardTechAdvance(final GameData data) {
    super(TECH_NAME_DESTROYER_BOMBARD, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_DESTROYER_BOMBARD;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getDestroyerBombard();
  }
}
