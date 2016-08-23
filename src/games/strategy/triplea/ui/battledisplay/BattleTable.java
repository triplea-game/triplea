package games.strategy.triplea.ui.battledisplay;

import games.strategy.triplea.image.UnitImageFactory;

import javax.swing.JButton;
import javax.swing.JTable;

class BattleTable extends JTable {
  private static final long serialVersionUID = 6737857639382012817L;

  BattleTable(final BattleModel model) {
    super(model);
    setDefaultRenderer(Object.class, new Renderer());
    setRowHeight(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE + 5);
    setBackground(new JButton().getBackground());
    setShowHorizontalLines(false);
    getTableHeader().setReorderingAllowed(false);
    // getTableHeader().setResizingAllowed(false);
  }
}
