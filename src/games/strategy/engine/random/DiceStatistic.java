package games.strategy.engine.random;

public class DiceStatistic
{
	private final double m_average;
	private final int m_total;
	private final double m_median;
	private final double m_stdDeviation;
	private final double m_variance;
	
	public DiceStatistic(final double average, final int total, final double median, final double stdDeviation, final double variance)
	{
		m_average = average;
		m_total = total;
		m_median = median;
		m_stdDeviation = stdDeviation;
		m_variance = variance;
	}
	
	public double getAverage()
	{
		return m_average;
	}
	
	public int getTotal()
	{
		return m_total;
	}
	
	public double getMedian()
	{
		return m_median;
	}
	
	public double getStdDeviation()
	{
		return m_stdDeviation;
	}
	
	public double getVariance()
	{
		return m_variance;
	}
}
