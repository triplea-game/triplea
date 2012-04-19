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
package games.strategy.engine.lobby.server.ui;

import games.strategy.engine.lobby.server.userDB.Database;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class DBExplorerPanel extends JPanel
{
	private static final long serialVersionUID = 7259741539317170247L;
	private JTable m_table;
	private JTextArea m_sql;
	private JButton m_execute;
	
	public DBExplorerPanel()
	{
		createComponents();
		layoutComponents();
		setupListeners();
		setWidgetActivation();
	}
	
	private void createComponents()
	{
		m_execute = new JButton("Execute");
		m_sql = new JTextArea();
		m_sql.setText("select * from ta_users " + "\n\n" + "update ta_users set password = \'foo\' where username = \'1152218272375\'\n\n"
					+ "select * from ta_users where CAST(joined as DATE) < CAST('2008-11-12' AS DATE) \n" + "select * from bad_words \n" + "select * from banned_ips \n" + "\n"
					+ "delete from banned_ips where ip = \'192.168.1.0\'");
		m_table = new JTable();
	}
	
	private void layoutComponents()
	{
		setLayout(new BorderLayout());
		add(m_sql, BorderLayout.NORTH);
		add(new JScrollPane(m_table), BorderLayout.CENTER);
		add(m_execute, BorderLayout.SOUTH);
	}
	
	private void setupListeners()
	{
		m_execute.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				execute();
			}
		});
	}
	
	public static boolean isNotQuery(String sql)
	{
		sql = sql.toUpperCase().trim();
		return (sql.startsWith("INSERT") || sql.startsWith("UPDATE") || sql.startsWith("CREATE") || sql.startsWith("DELETE"));
	}
	
	private void execute()
	{
		final Connection con = Database.getConnection();
		try
		{
			String sql = m_sql.getSelectedText();
			if (sql == null || sql.length() == 0)
				sql = m_sql.getText();
			final Statement ps = con.createStatement();
			if (isNotQuery(sql))
			{
				final int rs = ps.executeUpdate(sql);
				final DefaultTableModel model = new DefaultTableModel();
				model.addColumn("COUNT");
				model.addRow(new Object[] { rs });
				m_table.setModel(model);
			}
			else
			{
				final ResultSet rs = ps.executeQuery(sql);
				final TableModel model = createTableModel(rs);
				if (model != null)
					m_table.setModel(model);
			}
		} catch (final SQLException sqle)
		{
			sqle.printStackTrace();
		} finally
		{
			try
			{
				con.close();
			} catch (final SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void setWidgetActivation()
	{
	}
	
	public static TableModel createTableModel(final ResultSet rs)
	{
		try
		{
			final DefaultTableModel model = new DefaultTableModel();
			final List<String> columnNames = new ArrayList<String>();
			final int count = rs.getMetaData().getColumnCount();
			if (count <= 0)
				return null;
			model.addColumn("Count");
			for (int i = 1; i <= count; i++)
			{
				final String columnName = rs.getMetaData().getColumnName(i);
				columnNames.add(columnName);
				model.addColumn(columnName);
			}
			int index = 1;
			while (rs.next())
			{
				final List<String> values = new ArrayList<String>();
				values.add("" + index++);
				for (final String column : columnNames)
				{
					values.add(rs.getString(column));
				}
				model.addRow(values.toArray());
			}
			return model;
		} catch (final Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
