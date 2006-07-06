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

package games.strategy.engine.lobby.server.ui;

import games.strategy.engine.lobby.server.userDB.Database;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

import javax.swing.*;
import javax.swing.table.*;

public class DBExplorerPanel extends JPanel
{
    
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
        m_sql = new JTextArea(10, 80);
        m_sql.setText("select * from ta_users " +
                "\n\n" +
                "update ta_users set password = \'foo\' where username = \'1152218272375\'\n\n" +
                "select * from ta_users where CAST(joined as DATE) < CAST('2008-11-12' AS DATE)"
                
        );
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
        
            public void actionPerformed(ActionEvent e)
            {
                execute();
        
            }
        
        });
    }
    
    private boolean isNotQuery(String sql)
    {
        
        sql = sql.toUpperCase().trim();
        return (sql.startsWith("INSERT") || sql.startsWith("UPDATE") || sql.startsWith("CREATE") || sql.startsWith("DELETE"));
    }

    private void execute()
    {
        Connection con = Database.getConnection();
        try
        {
            
            String sql = m_sql.getSelectedText();
            if(sql == null || sql.length() == 0)
                sql = m_sql.getText();
            
            Statement ps = con.createStatement();
            
            if(isNotQuery(sql ))
            {
                int rs = ps.executeUpdate(sql);
                DefaultTableModel model = new DefaultTableModel();
                model.addColumn("COUNT");
                model.addRow(new Object[] {rs});
                m_table.setModel(model);
            }
            else
            {
                ResultSet rs = ps.executeQuery(sql);
                TableModel model = DbExplorerPanel(rs);
                if(model != null)
                    m_table.setModel(model);
            }
            
        }
        catch(SQLException sqle)
        {
            sqle.printStackTrace();
        }
        finally
        {
            try
            {
                con.close();
            } catch (SQLException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }    

    private void setWidgetActivation()
    {

    }
    
    
    public static TableModel DbExplorerPanel(ResultSet rs)
    { 
        try
        {
            DefaultTableModel model = new DefaultTableModel();
            
            List<String> columnNames = new ArrayList<String>();
            int count = rs.getMetaData().getColumnCount();
            
            if(count <= 0)
                return null;
            
            for(int i =1; i < count; i++)
            {
                String columnName = rs.getMetaData().getColumnName(i);
                columnNames.add(columnName);
                model.addColumn(columnName);
            }
            
            while(rs.next())
            {
                
                List<String> values = new ArrayList<String>();
                for(String column : columnNames)
                {
                    values.add(rs.getString(column));
                }
                model.addRow(values.toArray());
            }
            
            
            return model;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        
        
    }

    
    
}
