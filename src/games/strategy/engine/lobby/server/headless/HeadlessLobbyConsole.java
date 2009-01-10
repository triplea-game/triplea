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

package games.strategy.engine.lobby.server.headless;

import games.strategy.debug.Console;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.ui.DBExplorerPanel;
import games.strategy.engine.lobby.server.userDB.Database;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Formatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless admin console for the lobby.
 * 
 * Reads commands from stdin, and writes responses to stdout.
 * 
 * @author Sean Bridges
 *
 */

public class HeadlessLobbyConsole
{
    private final LobbyServer server;
    private final PrintStream out;
    private final BufferedReader in;

    @SuppressWarnings("deprecation")
    private final String startDate = new Date().toGMTString();
    private final AtomicInteger totalLogins = new AtomicInteger();
    private final AtomicInteger currentConnections = new AtomicInteger();
    private volatile int maxConcurrentLogins = 0;
    
    
    public HeadlessLobbyConsole(LobbyServer server, InputStream in, PrintStream out)
    {
        this.out = out;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.server = server;
        
        server.getMessenger().addConnectionChangeListener(new IConnectionChangeListener()
        {
        
            public void connectionAdded(INode to)
            {
                currentConnections.incrementAndGet();
                totalLogins.incrementAndGet();
                
                //not strictly thread safe, but good enough
                maxConcurrentLogins = Math.max(maxConcurrentLogins, currentConnections.get());
            }
        
            public void connectionRemoved(INode to)
            {
                currentConnections.decrementAndGet();
            }
        
        });
    }

    public void start() 
    {
        Thread t = new Thread(new Runnable()
        {
        
            public void run()
            {
                printEvalLoop();
        
            }
        
        }, "Headless console eval print loop");
        t.start();
    }
    
    private void printEvalLoop()
    {
       out.println();
        
        while(true) 
        {
            out.print(">>>>");
            out.flush();
            try
            {
                String command = in.readLine();
                process(command.trim());
            } 
            catch(Throwable t)
            {
                t.printStackTrace();
                t.printStackTrace(out);
            }
        }
        
        
        
    }

    private void process(String command)
    {
        if(command.equals("")) {        
            return;
        }
        final String noun = command.split("\\s")[0];
        if(noun.equals("help")) { 
            showHelp();
        } else if(noun.equals("status")) { 
            showStatus();
        } else if(noun.equals("sql")) {
            executeSql(command.substring("sql".length(), command.length()).trim());
        } else if(noun.equals("quit")) {
            quit();
        } else if(noun.equals("backup")) {
            backup();
        } else if(noun.equals("memory")) {
            memory();
        } else if(noun.equals("threads")) {
            threads();
        }
        else {
            out.println("unrecognized command:" + command);
            showHelp();
        }
            
        
    }

    private void threads()
    {
        out.println(Console.getThreadDumps());
        
    }

    private void memory()
    {
        out.println(Console.getMemory());        
    }

    private void backup()
    {
        Database.backup();        
    }

    private void quit()
    {
        out.println("Are you sure? (y/n)");
        try
        {
            if(in.readLine().toLowerCase().startsWith("y")) {
                System.exit(0);
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void executeSql(String sql)
    {
        Connection con = Database.getConnection();
        try
        {
            
            Statement ps = con.createStatement();
            
            if(DBExplorerPanel.isNotQuery(sql ))
            {
                int rs = ps.executeUpdate(sql);
                out.println("Update count:" + rs);
            }
            else
            {
                ResultSet rs = ps.executeQuery(sql);
                print(rs);
                rs.close();
            }
            
        }
        catch(SQLException sqle)
        {
            sqle.printStackTrace();
            out.println(sqle.getMessage());
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

    private void print(ResultSet rs)
    {
        try
        {
            Formatter f = new Formatter(out);
            String itemFormat = "%20s ";
            f.format(itemFormat, "Count");
            
            int count = rs.getMetaData().getColumnCount();
            
            for(int i =1; i <= count; i++)
            {
                String columnName = rs.getMetaData().getColumnName(i);
                f.format(itemFormat, columnName);     
            }
            
            f.format("\n");
            for(int i =0; i < count; i++) {
                f.format(itemFormat, "-----------");
            }
            f.format("\n");
            
            int row = 1;
            while(rs.next())
            {               
                f.format(itemFormat, row++);
                for(int i =1; i <= count; i++)
                {
                    f.format(itemFormat, rs.getString(i));
                }
                f.format("\n");
                f.flush();
            }
        } catch(SQLException e) {
            e.printStackTrace(out);
        }
    }

    private void showStatus()
    {
        
        int port = server.getMessenger().getServerNode().getPort();
        
        out.print(
                String.format(
                        "port:%s\n" +
                        "up since:%s\n" +
                		"total logins:%s\n" + 
                		"current connections:%s\n" +
                		"max concurrent connections:%s\n",
                        port,
                        startDate,
                        totalLogins.get(),
                        currentConnections.get(),
                        maxConcurrentLogins
                        )
         );
        
    }

    private void showHelp()
    {
        out.println("available commands:\n" +
                "  backup - backup the database \n" +
        		"  help - show this message\n" +
        		"  memory - show memory usage\n" +
        		"  status - show status information\n" +
        		"  sql {sql} - execute a sql command and print the results\n" +
        		"  threads - get thread dumps\n" +
        		"  quit - quit\n" 
        		);
    }
    
}
