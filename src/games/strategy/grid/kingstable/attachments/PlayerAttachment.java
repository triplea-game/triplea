package games.strategy.grid.kingstable.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.annotations.GameProperty;


public class PlayerAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -4833151445864523853L;
  private boolean m_needsKing = false;
  private int m_alphaBetaSearchDepth = 2;

  /** Creates new PlayerAttachment */
  public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNeedsKing(final String value) {
    m_needsKing = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setNeedsKing(final Boolean value) {
    m_needsKing = value;
  }

  public boolean getNeedsKing() {
    return m_needsKing;
  }

  public void resetNeedsKing() {
    m_needsKing = false;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlphaBetaSearchDepth(final String value) {
    m_alphaBetaSearchDepth = getInt(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAlphaBetaSearchDepth(final Integer value) {
    m_alphaBetaSearchDepth = value;
  }

  public int getAlphaBetaSearchDepth() {
    return m_alphaBetaSearchDepth;
  }

  public void resetAlphaBetaSearchDepth() {
    m_alphaBetaSearchDepth = 2;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    // TODO Auto-generated method stub
  }
}
