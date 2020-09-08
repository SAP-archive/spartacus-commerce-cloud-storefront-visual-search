/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.cron;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexerService;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerCronJobModel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class VisualSearchIndexerJobTest
{
	@Mock
	private VisualSearchIndexerService visualSearchIndexerService;

	private VisualSearchIndexerCronJobModel cronJob;
	private VisualSearchConfigModel visualSearchConfig;
	private VisualSearchIndexerJob indexerJob;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		indexerJob = new VisualSearchIndexerJob();
		indexerJob.setVisualSearchIndexerService(visualSearchIndexerService);

		cronJob = new VisualSearchIndexerCronJobModel();
		visualSearchConfig = new VisualSearchConfigModel();
	}

	@Test
	public void performWhenConfigIsNotSet()
	{
		// given

		// when
		final PerformResult result = indexerJob.perform(cronJob);

		// then
		assertEquals(CronJobResult.FAILURE, result.getResult());
		assertEquals(CronJobStatus.ABORTED, result.getStatus());
	}

	@Test
	public void performWhenSuccess() throws Exception
	{
		// given
		cronJob.setVisualSearchConfig(visualSearchConfig);
		willDoNothing().given(visualSearchIndexerService).performVisualSearchIndex(visualSearchConfig);

		// when
		final PerformResult result = indexerJob.perform(cronJob);

		assertEquals(CronJobResult.SUCCESS, result.getResult());
		assertEquals(CronJobStatus.FINISHED, result.getStatus());
	}

	@Test
	public void performWhenException() throws Exception
	{
		// given
		cronJob.setVisualSearchConfig(visualSearchConfig);
		willThrow(VisualSearchIndexerException.class).given(visualSearchIndexerService)
				.performVisualSearchIndex(visualSearchConfig);

		// when
		final PerformResult result = indexerJob.perform(cronJob);

		assertEquals(CronJobResult.FAILURE, result.getResult());
		assertEquals(CronJobStatus.ABORTED, result.getStatus());
	}
}
