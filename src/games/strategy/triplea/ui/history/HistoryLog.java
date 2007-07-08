package games.strategy.triplea.ui.history;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.io.PrintWriter;
import java.io.StringWriter;

import games.strategy.engine.data.*;
import games.strategy.engine.history.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.*;

import java.util.*;
import java.util.List;
import java.util.regex.*;
import games.strategy.util.*;


public class HistoryLog extends JFrame
{
    private JTextArea    m_textArea;
    private StringWriter m_stringWriter;
    private PrintWriter  m_printWriter;

    public HistoryLog()
    {
        m_textArea = new JTextArea(50, 50);
        m_textArea.setEditable(false);
        JScrollPane scrollingArea = new JScrollPane(m_textArea);
        
        //... Get the content pane, set layout, add to center
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(scrollingArea, BorderLayout.CENTER);
        m_stringWriter = new StringWriter();
        m_printWriter = new PrintWriter(m_stringWriter);
        //... Set window characteristics.
        this.setContentPane(content);
        this.setTitle("History Log");
        this.pack();
        this.setLocationRelativeTo(null);
    }

    public PrintWriter getWriter()
    {
        return m_printWriter;
    }

    public void setEditable(boolean editable)
    {
        //m_textArea.setEditable(editable);
    }

    // a hack, but it works.
    public void setVisible(boolean visible)
    {
        if(visible)
            m_textArea.setText(m_stringWriter.toString());
        super.setVisible(visible);
    }

    public String toString()
    {
        return m_stringWriter.toString();
    }

    public void printTurn(HistoryNode printNode, boolean verbose)
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
                stepNode = (Step)curNode;
                break;
            }
            curNode = (HistoryNode)curNode.getPreviousNode();
        }

        if (stepNode != null)
        {
            curPlayer = stepNode.getPlayerID();
            // get first step for this turn
            while (stepNode != null)
            {
                turnStartNode = stepNode;
                stepNode = (Step)stepNode.getPreviousSibling();
                if (stepNode == null)
                    break;
                if (stepNode.getPlayerID() == null)
                    break;
                if (! stepNode.getPlayerID().getName().equals(curPlayer.getName()))
                    break;
            }

            print(turnStartNode, verbose);
        }
        else
            System.err.println("No Step node found!");
    }

    public void print(HistoryNode printNode, boolean verbose)
    {
        PrintWriter logWriter = m_printWriter;
        String moreIndent = "    ";
        // print out the parent nodes
        DefaultMutableTreeNode curNode = (DefaultMutableTreeNode) printNode;
        TreePath parentPath = (new TreePath(printNode.getPath())).getParentPath();
        PlayerID curPlayer = null;
        if(parentPath != null)
        {
            Object pathToNode[] = parentPath.getPath();
            for (Object pathNode : pathToNode)
            {
                HistoryNode node = (HistoryNode)pathNode;
                for(int i = 0; i < node.getLevel(); i++)
                    logWriter.print(moreIndent);

                logWriter.println(node.getTitle());
                if(node.getLevel() == 0)
                    logWriter.println();
                if(node instanceof Step)
                    curPlayer = ((Step)node).getPlayerID();
            }

        }
        List<String> moveList = new ArrayList<String>();
        boolean moving = false;
        do
        {
            // keep track of conquered territory during combat
            String conquerStr = "";
            Enumeration nodeEnum = curNode.preorderEnumeration();
            while (nodeEnum.hasMoreElements())
            {
                HistoryNode node = (HistoryNode)nodeEnum.nextElement();
                String title = node.getTitle();
                String indent = "";
                for(int i = 0; i < node.getLevel(); i++)
                    indent = indent+moreIndent;

                // flush move list
                if(moving && !(node instanceof Renderable))
                {
                    Iterator<String> moveIter = moveList.iterator();
                    while (moveIter.hasNext())
                    {
                        logWriter.println(moveIter.next());
                        moveIter.remove();
                    }
                    moving = false;
                }
                if(node instanceof Renderable)
                {
                    Object details = ((Renderable)node).getRenderingData();
                    // flush move list
                    // support conquering territory on combat move
                    if (moving && ! (details instanceof MoveDescription
                                     || title.matches("\\w+ takes? .*? from \\w+")))
                    {
                        Iterator<String> moveIter = moveList.iterator();
                        while (moveIter.hasNext())
                        {
                            logWriter.println(moveIter.next());
                            moveIter.remove();
                        }
                        moving = false;
                    }
                    if(details instanceof DiceRoll)
                    {
                        if (!verbose)
                            continue;

                        String diceMsg1 = title.substring(0, title.indexOf(':') + 1);
                        if(diceMsg1.equals(""))
                        {
                            // tech roll
                            logWriter.println(indent+moreIndent+title);
                        } else
                        {
                            // dice roll
                            // Japanese roll dice for 1 armour in Russia, round 1
                            logWriter.print(indent+moreIndent+diceMsg1);
                            DiceRoll diceRoll = (DiceRoll)details;
                            int hits = diceRoll.getHits();
                            int rolls = 0;
                            for (int i=1; i<=Constants.MAX_DICE; i++)
                                rolls += diceRoll.getRolls(i).size();

                            logWriter.println("  "+hits+"/"+rolls+" hits");
                        }
                    } else if (details instanceof MoveDescription)
                    {
                        // movement
                        Pattern p = Pattern.compile("\\w+ undo move (\\d+).");
                        Matcher m = p.matcher(title);
                        if(m.matches())
                        {
                            moveList.remove(Integer.valueOf(m.group(1)).intValue() - 1);
                        } else
                        {
                            moveList.add(indent+title);
                            moving = true;
                        }
                    } else if (details instanceof Collection)
                    {
                        Collection objects = (Collection)details;
                        Iterator objIter = objects.iterator();
                        if(objIter.hasNext())
                        {
                            Object obj = objIter.next();
                            if(obj instanceof Unit)
                            {
                                // purchase/place units - don't need details
                                Unit unit = (Unit)obj;
                                if(title.matches("\\w+ buys? .*"))
                                {
                                    logWriter.println(indent+title.replaceAll("buys", "buy"));
                                } else if (title.matches("\\w+ attack with .*"))
                                {
                                    logWriter.println(indent+title);
                                } else if (title.matches("\\w+ defend with .*"))
                                {
                                    logWriter.println(indent+title);
                                } else if (title.matches("\\d+ \\w+ owned by the .*? lost .*"))
                                {
                                    if (!verbose)
                                        continue;
                                    logWriter.println(moreIndent+title);
                                } else if (title.matches("\\d+ \\w+ owned by the .*? lost"))
                                {
                                    if (!verbose)
                                        continue;
                                    logWriter.println(moreIndent+title);
                                } else if (title.startsWith("Battle casualty summary:"))
                                {
                                    //logWriter.println(indent+"CAS1: "+title);
                                    logWriter.println(indent+conquerStr+". Battle score "+
                                    title.substring(title.indexOf("for attacker is")));
                                    conquerStr = "";
                                    // separate units by player and show casualty summary
                                    IntegerMap<PlayerID> unitCount = new IntegerMap<PlayerID>();
                                    unitCount.add(unit.getOwner(), 1);
                                    while (objIter.hasNext()){
                                        unit = (Unit)objIter.next();
                                        unitCount.add(unit.getOwner(), 1);
                                    }
                                    Iterator<PlayerID> playerIter = unitCount.keySet().iterator();
                                    while (playerIter.hasNext())
                                    {
                                        PlayerID player = playerIter.next();
                                        logWriter.println(indent+"Casualties for "+player.getName()+": "+MyFormatter.unitsToTextNoOwner(objects, player));
                                    }
                                } else if (title.matches(".*? placed in .*"))
                                {
                                    logWriter.println(indent+title);
                                } else if (title.matches(".* owned by the \\w+ retreated to .*"))
                                {
                                    logWriter.println(indent+title);
                                } else if (title.matches("\\w+ win"))
                                {
                                    conquerStr = title+conquerStr+" with "+MyFormatter.unitsToTextNoOwner(objects)+" remaining";
                                } else
                                    logWriter.println(indent+title);
                            } else
                            {
                                // collection of unhandled objects
                                logWriter.println(indent+title);
                            }
                        }
                        else
                        {
                            // empty collection of something
                            if (title.matches("\\w+ win"))
                            {
                                conquerStr = title+conquerStr+" with no units remaining";
                            }
                            else
                            {
                                // empty collection of unhandled objects
                                logWriter.println(indent+title);
                            }
                        }
                    } else if (details instanceof Territory)
                    {
                        // territory details
                        logWriter.println(indent+title);
                    } else if (details == null)
                    {
                        if (title.equals("Adding original owners"))
                        {
                        } else if (title.matches("\\w+ collects? \\d+ ipcs.*"))
                        {
                            logWriter.println(indent+title.replaceAll("collects", "collect"));
                        } else if (title.matches("\\w+ takes? .*? from \\w+"))
                        {
                            // British take Libya from Germans
                            if (moving)
                            {
                                String str = moveList.remove(moveList.size()-1);
                                moveList.add(str+"\n  "+indent+title.replaceAll(" takes ", " take "));
                            } else
                                conquerStr += title.replaceAll("^\\w+ takes ", ", taking ");
                        } else if (title.matches("\\w+ spends? \\d+ on tech rolls"))
                        {
                            logWriter.println(indent+title.replaceAll("spends","spend"));
                        } else if (title.startsWith("Rolls to resolve tech hits:"))
                        {
                        } else if (title.matches("\\w+ discover .*"))
                        {
                            logWriter.println(indent+title);
                        } else if (title.matches("AA raid costs .*"))
                        {
                            logWriter.println(indent+title);
                        } else
                        {
                            // unhandled message with null details
                            logWriter.println(indent+title);
                        }
                    } else
                    {
                        // unknown details object
                        logWriter.println(indent+title);
                    }
                } else if (node instanceof Step)
                {
                    PlayerID playerId = ((Step)node).getPlayerID();
                    if (title.equals("Initializing Delegates"))
                    {
                    } else
                    {
                    logWriter.println();
                    logWriter.print(indent+title);
                    if (playerId != null)
                    {
                        curPlayer = playerId;
                        logWriter.print(" - "+playerId.getName());
                    }
                    logWriter.println();
                    }

                } else if (node instanceof Round)
                {
                    logWriter.println();
                    logWriter.println(indent+title);
                } else if (title.equals("Game History"))
                {
                    logWriter.println(indent+title);
                } else
                {
                    // unknown node type
                    logWriter.println(indent+title);
                }

            } // while (nodeEnum.hasMoreElements())

            curNode = curNode.getNextSibling();
        } while((curNode instanceof Step) && ((Step)curNode).getPlayerID().equals(curPlayer));

        logWriter.println();
    }
}
