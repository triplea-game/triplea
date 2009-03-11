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

package games.strategy.ui;

import java.awt.*;
import javax.swing.*;


/**
 * Utility to check that code that should be running in the swing event thread runs in the swing event thread.
 * 
 * @author Sean Bridges
 *
 */
public class SwingEventThreadGoon
{
    private static boolean s_isInitialized;
    
    
    public static synchronized void initialize()
    {
        if(s_isInitialized)
            return;
        s_isInitialized = true;
        
        RepaintManager manager = new RepaintManager()
        {
            /**
             * We are updating a portion of the screen, check to see if we are in the right thread.
             */
            public void addDirtyRegion(JComponent c, int x, int y, int w, int h)
            {
                //if a component is not displayable, then we can modify
                //if outside the swing event thread. 
                if(isComponentDisplayable(c))
                {
                    if(!SwingUtilities.isEventDispatchThread())
                    {   
                        Thread.dumpStack();
                    }
                }
                
                super.addDirtyRegion(c, x, y, w, h);
            } 
            
        
        };
        
        //update the repaint manager
        RepaintManager.setCurrentManager(manager);
        
    }
    
    /**
     * Once a component is added to a displayed heirarchy, all changes to the component
     * should be done in the swing event thread.
     * 
     *  Check the component (and its parent recursivly) till you get to the top 
     *  window (or null).  If the window is displayable, then the component is displayable.
     */
    private static boolean isComponentDisplayable(Component c)
    {
        //we are not visible
        if(!c.isVisible())
            return false;

        //if we have no parent, we are not part of a display hierarchy, and
        //we are not displayable
        Container parent = c.getParent();
        if(parent == null)
            return false;
        
        //is our window displayable?
        if(parent instanceof Window)
        {
            Window w = (Window) parent;
            
            return w.isDisplayable();
        }
        
        //recursivly check our parent
        return isComponentDisplayable((Component) parent); 
    }
    
    
    
}
