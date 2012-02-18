/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility for getting/setting java bean style properties on an object.
 * 
 * @author sgb
 */
public class PropertyUtil
{
	@SuppressWarnings("unused")
	private static final Class<?>[] STRING_ARGS = { String.class };
	@SuppressWarnings("unused")
	private static final Class<?>[] INT_ARGS = { int.class };
	
	public static void set(final String propertyName, final Object value, final Object subject)
	{
		final Method m = getSetter(propertyName, subject, value);
		try
		{
			m.setAccessible(true);
			m.invoke(subject, value);
		} catch (final Exception e)
		{
			throw new IllegalStateException("Could not set property:" + propertyName + " subject:" + subject + " new value:" + value, e);
		}
	}
	
	/**
	 * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public static void set(final String propertyName, final Object value, final Object subject, final boolean clearFirst)
	{
		if (clearFirst)
		{
			final Method c = getClearer(propertyName, subject);
			try
			{
				c.setAccessible(true);
				c.invoke(subject);
			} catch (final Exception e)
			{
				throw new IllegalStateException("Could not clear property:" + propertyName + " subject:" + subject + " new value:" + value, e);
			}
		}
		set(propertyName, value, subject);
	}
	
	/**
	 * You don't want to clear the variable unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
	 */
	public static void clear(final String propertyName, final Object subject)
	{
		try
		{
			final Method c = getClearer(propertyName, subject);
			c.setAccessible(true);
			c.invoke(subject);
		} catch (final Exception e)
		{
			throw new IllegalStateException("Could not clear property:" + propertyName + " subject:" + subject, e);
		}
	}
	
	public static Field getFieldIncludingFromSuperClasses(@SuppressWarnings("rawtypes") final Class c, final String name, final boolean justFromSuper)
	{
		Field rVal = null;
		
		if (!justFromSuper)
		{
			try
			{
				rVal = c.getDeclaredField(name);
				return rVal;
			} catch (final NoSuchFieldException e)
			{
				return getFieldIncludingFromSuperClasses(c, name, true);
			}
		}
		else
		{
			if (c.getSuperclass() == null)
				throw new IllegalStateException("No such Property Field: " + name);
			try
			{
				rVal = c.getSuperclass().getDeclaredField(name);
				return rVal;
			} catch (final NoSuchFieldException e)
			{
				return getFieldIncludingFromSuperClasses(c.getSuperclass(), name, true);
			}
		}
	}
	
	public static Object getPropertyFieldObject(final String propertyName, final Object subject)
	{
		Object rVal = null;
		Field field = null;
		try
		{
			field = getFieldIncludingFromSuperClasses(subject.getClass(), "m_" + propertyName, false);
		} catch (final Exception e)
		{
			throw new IllegalStateException("No such Property Field: " + "m_" + propertyName + " for Subject: " + subject.toString(), e);
		}
		try
		{
			field.setAccessible(true);
			rVal = field.get(subject);
		} catch (final Exception e)
		{
			throw new IllegalStateException("No such Property Field: " + "m_" + propertyName + " for Subject: " + subject.toString(), e);
		}
		return rVal;
	}
	
	/* DO NOT DELETE PLEASE
	public static Object get(final String propertyName, final Object subject)
	{
		try
		{
			final Method getter = subject.getClass().getMethod("get" + capitalizeFirstLetter(propertyName), new Class[0]);
			return getter.invoke(subject, new Object[0]);
		} catch (final Exception e)
		{
			throw new IllegalStateException("Could not get property:" + propertyName + " subject:" + subject, e);
		}
	}
	
	public static Object getRaw(final String property, final Object subject)
	{
		try
		{
			final Method getter = subject.getClass().getMethod("getRawPropertyObject", String.class);
			return getter.invoke(subject, property);
		} catch (final Exception e)
		{
			throw new IllegalStateException("Could not get property:" + property + " subject:" + subject, e);
		}
	}*/

	private static String capitalizeFirstLetter(final String aString)
	{
		char first = aString.charAt(0);
		first = Character.toUpperCase(first);
		return first + aString.substring(1);
	}
	
	private static Method getSetter(final String propertyName, final Object subject, final Object value)
	{
		final String setterName = "set" + capitalizeFirstLetter(propertyName);
		// for (final Method m : subject.getClass().getDeclaredMethods())
		for (final Method m : subject.getClass().getMethods())
		{
			if (m.getName().equals(setterName))
			{
				try
				{
					final Class<?> argType = value.getClass();
					return subject.getClass().getMethod(setterName, argType);
				} catch (final NoSuchMethodException nsmf)
				{
					// Go ahead and try the first one
					return m;
				} catch (final NullPointerException n)
				{
					// Go ahead and try the first one
					return m;
				}
			}
		}
		throw new IllegalStateException("No method called:" + setterName + " on:" + subject);
	}
	
	private static Method getClearer(final String propertyName, final Object subject)
	{
		final String clearerName = "clear" + capitalizeFirstLetter(propertyName);
		// for (final Method c : subject.getClass().getDeclaredMethods())
		for (final Method c : subject.getClass().getMethods())
		{
			if (c.getName().equals(clearerName))
			{
				try
				{
					return subject.getClass().getMethod(clearerName);
				} catch (final NoSuchMethodException nsmf)
				{
					// Go ahead and try the first one
					return c;
				} catch (final NullPointerException n)
				{
					// Go ahead and try the first one
					return c;
				}
			}
		}
		throw new IllegalStateException("No method called:" + clearerName + " on:" + subject);
	}
}
