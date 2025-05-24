package games.strategy.triplea.ui;

import static java.text.MessageFormat.format;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.SwingComponents;

class PlacePanel extends AbstractMovePanel implements GameDataChangeListener {
  private static final long serialVersionUID = -4411301492537704785L;
  public static final String LBL_UNITS_LEFT_TO_PLACE = "Units left to place:";
  private final JLabel leftToPlaceLabel = createIndentedLabel();
  private transient PlaceData placeData;

  private final SimpleUnitPanel unitsToPlacePanel;

  private GamePlayer lastPlayer;
  private boolean postProductionStep;

  private final transient MapSelectionListener placeMapSelectionListener =
      new DefaultMapSelectionListener() {

        private PlaceableUnits getUnitsToPlace(final Territory territory) {
          try (GameData.Unlocker ignored = getData().acquireReadLock()) {
            // not our territory
            if (!territory.isWater() && !territory.isOwnedBy(getCurrentPlayer())) {
              if (GameStepPropertiesHelper.isBid(getData())) {
                final PlayerAttachment pa = PlayerAttachment.get(territory.getOwner());
                if ((pa == null || !pa.getGiveUnitControl().contains(getCurrentPlayer()))
                    && !territory.anyUnitsMatch(Matches.unitIsOwnedBy(getCurrentPlayer()))) {
                  return new PlaceableUnits();
                }
              } else {
                return new PlaceableUnits();
              }
            }
            // get the units that can be placed on this territory.
            Collection<Unit> units = getCurrentPlayer().getUnits();
            if (territory.isWater()) {
              GameProperties properties = getData().getProperties();
              if (!(Properties.getProduceFightersOnCarriers(properties)
                  || Properties.getProduceNewFightersOnOldCarriers(properties)
                  || Properties.getLhtrCarrierProductionRules(properties)
                  || GameStepPropertiesHelper.isBid(getData()))) {
                units = CollectionUtils.getMatches(units, Matches.unitIsSea());
              } else {
                final Predicate<Unit> unitIsSeaOrCanLandOnCarrier =
                    Matches.unitIsSea().or(Matches.unitCanLandOnCarrier());
                units = CollectionUtils.getMatches(units, unitIsSeaOrCanLandOnCarrier);
              }
            } else {
              units = CollectionUtils.getMatches(units, Matches.unitIsNotSea());
            }
            if (units.isEmpty()) {
              return new PlaceableUnits();
            }
            final IAbstractPlaceDelegate placeDel =
                (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
            final PlaceableUnits production = placeDel.getPlaceableUnits(units, territory);
            if (production.isError()) {
              JOptionPane.showMessageDialog(
                  getTopLevelAncestor(),
                  production.getErrorMessage(),
                  "Cannot produce units",
                  JOptionPane.INFORMATION_MESSAGE);
            }
            return production;
          }
        }

        @Override
        public void territorySelected(final Territory territory, final MouseDetails e) {
          if (!isActive() || (e.getButton() != MouseEvent.BUTTON1)) {
            return;
          }
          final PlaceableUnits placeableUnits = getUnitsToPlace(territory);
          final Collection<Unit> units = placeableUnits.getUnits();
          final int maxUnits = placeableUnits.getMaxUnits();
          if (units.isEmpty()) {
            return;
          }
          final UnitChooser chooser =
              new UnitChooser(units, Map.of(), false, getMap().getUiContext());
          if (maxUnits >= 0) {
            chooser.setMaxAndShowMaxButton(maxUnits);
          }
          final JScrollPane scroll = getScrollPaneFromChooser(chooser);
          final int option =
              JOptionPane.showOptionDialog(
                  getTopLevelAncestor(),
                  scroll,
                  format("Place units in {0}", territory.getName()),
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  null,
                  null);
          if (option == JOptionPane.OK_OPTION) {
            final Collection<Unit> selectedUnits = chooser.getSelected();
            placeData = new PlaceData(selectedUnits, territory);
            updateUnits();
            if (selectedUnits.containsAll(units)) {
              leftToPlaceLabel.setText("");
            }
            release();
          }
        }

        @Nonnull
        private JScrollPane getScrollPaneFromChooser(final UnitChooser chooser) {
          final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
          final int availHeight = screenResolution.height - 120;
          final int availWidth = screenResolution.width - 40;
          final JScrollPane scroll = new JScrollPane(chooser);
          scroll.setBorder(BorderFactory.createEmptyBorder());
          scroll.setPreferredSize(
              new Dimension(
                  (scroll.getPreferredSize().width > availWidth
                      ? availWidth
                      : getPreferredWith(scroll, availHeight)),
                  (scroll.getPreferredSize().height > availHeight
                      ? availHeight
                      : getPreferredHeight(scroll, availWidth))));
          return scroll;
        }

        private int getPreferredHeight(final JScrollPane scroll, final int availWidth) {
          return scroll.getPreferredSize().height
              + (scroll.getPreferredSize().width > availWidth ? 26 : 0);
        }

        private int getPreferredWith(final JScrollPane scroll, final int availHeight) {
          return scroll.getPreferredSize().width
              + (scroll.getPreferredSize().height > availHeight ? 20 : 0);
        }
      };

  PlacePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map, frame);
    undoableMovesPanel = new UndoablePlacementsPanel(this);
    unitsToPlacePanel = new SimpleUnitPanel(map.getUiContext());
    data.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::updateStep);
    leftToPlaceLabel.setText(LBL_UNITS_LEFT_TO_PLACE);
  }

  private void updateStep() {
    final Collection<Unit> unitsToPlace;
    final boolean showUnitsToPlace;
    final GameData data = getData();
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final GameStep step = data.getSequence().getStep();
      if (step == null) {
        return;
      }
      // Note: This doesn't use getCurrentPlayer() as that may not be updated yet.
      final GamePlayer player = step.getPlayerId();
      if (player == null) {
        return;
      }
      final boolean isNewPlayerTurn = !player.equals(lastPlayer);
      if (isNewPlayerTurn) {
        postProductionStep = false;
      }
      final Collection<Unit> playerUnits = player.getUnits();
      // If we're past the production step (even if player didn't produce anything) or
      // there are units that are available to place, show the panel (set unitsToPlace).
      showUnitsToPlace = (postProductionStep || !playerUnits.isEmpty());
      unitsToPlace = showUnitsToPlace ? playerUnits : List.of();
      if (GameStep.isPurchaseOrBidStepName(step.getName())) {
        postProductionStep = true;
      }
      lastPlayer = player;
      // During the place step, listen for changes to update the panel.
      if (GameStep.isPlaceStepName(step.getName())) {
        data.addDataChangeListener(this);
      } else {
        data.removeDataChangeListener(this);
      }
    }

    if (showUnitsToPlace) {
      updateUnitsInUnitsToPlacePanel(unitsToPlace);
    } else {
      SwingUtilities.invokeLater(unitsToPlacePanel::removeAll);
    }
  }

  private void updateUnitsInUnitsToPlacePanel(final Collection<Unit> newUnitsToPlace) {
    // Small hack: copy the unit list before passing it to a new thread.
    // This is to prevent ConcurrentModification. If the 'unitsToPlace' list is modified
    // later in this thread, before "SwingUtilities.invokeLater" can execute and complete,
    // then we will get a ConcurrentModification exception.
    // Ideally we would not modify the 'unitsToPlace' collection again except when
    // the swing thread signals that the user has taken action.
    // Short of that, we create a copy here.
    final Collection<Unit> unitsToPlaceCopy =
        Collections.unmodifiableCollection(new ArrayList<>(newUnitsToPlace));
    SwingUtilities.invokeLater(
        () -> {
          unitsToPlacePanel.setUnits(unitsToPlaceCopy);
          SwingComponents.redraw(unitsToPlacePanel);
        });
  }

  @Override
  public void gameDataChanged(final Change change) {
    final GameData data = getData();
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final GamePlayer player = data.getSequence().getStep().getPlayerId();
      if (player != null) {
        final Collection<Unit> unitsToPlace = player.getUnits();
        updateUnitsInUnitsToPlacePanel(unitsToPlace);
      }
    }
  }

  @Override
  protected Component getUnitScrollerPanel() {
    return new JPanel();
  }

  @Override
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer, " place");
  }

  private void refreshActionLabelText(final boolean bid) {
    SwingUtilities.invokeLater(
        () ->
            actionLabel.setText(
                bid
                    ? format("{0} place for bid", getCurrentPlayer().getName())
                    : format("{0} place", getCurrentPlayer().getName())));
  }

  PlaceData waitForPlace(final boolean bid, final PlayerBridge playerBridge) {
    setUp(playerBridge);
    // workaround: meant to be in setUpSpecific, but it requires a variable
    refreshActionLabelText(bid);
    waitForRelease();
    cleanUp();
    return placeData;
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
    leftToPlaceLabel.setText(LBL_UNITS_LEFT_TO_PLACE);
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
    if (!getCurrentPlayer().getUnitCollection().isEmpty()) {
      final int option =
          JOptionPane.showConfirmDialog(
              getTopLevelAncestor(),
              "You have not placed all your units yet.  Are you sure you want to end your turn?",
              "Confirm End Turn",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.PLAIN_MESSAGE);
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
  protected final List<Component> getAdditionalButtons() {
    updateUnits();
    return Arrays.asList(SwingComponents.leftBox(leftToPlaceLabel), add(unitsToPlacePanel));
  }

  private void updateUnits() {
    updateUnitsInUnitsToPlacePanel(getCurrentPlayer().getUnits());
  }
}
