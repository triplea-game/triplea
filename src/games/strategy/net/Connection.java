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
import java.net.Socket;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Logger;

class Connection
{
    private Socket m_socket;
    private ObjectOutputStream m_out;
    private ObjectInputStream m_in;
    private volatile boolean m_shutdown = false;
    private IConnectionListener m_listener;
    private INode m_localNode;
    private INode m_remoteNode;
    private Thread m_reader;
    private Thread m_writer;
    //all adding and removing from this list must be synchronized
    //on the list object
    private final List m_waitingToBeSent = Collections.synchronizedList(new LinkedList());
    private IObjectStreamFactory m_objectStreamFactory;
    private final Object m_flushLock = new Object();

    //used to notify writer thread that an object is ready to be written
    private final Object m_writeWaitLock = new Object();

    public Connection(Socket s, INode ident, IConnectionListener listener, IObjectStreamFactory fact) throws IOException
    {
        m_objectStreamFactory = fact;
        init(s, ident, listener);
    }

    public void log(MessageHeader header, boolean read)
    {

        Logger logger = Logger.getLogger(this.getClass().getName());
        if (!logger.isLoggable(Level.FINEST))
            return;

        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        try
        {
            ObjectOutputStream out = m_objectStreamFactory.create(sink);
            out.writeObject(header);
            sink.close();
            String message = (read ? "READ:" : "WRITE:") + header.getMessage() + " size:" + sink.toByteArray().length;
            logger.log(Level.FINEST, message);

        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Creates new Connection s must be open.
     */
    public Connection(Socket s, INode ident, IConnectionListener listener) throws IOException
    {
        m_objectStreamFactory = new DefaultObjectStreamFactory();
        init(s, ident, listener);
    }

    private void init(Socket s, INode ident, IConnectionListener listener) throws IOException
    {
        m_socket = s;
        m_localNode = ident;
        m_listener = listener;

        //create the output
        BufferedOutputStream bufferedOut = new BufferedOutputStream(m_socket.getOutputStream());
        m_out = m_objectStreamFactory.create(bufferedOut);

        //write out our identity
        m_out.writeObject(m_localNode);
        m_out.flush();

        //create the input
        BufferedInputStream bufferedIn = new BufferedInputStream(m_socket.getInputStream());
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

        m_reader = new Thread(new Reader(), "ConnectionReader for " + m_localNode.getName());

        m_reader.start();

        m_writer = new Thread(new Writer(), "ConnectionWriter for" + m_localNode.getName());
        m_writer.start();
    }

    /**
     * Blocks until no more data remains to be written or the socket is
     * shutdown.
     */
    public void flush()
    {
        if (m_shutdown)
            return;

        synchronized (m_flushLock)
        {
            while (!m_shutdown && !m_waitingToBeSent.isEmpty())
            {
                try
                {
                    m_flushLock.wait();
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
        m_waitingToBeSent.add(msg);
        synchronized (m_writeWaitLock)
        {
            m_writeWaitLock.notifyAll();
        }
    }

    public void shutDown()
    {
        synchronized (m_flushLock)
        {
            if (!m_shutdown)
            {
                m_shutdown = true;
                try
                {
                    m_socket.close();
                    m_flushLock.notifyAll();

                } catch (Exception e)
                {
                    System.err.println("Exception shutting down");
                    e.printStackTrace();
                }
            }
        }

    }

    public boolean isConnected()
    {
        return !m_shutdown;
    }

    private void messageReceived(Serializable obj)
    {
        if (obj != null)
            m_listener.messageReceived(obj, this);
    }

    class Writer implements Runnable
    {

        public void run()
        {
            while (!m_shutdown)
            {
                if (!m_waitingToBeSent.isEmpty())
                {
                    MessageHeader next = (MessageHeader) m_waitingToBeSent.get(0);
                    write(next);
                    log(next, false);

                    m_waitingToBeSent.remove(0);

                    /**
                     * flush() may need to be woken up
                     */
                    synchronized (m_flushLock)
                    {
                        if (m_waitingToBeSent.isEmpty())
                        {
                            m_flushLock.notifyAll();
                        }
                    }
                } else
                {
                    try
                    {
                        //the stream keeps a memory of objects that have been
                        // written to the
                        //stream, preventing them from being gc'd. reset stream
                        // when we
                        //are out of things to send
                        try
                        {
                            m_out.reset();
                        } catch (IOException ioe)
                        {
                            ioe.printStackTrace();
                        }
                        synchronized (m_writeWaitLock)
                        {
                            m_writeWaitLock.wait();
                        }
                    } catch (InterruptedException ie)
                    {
                    }
                }

            }
        }

        private void write(Serializable next)
        {
            if (!m_shutdown)
            {
                try
                {
                    m_out.writeObject(next);
                    m_out.flush();
                } catch (IOException ioe)
                {
                    if(ioe instanceof ObjectStreamException)
                        System.err.println("Error writing:" + next);
                    if (!m_shutdown)
                    {
                        ioe.printStackTrace();
                        shutDown();
                        List unsent = new ArrayList(m_waitingToBeSent);
                        unsent.add(next);
                        m_listener.fatalError(ioe, Connection.this, unsent);
                    }
                }
            }
        }
    }

    class Reader implements Runnable
    {

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
                    cnfe.printStackTrace();
                } catch (IOException ioe)
                {
                    if (!m_shutdown)
                    {
                        //these normally occur when the socket is closed
                        //ignore
                        if (!(ioe instanceof EOFException))
                            ioe.printStackTrace();
                        shutDown();
                        List unsent = new ArrayList(m_waitingToBeSent);
                        m_listener.fatalError(ioe, Connection.this, unsent);
                    }
                }
            }
        }
    }
}