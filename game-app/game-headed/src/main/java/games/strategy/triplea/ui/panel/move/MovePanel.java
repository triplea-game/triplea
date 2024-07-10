package games.strategy.triplea.ui.panel.move;

import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableList;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.AbstractMoveDelegate.MoveType;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.battle.ScrambleLogic;
import games.strategy.triplea.delegate.data.MustMoveWithDetails;
import games.strategy.triplea.delegate.move.validation.MoveValidator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.AbstractMovePanel;
import games.strategy.triplea.ui.DefaultMapSelectionListener;
import games.strategy.triplea.ui.MouseDetails;
import games.strategy.triplea.ui.SimpleUnitPanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UndoableMovesPanel;
import games.strategy.triplea.ui.UnitChooser;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.panels.map.MapSelectionListener;
import games.strategy.triplea.ui.panels.map.MouseOverUnitListener;
import games.strategy.triplea.ui.panels.map.UnitSelectionListener;
import games.strategy.triplea.ui.unit.scroller.UnitScroller;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.ObjectUtils;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.CollapsiblePanel;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** The action panel displayed during the combat and non-combat move actions. */
public class MovePanel extends AbstractMovePanel {
  private static final long serialVersionUID = 5004515340964828564L;
  private static final int defaultMinTransportCost = 5;

  /** Number of units to add/remove when Alt key is down. */
  private static final int MULTI_SELECT_NUMBER = 10;

  // Map from air transport to units being transported for the current move being made.
  private final Map<Unit, Collection<Unit>> airTransportDependents = new HashMap<>();

  // access only through getter and setter!
  @Getter private @Nullable Territory firstSelectedTerritory;
  @Getter @Setter private @Nullable Territory selectedEndpointTerritory;
  private Territory mouseCurrentTerritory;
  private @Nullable List<Territory> forced;
  @Setter private boolean nonCombat;
  private Point mouseSelectedPoint;
  private Point mouseCurrentPoint;
  private Point mouseLastUpdatePoint;
  // use a LinkedHashSet because we want to know the order
  private final Set<Unit> selectedUnits = new LinkedHashSet<>();
  // the must move with details for the currently selected territory
  // note this is kept in sync because we do not modify selectedTerritory directly
  // instead we only do so through the private setter
  private @Nullable MustMoveWithDetails mustMoveWithDetails = null;
  // cache this so we can update it only when territory/units change
  private Collection<Unit> unitsThatCanMoveOnRoute;
  private @Nullable Image currentCursorImage;
  private final @Nullable Image warningImage;
  private final @Nullable Image errorImage;
  private @Nullable Route routeCached = null;
  private String displayText = "Combat Move";
  @Setter private MoveType moveType = MoveType.DEFAULT;
  private final UnitScroller unitScroller;

  @Getter(onMethod_ = @Override)
  private final CollapsiblePanel unitScrollerPanel;

  private boolean isHighlightingUnits;

  private final UnitSelectionListener unitSelectionListener =
      new UnitSelectionListener() {
        @Override
        public void unitsSelected(
            final List<Unit> units, final Territory t, final MouseDetails mouseDetails) {
          if (!getListening()) {
            return;
          }
          // check if we can handle this event, are we active?
          if (!isActive()) {
            return;
          }
          if (t == null) {
            return;
          }
          final boolean rightMouse = mouseDetails.isRightButton();
          final boolean isMiddleMouseButton = mouseDetails.getButton() == MouseEvent.BUTTON2;
          final boolean noSelectedTerritory = (firstSelectedTerritory == null);
          final boolean isFirstSelectedTerritory = Objects.equals(firstSelectedTerritory, t);
          // select units
          final GameData data = getData();
          try (GameData.Unlocker ignored = data.acquireReadLock()) {
            // de select units
            if (rightMouse && !noSelectedTerritory && !map.wasLastActionDraggingAndReset()) {
              deselectUnits(units, t, mouseDetails);
            } else if (!isMiddleMouseButton
                && !rightMouse
                && (noSelectedTerritory || isFirstSelectedTerritory)) {
              selectUnitsToMove(units, t, mouseDetails);
            } else if (!rightMouse && mouseDetails.isControlDown() && !isFirstSelectedTerritory) {
              selectWayPoint(t);
            } else if (!rightMouse
                && !noSelectedTerritory
                && !isFirstSelectedTerritory
                && !isMiddleMouseButton) {
              selectEndPoint(t);
            }
          }
          getMap().requestFocusInWindow();
        }

        private void selectUnitsToMove(
            final List<Unit> units, final Territory t, final MouseDetails mouseDetails) {
          if (!canSelectUnits(units)) {
            return;
          }

          // basic match criteria only
          final Predicate<Unit> unitsToMoveMatch = getMovableMatch(null, List.of());
          if (units.isEmpty() && selectedUnits.isEmpty() && !mouseDetails.isShiftDown()) {
            final List<Unit> unitsToMove = t.getMatches(unitsToMoveMatch);
            if (unitsToMove.isEmpty()) {
              return;
            }
            // matcher to prevent units of different owners being chosen (relevant for edit mode)
            final Predicate<Collection<Unit>> unitsHaveSameOwner =
                unitsToCheck -> {
                  final GamePlayer owner = CollectionUtils.getAny(unitsToCheck).getOwner();
                  return unitsToCheck.stream().allMatch(Matches.unitIsOwnedBy(owner));
                };
            final UnitChooser chooser =
                new UnitChooser(
                    unitsToMove,
                    selectedUnits,
                    null,
                    UnitSeparator.SeparatorCategories.builder().build(),
                    false,
                    getMap().getUiContext(),
                    unitsHaveSameOwner);
            if (!confirmUnitChooserDialog(chooser, "Select units to move from " + t.getName())) {
              return;
            }
            final List<Unit> chosenUnits = chooser.getSelected(false);
            if (chosenUnits.isEmpty()) {
              return;
            }
            selectedUnits.addAll(chosenUnits);
          }
          if (getFirstSelectedTerritory() == null) {
            setFirstSelectedTerritory(t);
            mouseSelectedPoint = mouseDetails.getMapPoint();
            mouseCurrentPoint = mouseDetails.getMapPoint();
            enableCancelButton();
          }
          if (!getFirstSelectedTerritory().equals(t)) {
            throw new IllegalStateException("Wrong selected territory");
          }
          // add all
          if (mouseDetails.isShiftDown()) {
            // prevent units of multiple owners from being chosen in edit mode
            final PredicateBuilder<Unit> ownedNotFactoryBuilder = PredicateBuilder.trueBuilder();
            if (!isEditMode()) {
              ownedNotFactoryBuilder.and(unitsToMoveMatch);
            } else if (!selectedUnits.isEmpty()) {
              ownedNotFactoryBuilder
                  .and(unitsToMoveMatch)
                  .and(Matches.unitIsOwnedBy(getUnitOwner(selectedUnits)));
            } else {
              ownedNotFactoryBuilder
                  .and(unitsToMoveMatch)
                  .and(Matches.unitIsOwnedBy(getUnitOwner(t.getUnits())));
            }
            selectedUnits.addAll(t.getMatches(ownedNotFactoryBuilder.build()));
          } else if (mouseDetails.isControlDown()) {
            selectedUnits.addAll(CollectionUtils.getMatches(units, unitsToMoveMatch));
          } else { // add one
            // best candidate unit for route is chosen dynamically later
            // check for alt key - add 10 units (useful for splitting large armies)
            final int maxCount = mouseDetails.isAltDown() ? MULTI_SELECT_NUMBER : 1;
            units.stream()
                .filter(unitsToMoveMatch)
                .filter(not(selectedUnits::contains))
                .sorted(UnitComparator.getHighestToLowestMovementComparator())
                .limit(maxCount)
                .forEachOrdered(selectedUnits::add);
          }
          if (!selectedUnits.isEmpty()) {
            map.notifyUnitsAreSelected();
            mouseLastUpdatePoint = mouseDetails.getMapPoint();
            final Route route = getRoute(getFirstSelectedTerritory(), t, selectedUnits);
            // Load Bombers with paratroops
            if ((!nonCombat
                    || Properties.getParatroopersCanMoveDuringNonCombat(getData().getProperties()))
                && getCurrentPlayer().getTechAttachment().getParatroopers()
                && selectedUnits.stream()
                    .anyMatch(Matches.unitIsAirTransport().and(Matches.unitHasNotMoved()))) {
              final GamePlayer player = getCurrentPlayer();
              // TODO Transporting allied units
              // Get the potential units to load
              final Predicate<Unit> unitsToLoadMatch =
                  Matches.unitIsAirTransportable()
                      .and(Matches.unitIsOwnedBy(player))
                      .and(Matches.unitHasNotMoved());
              final Collection<Unit> unitsToLoad =
                  CollectionUtils.getMatches(route.getStart().getUnits(), unitsToLoadMatch);
              unitsToLoad.removeAll(selectedUnits);
              for (final Collection<Unit> units2 : airTransportDependents.values()) {
                unitsToLoad.removeAll(units2);
              }
              // Get the potential air transports to load
              final Predicate<Unit> candidateAirTransportsMatch =
                  Matches.unitIsAirTransport()
                      .and(Matches.unitIsOwnedBy(player))
                      .and(Matches.unitHasNotMoved())
                      .and(transport -> transport.getTransporting(t).isEmpty());
              final Collection<Unit> candidateAirTransports =
                  t.getMatches(unitsToMoveMatch.and(candidateAirTransportsMatch));
              candidateAirTransports.removeAll(airTransportDependents.keySet());
              if (!unitsToLoad.isEmpty() && !candidateAirTransports.isEmpty()) {
                final Collection<Unit> airTransportsToLoad =
                    getAirTransportsToLoad(candidateAirTransports);
                if (!airTransportsToLoad.isEmpty()) {
                  selectedUnits.addAll(airTransportsToLoad);
                  final Collection<Unit> loadedAirTransports =
                      getLoadedAirTransports(route, unitsToLoad, airTransportsToLoad, player);
                  selectedUnits.addAll(loadedAirTransports);
                }
              }
            }
            updateUnitsThatCanMoveOnRoute(selectedUnits, route);
            updateRouteAndMouseShadowUnits(route);
          } else {
            setFirstSelectedTerritory(null);
          }
        }

        private boolean canSelectUnits(final List<Unit> units) {
          final GamePlayer requiredOwner;
          if (!isEditMode()) {
            requiredOwner = getCurrentPlayer();
          } else if (!selectedUnits.isEmpty()) {
            // In edit mode, only allow units that match the existing selection.
            requiredOwner = CollectionUtils.getAny(selectedUnits).getOwner();
          } else {
            return true;
          }
          return units.stream().allMatch(Matches.unitIsOwnedBy(requiredOwner));
        }

        public Collection<Unit> getAirTransportsToLoad(
            final Collection<Unit> candidateAirTransports) {
          final Set<Unit> defaultSelections = new HashSet<>();
          // prevent too many bombers from being selected
          final Predicate<Collection<Unit>> transportsToLoadMatch =
              units -> {
                final Collection<Unit> airTransports =
                    CollectionUtils.getMatches(units, Matches.unitIsAirTransport());
                return (airTransports.size() <= candidateAirTransports.size());
              };
          // Allow player to select which to load.
          final UnitChooser chooser =
              new UnitChooser(
                  candidateAirTransports,
                  defaultSelections,
                  airTransportDependents,
                  UnitSeparator.SeparatorCategories.builder().movement(true).build(),
                  false,
                  getMap().getUiContext(),
                  transportsToLoadMatch);
          chooser.setAllButtonVisible(false);
          chooser.setTitle("Select air transports to load");
          if (!confirmUnitChooserDialog(chooser, "What transports do you want to load")) {
            return List.of();
          }
          return chooser.getSelected(true);
        }

        /**
         * Allow the user to select what units to load. If null is returned, the move should be
         * canceled.
         */
        public Collection<Unit> getLoadedAirTransports(
            final Route route,
            final Collection<Unit> capableUnitsToLoad,
            final Collection<Unit> capableTransportsToLoad,
            final GamePlayer player) {
          // Get the minimum transport cost of a candidate unit
          int minTransportCost = Integer.MAX_VALUE;
          for (final Unit unit : capableUnitsToLoad) {
            minTransportCost =
                Math.min(minTransportCost, unit.getUnitAttachment().getTransportCost());
          }
          final Collection<Unit> airTransportsToLoad = new ArrayList<>();
          for (final Unit bomber : capableTransportsToLoad) {
            final int capacity = TransportTracker.getAvailableCapacity(bomber);
            if (capacity >= minTransportCost) {
              airTransportsToLoad.add(bomber);
            }
          }
          // If no airTransports can be loaded, return the empty set
          if (airTransportsToLoad.isEmpty()) {
            return List.of();
          }
          // Check to see if there's room for the selected units
          final Predicate<Collection<Unit>> unitsToLoadMatch =
              units -> {
                final Collection<Unit> unitsToLoad =
                    CollectionUtils.getMatches(units, Matches.unitIsAirTransportable());
                final Map<Unit, Unit> unitMap =
                    TransportUtils.mapTransportsToLoad(unitsToLoad, airTransportsToLoad);
                return unitMap.keySet().containsAll(unitsToLoad);
              };
          // Get a list of the units that could be loaded on the transport (based upon transport
          // capacity)
          final List<Unit> unitsToLoad =
              TransportUtils.findUnitsToLoadOnAirTransports(
                  capableUnitsToLoad, airTransportsToLoad);
          final Set<Unit> defaultSelections = new HashSet<>();
          List<Unit> loadedUnits =
              userChooseUnits(defaultSelections, unitsToLoadMatch, unitsToLoad);
          final Map<Unit, Unit> mapping =
              TransportUtils.mapTransportsToLoad(loadedUnits, airTransportsToLoad);
          for (final Unit unit : mapping.keySet()) {
            final Collection<Unit> unitsColl = new ArrayList<>();
            unitsColl.add(unit);
            final Unit airTransport = mapping.get(unit);
            if (airTransportDependents.containsKey(airTransport)) {
              unitsColl.addAll(airTransportDependents.get(airTransport));
            }
            airTransportDependents.put(airTransport, unitsColl);
            mustMoveWithDetails =
                MoveValidator.getMustMoveWith(route.getStart(), airTransportDependents, player);
          }
          return loadedUnits;
        }

        private void deselectUnits(
            final List<Unit> initialUnits, final Territory t, final MouseDetails me) {
          final Collection<Unit> unitsToRemove = new ArrayList<>(selectedUnits.size());
          // we have right clicked on a unit stack in a different territory
          final List<Unit> units = getFirstSelectedTerritory().equals(t) ? initialUnits : List.of();
          // remove the dependent units so we don't have to micromanage them
          final List<Unit> unitsWithoutDependents = new ArrayList<>(selectedUnits);
          for (final Unit unit : selectedUnits) {
            final Collection<Unit> forced = mustMoveWithDetails.getMustMoveWith().get(unit);
            if (forced != null) {
              unitsWithoutDependents.removeAll(forced);
            }
          }
          // no unit selected, remove the most recent, but skip dependents
          if (units.isEmpty()) {
            if (me.isControlDown()) {
              selectedUnits.clear();
              // Clear the stored dependents for AirTransports
              airTransportDependents.clear();
            } else if (!unitsWithoutDependents.isEmpty()) {
              // check for alt key - remove 10 units (useful for splitting large armies)
              final int removeCount = me.isAltDown() ? MULTI_SELECT_NUMBER : 1;
              // remove the last removeCount elements
              for (int i = 0; i < removeCount; i++) {
                unitsToRemove.add(unitsWithoutDependents.get(unitsWithoutDependents.size() - 1));
                // Clear the stored dependents for AirTransports
                if (!airTransportDependents.isEmpty()) {
                  for (final Unit airTransport : unitsWithoutDependents) {
                    if (airTransportDependents.containsKey(airTransport)) {
                      unitsToRemove.addAll(airTransportDependents.get(airTransport));
                      airTransportDependents.remove(airTransport);
                    }
                  }
                }
              }
            }
          } else { // we have actually clicked on a specific unit
            // remove all if control is down
            if (me.isControlDown()) {
              unitsToRemove.addAll(units);
              // Clear the stored dependents for AirTransports
              if (!airTransportDependents.isEmpty()) {
                for (final Unit airTransport : unitsWithoutDependents) {
                  if (airTransportDependents.containsKey(airTransport)) {
                    unitsToRemove.addAll(airTransportDependents.get(airTransport));
                    airTransportDependents.remove(airTransport);
                  }
                }
              }
            } else { // remove one
              if (!getFirstSelectedTerritory().equals(t)) {
                throw new IllegalStateException("Wrong selected territory");
              }
              // doesn't matter which unit we remove since units are assigned to routes later
              // check for alt key - remove 10 units (useful for splitting large armies)
              final int maxCount = me.isAltDown() ? MULTI_SELECT_NUMBER : 1;
              int remCount = 0;
              for (final Unit unit : units) {
                if (selectedUnits.contains(unit) && !unitsToRemove.contains(unit)) {
                  unitsToRemove.add(unit);
                  // Clear the stored dependents for AirTransports
                  if (!airTransportDependents.isEmpty()) {
                    for (final Unit airTransport : unitsWithoutDependents) {
                      if (airTransportDependents.containsKey(airTransport)) {
                        airTransportDependents.get(airTransport).remove(unit);
                      }
                    }
                  }
                  remCount++;
                  if (remCount >= maxCount) {
                    break;
                  }
                }
              }
            }
          }
          // perform the remove
          selectedUnits.removeAll(unitsToRemove);
          if (selectedUnits.isEmpty()) {
            // nothing left, cancel move
            cancelMove();
          } else {
            mouseLastUpdatePoint = me.getMapPoint();
            updateUnitsThatCanMoveOnRoute(
                selectedUnits, getRoute(getFirstSelectedTerritory(), t, selectedUnits));
            updateRouteAndMouseShadowUnits(getRoute(getFirstSelectedTerritory(), t, selectedUnits));
          }
        }

        private void selectWayPoint(final Territory territory) {
          if (forced == null) {
            forced = new ArrayList<>();
          }
          if (!forced.contains(territory)) {
            forced.add(territory);
          }
          updateRouteAndMouseShadowUnits(
              getRoute(getFirstSelectedTerritory(), getFirstSelectedTerritory(), selectedUnits));
        }

        private Predicate<Unit> getUnloadableMatch() {
          // are we unloading everything? if we are then we don't need to select the transports
          return PredicateBuilder.of(Matches.unitIsOwnedBy(getCurrentPlayer()))
              .and(Matches.unitIsLand())
              .andIf(nonCombat, Matches.unitCanNotMoveDuringCombatMove().negate())
              .build();
        }

        private void selectEndPoint(final Territory territory) {
          final Route route = getRoute(getFirstSelectedTerritory(), territory, selectedUnits);
          if (unitsThatCanMoveOnRoute.isEmpty() || route == null) {
            cancelMove();
            return;
          }
          if (!confirmEndPoint(territory)) {
            return;
          }
          setSelectedEndpointTerritory(territory);
          Collection<Unit> seaTransports = null;
          final var units = new ArrayList<>(unitsThatCanMoveOnRoute);
          if (route.isLoad() && units.stream().anyMatch(Matches.unitIsLand())) {
            seaTransports = getSeaTransportsToLoad(route, units);
            if (seaTransports.isEmpty()) {
              cancelMove();
              return;
            }
          } else if (route.isUnload() && units.stream().anyMatch(Matches.unitIsLand())) {
            units.clear();
            // Have user select which transports and land units to unload
            units.addAll(
                getUnitsToUnload(
                    route, CollectionUtils.getMatches(selectedUnits, getUnloadableMatch())));
            // Add in non-unloadable units (air) which can move along unloading route
            units.addAll(
                CollectionUtils.getMatches(unitsThatCanMoveOnRoute, getUnloadableMatch().negate()));
            if (units.isEmpty()) {
              cancelMove();
              return;
            }
            selectedUnits.clear();
            selectedUnits.addAll(units);
          } else {
            // keep a map of the max number of each eligible unitType that can be chosen
            final IntegerMap<UnitType> maxMap = new IntegerMap<>();
            for (final Unit unit : units) {
              maxMap.add(unit.getType(), 1);
            }
            // this match will make sure we can't select more units of a specific type then we had
            // originally selected
            final Predicate<Collection<Unit>> unitTypeCountMatch =
                unitsToCheck -> {
                  final IntegerMap<UnitType> currentMap = new IntegerMap<>();
                  for (final Unit unit : unitsToCheck) {
                    currentMap.add(unit.getType(), 1);
                  }
                  return maxMap.greaterThanOrEqualTo(currentMap);
                };
            allowSpecificUnitSelection(units, route, unitTypeCountMatch);
            if (units.isEmpty()) {
              cancelMove();
              return;
            }
          }
          final Map<Unit, Unit> unitsToSeaTransports =
              seaTransports == null
                  ? Map.of()
                  : TransportUtils.mapTransports(route, units, seaTransports);
          // If different air units were selected in the confirm dialog than the ones transporting,
          // remove them from the dependents map.
          airTransportDependents.keySet().removeIf(not(units::contains));
          final MoveDescription message =
              new MoveDescription(units, route, unitsToSeaTransports, airTransportDependents);
          setMoveMessage(message);
          setFirstSelectedTerritory(null);
          setSelectedEndpointTerritory(null);
          airTransportDependents.clear();
          mouseCurrentTerritory = null;
          forced = null;
          updateRouteAndMouseShadowUnits(null);
          release();
        }

        private boolean confirmEndPoint(final Territory territory) {
          if (!ClientSetting.showPotentialScrambleWarning.getValueOrThrow()
              || !willStartBattle(territory)) {
            return true;
          }
          if (!Properties.getScrambleRulesInEffect(getData().getProperties())) {
            return true;
          }
          final var scrambleLogic = new ScrambleLogic(getData(), getCurrentPlayer(), territory);
          final Collection<Unit> possibleScramblers = scrambleLogic.getUnitsThatCanScramble();
          return possibleScramblers.isEmpty()
              || showScrambleWarningAndConfirmMove(possibleScramblers);
        }

        private boolean willStartBattle(final Territory territory) {
          final GamePlayer player = getCurrentPlayer();
          return Matches.territoryHasUnitsOwnedBy(player)
              .negate()
              .and(Matches.territoryHasEnemyUnits(player))
              .test(territory);
        }

        private boolean showScrambleWarningAndConfirmMove(
            final Collection<Unit> possibleScramblers) {
          final SimpleUnitPanel unitPanel =
              new SimpleUnitPanel(
                  getMap().getUiContext(),
                  SimpleUnitPanel.Style.SMALL_ICONS_WRAPPED_WITH_LABEL_WHEN_EMPTY);
          unitPanel.setUnitsFromCategories(UnitSeparator.categorize(possibleScramblers));
          final String message = "Warning: Units may scramble from nearby territories to defend:";
          final JPanel panel =
              new JPanelBuilder()
                  .borderLayout()
                  .addNorth(JLabelBuilder.builder().text(message).build())
                  .addCenter(unitPanel)
                  .addSouth(JLabelBuilder.builder().text("Confirm move?").build())
                  .build();
          // The following is needed to help the unitPanel compute its initial height with
          // multiple units present. Without it, an extra row is added based on a too narrow
          // width initially.
          panel.setSize(new Dimension(400, 100));
          JCheckBox dontWarnAgain = new JCheckBox("Don't show this warning again");
          dontWarnAgain.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
          JPanel outer =
              new JPanelBuilder().borderLayout().addCenter(panel).addSouth(dontWarnAgain).build();
          final int option =
              JOptionPane.showConfirmDialog(
                  JOptionPane.getFrameForComponent(MovePanel.this),
                  outer,
                  "Scramble Warning",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE);
          if (dontWarnAgain.isSelected()) {
            ClientSetting.showPotentialScrambleWarning.setValue(false);
          }
          return option == JOptionPane.OK_OPTION;
        }
      };

  private final MouseOverUnitListener mouseOverUnitListener =
      new MouseOverUnitListener() {
        @Override
        public void mouseEnter(final List<Unit> units, final Territory territory) {
          if (!getListening()) {
            return;
          }
          final GamePlayer owner = getUnitOwner(selectedUnits);
          final Predicate<Unit> match = Matches.unitIsOwnedBy(owner).and(Matches.unitCanMove());
          final boolean someOwned = units.stream().anyMatch(match);
          final boolean isCorrectTerritory =
              (firstSelectedTerritory == null) || firstSelectedTerritory.equals(territory);
          if (someOwned && isCorrectTerritory) {
            getMap().setUnitHighlight(List.of(Collections.unmodifiableList(units)));
          } else {
            getMap().setUnitHighlight(Set.of());
          }
        }
      };

  private final MapSelectionListener mapSelectionListener =
      new DefaultMapSelectionListener() {
        @Override
        public void mouseMoved(final @Nullable Territory territory, final MouseDetails me) {
          if (!getListening()) {
            return;
          }
          if (getFirstSelectedTerritory() != null && territory != null) {
            Route route;
            if (mouseCurrentTerritory == null
                || !mouseCurrentTerritory.equals(territory)
                || mouseCurrentPoint.equals(mouseLastUpdatePoint)) {
              route = getRoute(getFirstSelectedTerritory(), territory, selectedUnits);
              try (GameData.Unlocker ignored = getData().acquireReadLock()) {
                updateUnitsThatCanMoveOnRoute(selectedUnits, route);
                // now, check if there is a better route for just the units that can get there (we
                // check only air since that
                // is the only one for which the route may actually change much)
                if (unitsThatCanMoveOnRoute.size() < selectedUnits.size()
                    && (unitsThatCanMoveOnRoute.isEmpty()
                        || unitsThatCanMoveOnRoute.stream().allMatch(Matches.unitIsAir()))) {
                  final Collection<Unit> airUnits =
                      CollectionUtils.getMatches(selectedUnits, Matches.unitIsAir());
                  if (!airUnits.isEmpty()) {
                    route = getRoute(getFirstSelectedTerritory(), territory, airUnits);
                    updateUnitsThatCanMoveOnRoute(airUnits, route);
                  }
                }
              }
            } else {
              route = routeCached;
            }
            mouseCurrentPoint = me.getMapPoint();
            updateRouteAndMouseShadowUnits(route);
          }
          mouseCurrentTerritory = territory;
        }
      };

  public MovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map, frame);

    undoableMovesPanel = new UndoableMovesPanel(this);
    mouseCurrentTerritory = null;
    unitsThatCanMoveOnRoute = List.of();
    currentCursorImage = null;
    warningImage = getMap().getWarningImage().orElse(null);
    errorImage = getMap().getErrorImage().orElse(null);

    unitScroller = new UnitScroller(getData(), getMap(), this::isVisible);
    unitScrollerPanel = unitScroller.build();
    unitScrollerPanel.setVisible(false);
    registerKeyBindings(frame);
  }

  private GamePlayer getUnitOwner(final Collection<Unit> units) {
    return (isEditMode() && units != null && !units.isEmpty())
        ? CollectionUtils.getAny(units).getOwner()
        : getCurrentPlayer();
  }

  /** Sorts the specified units in preferred movement or unload order. */
  private void sortUnitsToMove(final List<Unit> units, final Route route) {
    if (!units.isEmpty()) {
      units.sort(getUnitsToMoveComparator(units, route));
    }
  }

  private Comparator<Unit> getUnitsToMoveComparator(final List<Unit> units, final Route route) {
    // sort units based on which transports are allowed to unload
    if (route.isUnload() && units.stream().anyMatch(Matches.unitIsLand())) {
      return UnitComparator.getUnloadableUnitsComparator(units, route, getUnitOwner(units));
    } else {
      return UnitComparator.getMovableUnitsComparator(units, route);
    }
  }

  /** Sort the specified transports in preferred load order. */
  private void sortTransportsToLoad(final List<Unit> transports, final Route route) {
    if (transports.isEmpty()) {
      return;
    }
    transports.sort(
        UnitComparator.getLoadableTransportsComparator(route, getUnitOwner(transports)));
  }

  /** Sort the specified transports in preferred unload order. */
  private void sortTransportsToUnload(final List<Unit> transports, final Route route) {
    if (transports.isEmpty()) {
      return;
    }
    transports.sort(
        UnitComparator.getUnloadableTransportsComparator(route, getUnitOwner(transports), true));
  }

  /**
   * Return the units that are to be unloaded for this route. If needed will ask the user what
   * transports to unload. This is needed because the user needs to be able to select what
   * transports to unload in the case where some transports have different movement, different units
   * etc
   */
  private Collection<Unit> getUnitsToUnload(
      final Route route, final Collection<Unit> unitsToUnload) {
    final Collection<Unit> allUnits = getFirstSelectedTerritory().getUnits();
    final List<Unit> candidateUnits =
        CollectionUtils.getMatches(allUnits, getUnloadableMatch(route, unitsToUnload));
    if (unitsToUnload.size() == candidateUnits.size()) {
      return ImmutableList.copyOf(unitsToUnload);
    }
    final List<Unit> candidateTransports =
        CollectionUtils.getMatches(
            allUnits, Matches.unitIsTransportingSomeCategories(candidateUnits));

    // Remove all incapable transports
    final Collection<Unit> incapableTransports =
        CollectionUtils.getMatches(
            candidateTransports, Matches.transportCannotUnload(route.getEnd()));
    candidateTransports.removeAll(incapableTransports);
    if (candidateTransports.isEmpty()) {
      return List.of();
    }

    if (candidateTransports.size() == 1) {
      // Only one transport after filtering out incapable ones. Don't show a dialog but still run
      // the unload algorithm to substitute units on incapable transports with ones on capable ones.
      return chooseUnitsToUnload(route, unitsToUnload, candidateUnits, candidateTransports);
    }

    // Are the transports all of the same type and if they are, then don't ask
    final Collection<UnitCategory> categories =
        UnitSeparator.categorize(
            candidateTransports,
            UnitSeparator.SeparatorCategories.builder()
                .dependents(mustMoveWithDetails.getMustMoveWith())
                .movement(true)
                .build());
    if (categories.size() == 1) {
      // All transports of the same type, don't show a dialog but still run the unload algorithm
      // so that units on incapable transports are replaced with units on capable ones.
      return chooseUnitsToUnload(route, unitsToUnload, candidateUnits, candidateTransports);
    }
    sortTransportsToUnload(candidateTransports, route);

    // unitsToUnload are actually dependents, but need to select transports
    final Set<Unit> defaultSelections =
        TransportUtils.findMinTransportsToUnload(unitsToUnload, candidateTransports);

    // Match criteria to ensure that chosen transports will match selected units
    final Predicate<Collection<Unit>> transportsToUnloadMatch =
        units -> {
          final List<Unit> sortedTransports =
              CollectionUtils.getMatches(units, Matches.unitIsSeaTransport());
          final Collection<Unit> availableUnits = new ArrayList<>(unitsToUnload);

          // track the changing capacities of the transports as we assign units
          final IntegerMap<Unit> capacityMap = new IntegerMap<>();
          for (final Unit transport : sortedTransports) {
            final Collection<Unit> transporting = transport.getTransporting();
            capacityMap.add(transport, TransportUtils.getTransportCost(transporting));
          }
          boolean hasChanged;
          final Comparator<Unit> increasingCapacityComparator =
              UnitComparator.getIncreasingCapacityComparator();

          // This algorithm will ensure that it is actually possible to distribute
          // the selected units amongst the current selection of chosen transports.
          do {
            hasChanged = false;

            // Sort transports by increasing capacity
            sortedTransports.sort(increasingCapacityComparator);

            // Try to remove one unit from each transport, in succession
            final Iterator<Unit> transportIter = sortedTransports.iterator();
            while (transportIter.hasNext()) {
              final Unit transport = transportIter.next();
              final Collection<Unit> transporting = transport.getTransporting();
              if (transporting == null) {
                continue;
              }
              final Collection<UnitCategory> transCategories =
                  UnitSeparator.categorize(transporting);
              final Iterator<Unit> unitIter = availableUnits.iterator();
              while (unitIter.hasNext()) {
                final Unit unit = unitIter.next();
                final Collection<UnitCategory> unitCategory =
                    UnitSeparator.categorize(Set.of(unit));

                // Is one of the transported units of the same type we want to unload?
                if (!Collections.disjoint(transCategories, unitCategory)) {

                  // Unload the unit, remove the transport from our list, and continue
                  hasChanged = true;
                  unitIter.remove();
                  transportIter.remove();
                  break;
                }
              }
            }
            // Repeat until there are no units left or no changes occur
          } while (!availableUnits.isEmpty() && hasChanged);

          // If we haven't seen all of the transports (and removed them) then there are extra
          // transports that don't fit
          return sortedTransports.isEmpty();
        };

    // Choosing what transports to unload
    final UnitChooser chooser =
        new UnitChooser(
            candidateTransports,
            defaultSelections,
            mustMoveWithDetails.getMustMoveWith(),
            UnitSeparator.SeparatorCategories.builder().movement(true).build(),
            false,
            getMap().getUiContext(),
            transportsToUnloadMatch);
    chooser.setAllButtonVisible(false);
    if (!confirmUnitChooserDialog(chooser, "Select transports to unload")) {
      return List.of();
    }
    final Collection<Unit> chosenTransports =
        CollectionUtils.getMatches(chooser.getSelected(), Matches.unitIsSeaTransport());
    return chooseUnitsToUnload(route, unitsToUnload, candidateUnits, chosenTransports);
  }

  private List<Unit> chooseUnitsToUnload(
      final Route route,
      final Collection<Unit> unitsToUnload,
      final Collection<Unit> candidateUnits,
      final Collection<Unit> chosenTransports) {
    final List<Unit> allUnitsInSelectedTransports = new ArrayList<>();
    for (final Unit transport : chosenTransports) {
      final Collection<Unit> transporting = transport.getTransporting();
      if (transporting != null) {
        allUnitsInSelectedTransports.addAll(transporting);
      }
    }
    allUnitsInSelectedTransports.retainAll(candidateUnits);
    sortUnitsToMove(allUnitsInSelectedTransports, route);
    final List<Unit> selectedUnitsToUnload = new ArrayList<>();
    final List<Unit> sortedTransports = new ArrayList<>(chosenTransports);
    sortedTransports.sort(UnitComparator.getIncreasingCapacityComparator());
    final Collection<Unit> selectedUnits = new ArrayList<>(unitsToUnload);

    // First pass: choose one unit from each selected transport
    for (final Unit transport : sortedTransports) {
      boolean hasChanged = false;
      final Iterator<Unit> selectedIter = selectedUnits.iterator();
      while (selectedIter.hasNext()) {
        final Unit selected = selectedIter.next();
        final Collection<Unit> transporting = transport.getTransporting();
        for (final Unit candidate : transporting) {
          if (selected.getType().equals(candidate.getType())
              && selected.isOwnedBy(candidate.getOwner())
              && selected.getHits() == candidate.getHits()) {
            hasChanged = true;
            selectedUnitsToUnload.add(candidate);
            allUnitsInSelectedTransports.remove(candidate);
            selectedIter.remove();
            break;
          }
        }
        if (hasChanged) {
          break;
        }
      }
    }

    // Now fill remaining slots in preferred unit order
    for (final Unit selected : selectedUnits) {
      final Iterator<Unit> candidateIter = allUnitsInSelectedTransports.iterator();
      while (candidateIter.hasNext()) {
        final Unit candidate = candidateIter.next();
        if (selected.getType().equals(candidate.getType())
            && selected.isOwnedBy(candidate.getOwner())
            && selected.getHits() == candidate.getHits()) {
          selectedUnitsToUnload.add(candidate);
          candidateIter.remove();
          break;
        }
      }
    }
    // Return an unmodifiable list for consistent semantics for the caller, which sometimes returns
    // List.of() and sometimes the result of this functiom.
    return Collections.unmodifiableList(selectedUnitsToUnload);
  }

  public boolean confirmUnitChooserDialog(final UnitChooser chooser, final String title) {
    final int option =
        JOptionPane.showOptionDialog(
            getTopLevelAncestor(),
            chooser,
            title,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            null);
    return option == JOptionPane.OK_OPTION;
  }

  private Predicate<Unit> getUnloadableMatch(final Route route, final Collection<Unit> units) {
    return getMovableMatch(route, units).and(Matches.unitIsLand());
  }

  private Predicate<Unit> getMovableMatch(final Route route, final Collection<Unit> units) {
    final PredicateBuilder<Unit> movableBuilder = PredicateBuilder.trueBuilder();
    if (!isEditMode()) {
      movableBuilder.and(Matches.unitIsOwnedBy(getCurrentPlayer()));
    }
    /*
     * if you do not have selection of zero-movement units enabled,
     * this will restrict selection to units with 1 or more movement
     */
    if (!Properties.getSelectableZeroMovementUnits(getData().getProperties())) {
      movableBuilder.and(Matches.unitCanMove());
    }
    if (!nonCombat) {
      movableBuilder.and(Matches.unitCanNotMoveDuringCombatMove().negate());
    }
    if (route != null) {
      final Predicate<Unit> enoughMovement =
          u -> isEditMode() || (u.getMovementLeft().compareTo(route.getMovementCost(u)) >= 0);

      if (route.isUnload()) {
        final Predicate<Unit> notLandAndCanMove = enoughMovement.and(Matches.unitIsNotLand());
        final Predicate<Unit> landOrCanMove = Matches.unitIsLand().or(notLandAndCanMove);
        movableBuilder.and(landOrCanMove);
      } else {
        movableBuilder.and(enoughMovement);
      }
      final boolean water = route.getEnd().isWater();
      if (water && !route.isLoad()) {
        movableBuilder.and(Matches.unitIsNotLand());
      }
      if (!water) {
        movableBuilder.and(Matches.unitIsNotSea());
      }
    }
    if (!units.isEmpty()) {
      // force all units to have the same owner in edit mode
      final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(getUnitOwner(units));
      if (isEditMode()) {
        movableBuilder.and(ownedBy);
      }
      // Filter to only units with the same types as the input list.
      final Set<UnitType> typesOfOwnedUnits =
          units.stream().filter(ownedBy).map(Unit::getType).collect(Collectors.toSet());
      movableBuilder.and(u -> typesOfOwnedUnits.contains(u.getType()));
    }
    return movableBuilder.build();
  }

  private boolean isEditMode() {
    return EditDelegate.getEditMode(getData().getProperties());
  }

  private Route getRoute(
      final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    try (GameData.Unlocker ignored = getData().acquireReadLock()) {
      return (forced == null)
          ? getRouteNonForced(start, end, selectedUnits)
          : getRouteForced(start, end, selectedUnits);
    }
  }

  /** Get the route including the territories that we are forced to move through. */
  private Route getRouteForced(
      final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    if (forced == null || forced.isEmpty()) {
      throw new IllegalStateException(
          "No forced territories:" + forced + " end:" + end + " start:" + start);
    }
    Territory last = getFirstSelectedTerritory();

    Route total = new Route(last);
    for (final Territory current : forced) {
      final Route add = getRouteNonForced(last, current, selectedUnits);
      final Route newTotal = Route.join(total, add);
      if (newTotal == null) {
        return total;
      }
      total = newTotal;
      last = current;
    }
    if (!end.equals(last)) {
      final Route add = getRouteNonForced(last, end, selectedUnits);
      final Route newTotal = Route.join(total, add);
      if (newTotal != null) {
        total = newTotal;
      }
    }
    return total;
  }

  /** Get the route ignoring forced territories. */
  private @Nullable Route getRouteNonForced(
      final Territory start, final Territory end, final Collection<Unit> selectedUnits) {
    // can't rely on current player being the unit owner in Edit Mode
    // look at the units being moved to determine allies and enemies
    final GamePlayer owner = getUnitOwner(selectedUnits);
    return MoveValidator.getBestRoute(
        start,
        end,
        getData(),
        owner,
        selectedUnits,
        !GameStepPropertiesHelper.isAirborneMove(getData()));
  }

  private void updateUnitsThatCanMoveOnRoute(
      final Collection<Unit> units, final @Nullable Route route) {
    if (route == null || route.hasNoSteps()) {
      clearStatusMessage();
      getMap().showMouseCursor();
      currentCursorImage = null;
      unitsThatCanMoveOnRoute = new ArrayList<>(units);
      return;
    }
    getMap().hideMouseCursor();

    final MovableUnitsFilter unitsFilter =
        new MovableUnitsFilter(
            getData(),
            getUnitOwner(units),
            route,
            nonCombat,
            moveType,
            getUndoableMoves(),
            airTransportDependents);
    Collection<Unit> candidateUnits = TransportUtils.chooseEquivalentUnitsToUnload(route, units);
    final var result = unitsFilter.filterUnitsThatCanMove(candidateUnits);
    switch (result.getStatus()) {
      case NO_UNITS_CAN_MOVE:
        setStatusErrorMessage(result.getWarningOrErrorMessage().orElseThrow());
        currentCursorImage = errorImage;
        break;
      case SOME_UNITS_CAN_MOVE:
        setStatusWarningMessage(result.getWarningOrErrorMessage().orElseThrow());
        currentCursorImage = warningImage;
        break;
      case ALL_UNITS_CAN_MOVE:
        if (result.getUnitsWithDependents().containsAll(selectedUnits)) {
          clearStatusMessage();
          currentCursorImage = null;
        } else {
          setStatusWarningMessage("Not all units can move there");
          currentCursorImage = warningImage;
        }
        break;
      default:
        throw new IllegalStateException("unknown status");
    }

    if (unitsThatCanMoveOnRoute.size() != new HashSet<>(unitsThatCanMoveOnRoute).size()) {
      cancelMove();
      return;
    }
    unitsThatCanMoveOnRoute = result.getUnitsWithDependents();
  }

  /** Route can be null. */
  final void updateRouteAndMouseShadowUnits(final @Nullable Route route) {
    routeCached = route;
    getMap().setRoute(route, mouseSelectedPoint, mouseCurrentPoint, currentCursorImage);
    if (route == null) {
      getMap().setMouseShadowUnits(null);
    } else {
      getMap().setMouseShadowUnits(unitsThatCanMoveOnRoute);
    }
  }

  /**
   * Allow the user to select what transports to load. If an empty collection is returned, the move
   * should be canceled.
   */
  private Collection<Unit> getSeaTransportsToLoad(
      final Route route, final Collection<Unit> unitsToLoad) {
    if (!route.isLoad()) {
      return List.of();
    }
    if (unitsToLoad.stream().anyMatch(Matches.unitIsAir())) {
      return List.of();
    }
    final GamePlayer unitOwner = getUnitOwner(unitsToLoad);
    final MustMoveWithDetails endMustMoveWith =
        MoveValidator.getMustMoveWith(route.getEnd(), airTransportDependents, unitOwner);
    int minTransportCost = defaultMinTransportCost;
    for (final Unit unit : unitsToLoad) {
      minTransportCost = Math.min(minTransportCost, unit.getUnitAttachment().getTransportCost());
    }
    final Predicate<Unit> candidateSeaTransportsMatch =
        Matches.unitIsSeaTransport().and(Matches.alliedUnit(unitOwner));
    final List<Unit> candidateSeaTransports =
        CollectionUtils.getMatches(route.getEnd().getUnits(), candidateSeaTransportsMatch);

    // remove transports that don't have enough capacity
    final Iterator<Unit> transportIter = candidateSeaTransports.iterator();
    while (transportIter.hasNext()) {
      final Unit transport = transportIter.next();
      final int capacity = TransportTracker.getAvailableCapacity(transport);
      if (capacity < minTransportCost) {
        transportIter.remove();
      }
    }

    // nothing to choose
    if (candidateSeaTransports.isEmpty()) {
      return List.of();
    }

    // sort transports in preferred load order
    sortTransportsToLoad(candidateSeaTransports, route);
    final List<Unit> availableUnits = new ArrayList<>(unitsToLoad);
    final IntegerMap<Unit> availableCapacityMap = new IntegerMap<>();
    for (final Unit transport : candidateSeaTransports) {
      final int capacity = TransportTracker.getAvailableCapacity(transport);
      availableCapacityMap.put(transport, capacity);
    }
    final Set<Unit> defaultSelections = new HashSet<>();

    // Algorithm to choose defaultSelections (transports to load)
    // We are trying to determine which transports are the best defaults to select for loading,
    // and so we need a modified algorithm based strictly on candidateTransports order:
    // - owned, capable transports are chosen first; attempt to fill them
    // - allied, capable transports are chosen next; attempt to fill them
    // - finally, incapable transports are chosen last (will generate errors)
    // Note that if any allied transports qualify as defaults, we will always prompt with a
    // UnitChooser later on so that it is obvious to the player.
    boolean useAlliedTransports = false;
    final Collection<Unit> capableSeaTransports = new ArrayList<>(candidateSeaTransports);

    // only allow incapable transports for updateUnitsThatCanMoveOnRoute
    // so that we can have a nice UI error shown if these transports are selected, since it may not
    // be obvious
    final Collection<Unit> incapableTransports =
        CollectionUtils.getMatches(
            capableSeaTransports, Matches.transportCannotUnload(route.getEnd()));
    capableSeaTransports.removeAll(incapableTransports);
    final Predicate<Unit> alliedMatch = Matches.unitIsOwnedBy(unitOwner).negate();
    final Collection<Unit> alliedTransports =
        CollectionUtils.getMatches(capableSeaTransports, alliedMatch);
    capableSeaTransports.removeAll(alliedTransports);

    // First, load capable transports
    final Map<Unit, Unit> unitsToCapableTransports =
        TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, capableSeaTransports);
    for (final Unit unit : unitsToCapableTransports.keySet()) {
      final Unit transport = unitsToCapableTransports.get(unit);
      final int unitCost = unit.getUnitAttachment().getTransportCost();
      availableCapacityMap.add(transport, (-1 * unitCost));
      defaultSelections.add(transport);
    }
    availableUnits.removeAll(unitsToCapableTransports.keySet());

    // Next, load allied transports
    final Map<Unit, Unit> unitsToAlliedTransports =
        TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, alliedTransports);
    for (final Unit unit : unitsToAlliedTransports.keySet()) {
      final Unit transport = unitsToAlliedTransports.get(unit);
      final int unitCost = unit.getUnitAttachment().getTransportCost();
      availableCapacityMap.add(transport, (-1 * unitCost));
      defaultSelections.add(transport);
      useAlliedTransports = true;
    }
    availableUnits.removeAll(unitsToAlliedTransports.keySet());

    // only allow incapable transports for updateUnitsThatCanMoveOnRoute
    // so that we can have a nice UI error shown if these transports are selected, since it may not
    // be obvious
    if (getSelectedEndpointTerritory() == null) {
      final Map<Unit, Unit> unitsToIncapableTransports =
          TransportUtils.mapTransportsToLoadUsingMinTransports(availableUnits, incapableTransports);
      for (final Unit unit : unitsToIncapableTransports.keySet()) {
        final Unit transport = unitsToIncapableTransports.get(unit);
        final int unitCost = unit.getUnitAttachment().getTransportCost();
        availableCapacityMap.add(transport, (-1 * unitCost));
        defaultSelections.add(transport);
      }
      availableUnits.removeAll(unitsToIncapableTransports.keySet());
    } else {
      candidateSeaTransports.removeAll(incapableTransports);
    }

    // force UnitChooser to pop up if we are choosing allied transports
    if (!useAlliedTransports) {
      if (candidateSeaTransports.size() == 1) {
        return candidateSeaTransports;
      }
      // all the same type, don't ask unless we have more than 1 unit type
      if (UnitSeparator.categorize(
                      candidateSeaTransports,
                      UnitSeparator.SeparatorCategories.builder()
                          .dependents(endMustMoveWith.getMustMoveWith())
                          .movement(true)
                          .build())
                  .size()
              == 1
          && unitsToLoad.size() == 1) {
        return candidateSeaTransports;
      }
      // If we've filled all transports, then no user intervention is required.
      // It is possible to make "wrong" decisions if there are mixed unit types and
      // mixed transport categories, but there is no UI to manage that anyway.
      // Players will need to load incrementally in such cases.
      if (defaultSelections.containsAll(candidateSeaTransports)) {
        return candidateSeaTransports;
      }
    }

    // the match criteria to ensure that chosen transports will match selected units
    final Predicate<Collection<Unit>> seaTransportsToLoadMatch =
        units -> {
          final Collection<Unit> transports =
              CollectionUtils.getMatches(units, Matches.unitIsSeaTransport());
          // prevent too many transports from being selected
          return transports.size() <= Math.min(unitsToLoad.size(), candidateSeaTransports.size());
        };
    final UnitChooser chooser =
        new UnitChooser(
            candidateSeaTransports,
            defaultSelections,
            endMustMoveWith.getMustMoveWith(),
            UnitSeparator.SeparatorCategories.builder().movement(true).build(),
            false,
            getMap().getUiContext(),
            seaTransportsToLoadMatch);
    chooser.setAllButtonVisible(false);
    if (!confirmUnitChooserDialog(chooser, "Select transports to load")) {
      return List.of();
    }
    return chooser.getSelected(false);
  }

  /**
   * Allow the user to select specific units, if for example some units have different movement.
   * Units are sorted in preferred order, so units represents the default selections.
   */
  private void allowSpecificUnitSelection(
      final Collection<Unit> units,
      final Route route,
      final Predicate<Collection<Unit>> matchCriteria) {
    final List<Unit> candidateUnits =
        getFirstSelectedTerritory().getMatches(getMovableMatch(route, units));
    final Set<UnitCategory> categories =
        UnitSeparator.categorize(
            candidateUnits,
            UnitSeparator.SeparatorCategories.builder()
                .dependents(mustMoveWithDetails.getMustMoveWith())
                .movement(true)
                .build());
    boolean mustQueryUser = false;
    for (final UnitCategory category1 : categories) {
      // we cant move these, don't bother to check
      if (category1.getMovement().compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      for (final UnitCategory category2 : categories) {
        // we cant move these, don't bother to check
        if (category2.getMovement().compareTo(BigDecimal.ZERO) == 0) {
          continue;
        }
        // if we find that two categories are compatible, and some units are selected from one
        // category, but not the
        // other then the user has to refine his selection
        if (!ObjectUtils.referenceEquals(category1, category2)
            && Objects.equals(category1.getType(), category2.getType())
            && !category1.equals(category2)) {
          // if we are moving all the units from both categories, then nothing to choose
          if (units.containsAll(category1.getUnits()) && units.containsAll(category2.getUnits())) {
            continue;
          }
          // if we are moving some of the units from either category, then we need to stop
          if (!CollectionUtils.intersection(category1.getUnits(), units).isEmpty()
              || !CollectionUtils.intersection(category2.getUnits(), units).isEmpty()) {
            mustQueryUser = true;
          }
        }
      }
    }
    if (mustQueryUser) {
      final List<Unit> defaultSelections = new ArrayList<>(units.size());
      if (route.isLoad()) {
        final Collection<Unit> transportsToLoad = getSeaTransportsToLoad(route, units);
        defaultSelections.addAll(
            TransportUtils.mapTransports(route, units, transportsToLoad).keySet());
      } else {
        defaultSelections.addAll(units);
      }
      // sort candidateUnits in preferred order
      sortUnitsToMove(candidateUnits, route);
      final UnitChooser chooser =
          new UnitChooser(
              candidateUnits,
              defaultSelections,
              mustMoveWithDetails.getMustMoveWith(),
              UnitSeparator.SeparatorCategories.builder().movement(true).build(),
              false,
              getMap().getUiContext(),
              matchCriteria);
      chooser.setAllButtonVisible(false);
      final String text = "Select units to move from " + getFirstSelectedTerritory() + ".";
      if (!confirmUnitChooserDialog(chooser, text)) {
        units.clear();
        return;
      }
      units.clear();
      units.addAll(chooser.getSelected(false));
    }
    // add the dependent units
    final List<Unit> unitsCopy = new ArrayList<>(units);
    for (final Unit unit : unitsCopy) {
      for (final Unit dependent : mustMoveWithDetails.getMustMoveWithForUnit(unit)) {
        if (!unitsCopy.contains(dependent)) {
          units.add(dependent);
        }
      }
    }
  }

  @Override
  public final String toString() {
    return "MovePanel";
  }

  final void setFirstSelectedTerritory(final @Nullable Territory firstSelectedTerritory) {
    if (Objects.equals(this.firstSelectedTerritory, firstSelectedTerritory)) {
      return;
    }
    this.firstSelectedTerritory = firstSelectedTerritory;
    if (firstSelectedTerritory == null) {
      mustMoveWithDetails = null;
    } else {
      mustMoveWithDetails =
          MoveValidator.getMustMoveWith(
              firstSelectedTerritory, airTransportDependents, getCurrentPlayer());
    }
  }

  private List<Unit> userChooseUnits(
      final Set<Unit> defaultSelections,
      final Predicate<Collection<Unit>> unitsToLoadMatch,
      final List<Unit> unitsToLoad) {
    // Allow player to select which to load.
    final UnitChooser chooser =
        new UnitChooser(
            unitsToLoad,
            defaultSelections,
            airTransportDependents,
            UnitSeparator.SeparatorCategories.builder().transportCost(true).build(),
            false,
            getMap().getUiContext(),
            unitsToLoadMatch);
    chooser.setAllButtonVisible(false);
    chooser.setTitle("Load air transports");
    if (!confirmUnitChooserDialog(chooser, "What units do you want to load")) {
      return List.of();
    }
    return chooser.getSelected(true);
  }

  @Override
  protected final void cleanUpSpecific() {
    getMap().removeMapSelectionListener(mapSelectionListener);
    getMap().removeUnitSelectionListener(unitSelectionListener);
    getMap().removeMouseOverUnitListener(mouseOverUnitListener);
    getMap().setUnitHighlight(Set.of());
    selectedUnits.clear();
    updateRouteAndMouseShadowUnits(null);
    forced = null;
    getMap().showMouseCursor();
    unitScrollerPanel.setVisible(false);
  }

  @Override
  protected final void cancelMoveAction() {
    setFirstSelectedTerritory(null);
    setSelectedEndpointTerritory(null);
    mouseCurrentTerritory = null;
    forced = null;
    selectedUnits.clear();
    airTransportDependents.clear();
    currentCursorImage = null;
    updateRouteAndMouseShadowUnits(null);
    getMap().showMouseCursor();
    getMap().setMouseShadowUnits(null);
  }

  @Override
  protected final void undoMoveSpecific() {
    getMap().setRoute(null);
  }

  public final void setDisplayText(final String displayText) {
    this.displayText = displayText;
  }

  @Override
  public final void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer, displayText);
    SwingUtilities.invokeLater(() -> unitScrollerPanel.setVisible(true));
  }

  @Override
  protected final void setUpSpecific() {
    setFirstSelectedTerritory(null);
    forced = null;
    getMap().addMapSelectionListener(mapSelectionListener);
    getMap().addUnitSelectionListener(unitSelectionListener);
    getMap().addMouseOverUnitListener(mouseOverUnitListener);
  }

  private void registerKeyBindings(final JFrame frame) {
    SwingKeyBinding.addKeyBinding(
        frame, KeyCode.S, unitScrollerAction(unitScroller::skipCurrentUnits));
    SwingKeyBinding.addKeyBinding(
        frame, KeyCode.PERIOD, unitScrollerAction(unitScroller::centerOnNextMovableUnit));
    SwingKeyBinding.addKeyBinding(
        frame, KeyCode.COMMA, unitScrollerAction(unitScroller::centerOnPreviousMovableUnit));
    SwingKeyBinding.addKeyBinding(frame, KeyCode.F, this::highlightMovableUnits);
    SwingKeyBinding.addKeyBinding(
        frame,
        KeyCode.U,
        unitScrollerAction(() -> undoableMovesPanel.undoMoves(getMap().getHighlightedUnits())));
  }

  /**
   * Creates an action that only fires when the move panel is visible. Note, the move panel is
   * considered visible if it is part of a tabbed pane and even if it's not the currently selected
   * tab.
   */
  private Runnable unitScrollerAction(final Runnable scrollerAction) {
    return () -> {
      if (this.isVisible()) {
        scrollerAction.run();
      }
    };
  }

  @Override
  protected boolean doneMoveAction() {
    return DoneMoveAction.builder()
        .parentComponent(this)
        .undoableMovesPanel(undoableMovesPanel)
        .unitScrollerPanel(unitScrollerPanel)
        .build()
        .doneMoveAction();
  }

  @Override
  protected boolean setCancelButton() {
    return true;
  }

  /** Highlights movable units on the map for the current player. */
  private void highlightMovableUnits() {
    if (isHighlightingUnits) {
      isHighlightingUnits = false;
      getMap().setUnitHighlight(List.of());
      return;
    }
    final Predicate<Unit> movableUnitOwnedByMe =
        PredicateBuilder.of(Matches.unitIsOwnedBy(getData().getSequence().getStep().getPlayerId()))
            .and(Matches.unitHasMovementLeft())
            // if not non combat, cannot move aa units
            .andIf(!nonCombat, Matches.unitCanNotMoveDuringCombatMove().negate())
            .and(not(unitScroller.getAllSkippedUnits()::contains))
            .build();
    final Collection<Collection<Unit>> highlight = new ArrayList<>();
    for (final Territory t : getData().getMap().getTerritories()) {
      final List<Unit> movableUnits = t.getMatches(movableUnitOwnedByMe);
      if (!movableUnits.isEmpty()) {
        highlight.add(movableUnits);
      }
    }
    if (!highlight.isEmpty()) {
      getMap().setUnitHighlight(highlight);
      isHighlightingUnits = true;
    }
  }
}
