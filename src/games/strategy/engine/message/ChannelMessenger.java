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

package games.strategy.engine.message;

import java.io.Serializable;
import java.lang.reflect.*;
import java.lang.reflect.InvocationHandler;
import java.util.*;
import java.util.HashMap;

import games.strategy.net.*;
import games.strategy.net.IChannelMessenger;
import games.strategy.net.IChannelSubscribor;
import games.strategy.net.IMessenger;

/**
 * Implementation of IChannelMessenger built on top of an IMessenger
 * 
 * @author Sean Bridges
 */
public class ChannelMessenger implements IChannelMessenger
{
    //the messenger upon which we are built
    private final IMessenger m_messenger;
    //maps channelName as String -> Channel
    private final Map m_channels = Collections.synchronizedMap(new HashMap());
    
    public ChannelMessenger(IMessenger messenger)
    {
        m_messenger = messenger;
        m_messenger.addMessageListener(m_messageListener);
        m_messenger.broadcast(new ChannelRequestInit());
    }
    
    private void assertChannelExists(String channelName)
    {
        if(!m_channels.containsKey(channelName))
            throw new IllegalStateException("No channel called " + channelName);
    }
    
    /* 
     * @see games.strategy.net.IChannelMessenger#getChannelBroadcastor(java.lang.String)
     */
    public IChannelSubscribor getChannelBroadcastor(String channelName)
    {
        //return an IChannelSubscribor  that knows how to call the methods correctly
        assertChannelExists(channelName);
     
        Channel channel = (Channel) m_channels.get(channelName);
        InvocationHandler ih = new ChannelProxyInvocationHandler(channel, m_messenger);
        
        IChannelSubscribor rVal = (IChannelSubscribor) Proxy.newProxyInstance(
                  Thread.currentThread().getContextClassLoader(), 
                  new Class[] {channel.getChannelInterface()}, ih );
        
        return rVal;
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#registerChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void registerChannelSubscriber(Object implementor, String channelName)
    {
        assertChannelExists(channelName);
        Channel channel = (Channel) m_channels.get(channelName);
        channel.addSubscribor(implementor);
    }


    /* (non-Javadoc)
     * @see games.strategy.net.IChannelMessenger#unregisterChannelSubscriber(java.lang.Object, java.lang.String)
     */
    public void unregisterChannelSubscriber(Object implementor, String channelName)
    {
        Channel channel = (Channel) m_channels.get(channelName);
        if(channel == null)
            return;
        channel.removeSubscribor(implementor);
    }    
    
    /* 
     * @see games.strategy.net.IChannelMessenger#createChannel(java.lang.Class, java.lang.String)
     */
    public void createChannel(Class channelInterface, String channelName)
    {
        m_messenger.broadcast(new ChannelCreated(channelName, channelInterface.getName()));
        createChannelInternal(channelInterface, channelName);
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#destroyChannel(java.lang.String)
     */
    public void destroyChannel(String channelName)
    {
        assertChannelExists(channelName);
        m_messenger.broadcast(new ChannelDestroyed(channelName));
        m_channels.remove(channelName);
    }

    /**
     * @param creationMsg
     */
    private void createChannelInternal(Class channelInterface, String channelName)
    {
        if(! IChannelSubscribor.class.isAssignableFrom(channelInterface))
            throw new IllegalArgumentException(channelInterface.getName() +  " does not implement IChannelSubscribor");
        if(!channelInterface.isInterface())
            throw new IllegalArgumentException(channelInterface.getName() +  " must be an interface");

        Channel channel = new Channel(channelInterface, channelName);
        
        m_channels.put(channelName, channel);
    }
    
     
    IMessageListener m_messageListener = new IMessageListener()
    {
        public void messageReceived(Serializable msg, INode from)
        {
            if(msg instanceof ChannelCreated)
            {
                ChannelCreated creationMsg = (ChannelCreated) msg;
                
                Class channelInterface;
                try
                {
                    channelInterface = Class.forName(creationMsg.m_className);
                } catch (ClassNotFoundException e)
                {
                    //Should never happen
                    e.printStackTrace();
                    return;
                }
                
                createChannelInternal(channelInterface, creationMsg.m_channelName);
            }
            else if(msg instanceof ChannelDestroyed)
            {
                ChannelDestroyed destroyMsg = (ChannelDestroyed) msg;
                m_channels.remove(destroyMsg.m_channelName);
            }
            else if( msg instanceof RemoteMethodCall)
            {
                //a remote method has been called somewhere,
                //invoke locally

                RemoteMethodCall methodCall = (RemoteMethodCall) msg;
                
                assertChannelExists(methodCall.getRemoteName());
                Channel channel = (Channel) m_channels.get(methodCall.getRemoteName());
                Class[] argTypes = methodCall.getArgTypes();
                channel.invokeLocal(methodCall.getMethodName(), argTypes, methodCall.getArgs() );
            }
            else if (msg instanceof ChannelRequestInit)
            {
                //no need for everyone to respond
                if(!m_messenger.isServer())
                    return;
                Map channels = new HashMap();
                Iterator iter = m_channels.keySet().iterator();
                while(iter.hasNext())
                {
                    String name = (String) iter.next();
                    channels.put(name, ((Channel) m_channels.get(name)).getChannelInterface().getName());
                    m_messenger.send(new ChannelInit(channels), from);
                }
            }
            else if(msg instanceof ChannelInit)
            {
                ChannelInit init = (ChannelInit) msg;
                Iterator iter = init.m_channels.keySet().iterator();
                while(iter.hasNext())
                {
                    String name = (String) iter.next();
                    Class channelInterface;
                    try
                    {
                        channelInterface = Class.forName( (String) init.m_channels.get(name) );
                    } catch (ClassNotFoundException e)
                    {
                        // should never happen
                        e.printStackTrace();
                        throw new IllegalStateException(e.getMessage());
                    }
                    createChannelInternal(channelInterface, name);
                }
                
            }
        }
        
    };

    /* 
     * @see games.strategy.net.IChannelMessenger#getLocalSubscriborCount(java.lang.String)
     */
    public int getLocalSubscriborCount(String channelName)
    {
        Channel channel = (Channel) m_channels.get(channelName);
        if(channel == null)
            return 0;
        return channel.getSubscribors().size();
        
    }

    /* 
     * @see games.strategy.net.IChannelMessenger#hasChannel(java.lang.String)
     */
    public boolean hasChannel(String channelName)
    {
       return m_channels.containsKey(channelName);
    }

      
}

/**
 * The actual channel itself.
 * 
 */
class Channel
{
    private final Class m_channelInterface;
    private final List m_subscribors = Collections.synchronizedList(new ArrayList());
    private final String m_name;

    public Channel(Class channelInterface, String name)
    {
        m_channelInterface = channelInterface;
        m_name = name;
    }

    /**
     * @return Returns the channelInterface.
     */
    public Class getChannelInterface()
    {
        return m_channelInterface;
    }
    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return m_name;
    }
    /**
     * @return Returns the subscribors.
     */
    public List getSubscribors()
    {
        return new ArrayList(m_subscribors);
    }
    
    public void addSubscribor(Object subscribor)
    {
        if(! m_channelInterface.isAssignableFrom(subscribor.getClass())  )
            throw new IllegalArgumentException(subscribor.getClass().getName() + " does not implement " + m_channelInterface);
        
        m_subscribors.add(subscribor);
    }
    
    public void removeSubscribor(Object subscribor)
    {
        m_subscribors.remove(subscribor);
    }
    
    void invokeLocal(String methodName, Class[] argTypes, Object[] args)
    {
        Iterator iter = getSubscribors().iterator();
        while(iter.hasNext())
        {
            Object subscribor = iter.next();
            Method method;
            try
            {
                method = subscribor.getClass().getMethod(methodName, argTypes);
                //make sure we can call it
                //this may fail depending on how the security is set up
                method.setAccessible(true);
            } catch (SecurityException e)
            {
                e.printStackTrace();
                //oh well, we tried
                continue;
            } catch (NoSuchMethodException e)
            {
                //should never happen
                e.printStackTrace();
                continue;
            }
            try
            {
                method.invoke(subscribor, args);
            }
            //we cannot return the thrown exception to the caller, 
            //simply print to standard error
            catch (Throwable invocationError)
            {
                invocationError.printStackTrace();
            }      
        }
    }
}

/**
 * Broadcast when a channel is created
 */
class ChannelCreated implements Serializable
{
    public final String m_channelName;
    public final String m_className;
    
    /**
     * @param channelName
     * @param className
     */
    public ChannelCreated(String channelName, String className)
    {
        this.m_channelName = channelName;
        this.m_className = className;
    }
}

/**
 * A response to a channels request to be initialized
 */
class ChannelInit implements Serializable
{

    //maps String -> Class Name
    Map m_channels;
    
    public ChannelInit(Map channels)
    {
        m_channels = channels;
    }

}

/**
 * A channelMessenger has just joined the network and request
 * to be initialized
 */
class ChannelRequestInit implements Serializable
{
    
}

/**
 * Broadcasted when a channel is destroyed
 */
class ChannelDestroyed implements Serializable
{
    public final String m_channelName;
    
    public ChannelDestroyed(String channelName)
    {
        m_channelName = channelName;
    }
}

/**
 * Handles the invocation for a channel
 */
class ChannelProxyInvocationHandler implements InvocationHandler
{
    private final Channel m_channel;
    private final IMessenger m_messenger;
    
   
    public ChannelProxyInvocationHandler(final Channel channel,
            final IMessenger messenger)
    {
        m_channel = channel;
        m_messenger = messenger;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        //do the remote invocation first
        //allows the objects to start travelling over the web as soon as possible
        //without being delayed by what may be a length local invocation
        RemoteMethodCall remoteMethodMsg = new RemoteMethodCall(m_channel.getName(), method.getName(), args, method.getParameterTypes());
        m_messenger.broadcast(remoteMethodMsg);
        
        m_channel.invokeLocal(method.getName(), method.getParameterTypes(), args);
        return null;
    }
    
}

