package games.strategy.triplea.ui.battledisplay;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ui.IUIContext;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.util.Optional;

class TableData {
  static final TableData NULL = new TableData();
  private int m_count;
  private Optional<ImageIcon> m_icon;

  private TableData() {}

  TableData(final PlayerID player, final int count, final UnitType type, final GameData data, final boolean damaged,
      final boolean disabled, final IUIContext uiContext) {
    m_count = count;
    m_icon = uiContext.getUnitImageFactory().getIcon(type, player, data, damaged, disabled);
  }

  public void updateStamp(final JLabel stamp) {
    if (m_count == 0) {
      stamp.setText("");
      stamp.setIcon(null);
    } else {
      stamp.setText("x" + m_count);
      if (m_icon.isPresent()) {
        stamp.setIcon(m_icon.get());
      }
    }
  }
}
