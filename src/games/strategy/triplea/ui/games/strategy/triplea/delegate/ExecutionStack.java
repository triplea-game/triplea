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

package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegateBridge;

import java.io.Serializable;
import java.util.*;

/**
 * Utilility for tracking a sequence of executables.  
 *
 * It works like this,
 * 
 * We pop the top of the stack, store it in current, then execute it.
 * While exececuting the current element, the current element can push
 * more execution items onto the stack.<p>
 * 
 * After execution has finished, we pop the next item, and execute it, repeating 
 * till nothing is left to exectue.<p>
 * 
 * If an exception occurs during execution, we retain a reference to 
 * the current item.  When we start executing again, we first push current onto the stack.
 * In this way, an item may execute more than once. An IExecutable should be aware of this.<p>
 * 
 */
public class ExecutionStack implements Serializable
{
    private IExecutable m_current;
    private final Stack<IExecutable> m_stack = new Stack<IExecutable>();
    
    public void execute(IDelegateBridge bridge, GameData data)
    {
        //we were interrupted before, resume where we left off
        if(m_current != null)
            m_stack.push(m_current);
        
        while(!m_stack.isEmpty())
        {
            m_current =  m_stack.pop();
            m_current.execute(this, bridge, data);
        }
        m_current = null;
    }
    
    public void push(Collection<IExecutable> executables)
    {
        for(IExecutable ex : executables)
        {
            push(ex);
        }
        
    }
    
    public void push(IExecutable executable)
    {
        m_stack.push(executable);
    }
    
    public boolean isExecuting()
    {
        return m_current != null;
    }
    
    public boolean isEmpty()
    {
        return m_stack.isEmpty();
    }
    
}
