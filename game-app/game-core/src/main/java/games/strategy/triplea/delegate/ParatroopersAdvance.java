package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAttachment;

/**
 * A technology advance that allows bombers to carry one infantry unit for combat or non-combat
 * movement. If moved into combat, the bomber does not participate, but can be shot down by AA.
 */
public final class ParatroopersAdvance extends TechAdvance {
  private static final long serialVersionUID = 1457384348499672184L;

  public ParatroopersAdvance(final GameData data) {
    super(TECH_NAME_PARATROOPERS, data);
  }

  @Override
  public String getProperty() {
    return TECH_PROPERTY_PARATROOPERS;
  }

  @Override
  public void perform(final GamePlayer gamePlayer, final IDelegateBridge bridge) {}

  @Override
  public boolean hasTech(final TechAttachment ta) {
    return ta.getParatroopers();
  }
}
