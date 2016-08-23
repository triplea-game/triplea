package games.strategy.triplea.ui.battledisplay;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

class CasualtyNotificationPanel extends JPanel {
  private static final long serialVersionUID = -8254027929090027450L;
  private final DicePanel m_dice;
  private final JPanel m_killed = new JPanel();
  private final JPanel m_damaged = new JPanel();
  private final GameData m_data;
  private final IUIContext m_uiContext;

  public CasualtyNotificationPanel(final GameData data, final IUIContext uiContext) {
    m_data = data;
    m_uiContext = uiContext;
    m_dice = new DicePanel(uiContext, data);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(m_dice);
    add(m_killed);
    add(m_damaged);
  }

  protected void setNotification(final DiceRoll dice, final Collection<Unit> killed,
      final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents) {
    final boolean isEditMode = (dice == null);
    if (!isEditMode) {
      m_dice.setDiceRoll(dice);
    }
    m_killed.removeAll();
    m_damaged.removeAll();
    if (!killed.isEmpty()) {
      m_killed.add(new JLabel("Killed"));
    }
    final Iterator<UnitCategory> killedIter = UnitSeperator.categorize(killed, dependents, false, false).iterator();
    categorizeUnits(killedIter, false, false);
    damaged.removeAll(killed);
    if (!damaged.isEmpty()) {
      m_damaged.add(new JLabel("Damaged"));
    }
    final Iterator<UnitCategory> damagedIter = UnitSeperator.categorize(damaged, dependents, false, false).iterator();
    categorizeUnits(damagedIter, true, true);
    invalidate();
    validate();
  }

  protected void setNotificationShort(final Collection<Unit> killed, final Map<Unit, Collection<Unit>> dependents) {
    m_killed.removeAll();
    if (!killed.isEmpty()) {
      m_killed.add(new JLabel("Killed"));
    }
    final Iterator<UnitCategory> killedIter = UnitSeperator.categorize(killed, dependents, false, false).iterator();
    categorizeUnits(killedIter, false, false);
    invalidate();
    validate();
  }

  private void categorizeUnits(final Iterator<UnitCategory> categoryIter, final boolean damaged,
      final boolean disabled) {
    while (categoryIter.hasNext()) {
      final UnitCategory category = categoryIter.next();
      final JPanel panel = new JPanel();
      // TODO Kev determine if we need to identify if the unit is hit/disabled
      final Optional<ImageIcon> unitImage =
          m_uiContext.getUnitImageFactory().getIcon(category.getType(), category.getOwner(), m_data,
              damaged && category.hasDamageOrBombingUnitDamage(), disabled && category.getDisabled());
      final JLabel unit = unitImage.isPresent() ? new JLabel(unitImage.get()) : new JLabel();
      panel.add(unit);
      for (final UnitOwner owner : category.getDependents()) {
        unit.add(m_uiContext.createUnitImageJLabel(owner.getType(), owner.getOwner(), m_data));
      }
      panel.add(new JLabel("x " + category.getUnits().size()));
      if (damaged) {
        m_damaged.add(panel);
      } else {
        m_killed.add(panel);
      }
    }
  }
}
