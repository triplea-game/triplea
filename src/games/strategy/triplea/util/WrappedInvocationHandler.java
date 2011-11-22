package games.strategy.triplea.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class WrappedInvocationHandler implements InvocationHandler
{
	private final Object m_delegate;
	
	public WrappedInvocationHandler(final Object delegate)
	{
		if (delegate == null)
			throw new IllegalArgumentException("delegate cant be null");
		m_delegate = delegate;
	}
	
	public boolean wrappedEquals(final Object other)
	{
		if (other == this)
			return true;
		if (Proxy.isProxyClass(other.getClass()) && Proxy.getInvocationHandler(other) instanceof WrappedInvocationHandler)
		{
			final WrappedInvocationHandler otherWrapped = (WrappedInvocationHandler) Proxy.getInvocationHandler(other);
			return otherWrapped.m_delegate.equals(m_delegate);
		}
		return false;
	}
	
	public boolean shouldHandle(final Method method, final Object[] args)
	{
		if (method.getName().equals("equals") && args != null && args.length == 1)
		{
			return true;
		}
		else if (method.getName().equals("hashCode") && args == null)
		{
			return true;
		}
		return false;
	}
	
	public Object handle(final Method method, final Object[] args)
	{
		if (method.getName().equals("equals") && args != null && args.length == 1)
		{
			return wrappedEquals(args[0]);
		}
		else if (method.getName().equals("hashCode") && args == null)
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
	
	public Object invoke(final Object arg0, final Method arg1, final Object[] arg2) throws Throwable
	{
		if (shouldHandle(arg1, arg2))
			return handle(arg1, arg2);
		throw new IllegalStateException("not configured");
	}
}
