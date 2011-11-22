package games.strategy.util;

public class Triple<F, S, T> extends Tuple<F, S>
{
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
}
