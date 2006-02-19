/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * Connection.java
 * 
 * 
 * This is the code that handles writing and reading objects over the socket.
 * Each connection handles the threads and communications for 1 socket to 1
 * remote party.
 * 
 * Connections write objects in the order that they are sent, and read them in
 * the order that they arrive. Messages are sent to our message listener using
 * the same thread used to read the message over the network.
 * 
 * @author Sean Bridges Created on December 11, 2001, 8:23 PM
 */

package games.strategy.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.util.concurrent.LinkedBlockingQueue;

class Connection
{
    private static Logger s_logger = Logger.getLogger(Connection.class.getName());
    private Socket m_socket;
    
    private OutputStream m_socketOut;
    private InputStream m_socketIn;
    
    
    private ObjectOutputStream m_out;
    private ObjectInputStream m_in;
    private volatile boolean m_shutdown = false;
    private IConnectionListener m_listener;
    private INode m_localNode;
    private INode m_remoteNode;
    private Thread m_reader;
    private Thread m_writer;

    private final LinkedBlockingQueue<MessageHeader> m_waitingToBeSent = new LinkedBlockingQueue<MessageHeader>();
    private IObjectStreamFactory m_objectStreamFactory;

    
    private long m_totalRead = 0;
    private long m_totatlWriten = 0;

    public Connection(Socket s, INode ident, IConnectionListener listener, IObjectStreamFactory fact) throws IOException
    {
        m_objectStreamFactory = fact;
        init(s, ident, listener);
    }

    public void log(MessageHeader header, boolean read)
    {
        //we measure the size of the object by serializing it into a byte array.
        //this is a very expensive operation, so make sure we are really logging before we do it
        if (!s_logger.isLoggable(Level.FINEST))
            return;

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try
        {
            ObjectOutputStream out = m_objectStreamFactory.create(sink);
            out.writeObject(header);
            sink.close();
            int size = sink.toByteArray().length;
            
            if(read)
              m_totalRead += size;
            else
              m_totatlWriten += size;
            
            String message = (read ? "READ:" : "WRITE:") + header.getMessage() + " size:" + size + " total:" + (read ? m_totalRead : m_totatlWriten);
            s_logger.log(Level.FINEST, message);

        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    public void setRemoteName(String name)
    {
        send(new MessageHeader(m_remoteNode,  new NodeNameChange(name)));
        ((Node)  m_remoteNode).setName(name);
    }

    private void init(Socket s, INode ident, IConnectionListener listener) throws IOException
    {
        m_socket = s;
        m_localNode = ident;
        m_listener = listener;

        //create the output
        m_socketOut = m_socket.getOutputStream();
        BufferedOutputStream bufferedOut = new BufferedOutputStream(m_socketOut);
        m_out = m_objectStreamFactory.create(bufferedOut);

        //write out our identity
        m_out.writeObject(m_localNode);
        m_out.flush();

        //create the input
        m_socketIn = m_socket.getInputStream();
        BufferedInputStream bufferedIn = new BufferedInputStream(m_socketIn);
        m_in = m_objectStreamFactory.create(bufferedIn);

        //read the remote connections identity
        try
        {
            m_remoteNode = (INode) m_in.readObject();
        } catch (ClassNotFoundException cnfe)
        {
            //should never happen
            cnfe.printStackTrace();
            throw new IllegalStateException("INode class not found");
        }

        m_reader = new Thread(new Reader(), "ConnectionReader for " + m_localNode.getName() + ":" + m_localNode.getAddress());

        m_reader.start();

        m_writer = new Thread(new Writer(), "ConnectionWriter for " + m_localNode.getName() + ":" + m_localNode.getAddress());
        m_writer.start();
    }

    /**
     * Blocks until no more data remains to be written or the socket is
     * shutdown.
     */
    public void flush()
    {
        //TODO - this returns when the queue is empty
        //we should also block if the writer is sending data

        if (m_shutdown)
            return;

        Object lock = new Object();
        synchronized (lock)
        {
            while (!m_shutdown && !m_waitingToBeSent.isEmpty())
            {
                try
                {
                    lock.wait(50);
                } catch (InterruptedException ie)
                {
                }
            }
        }
    }

    public INode getLocalNode()
    {
        return m_localNode;
    }

    public INode getRemoteNode()
    {
        return m_remoteNode;
    }

    /**
     * Write the MessageHeader over the network. Returns immediately.
     * 
     * @param msg
     */
    public void send(MessageHeader msg)
    {
        m_waitingToBeSent.offer(msg);
    }

    public boolean shutDown()
    {
            if (!m_shutdown)
            {
                m_shutdown = true;
                try
                {
                    try
                    {
                        //shutting down the input and output
                        //wakes up the thread at the other side
                        m_socket.shutdownInput();
                        m_socket.shutdownOutput();
                        m_socketIn.close();
                        m_socketOut.close();
                    } catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                    
                    m_socket.close();
                    m_writer.interrupt();
                    
                    if(!m_socket.isClosed())
                        throw new IllegalStateException("Not closed");
                    
                    if(!m_socket.isOutputShutdown())
                        throw new IllegalStateException("Output not closed");
                    
                    if(!m_socket.isInputShutdown())
                        throw new IllegalStateException("Input not closed");                    
                    
                    
                    return true;
                } catch (Exception e)
                {
                    System.err.println("Exception shutting down");
                    e.printStackTrace();
                }
            }
            return false;
    }

    public boolean isConnected()
    {
        return !m_shutdown;
    }

    private void messageReceived(MessageHeader obj)
    {
        if(obj.getMessage() instanceof NodeNameChange)
        {
            ((Node) m_localNode).setName( ((NodeNameChange) obj.getMessage()).getNewName() );
        }
        if (obj != null)
            m_listener.messageReceived(obj, this);
    }

    class Writer implements Runnable
    {

        public void run()
        {
            while (!m_shutdown)
            {
                MessageHeader next;
                try
                {
                    next = (MessageHeader) m_waitingToBeSent.take();
                    if(next == null)
                        continue;
                } catch (InterruptedException e)
                {
                    continue;
                }
                
                write(next);
                log(next, false);
                
                
            }
            
        }
                

            
        private void write(MessageHeader next)
        {
            if (!m_shutdown)
            {
                try
                {
                    m_out.writeObject(next);
                    m_out.flush();
                    m_out.reset();
                } catch (IOException ioe)
                {
                    if(ioe instanceof ObjectStreamException)
                        System.err.println("Error writing:" + next);
                    if (!m_shutdown)
                    {
                        ioe.printStackTrace();
                        if(shutDown())
                        {
                            List<MessageHeader> unsent = new ArrayList<MessageHeader>(m_waitingToBeSent);
                            unsent.add(next);
                            m_listener.fatalError(ioe, Connection.this, unsent);
                        }
                    }
                }
            }
        }
    }

    class Reader implements Runnable
    {

        @SuppressWarnings("unchecked")
        public void run()
        {
            
            while (!m_shutdown)
            {
                try
                {
                    final MessageHeader msg = (MessageHeader) m_in.readObject();
                    log(msg, true);
                    messageReceived(msg);

                } catch (ClassNotFoundException cnfe)
                {
                    //should never happen
                    cnfe.printStackTrace();
                } catch (IOException ioe)
                {
                    if (!m_shutdown)
                    {
                        if(ioe instanceof EOFException)
                        {
                            //these normally occur when the socket is closed
                            //ignore
                        }
                        else if(ioe instanceof SocketException)
                        {
                           if(ioe.getMessage().equals("Connection reset") || ioe.getMessage().equals("Socket closed"))
                           {
                               //ignore
                           }
                           else
                           {
                               ioe.printStackTrace();
                           }
                        }
                        else
                        {
                            ioe.printStackTrace();
                        }
                        
                        if(shutDown())
                        {
                            List<MessageHeader> unsent = new ArrayList(Arrays.asList(m_waitingToBeSent.toArray()));
                            m_listener.fatalError(ioe, Connection.this, unsent);
                        }
                    }
                }
            }
        }
    }
}

class NodeNameChange implements Serializable
{    
    private final String m_newName;
    
    NodeNameChange(String name)
    {
        m_newName = name;
    }
    
    public String getNewName()
    {
        return m_newName;
    }

}