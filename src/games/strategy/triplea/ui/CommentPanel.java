package games.strategy.triplea.ui;
/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * CommentPanel.java Swing ui for comment logging.
 * 
 * Created on September 24, 2007
 */

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.ui.TripleAFrame;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 * A Comment logging window.  
 * 
 * @author Tony Clayton
 */
public class CommentPanel extends JPanel
{
    private static final int MAX_LINES = 5000;
    private JTextPane m_text;
    private JScrollPane m_scrollPane;
    private JTextField m_nextMessage;
    
    private JButton m_save;

    private GameData m_data;
    private TripleAFrame m_frame;
    private Map<PlayerID, Icon> m_iconMap;


    private final SimpleAttributeSet bold = new SimpleAttributeSet();
    private final SimpleAttributeSet italic = new SimpleAttributeSet();
    private final SimpleAttributeSet normal = new SimpleAttributeSet();
 
    public CommentPanel(TripleAFrame frame, GameData data)
    {
        m_frame = frame;
        m_data = data;
        init();
    }
    
    private void init()
    {
        createComponents();
        layoutComponents();
        setupKeyMap();

        StyleConstants.setBold(bold, true);
        StyleConstants.setItalic(italic, true);
        setSize(300, 200);

        loadHistory();
        setupListeners();
    }

    private void layoutComponents()
    {

        Container content = this;
        content.setLayout(new BorderLayout());
        m_scrollPane = new JScrollPane(m_text);
        
        content.add(m_scrollPane, BorderLayout.CENTER);

        
        content.add(m_scrollPane, BorderLayout.CENTER);

        JPanel savePanel = new JPanel();
        savePanel.setLayout(new BorderLayout());
        savePanel.add(m_nextMessage, BorderLayout.CENTER);
        savePanel.add(m_save, BorderLayout.WEST);

        content.add(savePanel, BorderLayout.SOUTH);
    }

    private void createComponents()
    {

        m_text = new JTextPane();
        m_text.setEditable(false);
        m_text.setFocusable(false);

        m_nextMessage = new JTextField(10);
        //when enter is pressed, send the message
        
        Insets inset = new Insets(3, 3, 3, 3);
        m_save = new JButton(m_saveAction);
        m_save.setMargin(inset);
        m_save.setFocusable(false);

        // create icon map
        m_iconMap = new HashMap<PlayerID, Icon>();
        for (PlayerID playerId : m_data.getPlayerList().getPlayers())
        {
            m_iconMap.put(playerId, new ImageIcon(m_frame.getUIContext().getFlagImageFactory().getSmallFlag( playerId )));
        }
        
    }

    private void setupListeners()
    {
        m_data.getHistory().addTreeModelListener(new TreeModelListener()
        {
            public void treeNodesChanged(TreeModelEvent e)
            {
            }
            public void treeNodesInserted(TreeModelEvent e)
            {
            }
            public void treeNodesRemoved(TreeModelEvent e)
            {
            }
            public void treeStructureChanged(TreeModelEvent e)
            {

                final TreeModelEvent tme = e;
                Runnable runner = new Runnable()
                {
                    public void run()
                    {
                        m_data.acquireReadLock();
                        try
                        {
                            Document doc = m_text.getDocument();

                            HistoryNode node = (HistoryNode)(tme.getTreePath().getLastPathComponent());
                            String title = node.getTitle();

                            Pattern p = Pattern.compile("^COMMENT: (.*)");
                            Matcher m = p.matcher(title);
                            if(m.matches())
                            {
                                PlayerID playerId = m_data.getSequence().getStep().getPlayerID();
                                int round = m_data.getSequence().getRound();
                                String player = playerId.getName();
                                Icon icon = m_iconMap.get(playerId);
                                try
                                {
                                    //insert into ui document
                                    String prefix = " " + player + "("+round+") : ";
                                    m_text.insertIcon(icon);
                                    doc.insertString(doc.getLength(), prefix, bold);
                                    doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
                                } 
                                catch (BadLocationException ble)
                                {
                                    ble.printStackTrace();
                                }
                            }
                        }
                        finally
                        {
                            m_data.releaseReadLock();
                        }
                    }
                };
                //invoke in the swing event thread
                if (SwingUtilities.isEventDispatchThread())
                    runner.run();
                else
                    SwingUtilities.invokeLater(runner);
            }
        });

    }


    private void setupKeyMap()
    {
        InputMap nextMessageKeymap = m_nextMessage.getInputMap();
        nextMessageKeymap.put(KeyStroke.getKeyStroke('\n'), m_saveAction);
    }
    

    private void cleanupKeyMap()
    {
        InputMap nextMessageKeymap = m_nextMessage.getInputMap();
        nextMessageKeymap.remove(KeyStroke.getKeyStroke('\n') );
    }
    
    private void loadHistory()
    {
        Document doc = m_text.getDocument();
        HistoryNode rootNode = (HistoryNode) m_data.getHistory().getRoot();

        Enumeration nodeEnum = rootNode.preorderEnumeration();
        Pattern p = Pattern.compile("^COMMENT: (.*)");
        String player = "";
        int round = 0;
        Icon icon = null;
        String message;
        while (nodeEnum.hasMoreElements())
        {
            HistoryNode node = (HistoryNode)nodeEnum.nextElement();

            if (node instanceof Round)
            {
                round++;
                continue;
            }
            else if (node instanceof Step)
            {
                PlayerID playerId = ((Step)node).getPlayerID();
                if (playerId != null)
                {
                    player = playerId.getName();
                    icon = m_iconMap.get(playerId);
                }
                continue;
            }
            else
                //if ((node instanceof Event) || (node instanceof EventChild))
            {
                String title = node.getTitle();
                Matcher m = p.matcher(title);
                if(m.matches())
                {
                    try
                    {
                        //insert into ui document
                        String prefix = " " + player + "("+round+") : ";
                        m_text.insertIcon(icon);
                        doc.insertString(doc.getLength(), prefix, bold);
                        doc.insertString(doc.getLength(), m.group(1) + "\n", normal);
                    } catch (BadLocationException ble)
                    {
                        ble.printStackTrace();
                    }
                 
                }
            }
        }
    }
    

    /** thread safe */
    public void addMessage(final String message)
    {
        Runnable runner = new Runnable()
        {
            public void run()
            {
                
                try
                {
                    Document doc = m_text.getDocument();

                    //save history entry
                    IEditDelegate delegate = m_frame.getEditDelegate();
                    String error;
                    if (delegate == null)
                        error = "You can only add comments during your turn";
                    else
                        error = delegate.addComment(message);

                    if (error != null)
                    {
                        doc.insertString(doc.getLength(), error + "\n", italic);
                    }

                } catch (BadLocationException ble)
                {
                    ble.printStackTrace();
                } 


             
                BoundedRangeModel scrollModel = m_scrollPane.getVerticalScrollBar().getModel();
                scrollModel.setValue(scrollModel.getMaximum());
            }

           
        };

        //invoke in the swing event thread
        if (SwingUtilities.isEventDispatchThread())
            runner.run();
        else
            SwingUtilities.invokeLater(runner);
    }
    

    /**
     * Show only the first n lines
     */
    public static void trimLines(Document doc, int lineCount)
    {
        if(doc.getLength() < lineCount)
            return;
        
        try
        {
            String text = doc.getText(0, doc.getLength());
            int returnsFound = 0;
            
            for(int i = text.length() - 1; i >= 0; i--)
            {
                if(text.charAt(i) == '\n')
                {
                    returnsFound ++;
                }
                if(returnsFound == lineCount)
                {
                    doc.remove(0, i);
                    return;
                }
                
            }
        } catch (BadLocationException e)
        {
            e.printStackTrace();
        }
        
        
        
        
    }

    private String trimMessage(String originalMessage)
    {
        //dont allow messages that are too long
        if(originalMessage.length() > 200)
        {
            return originalMessage.substring(0, 199) + "...";
        }
        else
        {
            return originalMessage;
        }
        
    }
    
    private Action m_saveAction = new AbstractAction("Add Comment")
    {

        public void actionPerformed(ActionEvent e)
        {
            if (m_nextMessage.getText().trim().length() == 0)
                return;
         
            
	    addMessage(m_nextMessage.getText());
            m_nextMessage.setText("");
        }
    };
    /*
    } catch (InvalidMoveException ime) {
        throw new MessengerException(ime.getMessage(), ime);
    }
    */
    
}

