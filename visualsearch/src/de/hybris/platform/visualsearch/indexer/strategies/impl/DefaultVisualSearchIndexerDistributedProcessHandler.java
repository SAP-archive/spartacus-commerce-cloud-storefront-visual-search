/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.strategies.impl;

import de.hybris.platform.processing.distributed.DistributedProcessService;
import de.hybris.platform.processing.distributed.defaultimpl.DistributedProcessHandler;
import de.hybris.platform.processing.distributed.simple.SimpleBatchProcessor;
import de.hybris.platform.processing.distributed.simple.SimpleDistributedProcessHandler;
import de.hybris.platform.processing.distributed.simple.context.SimpleProcessCreationContext;
import de.hybris.platform.processing.distributed.simple.data.SimpleAbstractDistributedProcessCreationData;
import de.hybris.platform.processing.distributed.simple.id.SimpleBatchID;
import de.hybris.platform.processing.enums.BatchType;
import de.hybris.platform.processing.model.BatchModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.solrfacetsearch.indexer.strategies.impl.DefaultIndexerProcessCreationContext;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerBatchModel;


/**
 * Implementation of {@link DistributedProcessHandler} for distributed indexing.
 */
public class DefaultVisualSearchIndexerDistributedProcessHandler extends SimpleDistributedProcessHandler
{
	public DefaultVisualSearchIndexerDistributedProcessHandler(final ModelService modelService,
			final FlexibleSearchService flexibleSearchService, final DistributedProcessService distributedProcessService,
			final SimpleBatchProcessor simpleBatchProcessor)
	{
		super(modelService, flexibleSearchService, distributedProcessService, simpleBatchProcessor);
	}

	@Override
	protected SimpleProcessCreationContext prepareProcessCreationContext(
			final SimpleAbstractDistributedProcessCreationData processData)
	{
		return new DefaultIndexerProcessCreationContext(modelService, processData);
	}

	@Override
	protected BatchModel prepareResultBatch()
	{
		final VisualSearchIndexerBatchModel resultBatch = modelService.create(VisualSearchIndexerBatchModel.class);
		resultBatch.setId(SimpleBatchID.asResultBatchID().toString());
		resultBatch.setType(BatchType.RESULT);
		resultBatch.setRemainingWorkLoad(WORK_DONE);

		return resultBatch;
	}
}
