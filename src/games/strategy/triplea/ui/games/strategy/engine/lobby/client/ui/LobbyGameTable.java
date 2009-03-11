/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.lobby.client.ui;

import games.strategy.net.GUID;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;

public class LobbyGameTable extends JTable
{
    
    private GUID m_selectedGame;
    private boolean inTableChange = false;
    

    public LobbyGameTable(TableModel model)
    {
        super(model);
        getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
        
            //track the currently selected row
            public void valueChanged(ListSelectionEvent e)
            {
                if(!inTableChange)
                    markSelection();
            }
        
        });
    }

    /**
     * The sorting model will loose the currently selected row.
     * So we need to resotre the selection after it has updated
     */
    public void tableChanged(TableModelEvent e) {
   
        inTableChange = true;
        try
        {
            super.tableChanged(e);
        }
        finally 
        {
            inTableChange = false;
        }
        restoreSelection();
            
    }
    

    /**
     * record the id of the currently selected game
     */
    private void markSelection() {
        
        int selected = getSelectedRow();
        if(selected >= 0) {
            m_selectedGame = (GUID) getModel().getValueAt(selected, LobbyGameTableModel.Column.GUID.ordinal());
        } else {
            m_selectedGame = null;
        }
    }
    
    /**
     * Restore the selection to the marked value
     */
    private void restoreSelection() {
        if(m_selectedGame == null)
            return;
        
        for(int i =0; i < getModel().getRowCount(); i++) {
            GUID current =  (GUID) getModel().getValueAt(i, LobbyGameTableModel.Column.GUID.ordinal());
            if(current.equals(m_selectedGame)) {
                getSelectionModel().setSelectionInterval(i, i);
                break;
            }
        }
    }


}
