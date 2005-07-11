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

package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * All the info neccassary to describe a method call in one handy
 * serializable package.
 * 
 */
public class RemoteMethodCall implements Externalizable
{
    private String m_remoteName;
    private String m_methodName;
    private Object[] m_args;

    //stored as a String[] so we can be serialzed
    private String[] m_argTypes;

    public RemoteMethodCall()
    {
    	
    }
    
    public RemoteMethodCall(final String remoteName, final String methodName,
            final Object[] args, final Class[] argTypes)
    {
        m_remoteName = remoteName;
        m_methodName = methodName;
        m_args = args;
        m_argTypes = classesToString(argTypes);
    }

    /**
     * @return Returns the channelName.
     */
    public String getRemoteName()
    {
        return m_remoteName;
    }

    /**
     * @return Returns the methodName.
     */
    public String getMethodName()
    {
        return m_methodName;
    }

    /**
     * @return Returns the args.
     */
    public Object[] getArgs()
    {
        return m_args;
    }

    /**
     * @return Returns the argTypes.
     */
    public Class[] getArgTypes()
    {
        return stringsToClasses(m_argTypes);
    }

    public static Class[] stringsToClasses(String[] strings)
    {
        Class[] rVal = new Class[strings.length];
        for (int i = 0; i < strings.length; i++)
        {
            try
            {
                //handle primitives
                if (strings[i].equals("int"))
                {
                    rVal[i] = Integer.TYPE;
                } else if (strings[i].equals("short"))
                {
                    rVal[i] = Short.TYPE;
                } else if (strings[i].equals("byte"))
                {
                    rVal[i] = Byte.TYPE;
                } else if (strings[i].equals("long"))
                {
                    rVal[i] = Long.TYPE;
                } else if (strings[i].equals("float"))
                {
                    rVal[i] = Float.TYPE;
                } else if (strings[i].equals("boolean"))
                {
                    rVal[i] = Boolean.TYPE;
                }
                //handle everything else
                //why is everything else so much easier than primitives
                else
                {
                    rVal[i] = Class.forName(strings[i]);
                }
            } catch (ClassNotFoundException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }
        }
        return rVal;
    }
    
    public static String[] classesToString(Class[] classes)
    {
        String[] rVal = new String[classes.length];
        for(int i = 0; i < classes.length; i++)
        {
            rVal[i] = classes[i].getName();
        }
        return rVal;
    }
    
    public String toString()
    {
        return "Remote method call:" + m_methodName + " on:" + m_remoteName;
    }

	public void writeExternal(ObjectOutput out) throws IOException 
	{
		out.writeObject(m_remoteName);
		out.writeObject(m_methodName);
		out.writeObject(m_args);
		out.writeObject(m_argTypes);
		
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException 
	{
		m_remoteName = (String) in.readObject();
		m_methodName = (String) in.readObject();
		m_args = (Object[]) in.readObject();
		m_argTypes = (String[]) in.readObject();
	}
}