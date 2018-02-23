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
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.TransportUtils;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

class EditPanel extends ActionPanel {
  private static final long serialVersionUID = 5043639777373556106L;
  private final TripleAFrame frame;
  private final Action performMoveAction;
  private final Action addUnitsAction;
  private final Action delUnitsAction;
  private final Action changePUsAction;
  private final Action addTechAction;
  private final Action removeTechAction;
  private final Action changeUnitHitDamageAction;
  private final Action changeUnitBombingDamageAction;
  private final Action changeTerritoryOwnerAction;
  private final Action changePoliticalRelationships;
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
    final JLabel actionLabel = new JLabel();
    performMoveAction = new AbstractAction("Perform Move or Other Actions") {
      private static final long serialVersionUID = 2205085537962024476L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        EditPanel.this.frame.showActionPanelTab();
        cancelEditAction.actionPerformed(null);
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
        final boolean mustChoose;
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
          final List<Unit> allOfCorrectType = CollectionUtils.getMatches(allUnits,
              o -> selectedUnitTypes.contains(o.getType()));
          final int allCategories =
              UnitSeperator.categorize(allOfCorrectType, mustMoveWithDetails.getMustMoveWith(), true, true).size();
          final int selectedCategories =
              UnitSeperator.categorize(selectedUnits, mustMoveWithDetails.getMustMoveWith(), true, true).size();
          mustChoose = (allCategories != selectedCategories);
        }
        final Collection<Unit> bestUnits;
        if (mustChoose) {
          final String chooserText = "Remove units from " + selectedTerritory + ":";
          final UnitChooser chooser = new UnitChooser(allUnits, selectedUnits, mustMoveWithDetails.getMustMoveWith(),
              true, false, /* allowMultipleHits= */false, getMap().getUiContext());
          final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, chooserText,
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
          if (option != JOptionPane.OK_OPTION) {
            cancelEditAction.actionPerformed(null);
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
        cancelEditAction.actionPerformed(null);
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
            new PlayerChooser(getData().getPlayerList(), getMap().getUiContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner PUs to change");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        getData().acquireReadLock();
        final Resource pus;
        try {
          pus = getData().getResourceList().getResource(Constants.PUS);
        } finally {
          getData().releaseReadLock();
        }
        if (pus == null) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final int oldTotal = player.getResources().getQuantity(pus);
        final JTextField pusField = new JTextField(String.valueOf(oldTotal), 4);
        pusField.setMaximumSize(pusField.getPreferredSize());
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), new JScrollPane(pusField),
            "Select new number of PUs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        int newTotal = oldTotal;
        try {
          newTotal = Integer.parseInt(pusField.getText());
        } catch (final Exception e) {
          // ignore malformed input
        }
        final String result = EditPanel.this.frame.getEditDelegate().changePUs(player, newTotal);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        cancelEditAction.actionPerformed(null);
      }
    };
    addTechAction = new AbstractAction("Add Technology") {
      private static final long serialVersionUID = -5536151512828077755L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), getMap().getUiContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to get technology");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        getData().acquireReadLock();
        final Collection<TechAdvance> techs;
        try {
          techs = TechnologyDelegate.getAvailableTechs(player, data);
        } finally {
          getData().releaseReadLock();
        }
        if (techs.isEmpty()) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final JList<TechAdvance> techList = new JList<>(SwingComponents.newListModel(techs));
        techList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        techList.setLayoutOrientation(JList.VERTICAL);
        techList.setVisibleRowCount(10);
        final JScrollPane scroll = new JScrollPane(techList);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to add",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final Set<TechAdvance> techAdvances = new HashSet<>(techList.getSelectedValuesList());
        final String result = EditPanel.this.frame.getEditDelegate().addTechAdvance(player, techAdvances);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        cancelEditAction.actionPerformed(null);
      }
    };
    removeTechAction = new AbstractAction("Remove Technology") {
      private static final long serialVersionUID = -2456111915025687825L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), getMap().getUiContext(), false);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to remove technology");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player == null) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        getData().acquireReadLock();
        final Collection<TechAdvance> techs;
        try {
          techs = TechTracker.getCurrentTechAdvances(player, data);
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
        if (techs.isEmpty()) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final JList<TechAdvance> techList = new JList<>(SwingComponents.newListModel(techs));
        techList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        techList.setLayoutOrientation(JList.VERTICAL);
        techList.setVisibleRowCount(10);
        final JScrollPane scroll = new JScrollPane(techList);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to remove",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final Set<TechAdvance> techAdvances = new HashSet<>(techList.getSelectedValuesList());
        final String result = EditPanel.this.frame.getEditDelegate().removeTechAdvance(player, techAdvances);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        cancelEditAction.actionPerformed(null);
      }
    };
    changeUnitHitDamageAction = new AbstractAction("Change Unit Hit Damage") {
      private static final long serialVersionUID = 1835547345902760810L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final List<Unit> units = CollectionUtils.getMatches(selectedUnits, Matches.unitHasMoreThanOneHitPointTotal());
        if ((units == null) || units.isEmpty() || !selectedTerritory.getUnits().getUnits().containsAll(units)) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        // all owned by one player
        final PlayerID player = units.get(0).getOwner();
        units.retainAll(CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(player)));
        if (units.isEmpty()) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        sortUnitsToRemove(units);
        Collections.sort(units, new UnitBattleComparator(false,
            TuvUtils.getCostsForTuv(player, getData()), null, getData(), true, false));
        Collections.reverse(units);
        // unit mapped to <max, min, current>
        final HashMap<Unit, Triple<Integer, Integer, Integer>> currentDamageMap =
            new HashMap<>();
        for (final Unit u : units) {
          currentDamageMap.put(u, Triple.of(UnitAttachment.get(u.getType()).getHitPoints() - 1, 0, u.getHits()));
        }
        final IndividualUnitPanel unitPanel = new IndividualUnitPanel(currentDamageMap, "Change Unit Hit Damage",
            getData(), getMap().getUiContext(), -1, true, true, null);
        final JScrollPane scroll = new JScrollPane(unitPanel);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Hit Damage",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
        final String result =
            EditPanel.this.frame.getEditDelegate().changeUnitHitDamage(newDamageMap, selectedTerritory);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        cancelEditAction.actionPerformed(null);
      }
    };
    changeUnitBombingDamageAction = new AbstractAction("Change Unit Bombing Damage") {
      private static final long serialVersionUID = 6975869192911780860L;

      @Override
      public void actionPerformed(final ActionEvent event) {
        currentAction = this;
        setWidgetActivation();
        final List<Unit> units = CollectionUtils.getMatches(selectedUnits, Matches.unitCanBeDamaged());
        if ((units == null) || units.isEmpty() || !selectedTerritory.getUnits().getUnits().containsAll(units)) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        // all owned by one player
        final PlayerID player = units.get(0).getOwner();
        units.retainAll(CollectionUtils.getMatches(units, Matches.unitIsOwnedBy(player)));
        if (units.isEmpty()) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        sortUnitsToRemove(units);
        Collections.sort(units, new UnitBattleComparator(false,
            TuvUtils.getCostsForTuv(player, getData()), null, getData(), true, false));
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
            getData(), getMap().getUiContext(), -1, true, true, null);
        final JScrollPane scroll = new JScrollPane(unitPanel);
        final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Bombing Damage",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION) {
          cancelEditAction.actionPerformed(null);
          return;
        }
        final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
        final String result =
            EditPanel.this.frame.getEditDelegate().changeUnitBombingDamage(newDamageMap, selectedTerritory);
        if (result != null) {
          JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
              JOptionPane.ERROR_MESSAGE);
        }
        cancelEditAction.actionPerformed(null);
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
        final PoliticalStateOverview pui =
            new PoliticalStateOverview(getData(), EditPanel.this.frame.getUiContext(), true);
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
                ((scroll.getPreferredSize().width > availWidth) ? availWidth : scroll.getPreferredSize().width),
                ((scroll.getPreferredSize().height > availHeight) ? availHeight : scroll.getPreferredSize().height)));
        final int option = JOptionPane.showConfirmDialog(EditPanel.this.frame, scroll, "Change Political Relationships",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
          final Collection<Triple<PlayerID, PlayerID, RelationshipType>> relationshipChanges = pui.getEditChanges();
          if ((relationshipChanges != null) && !relationshipChanges.isEmpty()) {
            final String result =
                EditPanel.this.frame.getEditDelegate().changePoliticalRelationships(relationshipChanges);
            if (result != null) {
              JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
        }
        cancelEditAction.actionPerformed(null);
      }
    };
    actionLabel.setText("Edit Mode Actions");
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));
    add(actionLabel);
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
      if (allUnitTypes.stream().anyMatch(Matches.unitTypeHasMoreThanOneHitPointTotal())) {
        add(new JButton(changeUnitHitDamageAction));
      }
      if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)
          && allUnitTypes.stream().anyMatch(Matches.unitTypeCanBeDamaged())) {
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
    return Comparator.comparing(TripleAUnit::get, (u1, u2) -> {
      if (UnitAttachment.get(u1.getType()).getTransportCapacity() != -1) {
        // Sort by decreasing transport capacity
        return Comparator.<TripleAUnit, Collection<Unit>>comparing(TripleAUnit::getTransporting,
            Comparator.comparingInt(TransportUtils::getTransportCost).reversed())
            .thenComparingInt(TripleAUnit::getMovementLeft)
            .thenComparingInt(Object::hashCode)
            .compare(u1, u2);
      }
      // Sort by increasing movement left
      return Comparator.comparingInt(TripleAUnit::getMovementLeft)
          .thenComparingInt(Object::hashCode)
          .compare(u1, u2);
    });
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
      addUnitsAction.setEnabled((currentAction == null) && selectedUnits.isEmpty());
      delUnitsAction.setEnabled((currentAction == null) && !selectedUnits.isEmpty());
      changeTerritoryOwnerAction.setEnabled((currentAction == null) && selectedUnits.isEmpty());
      changePUsAction.setEnabled((currentAction == null) && selectedUnits.isEmpty());
      addTechAction.setEnabled((currentAction == null) && selectedUnits.isEmpty());
      removeTechAction.setEnabled((currentAction == null) && selectedUnits.isEmpty());
      changeUnitHitDamageAction.setEnabled((currentAction == null) && !selectedUnits.isEmpty());
      changeUnitBombingDamageAction.setEnabled((currentAction == null) && !selectedUnits.isEmpty());
      changePoliticalRelationships.setEnabled((currentAction == null) && selectedUnits.isEmpty());
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
      getMap().removeMapSelectionListener(mapSelectionListener);
      getMap().removeUnitSelectionListener(unitSelectionListener);
      getMap().removeMouseOverUnitListener(mouseOverUnitListener);
      setWidgetActivation();
    } else if (!this.active && active) {
      getMap().addMapSelectionListener(mapSelectionListener);
      getMap().addUnitSelectionListener(unitSelectionListener);
      getMap().addMouseOverUnitListener(mouseOverUnitListener);
      setWidgetActivation();
    } else if (!active && this.active) {
      getMap().removeMapSelectionListener(mapSelectionListener);
      getMap().removeUnitSelectionListener(unitSelectionListener);
      getMap().removeMouseOverUnitListener(mouseOverUnitListener);
      cancelEditAction.actionPerformed(null);
    }
    this.active = active;
  }

  @Override
  public boolean getActive() {
    return active;
  }

  private final UnitSelectionListener unitSelectionListener = new UnitSelectionListener() {
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
        mapSelectionListener.territorySelected(t, md);
      } else if (!rightMouse) {
        // delete units
        selectUnitsToRemove(units, t, md);
      }
      setWidgetActivation();
    }

    private void deselectUnits(final List<Unit> units, final Territory t, final MouseDetails md) {
      // no unit selected, deselect the most recent
      if (units.isEmpty()) {
        if (md.isControlDown() || (t != selectedTerritory) || selectedUnits.isEmpty()) {
          selectedUnits.clear();
        } else {
          // remove the last element
          selectedUnits.remove(new ArrayList<>(selectedUnits).get(selectedUnits.size() - 1));
        }
      } else { // user has clicked on a specific unit
        // deselect all if control is down
        if (md.isControlDown() || (t != selectedTerritory)) {
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
        cancelEditAction.actionPerformed(null);
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
          final UnitChooser chooser = new UnitChooser(unitsToMove, selectedUnits, null, false, false,
              false, getMap().getUiContext());
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
        cancelEditAction.setEnabled(true);
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

  private final MouseOverUnitListener mouseOverUnitListener = (units, territory, md) -> {
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

  private final MapSelectionListener mapSelectionListener = new DefaultMapSelectionListener() {
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
            new PlayerChooser(getData().getPlayerList(), defaultPlayer, getMap().getUiContext(), true);
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
        SwingUtilities.invokeLater(() -> cancelEditAction.actionPerformed(null));
      } else if (currentAction == addUnitsAction) {
        final boolean allowNeutral = doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, getData());
        final PlayerChooser playerChooser =
            new PlayerChooser(getData().getPlayerList(), territory.getOwner(), getMap().getUiContext(), allowNeutral);
        final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner for new units");
        dialog.setVisible(true);
        final PlayerID player = playerChooser.getSelected();
        if (player != null) {
          // open production panel for adding new units
          final IntegerMap<ProductionRule> production =
              EditProductionPanel.getProduction(player, frame, getData(), getMap().getUiContext());
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
        SwingUtilities.invokeLater(() -> cancelEditAction.actionPerformed(null));
      }
    }

    @Override
    public void mouseMoved(final Territory territory, final MouseDetails md) {
      if (!getActive()) {
        return;
      }
      if (territory != null) {
        if ((currentAction == null) && (selectedTerritory != null)) {
          mouseCurrentPoint = md.getMapPoint();
          getMap().setMouseShadowUnits(selectedUnits);
        }
        // highlight territory
        if ((currentAction == changeTerritoryOwnerAction) || (currentAction == addUnitsAction)) {
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

  private final AbstractAction cancelEditAction = new AbstractAction("Cancel") {
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
