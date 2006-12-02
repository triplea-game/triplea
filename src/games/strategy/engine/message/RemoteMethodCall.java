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

import games.strategy.util.Tuple;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All the info neccassary to describe a method call in one handy
 * serializable package.
 * 
 */
public class RemoteMethodCall implements Externalizable
{
    private static final Logger s_logger = Logger.getLogger(RemoteMethodCall.class.getName());
    
    private String m_remoteName;
    private String m_methodName;
    private Object[] m_args;
    
    //to save space, we dont serialize method name/types
    //instead we just serialize a number which can be transalted into
    //the correct method.
    private int m_methodNumber;

    //stored as a String[] so we can be serialzed
    private String[] m_argTypes;

    public RemoteMethodCall()
    {}
    
    public RemoteMethodCall(final String remoteName, final String methodName,
            final Object[] args, final Class[] argTypes, Class remoteInterface)
    {
        if(argTypes == null)
            throw new IllegalArgumentException("ArgTypes are null");
        if(args == null && argTypes.length != 0)
            throw new IllegalArgumentException("args but no types");
        if(args != null && args.length != argTypes.length)
            throw new IllegalArgumentException("Arg and arg type lengths dont match");


        
        m_remoteName = remoteName;
        m_methodName = methodName;
        m_args = args;
        m_argTypes = classesToString(argTypes, args);
        m_methodNumber = RemoteInterfaceHelper.getNumber(methodName, argTypes, remoteInterface);

        if(s_logger.isLoggable(Level.FINE)) {
            s_logger.fine("Remote Method Call:" + debugMethodText());
        }

    }
    
    private String debugMethodText() {
	if(m_argTypes == null) 
	        return "." + m_methodName + "(" + ")";
	else
	        return "." + m_methodName + "(" + Arrays.asList(m_argTypes) + ")";
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
        return stringsToClasses(m_argTypes, m_args);
    }

    public static Class[] stringsToClasses(String[] strings, Object[] args)
    {
        Class[] rVal = new Class[strings.length];
        for (int i = 0; i < strings.length; i++)
        {
            try
            {
                //null if we skipped writing because the arg is the expected 
                //class, this saves some space since generally the arg will
                //be of the correct type
                if(strings[i] == null)
                {
                    rVal[i] = args[i].getClass();
                }
                //handle primitives
                else if (strings[i].equals("int"))
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
                }
                else if (strings[i].equals("double"))
                {
                    rVal[i] = Double.TYPE;
                }
                else if (strings[i].equals("boolean"))
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
    
    public static String[] classesToString(Class[] classes, Object[] args)
    {
        //as an optimization, if args[i].getClass == classes[i] then leave classes[i] as null
        //this will reduce the amount of info we write over the network in the common
        //case where the object is the same type as its arg
        
        String[] rVal = new String[classes.length];
        for(int i = 0; i < classes.length; i++)
        {
            if(args != null && args[i] != null && classes[i] == args[i].getClass())
            {
                rVal[i] = null;
            }
            else
            {
                rVal[i] = classes[i].getName();    
            }
                
           
            
        }
        return rVal;
    }
    
    public String toString()
    {
        return "Remote method call:" + m_methodName + " on:" + m_remoteName;
    }

	public void writeExternal(ObjectOutput out) throws IOException 
	{
        out.writeUTF(m_remoteName);
        out.writeByte(m_methodNumber);
        if(m_args == null)
        {
            out.writeByte(Byte.MAX_VALUE);;
        }
        else
        {
            out.writeByte(m_args.length);
            for(int i =0; i < m_args.length; i++)
            {
                out.writeObject(m_args[i]);
            }            
        }
            
            
        
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException 
	{
        m_remoteName = in.readUTF();
        m_methodNumber = in.readByte();
        
        byte count = in.readByte();
        
        if(count != Byte.MAX_VALUE)
        {
            
            m_args = new Object[count];
            
            for(int i =0; i < count; i++)
            {
                m_args[i] = in.readObject();
            }
        }
	}
    
    /**
     * After we have been de-serialized, we do not transmit enough
     * informatin to determine the method without being told
     * what class we operate on.
     */
    public void resolve(Class remoteType)
    {
        if(m_methodName != null)
            return;
        
        Tuple<String,Class[]> values = RemoteInterfaceHelper.getMethodInfo(m_methodNumber, remoteType);
        m_methodName = values.getFirst();
        m_argTypes = classesToString(values.getSecond(), m_args);
        

        if(s_logger.isLoggable(Level.FINE)) {
            s_logger.fine("Remote Method for class:" + remoteType.getSimpleName() + " Resolved To:" + debugMethodText());
        }
    }
}
