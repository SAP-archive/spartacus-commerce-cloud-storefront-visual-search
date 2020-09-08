/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.processing.model.SimpleBatchModel;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.util.Utilities;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexer;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerDistributedProcessModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class DefaultVisualSearchIndexerBatchProcessorTest
{
	public static final String EXPORT_SUB_DIR = "visualsearch";
	private static final String VISUAL_SEARCH_CONFIG = "visualSearchConfig";
	private static final long INDEX_OPERATION_ID = 12345;
	private static final String INDEX_CONFIG_QUERY = "query";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private GenericDao<VisualSearchConfigModel> visualSearchConfigGenericDao;
	@Mock
	private FlexibleSearchService flexibleSearchService;
	@Mock
	private VisualSearchIndexer visualSearchIndexer;

	@Mock
	private SimpleBatchModel inputBatch;

	private String filePath;
	private List<ItemModel> items;
	private VisualSearchConfigModel visualSearchConfig;
	private DefaultVisualSearchIndexerBatchProcessor indexerBatchProcessor;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		indexerBatchProcessor = new DefaultVisualSearchIndexerBatchProcessor();
		indexerBatchProcessor.setFlexibleSearchService(flexibleSearchService);
		indexerBatchProcessor.setVisualSearchConfigGenericDao(visualSearchConfigGenericDao);
		indexerBatchProcessor.setVisualSearchIndexer(visualSearchIndexer);

		final String platformTempDir = Utilities.getPlatformConfig().getSystemConfig().getTempDir().getPath() + File.separator
				+ EXPORT_SUB_DIR;
		final VisualSearchIndexerDistributedProcessModel distributedProcess = new VisualSearchIndexerDistributedProcessModel();
		distributedProcess.setVisualSearchConfig(VISUAL_SEARCH_CONFIG);
		distributedProcess.setIndexOperationId(INDEX_OPERATION_ID);
		distributedProcess.setExportPath(platformTempDir);
		given(inputBatch.getProcess()).willReturn(distributedProcess);
		given(inputBatch.getContext()).willReturn(new ArrayList());

		filePath = getExpectedFileName(platformTempDir);

		visualSearchConfig = new VisualSearchConfigModel();
		visualSearchConfig.setName(VISUAL_SEARCH_CONFIG);

		final VisualSearchIndexConfigModel indexConfig = new VisualSearchIndexConfigModel();
		indexConfig.setUser(new UserModel());
		indexConfig.setQuery(INDEX_CONFIG_QUERY);
		indexConfig.setCatalogVersion(new CatalogVersionModel());
		visualSearchConfig.setIndexConfig(indexConfig);

		given(visualSearchConfigGenericDao.find(any(Map.class))).willReturn(Collections.singletonList(visualSearchConfig));

		items = new ArrayList<>();
		final SearchResult searchResult = mock(SearchResult.class);
		given(searchResult.getResult()).willReturn(items);
		given(flexibleSearchService.search(any(FlexibleSearchQuery.class))).willReturn(searchResult);

	}

	@Test
	public void processIndexerSuccess() throws Exception
	{
		// given
		willDoNothing().given(visualSearchIndexer).generateDataFeed(items, visualSearchConfig, filePath);
		willDoNothing().given(visualSearchIndexer).uploadDataFeed(filePath, visualSearchConfig);

		// when
		indexerBatchProcessor.process(inputBatch);

		// then
		verify(visualSearchIndexer, times(1)).generateDataFeed(items, visualSearchConfig, filePath);
		verify(visualSearchIndexer, times(1)).uploadDataFeed(filePath, visualSearchConfig);
	}

	@Test
	public void processIndexerUploadFailureNoRetry() throws Exception
	{
		// given
		visualSearchConfig.getIndexConfig().setMaxBatchRetries(0);
		willDoNothing().given(visualSearchIndexer).generateDataFeed(items, visualSearchConfig, filePath);
		willThrow(VisualSearchIndexerException.class).given(visualSearchIndexer).uploadDataFeed(filePath, visualSearchConfig);

		// expect
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Upload of datafeed " + filePath + " has failed.");

		// when
		indexerBatchProcessor.process(inputBatch);

		// then
		verify(visualSearchIndexer, times(1)).generateDataFeed(items, visualSearchConfig, filePath);
		verify(visualSearchIndexer, times(1)).uploadDataFeed(filePath, visualSearchConfig);
	}

	@Test
	public void processIndexerUploadFailureWithRetry() throws Exception
	{
		// given
		visualSearchConfig.getIndexConfig().setMaxBatchRetries(2);
		willDoNothing().given(visualSearchIndexer).generateDataFeed(items, visualSearchConfig, filePath);
		willThrow(VisualSearchIndexerException.class).given(visualSearchIndexer).uploadDataFeed(filePath, visualSearchConfig);

		// expect
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Upload of datafeed " + filePath + " has failed.");

		// when
		indexerBatchProcessor.process(inputBatch);

		// then
		verify(visualSearchIndexer, times(1)).generateDataFeed(items, visualSearchConfig, filePath);
		verify(visualSearchIndexer, times(3)).uploadDataFeed(filePath, visualSearchConfig);
	}

	private String getExpectedFileName(final String platformTempDir)
	{
		final String fileName = DefaultVisualSearchIndexerBatchProcessor.VS_FILE_PREFIX + "_" + String.valueOf(INDEX_OPERATION_ID)
				+ "_1.csv";
		return platformTempDir + File.separator + fileName;
	}

}
