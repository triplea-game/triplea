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
import java.util.concurrent.CountDownLatch;

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
class BattleStepsPanel extends JPanel implements Active
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
    
    private final List<CountDownLatch> m_waiters = new ArrayList<CountDownLatch>();
    private boolean m_hasWalkThread = false;

    BattleStepsPanel()
    {
        setLayout(new BorderLayout());
        add(m_list, BorderLayout.CENTER);
        m_list.setBackground(this.getBackground());
        m_list.setSelectionModel(m_listSelectionModel);
    }

    public void deactivate()
    {
        wakeAll();
    }
    
    private void wakeAll()
    {
       synchronized(m_mutex)
       {
           for(CountDownLatch l : m_waiters)
           {
               l.countDown();
           }
           m_waiters.clear();
       }
        
    }

    /**
     * Set the steps given, setting the selected step to the first step.
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
        }

        validate();
    }

    private void clearTargetStep()
    {
        synchronized(m_mutex)
        {
            m_targetStep = null;
        }
        wakeAll();
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
                return true;
            }

            //we found it, we are done
            if (m_targetStep.equals(m_list.getSelectedValue()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks through and pause at each list item until we find our target.
     */
    private void walkStep()
    {
        if(!SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");

        if (doneWalkingSteps())
        {
            wakeAll();
            return;
        }

        int index = m_list.getSelectedIndex() + 1;
        if (index >= m_list.getModel().getSize())
            index = 0;
        m_listSelectionModel.hiddenSetSelectionInterval(index);

       waitThenWalk();
        
        
    }

    private void waitThenWalk()
    {
        Thread t = new Thread("Walk single step started at:" + new Date())
        {
            public void run()
            {
                synchronized(m_mutex)
                {
                    if(m_hasWalkThread)
                        return;
                    m_hasWalkThread = true;
                }
                try
                {
                
                    try
                    {
                        sleep(330);
                    } catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    SwingUtilities.invokeLater(new Runnable()
                    {
                    
                        public void run()
                        {
                            walkStep();
                        }
                    });
                } finally
                {
                    synchronized (m_mutex)
                    {
                        m_hasWalkThread = false;
                    }
                }
                
            }
        };
        t.start();
        
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

    private void goToTarget()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            CountDownLatch latch = new CountDownLatch(1);
            synchronized(m_mutex)
            {
                m_waiters.add(latch);
            }
            waitThenWalk();
            try
            {
                latch.await();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }            
        }
        waitThenWalk();
        
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