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

/*
 * Attatchment.java
 *
 * Created on November 8, 2001, 3:09 PM
 */

package games.strategy.engine.data;



/**
 * Contains some utility methods that subclasses can use to make writing attatchments easier
 *
 * @author  Sean Bridges
 */
public class DefaultAttachment implements IAttachment
{
    
    private GameData m_data;
    private Attachable m_attatchedTo;
    private String m_name;
    
    /**
     * Throws an error if format is invalid.
     */
    protected 	static int getInt(String aString)
    {
        int val = 0;
        try
        {
            val = Integer.parseInt(aString);
        } catch( NumberFormatException nfe)
        {
            throw new IllegalArgumentException(aString + " is not a valid int value");
        }
        return val;
    }
    
    /**
     * Throws an error if format is invalid.  Must be either true or false ignoring case.
     */
    protected static boolean getBool(String aString)
    {
        if(aString.equalsIgnoreCase("true") )
            return true;
        else if(aString.equalsIgnoreCase("false"))
            return false;
        else
            throw new IllegalArgumentException(aString + " is not a valid boolean");
    }
    
    public void setData(GameData data)
    {
        m_data = data;
    }
    
    protected GameData getData()
    {
        return m_data;
    }
    
    /**
     * Called after the attatchment is created.
     */
    public void validate() throws GameParseException
    {
    }
    
    public Attachable getAttatchedTo()
    {
        return m_attatchedTo;
    }
    
    public void setAttatchedTo(Attachable attatchable)
    {
        m_attatchedTo = attatchable;
    }
    
    
    /** Creates new Attatchment */
    public DefaultAttachment()
    {
        
    }
    public String getName()
    {
        return m_name;
    }
    
    public void setName(String aString)
    {
        m_name = aString;
    }
    
    
    public String toString()
    {
        return getClass().getSimpleName() + " attched to:" + m_attatchedTo + " with name:" + m_name;
    }
    
    
}
