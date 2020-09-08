/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.cron;

import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.visualsearch.enums.VisualSearchSyncResult;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchSyncStatusCronJobModel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


public class VisualSearchSyncStatusJob extends AbstractJobPerformable<VisualSearchSyncStatusCronJobModel>
{
	private static final Logger LOG = Logger.getLogger(VisualSearchIndexerJob.class);

	private I18NService i18NService;

	@Override
	public PerformResult perform(final VisualSearchSyncStatusCronJobModel cronJob)
	{
		try
		{
			final VisualSearchConfigModel visualSearchConfig = cronJob.getVisualSearchConfig();
			final VisualSearchIndexSyncStatus syncStatus = getStatus(visualSearchConfig);

			if (visualSearchConfig.getLastSyncTime() == null)
			{
				updateVisualSearchConfig(visualSearchConfig, syncStatus);
			}
			else if (visualSearchConfig.getLastSyncTime() != syncStatus.getCurrent().getSync_time())
			{
				updateVisualSearchConfig(visualSearchConfig, syncStatus);
				visualSearchConfig.setStatus(VisualSearchSyncResult.SUCCESS);
				modelService.save(visualSearchConfig);

				cronJob.setActive(Boolean.FALSE);
				cronJob.setRemoveOnExit(Boolean.TRUE);
				modelService.save(cronJob);

				return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
			}

			setNextActivationTime(cronJob);
			return new PerformResult(CronJobResult.UNKNOWN, CronJobStatus.RUNNING);
		}
		catch (final VisualSearchIndexerRuntimeException e)
		{
			LOG.error("Error running visual search status check job", e);
			return new PerformResult(CronJobResult.FAILURE, CronJobStatus.ABORTED);
		}
	}

	protected VisualSearchIndexSyncStatus getStatus(final VisualSearchConfigModel visualSearchConfig)
			throws VisualSearchIndexerRuntimeException
	{
		final String url = visualSearchConfig.getServerConfig().getSyncStatusUrl();
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		final HttpEntity<Object> httpEntity = new HttpEntity<>(headers);

		final RestTemplate restTemplate = new RestTemplate();
		final ResponseEntity<VisualSearchIndexSyncStatus> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity,
				VisualSearchIndexSyncStatus.class);

		if (response.getStatusCode().is2xxSuccessful())
		{
			return response.getBody();
		}

		throw new VisualSearchIndexerRuntimeException("Error while getting datafeed synchronization status");
	}

	protected void setNextActivationTime(final VisualSearchSyncStatusCronJobModel cronJob)
	{
		TriggerModel trigger = null;
		if (!CollectionUtils.isEmpty(cronJob.getTriggers()))
		{
			trigger = cronJob.getTriggers().iterator().next();
		}
		else
		{
			trigger = modelService.create(TriggerModel.class);
		}
		trigger.setActivationTime(calculateNextActivationTime());
		trigger.setActive(Boolean.TRUE);
		cronJob.setTriggers(Collections.singletonList(trigger));
		modelService.saveAll(trigger, cronJob);
	}

	protected Date calculateNextActivationTime()
	{
		final Calendar calendar = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
		calendar.add(Calendar.MINUTE, 15);
		return new Date(calendar.getTimeInMillis());
	}

	protected void updateVisualSearchConfig(final VisualSearchConfigModel visualSearchConfig,
			final VisualSearchIndexSyncStatus syncStatus)
	{
		visualSearchConfig.setLastSyncTime(syncStatus.getCurrent().getSync_time());
		visualSearchConfig.setIndexName(syncStatus.getCurrent().getIndex_name());
		modelService.save(visualSearchConfig);
	}

	@Required
	public void setI18NService(final I18NService i18nService)
	{
		i18NService = i18nService;
	}

	public static class VisualSearchIndexSyncStatus
	{
		private VisualSearchIndexSyncInfo current;
		private VisualSearchIndexSyncInfo previous;

		public VisualSearchIndexSyncInfo getCurrent()
		{
			return current;
		}

		public void setCurrent(final VisualSearchIndexSyncInfo current)
		{
			this.current = current;
		}

		public VisualSearchIndexSyncInfo getPrevious()
		{
			return previous;
		}

		public void setPrevious(final VisualSearchIndexSyncInfo previous)
		{
			this.previous = previous;
		}
	}

	public static class VisualSearchIndexSyncInfo
	{
		private String index_name;
		private Date sync_time;

		public String getIndex_name()
		{
			return index_name;
		}

		public void setIndex_name(final String index_name)
		{
			this.index_name = index_name;
		}

		public Date getSync_time()
		{
			return sync_time;
		}

		public void setSync_time(final Date sync_time)
		{
			this.sync_time = sync_time;
		}

		public void setSync_time(final String sync_time)
		{
			try
			{
				final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				final String string = sync_time.split("\\.")[0];
				this.sync_time = dateFormat.parse(sync_time);
			}
			catch (final ParseException e)
			{
				LOG.error("Error while parsing sync time " + sync_time, e);
			}
		}
	}
}
