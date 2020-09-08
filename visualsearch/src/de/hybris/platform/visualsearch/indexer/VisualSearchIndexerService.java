/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer;

import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;


/**
 * Implementations of this interface should be responsible for visual search indexing.
 */
public interface VisualSearchIndexerService
{
	/**
	 *
	 * @param visualSearchConfig
	 *           visual search configuration
	 * @throws VisualSearchIndexerException
	 *            an error during indexing process
	 */
	void performVisualSearchIndex(final VisualSearchConfigModel visualSearchConfig) throws VisualSearchIndexerException;
}
