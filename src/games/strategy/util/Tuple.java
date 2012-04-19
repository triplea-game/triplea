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
}
