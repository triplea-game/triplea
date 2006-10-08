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

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.ServerOptions;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.Messengers;
import games.strategy.ui.TableSorter;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;

public class LobbyGamePanel extends JPanel
{
    private JButton m_hostGame;
    private JButton m_joinGame;
    private LobbyGameTableModel m_gameTableModel;
    private Messengers m_messengers;
    private JTable m_gameTable;
    private TableSorter m_tableSorter;
    
    public LobbyGamePanel(Messengers messengers)
    {
        m_messengers = messengers;
        createComponents();
        layoutComponents();
        setupListeners();
        setWidgetActivation();
    }

    private void createComponents()
    {
        m_hostGame = new JButton("Host Game");
        m_joinGame = new JButton("Join Game");
        m_gameTableModel = new LobbyGameTableModel(m_messengers.getMessenger(), m_messengers.getChannelMessenger(), m_messengers.getRemoteMessenger());
        
        
        
        m_tableSorter = new TableSorter(m_gameTableModel); 
        m_gameTable = new JTable(m_tableSorter);         
        m_tableSorter.setTableHeader(m_gameTable.getTableHeader());

        
        //only allow one row to be selected
        m_gameTable.setColumnSelectionAllowed(false);
        m_gameTable.setRowSelectionAllowed(true);
        m_gameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        //by default, sort newest first
        int dateColumn = m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Started);
         m_tableSorter.setSortingStatus(dateColumn, TableSorter.DESCENDING);
        
         m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Players)).setPreferredWidth(65);
         m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Status )).setPreferredWidth(150);
         m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Name )).setPreferredWidth(150);
         m_gameTable.getColumnModel().getColumn(m_gameTableModel.getColumnIndex(LobbyGameTableModel.Column.Comments )).setPreferredWidth(150);
         
         m_gameTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer()
         {

            private SimpleDateFormat format = new SimpleDateFormat("h:m a" ); 
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
            {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setText(format.format((Date) value));
                return this;
            }
         
         });
        
    }

    private void layoutComponents()
    {
        JScrollPane scroll = new JScrollPane(m_gameTable);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
        
        JToolBar toolBar = new JToolBar();
        
        toolBar.add(m_hostGame);
        toolBar.add(m_joinGame);
        toolBar.setFloatable(false);
        
        add(toolBar, BorderLayout.SOUTH);
    }

    private void setupListeners()
    {
        m_hostGame.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                hostGame();
            }
        
        });
        
        m_joinGame.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                joinGame();
            }
        
        });
        
        m_gameTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
        
            public void valueChanged(ListSelectionEvent e)
            {
                setWidgetActivation();
            }
        
        });
        
    }
    

    private void joinGame()
    {
        int selectedIndex = m_gameTable.getSelectedRow();
        if(selectedIndex == -1)
            return;
        
        //we sort the table, so get the correct index
        int modelIndex = m_tableSorter.modelIndex(selectedIndex);
        GameDescription description = m_gameTableModel.get(modelIndex);
        
        
        List<String> commands = new ArrayList<String>();
        populateBasicJavaArgs(commands);
        
        commands.add("-D" + GameRunner2.TRIPLEA_CLIENT_PROPERTY + "=true");
        commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + description.getPort());
        commands.add("-D" + GameRunner2.TRIPLEA_HOST_PROPERTY + "=" + description.getHostedBy().getAddress().getHostAddress());
        commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + m_messengers.getMessenger().getLocalNode().getName());
        
         
        String javaClass = "games.strategy.engine.framework.GameRunner";
        commands.add(javaClass);
                
        
        
        try
        {
           
            
            @SuppressWarnings("unused")
            Process p =  Runtime.getRuntime().exec(commands.toArray(new String[] {}));
            
//            Reader reader = new InputStreamReader(p.getInputStream());
//            int c = reader.read();
//            while(c > 0)
//            {
//                System.out.write(c);
//                c = reader.read();
//            }
        } catch (IOException e)
        {
         
            e.printStackTrace();
        }
        
        
    }

    protected void hostGame()
    {
        ServerOptions options = new ServerOptions(JOptionPane.getFrameForComponent(this), m_messengers.getMessenger().getLocalNode().getName() ,3300, true);
        options.setLocationRelativeTo(JOptionPane.getFrameForComponent(this));
        options.setNameEditable(false);
        options.setVisible(true);
        if(!options.getOKPressed())
        {
            return;
        }
         
        List<String> commands = new ArrayList<String>();
        populateBasicJavaArgs(commands);
        
        commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PROPERTY + "=true");
        commands.add("-D" + GameRunner2.TRIPLEA_PORT_PROPERTY + "=" + options.getPort());
        commands.add("-D" + GameRunner2.TRIPLEA_NAME_PROPERTY + "=" + options.getName());
        
        
        commands.add("-D" + GameRunner2.LOBBY_HOST + "=" + m_messengers.getMessenger().getServerNode().getAddress().getHostAddress());
        commands.add("-D" + GameRunner2.LOBBY_PORT + "=" + m_messengers.getMessenger().getServerNode().getPort());
        commands.add("-D" + GameRunner2.LOBBY_GAME_COMMENTS + "=" + options.getComments());

        if(options.getPassword() != null &&  options.getPassword().length() > 0)
            commands.add("-D" + GameRunner2.TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + options.getPassword());

        String javaClass = "games.strategy.engine.framework.GameRunner";
        commands.add(javaClass);
        
        try
        {
           
            
            @SuppressWarnings("unused")
            Process p =  Runtime.getRuntime().exec(commands.toArray(new String[] {}));
            
//            Reader reader = new InputStreamReader(p.getInputStream());
//            int c = reader.read();
//            while(c > 0)
//            {
//                System.out.write(c);
//                c = reader.read();
//            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        
    }

    private static void populateBasicJavaArgs(List<String> commands)
    {
        String javaCommand =  System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"  ;
        commands.add(javaCommand);
        
        commands.add("-classpath");
        commands.add(System.getProperty("java.class.path"));
        commands.add("-Xmx128m");
        
        
        
        if(GameRunner.isMac())
        {   
            commands.add("-Dapple.laf.useScreenMenuBar=true");
            
            
            commands.add("-Xdock:name=\"TripleA\"");
            
            File icons = new File(GameRunner.getRootFolder(), "icons/triplea_icon.png" );
            if(!icons.exists())
                throw new IllegalStateException("Icon file not found");
            
            
            commands.add("-Xdock:icon=" + icons.getAbsolutePath() + "");
        }
    }


    private void setWidgetActivation()
    {
        boolean selected = m_gameTable.getSelectedRow() >= 0;
        m_joinGame.setEnabled(selected);
    }
    

}
