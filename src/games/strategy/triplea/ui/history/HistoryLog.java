package games.strategy.triplea.ui.history;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class HistoryLog extends JFrame
{
	private static final long serialVersionUID = 4880602702815333376L;
	private final JTextArea m_textArea;
	private final StringWriter m_stringWriter;
	private final PrintWriter m_printWriter;
	
	public HistoryLog()
	{
		m_textArea = new JTextArea(40, 80);
		m_textArea.setEditable(false);
		final JScrollPane scrollingArea = new JScrollPane(m_textArea);
		// ... Get the content pane, set layout, add to center
		final JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		content.add(scrollingArea, BorderLayout.CENTER);
		m_stringWriter = new StringWriter();
		m_printWriter = new PrintWriter(m_stringWriter);
		// ... Set window characteristics.
		this.setContentPane(content);
		this.setTitle("History Log");
		this.pack();
		this.setLocationRelativeTo(null);
	}
	
	public PrintWriter getWriter()
	{
		return m_printWriter;
	}
	
	@Override
	public String toString()
	{
		return m_stringWriter.toString();
	}
	
	public void clear()
	{
		m_stringWriter.getBuffer().delete(0, m_stringWriter.getBuffer().length());
		m_textArea.setText("");
	}
	
	public void printFullTurn(final HistoryNode printNode, final boolean verbose)
	{
		HistoryNode curNode = printNode;
		Step stepNode = null;
		Step turnStartNode = null;
		PlayerID curPlayer = null;
		// find Step node, if exists in this path
		while (curNode != null)
		{
			if (curNode instanceof Step)
			{
				stepNode = (Step) curNode;
				break;
			}
			curNode = (HistoryNode) curNode.getPreviousNode();
		}
		if (stepNode != null)
		{
			curPlayer = stepNode.getPlayerID();
			// get first step for this turn
			while (stepNode != null)
			{
				turnStartNode = stepNode;
				stepNode = (Step) stepNode.getPreviousSibling();
				if (stepNode == null)
					break;
				if (stepNode.getPlayerID() == null)
					break;
				if (!stepNode.getPlayerID().getName().equals(curPlayer.getName()))
					break;
			}
			printRemainingTurn(turnStartNode, verbose, curPlayer.getData().getDiceSides());
		}
		else
			System.err.println("No Step node found!");
	}
	
	@SuppressWarnings("unchecked")
	public void printRemainingTurn(final HistoryNode printNode, final boolean verbose, final int diceSides)
	{
		final PrintWriter logWriter = m_printWriter;
		final String moreIndent = "    ";
		// print out the parent nodes
		DefaultMutableTreeNode curNode = printNode;
		final TreePath parentPath = (new TreePath(printNode.getPath())).getParentPath();
		PlayerID curPlayer = null;
		if (parentPath != null)
		{
			final Object pathToNode[] = parentPath.getPath();
			for (final Object pathNode : pathToNode)
			{
				final HistoryNode node = (HistoryNode) pathNode;
				for (int i = 0; i < node.getLevel(); i++)
					logWriter.print(moreIndent);
				logWriter.println(node.getTitle());
				if (node.getLevel() == 0)
					logWriter.println();
				if (node instanceof Step)
					curPlayer = ((Step) node).getPlayerID();
			}
		}
		final List<String> moveList = new ArrayList<String>();
		boolean moving = false;
		do
		{
			// keep track of conquered territory during combat
			String conquerStr = "";
			final Enumeration nodeEnum = curNode.preorderEnumeration();
			while (nodeEnum.hasMoreElements())
			{
				final HistoryNode node = (HistoryNode) nodeEnum.nextElement();
				final String title = node.getTitle();
				String indent = "";
				for (int i = 0; i < node.getLevel(); i++)
					indent = indent + moreIndent;
				// flush move list
				if (moving && !(node instanceof Renderable))
				{
					final Iterator<String> moveIter = moveList.iterator();
					while (moveIter.hasNext())
					{
						logWriter.println(moveIter.next());
						moveIter.remove();
					}
					moving = false;
				}
				if (node instanceof Renderable)
				{
					final Object details = ((Renderable) node).getRenderingData();
					
					// TODO: why is this here when the one above seems to take care of it just fine? I am commenting out because it is causing MAJOR issues with PBEM posts (fatal error, because if the person does some moves, then for example turns on edit mode and does something then turns it off, then later undo's a move, and the move list is now empty, then we are have a fatal crash at end of the turn when we try to make the history post)
					// flush move list (but support conquering territory on combat move)
					/*if (moving && (title.equals(MoveDelegate.CLEANING_UP_AFTER_MOVEMENT_PHASES)) && !(details instanceof MoveDescription)) // && !title.matches("\\w+ takes? .*? from \\w+") && !title.indexOf(EditDelegate.EDITMODE_ON) != -1 && title.indexOf(EditDelegate.EDITMODE_OFF) != -1))
					{
						final Iterator<String> moveIter = moveList.iterator();
						while (moveIter.hasNext())
						{
							logWriter.println(moveIter.next());
							moveIter.remove();
						}
						moving = false;
					}*/
					if (details instanceof DiceRoll)
					{
						if (!verbose)
							continue;
						final String diceMsg1 = title.substring(0, title.indexOf(':') + 1);
						if (diceMsg1.equals(""))
						{
							// tech roll
							logWriter.println(indent + moreIndent + title);
						}
						else
						{
							// dice roll
							// Japanese roll dice for 1 armour in Russia, round 1
							logWriter.print(indent + moreIndent + diceMsg1);
							final DiceRoll diceRoll = (DiceRoll) details;
							final int hits = diceRoll.getHits();
							int rolls = 0;
							for (int i = 1; i <= diceSides; i++)
								rolls += diceRoll.getRolls(i).size();
							logWriter.println("  " + hits + "/" + rolls + " hits");
						}
					}
					else if (details instanceof MoveDescription)
					{
						// movement
						final Pattern p = Pattern.compile("\\w+ undo move (\\d+).");
						final Matcher m = p.matcher(title);
						if (m.matches())
						{
							moveList.remove(Integer.valueOf(m.group(1)).intValue() - 1);
						}
						else
						{
							moveList.add(indent + title);
							moving = true;
						}
					}
					else if (details instanceof Collection)
					{
						final Collection<Unit> objects = (Collection) details;
						final Iterator objIter = objects.iterator();
						if (objIter.hasNext())
						{
							final Object obj = objIter.next();
							if (obj instanceof Unit)
							{
								// purchase/place units - don't need details
								Unit unit = (Unit) obj;
								if (title.matches("\\w+ buy .*"))
								{
									logWriter.println(indent + title);
								}
								else if (title.matches("\\w+ attack with .*"))
								{
									logWriter.println(indent + title);
								}
								else if (title.matches("\\w+ defend with .*"))
								{
									logWriter.println(indent + title);
								}
								else if (title.matches("\\d+ \\w+ owned by the .*? lost .*"))
								{
									if (!verbose)
										continue;
									logWriter.println(indent + moreIndent + title);
								}
								else if (title.matches("\\d+ \\w+ owned by the .*? lost"))
								{
									if (!verbose)
										continue;
									logWriter.println(indent + moreIndent + title);
								}
								else if (title.startsWith("Battle casualty summary:"))
								{
									// logWriter.println(indent+"CAS1: "+title);
									logWriter.println(indent + conquerStr + ". Battle score " + title.substring(title.indexOf("for attacker is")));
									conquerStr = "";
									// separate units by player and show casualty summary
									final IntegerMap<PlayerID> unitCount = new IntegerMap<PlayerID>();
									unitCount.add(unit.getOwner(), 1);
									while (objIter.hasNext())
									{
										unit = (Unit) objIter.next();
										unitCount.add(unit.getOwner(), 1);
									}
									for (final PlayerID player : unitCount.keySet())
									{
										logWriter.println(indent + "Casualties for " + player.getName() + ": " + MyFormatter.unitsToTextNoOwner(objects, player));
									}
								}
								else if (title.matches(".*? placed in .*"))
								{
									logWriter.println(indent + title);
								}
								else if (title.matches(".* owned by the \\w+ retreated to .*"))
								{
									logWriter.println(indent + title);
								}
								else if (title.matches("\\w+ win"))
								{
									conquerStr = title + conquerStr + " with " + MyFormatter.unitsToTextNoOwner(objects) + " remaining";
								}
								else
									logWriter.println(indent + title);
							}
							else
							{
								// collection of unhandled objects
								logWriter.println(indent + title);
							}
						}
						else
						{
							// empty collection of something
							if (title.matches("\\w+ win"))
							{
								conquerStr = title + conquerStr + " with no units remaining";
							}
							else
							{
								// empty collection of unhandled objects
								logWriter.println(indent + title);
							}
						}
					}
					else if (details instanceof Territory)
					{
						// territory details
						logWriter.println(indent + title);
					}
					else if (details == null)
					{
						if (title.equals("Adding original owners"))
						{
						}
						else if (title.equals(MoveDelegate.CLEANING_UP_AFTER_MOVEMENT_PHASES))
						{
						}
						else if (title.equals("Game Loaded"))
						{
						}
						else if (title.indexOf("now being played by") != -1)
						{
						}
						else if (title.indexOf("Turn Summary") != -1)
						{
						}
						else if (title.indexOf("Setting uses for triggers used") != -1)
						{
						}
						else if (title.equals("Giving bonus movement to units"))
						{
						}
						else if (title.equals("Recording Battle Statistics"))
						{
						}
						else if (title.equals("Preparing Airbases for Possible Scrambling"))
						{
						}
						else if (title.matches("\\w+ collect \\d+ PUs?.*"))
						{
							logWriter.println(indent + title);
						}
						else if (title.matches("\\w+ takes? .*? from \\w+"))
						{
							// British take Libya from Germans
							if (moving)
							{
								final String str = moveList.remove(moveList.size() - 1);
								moveList.add(str + "\n  " + indent + title.replaceAll(" takes ", " take "));
							}
							else
								conquerStr += title.replaceAll("^\\w+ takes ", ", taking ");
						}
						else if (title.matches("\\w+ spend \\d+ on tech rolls"))
						{
							logWriter.println(indent + title);
						}
						else if (title.startsWith("Rolls to resolve tech hits:"))
						{
						}
						else if (title.matches("\\w+ discover .*"))
						{
							logWriter.println(indent + title);
						}
						else if (title.matches("AA raid costs .*"))
						{
							logWriter.println(indent + title);
						}
						else
						{
							// unhandled message with null details
							logWriter.println(indent + title);
						}
					}
					else
					{
						// unknown details object
						logWriter.println(indent + title);
					}
				}
				else if (node instanceof Step)
				{
					final PlayerID playerId = ((Step) node).getPlayerID();
					if (title.equals("Initializing Delegates"))
					{
					}
					else
					{
						logWriter.println();
						logWriter.print(indent + title);
						if (playerId != null)
						{
							curPlayer = playerId;
							logWriter.print(" - " + playerId.getName());
						}
						logWriter.println();
					}
				}
				else if (node instanceof Round)
				{
					logWriter.println();
					logWriter.println(indent + title);
				}
				else if (title.equals("Game History"))
				{
					logWriter.println(indent + title);
				}
				else
				{
					// unknown node type
					logWriter.println(indent + title);
				}
			} // while (nodeEnum.hasMoreElements())
			curNode = curNode.getNextSibling();
		} while ((curNode instanceof Step) && ((Step) curNode).getPlayerID().equals(curPlayer));
		// if we are mid-phase, this might not get flushed
		if (moving && !moveList.isEmpty())
		{
			final Iterator<String> moveIter = moveList.iterator();
			while (moveIter.hasNext())
			{
				logWriter.println(moveIter.next());
				moveIter.remove();
			}
			moving = false;
		}
		logWriter.println();
		m_textArea.setText(m_stringWriter.toString());
	}
	
	public void printTerritorySummary(final GameData data)
	{
		final PrintWriter logWriter = m_printWriter;
		Collection<Territory> territories;
		PlayerID player;
		// print all units in all territories, including "flags"
		data.acquireReadLock();
		try
		{
			player = data.getSequence().getStep().getPlayerID();
			territories = data.getMap().getTerritories();
		} finally
		{
			data.releaseReadLock();
		}
		logWriter.println("Territory Summary for " + player.getName() + " : \n");
		for (final Territory t : territories)
		{
			final List<Unit> ownedUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			// see if there's a flag
			final TerritoryAttachment ta = TerritoryAttachment.get(t);
			boolean hasFlag = false;
			if (t == null || ta == null)
				hasFlag = false;
			else
				hasFlag = t.getOwner() != null && t.getOwner().equals(player) && (ta.getOriginalOwner() == null || !ta.getOriginalOwner().equals(player));
			if (hasFlag || !ownedUnits.isEmpty())
			{
				logWriter.print("    " + t.getName() + " : ");
				if (hasFlag && ownedUnits.isEmpty())
					logWriter.println("1 flag");
				else
					logWriter.print("1 flag, ");
				if (!ownedUnits.isEmpty())
					logWriter.println(MyFormatter.unitsToTextNoOwner(ownedUnits));
			}
		}
		logWriter.println();
		m_textArea.setText(m_stringWriter.toString());
	}
	
	public void printProductionSummary(final GameData data)
	{
		final PrintWriter logWriter = m_printWriter;
		Collection<PlayerID> players;
		logWriter.println("Production/PUs Summary :\n");
		data.acquireReadLock();
		try
		{
			players = data.getPlayerList().getPlayers();
		} finally
		{
			data.releaseReadLock();
		}
		for (final PlayerID player : players)
		{
			final int PUs = player.getResources().getQuantity(Constants.PUS);
			final int production = getProduction(player, data);
			logWriter.println("    " + player.getName() + " : " + production + " / " + PUs);
		}
		logWriter.println();
		m_textArea.setText(m_stringWriter.toString());
	}
	
	// copied from StatPanel
	private int getProduction(final PlayerID player, final GameData data)
	{
		int rVal = 0;
		final Iterator<Territory> iter = data.getMap().getTerritories().iterator();
		while (iter.hasNext())
		{
			boolean isConvoyOrLand = false;
			final Territory place = iter.next();
			final OriginalOwnerTracker origOwnerTracker = new OriginalOwnerTracker();
			final TerritoryAttachment ta = TerritoryAttachment.get(place);
			if (!place.isWater())
			{
				isConvoyOrLand = true;
			}
			else if (place.isWater() && ta != null && origOwnerTracker.getOriginalOwner(place) != PlayerID.NULL_PLAYERID && origOwnerTracker.getOriginalOwner(place) == player
						&& place.getOwner().equals(player))
			{
				isConvoyOrLand = true;
			}
			if (place.getOwner().equals(player) && isConvoyOrLand)
			{
				if (ta != null)
					rVal += ta.getProduction();
			}
		}
		return rVal;
	}
}
