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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RemoteInterfaceHelper
{
    private static final Logger s_logger = Logger.getLogger(RemoteInterfaceHelper.class.getName());
    

    public static int getNumber(String methodName, Class<?>[] argTypes, Class<?> remoteInterface)
    {
        Method[] methods = remoteInterface.getMethods();
        Arrays.sort(methods, methodComparator);

         if(s_logger.isLoggable(Level.FINEST)) {
            s_logger.fine("Sorted methods:" + Arrays.asList(methods));
        } 
    
        for(int i =0; i < methods.length; i++)
        {
            if(methods[i].getName().equals(methodName))
            {
                Class<?>[] types = methods[i].getParameterTypes();
                //both null
                if(types == argTypes)
                {
                    return i;
                }
                else if(types != null && argTypes != null && types.length == argTypes.length)
                {
                    boolean match = true;
                    for(int j = 0; j < argTypes.length; j++)
                    {
                        if(!argTypes[j].equals(types[j]))
                        {
                            match= false;
                            break;
                        }
                    }
                    if(match)
                        return i;
                }
            }
        }
        throw new IllegalStateException("Method not found");
    }
    
    public static Tuple<String, Class<?>[]> getMethodInfo(int methodNumber, Class<?> remoteInterface)
    {
        Method[] methods = remoteInterface.getMethods();
        Arrays.sort(methods, methodComparator);

         if(s_logger.isLoggable(Level.FINEST)) {
            s_logger.fine("Sorted methods:" + Arrays.asList(methods));
        } 
        return new Tuple<String, Class<?>[]>(methods[methodNumber].getName(), methods[methodNumber].getParameterTypes());
    }
    
    
    /**
     * get methods does not guarantee an order, so sort.
     */
    private static Comparator<Method> methodComparator = new Comparator<Method>()
    {

        public int compare(Method o1, Method o2)
        {
            if(o1 == o2)
                return 0;
            
            if(!o1.getName().equals(o2.getName()))
                return o1.getName().compareTo(o2.getName());
            
            
            Class<?>[] t1 = o1.getParameterTypes();
            Class<?>[] t2 = o2.getParameterTypes();
            
            //both null
            if(t1 == t2)
                return 0;
            if(t1 == null)
                return -1;
            if(t2 == null)
                return 1;
            
            if(t1.length != t2.length)
                return t1.length - t2.length;
            for(int i =0; i < t1.length; i++)
            {
                if(!t1[i].getName().equals(t2[i].getName()))
                {
                    return t1[i].getName().compareTo(t2[i].getName());
                }
            }
            return 0;
        }
        
    };
    
}
