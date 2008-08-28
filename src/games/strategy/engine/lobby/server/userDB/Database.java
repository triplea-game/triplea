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

package games.strategy.engine.lobby.server.userDB;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;

import java.io.File;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Utility to get connections to the database.<p>
 * 
 * The database is embedded within the jvm.<p>
 * 
 * Getting a connection will cause the database (and the neccessary tables) to be created if it does not already exist.<p>
 * 
 * The database will be shutdown on System.exit through a shutdown hook.<p>
 * 
 * Getting a connection will also schedule backups at regular intervals.<p>
 * 
 * 
 * @author sgb
 */
public class Database
{
    private final static Logger s_logger = Logger.getLogger(Database.class.getName());
    
    private static final Object s_dbSetupLock = new Object();
    private static boolean s_isDbSetup = false;
    private static boolean s_areDBTablesCreated = false;

    
    private static File getCurrentDataBaseDir()
    {
        File dbRootDir = getDBRoot();
        File dbDir = new File(dbRootDir, "current");
        if(!dbDir.exists())
        {
            if(!dbDir.mkdirs())
                throw new IllegalStateException("Could not create derby dir");
        }
        return dbDir;
        
    }

    private static File getDBRoot()
    {
        File root;
        if(System.getProperties().containsKey(ServerLauncher.SERVER_ROOT_DIR_PROPERTY)) {        
            root = new File(System.getProperties().getProperty(ServerLauncher.SERVER_ROOT_DIR_PROPERTY));
        } else {
            root = GameRunner.getRootFolder();    
        }
        
        if(!root.exists())
        {
            throw new IllegalStateException("Root dir does not exist");
        }
        
        File dbRootDir = new File(root, "derby_db");
        return dbRootDir;
    }
    
    public static Connection getConnection()
    {
        ensureDbIsSetup();
        
        Connection conn = null;
        Properties props = getDbProps();

        /*
           The connection specifies create=true to cause
           the database to be created. To remove the database,
           remove the directory derbyDB and its contents.
           The directory derbyDB will be created under
           the directory that the system property
           derby.system.home points to, or the current
           directory if derby.system.home is not set.
         */
        String url = "jdbc:derby:ta_users;create=true";
        try
        {
            conn = DriverManager.getConnection(url, props);
        } catch (SQLException e)
        {
            s_logger.log(Level.SEVERE, e.getMessage(), e);
            throw new IllegalStateException("Could not create db connection");
        }
        
        ensureDbTablesAreCreated(conn);
        
        return conn;
    }

    /**
     * The connection passed in to this method is not closed, except in case of error.
     */
    private static void ensureDbTablesAreCreated(Connection conn)
    {
        synchronized(s_dbSetupLock)
        {
            try
            {
                if(s_areDBTablesCreated)
                    return;
                
                ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
                
                List<String> existing = new ArrayList<String>();
                while(rs.next()) 
                {
                    existing.add(rs.getString("TABLE_NAME").toUpperCase());
                }
                rs.close();

                
                if(!existing.contains("TA_USERS")) 
                {
                
                
                    Statement s = conn.createStatement();
                    s.execute("create table ta_users" +
                                "(" +
                                "userName varchar(40) NOT NULL PRIMARY KEY, " +
                                "password varchar(40) NOT NULL, " +
                                "email varchar(40) NOT NULL, " +
                                "joined timestamp NOT NULL, " +
                                "lastLogin timestamp NOT NULL, " +
                                "admin integer NOT NULL " +
                                ")"
                            );
                    s.close();
                }
                
                if(!existing.contains("BANNED_IPS")) 
                {
                    Statement s = conn.createStatement();
                    s.execute("create table banned_ips" +
                            "(" +
                            "ip varchar(40) NOT NULL PRIMARY KEY, " +
                            "ban_till timestamp  " +
                            ")"
                        );
                    s.close();
                }
                
                if(!existing.contains("BAD_WORDS")) 
                {
                    Statement s = conn.createStatement();
                    s.execute("create table bad_words" +
                            "(" +
                            "word varchar(40) NOT NULL PRIMARY KEY " +
                            ")"
                        );
                    s.close();
                }
                
                s_areDBTablesCreated = true;
            }
            catch(SQLException sqle)
            {
                //only close if an error occurs
                try
                {
                    conn.close();
                } catch (SQLException e)
                {}
                
                s_logger.log(Level.SEVERE, sqle.getMessage(), sqle);
                throw new IllegalStateException("Could not create tables");
            }
            
        }
        
    }

    /**
     * Set up folders and environment variables for database
     */
    private static void ensureDbIsSetup()
    {
        synchronized(s_dbSetupLock)
        {
            if(s_isDbSetup)
                return;
            
            //setup the derby location
            System.getProperties().setProperty("derby.system.home", getCurrentDataBaseDir().getAbsolutePath());

            //load the driver
            try
            {
                String driver = "org.apache.derby.jdbc.EmbeddedDriver";
                Class.forName(driver).newInstance();
            } catch (Exception e)
            {
                s_logger.log(Level.SEVERE, e.getMessage() ,e);
                throw new Error("Could not load db driver");
            }
         
            //shut the database down on finish
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
                    {

                        public void run()
                        {
                            shutDownDB();
                        }}));
            
            s_isDbSetup = true;
        }
        
        //we want to backup the database on occassion
        Thread backupThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(true)
                {
                    
                    //wait 7 days
                    try
                    {
                        Thread.sleep(7 * 24 * 60 * 60 * 1000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    backup();
                    
                }
                
            }
        
        }, "TripleA Database Backup Thread");
        backupThread.setDaemon(true);
        backupThread.start();
        
    
    }
    
    
    /**
     * This must be the first db call made.
     * 
     * Run Database as a main method to run the backup.
     * 
     * @param backupDir
     * @throws SQLException
     */
    public static void restoreFromBackup(File backupDir) throws SQLException
    {
        //http://www-128.ibm.com/developerworks/db2/library/techarticle/dm-0502thalamati/
        String url = "jdbc:derby:ta_users;restoreFrom="+ backupDir.getAbsolutePath() ;

        Properties props = getDbProps();
        
        Connection con = DriverManager.getConnection(url, props);
        con.close();
    }

    private static Properties getDbProps()
    {
        Properties props = new Properties();
        props.put("user", "user1");
        props.put("password", "user1");
        return props;
    }
    
    public static void backup()
    {
        String backupDirName = "backup_at_" + new SimpleDateFormat("yyyy_MM_dd__kk_mm_ss").format(new java.util.Date());
        
        File backupRootDir = getBackupDir();
        File backupDir = new File(backupRootDir, backupDirName);
        
        if(!backupDir.mkdirs())
        {
            s_logger.severe("Could not create backup dir" + backupDirName);
            return;
        }
        
        s_logger.log(Level.INFO, "Backing up database to " + backupDir.getAbsolutePath());
        
        Connection con = getConnection();
        try
        {
        
            //http://www-128.ibm.com/developerworks/db2/library/techarticle/dm-0502thalamati/
            String sqlstmt = "CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)";
            CallableStatement cs = con.prepareCall(sqlstmt); 
            cs.setString(1, backupDir.getAbsolutePath());
            cs.execute(); 
            cs.close();
        }
        catch(Exception e)
        {
            s_logger.log(Level.SEVERE, "Could not back up database", e);
        }
        finally
        {
            try
            {
                con.close();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        }
        
        s_logger.log(Level.INFO, "Done backing up database");
        
    }

    public static File getBackupDir()
    {
        return new File(getDBRoot(), "backups");
    }
    
    private static void shutDownDB()
    {
        try
        {
            DriverManager.getConnection("jdbc:derby:ta_users;shutdown=true");
        }
        catch (SQLException se)
        {
            if(se.getErrorCode() != 45000)
                s_logger.log(Level.WARNING, se.getMessage(), se);
        }
        s_logger.info("Databse shut down");

    }
    
    /**
     * 
     * Restore the database.
     */
    public static void main(String[] args)
    {

            ensureDbIsSetup();
            JFileChooser chooser = new JFileChooser(Database.getBackupDir()) ;
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            
            int rVal = chooser.showOpenDialog(null);

            if(rVal == JFileChooser.APPROVE_OPTION)
            {
                File f = chooser.getSelectedFile();
                if(!f.exists() && f.isDirectory())
                    throw new IllegalStateException("Does not exist, or not a directory");
                
                try
                {
                    Database.restoreFromBackup(chooser.getSelectedFile());
                    
                }
                catch(SQLException sqle)
                {
                    JOptionPane.showMessageDialog(null, sqle.getMessage());
                    sqle.printStackTrace();
                }
            }
            
        
    }
}
