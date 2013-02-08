package games.strategy.util;

public class Triple<F, S, T> extends Tuple<F, S>
{
	private static final long serialVersionUID = 4326718231392107904L;
	private final T m_third;
	
	public Triple(final F first, final S second, final T third)
	{
		super(first, second);
		m_third = third;
	}
	
	public T getThird()
	{
		return m_third;
	}
	
	@Override
	public String toString()
	{
		return "[" + (super.getFirst() == null ? "null" : super.getFirst().toString()) + ", " + (super.getSecond() == null ? "null" : super.getSecond().toString()) + ", "
					+ (m_third == null ? "null" : m_third.toString()) + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((m_third == null) ? 0 : m_third.hashCode());
		return result;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(final Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Triple other = (Triple) obj;
		if (m_third == null)
		{
			if (other.m_third != null)
				return false;
		}
		else if (!m_third.equals(other.m_third))
			return false;
		return true;
	}
}
