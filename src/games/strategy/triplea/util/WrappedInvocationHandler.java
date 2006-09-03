package games.strategy.triplea.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class WrappedInvocationHandler implements InvocationHandler 
{
    private final Object m_delegate;

    public WrappedInvocationHandler(final Object delegate)
    {
        if(delegate == null)
            throw new IllegalArgumentException("delegate cant be null");
        m_delegate = delegate;
    }

    
    public boolean wrappedEquals(Object other)
    {
        if(other == this)
            return true;
        if(Proxy.isProxyClass(other.getClass()) && Proxy.getInvocationHandler(other) instanceof WrappedInvocationHandler)
        {
            WrappedInvocationHandler otherWrapped = (WrappedInvocationHandler) Proxy.getInvocationHandler(other);
            return otherWrapped.m_delegate.equals(m_delegate);
        }
        return false;
    }
    
    public boolean shouldHandle( Method method, Object[] args)
    {
        if(method.getName().equals("equals") && args != null && args.length == 1)
        {
            return true;
        }
        else if(method.getName().equals("hashCode") && args == null)
        {
            return true;
        }
        return false;
    }
    
    public Object handle( Method method, Object[] args)
    {
        if(method.getName().equals("equals") && args != null && args.length == 1)
        {
            return wrappedEquals(args[0]);
        }
        else if(method.getName().equals("hashCode") && args == null)
        {
            return new Integer(wrappedashCode());
        }
        else
            throw new IllegalStateException("how did we get here");
        
    }
    
    public int wrappedashCode()
    {
        return m_delegate.hashCode();
    }


    public Object invoke(Object arg0, Method arg1, Object[] arg2) throws Throwable
    {
        if(shouldHandle(arg1, arg2))
            return handle(arg1, arg2);
        throw new IllegalStateException("not configured");
    }
    
    
}
