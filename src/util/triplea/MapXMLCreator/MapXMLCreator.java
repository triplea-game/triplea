package util.triplea.MapXMLCreator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;

import games.strategy.common.swing.SwingAction;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner2;

/**
 * A frame that will show the different steps creating a game XML.
 */
public class MapXMLCreator extends JFrame {
  private static final long serialVersionUID = 3593102638082774498L;
  public static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  public static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
  public static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
  public static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";

  private int highest_step = 1;

  static File mapImageFile = null;
  static File mapCentersFile = null;
  static File mapPolygonsFile = null;
  static String waterFilterString = "";
  static File mapFolderLocation = null;

  final JPanel mainPanel;
  final JPanel sidePanel;
  final JPanel stepPanel = new JPanel();
  final JPanel panel2 = new JPanel();
  final JPanel panel3 = new JPanel();
  final JPanel panel4 = new JPanel();
  private JPanel stepListPanel;
  private ArrayList<JLabel> stepList = new ArrayList<JLabel>();
  private final JPanel southPanel = new JPanel();
  private JPanel southLeftPanel;
  private JPanel southRightPanel;
  private JPanel southCenterPanel;
  private JButton buttonHelp;
  private JButton buttonAvailableChoices;
  private JButton backButton;
  JButton autoFillButton;
  private JButton nextButton;
  private JPanel stepActionPanel;
  private JLabel stepTitleLabel;
  private JPanel actionPanel;
  private int currentStep; // TODO: change to enum

  public static String[] getProperties() {
    return new String[] {TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT};
  }

  public static void main(final String[] args) {
    // TODO: change logging to appear in a log text area on the UI (maybe enable/disable functionality)
    System.out.println("Starting MapXMLCreator");
    // handleCommandLineArgs(args);
    GameRunner2.setupLookAndFeel();
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        final MapXMLCreator creator = new MapXMLCreator();
        creator.setSize(800, 600);
        creator.setPreferredSize(creator.getSize());
        creator.goToStep(1);
        creator.setLocationRelativeTo(null);
        creator.setVisible(true);
      }
    });
  }

  public MapXMLCreator() {
    super("TripleA Map XML Creator");

    mapFolderLocation = new File(ClientFileSystemHelper.getRootFolder() + File.separator + "maps");
    // keep for the moment for test purposes
    // mapFolderLocation = new File("C:\\Users\\evdO\\triplea\\triplea_1_7_0_3\\maps\\minimap");
    // mapFolderLocation = new File("C:\\Users\\User\\workspace\\triplea\\maps\\minimap");
    MapXMLHelper.mapXMLFile =
        new File(mapFolderLocation.getAbsolutePath() + "\\..\\new_world_order\\games\\new_world_order.xml");

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final Action openAction = createMenuBar();

    // components
    mainPanel = new JPanel();
    sidePanel = new JPanel();
    createStepList();

    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
    sidePanel.add(Box.createVerticalStrut(2));
    sidePanel.add(stepListPanel);
    sidePanel.add(Box.createVerticalGlue());


    stepListPanel.setLayout(new GridLayout(stepList.size(), 1, 0, 10));
    stepListPanel.setBorder(new EmptyBorder(10, 2, 0, 5));
    stepListPanel.setPreferredSize(new Dimension(170, 410));

    int current_label = 1;
    for (final JLabel labelStep : stepList) {
      final int label_order = current_label;
      labelStep.addMouseListener(new MouseListener() {

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {
          if (label_order <= highest_step && label_order != currentStep) {
            labelStep.setOpaque(false);
            labelStep.setBackground(labelStep.getBackground().darker().darker());
            labelStep.updateUI();
          }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
          if (label_order <= highest_step && label_order != currentStep) {
            labelStep.setOpaque(true);
            labelStep.setBackground(labelStep.getBackground().brighter().brighter());
            labelStep.updateUI();
          }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          labelStep.requestFocus();
          if (label_order <= highest_step && label_order != currentStep
              && (!DynamicRowsPanel.me.isPresent() || DynamicRowsPanel.me.get().dataIsConsistent())) {
            labelStep.setOpaque(false);
            goToStep(label_order);
          }
        }
      });
      stepListPanel.add(labelStep);
      ++current_label;
    }

    // set up the layout manager
    this.getContentPane().setLayout(new BorderLayout());
    final JScrollPane scrollPane = new JScrollPane(sidePanel);
    scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, Color.DARK_GRAY));
    this.getContentPane().add(scrollPane, BorderLayout.WEST);
    this.getContentPane().add(mainPanel, BorderLayout.CENTER);

    // now set up the main screen
    setupMainPanel(stepPanel);
    stepPanel.setLayout(new BoxLayout(stepPanel, BoxLayout.PAGE_AXIS));

    stepPanel.add(Box.createVerticalStrut(20));

    stepTitleLabel = new JLabel("Map Properties");
    stepTitleLabel.setFont(new Font(MapXMLHelper.defaultMapXMLCreatorFontName, Font.BOLD, 12));
    stepTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    stepPanel.add(stepTitleLabel);

    stepPanel.add(Box.createVerticalStrut(20));

    actionPanel = new JPanel();
    actionPanel.setLayout(new BorderLayout());
    actionPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.DARK_GRAY));
    stepActionPanel = new JPanel();
    stepPanel.add(actionPanel);

    actionPanel.add(stepActionPanel, BorderLayout.CENTER);

    layoutSouthPanel();

    openAction.actionPerformed(null);
  }

  /**
   * @return
   */
  private Action createMenuBar() {
    // set up the actions
    final Action openAction = SwingAction.of("Load Map XML", e -> {
      final int goToStep;
      try {
        goToStep = MapXMLHelper.loadXML();
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(null, ex.getMessage(), "Error while loading XML", JOptionPane.ERROR_MESSAGE);
        return;
      }
      highest_step = 1;
      DynamicRowsPanel.me = Optional.empty();
      if (goToStep > 0) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            goToStep(goToStep);
          }
        });
      }
      validateAndRepaint();
    });
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load an existing Map XML File");
    final Action saveAction = SwingAction.of("Save Map XML", e -> {
      stepActionPanel.requestFocus();
      if (!DynamicRowsPanel.me.isPresent() || DynamicRowsPanel.me.get().dataIsConsistent())
        MapXMLHelper.saveXML();
    });
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save the Map XML to File");
    final Action exitAction = new AbstractAction("Exit") {
      private static final long serialVersionUID = -9212762817640498442L;

      public void actionPerformed(final ActionEvent event) {
        System.exit(0);
      }
    };
    exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit The Program");
    // set up the menu items
    final JMenuItem openItem = new JMenuItem(openAction);
    openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
    final JMenuItem saveItem = new JMenuItem(saveAction);
    saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
    final JMenuItem exitItem = new JMenuItem(exitAction);
    // set up the menu bar
    final JMenuBar menuBar = new JMenuBar();
    final JMenu fileMenu = new JMenu("File");
    fileMenu.setMnemonic(KeyEvent.VK_F);
    fileMenu.add(openItem);
    fileMenu.add(saveItem);
    fileMenu.addSeparator();
    fileMenu.add(exitItem);
    menuBar.add(fileMenu);
    setJMenuBar(menuBar);
    return openAction;
  }

  private void createStepList() {
    stepListPanel = new JPanel();
    stepList.add(new JLabel("1: Map Properties"));
    stepList.add(new JLabel("2: Territory Definitions"));
    stepList.add(new JLabel("3: Territory Connections"));
    stepList.add(new JLabel("4: Players and Alliances"));
    stepList.add(new JLabel("5: Unit Definitions"));
    stepList.add(new JLabel("6: Gameplay Sequence"));
    stepList.add(new JLabel("7: Player Sequence"));
    stepList.add(new JLabel("8: Technology Definitions"));
    stepList.add(new JLabel("9: Production Frontiers"));
    stepList.add(new JLabel("10: Unit Attachments"));
    stepList.add(new JLabel("11: Territory Production"));
    stepList.add(new JLabel("12: Canal Definitions"));
    stepList.add(new JLabel("13: Territory Ownership"));
    stepList.add(new JLabel("14: Unit Placements"));
    stepList.add(new JLabel("15: Game Settings"));
    stepList.add(new JLabel("16: Map finished!"));
  }

  private void layoutSouthPanel() {
    mainPanel.add(southPanel, BorderLayout.SOUTH);
    southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));

    southLeftPanel = new JPanel();
    southLeftPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    FlowLayout southLeftPanelFlowLayout = (FlowLayout) southLeftPanel.getLayout();
    southLeftPanelFlowLayout.setAlignment(FlowLayout.LEFT);
    southLeftPanel.setBorder(null);
    southPanel.add(southLeftPanel);

    southPanel.add(Box.createHorizontalGlue());

    createButtonHelp();
    southLeftPanel.add(buttonHelp);

    southCenterPanel = new JPanel();
    southPanel.add(southCenterPanel);

    southPanel.add(Box.createHorizontalGlue());

    addSouthCenterPanelButtons();

    southRightPanel = new JPanel();
    FlowLayout southRightPanelFlowLayout = (FlowLayout) southRightPanel.getLayout();
    southRightPanelFlowLayout.setAlignment(FlowLayout.RIGHT);
    southPanel.add(southRightPanel);

    buttonAvailableChoices = MapXMLHelper.createButton("Available Choices", KeyEvent.VK_C);
    buttonAvailableChoices.addActionListener(e -> {
      switch (currentStep) {
        case 1: // TODO: Verify message text.
                // showInfoMessage("Map Name: The map name that is displayed in the New Game window in TripleA.
                // Examples: Revised, Classic, Big World, and Great War\r\rMap Version: The version of the map's
                // xml file. Examples: 0.0.1, 1.0.0.1, and 1.1\r\rResource Name: The name of the resource used in
                // the map. Resources can be thought of as buying tokens or currency used to buy units, technology,
                // etc. 'IPCs' have recently been replaced by 'PUs'. 'IPCs' will no longer work as the map resource.
                // \r\rMap Image Location: The location of the map image. Example: C:/My Maps/Sleeping
                // Giant/full_map.png\r\rMap Centers File: The location of the centers file produced by the
                // 'Center Picker' program. The centers file is used to automatically add the map's territories.
                // Example: C:/My Maps/Sleeping Giant/centers.txt\r\rWater Territory Filter: An optional setting
                // that makes the program automatically apply the 'Is Water' property to every territory that contains
                // the filter text. Examples: SZ, Sea Zone, Pacific, and Atlantic.", "Help For Current Step");
          break;
        case 2: // TODO: Verify message text.
                // showInfoMessage("To add a new territory, click somewhere on the map and enter a name for the
                // territory in the window that appears. \r If you want to change the properties of a territory,
                // left click on it and answer each question. The color of the territory label changes for each
                // property that is applies. If you want to remove a territory label, right click on it and click yes
                // when it asks for confirmation.", "Help For Current Step");
          // showInfoMessage("Right click to rename a territory. \r If you want to change the properties of a
          // territory, left click on it. \r The color of the territory label changes for each property that is
          // applied.", "Help For Current Step");
          break;
        case 3: // TODO: Verify message text.
                // showInfoMessage("To add a connection between two territories, click on the first territory in the
                // connection and then the second. To remove all the connections from a certain territory, right click
                // on it and click yes.\r\rNote: To have the program find the connections automatically, click the
                // 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;
        case 4: // TODO: Verify message text.
                // showInfoMessage("Player Name: The name of the player. Examples: Russians, Germans, British,
                // Americans, Chinese, and Italians.\r\rPlayer Alliance: The name of the alliance that the player
                // is part of. Examples: Allies, and Axis.\r\rInitial Resources: The amount of resources(PUs) the
                // player begins with when the map is started.", "Help For Current Step");
          break;
        case 5: // TODO: Verify message text.
                // showInfoMessage("Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter,
                // bomber, and transport.\r\rBuy Cost: The amount of resources it takes to buy the
                // unit.\r\rBuy Quantity: The amount of units to be placed with each purchase of the
                // unit.\r\rTo have the program automatically enter some of the commonly used units, click the
                // 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;
        case 6:
          showInfoMessage(
              "Here is a list of all the available gameplay sequences with their class name and display name:\r\r initDelegate-InitializationDelegate-Initializing Delegates,\r bid-BidPurchaseDelegate-Bid Purchase,\r placeBid-BidPlaceDelegate-Bid Placement,\r tech-TechnologyDelegate-Research Technology,\r tech_Activation-TechActivationDelegate-Activate Technology,\r purchase-PurchaseDelegate-Purchase Units,\r move-MoveDelegate-Combat Move,\r battle-BattleDelegate-Combat,\r place-PlaceDelegate-Place Units,\r endTurn-EndTurnDelegate-Turn Complete,\r endRound-EndRoundDelegate-Round Complete",
              "List Of Available Gameplay Sequence Choices (As Of TripleA 1.0.3.4)");
          break;
        case 7: // TODO: Verify message text.
                // showInfoMessage("Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace,
                // chineseTech, americanCombatMove, and germanPlace.\r\rGameplay Sequence: The name of the
                // Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place,
                // endTurn.\r\rPlayer: The name of the player that the Player Delegate is attached to. Examples:
                // Russians, Germans, Americans, and Chinese.\r\rMax Run Count: The maximum number of times the
                // Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to
                // have a run limit).\r\rTo have the program automatically enter the default Player Sequences,
                // click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;
        case 8:
          showInfoMessage(
              "Here is a list of all the available technology types: heavyBomber, longRangeAir, jetPower, rocket, industrialTechnology, superSub, destroyerBombard, improvedArtillerySupport, paratroopers, increasedFactoryProduction, warBonds, mechanizedInfantry, aARadar, shipyards",
              "List Of Available Technology Choices (As Of TripleA 1.0.3.4)");
          break;
        case 9: // TODO: Verify message text.
                // showInfoMessage("Unit Name: The name of the unit that is included in the production frontier.
                // Examples: infantry, artillery, and armour.\r\rTo have the program automatically add all the
                // units to the production frontier being shown, click the 'Auto-Fill' button between the Back and
                // Next buttons.\r\rNote: A production frontier is just a list of units that a player is allowed
                // to purchase.", "Help For Current Step");
          break;
        case 10:
          showInfoMessage(
              "Here is a list of all the available unit attachments: movement, attack, defense, isAir, isSea, isAA, isFactory, canBlitz, isSub, canBombard, isStrategicBomber, isTwoHit, isDestroyer, isArtillery, isArtillerySupportable, isMarine, isInfantry, isParatroop, isMechanized, transportCapacity, transportCost, carrierCapacity, carrierCost",
              "List Of Available Unit Attachment Choices (As Of TripleA 1.0.3.4)");
          break;
        case 11: // TODO: Verify message text.
                 // showInfoMessage("To change the production value of a territory, click on the territory and enter
                 // the territory's new production value in the window that appears.", "Help For Current Step");
          break;
        case 12: // TODO: Verify message text.
                 // showInfoMessage("To add a new canal, click on both of the land territories that form the
                 // canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from
                 // the map, right click on one of the territories and click 'Yes' to confirm.", "Help For Current
                 // Step");
          break;
        case 13: // TODO: Verify message text.
                 // showInfoMessage("To change the initial owner of a territory, click on the territory and type in
                 // its new owner when it asks for it.", "Help For Current Step");
          break;
        case 14: // TODO: Verify message text.
                 // showInfoMessage("To change the units that a territory begins with, click on the territory and use
                 // the window that opens to set the territory's units. To do so, use the + and - buttons below each
                 // unit's name to change the quantity of each unit in the territory. You can also edit the text in
                 // the result textbox directly to change the territory units. Just type the name of each unit you
                 // want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3,
                 // armour: 2, fighter: 1'.", "Help For Current Step");
          break;
        case 15:
          showInfoMessageOfAvailableGameSettingsChoices();
          break;
        case 16:
          showInfoMessage(
              "The map notes can be entered in the textbox. You can enter plain text or html code. Then just click the 'Save Map To File' button, and save the file in the map's 'games' folder. (Create the 'games' folder if not already created)",
              "Help For Current Step");
          break;
      }
    });
    southRightPanel.add(buttonAvailableChoices);
  }

  private void createButtonHelp() {
    buttonHelp = new JButton("Help");
    buttonHelp.setMnemonic(KeyEvent.VK_H);
    buttonHelp.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    buttonHelp.addActionListener(e -> {
      switch (currentStep) {
        case 1:
          showInfoMessage(
              "Map Name: The map name that is displayed in the New Game window in TripleA. Examples: Revised, Classic, Big World, and Great War\r\rMap Version: The version of the map's xml file. Examples: 0.0.1, 1.0.0.1, and 1.1\r\rResource Name: The name of the resource used in the map. Resources can be thought of as buying tokens or currency used to buy units, technology, etc. 'IPCs' have recently been replaced by 'PUs'. 'IPCs' will no longer work as the map resource. \r\rMap Image Location: The location of the map image. Example: C:/My Maps/Sleeping Giant/full_map.png\r\rMap Centers File: The location of the centers file produced by the 'Center Picker' program. The centers file is used to automatically add the map's territories. Example: C:/My Maps/Sleeping Giant/centers.txt\r\rWater Territory Filter: An optional setting that makes the program automatically apply the 'Is Water' property to every territory that contains the filter text. Examples: SZ, Sea Zone, Pacific, and Atlantic.",
              "Help For Current Step");
          break;
        case 2: // showInfoMessage("To add a new territory, click somewhere on the map and enter a name for the
                // territory in the window that appears. \r If you want to change the properties of a territory,
                // left click on it and answer each question. The color of the territory label changes for each
                // property that is applies. If you want to remove a territory label, right click on it and click yes
                // when it asks for confirmation.", "Help For Current Step");
          showInfoMessage(
              "Right click to rename a territory. \r If you want to change the properties of a territory, left click on it. \r The color of the territory label changes for each property that is applied.",
              "Help For Current Step");
          break;
        case 3:
          showInfoMessage(
              "To add a connection between two territories, click on the first territory in the connection and then the second. To remove all the connections from a certain territory, right click on it and click yes.\r\rNote: To have the program find the connections automatically, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 4:
          showInfoMessage(
              "Player Name: The name of the player. Examples: Russians, Germans, British, Americans, Chinese, and Italians.\r\rPlayer Alliance: The name of the alliance that the player is part of. Examples: Allies, and Axis.\r\rInitial Resources: The amount of resources(PUs) the player begins with when the map is started.",
              "Help For Current Step");
          break;
        case 5:
          showInfoMessage(
              "Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter, bomber, and transport.\r\rBuy Cost: The amount of resources it takes to buy the unit.\r\rBuy Quantity: The amount of units to be placed with each purchase of the unit.\r\rTo have the program automatically enter some of the commonly used units, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 6:
          showInfoMessage(
              "Sequence Name: The name of the sequence. Examples(Typical): tech, techActivation, battle, move, place, purchase, endTurn, placeBid, bid.\r\rClass Name: The name of the java delegate. Examples(Typical): TechnologyDelegate, TechActivationDelegate, BattleDelegate, MoveDelegate, PlaceDelegate, PurchaseDelegate, EndTurnDelegate, BidPlaceDelegate, BidPurchaseDelegate.\r\rDisplay: The text displayed for the delegate in TripleA. Examples(Typical): Research Technology, Activate Technology, Combat, Combat Move, Place Units, Purchase Units, Turn Complete, Bid Placement, and Bid Purchase\r\rTo have the program automatically enter the default Gameplay Sequences, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 7:
          showInfoMessage(
              "Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace, chineseTech, americanCombatMove, and germanPlace.\r\rGameplay Sequence: The name of the Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place, endTurn.\r\rPlayer: The name of the player that the Player Delegate is attached to. Examples: Russians, Germans, Americans, and Chinese.\r\rMax Run Count: The maximum number of times the Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to have a run limit).\r\rTo have the program automatically enter the default Player Sequences, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 8:
          showInfoMessage(
              "Technology Name: The name of the technology that can be researched and unlocked by the player. Examples(Typical): heavyBomber, jetPower, industrialTechnology, superSub, rocket, and longRangeAir.\r\rPlayer: The name of the player that is able to research the technology. Examples: Russians, Germans, Americans, and Chinese.\r\rAlready Enabled: Determines if the technology should be unlocked for the selected player when the game first starts.\r\rTo have the program automatically enter some of the commonly used technologies, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 9:
          showInfoMessage(
              "Unit Name: The name of the unit that is included in the production frontier. Examples: infantry, artillery, and armour.\r\rTo have the program automatically add all the units to the production frontier being shown, click the 'Auto-Fill' button between the Back and Next buttons.\r\rNote: A production frontier is just a list of units that a player is allowed to purchase.",
              "Help For Current Step");
          break;
        case 10:
          showInfoMessage(
              "Attachment Name: The name of the unit attachment that is applied to the unit. Examples: movement, attack, defense, isAir, isSea, and isStrategicBomber.\r\rValue: The attachment value. Examples: True, False, 1, 2\r\rTo have the program automatically enter the default attachments for the units that are commonly used, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 11:
          showInfoMessage(
              "To change the production value of a territory, click on the territory and enter the territory's new production value in the window that appears.",
              "Help For Current Step");
          break;
        case 12:
          showInfoMessage(
              "To add a new canal, click on both of the land territories that form the canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from the map, right click on one of the territories and click 'Yes' to confirm.",
              "Help For Current Step");
          break;
        case 13:
          showInfoMessage(
              "To change the initial owner of a territory, click on the territory and type in its new owner when it asks for it.",
              "Help For Current Step");
          break;
        case 14:
          showInfoMessage(
              "To change the units that a territory begins with, click on the territory and use the window that opens to set the territory's units. To do so, use the + and - buttons below each unit's name to change the quantity of each unit in the territory. You can also edit the text in the result textbox directly to change the territory units. Just type the name of each unit you want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3, armour: 2, fighter: 1'.",
              "Help For Current Step");
          break;
        case 15:
          showInfoMessage(
              "Setting Name: The name of the setting that is applied to the map. Examples: Always on AA, Two hit battleship, and Japanese bid.\r\rValue: The value of the game setting. Examples: true, false, 0, 5, 32.\r\rEditable: Whether players are allowed to change the value of the setting when the map is being started.\r\rMin. N. (Optional): The lowest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\rMax. N.(Optional): The highest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\rTo have the program automatically enter some of the commonly used Game Settings, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;
        case 16:
          showInfoMessage(
              "The map notes can be entered in the textbox. You can enter plain text or html code. Then just click the 'Save Map To File' button, and save the file in the map's 'games' folder. (Create the 'games' folder if not already created)",
              "Help For Current Step");
          break;
      }
    });
  }

  /**
   * 
   */
  private void addSouthCenterPanelButtons() {
    backButton = new JButton("Back");
    backButton.setMnemonic(KeyEvent.VK_B);
    backButton.setFont(MapXMLHelper.defaultMapXMLCreatorFont);
    backButton.addActionListener(e -> {
      if (currentStep > 1) {
        goToStep(currentStep - 1);
        SwingUtilities.invokeLater(() -> {
          actionPanel.validate();
          actionPanel.repaint();
        });
      }
    });
    southCenterPanel.add(backButton);

    autoFillButton = MapXMLHelper.createButton("Auto-Fill", KeyEvent.VK_A);
    autoFillButton.setMinimumSize(new Dimension(50, 23));
    autoFillButton.setMaximumSize(new Dimension(50, 23));
    southCenterPanel.add(autoFillButton);

    nextButton = MapXMLHelper.createButton("Next", KeyEvent.VK_N, e -> {
      if (currentStep < stepList.size()) {
        goToStep(currentStep + 1);
        actionPanel.validate();
        actionPanel.repaint();
      }
    });
    southCenterPanel.add(nextButton);
  }

  private void showInfoMessageOfAvailableGameSettingsChoices() {
    /*
     * Build alphabetically sorted table in HTML of the setting names
     * in which the first letter is displayed bigger in case it is
     * different to the previous first letter to improve readability
     */
    final String[] settings = GameSettingsPanel.allSettings.split(", ");
    Arrays.sort(settings);
    StringBuilder sb = new StringBuilder();
    sb.append(
        "<html>Here is a list of all the available game settings:<table border=0><tr><td><table border=0>");
    int settingNr = 1;
    char lastFirstLetteretter = 'z';
    final int third_size_setting = (settings.length + 2) / 3;
    for (final String settingName : settings) {
      char firstLetter = settingName.charAt(0);
      if (firstLetter == lastFirstLetteretter)
        sb.append("<tr><td>" + settingName + "</td></tr>");
      else
        sb.append("<tr><td><big>" + firstLetter + "</big>" + settingName.substring(1) + "</td></tr>");
      lastFirstLetteretter = firstLetter;
      if (settingNr % third_size_setting == 0)
        sb.append("</table></td><td><table border=0>");
      ++settingNr;
    }
    sb.append("</table></td></tr></table></html>");
    showInfoMessage(
        sb.toString(), "List Of Available Game Settings Choices");
  }

  private void showInfoMessage(final String message, final String title) {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  private void setupMainPanel(final JPanel panel) {
    mainPanel.removeAll();
    mainPanel.setLayout(new BorderLayout());
    panel.setBorder(new MatteBorder(1, 0, 1, 1, Color.DARK_GRAY));
    mainPanel.add(panel, BorderLayout.CENTER);
    validateAndRepaint();
  }

  void validateAndRepaint() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> validateAndRepaint());
      return;
    }
    mainPanel.revalidate();
    mainPanel.repaint();
    this.validate();
    this.repaint();
  }


  final private String stepLabelFontName = MapXMLHelper.defaultMapXMLCreatorFontName;
  final private Font stepLabelFontDefault = new Font(stepLabelFontName, Font.PLAIN, 13);
  final private Font stepLabelFontHighlighted = new Font(stepLabelFontName, Font.BOLD, 13);

  private void goToStep(final int step) {
    if (currentStep >= 1) {
      final JLabel stepLabelOld = stepList.get(currentStep - 1);
      stepLabelOld.setFont(stepLabelFontDefault);
      stepLabelOld.repaint();
    }

    if (step <= stepList.size()) {
      currentStep = step;
      highest_step = Math.max(highest_step, step);
      layoutStepActionPanel();

      final JLabel stepLabelNew = stepList.get(step - 1);
      stepLabelNew.setFont(stepLabelFontHighlighted);
      stepLabelNew.repaint();

      setStepTitleLabelTextFromStepLabel(stepLabelNew);

      if (currentStep == stepList.size()) {
        nextButton.setEnabled(false);
      } else {
        nextButton.setEnabled(true);
      }

      if (step > 1) {
        backButton.setEnabled(true);
      } else {
        backButton.setEnabled(false);
      }
    }
  }

  protected void setStepTitleLabelTextFromStepLabel(JLabel stepLabel) {
    String title = stepLabel.getText();
    final String titleSep = ": ";
    final int titleSepPos = title.indexOf(titleSep);
    title = title.substring(titleSepPos + titleSep.length(), title.length());
    stepTitleLabel.setText(title);
  }

  public void setAutoFillAction(final AbstractAction action) {
    for (final ActionListener actionListener : autoFillButton.getActionListeners())
      autoFillButton.removeActionListener(actionListener);
    autoFillButton.addActionListener(action);
    autoFillButton.setEnabled(true);
    SwingUtilities.invokeLater(() -> autoFillButton.repaint());
  }

  private void layoutStepActionPanel() {
    stepActionPanel.removeAll();
    autoFillButton.setEnabled(false);
    switch (currentStep) {
      case 1:
        MapPropertiesPanel.layout(this, stepActionPanel);
        break;
      case 2:
        TerritoryDefinitionsPanel.layout(this, stepActionPanel);
        break;
      case 3:
        TerritoryConnectionsPanel.layout(this, stepActionPanel);
        break;
      case 4:
        PlayerAndAlliancesPanel.layout(this, stepActionPanel);
        break;
      case 5:
        UnitDefinitionsPanel.layout(this, stepActionPanel);
        break;

      case 6:
        GameSequencePanel.layout(this, stepActionPanel);
        break;

      case 7:
        PlayerSequencePanel.layout(this, stepActionPanel);
        break;

      case 8:
        TechnologyDefinitionsPanel.layout(this, stepActionPanel);
        break;

      case 9:
        layoutTabbedPaneWith(MapXMLHelper.productionFrontiers.keySet());
        break;

      case 10:
        layoutTabbedPaneWith(MapXMLHelper.unitDefinitions.keySet());
        break;

      case 11:
        TerritoryProductionPanel.layout(this, stepActionPanel);
        break;

      case 12:
        CanalDefinitionsPanel.layout(this, stepActionPanel);
        break;

      case 13:
        TerritoryOwnershipPanel.layout(this, stepActionPanel);
        break;

      case 14:
        UnitPlacementsPanel.layout(this, stepActionPanel);
        break;

      case 15:
        GameSettingsPanel.layout(this, stepActionPanel);
        break;

      case 16:
        layoutNotesPanel();

      default:
        break;
    }
    final JFrame me = this;
    SwingUtilities.invokeLater(() -> {
      me.pack();
      stepActionPanel.revalidate();
      stepActionPanel.repaint();
    });
  }

  protected void layoutNotesPanel() {
    stepActionPanel.setLayout(new GridBagLayout());

    final JTextArea taNotes = new JTextArea(MapXMLHelper.notes);
    taNotes.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(FocusEvent arg0) {
        if (!MapXMLHelper.notes.equals(taNotes.getText())) {
          MapXMLHelper.notes = taNotes.getText().trim();
        }
      }

      @Override
      public void focusGained(FocusEvent arg0) {}
    });
    final JScrollPane spTaNotes = new JScrollPane(taNotes);

    Dimension size = stepActionPanel.getSize();
    size.height -= 100;
    spTaNotes.setMinimumSize(size);

    GridBagConstraints gridBadConstLabelNotesRow = new GridBagConstraints();
    gridBadConstLabelNotesRow.insets = new Insets(0, 0, 5, 0);
    gridBadConstLabelNotesRow.gridy = 0;
    gridBadConstLabelNotesRow.gridx = 0;
    gridBadConstLabelNotesRow.anchor = GridBagConstraints.NORTH;
    gridBadConstLabelNotesRow.weightx = 1.0;
    gridBadConstLabelNotesRow.weighty = 1.0;
    gridBadConstLabelNotesRow.gridwidth = 3;
    stepActionPanel.add(spTaNotes, gridBadConstLabelNotesRow);

    GridBagConstraints gridBadConstButtonPreviewHTMLRow = (GridBagConstraints) gridBadConstLabelNotesRow.clone();
    gridBadConstButtonPreviewHTMLRow.weighty = 0.0;
    gridBadConstButtonPreviewHTMLRow.insets = new Insets(0, 0, 0, 0);
    gridBadConstButtonPreviewHTMLRow.gridwidth = 1;
    gridBadConstButtonPreviewHTMLRow.gridx = 1;
    gridBadConstButtonPreviewHTMLRow.gridy = 1;

    final JButton buttonPreviewHTML = new JButton("Preview HTML");
    buttonPreviewHTML.setPreferredSize(new Dimension(300, 30));
    buttonPreviewHTML.setAction(SwingAction.of("Preview HTML", e -> showHTML(MapXMLHelper.notes, "HTML Preview")));
    stepActionPanel.add(buttonPreviewHTML, gridBadConstButtonPreviewHTMLRow);

    GridBagConstraints gridBadConstLabelCongratsRow = (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
    gridBadConstLabelCongratsRow.gridy = 2;
    stepActionPanel.add(new JLabel("<html><big>Congratulation!</big></html>"), gridBadConstLabelCongratsRow);

    GridBagConstraints gridBadConstLabelAllCompletedRow = (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
    gridBadConstLabelAllCompletedRow.gridy = 3;
    stepActionPanel.add(
        new JLabel("<html><nobr>You have completed all the steps for creating the map XML.</nobr></html>"),
        gridBadConstLabelAllCompletedRow);

    final JButton buttonSave = new JButton("Save Entries to XML");
    buttonSave.setPreferredSize(new Dimension(600, 35));
    buttonSave.setAction(SwingAction.of("Save Map XML", e -> MapXMLHelper.saveXML()));
    GridBagConstraints gridBadConstButtonSaveRow = (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
    gridBadConstButtonSaveRow.insets = new Insets(5, 0, 0, 0);
    gridBadConstButtonSaveRow.gridy = 4;
    stepActionPanel.add(buttonSave, gridBadConstButtonSaveRow);
  }

  protected void layoutTabbedPaneWith(final Set<String> keySet) {
    stepActionPanel.setLayout(new BorderLayout());
    final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
    for (final String key : keySet) {
      final JPanel innerTabPane = new JPanel();
      tabbedPane.addTab(key, innerTabPane);
      ProductionFrontiersPanel.layout(this, innerTabPane, key);
    }
    stepActionPanel.add(tabbedPane, BorderLayout.CENTER);
  }

  public void showHTML(final String htmlString, final String title) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {

        JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(jEditorPane);

        HTMLEditorKit kit = new HTMLEditorKit();
        jEditorPane.setEditorKit(kit);

        Document doc = kit.createDefaultDocument();
        jEditorPane.setDocument(doc);
        jEditorPane.setText(htmlString);

        JFrame j = new JFrame(title);
        j.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // make it easy to close the application
        j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // display the frame
        j.setSize(new Dimension(800, 600));

        // center the jframe, then make it visible
        j.setLocationRelativeTo(null);
        j.setVisible(true);
      }
    });
  }
}
