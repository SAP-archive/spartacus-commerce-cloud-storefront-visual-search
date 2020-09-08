/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.processing.distributed.simple.SimpleBatchProcessor;
import de.hybris.platform.processing.model.SimpleBatchModel;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.util.ServicesUtil;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexer;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerDistributedProcessModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Required;

import com.google.common.base.Preconditions;


public class DefaultVisualSearchIndexerBatchProcessor implements SimpleBatchProcessor
{
	private static final Logger LOG = Logger.getLogger(DefaultVisualSearchIndexerBatchProcessor.class);

	private static final String PARAMETER_NAME = "name";
	public static final String VS_FILE_PREFIX = "datafeed";

	private GenericDao<VisualSearchConfigModel> visualSearchConfigGenericDao;
	private FlexibleSearchService flexibleSearchService;
	private VisualSearchIndexer visualSearchIndexer;

	private final AtomicInteger counter = new AtomicInteger(1);

	@Override
	public void process(final SimpleBatchModel inputBatch)
	{
		LOG.info("Starting batch processor");
		try
		{
			final VisualSearchIndexerDistributedProcessModel distributedProcessModel = (VisualSearchIndexerDistributedProcessModel) inputBatch
					.getProcess();
			final VisualSearchConfigModel visualSearchConfig = getVisualSearchConfig(
					distributedProcessModel.getVisualSearchConfig());
			final long indexOperationId = distributedProcessModel.getIndexOperationId();
			final List<PK> pks = asList(inputBatch.getContext());

			final List<ItemModel> items = getItems(visualSearchConfig, pks);
			final String filePath = distributedProcessModel.getExportPath() + File.separator + resolveFileName(indexOperationId);

			visualSearchIndexer.generateDataFeed(items, visualSearchConfig, filePath);

			final int maxBatchRetries = visualSearchConfig.getIndexConfig().getMaxBatchRetries();

			try
			{
				visualSearchIndexer.uploadDataFeed(filePath, visualSearchConfig);
			}
			catch (final VisualSearchIndexerException e)
			{
				retryUpload(filePath, visualSearchConfig, maxBatchRetries);
			}

			try
			{
				Files.deleteIfExists(Paths.get(filePath));
			}
			catch (final IOException e)
			{
				LOG.error(String.format("File [%s] cannot be deleted!", filePath), e);
			}

		}
		catch (final VisualSearchIndexerException | SystemException | BeansException e)
		{
			throw new VisualSearchIndexerRuntimeException(e);
		}
		catch (final InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}

	protected void retryUpload(final String filePath, final VisualSearchConfigModel visualSearchConfig, final int retriesLeft)
			throws VisualSearchIndexerException, InterruptedException
	{
		LOG.info("Retry datafeed " + filePath);
		int currentRetriesLeft = retriesLeft;
		if (retriesLeft > 0)
		{
			try
			{
				visualSearchIndexer.uploadDataFeed(filePath, visualSearchConfig);
			}
			catch (final VisualSearchIndexerException e)
			{
				LOG.error(e);
				currentRetriesLeft--;
				retryUpload(filePath, visualSearchConfig, currentRetriesLeft);
			}
		}
		else
		{
			throw new VisualSearchIndexerException("Upload of datafeed " + filePath + " has failed.");
		}

	}

	protected VisualSearchConfigModel getVisualSearchConfig(final String name)
	{
		final Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(PARAMETER_NAME, name);

		final List<VisualSearchConfigModel> visualSearchConfig = visualSearchConfigGenericDao.find(paramMap);

		ServicesUtil.validateIfSingleResult(visualSearchConfig, "Visual search configuration not found: " + name,
				"More than one Visual search configuration found: " + name);

		return visualSearchConfig.iterator().next();
	}

	protected List<PK> asList(final Object ctx)
	{
		Preconditions.checkState(ctx instanceof List, "ctx must be instance of List");

		return (List<PK>) ctx;
	}

	protected String resolveFileName(final long operationId)
	{
		return VS_FILE_PREFIX + "_" + String.valueOf(operationId) + "_" + String.valueOf(counter.getAndIncrement()) + ".csv";
	}

	protected List<ItemModel> getItems(final VisualSearchConfigModel visualSearchConfig, final List<PK> pks)
	{
		final String query = "SELECT {pk} FROM {Product} where {pk} in (?pks)";
		final Map<String, Object> queryParameters = Collections.<String, Object> singletonMap("pks", pks);

		final FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(query, queryParameters);
		final VisualSearchIndexConfigModel indexConfig = visualSearchConfig.getIndexConfig();
		fsQuery.setUser(indexConfig.getUser());
		fsQuery.setCatalogVersions(Collections.singleton(indexConfig.getCatalogVersion()));
		final SearchResult<ItemModel> fsResult = flexibleSearchService.search(fsQuery);

		return fsResult.getResult();
	}

	@Required
	public void setVisualSearchConfigGenericDao(final GenericDao<VisualSearchConfigModel> visualSearchConfigGenericDao)
	{
		this.visualSearchConfigGenericDao = visualSearchConfigGenericDao;
	}

	@Required
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	@Required
	public void setVisualSearchIndexer(final VisualSearchIndexer visualSearchIndexer)
	{
		this.visualSearchIndexer = visualSearchIndexer;
	}

}
