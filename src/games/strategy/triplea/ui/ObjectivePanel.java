package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.display.DummyDisplay;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

/**
 * A panel that will show all objectives for all players, including if the objective is filled or not.
 * 
 * @author veqryn
 * 
 */
public class ObjectivePanel extends AbstractStatPanel
{
	private static final long serialVersionUID = 3759819236905645520L;
	private Map<String, Map<ICondition, String>> m_statsObjective;
	private ObjectiveTableModel m_objectiveModel;
	private IDelegateBridge m_dummyDelegate;
	
	public ObjectivePanel(final GameData data)
	{
		super(data);
		m_dummyDelegate = new ObjectivePanelDummyDelegateBridge(data);
		initLayout();
	}
	
	@Override
	public String getName()
	{
		return ObjectiveProperties.getInstance().getProperty(ObjectiveProperties.OBJECTIVES_PANEL_NAME, "Objectives");
	}
	
	public boolean isEmpty()
	{
		return m_statsObjective.isEmpty();
	}
	
	public void removeDataChangeListener()
	{
		m_objectiveModel.removeDataChangeListener();
	}
	
	@Override
	protected void initLayout()
	{
		// setLayout(new GridLayout(1, 1));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		m_objectiveModel = new ObjectiveTableModel();
		final JTable table = new JTable(m_objectiveModel);
		table.getTableHeader().setReorderingAllowed(false);
		// table.setDefaultRenderer(String.class, new TextAreaTableCellRenderer());
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
		refresh.addActionListener(new AbstractAction("Refresh Objectives")
		{
			private static final long serialVersionUID = -5217040341132623172L;
			
			public void actionPerformed(final ActionEvent e)
			{
				m_objectiveModel.loadData();
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						table.repaint();
					}
				});
			}
		});
		add(Box.createVerticalStrut(6));
		add(refresh);
		add(Box.createVerticalStrut(6));
		add(scroll);
	}
	
	
	class ObjectiveTableModel extends AbstractTableModel implements GameDataChangeListener
	{
		private static final long serialVersionUID = 2259315408905271333L;
		private static final int COLUMNS_TOTAL = 2;
		private boolean m_isDirty = true;
		private String[][] m_collectedData;
		final Map<String, List<String>> m_sections = new LinkedHashMap<String, List<String>>();
		private long m_timestamp = 0;
		
		public ObjectiveTableModel()
		{
			setObjectiveStats();
			m_data.addDataChangeListener(this);
			m_isDirty = true;
		}
		
		public void removeDataChangeListener()
		{
			m_data.removeDataChangeListener(this);
		}
		
		private void setObjectiveStats()
		{
			m_statsObjective = new LinkedHashMap<String, Map<ICondition, String>>();
			final ObjectiveProperties op = ObjectiveProperties.getInstance();
			final Collection<PlayerID> allPlayers = m_data.getPlayerList().getPlayers();
			final String gameName = IllegalCharacterRemover.replaceIllegalCharacter(m_data.getGameName(), '_').replaceAll(" ", "_").concat(".");
			final Map<String, List<String>> sectionsUnsorted = new HashMap<String, List<String>>();
			final List<String> sectionsSorters = new ArrayList<String>();
			final Map<String, Map<ICondition, String>> statsObjectiveUnsorted = new HashMap<String, Map<ICondition, String>>();
			// do sections first
			for (final Entry<Object, Object> entry : op.entrySet())
			{
				final String fileKey = (String) entry.getKey();
				if (!fileKey.startsWith(gameName))
					continue;
				final String[] key = fileKey.substring(gameName.length(), fileKey.length()).split(";");
				final String value = (String) entry.getValue();
				if (key.length != 2)
				{
					System.err.println("objective.properties keys must be 2 parts: <game_name>." + ObjectiveProperties.GROUP_PROPERTY + ".<#>;player  OR  <game_name>.player;attachmentName");
					continue;
				}
				if (!key[0].startsWith(ObjectiveProperties.GROUP_PROPERTY))
					continue;
				final String[] sorter = key[0].split("\\.");
				if (sorter.length != 2)
				{
					System.err.println("objective.properties " + ObjectiveProperties.GROUP_PROPERTY + "must have .<sorter> after it: " + key[0]);
					continue;
				}
				sectionsSorters.add(sorter[1] + ";" + key[1]);
				sectionsUnsorted.put(key[1], Arrays.asList(value.split(";")));
			}
			Collections.sort(sectionsSorters);
			for (final String section : sectionsSorters)
			{
				final String key = section.split(";")[1];
				m_sections.put(key, sectionsUnsorted.get(key));
				m_statsObjective.put(key, new LinkedHashMap<ICondition, String>());
				statsObjectiveUnsorted.put(key, new HashMap<ICondition, String>());
			}
			// now do the stuff in the sections
			for (final Entry<Object, Object> entry : op.entrySet())
			{
				final String fileKey = (String) entry.getKey();
				if (!fileKey.startsWith(gameName))
					continue;
				final String[] key = fileKey.substring(gameName.length(), fileKey.length()).split(";");
				final String value = (String) entry.getValue();
				if (key.length != 2)
				{
					System.err.println("objective.properties keys must be 2 parts: <game_name>." + ObjectiveProperties.GROUP_PROPERTY + ".<#>;player  OR  <game_name>.player;attachmentName");
					continue;
				}
				if (key[0].startsWith(ObjectiveProperties.GROUP_PROPERTY))
					continue;
				final PlayerID player = m_data.getPlayerList().getPlayerID(key[0]);
				if (player == null)
				{
					// could be an old map, or an old save, so we don't want to stop the game from running.
					System.err.println("objective.properties player does not exist: " + key[0]);
					continue;
				}
				IAttachment attachment = null;
				try
				{
					if (key[1].indexOf(Constants.RULES_OBJECTIVE_PREFIX) != -1 || key[1].indexOf(Constants.RULES_CONDITION_PREFIX) != -1)
						attachment = RulesAttachment.get(player, key[1], allPlayers);
					else if (key[1].indexOf(Constants.TRIGGER_ATTACHMENT_PREFIX) != -1)
						attachment = TriggerAttachment.get(player, key[1], allPlayers);
					else if (key[1].indexOf(Constants.POLITICALACTION_ATTACHMENT_PREFIX) != -1)
						attachment = PoliticalActionAttachment.get(player, key[1], allPlayers);
					else
					{
						System.err.println("objective.properties objective must begin with: " + Constants.RULES_OBJECTIVE_PREFIX + " or " + Constants.RULES_CONDITION_PREFIX + " or "
									+ Constants.TRIGGER_ATTACHMENT_PREFIX + " or " + Constants.POLITICALACTION_ATTACHMENT_PREFIX);
						continue;
					}
				} catch (final Exception e)
				{
					// could be an old map, or an old save, so we don't want to stop the game from running.
					System.err.println(e.getMessage());
					continue;
				}
				if (attachment == null)
				{
					System.err.println("objective.properties attachment does not exist: " + key[1]);
					continue;
				}
				if (!ICondition.class.isAssignableFrom(attachment.getClass()))
					throw new IllegalStateException("(wtf??) attachment is not an ICondition: " + attachment.getName());
				// find which section
				boolean found = false;
				if (m_sections.containsKey(player.getName()))
				{
					if (m_sections.get(player.getName()).contains(key[1]))
					{
						final Map<ICondition, String> map = statsObjectiveUnsorted.get(player.getName());
						if (map == null)
							throw new IllegalStateException("objective.properties group has nothing: " + player.getName());
						map.put((ICondition) attachment, value);
						statsObjectiveUnsorted.put(player.getName(), map);
						found = true;
					}
				}
				if (!found)
				{
					for (final Entry<String, List<String>> sectionEntry : m_sections.entrySet())
					{
						if (sectionEntry.getValue().contains(key[1]))
						{
							final Map<ICondition, String> map = statsObjectiveUnsorted.get(sectionEntry.getKey());
							if (map == null)
								throw new IllegalStateException("objective.properties group has nothing: " + sectionEntry.getKey());
							map.put((ICondition) attachment, value);
							statsObjectiveUnsorted.put(sectionEntry.getKey(), map);
							break;
						}
					}
				}
			}
			for (final Entry<String, Map<ICondition, String>> entry : m_statsObjective.entrySet())
			{
				final Map<ICondition, String> mapUnsorted = statsObjectiveUnsorted.get(entry.getKey());
				final Map<ICondition, String> mapSorted = entry.getValue();
				for (final String conditionString : m_sections.get(entry.getKey()))
				{
					final Iterator<ICondition> conditionIter = mapUnsorted.keySet().iterator();
					while (conditionIter.hasNext())
					{
						final ICondition condition = conditionIter.next();
						if (conditionString.equals(condition.getName()))
						{
							mapSorted.put(condition, mapUnsorted.get(condition));
							conditionIter.remove();
							break;
						}
					}
				}
			}
		}
		
		public synchronized Object getValueAt(final int row, final int col)
		{
			// do not refresh too often, or else it will slow the game down seriously
			if (m_isDirty && Calendar.getInstance().getTimeInMillis() > m_timestamp + 10000)
			{
				loadData();
				m_isDirty = false;
				m_timestamp = Calendar.getInstance().getTimeInMillis();
			}
			return m_collectedData[row][col];
		}
		
		private synchronized void loadData()
		{
			m_data.acquireReadLock();
			try
			{
				final HashMap<ICondition, String> conditions = getConditionComment(getTestedConditions());
				m_collectedData = new String[getRowTotal()][COLUMNS_TOTAL];
				int row = 0;
				for (final Entry<String, Map<ICondition, String>> mapEntry : m_statsObjective.entrySet())
				{
					m_collectedData[row][1] = "<html><span style=\"font-size:140%\"><b><em>" + mapEntry.getKey() + "</em></b></span></html>";
					for (final Entry<ICondition, String> attachmentEntry : mapEntry.getValue().entrySet())
					{
						row++;
						m_collectedData[row][0] = conditions.get(attachmentEntry.getKey());
						m_collectedData[row][1] = "<html>" + attachmentEntry.getValue() + "</html>";
					}
					row++;
					m_collectedData[row][1] = "--------------------";
					row++;
				}
			} finally
			{
				m_data.releaseReadLock();
			}
		}
		
		public HashMap<ICondition, String> getConditionComment(final HashMap<ICondition, Boolean> testedConditions)
		{
			final HashMap<ICondition, String> conditionsComments = new HashMap<ICondition, String>(testedConditions.size());
			for (final Entry<ICondition, Boolean> entry : testedConditions.entrySet())
			{
				final boolean satisfied = entry.getValue();
				if (entry.getKey() instanceof TriggerAttachment)
				{
					final TriggerAttachment ta = (TriggerAttachment) entry.getKey();
					final int each = TriggerAttachment.getEachMultiple(ta);
					final int uses = ta.getUses();
					if (uses < 0)
					{
						final String comment = satisfied ? (each > 1 ? "T" + each : "T") : "F";
						conditionsComments.put(entry.getKey(), comment);
					}
					else if (uses == 0)
					{
						final String comment = satisfied ? "Used" : "used";
						conditionsComments.put(entry.getKey(), comment);
					}
					else
					{
						// if (uses > 0)
						final String comment = uses + "" + (satisfied ? (each > 1 ? "T" + each : "T") : "F");
						conditionsComments.put(entry.getKey(), comment);
					}
				}
				else if (entry.getKey() instanceof RulesAttachment)
				{
					final RulesAttachment ra = (RulesAttachment) entry.getKey();
					final int each = ra.getEachMultiple();
					final int uses = ra.getUses();
					if (uses < 0)
					{
						final String comment = satisfied ? (each > 1 ? "T" + each : "T") : "F";
						conditionsComments.put(entry.getKey(), comment);
					}
					else if (uses == 0)
					{
						final String comment = satisfied ? "Used" : "used";
						conditionsComments.put(entry.getKey(), comment);
					}
					else
					{
						// if (uses > 0)
						final String comment = uses + "" + (satisfied ? (each > 1 ? "T" + each : "T") : "F");
						conditionsComments.put(entry.getKey(), comment);
					}
				}
				else
				{
					conditionsComments.put(entry.getKey(), entry.getValue().toString());
				}
			}
			return conditionsComments;
		}
		
		public HashMap<ICondition, Boolean> getTestedConditions()
		{
			final HashSet<ICondition> myConditions = new HashSet<ICondition>();
			for (final Map<ICondition, String> map : m_statsObjective.values())
			{
				myConditions.addAll(map.keySet());
			}
			final HashSet<ICondition> allConditionsNeeded = RulesAttachment.getAllConditionsRecursive(myConditions, null);
			return RulesAttachment.testAllConditionsRecursive(allConditionsNeeded, null, m_dummyDelegate);
		}
		
		public void gameDataChanged(final Change aChange)
		{
			synchronized (this)
			{
				m_isDirty = true;
			}
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					repaint();
				}
			});
		}
		
		@Override
		public String getColumnName(final int col)
		{
			if (col == 0)
				return "Done";
			else
				return "Objective Name";
		}
		
		public int getColumnCount()
		{
			return COLUMNS_TOTAL;
		}
		
		public synchronized int getRowCount()
		{
			if (!m_isDirty)
				return m_collectedData.length;
			else
			{
				m_data.acquireReadLock();
				try
				{
					return getRowTotal();
				} finally
				{
					m_data.releaseReadLock();
				}
			}
		}
		
		private int getRowTotal()
		{
			int rowsTotal = m_sections.size() * 2; // we include a space between sections as well
			for (final Map<ICondition, String> map : m_statsObjective.values())
			{
				rowsTotal += map.size();
			}
			return rowsTotal;
		}
		
		public synchronized void setGameData(final GameData data)
		{
			synchronized (this)
			{
				m_data.removeDataChangeListener(this);
				m_data = data;
				setObjectiveStats();
				m_data.addDataChangeListener(this);
				m_isDirty = true;
			}
			repaint();
		}
	}
	
	@Override
	public void setGameData(final GameData data)
	{
		m_dummyDelegate = new ObjectivePanelDummyDelegateBridge(data);
		m_data = data;
		m_objectiveModel.setGameData(data);
		m_objectiveModel.gameDataChanged(null);
	}
	
	public static void main(final String[] args)
	{
		final JFrame f = new JFrame();
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		final GameSelectorModel model = new GameSelectorModel();
		model.loadDefaultGame(f);
		final GameData data = model.getGameData();
		final UIContext uiContext = new UIContext();
		uiContext.setDefaultMapDir(data);
		uiContext.getMapData().verify(data);
		// uiContext.setPlayerList(players);
		final ObjectivePanel panelObj = new ObjectivePanel(data);
		/*final String blah = "<html><b>blah</b> blah blah blah blah blah blah blah blah blah blah blah blah " +
					"blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah " +
					"blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah blah</html>";
		final JEditorPane editor = new JEditorPane();
		editor.setContentType("text/html");
		editor.setText(blah);
		editor.setEditable(false);
		panel.add(editor);*/
		panel.add(panelObj);
		/*final JTable table = new JTable(1, 1);
		final TableColumnModel cmodel = table.getColumnModel();
		cmodel.getColumn(0).setCellRenderer(new EditorPaneTableCellRenderer());
		cmodel.getColumn(0).setCellEditor(new EditorPaneCellEditor());
		table.setValueAt(blah, 0, 0);
		panel.add(table);*/
		f.getContentPane().add(panel);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.pack();
		f.setVisible(true);
	}
}


class ObjectiveProperties
{
	// Filename
	private static final String PROPERTY_FILE = "objectives.properties";
	static final String GROUP_PROPERTY = "TABLEGROUP";
	static final String OBJECTIVES_PANEL_NAME = "Objectives.Panel.Name";
	private static ObjectiveProperties s_op = null;
	private static long s_timestamp = 0;
	private final Properties m_properties = new Properties();
	
	protected ObjectiveProperties()
	{
		final ResourceLoader loader = UIContext.getResourceLoader();
		final URL url = loader.getResource(PROPERTY_FILE);
		if (url == null)
		{
			// no property file found
		}
		else
		{
			try
			{
				m_properties.load(url.openStream());
			} catch (final IOException e)
			{
				System.out.println("Error reading " + PROPERTY_FILE + " : " + e);
			}
		}
	}
	
	public static ObjectiveProperties getInstance()
	{
		if (s_op == null || Calendar.getInstance().getTimeInMillis() > s_timestamp + 1000)
		{ // cache properties for 1 second
			s_op = new ObjectiveProperties();
			s_timestamp = Calendar.getInstance().getTimeInMillis();
		}
		return s_op;
	}
	
	public String getProperty(final String objectiveKey)
	{
		return getProperty(objectiveKey, "Not Found In objectives.properties");
	}
	
	public String getProperty(final String objectiveKey, final String defaultValue)
	{
		return m_properties.getProperty(objectiveKey, defaultValue);
	}
	
	public Set<Entry<Object, Object>> entrySet()
	{
		return m_properties.entrySet();
	}
}


class ObjectivePanelDummyDelegateBridge implements IDelegateBridge
{
	private final DummyDisplay m_display = new DummyDisplay();
	private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
	private final GameData m_data;
	private final ObjectivePanelDummyPlayer m_dummyAI = new ObjectivePanelDummyPlayer("objective panel dummy", "None (AI)");
	
	public ObjectivePanelDummyDelegateBridge(final GameData data)
	{
		m_data = data;
	}
	
	public GameData getData()
	{
		return m_data;
	}
	
	public void leaveDelegateExecution()
	{
	}
	
	public Properties getStepProperties()
	{
		throw new UnsupportedOperationException();
	}
	
	public String getStepName()
	{
		throw new UnsupportedOperationException();
	}
	
	public IRemote getRemote(final PlayerID id)
	{
		return m_dummyAI;
	}
	
	public IRemote getRemote()
	{
		return m_dummyAI;
	}
	
	public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType, final String annotation)
	{
		if (count <= 0)
		{
			throw new IllegalStateException("count must be > o, annotation:" + annotation);
		}
		final int[] numbers = new int[count];
		for (int i = 0; i < count; i++)
		{
			numbers[i] = getRandom(max, player, diceType, annotation);
		}
		return numbers;
	}
	
	public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation)
	{
		return 0;
	}
	
	public PlayerID getPlayerID()
	{
		return PlayerID.NULL_PLAYERID;
	}
	
	public IDelegateHistoryWriter getHistoryWriter()
	{
		return m_writer;
	}
	
	public IChannelSubscribor getDisplayChannelBroadcaster()
	{
		return m_display;
	}
	
	public void enterDelegateExecution()
	{
	}
	
	public void addChange(final Change aChange)
	{
	}
	
	public void stopGameSequence()
	{
	}
};


class DummyGameModifiedChannel implements IGameModifiedChannel
{
	public void addChildToEvent(final String text, final Object renderingData)
	{
	}
	
	public void gameDataChanged(final Change aChange)
	{
	}
	
	public void shutDown()
	{
	}
	
	public void startHistoryEvent(final String event)
	{
	}
	
	public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame)
	{
	}
	
	public void startHistoryEvent(final String event, final Object renderingData)
	{
	}
}


class ObjectivePanelDummyPlayer extends AbstractAI
{
	public ObjectivePanelDummyPlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void place(final boolean placeForBid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void purchase(final boolean purcahseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		throw new UnsupportedOperationException();
	}
	
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		throw new UnsupportedOperationException();
	}
	
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleSite, final Collection<Territory> possibleTerritories, final String message)
	{
		throw new UnsupportedOperationException();
	}
	
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		throw new UnsupportedOperationException();
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		throw new UnsupportedOperationException();
	}
	
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID)
	{
		throw new UnsupportedOperationException();
	}
	
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		throw new UnsupportedOperationException();
	}
	
	public boolean shouldBomberBomb(final Territory territory)
	{
		throw new UnsupportedOperationException();
	}
	
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		throw new UnsupportedOperationException();
	}
	
	public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message, final int diceSides)
	{
		final int[] dice = new int[numRolls];
		for (int i = 0; i < numRolls; i++)
		{
			dice[i] = (int) Math.ceil(Math.random() * diceSides);
		}
		return dice;
	}
}


class ColorTableCellRenderer extends DefaultTableCellRenderer
{
	private static final long serialVersionUID = 4197520597103598219L;
	private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
	
	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column)
	{
		adaptee.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		final JLabel renderer = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		renderer.setHorizontalAlignment(SwingConstants.CENTER);
		if (value == null)
			renderer.setBorder(BorderFactory.createEmptyBorder());
		else if (value.toString().indexOf("T") != -1)
			renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.red));
		else if (value.toString().indexOf("U") != -1)
			renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.blue));
		else if (value.toString().indexOf("u") != -1)
			renderer.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.cyan));
		else
			renderer.setBorder(BorderFactory.createEmptyBorder());
		return renderer;
	}
}


// author: Heinz M. Kabutz (modified for JEditorPane by Mark Christopher Duncan)
class EditorPaneCellEditor extends DefaultCellEditor
{
	private static final long serialVersionUID = 509377442956621991L;
	
	public EditorPaneCellEditor()
	{
		super(new JTextField());
		final JEditorPane textArea = new JEditorPane();
		// textArea.setWrapStyleWord(true);
		// textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setContentType("text/html");
		final JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(null);
		editorComponent = scrollPane;
		
		delegate = new DefaultCellEditor.EditorDelegate()
		{
			private static final long serialVersionUID = 5746645959173385516L;
			
			@Override
			public void setValue(final Object value)
			{
				textArea.setText((value != null) ? value.toString() : "");
			}
			
			@Override
			public Object getCellEditorValue()
			{
				return textArea.getText();
			}
		};
	}
}


// author: Heinz M. Kabutz (modified for JEditorPane by Mark Christopher Duncan)
@SuppressWarnings({ "unchecked", "rawtypes" })
class EditorPaneTableCellRenderer extends JEditorPane implements TableCellRenderer
{
	private static final long serialVersionUID = -2835145877164663862L;
	private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
	private final Map cellSizes = new HashMap();
	
	public EditorPaneTableCellRenderer()
	{
		// setLineWrap(true);
		// setWrapStyleWord(true);
		setEditable(false);
		setContentType("text/html");
	}
	
	public Component getTableCellRendererComponent(final JTable table, final Object obj, final boolean isSelected, final boolean hasFocus, final int row, final int column)
	{
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
		int height_wanted = (int) getPreferredSize().getHeight();
		addSize(table, row, column, height_wanted);
		height_wanted = findTotalMaximumRowSize(table, row);
		if (height_wanted != table.getRowHeight(row))
		{
			table.setRowHeight(row, height_wanted);
		}
		return this;
	}
	
	private void addSize(final JTable table, final int row, final int column,
				final int height)
	{
		Map rows = (Map) cellSizes.get(table);
		if (rows == null)
		{
			cellSizes.put(table, rows = new HashMap());
		}
		Map rowheights = (Map) rows.get(new Integer(row));
		if (rowheights == null)
		{
			rows.put(new Integer(row), rowheights = new HashMap());
		}
		rowheights.put(new Integer(column), new Integer(height));
	}
	
	/**
	 * Look through all columns and get the renderer. If it is
	 * also a TextAreaRenderer, we look at the maximum height in
	 * its hash table for this row.
	 */
	private int findTotalMaximumRowSize(final JTable table, final int row)
	{
		int maximum_height = 0;
		final Enumeration columns = table.getColumnModel().getColumns();
		while (columns.hasMoreElements())
		{
			final TableColumn tc = (TableColumn) columns.nextElement();
			final TableCellRenderer cellRenderer = tc.getCellRenderer();
			if (cellRenderer instanceof EditorPaneTableCellRenderer)
			{
				final EditorPaneTableCellRenderer tar = (EditorPaneTableCellRenderer) cellRenderer;
				maximum_height = Math.max(maximum_height, tar.findMaximumRowSize(table, row));
			}
		}
		return maximum_height;
	}
	
	private int findMaximumRowSize(final JTable table, final int row)
	{
		final Map rows = (Map) cellSizes.get(table);
		if (rows == null)
			return 0;
		final Map rowheights = (Map) rows.get(new Integer(row));
		if (rowheights == null)
			return 0;
		int maximum_height = 0;
		for (final Iterator it = rowheights.entrySet().iterator(); it.hasNext();)
		{
			final Map.Entry entry = (Map.Entry) it.next();
			final int cellHeight = ((Integer) entry.getValue()).intValue();
			maximum_height = Math.max(maximum_height, cellHeight);
		}
		return maximum_height;
	}
}

/*
// author: Heinz M. Kabutz
class TextAreaCellEditor extends DefaultCellEditor
{
	private static final long serialVersionUID = 1581016985298673267L;
	
	public TextAreaCellEditor()
	{
		super(new JTextField());
		final JTextArea textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		final JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(null);
		editorComponent = scrollPane;
		
		delegate = new DefaultCellEditor.EditorDelegate()
		{
			private static final long serialVersionUID = 382738058299167626L;
			
			@Override
			public void setValue(final Object value)
			{
				textArea.setText((value != null) ? value.toString() : "");
			}
			
			@Override
			public Object getCellEditorValue()
			{
				return textArea.getText();
			}
		};
	}
}


// author: Heinz M. Kabutz
@SuppressWarnings({ "unchecked", "rawtypes" })
class TextAreaTableCellRenderer extends JTextArea implements TableCellRenderer
{
	private static final long serialVersionUID = 3742567967589372832L;
	private final DefaultTableCellRenderer adaptee = new DefaultTableCellRenderer();
	private final Map cellSizes = new HashMap();
	
	public TextAreaTableCellRenderer()
	{
		setLineWrap(true);
		setWrapStyleWord(true);
		setEditable(false);
	}
	
	public Component getTableCellRendererComponent(final JTable table, final Object obj, final boolean isSelected, final boolean hasFocus, final int row, final int column)
	{
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
		int height_wanted = (int) getPreferredSize().getHeight();
		addSize(table, row, column, height_wanted);
		height_wanted = findTotalMaximumRowSize(table, row);
		if (height_wanted != table.getRowHeight(row))
		{
			table.setRowHeight(row, height_wanted);
		}
		return this;
	}
	
	private void addSize(final JTable table, final int row, final int column,
				final int height)
	{
		Map rows = (Map) cellSizes.get(table);
		if (rows == null)
		{
			cellSizes.put(table, rows = new HashMap());
		}
		Map rowheights = (Map) rows.get(new Integer(row));
		if (rowheights == null)
		{
			rows.put(new Integer(row), rowheights = new HashMap());
		}
		rowheights.put(new Integer(column), new Integer(height));
	}
	
	// Look through all columns and get the renderer. If it is
	// also a TextAreaRenderer, we look at the maximum height in
	// its hash table for this row.
	private int findTotalMaximumRowSize(final JTable table, final int row)
	{
		int maximum_height = 0;
		final Enumeration columns = table.getColumnModel().getColumns();
		while (columns.hasMoreElements())
		{
			final TableColumn tc = (TableColumn) columns.nextElement();
			final TableCellRenderer cellRenderer = tc.getCellRenderer();
			if (cellRenderer instanceof TextAreaTableCellRenderer)
			{
				final TextAreaTableCellRenderer tar = (TextAreaTableCellRenderer) cellRenderer;
				maximum_height = Math.max(maximum_height, tar.findMaximumRowSize(table, row));
			}
		}
		return maximum_height;
	}
	
	private int findMaximumRowSize(final JTable table, final int row)
	{
		final Map rows = (Map) cellSizes.get(table);
		if (rows == null)
			return 0;
		final Map rowheights = (Map) rows.get(new Integer(row));
		if (rowheights == null)
			return 0;
		int maximum_height = 0;
		for (final Iterator it = rowheights.entrySet().iterator(); it.hasNext();)
		{
			final Map.Entry entry = (Map.Entry) it.next();
			final int cellHeight = ((Integer) entry.getValue()).intValue();
			maximum_height = Math.max(maximum_height, cellHeight);
		}
		return maximum_height;
	}
}
*/
