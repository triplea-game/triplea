package games.strategy.grid.kingstable.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.annotations.GameProperty;

/**
 * Territory attachment for King's Table.
 *
 */
public class TerritoryAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -2114955190688754947L;
  private boolean m_kingsSquare = false;
  private boolean m_kingsExit = false;

  /** Creates new TerritoryAttachment */
  public TerritoryAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKingsSquare(final String value) {
    m_kingsSquare = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKingsSquare(final Boolean value) {
    m_kingsSquare = value;
  }

  public boolean getKingsSquare() {
    return m_kingsSquare;
  }

  public void resetKingsSquare() {
    m_kingsSquare = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKingsExit(final String value) {
    m_kingsExit = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setKingsExit(final Boolean value) {
    m_kingsExit = value;
  }

  public boolean getKingsExit() {
    return m_kingsExit;
  }

  public void resetKingsExit() {
    m_kingsExit = false;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    // TODO Auto-generated method stub
  }
}
