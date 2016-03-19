package util.triplea.MapXMLCreator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


public class GameSettingsPanel extends DynamicRowsPanel {

  final static String allSettings =
      "mapName, notes, Neutral Flyover Allowed, More Constructions with Factory, Unlimited Constructions, Move existing fighters to new carriers, More Constructions without Factory, Produce new fighters on old carriers, MaxFactoriesPerTerritory, Multiple AA Per Territory, Land existing fighters on new carriers, Kamikaze Airplanes, Multiply PUs, Submersible Subs, Battleships repair at beginning of round, Battleships repair at end of round, Choose AA Casualties, WW2V2, Low Luck, Low Luck for AntiAircraft, Low Luck for Technology, Low Luck for Bombing and Territory Damage, Use Triggers, neutralCharge, maxFactoriesPerTerritory, Always on AA, Produce fighters on carriers, LHTR Carrier production rules, Two hit battleship, 4th Edition, Partial Amphibious Retreat, Total Victory, Honorable Surrender, Projection of Power, All Rockets Attack, Neutrals Are Impassable, Neutrals Are Blitzable, Rockets Can Violate Neutrality, Rockets Can Fly Over Impassables, Pacific Edition, Anniversary Edition, No Economic Victory, Anniversary Edition Land Production, Anniversary Edition Air Naval, Placement Restricted By Factory, Selectable Tech Roll, AA50 Tech Model, Tech Development, Transport Restricted Unload, Random AA Casualties, Roll AA Individually, Limit SBR Damage To Factory Production, Limit SBR Damage To Factory Production, Limit SBR Damage Per Turn, Limit Rocket Damage Per Turn, Territory Turn Limit, SBR Victory Points, Rocket Attack Per Factory Restricted, Allied Air Dependents, Defending Subs Sneak Attack, Attacker Retreat Planes, Surviving Air Move To Land, Naval Bombard Casualties Return Fire Restricted, Blitz Through Factories And AA Restricted, Unit Placement In Enemy Seas, Sub Control Sea Zone Restricted, Transport Control Sea Zone, Production Per X Territories Restricted, Production Per Valued Territory Restricted, Place in Any Territory, Unit Placement Per Territory Restricted, Movement By Territory Restricted, Transport Casualties Restricted, Ignore Transport In Movement, Ignore Sub In Movement, Hari-Kari Units, Occupied Territories, Unplaced units live when not placed, Air Attack Sub Restricted, Sub Retreat Before Battle, Sub Retreat DD Restricted, Shore Bombard Per Ground Unit Restricted, SBR Affects Unit Production, AA Territory Restricted, National Objectives, Continuous Research";


  public static enum SETTING_TYPE {
    NORMAL, PER_PLAYER, PER_ALLY
  }

  public static SETTING_TYPE getSettingType(final String setting) {
    if (setting.endsWith(" bid"))
      return SETTING_TYPE.PER_PLAYER;
    else if (setting.endsWith(" Honorable Victory VCs"))
      return SETTING_TYPE.PER_ALLY;
    else
      return SETTING_TYPE.NORMAL;
  }

  public static boolean isBoolean(final String setting) {
    return (getSettingType(setting).equals(SETTING_TYPE.NORMAL) || setting.equals("MaxFactoriesPerTerritory"));
    // TODO: maybe list is incomplete!
  }

  private TreeSet<String> settingNames = new TreeSet<String>();

  public GameSettingsPanel(final JPanel stepActionPanel) {
    super(stepActionPanel);
  }

  public static void layout(final MapXMLCreator mapXMLCreator, final JPanel stepActionPanel) {
    if (!DynamicRowsPanel.me.isPresent() || !(me.get() instanceof GameSettingsPanel))
      me = Optional.of(new GameSettingsPanel(stepActionPanel));
    DynamicRowsPanel.layout(mapXMLCreator, stepActionPanel);
  }

  protected ActionListener getAutoFillAction() {
    return null;
  }

  protected void layoutComponents() {

    final JLabel labelSettingName = new JLabel("Setting Name");
    Dimension dimension = labelSettingName.getPreferredSize();
    labelSettingName.setPreferredSize(dimension);
    final JLabel labelValue = new JLabel("Value");
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    labelValue.setPreferredSize(dimension);
    final JLabel labelEditable = new JLabel("Editable");
    dimension = (Dimension) dimension.clone();
    dimension.width = DynamicRow.INPUT_FIELD_SIZE_SMALL;
    labelEditable.setPreferredSize(dimension);
    final JLabel labelMinNumber = new JLabel("Min. N.");
    dimension = (Dimension) dimension.clone();
    labelMinNumber.setPreferredSize(dimension);
    final JLabel labelMaxNumber = new JLabel("Max. N.");
    dimension = (Dimension) dimension.clone();
    labelMaxNumber.setPreferredSize(dimension);

    // <1> Set panel layout
    GridBagLayout gbl_stepActionPanel = new GridBagLayout();
    setColumns(gbl_stepActionPanel);
    setRows(gbl_stepActionPanel, MapXMLHelper.gameSettings.size());
    ownPanel.setLayout(gbl_stepActionPanel);

    // <2> Add Row Labels: Setting Name, Alliance Name, Buy Quantity
    GridBagConstraints gridBadConstLabelSettingName = new GridBagConstraints();
    gridBadConstLabelSettingName.insets = new Insets(0, 0, 5, 5);
    gridBadConstLabelSettingName.gridy = 0;
    gridBadConstLabelSettingName.gridx = 0;
    gridBadConstLabelSettingName.anchor = GridBagConstraints.WEST;
    ownPanel.add(labelSettingName, gridBadConstLabelSettingName);

    GridBagConstraints gridBadConstLabelValue = (GridBagConstraints) gridBadConstLabelSettingName.clone();
    gridBadConstLabelValue.gridx = 1;
    ownPanel.add(labelValue, gridBadConstLabelValue);

    GridBagConstraints gridBadConstLabelEditable = (GridBagConstraints) gridBadConstLabelSettingName.clone();
    gridBadConstLabelEditable.gridx = 2;
    ownPanel.add(labelEditable, gridBadConstLabelEditable);

    GridBagConstraints gridBadConstLabelMinNumber = (GridBagConstraints) gridBadConstLabelSettingName.clone();
    gridBadConstLabelMinNumber.gridx = 3;
    ownPanel.add(labelMinNumber, gridBadConstLabelMinNumber);

    GridBagConstraints gridBadConstLabelMaxNumber = (GridBagConstraints) gridBadConstLabelSettingName.clone();
    gridBadConstLabelMaxNumber.gridx = 4;
    ownPanel.add(labelMaxNumber, gridBadConstLabelMaxNumber);

    // <3> Add Main Input Rows
    int yValue = 1;

    final String[] settingNamesArray = settingNames.toArray(new String[settingNames.size()]);
    for (final Entry<String, List<String>> settingEntry : MapXMLHelper.gameSettings.entrySet()) {
      GridBagConstraints gbc_tValue = (GridBagConstraints) gridBadConstLabelSettingName.clone();
      gbc_tValue.gridx = 0;
      gridBadConstLabelValue.gridy = yValue;
      final List<String> settingValue = settingEntry.getValue();
      Integer minValueInteger, maxValueInteger;
      try {
        minValueInteger = Integer.valueOf(settingValue.get(2));
        maxValueInteger = Integer.valueOf(settingValue.get(3));
      } catch (NumberFormatException nfe) {
        minValueInteger = 0;
        maxValueInteger = 0;
      }
      final GameSettingsRow newRow = new GameSettingsRow(this, ownPanel, settingEntry.getKey(), settingNamesArray,
          settingValue.get(0), settingValue.get(1), minValueInteger, maxValueInteger);
      newRow.addToComponent(ownPanel, yValue, gbc_tValue);
      rows.add(newRow);
      ++yValue;
    }

    // <4> Add Final Button Row
    final JButton buttonAddValue = new JButton("Add Game Setting");

    buttonAddValue.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    buttonAddValue.addActionListener(new AbstractAction("Add Game Setting") {
      private static final long serialVersionUID = 6322566373692205163L;

      public void actionPerformed(final ActionEvent e) {
        final String suggestedSettingName = (String) JOptionPane.showInputDialog(ownPanel,
            "Which game setting should be added?", "Choose Game Setting", JOptionPane.QUESTION_MESSAGE, null,
            settingNames.toArray(new String[settingNames.size()]), // Array of choices
            settingNames.iterator().next()); // Initial choice
        if (suggestedSettingName == null || suggestedSettingName.isEmpty())
          return;

        final ArrayList<String> newSettingValue = new ArrayList<String>();
        final boolean settingIsBoolean = isBoolean(suggestedSettingName);
        String newValue;
        if (settingIsBoolean)
          newValue = "true";
        else
          newValue = "0";
        newSettingValue.add(newValue);
        newSettingValue.add("true");
        newSettingValue.add("0");
        newSettingValue.add("0");
        MapXMLHelper.putGameSettings(suggestedSettingName, newSettingValue);

        // UI Update
        setRows((GridBagLayout) ownPanel.getLayout(), MapXMLHelper.gameSettings.size());
        addRowWith(suggestedSettingName, newValue, "true", 0, 0);
        SwingUtilities.invokeLater(() -> {
          ownPanel.revalidate();
          ownPanel.repaint();
        });
      }
    });
    addButton(buttonAddValue);

    GridBagConstraints gridBadConstButtonAddUnit = (GridBagConstraints) gridBadConstLabelSettingName.clone();
    gridBadConstButtonAddUnit.gridx = 0;
    gridBadConstButtonAddUnit.gridy = yValue;
    addFinalButtonRow(gridBadConstButtonAddUnit);
  }

  private DynamicRow addRowWith(final String settingName, final String newValue, final String editable,
      final int minNumber, final int maxNumber) {
    final GameSettingsRow newRow = new GameSettingsRow(this, ownPanel, settingName,
        settingNames.toArray(new String[settingNames.size()]), newValue, editable, minNumber, maxNumber);
    addRow(newRow);
    return newRow;
  }


  protected void initializeSpecifics() {
    settingNames.clear();
    settingNames.addAll(Arrays.asList(allSettings.split(", ")));
    for (final String player : MapXMLHelper.playerName) {
      settingNames.add(player + " bid");
    }
    for (final String ally : MapXMLHelper.playerAlliance.values()) {
      settingNames.add(ally + " Honorable Victory VCs");
    }
  }

  protected void setColumns(GridBagLayout gbl_panel) {
    gbl_panel.columnWidths = new int[] {60, 30, 30, 30, 30, 30};
    gbl_panel.columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
  }

}
