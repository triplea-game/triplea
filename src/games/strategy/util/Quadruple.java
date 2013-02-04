package games.strategy.util;

public class Quadruple<F, S, T, Q> extends Triple<F, S, T>
{
	private static final long serialVersionUID = -6236381434393510449L;
	private final Q m_quad;
	
	public Quadruple(final F first, final S second, final T third, final Q forth)
	{
		super(first, second, third);
		m_quad = forth;
	}
	
	public Q getForth()
	{
		return m_quad;
	}
	
	@Override
	public String toString()
	{
		return "[" + (super.getFirst() == null ? "null" : super.getFirst().toString()) + ", " + (super.getSecond() == null ? "null" : super.getSecond().toString()) + ", "
					+ (super.getThird() == null ? "null" : super.getThird().toString()) + ", " + (m_quad == null ? "null" : m_quad.toString()) + "]";
	}
}
