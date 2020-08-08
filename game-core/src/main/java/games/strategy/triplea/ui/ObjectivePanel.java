package games.strategy.triplea.ui;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractPlayerRulesAttachment;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import java.awt.Color;
import java.awt.Component;
import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import lombok.extern.java.Log;
import org.triplea.swing.SwingAction;
import org.triplea.util.FileNameUtils;

/**
 * A panel that will show all objectives for all players, including if the objective is filled or
 * not.
 */
@Log
public class ObjectivePanel extends AbstractStatPanel {
  private static final long serialVersionUID = 3759819236905645520L;
  private Map<String, Map<ICondition, String>> statsObjective;
  private ObjectiveTableModel objectiveModel;
  private IDelegateBridge dummyDelegate;

  ObjectivePanel(final GameData data) {
    super(data);
    dummyDelegate = new ObjectiveDummyDelegateBridge(data);
    initLayout();
  }

  @Override
  public String getName() {
    return ObjectiveProperties.getInstance().getName();
  }

  public boolean isEmpty() {
    return statsObjective.isEmpty();
  }

  public void removeDataChangeListener() {
    objectiveModel.removeDataChangeListener();
  }

  protected void initLayout() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    objectiveModel = new ObjectiveTableModel();
    final JTable table = new JTable(objectiveModel);
    table.getTableHeader().setReorderingAllowed(false);
    final TableColumn column0 = table.getColumnModel().getColumn(0);
    column0.setPreferredWidth(34);
    column0.setWidth(34);
    column0.setMaxWidth(34);
    column0.setCellRenderer(new ColorTableCellRenderer());
    final TableColumn column1 = table.getColumnModel().getColumn(1);
    column1.setCellEditor(new EditorPaneCellEditor());
    column1.setCellRenderer(new EditorPaneTableCellRenderer());
    final JScrollPane scroll = new JScrollPane(table);
    final JButton refresh = new JButton("Refresh Objectives");
    refresh.setAlignmentY(Component.CENTER_ALIGNMENT);
    refresh.addActionListener(
        SwingAction.of(
            "Refresh Objectives",
            e -> {
              objectiveModel.loadData();
              SwingUtilities.invokeLater(table::repaint);
            }));
    add(Box.createVerticalStrut(6));
    add(refresh);
    add(Box.createVerticalStrut(6));
    add(scroll);
  }

  class ObjectiveTableModel extends AbstractTableModel implements GameDataChangeListener {
    private static final long serialVersionUID = 2259315408905271333L;
    private static final int COLUMNS_TOTAL = 2;
    private boolean isDirty = true;
    private String[][] collectedData;
    private final Map<String, List<String>> sections = new LinkedHashMap<>();
    private Instant timestamp = Instant.EPOCH;

    ObjectiveTableModel() {
      setObjectiveStats();
      gameData.addDataChangeListener(this);
    }

    public void removeDataChangeListener() {
      gameData.removeDataChangeListener(this);
    }

    private void setObjectiveStats() {
      statsObjective = new LinkedHashMap<>();
      final ObjectiveProperties op = ObjectiveProperties.getInstance();
      final String gameName =
          FileNameUtils.replaceIllegalCharacters(gameData.getGameName(), '_')
              .replaceAll(" ", "_")
              .concat(".");
      final Map<String, List<String>> sectionsUnsorted = new HashMap<>();
      final Set<String> sectionsSorters = new TreeSet<>();
      // do sections first
      for (final Entry<Object, Object> entry : op.entrySet()) {
        final String fileKey = (String) entry.getKey();
        if (!fileKey.startsWith(gameName)) {
          continue;
        }
        final List<String> key = Splitter.on(';').splitToList(fileKey.substring(gameName.length()));
        final String value = (String) entry.getValue();
        if (key.size() != 2) {
          log.severe(
              "objective.properties keys must be 2 parts: <game_name>."
                  + ObjectiveProperties.GROUP_PROPERTY
                  + ".<#>;player  OR  <game_name>.player;attachmentName");
          continue;
        }
        if (!key.get(0).startsWith(ObjectiveProperties.GROUP_PROPERTY)) {
          continue;
        }
        final List<String> sorter = Splitter.on('.').splitToList(key.get(0));
        if (sorter.size() != 2) {
          log.severe(
              "objective.properties "
                  + ObjectiveProperties.GROUP_PROPERTY
                  + "must have .<sorter> after it: "
                  + key.get(0));
          continue;
        }
        sectionsSorters.add(sorter.get(1) + ";" + key.get(1));
        sectionsUnsorted.put(key.get(1), List.of(value.split(";")));
      }
      final Map<String, Map<ICondition, String>> statsObjectiveUnsorted = new HashMap<>();
      for (final String section : sectionsSorters) {
        final String key = Iterables.get(Splitter.on(';').split(section), 1);
        sections.put(key, sectionsUnsorted.get(key));
        statsObjective.put(key, new LinkedHashMap<>());
        statsObjectiveUnsorted.put(key, new HashMap<>());
      }
      // now do the stuff in the sections
      for (final Entry<Object, Object> entry : op.entrySet()) {
        final String fileKey = (String) entry.getKey();
        if (!fileKey.startsWith(gameName)) {
          continue;
        }
        final List<String> key = Splitter.on(';').splitToList(fileKey.substring(gameName.length()));
        final String value = (String) entry.getValue();
        if (key.size() != 2) {
          log.severe(
              "objective.properties keys must be 2 parts: <game_name>."
                  + ObjectiveProperties.GROUP_PROPERTY
                  + ".<#>;player  OR  <game_name>.player;attachmentName");
          continue;
        }
        if (key.get(0).startsWith(ObjectiveProperties.GROUP_PROPERTY)) {
          continue;
        }
        final ICondition condition =
            AbstractPlayerRulesAttachment.getCondition(key.get(0), key.get(1), gameData);
        if (condition == null) {
          continue;
        }
        final GamePlayer player = gameData.getPlayerList().getPlayerId(key.get(0));

        // find which section
        boolean found = false;
        if (sections.containsKey(player.getName())
            && sections.get(player.getName()).contains(key.get(1))) {
          final Map<ICondition, String> map = statsObjectiveUnsorted.get(player.getName());
          if (map == null) {
            throw new IllegalStateException(
                "objective.properties group has nothing: " + player.getName());
          }
          map.put(condition, value);
          statsObjectiveUnsorted.put(player.getName(), map);
          found = true;
        }
        if (!found) {
          for (final Entry<String, List<String>> sectionEntry : sections.entrySet()) {
            if (sectionEntry.getValue().contains(key.get(1))) {
              final Map<ICondition, String> map = statsObjectiveUnsorted.get(sectionEntry.getKey());
              if (map == null) {
                throw new IllegalStateException(
                    "objective.properties group has nothing: " + sectionEntry.getKey());
              }
              map.put(condition, value);
              statsObjectiveUnsorted.put(sectionEntry.getKey(), map);
              break;
            }
          }
        }
      }
      for (final Entry<String, Map<ICondition, String>> entry : statsObjective.entrySet()) {
        final Map<ICondition, String> mapUnsorted = statsObjectiveUnsorted.get(entry.getKey());
        final Map<ICondition, String> mapSorted = entry.getValue();
        for (final String conditionString : sections.get(entry.getKey())) {
          final Iterator<ICondition> conditionIter = mapUnsorted.keySet().iterator();
          while (conditionIter.hasNext()) {
            final ICondition condition = conditionIter.next();
            if (conditionString.equals(condition.getName())) {
              mapSorted.put(condition, mapUnsorted.get(condition));
              conditionIter.remove();
              break;
            }
          }
        }
      }
    }

    @Override
    public synchronized Object getValueAt(final int row, final int col) {
      // do not refresh too often, or else it will slow the game down seriously
      if (isDirty && timestamp.plusSeconds(10).isBefore(Instant.now())) {
        loadData();
        isDirty = false;
        timestamp = Instant.now();
      }
      return collectedData[row][col];
    }

    private synchronized void loadData() {
      gameData.acquireReadLock();
      try {
        final Map<ICondition, String> conditions = getConditionComment(getTestedConditions());
        collectedData = new String[getRowTotal()][COLUMNS_TOTAL];
        int row = 0;
        for (final Entry<String, Map<ICondition, String>> mapEntry : statsObjective.entrySet()) {
          collectedData[row][1] =
              "<html><span style=\"font-size:140%\"><b><em>"
                  + mapEntry.getKey()
                  + "</em></b></span></html>";
          for (final Entry<ICondition, String> attachmentEntry : mapEntry.getValue().entrySet()) {
            row++;
            collectedData[row][0] = conditions.get(attachmentEntry.getKey());
            collectedData[row][1] = "<html>" + attachmentEntry.getValue() + "</html>";
          }
          row++;
          collectedData[row][1] = "--------------------";
          row++;
        }
      } finally {
        gameData.releaseReadLock();
      }
    }

    public Map<ICondition, String> getConditionComment(
        final Map<ICondition, Boolean> testedConditions) {
      final Map<ICondition, String> conditionsComments = new HashMap<>(testedConditions.size());
      for (final Entry<ICondition, Boolean> entry : testedConditions.entrySet()) {
        final boolean satisfied = entry.getValue();
        if (entry.getKey() instanceof TriggerAttachment) {
          final TriggerAttachment ta = (TriggerAttachment) entry.getKey();
          final int each = AbstractTriggerAttachment.getEachMultiple(ta);
          final int uses = ta.getUses();
          if (uses < 0) {
            final String comment = satisfied ? (each > 1 ? "T" + each : "T") : "F";
            conditionsComments.put(entry.getKey(), comment);
          } else if (uses == 0) {
            final String comment = satisfied ? "Used" : "used";
            conditionsComments.put(entry.getKey(), comment);
          } else {
            final String comment = uses + "" + (satisfied ? (each > 1 ? "T" + each : "T") : "F");
            conditionsComments.put(entry.getKey(), comment);
          }
        } else if (entry.getKey() instanceof RulesAttachment) {
          final RulesAttachment ra = (RulesAttachment) entry.getKey();
          final int each = ra.getEachMultiple();
          final int uses = ra.getUses();
          if (uses < 0) {
            final String comment = satisfied ? (each > 1 ? "T" + each : "T") : "F";
            conditionsComments.put(entry.getKey(), comment);
          } else if (uses == 0) {
            final String comment = satisfied ? "Used" : "used";
            conditionsComments.put(entry.getKey(), comment);
          } else {
            final String comment = uses + "" + (satisfied ? (each > 1 ? "T" + each : "T") : "F");
            conditionsComments.put(entry.getKey(), comment);
          }
        } else {
          conditionsComments.put(entry.getKey(), entry.getValue().toString());
        }
      }
      return conditionsComments;
    }

    public Map<ICondition, Boolean> getTestedConditions() {
      final Set<ICondition> myConditions = new HashSet<>();
      for (final Map<ICondition, String> map : statsObjective.values()) {
        myConditions.addAll(map.keySet());
      }
      final Set<ICondition> allConditionsNeeded =
          AbstractConditionsAttachment.getAllConditionsRecursive(myConditions, null);
      return AbstractConditionsAttachment.testAllConditionsRecursive(
          allConditionsNeeded, null, dummyDelegate);
    }

    @Override
    public void gameDataChanged(final Change change) {
      synchronized (this) {
        isDirty = true;
      }
      SwingUtilities.invokeLater(ObjectivePanel.this::repaint);
    }

    @Override
    public String getColumnName(final int col) {
      return (col == 0) ? "Done" : "Objective Name";
    }

    @Override
    public int getColumnCount() {
      return COLUMNS_TOTAL;
    }

    @Override
    public synchronized int getRowCount() {
      if (!isDirty) {
        return collectedData.length;
      }

      gameData.acquireReadLock();
      try {
        return getRowTotal();
      } finally {
        gameData.releaseReadLock();
      }
    }

    private int getRowTotal() {
      int rowsTotal = sections.size() * 2; // we include a space between sections as well
      for (final Map<ICondition, String> map : statsObjective.values()) {
        rowsTotal += map.size();
      }
      return rowsTotal;
    }

    public synchronized void setGameData(final GameData data) {
      synchronized (this) {
        gameData.removeDataChangeListener(this);
        gameData = data;
        setObjectiveStats();
        gameData.addDataChangeListener(this);
        isDirty = true;
      }
      repaint();
    }
  }

  public void setGameData(final GameData data) {
    dummyDelegate = new ObjectiveDummyDelegateBridge(data);
    gameData = data;
    objectiveModel.setGameData(data);
    objectiveModel.gameDataChanged(null);
  }

  private static final class ColorTableCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 4197520597103598219L;
    private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();

    @Override
    public Component getTableCellRendererComponent(
        final JTable table,
        final Object value,
        final boolean isSelected,
        final boolean hasFocus,
        final int row,
        final int column) {
      adaptee.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      final JLabel renderer =
          (JLabel)
              super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      renderer.setHorizontalAlignment(SwingConstants.CENTER);
      if (value == null) {
        renderer.setBorder(BorderFactory.createEmptyBorder());
      } else if (value.toString().contains("T")) {
        renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.green));
      } else if (value.toString().contains("U")) {
        renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.blue));
      } else if (value.toString().contains("u")) {
        renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.cyan));
      } else {
        renderer.setBorder(BorderFactory.createEmptyBorder());
      }
      return renderer;
    }
  }

  // author: Heinz M. Kabutz (modified for JEditorPane by Mark Christopher Duncan)
  private static final class EditorPaneCellEditor extends DefaultCellEditor {
    private static final long serialVersionUID = 509377442956621991L;

    EditorPaneCellEditor() {
      super(new JTextField());
      final JEditorPane textArea = new JEditorPane();
      textArea.setEditable(false);
      textArea.setContentType("text/html");
      final JScrollPane scrollPane = new JScrollPane(textArea);
      scrollPane.setBorder(null);
      editorComponent = scrollPane;
      delegate =
          new DefaultCellEditor.EditorDelegate() {
            private static final long serialVersionUID = 5746645959173385516L;

            @Override
            public void setValue(final Object value) {
              textArea.setText((value != null) ? value.toString() : "");
            }

            @Override
            public Object getCellEditorValue() {
              return textArea.getText();
            }
          };
    }
  }

  // author: Heinz M. Kabutz (modified for JEditorPane by Mark Christopher Duncan)
  private static final class EditorPaneTableCellRenderer extends JEditorPane
      implements TableCellRenderer {
    private static final long serialVersionUID = -2835145877164663862L;
    private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
    private final Map<JTable, Map<Integer, Map<Integer, Integer>>> cellSizes = new HashMap<>();

    EditorPaneTableCellRenderer() {
      setEditable(false);
      setContentType("text/html");
    }

    @Override
    public Component getTableCellRendererComponent(
        final JTable table,
        final Object obj,
        final boolean isSelected,
        final boolean hasFocus,
        final int row,
        final int column) {
      // set the colors, etc. using the standard for that platform
      adaptee.getTableCellRendererComponent(table, obj, isSelected, hasFocus, row, column);
      setForeground(adaptee.getForeground());
      setBackground(adaptee.getBackground());
      setBorder(adaptee.getBorder());
      setFont(adaptee.getFont());
      setText(adaptee.getText());
      // This line was very important to get it working with JDK1.4
      final TableColumnModel columnModel = table.getColumnModel();
      setSize(columnModel.getColumn(column).getWidth(), 100000);
      int heightWanted = (int) getPreferredSize().getHeight();
      addSize(table, row, column, heightWanted);
      heightWanted = findTotalMaximumRowSize(table, row);
      if (heightWanted != table.getRowHeight(row)) {
        table.setRowHeight(row, heightWanted);
      }
      return this;
    }

    private void addSize(final JTable table, final int row, final int column, final int height) {
      final Map<Integer, Map<Integer, Integer>> rows =
          cellSizes.computeIfAbsent(table, k -> new HashMap<>());
      final var rowHeights = rows.computeIfAbsent(row, k -> new HashMap<>());
      rowHeights.put(column, height);
    }

    /**
     * Look through all columns and get the renderer. If it is also a TextAreaRenderer, we look at
     * the maximum height in its hash table for this row.
     */
    private static int findTotalMaximumRowSize(final JTable table, final int row) {
      int maximumHeight = 0;
      final Enumeration<?> columns = table.getColumnModel().getColumns();
      while (columns.hasMoreElements()) {
        final TableColumn tc = (TableColumn) columns.nextElement();
        final TableCellRenderer cellRenderer = tc.getCellRenderer();
        if (cellRenderer instanceof EditorPaneTableCellRenderer) {
          final EditorPaneTableCellRenderer tar = (EditorPaneTableCellRenderer) cellRenderer;
          maximumHeight = Math.max(maximumHeight, tar.findMaximumRowSize(table, row));
        }
      }
      return maximumHeight;
    }

    private int findMaximumRowSize(final JTable table, final int row) {
      final Map<Integer, Map<Integer, Integer>> rows = cellSizes.get(table);
      if (rows == null) {
        return 0;
      }
      final Map<Integer, Integer> rowheights = rows.get(row);
      if (rowheights == null) {
        return 0;
      }
      int maximumHeight = 0;
      for (final Entry<Integer, Integer> entry : rowheights.entrySet()) {
        final int cellHeight = entry.getValue();
        maximumHeight = Math.max(maximumHeight, cellHeight);
      }
      return maximumHeight;
    }
  }
}
