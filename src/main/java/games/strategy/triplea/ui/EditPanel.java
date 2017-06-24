package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
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
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;

class EditPanel extends ActionPanel {
  private static final long serialVersionUID = 5043639777373556106L;
  private TripleAFrame frame;
  private Action performMoveAction;
  private Action addUnitsAction;
  private Action delUnitsAction;
  private Action changePUsAction;
  private Action addTechAction;
  private Action removeTechAction;
  private Action changeUnitHitDamageAction;
  private Action changeUnitBombingDamageAction;
  private Action changeTerritoryOwnerAction;
  private Action changePoliticalRelationships;
  private Action currentAction = null;
  private boolean active = false;
  private Point mouseSelectedPoint;
  private Point mouseCurrentPoint;
  // use a LinkedHashSet because we want to know the order
  private final Set<Unit> selectedUnits = new LinkedHashSet<>();
  private Territory selectedTerritory = null;
  private Territory currentTerritory = null;

  EditPanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    this.frame = frame;
    final JLabel m_actionLabel = new JLabel();
    performMoveAction = new AbstractAction("Perform Move or Other Actions") {
      private static final long serialVersionUID = 2205085537962024476L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        EditPanel.this.frame.showActionPanelTab();
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    addUnitsAction = new AbstractAction("Add Units") {
      private static final long serialVersionUID = 2205085537962024476L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        // TODO: change cursor to select territory
        // continued in territorySelected() handler below
      }
    };
    delUnitsAction = new AbstractAction("Remove Selected Units") {
      private static final long serialVersionUID = 5127470604727907906L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final List<Unit> allUnits = new ArrayList<>(selectedTerritory.getUnits().getUnits());
        sortUnitsToRemove(allUnits);
        final MustMoveWithDetails mustMoveWithDetails;
        try {
          getData().acquireReadLock();
          mustMoveWithDetails = MoveValidator.getMustMoveWith(selectedTerritory, allUnits,
              new HashMap<>(), getData(), getCurrentPlayer());
        } finally {
          getData().releaseReadLock();
        }
        boolean mustChoose = false;
        if (selectedUnits.containsAll(allUnits)) {
          mustChoose = false;
        } else {
          // if the unit choice is ambiguous then ask the user to clarify which units to remove
          // an ambiguous selection would be if the user selects 1 of 2 tanks, but
          // the tanks have different movement.
          final Set<UnitType> selectedUnitTypes = new HashSet<>();
          for (final Unit u : selectedUnits) {
            selectedUnitTypes.add(u.getType());
          }
          final List<Unit> allOfCorrectType = Match.getMatches(allUnits,
              Match.of(o -> selectedUnitTypes.contains(o.getType())));
          final int allCategories =
              UnitSeperator.categorize(allOfCorrectType, mustMoveWithDetails.getMustMoveWith(), true, true).size();
          final int selectedCategories =
              UnitSeperator.categorize(selectedUnits, mustMoveWithDetails.getMustMoveWith(), true, true).size();
          mustChoose = (allCategories != selectedCategories);
        }
        Collection<Unit> bestUnits;
        if (mustChoose) {
          final String chooserText = "Remove units from " + selectedTerritory + ":";
          final UnitChooser chooser = new UnitChooser(allUnits, selectedUnits, mustMoveWithDetails.getMustMoveWith(),
              true, false, getData(), /* allowTwoHit= */false, getMap().getUIContext());
          final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, chooserText,
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
          if (option != JOptionPane.OK_OPTION) {
            CANCEL_EDIT_ACTION.actionPerformed(null);
            return;
          }
          bestUnits = chooser.getSelected(true);
        } else {
          bestUnits = new ArrayList<>(selectedUnits);
        }
        final String result = EditPanel.this.frame.getEditDelegate().removeUnits(selectedTerritory, bestUnits);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result,
              MyFormatter.pluralize("Could not remove unit", selectedUnits.size()), JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    changeTerritoryOwnerAction = new AbstractAction("Change Territory Owner") {
      private static final long serialVersionUID = 8547635747553626362L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        // TODO: change cursor to select territory
        // continued in territorySelected() handler below
      }
    };
    changePUsAction = new AbstractAction("Change PUs") {
      private static final long serialVersionUID = -2751668909341983795L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner PUs to change");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        Resource PUs = null;
        getData().acquireReadLock();
        try {
          PUs = getData().getResourceList().getResource(Constants.PUS);
        } finally {
          getData().releaseReadLock();
        }
        if (PUs == null) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final int oldTotal = player.getResources().getQuantity(PUs);
        int newTotal = oldTotal;
        final JTextField PUsField = new JTextField(String.valueOf(oldTotal), 4);
        PUsField.setMaximumSize(PUsField.getPreferredSize());
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), new JScrollPane(PUsField),
            "Select new number of PUs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        try {
          newTotal = Integer.parseInt(PUsField.getText());
        } catch (final Exception e) {
          // ignore malformed input
        }
        final String result = EditPanel.this.frame.getEditDelegate().changePUs(player, newTotal);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    addTechAction = new AbstractAction("Add Technology") {
      private static final long serialVersionUID = -5536151512828077755L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to get technology");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        Vector<TechAdvance> techs = null;
        getData().acquireReadLock();
        try {
          techs = new Vector<>(TechnologyDelegate.getAvailableTechs(player, data));
        } finally {
          getData().releaseReadLock();
        }
        if (techs == null || techs.isEmpty()) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final JList<?> techList = new JList<>(techs);
        techList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        techList.setLayoutOrientation(JList.VERTICAL);
        techList.setVisibleRowCount(10);
        final JScrollPane scroll = new JScrollPane(techList);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to add",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final Set<TechAdvance> advance = new HashSet<>();
        try {
          for (final Object selection : techList.getSelectedValuesList()) {
            advance.add((TechAdvance) selection);
          }
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
        final String result = EditPanel.this.frame.getEditDelegate().addTechAdvance(player, advance);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    removeTechAction = new AbstractAction("Remove Technology") {
      private static final long serialVersionUID = -2456111915025687825L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to remove technology");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        Vector<TechAdvance> techs = null;
        getData().acquireReadLock();
        try {
          techs = new Vector<>(TechTracker.getCurrentTechAdvances(player, data));
          // there is no way to "undo" these two techs, so do not allow them to be removed
          final Iterator<TechAdvance> iter = techs.iterator();
          while (iter.hasNext()) {
            final TechAdvance ta = iter.next();
            if (ta.getProperty().equals(TechAdvance.TECH_PROPERTY_IMPROVED_SHIPYARDS)
                || ta.getProperty().equals(TechAdvance.TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY)) {
              iter.remove();
            }
          }
        } finally {
          getData().releaseReadLock();
        }
        if (techs == null || techs.isEmpty()) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final JList<?> techList = new JList<>(techs);
        techList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        techList.setLayoutOrientation(JList.VERTICAL);
        techList.setVisibleRowCount(10);
        final JScrollPane scroll = new JScrollPane(techList);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to remove",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final Set<TechAdvance> advance = new HashSet<>();
        try {
          for (final Object selection : techList.getSelectedValuesList()) {
            advance.add((TechAdvance) selection);
          }
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
        }
        final String result = EditPanel.this.frame.getEditDelegate().removeTechAdvance(player, advance);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    changeUnitHitDamageAction = new AbstractAction("Change Unit Hit Damage") {
      private static final long serialVersionUID = 1835547345902760810L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final List<Unit> units = Match.getMatches(selectedUnits, Matches.UnitHasMoreThanOneHitPointTotal);
        if (units == null || units.isEmpty() || !selectedTerritory.getUnits().getUnits().containsAll(units)) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        // all owned by one player
        units.retainAll(Match.getMatches(units, Matches.unitIsOwnedBy(units.iterator().next().getOwner())));
        if (units.isEmpty()) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        sortUnitsToRemove(units);
        Collections.sort(units, new UnitBattleComparator(false,
            BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(getData()), null, getData(), true, false));
        Collections.reverse(units);
        // unit mapped to <max, min, current>
        final HashMap<Unit, Triple<Integer, Integer, Integer>> currentDamageMap =
            new HashMap<>();
        for (final Unit u : units) {
          currentDamageMap.put(u, Triple.of(UnitAttachment.get(u.getType()).getHitPoints() - 1, 0, u.getHits()));
        }
        final IndividualUnitPanel unitPanel = new IndividualUnitPanel(currentDamageMap, "Change Unit Hit Damage",
            getData(), getMap().getUIContext(), -1, true, true, null);
        final JScrollPane scroll = new JScrollPane(unitPanel);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Hit Damage",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
        final String result = EditPanel.this.frame.getEditDelegate().changeUnitHitDamage(newDamageMap, selectedTerritory);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    changeUnitBombingDamageAction = new AbstractAction("Change Unit Bombing Damage") {
      private static final long serialVersionUID = 6975869192911780860L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final List<Unit> units = Match.getMatches(selectedUnits, Matches.UnitCanBeDamaged);
        if (units == null || units.isEmpty() || !selectedTerritory.getUnits().getUnits().containsAll(units)) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        // all owned by one player
        units.retainAll(Match.getMatches(units, Matches.unitIsOwnedBy(units.iterator().next().getOwner())));
        if (units.isEmpty()) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        sortUnitsToRemove(units);
        Collections.sort(units, new UnitBattleComparator(false,
            BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(getData()), null, getData(), true, false));
        Collections.reverse(units);
        // unit mapped to <max, min, current>
        final HashMap<Unit, Triple<Integer, Integer, Integer>> currentDamageMap =
            new HashMap<>();
        for (final Unit u : units) {
          currentDamageMap.put(u,
              Triple.of(((TripleAUnit) u).getHowMuchDamageCanThisUnitTakeTotal(u, selectedTerritory), 0,
                  ((TripleAUnit) u).getUnitDamage()));
        }
        final IndividualUnitPanel unitPanel = new IndividualUnitPanel(currentDamageMap, "Change Unit Bombing Damage",
            getData(), getMap().getUIContext(), -1, true, true, null);
        final JScrollPane scroll = new JScrollPane(unitPanel);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Bombing Damage",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          CANCEL_EDIT_ACTION.actionPerformed(null);
          return;
        }
        final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
        final String result = EditPanel.this.frame.getEditDelegate().changeUnitBombingDamage(newDamageMap, selectedTerritory);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    changePoliticalRelationships = new AbstractAction("Change Political Relationships") {
      private static final long serialVersionUID = -2950034347058147592L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder());
        final JLabel helpText = new JLabel("<html><b>Click the buttons inside the relationship squares to change the "
            + "relationships between players.</b>"
            + "<br />Please note that none of this is validated by the engine or map, so the results are not "
            + "guaranteed to be perfectly what you expect."
            + "<br />In addition, any maps that use triggers could be royalled messed up by changing editing political "
            + "relationships:"
            + "<br /><em>Example: Take a map where America gets some benefit (like upgraded factories) after it goes "
            + "to war for the first time, "
            + "<br />and the american player accidentally clicked to go to war, and now wishes to undo that change. "
            + "Changing America from being at war to "
            + "<br />not being at war will not undo the benefit if it has already happened. And later, when America "
            + "goes to war (for real this time), that "
            + "<br />benefit may or may not be applied a second time, totally depending on how the map was coded (and "
            + "this is not the map's fault either, "
            + "<br />since you are using edit mode).  So if you change anything here, be on the look out for "
            + "unintended consequences!</em></html>");
        panel.add(helpText, BorderLayout.NORTH);
        final PoliticalStateOverview pui = new PoliticalStateOverview(getData(), EditPanel.this.frame.getUIContext(), true);
        panel.add(pui, BorderLayout.CENTER);
        final JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
        // not only do we have a start bar, but we also have the message dialog to account for
        final int availHeight = screenResolution.height - 120;
        // just the scroll bars plus the window sides
        final int availWidth = screenResolution.width - 40;
        scroll.setPreferredSize(
            new Dimension(
                (scroll.getPreferredSize().width > availWidth ? availWidth : scroll.getPreferredSize().width),
                (scroll.getPreferredSize().height > availHeight ? availHeight : scroll.getPreferredSize().height)));
        final int option = JOptionPane.showConfirmDialog(EditPanel.this.frame, scroll, "Change Political Relationships",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
          final Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges = pui.getEditChanges();
          if (relationshipChanges != null && !relationshipChanges.isEmpty()) {
            final String result = EditPanel.this.frame.getEditDelegate().changePoliticalRelationships(relationshipChanges);
            if (result != null) {
              JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
        }
        CANCEL_EDIT_ACTION.actionPerformed(null);
      }
    };
    m_actionLabel.setText("Edit Mode Actions");
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));
    add(m_actionLabel);
    final JButton performMove = new JButton(performMoveAction);
    performMove.setToolTipText("<html>When in Edit Mode, you can perform special actions according to whatever phase "
        + "you are in, by switching back to the 'Action' tab.<br /> "
        + "So if you are in the 'Move' phase, you can move units virtually anywhere, because Edit Mode turns off the "
        + "movement validation.<br /> "
        + "You can use 'Action' tab during Edit Mode to do things not available by the other edit buttons.</html>");
    add(performMove);
    add(new JButton(addUnitsAction));
    add(new JButton(delUnitsAction));
    add(new JButton(changeTerritoryOwnerAction));
    add(new JButton(changePUsAction));
    if (Properties.getTechDevelopment(getData())) {
      add(new JButton(addTechAction));
      add(new JButton(removeTechAction));
    }
    data.acquireReadLock();
    try {
      final Set<UnitType> allUnitTypes = data.getUnitTypeList().getAllUnitTypes();
      if (Match.someMatch(allUnitTypes, Matches.UnitTypeHasMoreThanOneHitPointTotal)) {
        add(new JButton(changeUnitHitDamageAction));
      }
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)
          && Match.someMatch(allUnitTypes, Matches.UnitTypeCanBeDamaged)) {
        add(new JButton(changeUnitBombingDamageAction));
      }
    } finally {
      data.releaseReadLock();
    }
    add(new JButton(changePoliticalRelationships));
    add(Box.createVerticalStrut(15));
    setWidgetActivation();
  }

  private static void sortUnitsToRemove(final List<Unit> units) {
    if (units.isEmpty()) {
      return;
    }
    // sort units based on which transports are allowed to unload
    Collections.sort(units, getRemovableUnitsOrder());
  }

  private static Comparator<Unit> getRemovableUnitsOrder() {
    final Comparator<Unit> removableUnitsOrder = (unit1, unit2) -> {
      final TripleAUnit u1 = TripleAUnit.get(unit1);
      final TripleAUnit u2 = TripleAUnit.get(unit2);
      if (UnitAttachment.get(u1.getType()).getTransportCapacity() != -1) {

        // Sort by decreasing transport capacity
        final Collection<Unit> transporting1 = u1.getTransporting();
        final Collection<Unit> transporting2 = u2.getTransporting();
        final int cost1 = TransportUtils.getTransportCost(transporting1);
        final int cost2 = TransportUtils.getTransportCost(transporting2);
        if (cost1 != cost2) {
          return cost2 - cost1;
        }
      }

      // Sort by increasing movement left
      final int left1 = u1.getMovementLeft();
      final int left2 = u2.getMovementLeft();
      if (left1 != left2) {
        return left1 - left2;
      }

      return Integer.compare(u1.hashCode(), u2.hashCode());
    };
    return removableUnitsOrder;
  }

  private void setWidgetActivation() {
    if (frame.getEditDelegate() == null) {
      // current turn belongs to remote player or AI player
      performMoveAction.setEnabled(false);
      addUnitsAction.setEnabled(false);
      delUnitsAction.setEnabled(false);
      changeTerritoryOwnerAction.setEnabled(false);
      changePUsAction.setEnabled(false);
      addTechAction.setEnabled(false);
      removeTechAction.setEnabled(false);
      changeUnitHitDamageAction.setEnabled(false);
      changeUnitBombingDamageAction.setEnabled(false);
      changePoliticalRelationships.setEnabled(false);
    } else {
      performMoveAction.setEnabled(currentAction == null);
      addUnitsAction.setEnabled(currentAction == null && selectedUnits.isEmpty());
      delUnitsAction.setEnabled(currentAction == null && !selectedUnits.isEmpty());
      changeTerritoryOwnerAction.setEnabled(currentAction == null && selectedUnits.isEmpty());
      changePUsAction.setEnabled(currentAction == null && selectedUnits.isEmpty());
      addTechAction.setEnabled(currentAction == null && selectedUnits.isEmpty());
      removeTechAction.setEnabled(currentAction == null && selectedUnits.isEmpty());
      changeUnitHitDamageAction.setEnabled(currentAction == null && !selectedUnits.isEmpty());
      changeUnitBombingDamageAction.setEnabled(currentAction == null && !selectedUnits.isEmpty());
      changePoliticalRelationships.setEnabled(currentAction == null && selectedUnits.isEmpty());
    }
  }

  @Override
  public String toString() {
    return "EditPanel";
  }

  @Override
  public void setActive(final boolean active) {
    if (frame.getEditDelegate() == null) {
      // current turn belongs to remote player or AI player
      getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
      getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
      getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
      setWidgetActivation();
    } else if (!this.active && active) {
      getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
      getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
      getMap().addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
      setWidgetActivation();
    } else if (!active && this.active) {
      getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
      getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
      getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
      CANCEL_EDIT_ACTION.actionPerformed(null);
    }
    this.active = active;
  }

  @Override
  public boolean getActive() {
    return active;
  }

  private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener() {
    @Override
    public void unitsSelected(final List<Unit> units, final Territory t, final MouseDetails md) {
      // check if we can handle this event, are we active?
      if (!getActive()) {
        return;
      }
      if (t == null) {
        return;
      }
      if (currentAction != null) {
        return;
      }
      final boolean rightMouse = md.isRightButton();
      if (!selectedUnits.isEmpty() && !(selectedTerritory == t)) {
        deselectUnits(new ArrayList<>(selectedUnits), t, md);
        selectedTerritory = null;
      }
      if (rightMouse && (selectedTerritory == t)) {
        deselectUnits(units, t, md);
      }
      if (!rightMouse && (currentAction == addUnitsAction)) {
        // clicking on unit or territory selects territory
        selectedTerritory = t;
        MAP_SELECTION_LISTENER.territorySelected(t, md);
      } else if (!rightMouse) {
        // delete units
        selectUnitsToRemove(units, t, md);
      }
      setWidgetActivation();
    }

    private void deselectUnits(final List<Unit> units, final Territory t, final MouseDetails md) {
      // no unit selected, deselect the most recent
      if (units.isEmpty()) {
        if (md.isControlDown() || t != selectedTerritory || selectedUnits.isEmpty()) {
          selectedUnits.clear();
        } else {
          // remove the last element
          selectedUnits.remove(new ArrayList<>(selectedUnits).get(selectedUnits.size() - 1));
        }
      } else { // user has clicked on a specific unit
        // deselect all if control is down
        if (md.isControlDown() || t != selectedTerritory) {
          selectedUnits.removeAll(units);
        } else { // deselect one
          // remove those with the least movement first
          for (final Unit unit : units) {
            if (selectedUnits.contains(unit)) {
              selectedUnits.remove(unit);
              break;
            }
          }
        }
      }
      // nothing left, cancel edit
      if (selectedUnits.isEmpty()) {
        CANCEL_EDIT_ACTION.actionPerformed(null);
      } else {
        getMap().setMouseShadowUnits(selectedUnits);
      }
    }

    private void selectUnitsToRemove(final List<Unit> units, final Territory t, final MouseDetails md) {
      if (units.isEmpty() && selectedUnits.isEmpty()) {
        if (!md.isShiftDown()) {
          final Collection<Unit> unitsToMove = t.getUnits().getUnits();
          if (unitsToMove.isEmpty()) {
            return;
          }
          final String text = "Remove from " + t.getName();
          final UnitChooser chooser = new UnitChooser(unitsToMove, selectedUnits, null, false, false, getData(),
              false, getMap().getUIContext());
          final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text,
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
          if (option != JOptionPane.OK_OPTION) {
            return;
          }
          if (chooser.getSelected(false).isEmpty()) {
            return;
          }
          selectedUnits.addAll(chooser.getSelected(false));
        }
      }
      if (selectedTerritory == null) {
        selectedTerritory = t;
        mouseSelectedPoint = md.getMapPoint();
        mouseCurrentPoint = md.getMapPoint();
        CANCEL_EDIT_ACTION.setEnabled(true);
      }
      // select all
      if (md.isShiftDown()) {
        selectedUnits.addAll(t.getUnits().getUnits());
      } else if (md.isControlDown()) {
        selectedUnits.addAll(units);
      } else { // select one
        for (final Unit unit : units) {
          if (!selectedUnits.contains(unit)) {
            selectedUnits.add(unit);
            break;
          }
        }
      }
      final Route defaultRoute = getData().getMap().getRoute(selectedTerritory, selectedTerritory);
      getMap().setRoute(defaultRoute, mouseSelectedPoint, mouseCurrentPoint, null);
      getMap().setMouseShadowUnits(selectedUnits);
    }
  };
  private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = (units, territory, md) -> {
    if (!getActive()) {
      return;
    }
    if (currentAction != null) {
      return;
    }
    if (!units.isEmpty()) {
      final Map<Territory, List<Unit>> highlight = new HashMap<>();
      highlight.put(territory, units);
      getMap().setUnitHighlight(highlight);
    } else {
      getMap().setUnitHighlight(null);
    }
  };
  private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {
    @Override
    public void territorySelected(final Territory territory, final MouseDetails md) {
      if (territory == null) {
        return;
      }
      if (currentAction == changeTerritoryOwnerAction) {
        final TerritoryAttachment ta = TerritoryAttachment.get(territory);
        if (ta == null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), "No TerritoryAttachment for " + territory + ".",
              "Could not perform edit", JOptionPane.ERROR_MESSAGE);
          return;
        }
        // PlayerID defaultPlayer = TerritoryAttachment.get(territory).getOriginalOwner();
        final PlayerID defaultPlayer = ta.getOriginalOwner();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), defaultPlayer, getMap().getUIContext(), true);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select new owner for territory");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player != null) {
          final String result = frame.getEditDelegate().changeTerritoryOwner(territory, player);
          if (result != null) {
            JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
                JOptionPane.ERROR_MESSAGE);
          }
        }
        SwingUtilities.invokeLater(() -> CANCEL_EDIT_ACTION.actionPerformed(null));
      } else if (currentAction == addUnitsAction) {
        final boolean allowNeutral = doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, getData());
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), territory.getOwner(), getMap().getUIContext(), allowNeutral);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner for new units");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player != null) {
          // open production panel for adding new units
          final IntegerMap<ProductionRule> production =
              EditProductionPanel.getProduction(player, frame, getData(), getMap().getUIContext());
          final Collection<Unit> units = new ArrayList<>();
          for (final ProductionRule productionRule : production.keySet()) {
            final int quantity = production.getInt(productionRule);
            final NamedAttachable resourceOrUnit = productionRule.getResults().keySet().iterator().next();
            if (!(resourceOrUnit instanceof UnitType)) {
              continue;
            }
            final UnitType type = (UnitType) resourceOrUnit;
            units.addAll(type.create(quantity, player));
          }
          final String result = frame.getEditDelegate().addUnits(territory, units);
          if (result != null) {
            JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
                JOptionPane.ERROR_MESSAGE);
          }
        }
        SwingUtilities.invokeLater(() -> CANCEL_EDIT_ACTION.actionPerformed(null));
      }
    }

    @Override
    public void mouseMoved(final Territory territory, final MouseDetails md) {
      if (!getActive()) {
        return;
      }
      if (territory != null) {
        if (currentAction == null && selectedTerritory != null) {
          mouseCurrentPoint = md.getMapPoint();
          getMap().setMouseShadowUnits(selectedUnits);
        }
        // highlight territory
        if (currentAction == changeTerritoryOwnerAction || currentAction == addUnitsAction) {
          if (currentTerritory != territory) {
            if (currentTerritory != null) {
              getMap().clearTerritoryOverlay(currentTerritory);
            }
            currentTerritory = territory;
            getMap().setTerritoryOverlay(currentTerritory, Color.WHITE, 200);
            getMap().repaint();
          }
        }
      }
    }
  };
  private final AbstractAction CANCEL_EDIT_ACTION = new AbstractAction("Cancel") {
    private static final long serialVersionUID = 6394987295241603443L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      selectedTerritory = null;
      selectedUnits.clear();
      this.setEnabled(false);
      getMap().setRoute(null, mouseSelectedPoint, mouseCurrentPoint, null);
      getMap().setMouseShadowUnits(null);
      if (currentTerritory != null) {
        getMap().clearTerritoryOverlay(currentTerritory);
      }
      currentTerritory = null;
      currentAction = null;
      setWidgetActivation();
    }
  };

  private static boolean doesPlayerHaveUnitsOnMap(final PlayerID player, final GameData data) {
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnits()) {
        if (u.getOwner().equals(player)) {
          return true;
        }
      }
    }
    return false;
  }
}
