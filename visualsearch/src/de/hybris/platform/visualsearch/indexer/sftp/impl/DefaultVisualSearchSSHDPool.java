/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.sftp.impl;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNullStandardMessage;

import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.sftp.VisualSearchSSHDPool;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class DefaultVisualSearchSSHDPool implements VisualSearchSSHDPool, AutoCloseable
{
	private static final Logger LOG = Logger.getLogger(DefaultVisualSearchSSHDPool.class);

	private static GenericObjectPool<ChannelSftp> pool;
	private static Session session;

	protected synchronized void createPool(final VisualSearchServerConfigModel serverConfig) throws VisualSearchIndexerException
	{
		getOrCreateSession(serverConfig);
		if ((pool == null) || pool.isClosed())
		{
			LOG.info("Creating pool");
			final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
			poolConfig.setMaxTotal(serverConfig.getMaxTotalConnections());
			pool = new GenericObjectPool<ChannelSftp>(new BasePooledObjectFactory<ChannelSftp>()
			{
				@Override
				public ChannelSftp create() throws JSchException
				{
					LOG.info("Creating channel");
					final ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
					channel.connect();
					return channel;
				}

				@Override
				public PooledObject<ChannelSftp> wrap(final ChannelSftp channelSftp)
				{
					return new DefaultPooledObject<ChannelSftp>(channelSftp);
				}

				@Override
				public void destroyObject(final PooledObject<ChannelSftp> wrapper)
				{
					if (wrapper.getObject().isConnected())
					{
						LOG.info("Destroying connected channel");
						wrapper.getObject().disconnect();
					}
				}
			}, poolConfig);
		}
	}

	protected synchronized Session getOrCreateSession(final VisualSearchServerConfigModel serverConfig)
			throws VisualSearchIndexerException
	{
		if (session != null && session.isConnected())
		{
			return session;
		}

		validateParameterNotNullStandardMessage("visual search server configuration", serverConfig);

		try
		{
			LOG.info("Creating session");
			final JSch jsch = new JSch();
			session = jsch.getSession(serverConfig.getUsername(), serverConfig.getUrl(), serverConfig.getPort());
			session.setPassword(serverConfig.getPassword());

			final java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");

			session.setConfig(config);
			session.setTimeout(60000);
			session.connect();
			return session;
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException(
					String.format("Error while connecting to sshd server [%s]:[%d] with username [%s]", serverConfig.getUrl(),
							serverConfig.getPort(), serverConfig.getUsername()),
					e);
		}
	}

	@Override
	public void close() throws Exception
	{
		LOG.info("Pool close");
		pool.clear();
		pool.close();
		if (session != null)
		{
			session.disconnect();
			session = null;
		}
	}

	@Override
	public ChannelSftp getConnection(final VisualSearchServerConfigModel serverConfig) throws VisualSearchIndexerException
	{
		try
		{
			createPool(serverConfig);
			final ChannelSftp channelSftp = pool.borrowObject();
			if (!channelSftp.isConnected())
			{
				channelSftp.connect();
			}
			return channelSftp;
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException("Cannot obtain sftp session ", e);
		}
	}

	@Override
	public void returnConnection(final ChannelSftp channelSftp) throws VisualSearchIndexerException
	{
		try
		{
			LOG.info("Return connection");
			pool.returnObject(channelSftp);
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException("Cannot return sftp session to the pool", e);
		}
	}

	@Override
	public void invalidateAll() throws VisualSearchIndexerException
	{
		try
		{
			close();
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException("Cannot closing sftp session pool", e);
		}
	}

	@Override
	public void invalidateObject(final ChannelSftp channelSftp) throws VisualSearchIndexerException
	{
		try
		{
			pool.invalidateObject(channelSftp);
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException("Cannot invalidate connection", e);
		}
	}
}
