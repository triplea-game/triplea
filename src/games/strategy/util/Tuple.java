package games.strategy.util;

import java.io.Serializable;

public class Tuple<T, S> implements Serializable
{
	private static final long serialVersionUID = -5091545494950868125L;
	private final T m_first;
	private final S m_second;
	
	public Tuple(final T first, final S second)
	{
		m_first = first;
		m_second = second;
	}
	
	public T getFirst()
	{
		return m_first;
	}
	
	public S getSecond()
	{
		return m_second;
	}
	
	@Override
	public String toString()
	{
		return "[" + (m_first == null ? "null" : m_first.toString()) + ", " + (m_second == null ? "null" : m_second.toString()) + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((m_first == null) ? 0 : m_first.hashCode());
		result = prime * result + ((m_second == null) ? 0 : m_second.hashCode());
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Tuple other = (Tuple) obj;
		if (m_first == null)
		{
			if (other.m_first != null)
				return false;
		}
		else if (!m_first.equals(other.m_first))
			return false;
		if (m_second == null)
		{
			if (other.m_second != null)
				return false;
		}
		else if (!m_second.equals(other.m_second))
			return false;
		return true;
	}
}
