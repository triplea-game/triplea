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

package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.util.*;

import javax.swing.*;

/**
 * A panel for showing the battle steps in a display.
 * 
 * Contains code for walking from the current step, to a given step
 * there is a delay while we walk so that the user can see the steps progression.
 * 
 * Users of this class should deactive it after they are done.
 *
 * @author Sean Bridges
 */
class BattleStepsPanel extends JPanel
{
    //if this is the target step, we want to walk to the last step
    private final static String LAST_STEP = "NULL MARKER FOR LAST STEP";

    private final DefaultListModel m_listModel = new DefaultListModel();
    private final JList m_list = new JList(m_listModel);
    private final MyListSelectionModel m_listSelectionModel = new MyListSelectionModel();

    //the step we want to reach
    private String m_targetStep = null;
    
    //all changes to state should be done while locked on this object.
    //when we reach the target step, or when we want to walk the step
    //notifyAll on this object
    private final Object m_mutex = new Object();
    
    private boolean m_deactivated = false;

    //do all walking in a single thread
    private final Thread m_walkThread = new Thread("Triplea Battle steps panel step walking thread")
    {
        public void run()
        {
            while (!m_deactivated)
            {
                //we will be notified if we need to start walking
                synchronized (m_mutex)
                {
                    try
                    {
                        m_mutex.wait();
                    } catch (InterruptedException e)
                    {
                    }
                }

                while (!doneWalkingSteps() && !m_deactivated)
                    walkStep();

            }
        }
    };

    BattleStepsPanel()
    {
        setLayout(new BorderLayout());
        add(m_list, BorderLayout.CENTER);
        m_list.setBackground(this.getBackground());
        m_list.setSelectionModel(m_listSelectionModel);
        m_walkThread.setDaemon(true);
        m_walkThread.start();
    }

    public void deactivate()
    {
        synchronized(m_mutex)
        {
            m_deactivated = true;
            m_mutex.notifyAll();
        }
    }
    
    /**
     * Set the steps given, walking to the first step.
     * This method blocks until the first step is reached, unless 
     * this method is called from the swing event thread.
     *   
     * @param steps
     */
    public void listBattle(List steps)
    {
        if (!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Not in dispatch thread");

        synchronized (m_mutex)
        {
            m_listModel.removeAllElements();

            Iterator iter = steps.iterator();
            while (iter.hasNext())
            {
                m_listModel.addElement(iter.next());
            }
            m_listSelectionModel.hiddenSetSelectionInterval(0);
            m_targetStep = (String) steps.get(steps.size() - 1);
        }

        validate();
        goToTarget();
    }

    private void clearTargetStep()
    {
        m_targetStep = null;
        m_mutex.notifyAll();
    }

    private boolean doneWalkingSteps()
    {
        synchronized (m_mutex)
        {
            //not looking for anything
            if (m_targetStep == null)
                return true;

            //we cant find it, something is wrong
            if (m_targetStep != LAST_STEP && m_listModel.lastIndexOf(m_targetStep) == -1)
            {
                new IllegalStateException("Step not found:" + m_targetStep + " in:" + m_listModel).printStackTrace();
                clearTargetStep();
                return true;
            }

            //at end, we are done
            if (m_targetStep == LAST_STEP && m_list.getSelectedIndex() == m_listModel.getSize() - 1)
            {
                clearTargetStep();
                return true;
            }

            //we found it, we are done
            if (m_targetStep.equals(m_list.getSelectedValue()))
            {
                clearTargetStep();
                return true;
            }
        }
        return false;
    }

    private void goToTarget()
    {
        boolean wait = !SwingUtilities.isEventDispatchThread();

        synchronized (m_mutex)
        {
            //signal that we want to walk
            m_mutex.notifyAll();

            if (!wait)
                return;

            try
            {
                m_mutex.wait(2000);
            } catch (InterruptedException e)
            {
                return;
            }
            if (!doneWalkingSteps())
                goToTarget();
        }
    }

    /**
     * Walks through and pause at each list item until we find our target.
     */
    private void walkStep()
    {

        //we want to run in the swing event thread
        Runnable advanceStep = new Runnable()
        {
            public void run()
            {
                synchronized (m_mutex)
                {
                    if (doneWalkingSteps())
                        return;

                    int index = m_list.getSelectedIndex() + 1;
                    if (index >= m_list.getModel().getSize())
                        index = 0;

                    m_listSelectionModel.hiddenSetSelectionInterval(index);
                }
            }
        };

        try
        {
            SwingUtilities.invokeAndWait(advanceStep);
            //pause to allow the user to follow
            Thread.sleep(300);
        } catch (InterruptedException ie)
        {
            ie.printStackTrace();
        } catch (java.lang.reflect.InvocationTargetException ioe)
        {
            ioe.printStackTrace();
            throw new RuntimeException(ioe.getMessage());
        }

    }

    /**
     * This method blocks until the last step is reached, unless 
     * this method is called from the swing event thread.
     *
     */
    public void walkToLastStep()
    {
        synchronized (m_mutex)
        {
            m_targetStep = LAST_STEP;
        }
        goToTarget();
    }

    public String getStep()
    {
        synchronized (m_mutex)
        {
            return (String) m_list.getSelectedValue();
        }
    }

    /**
    * This method blocks until the step is reached, unless 
    * this method is called from the swing event thread.
    */
    public void setStep(String step)
    {
        synchronized (m_mutex)
        {
            m_targetStep = step;
        }
        goToTarget();

    }
    
    public void finalize()
    {
        m_deactivated = true;
    }
}


/**
 * Doesnt allow the user to change the selection, must be done through
 * hiddenSetSelectionInterval.
 */
class MyListSelectionModel extends DefaultListSelectionModel
{

    public void setSelectionInterval(int index0, int index1)
    {

    }

    public void hiddenSetSelectionInterval(int index)
    {

        super.setSelectionInterval(index, index);
    }
}