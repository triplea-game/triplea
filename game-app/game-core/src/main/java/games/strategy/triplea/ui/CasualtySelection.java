package games.strategy.triplea.ui;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.util.UnitOwner;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * drives the casualty selection process:
 *
 * <p>1. construct a {@code CasualtySelection} object e.g. {@code final var casualtySelection = new
 * CasualtySelection([...]);}
 *
 * <p>2. call {@code showModelDialog} e.g. {@code final CasualtyDetails selectedCasualties =
 * casualtySelection.showModalDialog().orElse(null);}
 */
public class CasualtySelection {
  @VisibleForTesting public UnitChooser chooser;

  private final int hitsToTake;
  private final GamePlayer player;

  private final JDialog dialog;
  private final boolean isEditMode;

  private final JOptionPane optionPane;

  public CasualtySelection(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int hitsToTake,
      final String title,
      final GamePlayer player,
      final CasualtyList defaultCasualties,
      final boolean allowMultipleHitsPerUnit,
      final UiContext uiContext,
      final Component dialogParent,
      final boolean isEditMode) {
    this.hitsToTake = hitsToTake;
    this.player = player;

    this.isEditMode = isEditMode;

    final boolean movementForAirUnitsOnly =
        playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(selectFrom);

    chooser =
        new UnitChooser(
            selectFrom,
            defaultCasualties,
            dependents,
            Properties.getPartialAmphibiousRetreat(player.getData().getProperties()),
            movementForAirUnitsOnly,
            allowMultipleHitsPerUnit,
            uiContext);
    chooser.setTitle(title);

    if (isEditMode) {
      chooser.disableMax();
    } else {
      chooser.setMax(hitsToTake);
    }

    final JScrollPane chooserScrollPane = new JScrollPane(chooser);
    chooserScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
    optionPane =
        new JOptionPane(chooserScrollPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

    dialog = optionPane.createDialog(dialogParent, player.getName() + " select casualties");
  }

  /**
   * @return the CasualtyDetails that describe which casualties the player selected or empty iff
   *     something went weirdly wrong
   */
  public Optional<CasualtyDetails> showModalDialog() {
    final JScrollPane chooserScrollPane = (JScrollPane) optionPane.getMessage();
    chooserScrollPane.setFont(
        BattleDisplay.getPlayerComponent(player).getFont().deriveFont(Font.BOLD, 14));
    final Dimension size = chooserScrollPane.getPreferredSize();
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availHeight = screenResolution.height - 130;
    final int availWidth = screenResolution.width - 30;
    chooserScrollPane.setPreferredSize(
        new Dimension(
            Math.min(
                availWidth,
                size.width
                    + (size.height > availHeight
                        ? chooserScrollPane.getVerticalScrollBar().getPreferredSize().width
                        : 0)),
            Math.min(availHeight, size.height)));

    dialog.setVisible(true);
    final Object option = optionPane.getValue();
    final int ioption = option instanceof Integer ? (Integer) option : -1;

    if (ioption != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }

    final List<Unit> killed = chooser.getSelected(false);
    final List<Unit> damaged = chooser.getSelectedDamagedMultipleHitPointUnits();
    if (!isEditMode && (killed.size() + damaged.size() != hitsToTake)) {
      JOptionPane.showMessageDialog(
          dialog /*.getParent()*/,
          "Wrong number of casualties selected",
          player.getName() + " select casualties",
          JOptionPane.ERROR_MESSAGE);

      return Optional.empty();
    }

    return Optional.of(new CasualtyDetails(killed, damaged, false));
  }

  /**
   * This method determines whether the system should let the player choose how to distribute hits
   * between units of the same owner and type differentiating by the movement points left.
   *
   * <p>This is only considered to be the case if there are - air units - that can take damage
   * without bing killed (i.e. that have more than one hitpoint left) and - that have different
   * movement points left.
   *
   * @param units among which the hits have to distributed
   * @return {@code true} iff the system should let the player choose
   */
  static boolean playerMayChooseToDistributeHitsToUnitsWithDifferentMovement(
      final Collection<Unit> units) {
    final Map<UnitOwner, List<Unit>> unitsGroupedByOwnerAndType =
        units.stream()
            .filter(Matches.unitIsAir())
            .filter(Unit::canTakeHitWithoutBeingKilled)
            .collect(Collectors.groupingBy(UnitOwner::new, Collectors.toList()));

    for (final UnitOwner ownerAndType : unitsGroupedByOwnerAndType.keySet()) {
      final Iterator<Unit> unitsOfSameOwnerAndType =
          unitsGroupedByOwnerAndType.get(ownerAndType).iterator();

      final BigDecimal movementOfFirstUnit = unitsOfSameOwnerAndType.next().getMovementLeft();

      while (unitsOfSameOwnerAndType.hasNext()) {
        final Unit next = unitsOfSameOwnerAndType.next();

        if (!next.getMovementLeft().equals(movementOfFirstUnit)) {
          return true;
        }
      }
    }

    return false;
  }
}
