/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.strategies;

import de.hybris.platform.core.PK;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;

import java.util.List;


/**
 * Strategy for performing the visual search indexing process.
 */
public interface VisualSearchIndexerStrategy
{
	/**
	 *
	 * @param visualSearchConfig
	 *           visual search configuration
	 * @param pks
	 *           list of pks to be indexed
	 * @throws VisualSearchIndexerException
	 */
	public void execute(final VisualSearchConfigModel visualSearchConfig, final List<PK> pks) throws VisualSearchIndexerException;
}
