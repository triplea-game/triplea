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
package games.strategy.engine.random;

import games.strategy.engine.data.PlayerID;
import games.strategy.util.IntegerMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RandomStatsDetails implements Serializable
{
	private static final long serialVersionUID = 69602197220912520L;
	private final Map<PlayerID, IntegerMap<Integer>> m_data;
	private final IntegerMap<Integer> m_totalMap;
	private final DiceStatistic m_totalStats;
	private final Map<PlayerID, DiceStatistic> m_playerStats = new HashMap<PlayerID, DiceStatistic>();
	
	public RandomStatsDetails(final Map<PlayerID, IntegerMap<Integer>> randomStats, final int diceSides)
	{
		m_data = randomStats;
		m_totalMap = new IntegerMap<Integer>();
		for (final Entry<PlayerID, IntegerMap<Integer>> entry : m_data.entrySet())
		{
			m_totalMap.add(entry.getValue());
		}
		m_totalStats = getDiceStatistic(m_totalMap, diceSides);
		for (final Entry<PlayerID, IntegerMap<Integer>> entry : m_data.entrySet())
		{
			m_playerStats.put(entry.getKey(), getDiceStatistic(entry.getValue(), diceSides));
		}
	}
	
	private static DiceStatistic getDiceStatistic(final IntegerMap<Integer> stats, final int diceSides)
	{
		final double m_average;
		final int m_total;
		final double m_median;
		final double m_stdDeviation;
		final double m_variance;
		if (stats.totalValues() != 0)
		{
			
			int sumTotal = 0;
			int total = 0;
			// TODO: does this need to be updated to take data.getDiceSides() ?
			for (int i = 1; i <= diceSides; i++)
			{
				sumTotal += i * stats.getInt(Integer.valueOf(i));
				total += stats.getInt(Integer.valueOf(i));
			}
			m_total = total;
			m_average = (sumTotal) / ((double) stats.totalValues());
			/**
			 * calculate median
			 */
			if (total % 2 != 0)
			{
				m_median = calcMedian((total / 2) + 1, diceSides, stats);
			}
			else
			{
				double tmp1 = 0;
				double tmp2 = 0;
				tmp1 = calcMedian((total / 2), diceSides, stats);
				tmp2 = calcMedian((total / 2) + 1, diceSides, stats);
				m_median = (tmp1 + tmp2) / 2;
			}
			/**
			 * calculate variance
			 */
			double variance = 0;
			// TODO: does this need to be updated to take data.getDiceSides() ?
			for (int i = 1; i <= diceSides; i++)
			{
				variance += (stats.getInt(Integer.valueOf(i)) - (total / diceSides)) * (stats.getInt(Integer.valueOf(i)) - (total / diceSides));
			}
			m_variance = variance / (total - 1);
			/**
			 * calculate standard deviation
			 */
			m_stdDeviation = Math.sqrt(m_variance);
		}
		else
		{
			m_average = 0;
			m_total = 0;
			m_median = 0;
			m_stdDeviation = 0;
			m_variance = 0;
		}
		return new DiceStatistic(m_average, m_total, m_median, m_stdDeviation, m_variance);
	}
	
	public Map<PlayerID, IntegerMap<Integer>> getData()
	{
		return m_data;
	}
	
	public IntegerMap<Integer> getTotalData()
	{
		return m_totalMap;
	}
	
	public Map<PlayerID, DiceStatistic> getPlayerStats()
	{
		return m_playerStats;
	}
	
	public DiceStatistic getTotalStats()
	{
		return m_totalStats;
	}
	
	private static int calcMedian(final int centerPoint, final int diceSides, final IntegerMap<Integer> stats)
	{
		int sum = 0;
		int i = 1;
		for (i = 1; i <= diceSides; i++)
		{
			sum += stats.getInt(Integer.valueOf(i));
			if (sum >= centerPoint)
			{
				return i;
			}
		}
		return i; // This is to stop java from complaining
		// it should never reach this part.
	}
	
	public static String getStatsString(final IntegerMap<Integer> diceRolls, final DiceStatistic diceStats, final String title, final String indentation)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append(indentation + title + "\n");
		for (final Integer key : new TreeSet<Integer>(diceRolls.keySet()))
		{
			final int value = diceRolls.getInt(key);
			sb.append(indentation + indentation + indentation + key + " was rolled " + value + " times" + "\n");
		}
		final DecimalFormat format = new DecimalFormat("#0.000");
		sb.append(indentation + indentation + "Average roll : " + format.format(diceStats.getAverage()) + "\n");
		sb.append(indentation + indentation + "Median : " + format.format(diceStats.getMedian()) + "\n");
		sb.append(indentation + indentation + "Variance : " + format.format(diceStats.getVariance()) + "\n");
		sb.append(indentation + indentation + "Standard Deviation : " + format.format(diceStats.getStdDeviation()) + "\n");
		sb.append(indentation + indentation + "Total rolls : " + diceStats.getTotal() + "\n");
		return sb.toString();
	}
	
	public static String getAllStatsString(final RandomStatsDetails details, final String indentation)
	{
		if (details.getTotalStats().getTotal() <= 0)
			return "";
		final StringBuilder sb = new StringBuilder();
		sb.append("Dice Statistics:\n\n");
		sb.append(getStatsString(details.getTotalData(), details.getTotalStats(), "Total", indentation));
		if (details.getData().containsKey(null))
		{
			sb.append("\n");
			sb.append(getStatsString(details.getData().get(null), details.getPlayerStats().get(null), "Null / Other", indentation));
		}
		for (final Entry<PlayerID, IntegerMap<Integer>> entry : details.getData().entrySet())
		{
			if (entry.getKey() == null)
				continue;
			sb.append("\n");
			sb.append(getStatsString(entry.getValue(), details.getPlayerStats().get(entry.getKey()), (entry.getKey() == null ? "Null / Other" : entry.getKey().getName() + " Combat"), indentation));
		}
		return sb.toString();
	}
	
	public String getAllStatsString(final String indentation)
	{
		return getAllStatsString(this, indentation);
	}
	
	public static JPanel getStatsDisplay(final IntegerMap<Integer> diceRolls, final DiceStatistic diceStats, final String title)
	{
		final JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createEtchedBorder());
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(new JLabel("<html><b>" + title + "</b></html>"));
		for (final Integer key : new TreeSet<Integer>(diceRolls.keySet()))
		{
			final int value = diceRolls.getInt(key);
			final JLabel label = new JLabel(key + " was rolled " + value + " times");
			panel.add(label);
		}
		panel.add(new JLabel("  "));
		final DecimalFormat format = new DecimalFormat("#0.000");
		panel.add(new JLabel("Average roll : " + format.format(diceStats.getAverage())));
		panel.add(new JLabel("Median : " + format.format(diceStats.getMedian())));
		panel.add(new JLabel("Variance : " + format.format(diceStats.getVariance())));
		panel.add(new JLabel("Standard Deviation : " + format.format(diceStats.getStdDeviation())));
		panel.add(new JLabel("Total rolls : " + diceStats.getTotal()));
		return panel;
	}
	
	public static JPanel getAllStats(final RandomStatsDetails details)
	{
		final Insets insets = new Insets(2, 2, 2, 2);
		final JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		panel.setBorder(BorderFactory.createEmptyBorder());
		panel.add(getStatsDisplay(details.getTotalData(), details.getTotalStats(), "Total"),
					new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, insets, 0, 0));
		if (details.getData().containsKey(null))
		{
			panel.add(getStatsDisplay(details.getData().get(null), details.getPlayerStats().get(null), "Null / Other"),
						new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, insets, 0, 0));
		}
		final int rows = Math.max(2, details.getData().size() / 6);
		int x = 0;
		for (final Entry<PlayerID, IntegerMap<Integer>> entry : details.getData().entrySet())
		{
			if (entry.getKey() == null)
				continue;
			panel.add(getStatsDisplay(entry.getValue(), details.getPlayerStats().get(entry.getKey()), (entry.getKey() == null ? "Null / Other" : entry.getKey().getName() + " Combat")),
						new GridBagConstraints((x / rows), 1 + (x % rows), 1, 1, 1, 1, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE, insets, 0, 0));
			x++;
		}
		return panel;
	}
	
	public JPanel getAllStats()
	{
		return getAllStats(this);
	}
} // end class RandomStatsDetails
