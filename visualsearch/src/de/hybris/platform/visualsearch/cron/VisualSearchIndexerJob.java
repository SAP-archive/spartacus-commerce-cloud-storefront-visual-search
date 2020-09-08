/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.cron;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexerService;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerCronJobModel;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;


public class VisualSearchIndexerJob extends AbstractJobPerformable<VisualSearchIndexerCronJobModel>
{
	private static final Logger LOG = Logger.getLogger(VisualSearchIndexerJob.class);

	private VisualSearchIndexerService visualSearchIndexerService;

	@Override
	public PerformResult perform(final VisualSearchIndexerCronJobModel cronJob)
	{
		try
		{
			final VisualSearchConfigModel visualSearchConfig = cronJob.getVisualSearchConfig();
			if (visualSearchConfig == null)
			{
				LOG.error("Visual search config is not defined.");
				return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
			}

			visualSearchIndexerService.performVisualSearchIndex(visualSearchConfig);

		}
		catch (final VisualSearchIndexerException e)
		{
			LOG.error("Error running visual search indexing", e);
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		}

		return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
	}

	@Required
	public void setVisualSearchIndexerService(final VisualSearchIndexerService visualSearchIndexerService)
	{
		this.visualSearchIndexerService = visualSearchIndexerService;
	}

}
