/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.export.GameDataExporter;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.engine.stats.IStat;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.IntTextField;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.Triple;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * Main menu for the triplea frame.
 * 
 * @author sgb
 * 
 */
public class TripleaMenu extends BasicGameMenuBar<TripleAFrame>
{
	private static final long serialVersionUID = 987243661147503593L;
	private JCheckBoxMenuItem showMapDetails;
	private JCheckBoxMenuItem showMapBlends;
	
	TripleaMenu(final TripleAFrame frame)
	{
		super(frame);
		setWidgetActivation();
	}
	
	private UIContext getUIContext()
	{
		return m_frame.getUIContext();
	}
	
	@Override
	protected void addGameSpecificHelpMenus(final JMenu helpMenu)
	{
		addMoveHelpMenu(helpMenu);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addMoveHelpMenu(final JMenu parentMenu)
	{
		parentMenu.add(new AbstractAction("Movement/Selection help...")
		{
			private static final long serialVersionUID = 6388976622644695135L;
			
			public void actionPerformed(final ActionEvent e)
			{
				// html formatted string
				final String hints = "<b> Selecting Units</b><br>"
							+ "Left click on a unit stack to select 1 unit.<br>"
							+ "ALT-Left click on a unit stack to select 10 units of that type in the stack.<br>"
							+ "CTRL-Left click on a unit stack to select all units of that type in the stack.<br>"
							+ "Shift-Left click on a unit to select all units in the territory.<br>"
							+ "Left click on a territory but not on a unit to bring up a selection window for inputing the desired selection.<br>"
							+ "<br><b> Deselecting Units</b><br>"
							+ "Right click somewhere not on a unit stack to unselect the last selected unit.<br>"
							+ "Right click on a unit stack to unselect one unit in the stack.<br>"
							+ "ALT-Right click on a unit stack to unselect 10 units of that type in the stack.<br>"
							+ "CTRL-Right click on a unit stack to unselect all units of that type in the stack.<br>"
							+ "CTRL-Right click somewhere not on a unit stack to unselect all units selected.<br>"
							+ "<br><b> Moving Units to a new Territories</b><br>"
							+ "After selecting units Left click on a territory to move units there (do not Left click and Drag, instead select units, then move the mouse, then select the territory).<br>"
							+ "CTRL-Left click on a territory to select the territory as a way point (this will force the units to move through this territory on their way to the destination).<br>"
							+ "<br><b> Moving the Map Screen</b><br>" + "Right click and Drag the mouse to move your screen over the map.<br>"
							+ "Left click on the map (anywhere), then use the Arrow Keys to move your map around.<br>"
							+ "Left click in the Minimap at the top right of the screen, and Drag the mouse.<br>"
							+ "Move the mouse to the edge of the map window, and the screen will scroll in that direction.<br>" + "Scrolling the mouse wheel will move the map up and down.<br>"
							+ "<br><b> Zooming Out</b><br>" + "Holding ALT while Scrolling the Mouse Wheel will zoom the map in and out.<br>"
							+ "Select 'Zoom' from the 'View' menu, and change to the desired level.<br>"
							+ "<br><b> Turn off Art</b><br>" + "Deselect 'Map Details' in the 'View' menu, to show a map without the artwork.<br>"
							+ "Select a new 'Map Skin' from the 'View' menu to show a different kind of artwork (not all maps have skins).<br>";
				final JEditorPane editorPane = new JEditorPane();
				editorPane.setEditable(false);
				editorPane.setContentType("text/html");
				editorPane.setText(hints);
				final JScrollPane scroll = new JScrollPane(editorPane);
				JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
	
	@Override
	protected void createGameSpecificMenus(final JMenuBar menuBar)
	{
		createViewMenu(menuBar);
		createGameMenu(menuBar);
		createExportMenu(menuBar);
	}
	
	/**
	 * @param menuBar
	 */
	private void createGameMenu(final JMenuBar menuBar)
	{
		final JMenu menuGame = new JMenu("Game");
		menuBar.add(menuGame);
		addEditMode(menuGame);
		menuGame.add(m_frame.getShowGameAction());
		menuGame.add(m_frame.getShowHistoryAction());
		menuGame.add(m_frame.getShowMapOnlyAction());
		addShowVerifiedDice(menuGame);
		SoundOptions.addGlobalSoundSwitchMenu(menuGame);
		SoundOptions.addToMenu(menuGame);
		menuGame.addSeparator();
		addGameOptionsMenu(menuGame);
		addPoliticsMenu(menuGame);
		addShowEnemyCasualties(menuGame);
		addShowAIBattles(menuGame);
		addChangeDynamixAISettings(menuGame);
		addAISleepDuration(menuGame);
		addShowDiceStats(menuGame);
		addRollDice(menuGame);
		addBattleCalculatorMenu(menuGame);
	}
	
	private void createExportMenu(final JMenuBar menuBar)
	{
		final JMenu menuGame = new JMenu("Export");
		menuBar.add(menuGame);
		addExportXML(menuGame);
		addExportStats(menuGame);
		addExportStatsFull(menuGame);
		addExportSetupCharts(menuGame);
		addSaveScreenshot(menuGame);
	}
	
	/**
	 * @param menuBar
	 */
	private void createViewMenu(final JMenuBar menuBar)
	{
		final JMenu menuView = new JMenu("View");
		menuBar.add(menuView);
		addZoomMenu(menuView);
		addUnitSizeMenu(menuView);
		addLockMap(menuView);
		addShowUnits(menuView);
		if (getUIContext().getMapData().useTerritoryEffectMarkers())
		{
			addShowTerritoryEffects(menuView);
		}
		addMapSkinsMenu(menuView);
		addShowMapDetails(menuView);
		addShowMapBlends(menuView);
		addDrawTerritoryBordersAgain(menuView);
		addChatTimeMenu(menuView);
		addShowCommentLog(menuView);
		// The menuItem to turn TabbedProduction on or off
		addTabbedProduction(menuView);
		addShowGameUuid(menuView);
		addSetLookAndFeel(menuView);
	}
	
	private static boolean isJavaGreatThan5()
	{
		final String version = System.getProperties().getProperty("java.version");
		return version.indexOf("1.5") == -1;
	}
	
	private static boolean isJavaGreatThan6()
	{
		final String version = System.getProperties().getProperty("java.version");
		return version.indexOf("1.5") == -1 && version.indexOf("1.6") == -1;
	}
	
	public static List<String> getLookAndFeelAvailableList()
	{
		final List<String> substanceLooks = new ArrayList<String>(Arrays.asList(new String[] {
					UIManager.getSystemLookAndFeelClassName(),
					MetalLookAndFeel.class.getName(),
					UIManager.getCrossPlatformLookAndFeelClassName()
		}));
		if (isJavaGreatThan5())
		{
			substanceLooks.addAll(new ArrayList<String>(Arrays.asList(new String[] {
						UIManager.getSystemLookAndFeelClassName(),
						MetalLookAndFeel.class.getName(),
						UIManager.getCrossPlatformLookAndFeelClassName(),
						// Substance 5.x
						"org.jvnet.substance.skin.SubstanceAutumnLookAndFeel",
						"org.jvnet.substance.skin.SubstanceChallengerDeepLookAndFeel",
						"org.jvnet.substance.skin.SubstanceCremeCoffeeLookAndFeel",
						"org.jvnet.substance.skin.SubstanceCremeLookAndFeel",
						"org.jvnet.substance.skin.SubstanceDustCoffeeLookAndFeel",
						"org.jvnet.substance.skin.SubstanceDustLookAndFeel",
						"org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel",
						"org.jvnet.substance.skin.SubstanceMagmaLookAndFeel",
						"org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel",
						"org.jvnet.substance.skin.SubstanceModerateLookAndFeel",
						"org.jvnet.substance.skin.SubstanceNebulaLookAndFeel",
						"org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel",
						"org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel",
						"org.jvnet.substance.skin.SubstanceRavenLookAndFeel",
						"org.jvnet.substance.skin.SubstanceTwilightLookAndFeel"

			/* Substance (insubstantial) 7.x
			"org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceCeruleanLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceMarinerLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceOfficeBlack2007LookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel",
			"org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel"*/
			})));
		}
		if (isJavaGreatThan6())
			substanceLooks.add("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		return substanceLooks;
	}
	
	/**
	 * First is our JList, second is our LookAndFeels string -> class map, third is our 'current' look and feel.
	 * 
	 * @return
	 */
	public static Triple<JList, Map<String, String>, String> getLookAndFeelList()
	{
		final Map<String, String> lookAndFeels = new LinkedHashMap<String, String>();
		try
		{
			final List<String> substanceLooks = getLookAndFeelAvailableList();
			for (final String s : substanceLooks)
			{
				@SuppressWarnings("rawtypes")
				final Class c = Class.forName(s);
				final LookAndFeel lf = (LookAndFeel) c.newInstance();
				lookAndFeels.put(lf.getName(), s);
			}
			
		} catch (final Exception t)
		{
			t.printStackTrace();
			// we know all machines have these 3, so use them
			lookAndFeels.clear();
			lookAndFeels.put("Original", UIManager.getSystemLookAndFeelClassName());
			lookAndFeels.put("Metal", MetalLookAndFeel.class.getName());
			lookAndFeels.put("Platform Independent", UIManager.getCrossPlatformLookAndFeelClassName());
		}
		final JList list = new JList(new Vector<String>(lookAndFeels.keySet()));
		String currentKey = null;
		for (final String s : lookAndFeels.keySet())
		{
			final String currentName = UIManager.getLookAndFeel().getClass().getName();
			if (lookAndFeels.get(s).equals(currentName))
			{
				currentKey = s;
				break;
			}
		}
		list.setSelectedValue(currentKey, false);
		return new Triple<JList, Map<String, String>, String>(list, lookAndFeels, currentKey);
	}
	
	private void addSetLookAndFeel(final JMenu menuView)
	{
		menuView.add(new AbstractAction("Set Look and Feel...")
		{
			private static final long serialVersionUID = 379919988820952164L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final Triple<JList, Map<String, String>, String> lookAndFeel = getLookAndFeelList();
				final JList list = lookAndFeel.getFirst();
				final String currentKey = lookAndFeel.getThird();
				final Map<String, String> lookAndFeels = lookAndFeel.getSecond();
				if (JOptionPane.showConfirmDialog(m_frame, list) == JOptionPane.OK_OPTION)
				{
					final String selectedValue = (String) list.getSelectedValue();
					if (selectedValue == null)
					{
						return;
					}
					if (selectedValue.equals(currentKey))
					{
						return;
					}
					GameRunner2.setDefaultLookAndFeel(lookAndFeels.get(selectedValue));
					EventThreadJOptionPane.showMessageDialog(m_frame, "The look and feel will update when you restart TripleA");
				}
			}
		});
	}
	
	private void addShowGameUuid(final JMenu menuView)
	{
		menuView.add(new AbstractAction("Game UUID...")
		{
			private static final long serialVersionUID = 119615303846107510L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final String id = (String) getData().getProperties().get(GameData.GAME_UUID);
				final JTextField text = new JTextField();
				text.setText(id);
				final JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("Game UUID:"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Game UUID", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
			}
		});
	}
	
	private void addChatTimeMenu(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem chatTimeBox = new JCheckBoxMenuItem("Show Chat Times");
		chatTimeBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_frame.setShowChatTime(chatTimeBox.isSelected());
			}
		});
		chatTimeBox.setSelected(false);
		parentMenu.add(chatTimeBox);
		chatTimeBox.setEnabled(MainFrame.getInstance().getChat() != null);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addEditMode(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
		editMode.setModel(m_frame.getEditModeButtonModel());
		parentMenu.add(editMode);
	}
	
	private void addZoomMenu(final JMenu menuGame)
	{
		final Action mapZoom = new AbstractAction("Map Zoom...")
		{
			private static final long serialVersionUID = 1114072913279994047L;
			
			public void actionPerformed(final ActionEvent arg0)
			{
				final SpinnerNumberModel model = new SpinnerNumberModel();
				model.setMaximum(100);
				model.setMinimum(15);
				model.setStepSize(1);
				model.setValue((int) (m_frame.getMapPanel().getScale() * 100));
				final JSpinner spinner = new JSpinner(model);
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				panel.add(new JLabel("Choose Map Scale Percentage"), BorderLayout.NORTH);
				panel.add(spinner, BorderLayout.CENTER);
				final JPanel buttons = new JPanel();
				final JButton fitWidth = new JButton("Fit Width");
				buttons.add(fitWidth);
				final JButton fitHeight = new JButton("Fit Height");
				buttons.add(fitHeight);
				final JButton reset = new JButton("Reset");
				buttons.add(reset);
				panel.add(buttons, BorderLayout.SOUTH);
				fitWidth.addActionListener(new ActionListener()
				{
					public void actionPerformed(final ActionEvent e)
					{
						final double screenWidth = m_frame.getMapPanel().getWidth();
						final double mapWidth = m_frame.getMapPanel().getImageWidth();
						double ratio = screenWidth / mapWidth;
						ratio = Math.max(0.15, ratio);
						ratio = Math.min(1, ratio);
						model.setValue((int) (ratio * 100));
					}
				});
				fitHeight.addActionListener(new ActionListener()
				{
					public void actionPerformed(final ActionEvent e)
					{
						final double screenHeight = m_frame.getMapPanel().getHeight();
						final double mapHeight = m_frame.getMapPanel().getImageHeight();
						double ratio = screenHeight / mapHeight;
						ratio = Math.max(0.15, ratio);
						model.setValue((int) (ratio * 100));
					}
				});
				reset.addActionListener(new ActionListener()
				{
					public void actionPerformed(final ActionEvent e)
					{
						model.setValue(100);
					}
				});
				final int result = JOptionPane.showOptionDialog(m_frame, panel, "Choose Map Scale", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK", "Cancel" }, 0);
				if (result != 0)
					return;
				final Number value = (Number) model.getValue();
				m_frame.setScale(value.doubleValue());
			}
		};
		menuGame.add(mapZoom);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowVerifiedDice(final JMenu parentMenu)
	{
		final Action showVerifiedDice = new AbstractAction("Show Verified Dice..")
		{
			private static final long serialVersionUID = -2753073276162734526L;
			
			public void actionPerformed(final ActionEvent e)
			{
				new VerifiedRandomNumbersDialog(m_frame.getRootPane()).setVisible(true);
			}
		};
		if (getGame() instanceof ClientGame)
			parentMenu.add(showVerifiedDice);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addSaveScreenshot(final JMenu parentMenu)
	{
		parentMenu.add(m_frame.getSaveScreenshotAction());
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowCommentLog(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem showCommentLog = new JCheckBoxMenuItem("Show Comment Log");
		showCommentLog.setModel(m_frame.getShowCommentLogButtonModel());
		parentMenu.add(showCommentLog);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowEnemyCasualties(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem showEnemyCasualties = new JCheckBoxMenuItem("Confirm Enemy Casualties");
		showEnemyCasualties.setSelected(BattleDisplay.getShowEnemyCasualtyNotification());
		showEnemyCasualties.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -7445092411530581715L;
			
			public void actionPerformed(final ActionEvent e)
			{
				BattleDisplay.setShowEnemyCasualtyNotification(showEnemyCasualties.isSelected());
			}
		});
		parentMenu.add(showEnemyCasualties);
	}
	
	private void addTabbedProduction(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem tabbedProduction = new JCheckBoxMenuItem("Show Production Tabs");
		tabbedProduction.setSelected(PurchasePanel.isTabbedProduction());
		tabbedProduction.addActionListener(new AbstractAction()
		{
			private static final long serialVersionUID = -352890957050498753L;
			
			public void actionPerformed(final ActionEvent e)
			{
				PurchasePanel.setTabbedProduction(tabbedProduction.isSelected());
			}
		});
		parentMenu.add(tabbedProduction);
	}
	
	/**
	 * @param menuGame
	 */
	private void addGameOptionsMenu(final JMenu menuGame)
	{
		if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
		{
			final AbstractAction optionsAction = new AbstractAction("View Game Options...")
			{
				private static final long serialVersionUID = 8937205081994328616L;
				
				public void actionPerformed(final ActionEvent e)
				{
					final PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties().getEditableProperties(), false);
					JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
				}
			};
			menuGame.add(optionsAction);
		}
	}
	
	/**
	 * Add a Politics Panel button to the game menu, this panel will show the
	 * current political landscape as a reference, no actions on this panel.
	 * 
	 * @param menuGame
	 */
	private void addPoliticsMenu(final JMenu menuGame)
	{
		final AbstractAction politicsAction = new AbstractAction("Show Politics Panel")
		{
			private static final long serialVersionUID = 7880136597816371478L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final PoliticalStateOverview ui = new PoliticalStateOverview(getData(), getUIContext());
				final JScrollPane scroll = new JScrollPane(ui);
				scroll.setBorder(BorderFactory.createEmptyBorder());
				final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
				final int availHeight = screenResolution.height - 120; // not only do we have a start bar, but we also have the message dialog to account for
				final int availWidth = screenResolution.width - 40; // just the scroll bars plus the window sides
				// availHeight = (int) ((float) availHeight * 2 / 3);
				scroll.setPreferredSize(new Dimension((scroll.getPreferredSize().width > availWidth ? availWidth : scroll.getPreferredSize().width),
							(scroll.getPreferredSize().height > availHeight ? availHeight : scroll.getPreferredSize().height)));
				// scroll.setMaximumSize(new Dimension(availWidth, availHeight));
				JOptionPane.showMessageDialog(m_frame, scroll, "Politics Panel", JOptionPane.PLAIN_MESSAGE);
			}
		};
		menuGame.add(politicsAction);
	}
	
	private void addShowUnits(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem showUnitsBox = new JCheckBoxMenuItem("Show Units");
		showUnitsBox.setSelected(true);
		showUnitsBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final boolean tfselected = showUnitsBox.isSelected();
				getUIContext().setShowUnits(tfselected);
				// games.strategy.triplea.ui.screen.TileManager.store(tfselected);
				m_frame.getMapPanel().resetMap();
			}
		});
		parentMenu.add(showUnitsBox);
	}
	
	private void addDrawTerritoryBordersAgain(final JMenu parentMenu)
	{
		final JMenu drawBordersMenu = new JMenu();
		drawBordersMenu.setText("Draw Borders On Top");
		final JRadioButton noneButton = new JRadioButton("Low");
		final JRadioButton mediumButton = new JRadioButton("Medium");
		final JRadioButton highButton = new JRadioButton("High");
		final ButtonGroup group = new ButtonGroup();
		group.add(noneButton);
		group.add(mediumButton);
		group.add(highButton);
		drawBordersMenu.addMenuListener(new MenuListener()
		{
			public void menuSelected(final MenuEvent e)
			{
				final OptionalExtraBorderLevel current = getUIContext().getDrawTerritoryBordersAgain();
				if (current == OptionalExtraBorderLevel.LOW)
					noneButton.setSelected(true);
				else if (current == OptionalExtraBorderLevel.MEDIUM)
					mediumButton.setSelected(true);
				else if (current == OptionalExtraBorderLevel.HIGH)
					highButton.setSelected(true);
			}
			
			public void menuDeselected(final MenuEvent e)
			{
			}
			
			public void menuCanceled(final MenuEvent e)
			{
			}
		});
		noneButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (noneButton.isSelected() && getUIContext().getDrawTerritoryBordersAgain() != OptionalExtraBorderLevel.LOW)
				{
					getUIContext().setDrawTerritoryBordersAgain(OptionalExtraBorderLevel.LOW);
					m_frame.getMapPanel().resetMap();
				}
			}
		});
		mediumButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (mediumButton.isSelected() && getUIContext().getDrawTerritoryBordersAgain() != OptionalExtraBorderLevel.MEDIUM)
				{
					getUIContext().setDrawTerritoryBordersAgain(OptionalExtraBorderLevel.MEDIUM);
					m_frame.getMapPanel().resetMap();
				}
			}
		});
		highButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (highButton.isSelected() && getUIContext().getDrawTerritoryBordersAgain() != OptionalExtraBorderLevel.HIGH)
				{
					getUIContext().setDrawTerritoryBordersAgain(OptionalExtraBorderLevel.HIGH);
					m_frame.getMapPanel().resetMap();
				}
			}
		});
		drawBordersMenu.add(noneButton);
		drawBordersMenu.add(mediumButton);
		drawBordersMenu.add(highButton);
		parentMenu.add(drawBordersMenu);
	}
	
	private void addShowTerritoryEffects(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem territoryEffectsBox = new JCheckBoxMenuItem("Show TerritoryEffects");
		
		territoryEffectsBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final boolean tfselected = territoryEffectsBox.isSelected();
				getUIContext().setShowTerritoryEffects(tfselected);
				m_frame.getMapPanel().resetMap();
			}
		});
		parentMenu.add(territoryEffectsBox);
		territoryEffectsBox.setSelected(true);
	}
	
	private void addLockMap(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem lockMapBox = new JCheckBoxMenuItem("Lock Map");
		lockMapBox.setSelected(getUIContext().getLockMap());
		lockMapBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				getUIContext().setLockMap(lockMapBox.isSelected());
			}
		});
		parentMenu.add(lockMapBox);
	}
	
	private void addShowAIBattles(final JMenu parentMenu)
	{
		final JCheckBoxMenuItem showAIBattlesBox = new JCheckBoxMenuItem("Show Battles Between AIs");
		showAIBattlesBox.setSelected(getUIContext().getShowBattlesBetweenAIs());
		showAIBattlesBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				getUIContext().setShowBattlesBetweenAIs(showAIBattlesBox.isSelected());
			}
		});
		parentMenu.add(showAIBattlesBox);
	}
	
	private void addChangeDynamixAISettings(final JMenu parentMenu)
	{
		boolean areThereDynamixAIs = false;
		final Set<IGamePlayer> players = (m_frame).GetLocalPlayers();
		for (final IGamePlayer player : players)
		{
			if (player instanceof Dynamix_AI)
				areThereDynamixAIs = true;
		}
		if (areThereDynamixAIs)
		{
			Dynamix_AI.ClearAIInstancesMemory();
			Dynamix_AI.Initialize(m_frame);
			for (final IGamePlayer player : players)
			{
				if (player instanceof Dynamix_AI)
					Dynamix_AI.AddDynamixAIIntoAIInstancesMemory((Dynamix_AI) player);
			}
			parentMenu.addSeparator();
			parentMenu.add(new AbstractAction("Change Dynamix AI Settings")
			{
				private static final long serialVersionUID = 2257295609477283292L;
				
				public void actionPerformed(final ActionEvent e)
				{
					Dynamix_AI.ShowSettingsWindow();
				}
			});
			parentMenu.addSeparator();
		}
	}
	
	private void addAISleepDuration(final JMenu parentMenu)
	{
		final JMenuItem AISleepDurationBox = new JMenuItem("AI Pause Duration...");
		AISleepDurationBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final IntTextField text = new IntTextField(0, 10000);
				text.setText(String.valueOf(UIContext.getAIPauseDuration()));
				final JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("AI Pause Duration (ms):"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				panel.add(text, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Set AI Pause Duration", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
				try
				{
					UIContext.setAIPauseDuration(Integer.parseInt(text.getText()));
				} catch (final Exception ex)
				{
				}
			}
		});
		parentMenu.add(AISleepDurationBox);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addShowDiceStats(final JMenu parentMenu)
	{
		final Action showDiceStats = new AbstractAction("Show Dice Stats...")
		{
			private static final long serialVersionUID = 5530880208301800861L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final IRandomStats randomStats = (IRandomStats) getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);
				final RandomStatsDetails stats = randomStats.getRandomStats(getData().getDiceSides());
				JOptionPane.showMessageDialog(m_frame, new JScrollPane(stats.getAllStats()), "Random Stats", JOptionPane.INFORMATION_MESSAGE);
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	private void addRollDice(final JMenu parentMenu)
	{
		final JMenuItem RollDiceBox = new JMenuItem("Roll Dice...");
		RollDiceBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				final IntTextField numberOfText = new IntTextField(0, 100);
				final IntTextField diceSidesText = new IntTextField(1, 200);
				numberOfText.setText(String.valueOf(0));
				diceSidesText.setText(String.valueOf(getGame().getData().getDiceSides()));
				final JPanel panel = new JPanel();
				panel.setLayout(new GridBagLayout());
				panel.add(new JLabel("Number of Dice to Roll: "), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
				panel.add(new JLabel("Sides on the Dice: "), new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
				panel.add(numberOfText, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
				panel.add(diceSidesText, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
				JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleaMenu.this), panel, "Roll Dice", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK" }, "OK");
				try
				{
					final int numberOfDice = Integer.parseInt(numberOfText.getText());
					if (numberOfDice > 0)
					{
						final int diceSides = Integer.parseInt(diceSidesText.getText());
						final int[] dice = getGame().getRandomSource().getRandom(diceSides, numberOfDice, "Rolling Dice, no effect on game.");
						final JPanel panelDice = new JPanel();
						final BoxLayout layout = new BoxLayout(panelDice, BoxLayout.Y_AXIS);
						panelDice.setLayout(layout);
						final JLabel label = new JLabel("Rolls (no effect on game): ");
						panelDice.add(label);
						String diceString = "";
						for (int i = 0; i < dice.length; i++)
							diceString += String.valueOf(dice[i] + 1) + ((i == dice.length - 1) ? "" : ", ");
						final JTextField diceList = new JTextField(diceString);
						diceList.setEditable(false);
						panelDice.add(diceList);
						JOptionPane.showMessageDialog(m_frame, panelDice, "Dice Rolled", JOptionPane.INFORMATION_MESSAGE);
					}
				} catch (final Exception ex)
				{
				}
			}
		});
		parentMenu.add(RollDiceBox);
	}
	
	private void addBattleCalculatorMenu(final JMenu menuGame)
	{
		final Action showBattleMenu = new AbstractAction("Battle Calculator...")
		{
			private static final long serialVersionUID = 6211814774301020553L;
			
			public void actionPerformed(final ActionEvent arg0)
			{
				OddsCalculatorDialog.show(m_frame, null);
			}
		};
		menuGame.add(showBattleMenu);
	}
	
	// TODO: create a second menu option for parsing current attachments
	private void addExportXML(final JMenu parentMenu)
	{
		final Action exportXML = new AbstractAction("Export game.xml file (Beta)...")
		{
			private static final long serialVersionUID = 8379478036021948990L;
			
			public void actionPerformed(final ActionEvent e)
			{
				exportXMLFile();
			}
			
			private void exportXMLFile()
			{
				final JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				final File rootDir = new File(System.getProperties().getProperty("user.dir"));
				final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
				String defaultFileName = "xml_" + formatDate.format(new Date()) + "_" + getData().getGameName() + "_round_" + getData().getSequence().getRound();
				defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
				defaultFileName = defaultFileName + ".xml";
				chooser.setSelectedFile(new File(rootDir, defaultFileName));
				if (chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION)
					return;
				final GameData data = getData();
				final GameDataExporter exporter = new games.strategy.engine.data.export.GameDataExporter(data, false);
				final String xmlFile = exporter.getXML();
				try
				{
					final FileWriter writer = new FileWriter(chooser.getSelectedFile());
					try
					{
						writer.write(xmlFile);
					} finally
					{
						writer.close();
					}
				} catch (final IOException e1)
				{
					e1.printStackTrace();
				}
			}
		};
		parentMenu.add(exportXML);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportStatsFull(final JMenu parentMenu)
	{
		final Action showDiceStats = new AbstractAction("Export Full Game Stats...")
		{
			private static final long serialVersionUID = -1732417863580400806L;
			
			public void actionPerformed(final ActionEvent e)
			{
				createAndSaveStats(true);
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportStats(final JMenu parentMenu)
	{
		final Action showDiceStats = new AbstractAction("Export Short Game Stats...")
		{
			private static final long serialVersionUID = 8196345604657706754L;
			
			public void actionPerformed(final ActionEvent e)
			{
				createAndSaveStats(false);
			}
		};
		parentMenu.add(showDiceStats);
	}
	
	/**
     *
     */
	private void createAndSaveStats(final boolean showPhaseStats)
	{
		final ExtendedStats statPanel = new ExtendedStats(getData());
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final File rootDir = new File(System.getProperties().getProperty("user.dir"));
		final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
		String defaultFileName = "stats_" + formatDate.format(new Date()) + "_" + getData().getGameName() + "_round_" + getData().getSequence().getRound() + (showPhaseStats ? "_full" : "_short");
		defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
		defaultFileName = defaultFileName + ".csv";
		chooser.setSelectedFile(new File(rootDir, defaultFileName));
		if (chooser.showSaveDialog(m_frame) != JOptionPane.OK_OPTION)
			return;
		GameData clone;
		getData().acquireReadLock();
		try
		{
			clone = GameDataUtils.cloneGameData(getData());
		} finally
		{
			getData().releaseReadLock();
		}
		final IStat[] stats = statPanel.getStats();
		// extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech tokens or # techs, etc.
		final IStat[] statsExtended = statPanel.getStatsExtended(getData());
		final String[] alliances = statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
		final PlayerID[] players = statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
		// its important here to translate the player objects into our game data
		// the players for the stat panel are only relevant with respect to
		// the game data they belong to
		for (int i = 0; i < players.length; i++)
		{
			players[i] = clone.getPlayerList().getPlayerID(players[i].getName());
		}
		final StringBuilder text = new StringBuilder(1000);
		text.append(defaultFileName + ",");
		text.append("\n");
		text.append("TripleA Engine Version: ,");
		text.append(games.strategy.engine.EngineVersion.VERSION.toString() + ",");
		text.append("\n");
		text.append("Game Name: ,");
		text.append(getData().getGameName() + ",");
		text.append("\n");
		text.append("Game Version: ,");
		text.append(getData().getGameVersion() + ",");
		text.append("\n");
		text.append("\n");
		text.append("Current Round: ,");
		text.append(getData().getSequence().getRound() + ",");
		text.append("\n");
		text.append("Number of Players: ,");
		text.append(statPanel.getPlayers().size() + ",");
		text.append("\n");
		text.append("Number of Alliances: ,");
		text.append(statPanel.getAlliances().size() + ",");
		text.append("\n");
		text.append("\n");
		text.append("Turn Order: ,");
		text.append("\n");
		final List<PlayerID> playerOrderList = new ArrayList<PlayerID>();
		playerOrderList.addAll(getData().getPlayerList().getPlayers());
		Collections.sort(playerOrderList, new PlayerOrderComparator(getData()));
		final Set<PlayerID> playerOrderSetNoDuplicates = new LinkedHashSet<PlayerID>(playerOrderList);
		final Iterator<PlayerID> playerOrderIterator = playerOrderSetNoDuplicates.iterator();
		while (playerOrderIterator.hasNext())
		{
			final PlayerID currentPlayerID = playerOrderIterator.next();
			text.append(currentPlayerID.getName() + ",");
			final Iterator<String> allianceName = getData().getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerID).iterator();
			while (allianceName.hasNext())
			{
				text.append(allianceName.next() + ",");
			}
			text.append("\n");
		}
		text.append("\n");
		text.append("Winners: ,");
		final EndRoundDelegate delegateEndRound = (EndRoundDelegate) getData().getDelegateList().getDelegate("endRound");
		if (delegateEndRound != null && delegateEndRound.getWinners() != null)
		{
			for (final PlayerID p : delegateEndRound.getWinners())
			{
				text.append(p.getName() + ",");
			}
		}
		else
			text.append("none yet; game not over,");
		text.append("\n");
		text.append("\n");
		text.append("Resource Chart: ,");
		text.append("\n");
		final Iterator<Resource> resourceIterator = getData().getResourceList().getResources().iterator();
		while (resourceIterator.hasNext())
		{
			text.append(resourceIterator.next().getName() + ",");
			text.append("\n");
		}
		// if short, we won't both showing production and unit info
		if (showPhaseStats)
		{
			text.append("\n");
			text.append("Production Rules: ,");
			text.append("\n");
			text.append("Name,Result,Quantity,Cost,Resource,\n");
			final Iterator<ProductionRule> purchaseOptionsIterator = getData().getProductionRuleList().getProductionRules().iterator();
			while (purchaseOptionsIterator.hasNext())
			{
				final ProductionRule pr = purchaseOptionsIterator.next();
				String costString = pr.toStringCosts().replaceAll("; ", ",");
				costString = costString.replaceAll(" ", ",");
				text.append(pr.getName() + "," + pr.getResults().keySet().iterator().next().getName() + "," + pr.getResults().getInt(pr.getResults().keySet().iterator().next()) + ","
							+ costString + ",");
				text.append("\n");
			}
			text.append("\n");
			text.append("Unit Types: ,");
			text.append("\n");
			text.append("Name,Listed Abilities\n");
			final Iterator<UnitType> allUnitsIterator = getData().getUnitTypeList().iterator();
			while (allUnitsIterator.hasNext())
			{
				final UnitAttachment ua = UnitAttachment.get(allUnitsIterator.next());
				if (ua == null)
					continue;
				String toModify = ua.allUnitStatsForExporter();
				toModify = toModify.replaceFirst("UnitType called ", "").replaceFirst(" with:", "").replaceAll("games.strategy.engine.data.", "")
							.replaceAll("\n", ";").replaceAll(",", ";");
				toModify = toModify.replaceAll("  ", ",");
				toModify = toModify.replaceAll(", ", ",").replaceAll(" ,", ",");
				text.append(toModify);
				text.append("\n");
			}
		}
		text.append("\n");
		text.append((showPhaseStats ? "Full Stats (includes each phase that had activity)," : "Short Stats (only shows first phase with activity per player per round),"));
		text.append("\n");
		text.append("Turn Stats: ,");
		text.append("\n");
		text.append("Round,Player Turn,Phase Name,");
		for (int i = 0; i < stats.length; i++)
		{
			for (int j = 0; j < players.length; j++)
			{
				text.append(stats[i].getName()).append(" ");
				text.append(players[j].getName());
				text.append(",");
			}
			for (int j = 0; j < alliances.length; j++)
			{
				text.append(stats[i].getName()).append(" ");
				text.append(alliances[j]);
				text.append(",");
			}
		}
		for (int i = 0; i < statsExtended.length; i++)
		{
			for (int j = 0; j < players.length; j++)
			{
				text.append(statsExtended[i].getName()).append(" ");
				text.append(players[j].getName());
				text.append(",");
			}
			for (int j = 0; j < alliances.length; j++)
			{
				text.append(statsExtended[i].getName()).append(" ");
				text.append(alliances[j]);
				text.append(",");
			}
		}
		text.append("\n");
		clone.getHistory().gotoNode(clone.getHistory().getLastNode());
		@SuppressWarnings("rawtypes")
		final Enumeration nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
		PlayerID currentPlayer = null;
		int round = 0;
		while (nodes.hasMoreElements())
		{
			// we want to export on change of turn
			final HistoryNode element = (HistoryNode) nodes.nextElement();
			if (element instanceof Round)
				round++;
			if (!(element instanceof Step))
				continue;
			final Step step = (Step) element;
			if (step.getPlayerID() == null || step.getPlayerID().isNull())
				continue;
			// this is to stop from having multiple entries for each players turn.
			if (!showPhaseStats)
			{
				if (step.getPlayerID() == currentPlayer)
					continue;
			}
			currentPlayer = step.getPlayerID();
			clone.getHistory().gotoNode(element);
			final String playerName = step.getPlayerID() == null ? "" : step.getPlayerID().getName() + ": ";
			String stepName = step.getStepName();
			// copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
			if (stepName.endsWith("Bid"))
				stepName = "Bid";
			else if (stepName.endsWith("Tech"))
				stepName = "Tech";
			else if (stepName.endsWith("TechActivation"))
				stepName = "TechActivation";
			else if (stepName.endsWith("Purchase"))
				stepName = "Purchase";
			else if (stepName.endsWith("NonCombatMove"))
				stepName = "NonCombatMove";
			else if (stepName.endsWith("Move"))
				stepName = "Move";
			else if (stepName.endsWith("Battle"))
				stepName = "Battle";
			else if (stepName.endsWith("BidPlace"))
				stepName = "BidPlace";
			else if (stepName.endsWith("Place"))
				stepName = "Place";
			else if (stepName.endsWith("Politics"))
				stepName = "Politics";
			else if (stepName.endsWith("EndTurn"))
				stepName = "EndTurn";
			else
				stepName = "";
			text.append(round).append(",").append(playerName).append(",").append(stepName).append(",");
			for (int i = 0; i < stats.length; i++)
			{
				for (int j = 0; j < players.length; j++)
				{
					text.append(stats[i].getFormatter().format(stats[i].getValue(players[j], clone)));
					text.append(",");
				}
				for (int j = 0; j < alliances.length; j++)
				{
					text.append(stats[i].getFormatter().format(stats[i].getValue(alliances[j], clone)));
					text.append(",");
				}
			}
			for (int i = 0; i < statsExtended.length; i++)
			{
				for (int j = 0; j < players.length; j++)
				{
					text.append(statsExtended[i].getFormatter().format(statsExtended[i].getValue(players[j], clone)));
					text.append(",");
				}
				for (int j = 0; j < alliances.length; j++)
				{
					text.append(statsExtended[i].getFormatter().format(statsExtended[i].getValue(alliances[j], clone)));
					text.append(",");
				}
			}
			text.append("\n");
		}
		try
		{
			final FileWriter writer = new FileWriter(chooser.getSelectedFile());
			try
			{
				writer.write(text.toString());
			} finally
			{
				writer.close();
			}
		} catch (final IOException e1)
		{
			e1.printStackTrace();
		}
	}
	
	private void setWidgetActivation()
	{
		showMapDetails.setEnabled(getUIContext().getMapData().getHasRelief());
	}
	
	/**
	 * @param menuGame
	 */
	private void addShowMapDetails(final JMenu menuGame)
	{
		showMapDetails = new JCheckBoxMenuItem("Show Map Details");
		showMapDetails.setSelected(TileImageFactory.getShowReliefImages());
		showMapDetails.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (TileImageFactory.getShowReliefImages() == showMapDetails.isSelected())
					return;
				TileImageFactory.setShowReliefImages(showMapDetails.isSelected());
				final Thread t = new Thread("Triplea : Show map details thread")
				{
					@Override
					public void run()
					{
						yield();
						m_frame.getMapPanel().updateCountries(getData().getMap().getTerritories());
					}
				};
				t.start();
			}
		});
		menuGame.add(showMapDetails);
	}
	
	/**
	 * @param menuGame
	 */
	private void addShowMapBlends(final JMenu menuGame)
	{
		showMapBlends = new JCheckBoxMenuItem("Show Map Blends");
		if (getUIContext().getMapData().getHasRelief() && showMapDetails.isEnabled() && showMapDetails.isSelected())
		{
			showMapBlends.setEnabled(true);
			showMapBlends.setSelected(TileImageFactory.getShowMapBlends());
		}
		else
		{
			showMapBlends.setSelected(false);
			showMapBlends.setEnabled(false);
		}
		showMapBlends.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				if (TileImageFactory.getShowMapBlends() == showMapBlends.isSelected())
				{
					return;
				}
				TileImageFactory.setShowMapBlends(showMapBlends.isSelected());
				TileImageFactory.setShowMapBlendMode(m_frame.getUIContext().getMapData().getMapBlendMode());
				TileImageFactory.setShowMapBlendAlpha(m_frame.getUIContext().getMapData().getMapBlendAlpha());
				final Thread t = new Thread("Triplea : Show map Blends thread")
				{
					@Override
					public void run()
					{
						m_frame.setScale(m_frame.getUIContext().getScale() * 100);
						yield();
						m_frame.getMapPanel().updateCountries(getData().getMap().getTerritories());
					}
				};
				t.start();
			}
		});
		menuGame.add(showMapBlends);
	}
	
	/**
	 * @param menuGame
	 */
	private void addMapSkinsMenu(final JMenu menuGame)
	{
		// beagles Mapskin code
		// creates a sub menu of radiobuttons for each available mapdir
		JMenuItem mapMenuItem;
		final JMenu mapSubMenu = new JMenu("Map Skins");
		final ButtonGroup mapButtonGroup = new ButtonGroup();
		menuGame.add(mapSubMenu);
		final Map<String, String> skins = UIContext.getSkins(m_frame.getGame().getData());
		for (final String key : skins.keySet())
		{
			mapMenuItem = new JRadioButtonMenuItem(key);
			mapButtonGroup.add(mapMenuItem);
			mapSubMenu.add(mapMenuItem);
			mapSubMenu.setEnabled(skins.size() > 1);
			if (skins.get(key).equals(UIContext.getMapDir()))
				mapMenuItem.setSelected(true);
			mapMenuItem.addActionListener(new ActionListener()
			{
				public void actionPerformed(final ActionEvent e)
				{
					try
					{
						m_frame.updateMap(skins.get(key));
						if (m_frame.getUIContext().getMapData().getHasRelief())
						{
							showMapDetails.setSelected(true);
						}
						setWidgetActivation();
					} catch (final Exception se)
					{
						se.printStackTrace();
						JOptionPane.showMessageDialog(m_frame, se.getMessage(), "Error Changing Map Skin2", JOptionPane.OK_OPTION);
					}
				}// else
			}// actionPerformed
			);
		}
	}
	
	/**
	 * @param parentMenu
	 */
	private void addExportSetupCharts(final JMenu parentMenu)
	{
		final JMenuItem menuFileExport = new JMenuItem(new AbstractAction("Export Setup Charts...")
		{
			private static final long serialVersionUID = 1596267069546601864L;
			
			public void actionPerformed(final ActionEvent e)
			{
				final JFrame frame = new JFrame("Export Setup Files");
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				final GameData data = m_frame.getGame().getData();
				GameData clonedGameData;
				data.acquireReadLock();
				try
				{
					clonedGameData = GameDataUtils.cloneGameData(data);
				} finally
				{
					data.releaseReadLock();
				}
				final JComponent newContentPane = new SetupFrame(clonedGameData);
				newContentPane.setOpaque(true); // content panes must be opaque
				frame.setContentPane(newContentPane);
				// Display the window.
				frame.pack();
				frame.setLocationRelativeTo(m_frame);
				frame.setVisible(true);
				m_frame.getUIContext().addShutdownWindow(frame);
			}
		});
		parentMenu.add(menuFileExport);
	}
	
	private void addUnitSizeMenu(final JMenu parentMenu)
	{
		final NumberFormat s_decimalFormat = new DecimalFormat("00.##");
		// This is the action listener used
		class UnitSizeAction extends AbstractAction
		{
			private static final long serialVersionUID = -6280511505686687867L;
			private final double m_scaleFactor;
			
			public UnitSizeAction(final double scaleFactor)
			{
				m_scaleFactor = scaleFactor;
				putValue(Action.NAME, s_decimalFormat.format(m_scaleFactor * 100) + "%");
			}
			
			public void actionPerformed(final ActionEvent e)
			{
				getUIContext().setUnitScaleFactor(m_scaleFactor);
				m_frame.getMapPanel().resetMap();
			}
		}
		final JMenu unitSizeMenu = new JMenu();
		unitSizeMenu.setText("Unit Size");
		final ButtonGroup unitSizeGroup = new ButtonGroup();
		final JRadioButtonMenuItem radioItem125 = new JRadioButtonMenuItem(new UnitSizeAction(1.25));
		final JRadioButtonMenuItem radioItem100 = new JRadioButtonMenuItem(new UnitSizeAction(1.0));
		final JRadioButtonMenuItem radioItem87 = new JRadioButtonMenuItem(new UnitSizeAction(0.875));
		final JRadioButtonMenuItem radioItem83 = new JRadioButtonMenuItem(new UnitSizeAction(0.8333));
		final JRadioButtonMenuItem radioItem75 = new JRadioButtonMenuItem(new UnitSizeAction(0.75));
		final JRadioButtonMenuItem radioItem66 = new JRadioButtonMenuItem(new UnitSizeAction(0.6666));
		final JRadioButtonMenuItem radioItem56 = new JRadioButtonMenuItem(new UnitSizeAction(0.5625));
		final JRadioButtonMenuItem radioItem50 = new JRadioButtonMenuItem(new UnitSizeAction(0.5));
		unitSizeGroup.add(radioItem125);
		unitSizeGroup.add(radioItem100);
		unitSizeGroup.add(radioItem87);
		unitSizeGroup.add(radioItem83);
		unitSizeGroup.add(radioItem75);
		unitSizeGroup.add(radioItem66);
		unitSizeGroup.add(radioItem56);
		unitSizeGroup.add(radioItem50);
		radioItem100.setSelected(true);
		// select the closest to to the default size
		@SuppressWarnings("rawtypes")
		final Enumeration enum1 = unitSizeGroup.getElements();
		boolean matchFound = false;
		while (enum1.hasMoreElements())
		{
			final JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) enum1.nextElement();
			final UnitSizeAction action = (UnitSizeAction) menuItem.getAction();
			if (Math.abs(action.m_scaleFactor - getUIContext().getUnitImageFactory().getScaleFactor()) < 0.01)
			{
				menuItem.setSelected(true);
				matchFound = true;
				break;
			}
		}
		if (!matchFound)
			System.err.println("default unit size does not match any menu item");
		unitSizeMenu.add(radioItem125);
		unitSizeMenu.add(radioItem100);
		unitSizeMenu.add(radioItem87);
		unitSizeMenu.add(radioItem83);
		unitSizeMenu.add(radioItem75);
		unitSizeMenu.add(radioItem66);
		unitSizeMenu.add(radioItem56);
		unitSizeMenu.add(radioItem50);
		parentMenu.add(unitSizeMenu);
	}
}
