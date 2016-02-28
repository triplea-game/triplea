package util.triplea.MapXMLCreator;

import games.strategy.engine.framework.GameRunner2;

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
import java.util.Iterator;
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

/**
 * A frame that will show the different steps creating a game XML.
 * 
 * @author Erik von der Osten
 * 
 */
public class MapXMLCreator extends JFrame
{
	private static final long serialVersionUID = 3593102638082774498L;
	public static final String TRIPLEA_MAP_FOLDER = "triplea.map.folder";
	public static final String TRIPLEA_UNIT_ZOOM = "triplea.unit.zoom";
	public static final String TRIPLEA_UNIT_WIDTH = "triplea.unit.width";
	public static final String TRIPLEA_UNIT_HEIGHT = "triplea.unit.height";
	
	private int m_highest_step = 1;

	static File s_mapImageFile = null;
	static File s_mapCentersFile = null;
	static File s_mapPolygonsFile = null;
	static String s_waterFilterString = "";
	static File s_mapFolderLocation = null;
	
	final JPanel m_mainPanel;
	final JPanel m_sidePanel;
	final JPanel m_stepPanel = new JPanel();
	final JPanel m_panel2 = new JPanel();
	final JPanel m_panel3 = new JPanel();
	final JPanel m_panel4 = new JPanel();
	private JPanel stepListPanel;
	private ArrayList<JLabel> m_stepList = new ArrayList<JLabel>();
	private final JPanel m_southPanel = new JPanel();
	private JPanel southLeftPanel;
	private JPanel southRightPanel;
	private JPanel southCenterPanel;
	private JButton m_bHelp;
	private JButton m_bAvailableChoices;
	JButton m_bBack;
	JButton m_bAuto;
	JButton m_bNext;
	private JPanel m_stepActionPanel;
	private JLabel m_lStepTitle;
	private JPanel m_actionPanel;
	private int m_currentStep;
	
	public static String[] getProperties()
	{
		return new String[] { TRIPLEA_MAP_FOLDER, TRIPLEA_UNIT_ZOOM, TRIPLEA_UNIT_WIDTH, TRIPLEA_UNIT_HEIGHT };
	}
	
	public static void main(final String[] args)
	{
		System.out.println("Starting MapXMLCreator");
//		handleCommandLineArgs(args);
		GameRunner2.setupLookAndFeel();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final MapXMLCreator creator = new MapXMLCreator();
				creator.setSize(800, 600);
				creator.setPreferredSize(creator.getSize());
				creator.goToStep(1);
				creator.setLocationRelativeTo(null);
				creator.setVisible(true);
			}
		});
	}

	public MapXMLCreator()
	{
		super("TripleA Map XML Creator");
		
		s_mapFolderLocation = new File("C:\\Users\\evdO\\triplea\\triplea_1_7_0_3\\maps\\minimap");
		// s_mapFolderLocation = new File("C:\\Users\\User\\workspace\\triplea\\maps\\minimap");
		MapXMLHelper.s_mapXMLFile = new File(s_mapFolderLocation.getAbsolutePath() + "\\..\\new_world_order\\games\\new_world_order.xml");
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// set up the actions
		final Action openAction = new AbstractAction("Load Map XML")
		{
			private static final long serialVersionUID = -3135749471880991185L;
			
			public void actionPerformed(final ActionEvent event)
			{
		final int goToStep = MapXMLHelper.loadXML();
				m_highest_step = 1;
				DynamicRowsPanel.s_me = null;
		if (goToStep > 0)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					goToStep(goToStep);
				}
			});
		}
				setWidgetActivation();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Load an existing Map XML File");
		final Action saveAction = new AbstractAction("Save Map XML")
		{
			private static final long serialVersionUID = -5608941822299486808L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_stepActionPanel.requestFocus();
				if (DynamicRowsPanel.s_me == null || DynamicRowsPanel.s_me.dataIsConsistent())
					MapXMLHelper.saveXML();
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save the Map XML to File");
		final Action exitAction = new AbstractAction("Exit")
		{
			private static final long serialVersionUID = -9212762817640498442L;
			
			public void actionPerformed(final ActionEvent event)
			{
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
		setJMenuBar(menuBar);
		final JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		menuBar.add(fileMenu);
		// components
		m_mainPanel = new JPanel();
		m_sidePanel = new JPanel();
		stepListPanel = new JPanel();
		
		m_sidePanel.setLayout(new BoxLayout(m_sidePanel, BoxLayout.Y_AXIS));
		m_sidePanel.add(Box.createVerticalStrut(2));
		m_sidePanel.add(stepListPanel);
		m_sidePanel.add(Box.createVerticalGlue());
		
		m_stepList.add(new JLabel("1: Map Properties"));		
		m_stepList.add(new JLabel("2: Territory Definitions"));		
		m_stepList.add(new JLabel("3: Territory Connections"));		
		m_stepList.add(new JLabel("4: Players and Alliances"));		
		m_stepList.add(new JLabel("5: Unit Definitions"));		
		m_stepList.add(new JLabel("6: Gameplay Sequence"));		
		m_stepList.add(new JLabel("7: Player Sequence"));		
		m_stepList.add(new JLabel("8: Technology Definitions"));		
		m_stepList.add(new JLabel("9: Production Frontiers"));		
		m_stepList.add(new JLabel("10: Unit Attachments"));		
		m_stepList.add(new JLabel("11: Territory Production"));		
		m_stepList.add(new JLabel("12: Canal Definitions"));		
		m_stepList.add(new JLabel("13: Territory Ownership"));		
		m_stepList.add(new JLabel("14: Unit Placements"));		
		m_stepList.add(new JLabel("15: Game Settings"));		
		m_stepList.add(new JLabel("16: Map finished!"));
		
		stepListPanel.setLayout(new GridLayout(m_stepList.size(), 1, 0, 10));
		stepListPanel.setBorder(new EmptyBorder(10, 2, 0, 5));
		stepListPanel.setPreferredSize(new Dimension(170, 410));

		int current_label = 1;
		for (final Iterator<JLabel> iter = m_stepList.iterator(); iter.hasNext();)
		{
			final JLabel lStep = iter.next();
			final int label_order = current_label;
			lStep.addMouseListener(new MouseListener()
			{
				
				@Override
				public void mouseReleased(MouseEvent e)
				{
				}
				
				@Override
				public void mousePressed(MouseEvent e)
				{
				}
				
				@Override
				public void mouseExited(MouseEvent e)
				{
					if (label_order <= m_highest_step && label_order != m_currentStep)
					{
						lStep.setOpaque(false);
						lStep.setBackground(lStep.getBackground().darker().darker());
						lStep.updateUI();
					}
				}
				
				@Override
				public void mouseEntered(MouseEvent e)
				{
					if (label_order <= m_highest_step && label_order != m_currentStep)
					{
						lStep.setOpaque(true);
						lStep.setBackground(lStep.getBackground().brighter().brighter());
						lStep.updateUI();
					}
				}
				
				@Override
				public void mouseClicked(MouseEvent e)
				{
					lStep.requestFocus();
					if (label_order <= m_highest_step && label_order != m_currentStep && (DynamicRowsPanel.s_me == null || DynamicRowsPanel.s_me.dataIsConsistent()))
					{
						lStep.setOpaque(false);
						goToStep(label_order);
					}
				}
			});
			stepListPanel.add(lStep);
			++current_label;
		}
		
		// set up the layout manager
		this.getContentPane().setLayout(new BorderLayout());
		final JScrollPane scrollPane = new JScrollPane(m_sidePanel);
		scrollPane.setBorder(new MatteBorder(1, 1, 1, 1, Color.DARK_GRAY));
		this.getContentPane().add(scrollPane, BorderLayout.WEST);
		this.getContentPane().add(m_mainPanel, BorderLayout.CENTER);
		
		// now set up the main screen
		setupMainPanel(m_stepPanel);
		m_stepPanel.setLayout(new BoxLayout(m_stepPanel, BoxLayout.PAGE_AXIS));
		
		m_stepPanel.add(Box.createVerticalStrut(20));
		
		m_lStepTitle = new JLabel("Map Properties");
		m_lStepTitle.setFont(new Font("Tahoma", Font.BOLD, 12));
		m_lStepTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_stepPanel.add(m_lStepTitle);
		
		m_stepPanel.add(Box.createVerticalStrut(20));
		
		m_actionPanel = new JPanel();
		m_actionPanel.setLayout(new BorderLayout());
		m_actionPanel.setBorder(new MatteBorder(1, 0, 0, 0, Color.DARK_GRAY));
		m_stepActionPanel = new JPanel();
		m_stepPanel.add(m_actionPanel);

		m_actionPanel.add(m_stepActionPanel,BorderLayout.CENTER);
		
		layoutSouthPanel();

		openAction.actionPerformed(null);
	}

	private void layoutSouthPanel() {
		m_mainPanel.add(m_southPanel, BorderLayout.SOUTH);
		m_southPanel.setLayout(new BoxLayout(m_southPanel, BoxLayout.X_AXIS));
		
		southLeftPanel = new JPanel();
		southLeftPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
		FlowLayout fl_southLeftPanel = (FlowLayout) southLeftPanel.getLayout();
		fl_southLeftPanel.setAlignment(FlowLayout.LEFT);
		southLeftPanel.setBorder(null);
		m_southPanel.add(southLeftPanel);
		
		m_southPanel.add(Box.createHorizontalGlue());
		
		m_bHelp = new JButton("Help");
		m_bHelp.setMnemonic('H');
		m_bHelp.setFont(new Font("Tahoma", Font.PLAIN, 11));
		m_bHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (m_currentStep) {
				case 1:	 showInfoMessage("Map Name: The map name that is displayed in the New Game window in TripleA. Examples: Revised, Classic, Big World, and Great War\r\n\r\nMap Version: The version of the map's xml file. Examples: 0.0.1, 1.0.0.1, and 1.1\r\n\r\nResource Name: The name of the resource used in the map. Resources can be thought of as buying tokens or currency used to buy units, technology, etc. 'IPCs' have recently been replaced by 'PUs'. 'IPCs' will no longer work as the map resource. \r\n\r\nMap Image Location: The location of the map image. Example: C:/My Maps/Sleeping Giant/full_map.png\r\n\r\nMap Centers File: The location of the centers file produced by the 'Center Picker' program. The centers file is used to automatically add the map's territories. Example: C:/My Maps/Sleeping Giant/centers.txt\r\n\r\nWater Territory Filter: An optional setting that makes the program automatically apply the 'Is Water' property to every territory that contains the filter text. Examples: SZ, Sea Zone, Pacific, and Atlantic.", "Help For Current Step");
		        break;
		        case 2: //showInfoMessage("To add a new territory, click somewhere on the map and enter a name for the territory in the window that appears. \r\n If you want to change the properties of a territory, left click on it and answer each question. The color of the territory label changes for each property that is applies. If you want to remove a territory label, right click on it and click yes when it asks for confirmation.", "Help For Current Step");
		        	showInfoMessage("Right click to rename a territory. \r\n If you want to change the properties of a territory, left click on it. \r\n The color of the territory label changes for each property that is applied.", "Help For Current Step");
		        break;
		        case 3: showInfoMessage("To add a connection between two territories, click on the first territory in the connection and then the second. To remove all the connections from a certain territory, right click on it and click yes.\r\n\r\nNote: To have the program find the connections automatically, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 4: showInfoMessage("Player Name: The name of the player. Examples: Russians, Germans, British, Americans, Chinese, and Italians.\r\n\r\nPlayer Alliance: The name of the alliance that the player is part of. Examples: Allies, and Axis.\r\n\r\nInitial Resources: The amount of resources(PUs) the player begins with when the map is started.", "Help For Current Step");
		        break;
		        case 5: showInfoMessage("Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter, bomber, and transport.\r\n\r\nBuy Cost: The amount of resources it takes to buy the unit.\r\n\r\nBuy Quantity: The amount of units to be placed with each purchase of the unit.\r\n\r\nTo have the program automatically enter some of the commonly used units, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 6: showInfoMessage("Sequence Name: The name of the sequence. Examples(Typical): tech, techActivation, battle, move, place, purchase, endTurn, placeBid, bid.\r\n\r\nClass Name: The name of the java delegate. Examples(Typical): TechnologyDelegate, TechActivationDelegate, BattleDelegate, MoveDelegate, PlaceDelegate, PurchaseDelegate, EndTurnDelegate, BidPlaceDelegate, BidPurchaseDelegate.\r\n\r\nDisplay: The text displayed for the delegate in TripleA. Examples(Typical): Research Technology, Activate Technology, Combat, Combat Move, Place Units, Purchase Units, Turn Complete, Bid Placement, and Bid Purchase\r\n\r\nTo have the program automatically enter the default Gameplay Sequences, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 7: showInfoMessage("Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace, chineseTech, americanCombatMove, and germanPlace.\r\n\r\nGameplay Sequence: The name of the Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place, endTurn.\r\n\r\nPlayer: The name of the player that the Player Delegate is attached to. Examples: Russians, Germans, Americans, and Chinese.\r\n\r\nMax Run Count: The maximum number of times the Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to have a run limit).\r\n\r\nTo have the program automatically enter the default Player Sequences, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 8: showInfoMessage("Technology Name: The name of the technology that can be researched and unlocked by the player. Examples(Typical): heavyBomber, jetPower, industrialTechnology, superSub, rocket, and longRangeAir.\r\n\r\nPlayer: The name of the player that is able to research the technology. Examples: Russians, Germans, Americans, and Chinese.\r\n\r\nAlready Enabled: Determines if the technology should be unlocked for the selected player when the game first starts.\r\n\r\nTo have the program automatically enter some of the commonly used technologies, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 9: showInfoMessage("Unit Name: The name of the unit that is included in the production frontier. Examples: infantry, artillery, and armour.\r\n\r\nTo have the program automatically add all the units to the production frontier being shown, click the 'Auto-Fill' button between the Back and Next buttons.\r\n\r\nNote: A production frontier is just a list of units that a player is allowed to purchase.", "Help For Current Step");
		        break;
		        case 10: showInfoMessage("Attachment Name: The name of the unit attachment that is applied to the unit. Examples: movement, attack, defense, isAir, isSea, and isStrategicBomber.\r\n\r\nValue: The attachment value. Examples: True, False, 1, 2\r\n\r\nTo have the program automatically enter the default attachments for the units that are commonly used, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
		        break;
		        case 11: showInfoMessage("To change the production value of a territory, click on the territory and enter the territory's new production value in the window that appears.", "Help For Current Step");
		        break;
		        case 12: showInfoMessage("To add a new canal, click on both of the land territories that form the canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from the map, right click on one of the territories and click 'Yes' to confirm.", "Help For Current Step");
		        break;
		        case 13: showInfoMessage("To change the initial owner of a territory, click on the territory and type in its new owner when it asks for it.", "Help For Current Step");
		        break;
		        case 14: showInfoMessage("To change the units that a territory begins with, click on the territory and use the window that opens to set the territory's units. To do so, use the + and - buttons below each unit's name to change the quantity of each unit in the territory. You can also edit the text in the result textbox directly to change the territory units. Just type the name of each unit you want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3, armour: 2, fighter: 1'.", "Help For Current Step");
		        break;
					case 15:
						final String[] settings = GameSettingsPanel.s_allSettings.split(", ");
						StringBuilder sb = new StringBuilder();
						sb.append("<html><table border=0><tr>");
						int setting_nr = 1;
						for (final String setting : settings)
						{
							sb.append("<td>" + setting + "</td>");
							if (setting_nr % 3 == 0)
								sb.append("</tr><tr>");
							++setting_nr;
						}
						sb.append("</tr></table></html>");
						showInfoMessage(
									"Setting Name: The name of the setting that is applied to the map. Examples: Always on AA, Two hit battleship, and Japanese bid.\r\n\r\nValue: The value of the game setting. Examples: true, false, 0, 5, 32.\r\n\r\nEditable: Whether players are allowed to change the value of the setting when the map is being started.\r\n\r\nMin. N. (Optional): The lowest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\n\r\nMax. N.(Optional): The highest number that the value can be set to when the user sets the game options in TripleA. Only change this if the setting is a number.\r\n\r\nTo have the program automatically enter some of the commonly used Game Settings, click the 'Auto-Fill' button between the Back and Next buttons.",
									"Help For Current Step");
		        break;
		        case 16: showInfoMessage("The map notes can be entered in the textbox. You can enter plain text or html code. Then just click the 'Save Map To File' button, and save the file in the map's 'games' folder. (Create the 'games' folder if not already created)", "Help For Current Step");
		        break;
				}
			}
		});
		southLeftPanel.add(m_bHelp);
		
		southCenterPanel = new JPanel();
		m_southPanel.add(southCenterPanel);
		
		m_southPanel.add(Box.createHorizontalGlue());
		
		m_bBack = new JButton("Back");
		m_bBack.setMnemonic(KeyEvent.VK_B);
		m_bBack.setFont(new Font("Tahoma", Font.PLAIN, 11));
		m_bBack.addActionListener(new AbstractAction("Previous Step")
		{		
			private static final long serialVersionUID = -8623081809358073307L;

			public void actionPerformed(final ActionEvent e)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if (m_currentStep > 1)
							goToStep(m_currentStep-1);
						m_actionPanel.validate();
						m_actionPanel.repaint();
					}
				});
			}
		});
		southCenterPanel.add(m_bBack);
		
		m_bAuto = new JButton("Auto-Fill");
		m_bAuto.setMnemonic('A');
		m_bAuto.setFont(new Font("Tahoma", Font.PLAIN, 11));
		m_bAuto.setMargin(new Insets(2, 5, 2, 5));
		m_bAuto.setMinimumSize(new Dimension(50, 23));
		m_bAuto.setMaximumSize(new Dimension(50, 23));
		southCenterPanel.add(m_bAuto);
		
		m_bNext = new JButton("Next");
		m_bNext.setMnemonic(KeyEvent.VK_N);
		m_bNext.setFont(new Font("Tahoma", Font.PLAIN, 11));
		m_bNext.addActionListener(new AbstractAction("Next Step")
		{
			private static final long serialVersionUID = 6322566373692205163L;

			public void actionPerformed(final ActionEvent e)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if (m_currentStep < m_stepList.size())
						{
							goToStep(m_currentStep+1);
							m_actionPanel.validate();
							m_actionPanel.repaint();
						}
					}
				});
			}
		});
		southCenterPanel.add(m_bNext);
		
		southRightPanel = new JPanel();
		FlowLayout fl_southRightPanel = (FlowLayout) southRightPanel.getLayout();
		fl_southRightPanel.setAlignment(FlowLayout.RIGHT);
		m_southPanel.add(southRightPanel);
		
		m_bAvailableChoices = new JButton("Available Choices");
		m_bAvailableChoices.setMnemonic('C');
		m_bAvailableChoices.setMargin(new Insets(2, 5, 2, 5));
		m_bAvailableChoices.setFont(new Font("Tahoma", Font.PLAIN, 11));
		m_bAvailableChoices.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				switch (m_currentStep)
				{
					case 1: // showInfoMessage("Map Name: The map name that is displayed in the New Game window in TripleA. Examples: Revised, Classic, Big World, and Great War\r\n\r\nMap Version: The version of the map's xml file. Examples: 0.0.1, 1.0.0.1, and 1.1\r\n\r\nResource Name: The name of the resource used in the map. Resources can be thought of as buying tokens or currency used to buy units, technology, etc. 'IPCs' have recently been replaced by 'PUs'. 'IPCs' will no longer work as the map resource. \r\n\r\nMap Image Location: The location of the map image. Example: C:/My Maps/Sleeping Giant/full_map.png\r\n\r\nMap Centers File: The location of the centers file produced by the 'Center Picker' program. The centers file is used to automatically add the map's territories. Example: C:/My Maps/Sleeping Giant/centers.txt\r\n\r\nWater Territory Filter: An optional setting that makes the program automatically apply the 'Is Water' property to every territory that contains the filter text. Examples: SZ, Sea Zone, Pacific, and Atlantic.", "Help For Current Step");
						break;
					case 2: // showInfoMessage("To add a new territory, click somewhere on the map and enter a name for the territory in the window that appears. \r\n If you want to change the properties of a territory, left click on it and answer each question. The color of the territory label changes for each property that is applies. If you want to remove a territory label, right click on it and click yes when it asks for confirmation.", "Help For Current Step");
						// showInfoMessage("Right click to rename a territory. \r\n If you want to change the properties of a territory, left click on it. \r\n The color of the territory label changes for each property that is applied.", "Help For Current Step");
						break;
					case 3: // showInfoMessage("To add a connection between two territories, click on the first territory in the connection and then the second. To remove all the connections from a certain territory, right click on it and click yes.\r\n\r\nNote: To have the program find the connections automatically, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
						break;
					case 4: // showInfoMessage("Player Name: The name of the player. Examples: Russians, Germans, British, Americans, Chinese, and Italians.\r\n\r\nPlayer Alliance: The name of the alliance that the player is part of. Examples: Allies, and Axis.\r\n\r\nInitial Resources: The amount of resources(PUs) the player begins with when the map is started.", "Help For Current Step");
						break;
					case 5: // showInfoMessage("Unit Name: The name of the unit. Examples: infantry, artillery, armour, fighter, bomber, and transport.\r\n\r\nBuy Cost: The amount of resources it takes to buy the unit.\r\n\r\nBuy Quantity: The amount of units to be placed with each purchase of the unit.\r\n\r\nTo have the program automatically enter some of the commonly used units, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
						break;
					case 6:
						showInfoMessage(
									"Here is a list of all the available gameplay sequences with their class name and display name:\r\n\r\n initDelegate-InitializationDelegate-Initializing Delegates,\r\n bid-BidPurchaseDelegate-Bid Purchase,\r\n placeBid-BidPlaceDelegate-Bid Placement,\r\n tech-TechnologyDelegate-Research Technology,\r\n tech_Activation-TechActivationDelegate-Activate Technology,\r\n purchase-PurchaseDelegate-Purchase Units,\r\n move-MoveDelegate-Combat Move,\r\n battle-BattleDelegate-Combat,\r\n place-PlaceDelegate-Place Units,\r\n endTurn-EndTurnDelegate-Turn Complete,\r\n endRound-EndRoundDelegate-Round Complete",
									"List Of Available Gameplay Sequence Choices (As Of TripleA 1.0.3.4)");
						break;
					case 7: // showInfoMessage("Sequence Name: The name of the sequence. Examples: russianBid, germanBidPlace, chineseTech, americanCombatMove, and germanPlace.\r\n\r\nGameplay Sequence: The name of the Gameplay Sequence that the player sequence uses. Examples: bid, tech, move, place, endTurn.\r\n\r\nPlayer: The name of the player that the Player Delegate is attached to. Examples: Russians, Germans, Americans, and Chinese.\r\n\r\nMax Run Count: The maximum number of times the Sequence can be used in the game. (You can set this value to 0 if you do not want the sequence to have a run limit).\r\n\r\nTo have the program automatically enter the default Player Sequences, click the 'Auto-Fill' button between the Back and Next buttons.", "Help For Current Step");
						break;
					case 8:
						showInfoMessage(
									"Here is a list of all the available technology types: heavyBomber, longRangeAir, jetPower, rocket, industrialTechnology, superSub, destroyerBombard, improvedArtillerySupport, paratroopers, increasedFactoryProduction, warBonds, mechanizedInfantry, aARadar, shipyards",
									"List Of Available Technology Choices (As Of TripleA 1.0.3.4)");
						break;
					case 9: // showInfoMessage("Unit Name: The name of the unit that is included in the production frontier. Examples: infantry, artillery, and armour.\r\n\r\nTo have the program automatically add all the units to the production frontier being shown, click the 'Auto-Fill' button between the Back and Next buttons.\r\n\r\nNote: A production frontier is just a list of units that a player is allowed to purchase.", "Help For Current Step");
						break;
					case 10:
						showInfoMessage(
									"Here is a list of all the available unit attachments: movement, attack, defense, isAir, isSea, isAA, isFactory, canBlitz, isSub, canBombard, isStrategicBomber, isTwoHit, isDestroyer, isArtillery, isArtillerySupportable, isMarine, isInfantry, isParatroop, isMechanized, transportCapacity, transportCost, carrierCapacity, carrierCost",
									"List Of Available Unit Attachment Choices (As Of TripleA 1.0.3.4)");
						break;
					case 11: // showInfoMessage("To change the production value of a territory, click on the territory and enter the territory's new production value in the window that appears.", "Help For Current Step");
						break;
					case 12: // showInfoMessage("To add a new canal, click on both of the land territories that form the canal(Touch the canal) and click yes when it asks for confirmation. To remove all the canals from the map, right click on one of the territories and click 'Yes' to confirm.", "Help For Current Step");
						break;
					case 13: // showInfoMessage("To change the initial owner of a territory, click on the territory and type in its new owner when it asks for it.", "Help For Current Step");
						break;
					case 14: // showInfoMessage("To change the units that a territory begins with, click on the territory and use the window that opens to set the territory's units. To do so, use the + and - buttons below each unit's name to change the quantity of each unit in the territory. You can also edit the text in the result textbox directly to change the territory units. Just type the name of each unit you want to add, followed by ':', followed by the unit quantitiy: Example: 'infantry: 1, artillery: 3, armour: 2, fighter: 1'.", "Help For Current Step");
						break;
					case 15:
						final String[] settings = GameSettingsPanel.s_allSettings.split(", ");
						Arrays.sort(settings);
						StringBuilder sb = new StringBuilder();
						sb.append("<html>Here is a list of all the available game settings:<table border=0><tr><td><table border=0>");
						int setting_nr = 1;
						char last_first_letter = 'z';
						final int third_size_setting = (settings.length + 2) / 3;
						for (final String setting : settings)
						{
							char first_letter = setting.charAt(0);
							if (first_letter == last_first_letter)
								sb.append("<tr><td>" + setting + "</td></tr>");
							else
								sb.append("<tr><td><big>" + first_letter + "</big>" + setting.substring(1) + "</td></tr>");
							last_first_letter = first_letter;
							if (setting_nr % third_size_setting == 0)
								sb.append("</table></td><td><table border=0>");
							++setting_nr;
						}
						sb.append("</table></td></tr></table></html>");
						showInfoMessage(
sb.toString(), "List Of Available Game Settings Choices");
						break;
					case 16:
						showInfoMessage(
									"The map notes can be entered in the textbox. You can enter plain text or html code. Then just click the 'Save Map To File' button, and save the file in the map's 'games' folder. (Create the 'games' folder if not already created)",
									"Help For Current Step");
						break;
				}
			}
		});
		southRightPanel.add(m_bAvailableChoices);
	}
	
	private void showInfoMessage(final String message, final String title) {
		JOptionPane.showMessageDialog(null,message,title,JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void setupMainPanel(final JPanel panel)
	{
		m_mainPanel.removeAll();
		m_mainPanel.setLayout(new BorderLayout());
		panel.setBorder(new MatteBorder(1, 0, 1, 1, Color.DARK_GRAY));
		m_mainPanel.add(panel,BorderLayout.CENTER);
		setWidgetActivation();
	}
	
	public void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		m_mainPanel.revalidate();
		m_mainPanel.repaint();
		this.validate();
		this.repaint();
	}
	
	private void goToStep(final int step) {
		JLabel stepLabel;
		if (m_currentStep >= 1) {
			stepLabel = m_stepList.get(m_currentStep-1);
			stepLabel.setFont(new Font("Tahoma", Font.PLAIN, 13));
			stepLabel.repaint();
		}
		
		if (step <= m_stepList.size())
		{
			m_currentStep = step;
			m_highest_step = Math.max(m_highest_step, step);
			layoutStepActionPanel();
			
			stepLabel = m_stepList.get(step-1);
			stepLabel.setFont(new Font("Tahoma", Font.BOLD, 13));
			stepLabel.repaint();
			
			String title = stepLabel.getText();
			final int titleSepPos = title.indexOf(": ");
			title = title.substring(titleSepPos+2, title.length());
			m_lStepTitle.setText(title);
			
			if (m_currentStep ==  m_stepList.size())
				m_bNext.setEnabled(false);
			else 
				m_bNext.setEnabled(true);
			
			if (step > 1) 
				m_bBack.setEnabled(true);
			else 
				m_bBack.setEnabled(false);
		}
	}

	public void setAutoFillAction(final AbstractAction action)
	{
		for (final ActionListener actionListener : m_bAuto.getActionListeners())
			m_bAuto.removeActionListener(actionListener);
		m_bAuto.addActionListener(action);
		m_bAuto.setEnabled(true);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				
				m_bAuto.repaint();
			}
		});
	}

	private void layoutStepActionPanel() {
 		m_stepActionPanel.removeAll();
		m_bAuto.setEnabled(false);
		switch (m_currentStep) {
			case 1: MapPropertiesPanel.layout(this, m_stepActionPanel);
				break;
			case 2:
				TerritoryDefinitionsPanel.layout(this, m_stepActionPanel);
			break;
			case 3:
				TerritoryConnectionsPanel.layout(this, m_stepActionPanel);
				break;
			case 4:
				PlayerAndAlliancesPanel.layout(this, m_stepActionPanel);
				break;
			case 5:
				UnitDefinitionsPanel.layout(this, m_stepActionPanel);
				break;

			case 6:
				GameSequencePanel.layout(this, m_stepActionPanel);
				break;

			case 7:
				PlayerSequencePanel.layout(this, m_stepActionPanel);
				break;

			case 8:
				TechnologyDefinitionsPanel.layout(this, m_stepActionPanel);
				break;
				
			case 9:
				layoutTabbedPaneWith(MapXMLHelper.s_productionFrontiers.keySet());
				break;

			case 10:
				layoutTabbedPaneWith(MapXMLHelper.s_unitDefinitions.keySet());
				break;

			case 11:
				TerritoryProductionPanel.layout(this, m_stepActionPanel);
				break;
			
			case 12:
				CanalDefinitionsPanel.layout(this, m_stepActionPanel);
				break;
			
			case 13:
				TerritoryOwnershipPanel.layout(this, m_stepActionPanel);
				break;
			
			case 14:
				UnitPlacementsPanel.layout(this, m_stepActionPanel);
				break;
			
			case 15:
				GameSettingsPanel.layout(this, m_stepActionPanel);
				break;
			
			case 16:
				layoutNotesPanel();

		default:
			break;
		}
		final JFrame me = this;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				me.pack();
				m_stepActionPanel.revalidate();
				m_stepActionPanel.repaint();
			}
		});
	}
	
	protected void layoutNotesPanel()
	{
		m_stepActionPanel.setLayout(new GridBagLayout());
		
		final JTextArea taNotes = new JTextArea(MapXMLHelper.s_notes);
		taNotes.addFocusListener(new FocusListener()
		{
			
			@Override
			public void focusLost(FocusEvent arg0)
			{
				if (!MapXMLHelper.s_notes.equals(taNotes.getText()))
				{
					MapXMLHelper.s_notes = taNotes.getText().trim();
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0)
			{
			}
		});
		final JScrollPane spTaNotes = new JScrollPane(taNotes);
		
		Dimension size = m_stepActionPanel.getSize();
		size.height -= 100;
		spTaNotes.setMinimumSize(size);
		
		GridBagConstraints gbc_lFirstRow = new GridBagConstraints();
		gbc_lFirstRow.insets = new Insets(0, 0, 5, 0);
		gbc_lFirstRow.gridy = 0;
		gbc_lFirstRow.gridx = 0;
		gbc_lFirstRow.anchor = GridBagConstraints.NORTH;
		gbc_lFirstRow.weightx = 1.0;
		gbc_lFirstRow.weighty = 1.0;
		gbc_lFirstRow.gridwidth = 3;
		m_stepActionPanel.add(spTaNotes, gbc_lFirstRow);

		GridBagConstraints gbc_lSecondRow = (GridBagConstraints) gbc_lFirstRow.clone();
		gbc_lSecondRow.weighty = 0.0;
		gbc_lSecondRow.insets = new Insets(0, 0, 0, 0);
		gbc_lSecondRow.gridwidth = 1;
		gbc_lSecondRow.gridx = 1;
		gbc_lSecondRow.gridy = 1;
		
		final JButton bPreviewHTML = new JButton("Preview HTML");
		bPreviewHTML.setPreferredSize(new Dimension(300, 30));
		bPreviewHTML.setAction(new AbstractAction("Preview HTML")
		{
			private static final long serialVersionUID = -5608941822299486808L;
			
			public void actionPerformed(final ActionEvent event)
			{
				showHTML(MapXMLHelper.s_notes, "HTML Preview");
			}
		});
		m_stepActionPanel.add(bPreviewHTML, gbc_lSecondRow);

		GridBagConstraints gbc_lThirdRow = (GridBagConstraints) gbc_lSecondRow.clone();
		gbc_lThirdRow.gridy = 2;
		m_stepActionPanel.add(new JLabel("<html><big>Congratulation!</big></html>"), gbc_lThirdRow);
		
		GridBagConstraints gbc_lFourthRow = (GridBagConstraints) gbc_lSecondRow.clone();
		gbc_lFourthRow.gridy = 3;
		m_stepActionPanel.add(new JLabel("<html><nobr>You have completed all the steps for creating the map XML.</nobr></html>"), gbc_lFourthRow);
		
		final JButton bSave = new JButton("Save Entries to XML");
		bSave.setPreferredSize(new Dimension(600, 35));
		bSave.setAction(new AbstractAction("Save Map XML")
		{
			private static final long serialVersionUID = -5608941822299486808L;
			
			public void actionPerformed(final ActionEvent event)
			{
				MapXMLHelper.saveXML();
			}
		});
		GridBagConstraints gbc_lFifthRow = (GridBagConstraints) gbc_lSecondRow.clone();
		gbc_lFifthRow.insets = new Insets(5, 0, 0, 0);
		gbc_lFifthRow.gridy = 4;
		m_stepActionPanel.add(bSave, gbc_lFifthRow);
	}
	
	protected void layoutTabbedPaneWith(final Set<String> keySet)
	{
		m_stepActionPanel.setLayout(new BorderLayout());
		final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.SCROLL_TAB_LAYOUT);
		for (final String key : keySet)
		{
			final JPanel innerTabPane = new JPanel();
			tabbedPane.addTab(key, innerTabPane);
			ProductionFrontiersPanel.layout(this, innerTabPane, key);
		}
		m_stepActionPanel.add(tabbedPane, BorderLayout.CENTER);
	}
	
	
	/**
	 * @
	 * 
	 * @see HtmlEditorKitTest {@link}https://github.com/KaBoKu/PI/blob/master/WPProjekt/src/kus/swing/HtmlEditorKitTest.java
	 */
	public void showHTML(final String htmlString, final String title)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// create jeditorpane
				JEditorPane jEditorPane = new JEditorPane();
				
				// make it read-only
				jEditorPane.setEditable(false);
				
				// create a scrollpane; modify its attributes as desired
				JScrollPane scrollPane = new JScrollPane(jEditorPane);
				
				// add an html editor kit
				HTMLEditorKit kit = new HTMLEditorKit();
				jEditorPane.setEditorKit(kit);
				
				// create a document, set it on the jeditorpane, then add the html
				Document doc = kit.createDefaultDocument();
				jEditorPane.setDocument(doc);
				jEditorPane.setText(htmlString);
				
				// now add it all to a frame
				JFrame j = new JFrame(title);
				j.getContentPane().add(scrollPane, BorderLayout.CENTER);
				
				// make it easy to close the application
				j.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				
				// display the frame
				j.setSize(new Dimension(800, 600));
				
				// pack it, if you prefer
				// j.pack();
				
				// center the jframe, then make it visible
				j.setLocationRelativeTo(null);
				j.setVisible(true);
			}
		});
	}
}