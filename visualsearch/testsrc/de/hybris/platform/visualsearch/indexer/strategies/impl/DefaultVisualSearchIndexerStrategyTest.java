/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.strategies.impl;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.processing.distributed.DistributedProcessService;
import de.hybris.platform.processing.distributed.simple.data.CollectionBasedCreationData;
import de.hybris.platform.processing.enums.DistributedProcessState;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.tenant.TenantService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerDistributedProcessModel;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchSyncStatusCronJobModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


@UnitTest
public class DefaultVisualSearchIndexerStrategyTest
{
	private static final String TRIGGER_SYNC_URL = "triggerSyncUrl";
	private static final String VISUAL_SEARCH_CONFIG = "visualSearchConfig";
	private static final String TEST_TENANT = "testTenant";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private DistributedProcessService distributedProcessService;
	@Mock
	private KeyGenerator indexOperationIdKeyGenerator;
	@Mock
	private ModelService modelService;
	@Mock
	private TenantService tenantService;
	@Mock
	private UserService userService;
	@Mock
	private CronJobService cronJobService;
	@Mock
	private I18NService i18NService;
	@Mock
	private RestTemplate restTemplate;

	private VisualSearchIndexerDistributedProcessModel distributedIndexerProcess;
	private List<PK> pks;
	private VisualSearchConfigModel visualSearchConfig;

	private DefaultVisualSearchIndexerStrategy visualSearchIndexedStrategy;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		visualSearchIndexedStrategy = new DefaultVisualSearchIndexerStrategy();
		visualSearchIndexedStrategy.setCronJobService(cronJobService);
		visualSearchIndexedStrategy.setDistributedProcessService(distributedProcessService);
		visualSearchIndexedStrategy.setI18NService(i18NService);
		visualSearchIndexedStrategy.setIndexOperationIdKeyGenerator(indexOperationIdKeyGenerator);
		visualSearchIndexedStrategy.setModelService(modelService);
		visualSearchIndexedStrategy.setTenantService(tenantService);
		visualSearchIndexedStrategy.setUserService(userService);
		visualSearchIndexedStrategy.setRestTemplate(restTemplate);

		given(indexOperationIdKeyGenerator.generate()).willReturn("1");
		given(userService.getCurrentUser()).willReturn(mock(UserModel.class));
		given(i18NService.getCurrentTimeZone()).willReturn(TimeZone.getDefault());
		given(i18NService.getCurrentLocale()).willReturn(Locale.ENGLISH);
		given(tenantService.getCurrentTenantId()).willReturn(TEST_TENANT);

		pks = new ArrayList<PK>(10);
		for (int i = 0; i < 10; i++)
		{
			pks.add(PK.createFixedCounterPK(1, i + 1));
		}

		visualSearchConfig = new VisualSearchConfigModel();
		visualSearchConfig.setName(VISUAL_SEARCH_CONFIG);

		final VisualSearchIndexConfigModel indexConfig = new VisualSearchIndexConfigModel();
		indexConfig.setBatchSize(100);
		visualSearchConfig.setIndexConfig(indexConfig);

		final VisualSearchServerConfigModel serverConfig = new VisualSearchServerConfigModel();
		serverConfig.setTriggerSyncUrl(TRIGGER_SYNC_URL);
		visualSearchConfig.setServerConfig(serverConfig);


		distributedIndexerProcess = new VisualSearchIndexerDistributedProcessModel();
		given(distributedProcessService.create(any(CollectionBasedCreationData.class))).willReturn(distributedIndexerProcess);
		given(distributedProcessService.start(distributedIndexerProcess.getCode())).willReturn(distributedIndexerProcess);
	}

	@Test
	public void executeFailedDistributedProcess() throws Exception
	{
		// given
		distributedIndexerProcess.setState(DistributedProcessState.FAILED);
		given(distributedProcessService.wait(distributedIndexerProcess.getCode(), 5)).willReturn(distributedIndexerProcess);

		// expect
		expectedException.expect(VisualSearchIndexerException.class);
		expectedException.expectMessage("Visual Search Indexing process has failed");

		// when
		visualSearchIndexedStrategy.execute(visualSearchConfig, pks);
	}

	@Test
	public void executeFailedSyncTrigger() throws Exception
	{
		// given
		distributedIndexerProcess.setState(DistributedProcessState.SUCCEEDED);
		given(distributedProcessService.wait(distributedIndexerProcess.getCode(), 5)).willReturn(distributedIndexerProcess);

		final ResponseEntity<String> response = new ResponseEntity(HttpStatus.BAD_REQUEST);
		given(restTemplate.exchange(eq(TRIGGER_SYNC_URL), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(response);

		// expect
		expectedException.expect(VisualSearchIndexerException.class);
		expectedException.expectMessage("Error while triggering synchronization.");

		// when
		visualSearchIndexedStrategy.execute(visualSearchConfig, pks);
	}

	@Test
	public void executeSuccess() throws Exception
	{
		// given
		distributedIndexerProcess.setState(DistributedProcessState.SUCCEEDED);
		given(distributedProcessService.wait(distributedIndexerProcess.getCode(), 5)).willReturn(distributedIndexerProcess);

		final ResponseEntity<String> response = new ResponseEntity(HttpStatus.ACCEPTED);
		given(restTemplate.exchange(eq(TRIGGER_SYNC_URL), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(response);

		final VisualSearchSyncStatusCronJobModel cronJob = new VisualSearchSyncStatusCronJobModel();
		given(modelService.create(VisualSearchSyncStatusCronJobModel.class)).willReturn(cronJob);

		final JobModel jobModel = new JobModel();
		given(cronJobService.getJob(VisualsearchConstants.VS_SYNC_STATUS_JOB_SPRING_ID)).willReturn(jobModel);

		final TriggerModel trigger = new TriggerModel();
		given(modelService.create(TriggerModel.class)).willReturn(trigger);

		// when
		visualSearchIndexedStrategy.execute(visualSearchConfig, pks);

		// then
		assertTrue(cronJob.getCode().startsWith(VisualsearchConstants.VS_SYNC_STATUS_JOB_SPRING_ID));
		assertEquals(jobModel, cronJob.getJob());
		assertThat(cronJob.getTriggers(), hasItem(trigger));

	}
}
