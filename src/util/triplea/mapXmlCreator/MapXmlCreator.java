package util.triplea.mapXmlCreator;

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
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.ParseException;

import games.strategy.common.swing.SwingAction;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import util.image.FileSave;

/**
 * A frame that will show the different steps creating a game XML.
 */
public class MapXmlCreator extends JFrame {
  public static boolean testMode = false;

  private static final String BUTTON_LABEL_SAVE_MAP_XML = "Save Map XML";
  private static final String FILE_NAME_CENTERS_TXT = "centers.txt";
  static final String FILE_NAME_ENDING_XML = ".xml";
  private static final String FILE_NAME_ENDING_GIF = ".gif";
  public static final String FILE_NAME_ENDING_PNG = ".png";
  private static final long serialVersionUID = 3593102638082774498L;
  public static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
  public static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
  public static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
  public static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";

  private GAME_STEP highestStep = GAME_STEP_FIRST;

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
  private final ArrayList<GameStepLabel> stepList = new ArrayList<GameStepLabel>();
  private final JPanel southPanel = new JPanel();
  private JPanel southLeftPanel;
  private JPanel southRightPanel;
  private JPanel southCenterPanel;
  private JButton buttonHelp;
  private JButton buttonAvailableChoices;
  private JButton buttonBack;
  private JButton autoFillButton;
  private JButton nextButton;
  private final JPanel stepActionPanel;

  public JPanel getStepActionPanel() {
    return stepActionPanel;
  }


  private final JLabel stepTitleLabel;
  private final JPanel actionPanel;
  private GAME_STEP currentStep = GAME_STEP_FIRST;

  final public static GAME_STEP GAME_STEP_FIRST = GAME_STEP.MAP_PROPERTIES;
  public static final String MAP_XML_CREATOR_LOGGER_NAME = "Logger for Map XML Creation";

  public static enum GAME_STEP {
    MAP_PROPERTIES, TERRITORY_DEFINITIONS, TERRITORY_CONNECTIONS, PLAYERS_AND_ALLIANCES, UNIT_DEFINITIONS, GAMEPLAY_SEQUENCE, PLAYER_SEQUENCE, TECHNOLOGY_DEFINITIONS, PRODUCTION_FRONTIERS, UNIT_ATTACHMENTS, TERRITORY_PRODUCTION, CANAL_DEFINITIONS, TERRITORY_OWNERSHIP, UNIT_PLACEMENTS, GAME_SETTINGS, MAP_FINISHED
  }

  public static GAME_STEP getMaxGameStep(final GAME_STEP step1, final GAME_STEP step2) {
    return (step1.ordinal() >= step2.ordinal()) ? step1 : step2;
  }

  private static String getGameStepName(final GAME_STEP step) {
    switch (step) {
      case MAP_PROPERTIES:
        return "Map Properties";
      case TERRITORY_DEFINITIONS:
        return "Territory Definitions";
      case TERRITORY_CONNECTIONS:
        return "Territory Connections";
      case PLAYERS_AND_ALLIANCES:
        return "Players and Alliances";
      case UNIT_DEFINITIONS:
        return "Unit Definitions";
      case GAMEPLAY_SEQUENCE:
        return "Gameplay Sequence";
      case PLAYER_SEQUENCE:
        return "Player Sequence";
      case TECHNOLOGY_DEFINITIONS:
        return "Technology Definitions";
      case PRODUCTION_FRONTIERS:
        return "Production Frontiers";
      case UNIT_ATTACHMENTS:
        return "Unit Attachments";
      case TERRITORY_PRODUCTION:
        return "Territory Production";
      case CANAL_DEFINITIONS:
        return "Canal Definitions";
      case TERRITORY_OWNERSHIP:
        return "Territory Ownership";
      case UNIT_PLACEMENTS:
        return "Unit Placements";
      case GAME_SETTINGS:
        return "Game Settings";
      case MAP_FINISHED:
        return "Map finished!";
      default:
        throw new IllegalArgumentException(
            "Provided value is not valid for " + GAME_STEP.class);
    }
  }

  private static GAME_STEP getNextGameStepTo(final GAME_STEP step_cur) {
    switch (step_cur) {
      case MAP_PROPERTIES:
        return GAME_STEP.TERRITORY_DEFINITIONS;
      case TERRITORY_DEFINITIONS:
        return GAME_STEP.TERRITORY_CONNECTIONS;
      case TERRITORY_CONNECTIONS:
        return GAME_STEP.PLAYERS_AND_ALLIANCES;
      case PLAYERS_AND_ALLIANCES:
        return GAME_STEP.UNIT_DEFINITIONS;
      case UNIT_DEFINITIONS:
        return GAME_STEP.GAMEPLAY_SEQUENCE;
      case GAMEPLAY_SEQUENCE:
        return GAME_STEP.PLAYER_SEQUENCE;
      case PLAYER_SEQUENCE:
        return GAME_STEP.TECHNOLOGY_DEFINITIONS;
      case TECHNOLOGY_DEFINITIONS:
        return GAME_STEP.PRODUCTION_FRONTIERS;
      case PRODUCTION_FRONTIERS:
        return GAME_STEP.UNIT_ATTACHMENTS;
      case UNIT_ATTACHMENTS:
        return GAME_STEP.TERRITORY_PRODUCTION;
      case TERRITORY_PRODUCTION:
        return GAME_STEP.CANAL_DEFINITIONS;
      case CANAL_DEFINITIONS:
        return GAME_STEP.TERRITORY_OWNERSHIP;
      case TERRITORY_OWNERSHIP:
        return GAME_STEP.UNIT_PLACEMENTS;
      case UNIT_PLACEMENTS:
        return GAME_STEP.GAME_SETTINGS;
      case GAME_SETTINGS:
        return GAME_STEP.MAP_FINISHED;
      case MAP_FINISHED:
        return GAME_STEP.MAP_FINISHED;
      default:
        throw new IllegalArgumentException(
            "'" + step_cur + "' is not a valid string for " + GAME_STEP.class);
    }
  }

  private static GAME_STEP getPrevGameStepTo(final GAME_STEP step_cur) {
    switch (step_cur) {
      case MAP_PROPERTIES:
        return GAME_STEP.MAP_PROPERTIES;
      case TERRITORY_DEFINITIONS:
        return GAME_STEP.MAP_PROPERTIES;
      case TERRITORY_CONNECTIONS:
        return GAME_STEP.TERRITORY_DEFINITIONS;
      case PLAYERS_AND_ALLIANCES:
        return GAME_STEP.TERRITORY_CONNECTIONS;
      case UNIT_DEFINITIONS:
        return GAME_STEP.PLAYERS_AND_ALLIANCES;
      case GAMEPLAY_SEQUENCE:
        return GAME_STEP.UNIT_DEFINITIONS;
      case PLAYER_SEQUENCE:
        return GAME_STEP.GAMEPLAY_SEQUENCE;
      case TECHNOLOGY_DEFINITIONS:
        return GAME_STEP.PLAYER_SEQUENCE;
      case PRODUCTION_FRONTIERS:
        return GAME_STEP.TECHNOLOGY_DEFINITIONS;
      case UNIT_ATTACHMENTS:
        return GAME_STEP.PRODUCTION_FRONTIERS;
      case TERRITORY_PRODUCTION:
        return GAME_STEP.UNIT_ATTACHMENTS;
      case CANAL_DEFINITIONS:
        return GAME_STEP.TERRITORY_PRODUCTION;
      case TERRITORY_OWNERSHIP:
        return GAME_STEP.CANAL_DEFINITIONS;
      case UNIT_PLACEMENTS:
        return GAME_STEP.TERRITORY_OWNERSHIP;
      case GAME_SETTINGS:
        return GAME_STEP.UNIT_PLACEMENTS;
      case MAP_FINISHED:
        return GAME_STEP.GAME_SETTINGS;
      default:
        throw new IllegalArgumentException(
            "'" + step_cur + "' is not a valid string for " + GAME_STEP.class);
    }
  }

  public static String[] getProperties() {
    return new String[] {TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT};
  }


  final private static Logger logger = Logger.getLogger(MAP_XML_CREATOR_LOGGER_NAME);

  public static Logger getLogger() {
    return logger;
  }

  public static void main(final String[] args) {
    MapXmlCreator.getLogger().setLevel(Level.OFF);
    MapXmlCreator.log(Level.INFO, "Starting MapXMLCreator");

    // handleCommandLineArgs(args);
    GameRunner2.setupLookAndFeel();
    start();
  }

  public static void start() {
    SwingUtilities.invokeLater(() -> {
      final MapXmlCreator creator = new MapXmlCreator();
      creator.setSize(800, 600);
      creator.setPreferredSize(creator.getSize());
      creator.goToStep(GAME_STEP_FIRST);
      creator.setLocationRelativeTo(null);
      creator.setVisible(true);
    });
  }

  public MapXmlCreator(final boolean testMode) {
    this();
    MapXmlCreator.testMode = testMode;
  }

  public MapXmlCreator() {
    super("TripleA Map XML Creator");

    mapFolderLocation = getDefaultMapFolderLocation();
    // keep for the moment for test purposes
    // mapFolderLocation = new File("C:\\Users\\evdO\\triplea\\triplea_1_7_0_3\\maps\\minimap");
    // mapFolderLocation = new File("C:\\Users\\User\\workspace\\triplea\\maps\\minimap");
    final File myFile = getDefaultMapXmlFile();
    if (myFile.exists()) {
      try {
        loadXmlFromFilePath(myFile.getAbsolutePath());
      } catch (SAXException | IOException | ParserConfigurationException e) {
        // TODO Auto-generated catch block
        log(Level.SEVERE, "Default Map XML File could not be loaded from '" + myFile.getAbsolutePath() + "'.");
        e.printStackTrace();
        throw new IllegalStateException(
            "Default Map XML File could not be loaded from '" + myFile.getAbsolutePath() + "'.");
      }
    }

    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    final Action openAction = createMenuBar();

    // components
    mainPanel = new JPanel();
    sidePanel = new JPanel();
    createStepListPanel();

    sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
    sidePanel.add(Box.createVerticalStrut(2));
    sidePanel.add(stepListPanel);
    sidePanel.add(Box.createVerticalGlue());


    stepListPanel.setLayout(new GridLayout(stepList.size(), 1, 0, 10));
    stepListPanel.setBorder(new EmptyBorder(10, 2, 0, 5));
    stepListPanel.setPreferredSize(new Dimension(170, 410));

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
    stepTitleLabel.setFont(new Font(MapXmlUIHelper.defaultMapXMLCreatorFontName, Font.BOLD, 12));
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

    if (!myFile.exists()) {
      openAction.actionPerformed(null);
    }
  }

  /**
   * @return
   */
  public File getDefaultMapXmlFile() {
    return new File(mapFolderLocation.getAbsolutePath() + File.separator + "new_world_order"
        + File.separator + "games" + File.separator + "new_world_order.xml");
  }

  /**
   * @return
   */
  public File getDefaultMapFolderLocation() {
    return new File(ClientFileSystemHelper.getRootFolder() + File.separator + "maps");
  }

  /**
   * @return
   */
  private Action createMenuBar() {
    // set up the actions
    final Action openAction = SwingAction.of("Load Map XML", e -> {
      final GAME_STEP goToStep;
      goToStep = MapXmlCreator.loadXML();
      highestStep = goToStep;
      DynamicRowsPanel.me = Optional.empty();
      SwingUtilities.invokeLater(() -> {
        goToStep(goToStep);
        validateAndRepaint();
      });
    });
    openAction.putValue(Action.SHORT_DESCRIPTION, "Load an existing Map XML File");
    final Action saveAction = SwingAction.of(BUTTON_LABEL_SAVE_MAP_XML, e -> {
      stepActionPanel.requestFocus();
      if (!DynamicRowsPanel.me.isPresent() || DynamicRowsPanel.me.get().dataIsConsistent()) {
        MapXmlCreator.saveXML();
      }
    });
    saveAction.putValue(Action.SHORT_DESCRIPTION, "Save the Map XML to File");
    final Action exitAction = SwingAction.of("Exit", e -> System.exit(0));
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

  private void createStepListPanel() {
    stepListPanel = new JPanel();
    GAME_STEP step_cur = null;
    GAME_STEP step_next = GAME_STEP_FIRST;
    int stepCounter = 1;
    while (step_cur != step_next) {
      step_cur = step_next;
      final GameStepLabel gameStepLabelCur =
          new GameStepLabel(stepCounter + ": " + getGameStepName(step_cur), step_cur);
      stepListPanel.add(gameStepLabelCur);
      stepList.add(gameStepLabelCur);
      ++stepCounter;
      step_next = getNextGameStepTo(step_cur);
    }
  }

  private void layoutSouthPanel() {
    mainPanel.add(southPanel, BorderLayout.SOUTH);
    southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));

    southLeftPanel = new JPanel();
    southLeftPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
    final FlowLayout southLeftPanelFlowLayout = (FlowLayout) southLeftPanel.getLayout();
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
    final FlowLayout southRightPanelFlowLayout = (FlowLayout) southRightPanel.getLayout();
    southRightPanelFlowLayout.setAlignment(FlowLayout.RIGHT);
    southPanel.add(southRightPanel);

    buttonAvailableChoices = MapXmlUIHelper.createButton("Available Choices", KeyEvent.VK_C);
    buttonAvailableChoices.addActionListener(e -> {
      switch (currentStep) {

        case MAP_PROPERTIES: // TODO: Verify message text.
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

        case TERRITORY_DEFINITIONS: // TODO: Verify message text.
          // showInfoMessage("To add a new territory, click somewhere on the map and enter a name for the
          // territory in the window that appears. \r If you want to change the properties of a territory,
          // left click on it and answer each question. The color of the territory label changes for each
          // property that is applies. If you want to remove a territory label, right click on it and click yes
          // when it asks for confirmation.", "Help For Current Step");
          // showInfoMessage("Right click to rename a territory. \r If you want to change the properties of a
          // territory, left click on it. \r The color of the territory label changes for each property that is
          // applied.", "Help For Current Step");
          break;

        case TERRITORY_CONNECTIONS: // TODO: Verify message text.
          // showInfoMessage("To add a connection between two territories, click on the first territory in the
          // connection and then the second. To remove all the connections from a certain territory, right click
          // on it and click yes.\r\rNote: To have the program find the connections automatically, click the
          // 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;

        case PLAYERS_AND_ALLIANCES: // TODO: Verify message text.
          // showInfoMessage("Player Name: The name of the player. Examples: Russians, Germans, British,
          // Americans, Chinese, and Italians.\r\rPlayer Alliance: The name of the alliance that the player
          // is part of. Examples: Allies, and Axis.\r\rInitial Resources: The amount of resources(PUs) the
          // player begins with when the map is started.", "Help For Current Step");
          break;

        case UNIT_DEFINITIONS: // TODO: Verify message text.
          // showInfoMessage("Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter,
          // bomber, and transport.\r\rBuy Cost: The amount of resources it takes to buy the
          // unit.\r\rBuy Quantity: The amount of units to be placed with each purchase of the
          // unit.\r\rTo have the program automatically enter some of the commonly used units, click the
          // 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;

        case GAMEPLAY_SEQUENCE:
          showInfoMessage(
              "Here is a list of all the available gameplay sequences with their class name and display name:\r\r initDelegate-InitializationDelegate-Initializing Delegates,\r bid-BidPurchaseDelegate-Bid Purchase,\r placeBid-BidPlaceDelegate-Bid Placement,\r tech-TechnologyDelegate-Research Technology,\r tech_Activation-TechActivationDelegate-Activate Technology,\r purchase-PurchaseDelegate-Purchase Units,\r move-MoveDelegate-Combat Move,\r battle-BattleDelegate-Combat,\r place-PlaceDelegate-Place Units,\r endTurn-EndTurnDelegate-Turn Complete,\r endRound-EndRoundDelegate-Round Complete",
              "List Of Available Gameplay Sequence Choices (As Of TripleA 1.0.3.4)");
          break;

        case PLAYER_SEQUENCE: // TODO: Verify message text.
          // showInfoMessage("Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace,
          // chineseTech, americanCombatMove, and germanPlace.\r\rGameplay Sequence: The name of the
          // Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place,
          // endTurn.\r\rPlayer: The name of the player that the Player Delegate is attached to. Examples:
          // Russians, Germans, Americans, and Chinese.\r\rMax Run Count: The maximum number of times the
          // Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to
          // have a run limit).\r\rTo have the program automatically enter the default Player Sequences,
          // click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
          break;

        case TECHNOLOGY_DEFINITIONS:
          showInfoMessage(
              "Here is a list of all the available technology types: heavyBomber, longRangeAir, jetPower, rocket, industrialTechnology, superSub, destroyerBombard, improvedArtillerySupport, paratroopers, increasedFactoryProduction, warBonds, mechanizedInfantry, aARadar, shipyards",
              "List Of Available Technology Choices (As Of TripleA 1.0.3.4)");
          break;

        case PRODUCTION_FRONTIERS: // TODO: Verify message text.
          // showInfoMessage("Unit Name: The name of the unit that is included in the production frontier.
          // Examples: infantry, artillery, and armour.\r\rTo have the program automatically add all the
          // units to the production frontier being shown, click the 'Auto-Fill' button between the Back and
          // Next buttons.\r\rNote: A production frontier is just a list of units that a player is allowed
          // to purchase.", "Help For Current Step");
          break;

        case UNIT_ATTACHMENTS:
          showInfoMessage(
              "Here is a list of all the available unit attachments: movement, attack, defense, isAir, isSea, isAA, isFactory, canBlitz, isSub, canBombard, isStrategicBomber, isTwoHit, isDestroyer, isArtillery, isArtillerySupportable, isMarine, isInfantry, isParatroop, isMechanized, transportCapacity, transportCost, carrierCapacity, carrierCost",
              "List Of Available Unit Attachment Choices (As Of TripleA 1.0.3.4)");
          break;

        case TERRITORY_PRODUCTION: // TODO: Verify message text.
          // showInfoMessage("To change the production value of a territory, click on the territory and enter
          // the territory's new production value in the window that appears.", "Help For Current Step");
          break;

        case CANAL_DEFINITIONS: // TODO: Verify message text.
          // showInfoMessage("To add a new canal, click on both of the land territories that form the
          // canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from
          // the map, right click on one of the territories and click 'Yes' to confirm.", "Help For Current
          // Step");
          break;

        case TERRITORY_OWNERSHIP: // TODO: Verify message text.
          // showInfoMessage("To change the initial owner of a territory, click on the territory and type in
          // its new owner when it asks for it.", "Help For Current Step");
          break;

        case UNIT_PLACEMENTS: // TODO: Verify message text.
          // showInfoMessage("To change the units that a territory begins with, click on the territory and use
          // the window that opens to set the territory's units. To do so, use the + and - buttons below each
          // unit's name to change the quantity of each unit in the territory. You can also edit the text in
          // the result textbox directly to change the territory units. Just type the name of each unit you
          // want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3,
          // armour: 2, fighter: 1'.", "Help For Current Step");
          break;

        case GAME_SETTINGS:
          showInfoMessageOfAvailableGameSettingsChoices();
          break;

        case MAP_FINISHED:
          showInfoMessage(
              "The map notes can be entered in the textbox. You can enter plain text or html code. Then just click the '"
                  + BUTTON_LABEL_SAVE_MAP_XML
                  + "' button, and save the file in the map's 'games' folder. (Create the 'games' folder if not already created)",
              "Help For Current Step");
          break;
      }
    });
    southRightPanel.add(buttonAvailableChoices);
  }

  private void createButtonHelp() {
    buttonHelp = new JButton("Help");
    buttonHelp.setMnemonic(KeyEvent.VK_H);
    buttonHelp.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonHelp.addActionListener(e -> {
      switch (currentStep) {
        case MAP_PROPERTIES:
          showInfoMessage(
              "Map Name: The map name that is displayed in the New Game window in TripleA. Examples: Revised, Classic, Big World, and Great War\r\rMap Version: The version of the map's xml file. Examples: 0.0.1, 1.0.0.1, and 1.1\r\rResource Name: The name of the resource used in the map. Resources can be thought of as buying tokens or currency used to buy units, technology, etc. 'IPCs' have recently been replaced by 'PUs'. 'IPCs' will no longer work as the map resource. \r\rMap Image Location: The location of the map image. Example: C:/My Maps/Sleeping Giant/full_map.png\r\rMap Centers File: The location of the centers file produced by the 'Center Picker' program. The centers file is used to automatically add the map's territories. Example: C:/My Maps/Sleeping Giant/centers.txt\r\rWater Territory Filter: An optional setting that makes the program automatically apply the 'Is Water' property to every territory that contains the filter text. Examples: SZ, Sea Zone, Pacific, and Atlantic.",
              "Help For Current Step");
          break;

        case TERRITORY_DEFINITIONS: // showInfoMessage("To add a new territory, click somewhere on the map and enter a
                                    // name for the
          // territory in the window that appears. \r If you want to change the properties of a territory,
          // left click on it and answer each question. The color of the territory label changes for each
          // property that is applies. If you want to remove a territory label, right click on it and click yes
          // when it asks for confirmation.", "Help For Current Step");
          showInfoMessage(
              "Right click to rename a territory. \r If you want to change the properties of a territory, left click on it. \r The color of the territory label changes for each property that is applied.",
              "Help For Current Step");
          break;

        case TERRITORY_CONNECTIONS:
          showInfoMessage(
              "To add a connection between two territories, click on the first territory in the connection and then the second. To remove all the connections from a certain territory, right click on it and click yes.\r\rNote: To have the program find the connections automatically, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case PLAYERS_AND_ALLIANCES:
          showInfoMessage(
              "Player Name: The name of the player. Examples: Russians, Germans, British, Americans, Chinese, and Italians.\r\rPlayer Alliance: The name of the alliance that the player is part of. Examples: Allies, and Axis.\r\rInitial Resources: The amount of resources(PUs) the player begins with when the map is started.",
              "Help For Current Step");
          break;

        case UNIT_DEFINITIONS:
          showInfoMessage(
              "Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter, bomber, and transport.\r\rBuy Cost: The amount of resources it takes to buy the unit.\r\rBuy Quantity: The amount of units to be placed with each purchase of the unit.\r\rTo have the program automatically enter some of the commonly used units, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case GAMEPLAY_SEQUENCE:
          showInfoMessage(
              "Sequence Name: The name of the sequence. Examples(Typical): tech, techActivation, battle, move, place, purchase, endTurn, placeBid, bid.\r\rClass Name: The name of the java delegate. Examples(Typical): TechnologyDelegate, TechActivationDelegate, BattleDelegate, MoveDelegate, PlaceDelegate, PurchaseDelegate, EndTurnDelegate, BidPlaceDelegate, BidPurchaseDelegate.\r\rDisplay: The text displayed for the delegate in TripleA. Examples(Typical): Research Technology, Activate Technology, Combat, Combat Move, Place Units, Purchase Units, Turn Complete, Bid Placement, and Bid Purchase\r\rTo have the program automatically enter the default Gameplay Sequences, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case PLAYER_SEQUENCE:
          showInfoMessage(
              "Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace, chineseTech, americanCombatMove, and germanPlace.\r\rGameplay Sequence: The name of the Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place, endTurn.\r\rPlayer: The name of the player that the Player Delegate is attached to. Examples: Russians, Germans, Americans, and Chinese.\r\rMax Run Count: The maximum number of times the Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to have a run limit).\r\rTo have the program automatically enter the default Player Sequences, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case TECHNOLOGY_DEFINITIONS:
          showInfoMessage(
              "Technology Name: The name of the technology that can be researched and unlocked by the player. Examples(Typical): heavyBomber, jetPower, industrialTechnology, superSub, rocket, and longRangeAir.\r\rPlayer: The name of the player that is able to research the technology. Examples: Russians, Germans, Americans, and Chinese.\r\rAlready Enabled: Determines if the technology should be unlocked for the selected player when the game first starts.\r\rTo have the program automatically enter some of the commonly used technologies, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case PRODUCTION_FRONTIERS:
          showInfoMessage(
              "Unit Name: The name of the unit that is included in the production frontier. Examples: infantry, artillery, and armour.\r\rTo have the program automatically add all the units to the production frontier being shown, click the 'Auto-Fill' button between the Back and Next buttons.\r\rNote: A production frontier is just a list of units that a player is allowed to purchase.",
              "Help For Current Step");
          break;

        case UNIT_ATTACHMENTS:
          showInfoMessage(
              "Attachment Name: The name of the unit attachment that is applied to the unit. Examples: movement, attack, defense, isAir, isSea, and isStrategicBomber.\r\rValue: The attachment value. Examples: True, False, 1, 2\r\rTo have the program automatically enter the default attachments for the units that are commonly used, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case TERRITORY_PRODUCTION:
          showInfoMessage(
              "To change the production value of a territory, click on the territory and enter the territory's new production value in the window that appears.",
              "Help For Current Step");
          break;

        case CANAL_DEFINITIONS:
          showInfoMessage(
              "To add a new canal, click on both of the land territories that form the canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from the map, right click on one of the territories and click 'Yes' to confirm.",
              "Help For Current Step");
          break;

        case TERRITORY_OWNERSHIP:
          showInfoMessage(
              "To change the initial owner of a territory, click on the territory and type in its new owner when it asks for it.",
              "Help For Current Step");
          break;

        case UNIT_PLACEMENTS:
          showInfoMessage(
              "To change the units that a territory begins with, click on the territory and use the window that opens to set the territory's units. To do so, use the + and - buttons below each unit's name to change the quantity of each unit in the territory. You can also edit the text in the result textbox directly to change the territory units. Just type the name of each unit you want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3, armour: 2, fighter: 1'.",
              "Help For Current Step");
          break;

        case GAME_SETTINGS:
          showInfoMessage(
              "Setting Name: The name of the setting that is applied to the map. Examples: Always on AA, Two hit battleship, and Japanese bid.\r\rValue: The value of the game setting. Examples: true, false, 0, 5, 32.\r\rEditable: Whether players are allowed to change the value of the setting when the map is being started.\r\rMin. N. (Optional): The lowest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\rMax. N.(Optional): The highest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\rTo have the program automatically enter some of the commonly used Game Settings, click the 'Auto-Fill' button between the Back and Next buttons.",
              "Help For Current Step");
          break;

        case MAP_FINISHED:
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
    buttonBack = new JButton("Back");
    buttonBack.setMnemonic(KeyEvent.VK_B);
    buttonBack.setFont(MapXmlUIHelper.defaultMapXMLCreatorFont);
    buttonBack.addActionListener(e -> {
      goToStep(getPrevGameStepTo(currentStep));
      invokeLaterRepaintActionPanel();
    });
    southCenterPanel.add(buttonBack);

    autoFillButton = MapXmlUIHelper.createButton("Auto-Fill", KeyEvent.VK_A);
    autoFillButton.setMinimumSize(new Dimension(50, 23));
    autoFillButton.setMaximumSize(new Dimension(50, 23));
    southCenterPanel.add(autoFillButton);

    nextButton = MapXmlUIHelper.createButton("Next", KeyEvent.VK_N, e -> {
      goToStep(getNextGameStepTo(currentStep));
      invokeLaterRepaintActionPanel();
    });
    southCenterPanel.add(nextButton);
  }

  /**
   *
   */
  private void invokeLaterRepaintActionPanel() {
    SwingUtilities.invokeLater(() -> {
      actionPanel.validate();
      actionPanel.repaint();
    });
  }

  private void showInfoMessageOfAvailableGameSettingsChoices() {
    /*
     * Build alphabetically sorted table in HTML of the setting names
     * in which the first letter is displayed bigger in case it is
     * different to the previous first letter to improve readability
     */
    final String[] settings = GameSettingsPanel.allSettings.split(", ");
    Arrays.sort(settings);
    final StringBuilder sb = new StringBuilder();
    sb.append(
        "<html>Here is a list of all the available game settings:<table border=0><tr><td><table border=0>");
    int settingNr = 1;
    char lastFirstLetteretter = 'z';
    final int third_size_setting = (settings.length + 2) / 3;
    for (final String settingName : settings) {
      final char firstLetter = settingName.charAt(0);
      if (firstLetter == lastFirstLetteretter) {
        sb.append("<tr><td>" + settingName + "</td></tr>");
      } else {
        sb.append("<tr><td><big>" + firstLetter + "</big>" + settingName.substring(1) + "</td></tr>");
      }
      lastFirstLetteretter = firstLetter;
      if (settingNr % third_size_setting == 0) {
        sb.append("</table></td><td><table border=0>");
      }
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


  final private String stepLabelFontName = MapXmlUIHelper.defaultMapXMLCreatorFontName;
  final private Font stepLabelFontDefault = new Font(stepLabelFontName, Font.PLAIN, 13);
  final private Font stepLabelFontHighlighted = new Font(stepLabelFontName, Font.BOLD, 13);

  private void goToStep(final GAME_STEP step) {
    if (currentStep != step) {
      final GameStepLabel stepLabelOld = stepList.get(currentStep.ordinal());
      stepLabelOld.setFont(stepLabelFontDefault);
      stepLabelOld.repaint();
    }

    if (step.ordinal() < stepList.size()) {
      currentStep = step;
      if (step.ordinal() > highestStep.ordinal()) {
        highestStep = step;
      }
      layoutStepActionPanel();

      final GameStepLabel stepLabelNew = stepList.get(step.ordinal());
      stepLabelNew.setFont(stepLabelFontHighlighted);
      stepLabelNew.repaint();

      setStepTitleLabelTextFromStepLabel(stepLabelNew);

      if (currentStep.ordinal() + 1 == stepList.size()) {
        nextButton.setEnabled(false);
      } else {
        nextButton.setEnabled(true);
      }

      if (step != GAME_STEP_FIRST) {
        buttonBack.setEnabled(true);
      } else {
        buttonBack.setEnabled(false);
      }
    }
  }

  protected void setStepTitleLabelTextFromStepLabel(final JLabel stepLabel) {
    String title = stepLabel.getText();
    final String titleSep = ": ";
    final int titleSepPos = title.indexOf(titleSep);
    title = title.substring(titleSepPos + titleSep.length(), title.length());
    stepTitleLabel.setText(title);
  }

  public void setAutoFillAction(final AbstractAction action) {
    for (final ActionListener actionListener : autoFillButton.getActionListeners()) {
      autoFillButton.removeActionListener(actionListener);
    }
    autoFillButton.addActionListener(action);
    autoFillButton.setEnabled(true);
    SwingUtilities.invokeLater(() -> autoFillButton.repaint());
  }

  private void layoutStepActionPanel() {
    stepActionPanel.removeAll();
    autoFillButton.setEnabled(false);
    switch (currentStep) {

      case MAP_PROPERTIES:
        MapPropertiesPanel.layout(this);
        break;

      case TERRITORY_DEFINITIONS:
        TerritoryDefinitionsPanel.layout(this);
        break;

      case TERRITORY_CONNECTIONS:
        TerritoryConnectionsPanel.layout(this);
        break;
      case PLAYERS_AND_ALLIANCES:
        PlayerAndAlliancesPanel.layout(this);
        break;
      case UNIT_DEFINITIONS:
        UnitDefinitionsPanel.layout(this);
        break;

      case GAMEPLAY_SEQUENCE:
        GameSequencePanel.layout(this);
        break;

      case PLAYER_SEQUENCE:
        PlayerSequencePanel.layout(this);
        break;

      case TECHNOLOGY_DEFINITIONS:
        TechnologyDefinitionsPanel.layout(this);
        break;

      case PRODUCTION_FRONTIERS:
        layoutTabbedPaneWith(MapXmlHelper.getProductionFrontiersMap().keySet());
        break;

      case UNIT_ATTACHMENTS:
        layoutTabbedPaneWith(MapXmlHelper.getUnitDefinitionsMap().keySet());
        break;

      case TERRITORY_PRODUCTION:
        TerritoryProductionPanel.layout(this);
        break;

      case CANAL_DEFINITIONS:
        CanalDefinitionsPanel.layout(this);
        break;

      case TERRITORY_OWNERSHIP:
        TerritoryOwnershipPanel.layout(this);
        break;

      case UNIT_PLACEMENTS:
        UnitPlacementsPanel.layout(this);
        break;

      case GAME_SETTINGS:
        GameSettingsPanel.layout(this);
        break;


      case MAP_FINISHED:
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


  public void setAutoFillActionListener(final ActionListener autoFillActionListener) {
    if (autoFillActionListener == null) {
      autoFillButton.setEnabled(false);
    } else {
      autoFillButton.setEnabled(true);
      for (final ActionListener curr_actionListener : autoFillButton.getActionListeners()) {
        autoFillButton.removeActionListener(curr_actionListener);
      }
      autoFillButton.addActionListener(autoFillActionListener);
    }
  }

  protected void layoutNotesPanel() {
    stepActionPanel.setLayout(new GridBagLayout());

    final JTextArea taNotes = new JTextArea(MapXmlHelper.getNotes());
    taNotes.addFocusListener(new FocusListener() {

      @Override
      public void focusLost(final FocusEvent arg0) {
        if (!MapXmlHelper.getNotes().equals(taNotes.getText())) {
          MapXmlHelper.setNotes(taNotes.getText().trim());
        }
      }

      @Override
      public void focusGained(final FocusEvent arg0) {}
    });
    final JScrollPane spTaNotes = new JScrollPane(taNotes);

    final Dimension size = stepActionPanel.getSize();
    size.height -= 100;
    spTaNotes.setMinimumSize(size);

    final GridBagConstraints gridBadConstLabelNotesRow = new GridBagConstraints();
    gridBadConstLabelNotesRow.insets = new Insets(0, 0, 5, 0);
    gridBadConstLabelNotesRow.gridy = 0;
    gridBadConstLabelNotesRow.gridx = 0;
    gridBadConstLabelNotesRow.anchor = GridBagConstraints.NORTH;
    gridBadConstLabelNotesRow.weightx = 1.0;
    gridBadConstLabelNotesRow.weighty = 1.0;
    gridBadConstLabelNotesRow.gridwidth = 3;
    stepActionPanel.add(spTaNotes, gridBadConstLabelNotesRow);

    final GridBagConstraints gridBadConstButtonPreviewHTMLRow = (GridBagConstraints) gridBadConstLabelNotesRow.clone();
    gridBadConstButtonPreviewHTMLRow.weighty = 0.0;
    gridBadConstButtonPreviewHTMLRow.insets = new Insets(0, 0, 0, 0);
    gridBadConstButtonPreviewHTMLRow.gridwidth = 1;
    gridBadConstButtonPreviewHTMLRow.gridx = 1;
    gridBadConstButtonPreviewHTMLRow.gridy = 1;

    final JButton buttonPreviewHTML = new JButton("Preview HTML");
    buttonPreviewHTML.setPreferredSize(new Dimension(300, 30));
    buttonPreviewHTML.setAction(SwingAction.of("Preview HTML", e -> showHTML(MapXmlHelper.getNotes(), "HTML Preview")));
    stepActionPanel.add(buttonPreviewHTML, gridBadConstButtonPreviewHTMLRow);

    final GridBagConstraints gridBadConstLabelCongratsRow =
        (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
    gridBadConstLabelCongratsRow.gridy = 2;
    stepActionPanel.add(new JLabel("<html><big>Congratulation!</big></html>"), gridBadConstLabelCongratsRow);

    final GridBagConstraints gridBadConstLabelAllCompletedRow =
        (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
    gridBadConstLabelAllCompletedRow.gridy = 3;
    stepActionPanel.add(
        new JLabel("<html><nobr>You have completed all the steps for creating the map XML.</nobr></html>"),
        gridBadConstLabelAllCompletedRow);

    final JButton buttonSave = new JButton("Save Entries to XML");
    buttonSave.setPreferredSize(new Dimension(600, 35));
    buttonSave.setAction(SwingAction.of(BUTTON_LABEL_SAVE_MAP_XML, e -> MapXmlCreator.saveXML()));
    final GridBagConstraints gridBadConstButtonSaveRow = (GridBagConstraints) gridBadConstButtonPreviewHTMLRow.clone();
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
      @Override
      public void run() {

        final JEditorPane jEditorPane = new JEditorPane();
        jEditorPane.setEditable(false);

        final JScrollPane scrollPane = new JScrollPane(jEditorPane);

        final HTMLEditorKit kit = new HTMLEditorKit();
        jEditorPane.setEditorKit(kit);

        final Document doc = kit.createDefaultDocument();
        jEditorPane.setDocument(doc);
        jEditorPane.setText(htmlString);

        final JFrame j = new JFrame(title);
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

  static void saveXML() {
    try {
      final String fileName =
          new FileSave("Where to Save the Game XML ?",
              MapXmlHelper.getXmlStringsMap().get("info_@name") + FILE_NAME_ENDING_XML,
              mapFolderLocation).getPathString();
      if (fileName == null) {
        return;
      }

      // write the content into xml file
      final TransformerFactory transformerFactory = TransformerFactory.newInstance();
      final Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, GameParser.DTD_FILE_NAME);
      final DOMSource source = new DOMSource(MapXmlHelper.getXMLDocument());
      final File newFile = new File(fileName);
      final StreamResult result = new StreamResult(newFile);

      transformer.transform(source, result);

      Logger.getLogger(MapXmlCreator.MAP_XML_CREATOR_LOGGER_NAME).log(Level.INFO,
          "Game XML written to " + newFile.getCanonicalPath());
    } catch (final IOException | HeadlessException | TransformerException | ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  /**
   * Log a message, with no arguments.
   * <p>
   * If the logger is currently enabled for the given message
   * level then the given message is forwarded to all the
   * registered output Handler objects.
   * <p>
   *
   * @param level One of the message level identifiers, e.g., SEVERE
   * @param msg The string message (or a key in the message catalog)
   */
  static public void log(final Level level, final String msg) {
    getLogger().log(level, msg);
  }

  static GAME_STEP loadXML() {
    final String gameXMLPath =
        MapXmlUIHelper.selectFile("Game XML File", MapXmlHelper.getMapXMLFile(), FILE_NAME_ENDING_XML).getPathString();
    MapXmlCreator.log(Level.INFO, "Load Game XML from " + gameXMLPath);
    try {
      return loadXmlFromFilePath(gameXMLPath);
    } catch (final IOException | HeadlessException | ParserConfigurationException | ParseException | SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return loadXML();
    }
  }

  /**
   * @param gameXMLPath
   * @return
   * @throws FileNotFoundException
   * @throws SAXException
   * @throws IOException
   * @throws ParserConfigurationException
   */
  public static GAME_STEP loadXmlFromFilePath(final String gameXMLPath)
      throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
    final FileInputStream in = new FileInputStream(gameXMLPath);

    // parse using builder to get DOM representation of the XML file
    final org.w3c.dom.Document dom = new GameParser().getDocument(in);

    GAME_STEP goToStep = MapXmlHelper.parseValuesFromXML(dom);

    // set map file, image file and map folder
    MapXmlHelper.setMapXMLFile(new File(gameXMLPath));
    File mapFolder = MapXmlHelper.getMapXMLFile().getParentFile();
    if (mapFolder.getName().equals(GameSelectorModel.DEFAULT_GAME_XML_DIRECTORY_NAME)) {
      mapFolder = mapFolder.getParentFile();
    }
    mapFolderLocation = mapFolder;
    final File[] imageFiles = mapFolderLocation.listFiles(new FilenameFilter() {

      @Override
      public boolean accept(final File arg0, final String arg1) {
        return (arg1.endsWith(FILE_NAME_ENDING_GIF) || arg1.endsWith(FILE_NAME_ENDING_PNG));
      }
    });
    if (imageFiles.length == 1) {
      mapImageFile = imageFiles[0];
    } else {
      MapPropertiesPanel.selectMapImageFile();
    }

    final File fileGuess = new File(mapFolderLocation, FILE_NAME_CENTERS_TXT);
    if (fileGuess.exists()) {
      mapCentersFile = fileGuess;
    } else {
      MapPropertiesPanel.selectCentersFile();
    }

    if (mapImageFile == null || mapCentersFile == null) {
      goToStep = GAME_STEP_FIRST;
    }

    ImageScrollPanePanel.polygonsInvalid = true;

    return goToStep;
  }

  class GameStepLabel extends JLabel {
    private static final long serialVersionUID = -2034374214878411484L;
    private final GAME_STEP gameStep;

    /**
     * Creates a <code>JLabel</code> instance with the specified text.
     * The label is aligned against the leading edge of its display area,
     * and centered vertically.
     *
     * @param text The text to be displayed by the label.
     */
    public GameStepLabel(final String text, final GAME_STEP step) {
      super(text);
      gameStep = step;
      final GameStepLabel me = this;
      addMouseListener(new MouseListener() {

        @Override
        public void mouseReleased(final MouseEvent e) {}

        @Override
        public void mousePressed(final MouseEvent e) {}

        @Override
        public void mouseExited(final MouseEvent e) {
          if (gameStep != currentStep) {
            me.setOpaque(false);
            me.setBackground(me.getBackground().darker().darker());
            me.updateUI();
          }
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
          if (gameStep != currentStep) {
            me.setOpaque(true);
            me.setBackground(me.getBackground().brighter().brighter());
            me.updateUI();
          }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
          me.requestFocus();
          if (gameStep != currentStep
              && (!DynamicRowsPanel.me.isPresent() || DynamicRowsPanel.me.get().dataIsConsistent())) {
            me.setOpaque(false);
            goToStep(gameStep);
          }
        }
      });
    }
  }

}
