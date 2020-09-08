/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.cron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.visualsearch.cron.VisualSearchSyncStatusJob.VisualSearchIndexSyncInfo;
import de.hybris.platform.visualsearch.cron.VisualSearchSyncStatusJob.VisualSearchIndexSyncStatus;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchSyncStatusCronJobModel;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;


@UnitTest
public class VisualSearchSyncStatusJobTest
{
	private static final String NEW_INDEX_NAME = "newIndexName";
	private static final String OLD_INDEX_NAME = "oldIndexName";

	private static final String SYNC_STATUS_URL = "http://checkstatus.com";

	@Mock
	private I18NService i18NService;

	@Mock
	private ModelService modelService;

	private VisualSearchSyncStatusCronJobModel cronJob;
	private VisualSearchSyncStatusJob visualSearchSyncStatusJob;

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);

		visualSearchSyncStatusJob = Mockito.spy(VisualSearchSyncStatusJob.class);
		visualSearchSyncStatusJob.setI18NService(i18NService);
		visualSearchSyncStatusJob.setModelService(modelService);

		final VisualSearchConfigModel visualSearchConfig = new VisualSearchConfigModel();
		final VisualSearchServerConfigModel visualSearchServerConfigModel = new VisualSearchServerConfigModel();
		visualSearchConfig.setServerConfig(visualSearchServerConfigModel);

		cronJob = new VisualSearchSyncStatusCronJobModel();
		cronJob.setVisualSearchConfig(visualSearchConfig);

		when(i18NService.getCurrentTimeZone()).thenReturn(TimeZone.getDefault());
		when(i18NService.getCurrentLocale()).thenReturn(Locale.ENGLISH);

		final TriggerModel trigger = new TriggerModel();
		when(modelService.create(TriggerModel.class)).thenReturn(trigger);
	}

	@Test
	public void updateTriggerWhenSyncStatusIsNull()
	{
		// given
		final VisualSearchConfigModel visualSearchConfig = cronJob.getVisualSearchConfig();
		final VisualSearchIndexSyncStatus status = getStatusNull();

		final Date activationDate = getActivationTime();
		doReturn(status).when(visualSearchSyncStatusJob).getStatus(visualSearchConfig);
		doReturn(activationDate).when(visualSearchSyncStatusJob).calculateNextActivationTime();

		// when
		visualSearchSyncStatusJob.perform(cronJob);

		// then
		assertNull(visualSearchConfig.getLastSyncTime());
		assertThat(cronJob.getTriggers(), Matchers.hasSize(1));
		assertEquals(activationDate, cronJob.getTriggers().get(0).getActivationTime());
	}

	@Test
	public void updateTriggerAndSetActivationTimeIfNew()
	{
		// given
		final VisualSearchConfigModel visualSearchConfig = cronJob.getVisualSearchConfig();
		final VisualSearchIndexSyncStatus status = getStatus();

		final Date activationDate = getActivationTime();
		doReturn(status).when(visualSearchSyncStatusJob).getStatus(visualSearchConfig);
		doReturn(activationDate).when(visualSearchSyncStatusJob).calculateNextActivationTime();

		// when
		visualSearchSyncStatusJob.perform(cronJob);

		// then
		assertEquals(visualSearchConfig.getLastSyncTime(), status.getCurrent().getSync_time());
		assertThat(cronJob.getTriggers(), Matchers.hasSize(1));
		assertEquals(cronJob.getTriggers().get(0).getActivationTime(), activationDate);
	}

	private VisualSearchIndexSyncStatus getStatus()
	{
		final VisualSearchIndexSyncStatus status = new VisualSearchIndexSyncStatus();

		final VisualSearchIndexSyncInfo syncInfoCurrent = new VisualSearchIndexSyncInfo();
		syncInfoCurrent.setIndex_name(OLD_INDEX_NAME);
		syncInfoCurrent.setSync_time("2020-07-06 11:31:23.953339");
		status.setCurrent(syncInfoCurrent);

		final VisualSearchIndexSyncInfo syncInfoPrevious = new VisualSearchIndexSyncInfo();
		syncInfoPrevious.setIndex_name(NEW_INDEX_NAME);
		syncInfoPrevious.setSync_time("2020-06-18 14:34:29.738197");
		status.setPrevious(syncInfoPrevious);

		return status;
	}

	private VisualSearchIndexSyncStatus getStatusNull()
	{
		final VisualSearchIndexSyncStatus status = new VisualSearchIndexSyncStatus();
		status.setCurrent(new VisualSearchIndexSyncInfo());
		status.setPrevious(new VisualSearchIndexSyncInfo());

		return status;
	}

	private Date getActivationTime()
	{
		final Calendar calendar = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
		calendar.add(Calendar.MINUTE, 15);
		return new Date(calendar.getTimeInMillis());
	}
}
