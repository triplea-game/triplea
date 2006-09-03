package games.strategy.triplea.util;

import java.lang.reflect.Proxy;

import junit.framework.TestCase;

public class WrappedInvocationHandlerTest extends TestCase
{

    public void testEquals()
    {
        String s1 = "test";
        WrappedInvocationHandler handler = new WrappedInvocationHandler(s1);
        Object o1 = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {}, handler);
        
        assertEquals(o1.hashCode(), s1.hashCode());
        
        
        
        String s2 = new String("test");
        WrappedInvocationHandler handler2 = new WrappedInvocationHandler(s2);
        Object o2 = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {}, handler2);

        
        assertEquals(o1,o2);
        
    }
}
