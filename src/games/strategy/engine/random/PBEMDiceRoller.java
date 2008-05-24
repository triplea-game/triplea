/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.random;

import games.strategy.util.Util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;

import javax.swing.*;

/**
 * Rolls the dice using http://www.irony.com/mailroll.html
 * 
 * Its a bit messy, but the threads are a pain to deal with We want to be able
 * to call this from any thread, and have a dialog that doesnt close until the
 * dice roll finishes. If there is an error we wait until we get a good roll
 * before returning.
 */
public class PBEMDiceRoller implements IRandomSource
{
    private final String m_player1Email;
    private final String m_player2Email;
    private final String m_gameID;
    private final String m_gameUUID;
    private final IRemoteDiceServer m_remoteDiceServer;

    private static Frame s_focusWindow;

    /*
     * If the game has multiple frames, allows the ui to 
     * set what frame should be the parent of the dice rolling window
     * if set to null, or not set, we try to guess by finding the currently 
     * focused window (or a visble window if none are focused).
     */
    public static void setFocusWindow(Frame w)
    {
	s_focusWindow = w;	    
    }    
    
    public PBEMDiceRoller(String player1Email, String player2Email, String gameID, IRemoteDiceServer diceServer, String gameUUID)
    {
        m_player1Email = player1Email;
        m_player2Email = player2Email;
        m_gameID = gameID;
        m_remoteDiceServer = diceServer;
        m_gameUUID = gameUUID;
    }

    /**
     * Do a test roll, leaving the dialog open after the roll is done.
     */
    public void test()
    {
        HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(getFocusedFrame(), 6, 1, "Test", m_player1Email, m_player2Email, m_gameID, m_remoteDiceServer, "test-roll");
        dialog.setTest();

        dialog.roll();

    }

    /**
     * getRandom
     */
    public int[] getRandom(final int max, final int count, final String annotation)
    {
        
        HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(getFocusedFrame(), max, count, annotation, m_player1Email, m_player2Email, m_gameID, m_remoteDiceServer, m_gameUUID);
        dialog.roll();
        return dialog.getDiceRoll();
    }

    private Frame getFocusedFrame()
    {
        if(s_focusWindow != null)
		return s_focusWindow;
	    
        Frame[] frames = Frame.getFrames();
        Frame rVal = null;
        for (int i = 0; i < frames.length; i++)
        {
            //find the window with focus, failing that, get something that is
            // visible
            if (frames[i].isFocused())
	    {
                rVal = frames[i];
	    }
            else if (rVal == null && frames[i].isVisible())
            {
                rVal = frames[i];
            }
        }

        return rVal;

    }

    /**
     * getRandom
     * 
     * @param max
     *            int
     * @param annotation
     *            String
     * @return int
     */
    public int getRandom(int max, String annotation)
    {
        return getRandom(max, 1, annotation)[0];
    }

}

class HttpDiceRollerDialog extends JDialog
{
    private JButton m_exitButton = new JButton("Exit");

    private JButton m_reRollButton = new JButton("Roll Again");

    private JButton m_okButton = new JButton("OK");

    private JTextArea m_text = new JTextArea();

    private int[] m_diceRoll;

    private final int m_count;

    private final int m_max;

    private final String m_annotation;

    private final String m_email1;

    private final String m_email2;
    private final String m_gameID;
    private final IRemoteDiceServer m_diceServer;
    private final String m_gameUUID;

    private Object m_lock;

    public boolean m_test = false;

    private JPanel m_buttons = new JPanel();
    private final Window m_owner;
    
    public HttpDiceRollerDialog(Frame owner, int max, int count, String annotation, String email1, String email2, String gameID, IRemoteDiceServer diceServer, String gameUUID)
    {
        super(owner, "Dice roller", true);
        m_owner = owner;
        m_max = max;
        m_count = count;
        m_annotation = annotation;
        m_email1 = email1;
        m_email2 = email2;
        m_gameID = gameID;
        m_diceServer = diceServer;
        m_gameUUID = gameUUID;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        m_exitButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                System.exit(-1);
            }
        });

        m_exitButton.setEnabled(false);

        m_reRollButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                rollInternal();
            }
        });

        m_okButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                closeAndReturn();
            }
        });

        m_reRollButton.setEnabled(false);

        getContentPane().setLayout(new BorderLayout());
        m_buttons.add(m_exitButton);
        m_buttons.add(m_reRollButton);
        getContentPane().add(m_buttons, BorderLayout.SOUTH);
        getContentPane().add(new JScrollPane(m_text));
        m_text.setEditable(false);

        setSize(400, 300);
        games.strategy.ui.Util.center(this); // games.strategy.ui.Util

    }

    /**
     * There are three differences when we are testing, 1 dont close the window
     * when we are done 2 remove the exit button 3 add a close button
     */
    public void setTest()
    {
        m_test = true;
        m_buttons.removeAll();
        m_buttons.add(m_okButton);
        m_buttons.add(m_reRollButton);
    }

    public void appendText(String aString)
    {
        m_text.setText(m_text.getText() + aString);
    }

    public void notifyError()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_exitButton.setEnabled(true);
                m_reRollButton.setEnabled(true);                
            }
        
        });
        
    }

    public int[] getDiceRoll()
    {
        return m_diceRoll;
    }

    //should only be called if we are not visible
    //can be called from any thread
    //wont return until the roll is done.
    public void roll()
    {

        //if we are not the event thread, then start again in the event thread
        //pausing this thread until we are done
        if (!SwingUtilities.isEventDispatchThread())
        {
            m_lock = new Object();
            synchronized (m_lock)
            {

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        roll();
                    }
                });

                try
                {
                    m_lock.wait();
                } catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }
            }
            return;
        }

        rollInternal();
        setVisible(true);

    }

    //should be called from the event thread
    private void rollInternal()
    {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");

        m_reRollButton.setEnabled(false);
        m_exitButton.setEnabled(false);

        Thread t = new Thread("Triplea, roll in seperate thread")
        {
            public void run()
            {
                rollInSeperateThread();
            }
        };
        t.start();
    }

    private void closeAndReturn()
    {
        //releast any threads waiting on the lock
        if (m_lock != null)
        {
            synchronized (m_lock)
            {
                m_lock.notifyAll();
            }
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                setVisible(false);
                m_owner.toFront();
            }
        
        });
        
    }

    /**
     * should be called from a thread other than the event thread after we are
     * open (or at least in the process of opening) will close the window and
     * notify any waiting threads when completed succeddfully.
     * 
     * Before contacting Irony Dice Server, check if email has a reasonable
     * valid syntax.
     * 
     * @author George_H
     */
    private void rollInSeperateThread()
    {
        if (SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");

        boolean validMail = false;

        //check the email formats are valid
        validMail = Util.isMailValid(m_email1) && Util.isMailValid(m_email2);
        //make sure the to isnt empty
        validMail &= m_email1.trim().length() > 0;
        

        if (validMail)
        {

            while (!isVisible())
                Thread.yield();

            appendText(m_annotation + "\n");
            appendText("Contacting  " + m_diceServer.getName() + "\n");

            String text = null;
            try
            {
                text = m_diceServer.postRequest(m_email1, m_email2, m_max, m_count, m_annotation, m_gameID, m_gameUUID);

                if (text.length() == 0)
                {
                    appendText("Nothing could be read from dice server\n");
                    appendText("Please check your firewall settings");
                    notifyError();
                }

                if (!m_test)
                    appendText("Contacted :" + text + "\n");
                m_diceRoll = m_diceServer.getDice(text, m_count);
                appendText("Success!");
                if (!m_test)
                    closeAndReturn();
            }
            //an error in networking
            catch (SocketException ex)
            {
                appendText("Connection failure:" + ex.getMessage() + "\n" + "Please ensure your Internet connection is working, and try again.");
                notifyError();
            }
            catch(InvocationTargetException e) 
            {
                appendText("\nError:" + e.getMessage() + "\n\n");
                if (text != null)
                {
                    appendText("Text from dice server:\n" + text + "\n");
                }
                notifyError();
            }
            catch (IOException ex)
            {
                try
                {
                    appendText("An error has occured!\n");
                    appendText("Possible reasons the error could have happened:\n");
                    appendText("  1: An invalid e-mail address\n");
                    appendText("  2: Firewall could be blocking TripleA from connecting to the Dice Server\n");
                    appendText("  3: The e-mail address does not exist\n");
                    appendText("  4: An unknown error, please see the error console and consult the forums for help\n");
                    appendText("     Visit http://tripleadev.org  for extra help\n");

                    if (text != null)
                    {
                        appendText("Text from dice server:\n" + text + "\n");
                    }

                    StringWriter writer = new StringWriter();
                    ex.printStackTrace(new PrintWriter(writer));
                    writer.close();
                    appendText(writer.toString());
                } catch (IOException ex1)
                {
                    ex1.printStackTrace();
                }
                notifyError();

            }
        } else
        { //enter here for invalid e-mail

            appendText("There is an error in the e-mails you have entered\n");
            if(!m_test)
                appendText("and the game cannot proceed.\n");
            appendText("Please check the following:\n");
            appendText("  1: Do you have a valid e-mail syntax ? (ie. someone@someplace.com) ?\n");
            appendText("  2: Are both e-mail boxes filled out ?\n\n");
            if(!m_test)
            {
                appendText("Click Exit, reload the autosave.tsvg game\n");
                appendText("in the savedGames directory, and correct\n");
                appendText("this problem in the PBEM Setup Panel.\n\n");
            }
            notifyError();
        }

    }//end of method
}
