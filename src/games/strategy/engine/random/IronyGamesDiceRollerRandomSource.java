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

import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;


/**
 * Rolls the dice using http://www.irony.com/mailroll.html
 *
 * Its a bit messy, but the threads are a pain to deal with
 * We want to be able to call this from any thread, and have a dialog that
 * doesnt close until the dice roll finishes.  If there is an error we
 * wait until we get a good roll before returning.
 */
public class IronyGamesDiceRollerRandomSource implements IRandomSource
{
  private final String m_player1Email;
  private final String m_player2Email;
  private transient Component m_ui;

  public IronyGamesDiceRollerRandomSource(String player1Email, String player2Email)
  {
    m_player1Email = player1Email;
    m_player2Email = player2Email;
  }

  /**
   * Do a test roll, leaving the dialog open after the roll is done.
   */
  public void test()
  {
    HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(JOptionPane.getFrameForComponent(m_ui), 1, "Test", m_player1Email, m_player2Email);
    dialog.setTest();

    dialog.roll();

  }

  public void setUI(Component component)
  {
    m_ui = component;
  }

  /**
   * getRandom
   */
  public int[] getRandom(final int max, final int count, final String annotation)
  {
    if (m_ui == null)
      throw new IllegalStateException("No ui");

    HttpDiceRollerDialog dialog = new HttpDiceRollerDialog(JOptionPane.getFrameForComponent(m_ui), count, annotation, m_player1Email, m_player2Email);
    dialog.roll();
    return dialog.getDiceRoll();
  }

  /**
   * getRandom
   *
   * @param max int
   * @param annotation String
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
  private final String m_annotation;
  private final String m_email1;
  private final String m_email2;
  private Object m_lock;
  public boolean m_test = false;
  private JPanel m_buttons = new JPanel();

  public HttpDiceRollerDialog(Frame owner, int count, String annotation, String email1, String email2)
  {
    super(owner, "Dice roller", true);
    m_count = count;
    m_annotation = annotation;
    m_email1 = email1;
    m_email2 = email2;

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    m_exitButton.addActionListener(
      new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        System.exit( -1);
      }
    }
    );

    m_exitButton.setEnabled(false);

    m_reRollButton.addActionListener(
      new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        rollInternal();
      }
    }
    );

    m_okButton.addActionListener(
      new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        closeAndReturn();
      }
    }
    );



    m_reRollButton.setEnabled(false);

    getContentPane().setLayout(new BorderLayout());
    m_buttons.add(m_exitButton);
    m_buttons.add(m_reRollButton);
    getContentPane().add(m_buttons, BorderLayout.SOUTH);
    getContentPane().add(new JScrollPane(m_text));
    m_text.setEditable(false);

    setSize(400, 300);
  }

  /**
   * There are three differences when we are testing,
   * 1 dont close the window when we are done
   * 2 remove the exit button
   * 3 add a close button
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
    m_exitButton.setEnabled(true);
    m_reRollButton.setEnabled(true);
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
    m_reRollButton.setEnabled(false);
    m_exitButton.setEnabled(false);


    //if we are not the event thread, then start again in the event thread
    //pausing this thread until we are done
    if (!SwingUtilities.isEventDispatchThread())
    {
      m_lock = new Object();
      synchronized (m_lock)
      {

        SwingUtilities.invokeLater
          (new Runnable()
        {
          public void run()
          {
            roll();
          }
        }
        );

        try
        {
          m_lock.wait();
        }
        catch (InterruptedException ie)
        {
          ie.printStackTrace();
        }
      }
      return;
    }


    rollInternal();
    show();

  }

  //should be called from the event thread
  private void rollInternal()
  {
    if(!SwingUtilities.isEventDispatchThread())
      throw new IllegalStateException("Wrong thread");

    Thread t = new Thread()
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
    setVisible(false);
  }


  //should be called from a thread other than the event thread after we are open
  //(or at least in the process of opening)
  //will close the window and notify any waiting threads when
  //completed succeddfully.
  private void rollInSeperateThread()
  {
    if(SwingUtilities.isEventDispatchThread())
      throw new IllegalStateException("Wrong thread");


    while(! isVisible())
      Thread.currentThread().yield();

    appendText(m_annotation + "\n");
    appendText("Contacting  http://www.irony.com/mailroll.html...\n");

    try
    {
      String text = DiceStatic.postRequest(m_email1, m_email2, m_count, m_annotation);
      if(!m_test)
        appendText("Contacted :" + text + "\n");
      m_diceRoll = DiceStatic.getDice(text, m_count);
      appendText("Success!");
      if(!m_test)
        closeAndReturn();
    }
    catch (IOException ex)
    {
      try
      {
        appendText("Failure!  Did you enter valid email addresses?\n");
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        writer.close();
        appendText(writer.toString());
        notifyError();
      }
      catch (IOException ex1)
      {
        ex1.printStackTrace();
      }

    }

  }

}

class DiceStatic
{

  public static String postRequest(String player1, String player2, int numDice, String text) throws IOException
  {
    URL url = new URL("http://www.irony.com/cgi-bin/mroll-query");
    URLConnection urlConn = url.openConnection();
    urlConn.setDoInput(true);
    urlConn.setDoOutput(true);
    urlConn.setUseCaches(false);
    urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

    DataOutputStream out = new DataOutputStream(urlConn.getOutputStream());

    String content =
      "numdice=" + URLEncoder.encode(numDice + "", "UTF-8") +
      "&numsides=" + URLEncoder.encode("6", "UTF-8") +
      "&modroll=" + URLEncoder.encode("No", "UTF-8") +
      //how many times to repeat
      "&numroll=" + URLEncoder.encode("1", "UTF-8") +

      "&subject=" + URLEncoder.encode("TripleA:" + text, "UTF-8") +
      "&roller=" + URLEncoder.encode(player1, "UTF-8") +
      "&gm=" + URLEncoder.encode(player2, "UTF-8");

    out.writeBytes(content);
    out.flush();
    out.close();

    BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
    try
    {
      StringBuffer results = new StringBuffer();

      while (input.ready())
      {
        results.append(input.readLine());
      }
      return results.toString();
    }
    finally
    {
      try
      {
        input.close();
      }
      catch (Exception e)
      {

      }
    }
  }

  /**
   *
   * @throws IOException if there was an error parsing the string
   */
  public static int[] getDice(String string, int count) throws IOException
  {
    String rollStartString;
    String rollEndString;
    if (count == 1)
    {
      rollStartString = "Roll 1: <b>";
      rollEndString = "</b>";
    }
    else
    {
      rollStartString = "<p>Roll 1:";
      rollEndString = "=";
    }

    int startIndex = string.indexOf(rollStartString);
    if (startIndex == -1)
    {
      throw new IOException("Cound not find start index");

    }
    startIndex += rollStartString.length();

    int endIndex = string.indexOf(rollEndString, startIndex);
    if (endIndex == -1)
    {
      throw new IOException("Cound not find end index");
    }

    StringTokenizer tokenizer = new StringTokenizer(string.substring(startIndex, endIndex), " ,", false);

    int[] rVal = new int[count];
    for (int i = 0; i < count; i++)
    {
      try
      {
        //-1 since we are 0 based
        rVal[i] = Integer.parseInt(tokenizer.nextToken()) - 1;
      }
      catch (NumberFormatException ex)
      {
        ex.printStackTrace();
        throw new IOException(ex.getMessage());
      }
    }

    return rVal;
  }

}
