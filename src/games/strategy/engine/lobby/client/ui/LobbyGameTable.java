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
package games.strategy.engine.lobby.client.ui;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.GUID;
import games.strategy.ui.TableSorter;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class LobbyGameTable extends JTable
{
	private static final long serialVersionUID = 8632519876114231003L;
	private GUID m_selectedGame;
	private boolean inTableChange = false;
	private final Font m_defaultFont = UIManager.getDefaults().getFont("Table.font");
	private final Font m_italicFont = new Font(m_defaultFont.getFamily(), Font.ITALIC, m_defaultFont.getSize());
	
	public LobbyGameTable(final TableModel model)
	{
		super(model);
		getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			// track the currently selected row
			public void valueChanged(final ListSelectionEvent e)
			{
				if (!inTableChange)
					markSelection();
			}
		});
	}
	
	@Override
	public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex, final int vColIndex)
	{
		final Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
		if (this.dataModel instanceof TableSorter)
		{
			final TableSorter tmodel = (TableSorter) this.dataModel;
			if (tmodel.getTableModel() instanceof LobbyGameTableModel)
			{
				final LobbyGameTableModel lmodel = (LobbyGameTableModel) tmodel.getTableModel();
				final int row = tmodel.getUnderlyingModelRowAt(rowIndex);
				final GameDescription gd = lmodel.get(row);
				if (gd.getBotSupportEmail() != null && gd.getBotSupportEmail().length() > 0)
					c.setFont(m_italicFont);
				else
					c.setFont(m_defaultFont);
			}
		}
		return c;
	}
	
	/**
	 * The sorting model will loose the currently selected row.
	 * So we need to restore the selection after it has updated
	 */
	@Override
	public void tableChanged(final TableModelEvent e)
	{
		inTableChange = true;
		try
		{
			super.tableChanged(e);
		} finally
		{
			inTableChange = false;
		}
		restoreSelection();
	}
	
	/**
	 * record the id of the currently selected game
	 */
	private void markSelection()
	{
		final int selected = getSelectedRow();
		if (selected >= 0)
		{
			m_selectedGame = (GUID) getModel().getValueAt(selected, LobbyGameTableModel.Column.GUID.ordinal());
		}
		else
		{
			m_selectedGame = null;
		}
	}
	
	/**
	 * Restore the selection to the marked value
	 */
	private void restoreSelection()
	{
		if (m_selectedGame == null)
			return;
		for (int i = 0; i < getModel().getRowCount(); i++)
		{
			final GUID current = (GUID) getModel().getValueAt(i, LobbyGameTableModel.Column.GUID.ordinal());
			if (current.equals(m_selectedGame))
			{
				getSelectionModel().setSelectionInterval(i, i);
				break;
			}
		}
	}
}
