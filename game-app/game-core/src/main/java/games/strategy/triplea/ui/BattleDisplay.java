package games.strategy.triplea.ui;

import static games.strategy.triplea.image.UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.battle.casualty.CasualtyUtil;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellRenderer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.ScrollableJPanel;
import org.triplea.swing.SwingAction;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

/** Displays a running battle. */
@Slf4j
public class BattleDisplay extends JPanel {
  private static final long serialVersionUID = -7939993104972562765L;

  private static final int MY_WIDTH = 100;
  private static final int MY_HEIGHT = 100;

  @VisibleForTesting final JButton actionButton = new JButton();
  private final GamePlayer defender;
  private final GamePlayer attacker;

  @Getter(AccessLevel.PACKAGE)
  private final Territory battleLocation;

  private final GameData gameData;
  private final MapPanel mapPanel;
  private final UiContext uiContext;

  private final BattleModel defenderModel;
  private final BattleModel attackerModel;
  private BattleStepsPanel steps;
  private DicePanel dicePanel;
  private final CasualtyNotificationPanel casualties;
  private final JPanel messagePanel = new JPanel();
  private final JPanel casualtiesInstantPanelDefender = new JPanel();
  private final JPanel casualtiesInstantPanelAttacker = new JPanel();
  private final JLabel labelNoneAttacker = new JLabel("None");
  private final JLabel labelNoneDefender = new JLabel("None");
  private final JLabel messageLabel = new JLabel();
  private final Action nullAction = SwingAction.of(" ", e -> {});
  @Getter private final String description;

  BattleDisplay(
      final GameData data,
      final Territory territory,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie,
      final Collection<Unit> defendingWaitingToDie,
      final MapPanel mapPanel,
      final BattleType battleType) {
    this.defender = defender;
    this.attacker = attacker;
    this.battleLocation = territory;
    this.gameData = data;
    this.mapPanel = mapPanel;
    this.uiContext = mapPanel.getUiContext();

    final String battleStr =
        (battleType == BattleType.NORMAL)
            ? ""
            : String.format("  (%s)", battleType.toDisplayText());
    this.description =
        attacker.getName()
            + " attacks "
            + defender.getName()
            + " in "
            + battleLocation.getName()
            + battleStr;

    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(territory);
    defenderModel =
        new BattleModel(
            defendingUnits,
            BattleState.Side.DEFENSE,
            battleType,
            gameData,
            territoryEffects,
            uiContext);
    attackerModel =
        new BattleModel(
            attackingUnits,
            BattleState.Side.OFFENSE,
            battleType,
            gameData,
            territoryEffects,
            uiContext);
    defenderModel.setEnemyBattleModel(attackerModel);
    attackerModel.setEnemyBattleModel(defenderModel);
    defenderModel.refresh();
    attackerModel.refresh();
    casualties = new CasualtyNotificationPanel(uiContext);
    if (!killedUnits.isEmpty()
        || !attackingWaitingToDie.isEmpty()
        || !defendingWaitingToDie.isEmpty()) {
      final Collection<Unit> attackerUnitsKilled =
          CollectionUtils.getMatches(killedUnits, Matches.unitIsOwnedBy(attacker));
      attackerUnitsKilled.addAll(attackingWaitingToDie);
      if (!attackerUnitsKilled.isEmpty()) {
        updateKilledUnits(attackerUnitsKilled, attacker);
      }
      final Collection<Unit> defenderUnitsKilled =
          CollectionUtils.getMatches(killedUnits, Matches.unitIsOwnedBy(defender));
      defenderUnitsKilled.addAll(defendingWaitingToDie);
      if (!defenderUnitsKilled.isEmpty()) {
        updateKilledUnits(defenderUnitsKilled, defender);
      }
    }
    initLayout();
  }

  void cleanUp() {
    actionButton.setAction(nullAction);
    steps.wakeAll();
    uiContext.removeShutdownHook(steps::wakeAll);
    steps = null;
  }

  void takeFocus() {
    // we want a component on this frame to take focus so that pressing space will work (since it
    // requires in focused window). Only seems to be an issue on Windows.
    actionButton.requestFocus();
  }

  void bombingResults(final List<Die> dice, final int cost) {
    dicePanel.setDiceRollForBombing(dice, cost);
    casualties.setVisible(false);
  }

  /**
   * updates the panel content according to killed units for the player.
   *
   * @param killedUnits list of units killed
   * @param gamePlayer player kills belongs to
   */
  private Collection<Unit> updateKilledUnits(
      final Collection<Unit> killedUnits, final GamePlayer gamePlayer) {
    final JPanel casualtyPanel;
    if (gamePlayer.equals(defender)) {
      casualtyPanel = casualtiesInstantPanelDefender;
      if (!killedUnits.isEmpty()) {
        casualtyPanel.remove(labelNoneDefender);
      }
    } else {
      casualtyPanel = casualtiesInstantPanelAttacker;
      if (!killedUnits.isEmpty()) {
        casualtyPanel.remove(labelNoneAttacker);
      }
    }
    final Map<Unit, Collection<Unit>> dependentsMap;

    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      dependentsMap = CasualtyUtil.getDependents(killedUnits);
    }
    final Collection<Unit> dependentUnitsReturned = new ArrayList<>();
    for (final Collection<Unit> dependentCollection : dependentsMap.values()) {
      dependentUnitsReturned.addAll(dependentCollection);
    }
    for (final UnitCategory category :
        UnitSeparator.categorize(
            killedUnits,
            UnitSeparator.SeparatorCategories.builder().dependents(dependentsMap).build())) {
      final JPanel panel = new JPanel();
      JLabel unit = uiContext.newUnitImageLabel(category.getType(), category.getOwner());
      panel.add(unit);
      panel.add(new JLabel("x " + category.getUnits().size()));
      for (final UnitOwner owner : category.getDependents()) {
        unit = uiContext.newUnitImageLabel(owner.getType(), owner.getOwner());
        panel.add(unit);
        // TODO this size is of the transport collection size, not the transportED collection size.
        panel.add(new JLabel("x " + category.getUnits().size()));
      }
      casualtyPanel.add(panel);
    }
    return dependentUnitsReturned;
  }

  void casualtyNotification(
      final String step,
      final DiceRoll dice,
      final GamePlayer player,
      final Collection<Unit> killed,
      final Collection<Unit> damaged,
      final Map<Unit, Collection<Unit>> dependents) {
    setStep(step);
    dicePanel.setDiceRoll(dice);
    casualties.setNotification(killed, damaged, dependents);
    killed.addAll(updateKilledUnits(killed, player));
    if (player.equals(defender)) {
      defenderModel.removeCasualties(killed);
    } else {
      attackerModel.removeCasualties(killed);
    }
    casualties.setVisible(true);
  }

  void deadUnitNotification(
      final GamePlayer player,
      final Collection<Unit> killed,
      final Map<Unit, Collection<Unit>> dependents) {
    casualties.setNotificationShort(killed, dependents);
    killed.addAll(updateKilledUnits(killed, player));
    if (player.equals(defender)) {
      defenderModel.removeCasualties(killed);
    } else {
      attackerModel.removeCasualties(killed);
    }
    casualties.setVisible(true);
  }

  void changedUnitsNotification(
      final GamePlayer player,
      final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits) {
    if (player.equals(defender)) {
      if (removedUnits != null) {
        defenderModel.removeCasualties(removedUnits);
      }
      if (addedUnits != null) {
        defenderModel.addUnits(addedUnits);
      }
    } else {
      if (removedUnits != null) {
        attackerModel.removeCasualties(removedUnits);
      }
      if (addedUnits != null) {
        attackerModel.addUnits(addedUnits);
      }
    }
  }

  void waitForConfirmation(final String message) {
    Util.ensureNotOnEventDispatchThread();

    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Action buttonAction = SwingAction.of(message, e -> continueLatch.countDown());
    SwingUtilities.invokeLater(() -> actionButton.setAction(buttonAction));
    uiContext.addShutdownLatch(continueLatch);

    // Set an auto-wait expiration if the option is set or
    // waits for the button to be pressed otherwise.
    if (!ClientSetting.confirmDefensiveRolls.getValueOrThrow()) {
      final int maxWaitTime = 1500;
      Interruptibles.await(() -> continueLatch.await(maxWaitTime, TimeUnit.MILLISECONDS));
    } else {
      Interruptibles.await(continueLatch);
    }

    uiContext.removeShutdownLatch(continueLatch);
    SwingUtilities.invokeLater(() -> actionButton.setAction(nullAction));
  }

  void endBattle(final String message, final Window enclosingFrame) {
    steps.walkToLastStep();
    final Action close =
        SwingAction.of(
            message + " : (Press Space to Close)", e -> enclosingFrame.setVisible(false));
    SwingUtilities.invokeLater(() -> actionButton.setAction(close));
  }

  void notifyRetreat(final Collection<Unit> retreating) {
    defenderModel.notifyRetreat(retreating);
    attackerModel.notifyRetreat(retreating);
  }

  @Nullable
  Territory getRetreat(
      final String message, final Collection<Territory> possible, final boolean submerge) {
    Util.ensureNotOnEventDispatchThread();
    final String title;
    final Supplier<RetreatResult> supplier;
    if (!submerge || possible.size() > 1) {
      title = "Retreat?";
      supplier = () -> showRetreatDialog(message, possible);
    } else {
      title = "Submerge Subs?";
      supplier = () -> showSubmergeDialog(message);
    }
    final CompletableFuture<Territory> future = new CompletableFuture<>();
    final Action action = getPlayerAction(title, supplier, future);
    SwingUtilities.invokeLater(
        () -> {
          actionButton.setAction(action);
          action.actionPerformed(null);
        });

    return uiContext.awaitUserInput(future).orElse(null);
  }

  private Action getPlayerAction(
      final String title,
      final Supplier<RetreatResult> showDialog,
      final CompletableFuture<Territory> future) {
    return SwingAction.of(
        title,
        e -> {
          actionButton.setEnabled(false);
          final RetreatResult retreatResult = showDialog.get();
          if (retreatResult.isConfirmed()) {
            future.complete(retreatResult.getTarget());
            actionButton.setAction(nullAction);
          }
          actionButton.setEnabled(true);
        });
  }

  private RetreatResult showSubmergeDialog(final String message) {
    final String ok = "Submerge";
    final String cancel = "Remain";
    final String wait = "Ask Me Later";
    final String[] options = {ok, cancel, wait};
    final int choice =
        JOptionPane.showOptionDialog(
            BattleDisplay.this,
            message,
            "Submerge Subs?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            cancel);
    // dialog dismissed
    if (choice == JOptionPane.CLOSED_OPTION || choice == JOptionPane.CANCEL_OPTION) {
      return RetreatResult.noResult();
    }
    // remain
    if (choice == JOptionPane.NO_OPTION) {
      return RetreatResult.remain();
    }
    // submerge
    return RetreatResult.retreatTo(battleLocation);
  }

  private RetreatResult showRetreatDialog(
      final String message, final Collection<Territory> possible) {
    final String yes =
        possible.size() == 1
            ? "Retreat to " + CollectionUtils.getAny(possible).getName()
            : "Retreat";
    final String no = "Remain";
    final String cancel = "Ask Me Later";
    final String[] options = {yes, no, cancel};
    final int choice =
        JOptionPane.showOptionDialog(
            BattleDisplay.this,
            message,
            "Retreat?",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            no);
    // dialog dismissed
    if (choice == JOptionPane.CLOSED_OPTION || choice == JOptionPane.CANCEL_OPTION) {
      return RetreatResult.noResult();
    }
    // remain
    if (choice == JOptionPane.NO_OPTION) {
      return RetreatResult.remain();
    }

    // retreat
    if (possible.size() == 1) {
      return RetreatResult.retreatTo(CollectionUtils.getAny(possible));
    }

    return showRetreatOptions(message, possible);
  }

  private RetreatResult showRetreatOptions(
      final String message, final Collection<Territory> possible) {
    final SelectTerritoryComponent comp =
        new SelectTerritoryComponent(battleLocation, possible, mapPanel);
    final int option =
        JOptionPane.showConfirmDialog(
            BattleDisplay.this,
            comp,
            message,
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null);
    return option == JOptionPane.OK_OPTION && comp.getSelection() != null
        ? RetreatResult.retreatTo(comp.getSelection())
        : RetreatResult.noResult();
  }

  CasualtyDetails getCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final CasualtyList defaultCasualties,
      final boolean allowMultipleHitsPerUnit) {
    Util.ensureNotOnEventDispatchThread();
    final AtomicReference<CasualtyDetails> casualtyDetails =
        new AtomicReference<>(new CasualtyDetails());
    final CountDownLatch continueLatch = new CountDownLatch(1);

    SwingUtilities.invokeLater(
        () -> {
          dicePanel.setDiceRoll(dice);
          casualties.setVisible(false);
          final boolean plural = (count > 1);
          final String btnText =
              hit.getName() + " select " + count + (plural ? " casualties" : " casualty");
          final var casualtySelection =
              new CasualtySelection(
                  selectFrom,
                  dependents,
                  count,
                  message + " " + btnText + ".",
                  hit,
                  defaultCasualties,
                  allowMultipleHitsPerUnit,
                  uiContext,
                  BattleDisplay.this);

          actionButton.setAction(
              new AbstractAction(btnText) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                  actionButton.setEnabled(false);

                  final CasualtyDetails selectedCasualties =
                      casualtySelection.showModalDialog().orElse(null);

                  if (selectedCasualties != null) {
                    casualtyDetails.set(selectedCasualties);
                    dicePanel.removeAll();
                    continueLatch.countDown();
                    actionButton.setAction(nullAction);
                    actionButton.setEnabled(false);
                  } else {
                    actionButton.setEnabled(true);
                  }
                }
              });
        });

    uiContext.addShutdownLatch(continueLatch);
    Interruptibles.await(continueLatch);
    uiContext.removeShutdownLatch(continueLatch);
    return casualtyDetails.get();
  }

  private void initLayout() {
    final JPanel attackerUnits = new JPanel();
    attackerUnits.setLayout(new BoxLayout(attackerUnits, BoxLayout.Y_AXIS));
    attackerUnits.add(getPlayerComponent(attacker));
    attackerUnits.add(Box.createGlue());
    final JTable attackerTable = new BattleTable(attackerModel);
    attackerUnits.add(attackerTable);
    attackerUnits.add(attackerTable.getTableHeader());
    final JPanel defenderUnits = new JPanel();
    defenderUnits.setLayout(new BoxLayout(defenderUnits, BoxLayout.Y_AXIS));
    defenderUnits.add(getPlayerComponent(defender));
    defenderUnits.add(Box.createGlue());
    final JTable defenderTable = new BattleTable(defenderModel);
    defenderUnits.add(defenderTable);
    defenderUnits.add(defenderTable.getTableHeader());
    final JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
    north.add(attackerUnits);
    north.add(getTerritoryComponent());
    north.add(defenderUnits);
    messagePanel.setLayout(new BorderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    steps = new BattleStepsPanel();
    uiContext.addShutdownHook(steps::wakeAll);
    steps.setBorder(new EtchedBorder(EtchedBorder.LOWERED));

    dicePanel = new DicePanel(uiContext, gameData);
    final JScrollPane diceScroll = new JScrollPane(dicePanel);
    diceScroll.setBorder(null);
    diceScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    diceScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    final JPanel actionPanel = new ScrollableJPanel();
    actionPanel.setLayout(new BorderLayout());
    actionPanel.add(diceScroll, BorderLayout.CENTER);
    casualties.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    casualties.setVisible(false);
    actionPanel.add(casualties, BorderLayout.SOUTH);

    final JPanel diceAndSteps = new JPanel();
    diceAndSteps.setLayout(new BorderLayout());
    diceAndSteps.add(steps, BorderLayout.WEST);
    diceAndSteps.add(actionPanel, BorderLayout.CENTER);

    casualtiesInstantPanelAttacker.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
    casualtiesInstantPanelAttacker.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    casualtiesInstantPanelAttacker.add(labelNoneAttacker);
    casualtiesInstantPanelDefender.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
    casualtiesInstantPanelDefender.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    casualtiesInstantPanelDefender.add(labelNoneDefender);
    final JPanel instantCasualtiesPanel = new JPanel();
    instantCasualtiesPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    instantCasualtiesPanel.setLayout(new GridBagLayout());
    final JLabel casualtiesLabel = new JLabel("Casualties", SwingConstants.CENTER);
    casualtiesLabel.setFont(getPlayerComponent(attacker).getFont().deriveFont(Font.BOLD, 14));
    instantCasualtiesPanel.add(
        casualtiesLabel,
        new GridBagConstraints(
            0,
            0,
            2,
            1,
            1.0d,
            1.0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));
    instantCasualtiesPanel.add(
        casualtiesInstantPanelAttacker,
        new GridBagConstraints(
            0,
            2,
            1,
            1,
            1.0d,
            1.0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));
    instantCasualtiesPanel.add(
        casualtiesInstantPanelDefender,
        new GridBagConstraints(
            1,
            2,
            1,
            1,
            1.0d,
            1.0d,
            GridBagConstraints.CENTER,
            GridBagConstraints.BOTH,
            new Insets(0, 0, 0, 0),
            0,
            0));
    diceAndSteps.add(instantCasualtiesPanel, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(north, BorderLayout.NORTH);
    add(diceAndSteps, BorderLayout.CENTER);
    add(actionButton, BorderLayout.SOUTH);
    actionButton.setEnabled(false);
    setDefaultWidths(defenderTable);
    setDefaultWidths(attackerTable);

    // press space to continue
    SwingKeyBinding.addKeyBinding(
        this,
        KeyCode.SPACE,
        () ->
            Optional.ofNullable(actionButton.getAction()) //
                .ifPresent(a -> a.actionPerformed(null)));
  }

  /** Shorten columns with no units. */
  private static void setDefaultWidths(final JTable table) {
    for (int column = 0; column < table.getColumnCount(); column++) {
      boolean hasData = false;
      for (int row = 0; row < table.getRowCount(); row++) {
        hasData |= (table.getValueAt(row, column) != BattleModel.TableData.NULL);
      }
      if (!hasData) {
        table.getColumnModel().getColumn(column).setPreferredWidth(8);
      }
    }
  }

  public void setStep(final String step) {
    steps.setStep(step);
  }

  void battleInfo(final DiceRoll message, final String step) {
    setStep(step);
    dicePanel.setDiceRoll(message);
    casualties.setVisible(false);
  }

  void battleInfo(final String message, final String step) {
    messageLabel.setText(message);
    setStep(step);
    casualties.setVisible(false);
  }

  void listBattle(final List<String> steps) {
    this.steps.listBattle(steps);
  }

  static JComponent getPlayerComponent(final GamePlayer gamePlayer) {
    final JLabel player = new JLabel(gamePlayer.getName());
    player.setBorder(new EmptyBorder(5, 5, 5, 5));
    player.setFont(player.getFont().deriveFont((float) 14));
    return player;
  }

  private JComponent getTerritoryComponent() {
    final Image finalImage = Util.newImage(MY_WIDTH, MY_HEIGHT, true);
    final Image territory = mapPanel.getTerritoryImage(battleLocation);
    final Graphics g = finalImage.getGraphics();
    g.drawImage(territory, 0, 0, MY_WIDTH, MY_HEIGHT, this);
    g.dispose();
    return new JLabel(new ImageIcon(finalImage));
  }

  private static final class BattleTable extends JTable {
    private static final long serialVersionUID = 6737857639382012817L;

    BattleTable(final BattleModel model) {
      super(model);
      setDefaultRenderer(Object.class, new Renderer());
      setRowHeight(DEFAULT_UNIT_ICON_SIZE + 5);
      setBackground(new JButton().getBackground());
      setShowHorizontalLines(false);
      getTableHeader().setReorderingAllowed(false);
      // getTableHeader().setResizingAllowed(false);
    }
  }

  private static final class Renderer implements TableCellRenderer {
    final JLabel stamp = new JLabel();

    @Override
    public Component getTableCellRendererComponent(
        final JTable table,
        final Object value,
        final boolean isSelected,
        final boolean hasFocus,
        final int row,
        final int column) {
      ((BattleModel.TableData) value).updateStamp(stamp);
      return stamp;
    }
  }

  private static final class CasualtyNotificationPanel extends JPanel {
    private static final long serialVersionUID = -8254027929090027450L;
    private final JPanel killed = new JPanel();
    private final JPanel damaged = new JPanel();
    private final UiContext uiContext;

    CasualtyNotificationPanel(final UiContext uiContext) {
      this.uiContext = uiContext;
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      add(killed);
      add(damaged);
    }

    void setNotification(
        final Collection<Unit> killed,
        final Collection<Unit> damaged,
        final Map<Unit, Collection<Unit>> dependents) {
      this.killed.removeAll();
      this.damaged.removeAll();
      if (!killed.isEmpty()) {
        this.killed.add(new JLabel("Killed"));
      }
      final Iterable<UnitCategory> killedIter =
          UnitSeparator.categorize(
              killed, UnitSeparator.SeparatorCategories.builder().dependents(dependents).build());
      categorizeUnits(killedIter, false, false);
      damaged.removeAll(killed);
      if (!damaged.isEmpty()) {
        this.damaged.add(new JLabel("Damaged"));
      }
      final Iterable<UnitCategory> damagedIter =
          UnitSeparator.categorize(
              damaged, UnitSeparator.SeparatorCategories.builder().dependents(dependents).build());
      categorizeUnits(damagedIter, true, true);
      invalidate();
      validate();
    }

    void setNotificationShort(
        final Collection<Unit> killed, final Map<Unit, Collection<Unit>> dependents) {
      this.killed.removeAll();
      if (!killed.isEmpty()) {
        this.killed.add(new JLabel("Killed"));
      }
      final Iterable<UnitCategory> killedIter =
          UnitSeparator.categorize(
              killed, UnitSeparator.SeparatorCategories.builder().dependents(dependents).build());
      categorizeUnits(killedIter, false, false);
      invalidate();
      validate();
    }

    private void categorizeUnits(
        final Iterable<UnitCategory> categoryIter, final boolean damaged, final boolean disabled) {
      for (final UnitCategory category : categoryIter) {
        final JPanel panel = new JPanel();
        final ImageIcon unitImage =
            uiContext
                .getUnitImageFactory()
                .getIcon(
                    ImageKey.builder()
                        .player(category.getOwner())
                        .type(category.getType())
                        .damaged(damaged && category.hasDamageOrBombingUnitDamage())
                        .disabled(disabled && category.getDisabled())
                        .build());
        final JLabel unit = new JLabel(unitImage);
        panel.add(unit);
        // Add a tooltip, with a count of 1 so that the tooltip doesn't have a number label (so it
        // won't get out of date
        // when units are killed.)
        MapUnitTooltipManager.setUnitTooltip(
            unit, category.getType(), category.getOwner(), 1, uiContext);
        for (final UnitOwner owner : category.getDependents()) {
          unit.add(uiContext.newUnitImageLabel(owner.getType(), owner.getOwner()));
        }
        panel.add(new JLabel("x " + category.getUnits().size()));
        if (damaged) {
          this.damaged.add(panel);
        } else {
          killed.add(panel);
        }
      }
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Getter
  private static class RetreatResult {
    private final boolean confirmed;
    @Nullable private final Territory target;

    static RetreatResult noResult() {
      return new RetreatResult(false, null);
    }

    static RetreatResult remain() {
      return new RetreatResult(true, null);
    }

    static RetreatResult retreatTo(final Territory territory) {
      return new RetreatResult(true, Preconditions.checkNotNull(territory));
    }
  }
}
