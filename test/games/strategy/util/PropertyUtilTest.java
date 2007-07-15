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

package games.strategy.util;

import javax.swing.JLabel;

import junit.framework.TestCase;

public class PropertyUtilTest extends TestCase
{

    
    public void testGet() 
    {
        JLabel label = new JLabel("TestCase");
        
        assertEquals("TestCase", PropertyUtil.get("text", label));
    }
    
    public void testSet()
    {
        JLabel label = new JLabel("TestCase");
        PropertyUtil.set("text", "changed", label);
        assertEquals("changed", PropertyUtil.get("text", label));
    }
    
    public void testSetInt()
    {
        
        JLabel label = new JLabel();
        PropertyUtil.set("IconTextGap",10, label);
        PropertyUtil.get("IconTextGap",label);
    }
}
