package games.strategy.triplea.ui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CollectionUtils;

public class PlacePanel extends AbstractMovePanel {
  private static final long serialVersionUID = -4411301492537704785L;
  private final JLabel actionLabel = new JLabel();
  private final JLabel leftToPlaceLabel = new JLabel();
  private PlaceData placeData;
  private final SimpleUnitPanel unitsToPlace;

  /** Creates new PlacePanel. */
  public PlacePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map, frame);
    undoableMovesPanel = new UndoablePlacementsPanel(this);
    unitsToPlace = new SimpleUnitPanel(map.getUiContext());
    leftToPlaceLabel.setText("Units left to place:");
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id, " place");
  }

  private void refreshActionLabelText(final boolean bid) {
    SwingUtilities
        .invokeLater(() -> actionLabel.setText(getCurrentPlayer().getName() + " place" + (bid ? " for bid" : "")));
  }

  PlaceData waitForPlace(final boolean bid, final IPlayerBridge playerBridge) {
    setUp(playerBridge);
    // workaround: meant to be in setUpSpecific, but it requires a variable
    refreshActionLabelText(bid);
    waitForRelease();
    cleanUp();
    return placeData;
  }

  private boolean canProduceFightersOnCarriers() {
    return Properties.getProduceFightersOnCarriers(getData());
  }

  private boolean canProduceNewFightersOnOldCarriers() {
    return Properties.getProduceNewFightersOnOldCarriers(getData());
  }

  private boolean isLhtrCarrierProductionRules() {
    return Properties.getLhtrCarrierProductionRules(getData());
  }

  private final MapSelectionListener placeMapSelectionListener = new DefaultMapSelectionListener() {
    @Override
    public void territorySelected(final Territory territory, final MouseDetails e) {
      if (!getActive() || (e.getButton() != MouseEvent.BUTTON1)) {
        return;
      }
      final int[] maxUnits = new int[1];
      final Collection<Unit> units = getUnitsToPlace(territory, maxUnits);
      if (units.isEmpty()) {
        return;
      }
      final UnitChooser chooser = new UnitChooser(units, Collections.emptyMap(), false, getMap().getUiContext());
      final String messageText = "Place units in " + territory.getName();
      if (maxUnits[0] >= 0) {
        chooser.setMaxAndShowMaxButton(maxUnits[0]);
      }
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      final int availHeight = screenResolution.height - 120;
      final int availWidth = screenResolution.width - 40;
      final JScrollPane scroll = new JScrollPane(chooser);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.setPreferredSize(new Dimension(
          ((scroll.getPreferredSize().width > availWidth) ? availWidth
              : (scroll.getPreferredSize().width + ((scroll.getPreferredSize().height > availHeight) ? 20 : 0))),
          ((scroll.getPreferredSize().height > availHeight) ? availHeight
              : (scroll.getPreferredSize().height + ((scroll.getPreferredSize().width > availWidth) ? 26 : 0)))));
      final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, messageText,
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
      if (option == JOptionPane.OK_OPTION) {
        final Collection<Unit> choosen = chooser.getSelected();
        placeData = new PlaceData(choosen, territory);
        updateUnits();
        if (choosen.containsAll(units)) {
          leftToPlaceLabel.setText("");
        }
        release();
      }
    }
  };

  private Collection<Unit> getUnitsToPlace(final Territory territory, final int[] maxUnits) {
    getData().acquireReadLock();
    try {
      // not our territory
      if (!territory.isWater() && !territory.getOwner().equals(getCurrentPlayer())) {
        if (GameStepPropertiesHelper.isBid(getData())) {
          final PlayerAttachment pa = PlayerAttachment.get(territory.getOwner());
          if (((pa == null) || (pa.getGiveUnitControl() == null) || !pa.getGiveUnitControl()
              .contains(getCurrentPlayer()))
              && !territory.getUnits().anyMatch(Matches.unitIsOwnedBy(getCurrentPlayer()))) {
            return Collections.emptyList();
          }
        } else {
          return Collections.emptyList();
        }
      }
      // get the units that can be placed on this territory.
      Collection<Unit> units = getCurrentPlayer().getUnits().getUnits();
      if (territory.isWater()) {
        if (!(canProduceFightersOnCarriers() || canProduceNewFightersOnOldCarriers()
            || isLhtrCarrierProductionRules() || GameStepPropertiesHelper.isBid(getData()))) {
          units = CollectionUtils.getMatches(units, Matches.unitIsSea());
        } else {
          final Predicate<Unit> unitIsSeaOrCanLandOnCarrier = Matches.unitIsSea().or(Matches.unitCanLandOnCarrier());
          units = CollectionUtils.getMatches(units, unitIsSeaOrCanLandOnCarrier);
        }
      } else {
        units = CollectionUtils.getMatches(units, Matches.unitIsNotSea());
      }
      if (units.isEmpty()) {
        return Collections.emptyList();
      }
      final IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
      final PlaceableUnits production = placeDel.getPlaceableUnits(units, territory);
      if (production.isError()) {
        JOptionPane.showMessageDialog(getTopLevelAncestor(), production.getErrorMessage(), "No units",
            JOptionPane.INFORMATION_MESSAGE);
        return Collections.emptyList();
      }
      maxUnits[0] = production.getMaxUnits();
      return production.getUnits();
    } finally {
      getData().releaseReadLock();
    }
  }

  private void updateUnits() {
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(getCurrentPlayer().getUnits().getUnits());
    unitsToPlace.setUnitsFromCategories(unitCategories);
  }

  @Override
  public String toString() {
    return "PlacePanel";
  }

  @Override
  protected final void cancelMoveAction() {
    getMap().showMouseCursor();
    getMap().setMouseShadowUnits(null);
  }

  @Override
  protected final void undoMoveSpecific() {
    leftToPlaceLabel.setText("Units left to place:");
    updateUnits();
  }

  @Override
  protected final void cleanUpSpecific() {
    getMap().removeMapSelectionListener(placeMapSelectionListener);
  }

  @Override
  protected final void setUpSpecific() {
    getMap().addMapSelectionListener(placeMapSelectionListener);
  }

  @Override
  protected boolean doneMoveAction() {
    if (getCurrentPlayer().getUnits().size() > 0) {
      final int option = JOptionPane.showConfirmDialog(getTopLevelAncestor(),
          "You have not placed all your units yet.  Are you sure you want to end your turn?", "TripleA",
          JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
      // TODO COMCO add code here to store the units until next time
      if (option != JOptionPane.YES_OPTION) {
        return false;
      }
    }
    placeData = null;
    return true;
  }

  @Override
  protected boolean setCancelButton() {
    return false;
  }

  @Override
  protected final void addAdditionalButtons() {
    add(leftBox(leftToPlaceLabel));
    add(unitsToPlace);
    updateUnits();
  }
}
