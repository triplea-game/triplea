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
package games.strategy.triplea.ui;

import games.strategy.engine.framework.VerifiedRandomNumbers;
import games.strategy.engine.random.RemoteRandom;
import games.strategy.triplea.formatter.MyFormatter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * @author Sean Bridges
 */
public class VerifiedRandomNumbersDialog extends JDialog
{
	public VerifiedRandomNumbersDialog(final Component parent)
	{
		super(JOptionPane.getFrameForComponent(parent), "Verified Random Numbers", false);
		init();
		pack();
	}
	
	private void init()
	{
		final List<VerifiedRandomNumbers> verified = RemoteRandom.getVerifiedRandomNumbers();
		final String[][] tableValues = getTableValues(verified);
		final DefaultTableModel model = new DefaultTableModel(tableValues, new String[] { "Reason", "Dice Rolls" })
		{
			@Override
			public boolean isCellEditable(final int row, final int column)
			{
				return false;
			}
		};
		final JTable table = new JTable(model);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
		final JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
		getContentPane().add(buttons, BorderLayout.SOUTH);
		final JButton close = new JButton("Close");
		close.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				setVisible(false);
			}
		});
		buttons.add(close);
	}
	
	/**
	 * @param verified
	 * @return
	 */
	private String[][] getTableValues(final List<VerifiedRandomNumbers> verified)
	{
		if (verified.isEmpty())
			return new String[][] { { "", "" } };
		final String[][] tableValues = new String[verified.size()][2];
		for (int i = 0; i < verified.size(); i++)
		{
			final VerifiedRandomNumbers number = verified.get(i);
			tableValues[i][0] = number.getAnnotation();
			tableValues[i][1] = MyFormatter.asDice(number.getValues());
		}
		return tableValues;
	}
}
