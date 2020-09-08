/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;

import java.util.Collection;


/**
 * Indexer delivers functionality for generating and uploading data feed for visual search.
 */
public interface VisualSearchIndexer
{
	/**
	 *
	 * @param items
	 *           items for send to indexer in one transaction
	 * @param visualSearchConfig
	 *           visual search configuration
	 * @param fileName
	 *           name of the file to be generated
	 * @return generated file name
	 *
	 * @throws VisualSearchIndexerException
	 *            if an error occurs during indexing
	 * @throws InterruptedException
	 *            if any thread interrupted the current thread before before it completed indexing. The interrupted
	 *            status of the current thread is cleared when this exception is thrown.
	 */
	public void generateDataFeed(final Collection<ItemModel> items, final VisualSearchConfigModel visualSearchConfig,
			final String fileName) throws VisualSearchIndexerException, InterruptedException;

	/**
	 * Uploads generated data feed (csv file) to a sftp server.
	 *
	 * @param fileName
	 *           name of the file to be uploaded
	 * @param visualSearchConfig
	 *           visual search configuration
	 * @throws VisualSearchIndexerException
	 *            if an error occurs during indexing
	 * @throws InterruptedException
	 *            if any thread interrupted the current thread before before it completed indexing. The interrupted
	 *            status of the current thread is cleared when this exception is thrown.
	 */
	public void uploadDataFeed(final String fileName, final VisualSearchConfigModel visualSearchConfig)
			throws VisualSearchIndexerException, InterruptedException;
}
