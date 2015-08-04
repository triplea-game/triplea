package games.strategy.grid.chess.attachments;

import java.util.ArrayList;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.annotations.GameProperty;


public class PlayerAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -3602673484709292709L;
  public static final String ATTACHMENT_NAME = "playerAttachment";
  public static final String LAST_PIECES_MOVED = "lastPiecesMoved";
  private ArrayList<Unit> m_lastPiecesMoved = new ArrayList<Unit>();

  /**
   * Convenience method. can return null.
   */
  public static PlayerAttachment get(final PlayerID p) {
    final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(ATTACHMENT_NAME);
    return rVal;
  }

  /** Creates new PlayerAttachment */
  public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLastPiecesMoved(final String value) {}

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLastPiecesMoved(final ArrayList<Unit> value) {
    m_lastPiecesMoved = value;
  }

  public ArrayList<Unit> getLastPiecesMoved() {
    return m_lastPiecesMoved;
  }

  public void resetLastPiecesMoved() {
    m_lastPiecesMoved = new ArrayList<Unit>();
  }

  @Override
  public void validate(final GameData data) throws GameParseException {}
}
