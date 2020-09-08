/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.sftp;

import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;

import com.jcraft.jsch.ChannelSftp;


public interface VisualSearchSSHDPool
{
	/**
	 * Gets sshd connection from a pool.
	 *
	 * @return sshd session
	 * @throws VisualSearchIndexerException
	 */
	public ChannelSftp getConnection(final VisualSearchServerConfigModel serverConfig) throws VisualSearchIndexerException;

	/**
	 * Returns sshd connection to a pool.
	 *
	 * @param session
	 * @throws VisualSearchIndexerException
	 */
	public void returnConnection(final ChannelSftp channelSftp) throws VisualSearchIndexerException;

	/**
	 * Invalidates all connections and closes the pool.
	 *
	 * @throws VisualSearchIndexerException
	 */
	public void invalidateAll() throws VisualSearchIndexerException;

	/**
	 * Invalidates the connection.
	 *
	 * @param channelSftp
	 *           channel to invalidate
	 * @throws VisualSearchIndexerException
	 */
	public void invalidateObject(final ChannelSftp channelSftp) throws VisualSearchIndexerException;

}
