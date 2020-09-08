/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.strategies.impl;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.core.suspend.SystemIsSuspendedException;
import de.hybris.platform.core.threadregistry.OperationInfo;
import de.hybris.platform.core.threadregistry.RevertibleUpdate;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.cronjob.model.TriggerModel;
import de.hybris.platform.processing.distributed.DistributedProcessService;
import de.hybris.platform.processing.distributed.defaultimpl.DistributedProcessHelper;
import de.hybris.platform.processing.distributed.simple.data.CollectionBasedCreationData;
import de.hybris.platform.processing.distributed.simple.data.CollectionBasedCreationData.Builder;
import de.hybris.platform.processing.enums.DistributedProcessState;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.keygenerator.KeyGenerator;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.tenant.TenantService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.util.Utilities;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.strategies.VisualSearchIndexerStrategy;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerDistributedProcessModel;
import de.hybris.platform.visualsearch.model.VisualSearchSyncStatusCronJobModel;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


/**
 * Default implementation of {@link VisualSearchIndexerStrategy}
 */
public class DefaultVisualSearchIndexerStrategy implements VisualSearchIndexerStrategy
{
	private static final Logger LOG = Logger.getLogger(DefaultVisualSearchIndexerStrategy.class);

	public static final String EXPORT_SUB_DIR = "visualsearch";
	private static final String DISTRIBUTED_PROCESS_HANDLER = "visualSearchIndexerDistributedProcessHandler";

	private DistributedProcessService distributedProcessService;
	private KeyGenerator indexOperationIdKeyGenerator;
	private ModelService modelService;
	private TenantService tenantService;
	private UserService userService;
	private CronJobService cronJobService;
	private I18NService i18NService;
	private RestTemplate restTemplate;

	@Override
	public void execute(final VisualSearchConfigModel visualSearchConfig, final List<PK> pks) throws VisualSearchIndexerException
	{
		try (RevertibleUpdate revertibleUpdate = markThreadAsSuspendable())
		{
			LOG.info("Execute visual search strategy");

			final long indexProcessId = generateIndexId();
			final String exportPath = resolveFileDir(visualSearchConfig.getIndexConfig());

			final CollectionBasedCreationData indexerProcessData = buildIndexerCreationData(visualSearchConfig, pks, indexProcessId);
			final VisualSearchIndexerDistributedProcessModel distributedIndexerProcess = createDistributedIndexerProcess(
					indexerProcessData, visualSearchConfig, indexProcessId, exportPath);

			distributedProcessService.start(distributedIndexerProcess.getCode());
			waitForDistributedIndexer(distributedIndexerProcess.getCode());

			triggerSynchronization(visualSearchConfig);

			createSyncStatusCronJob(visualSearchConfig);
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException(e);
		}
	}

	protected RevertibleUpdate markThreadAsSuspendable()
	{
		return OperationInfo.updateThread(OperationInfo.builder().withTenant(resolveTenantId())
				.withStatusInfo("Starting distributed visual search indexing process as suspendable thread...")
				.asSuspendableOperation().build());
	}

	protected CollectionBasedCreationData buildIndexerCreationData(final VisualSearchConfigModel visualSearchConfig,
			final List<PK> pks, final long indexProcessId)
	{
		final VisualSearchIndexConfigModel indexConfig = visualSearchConfig.getIndexConfig();

		final int batchSize = indexConfig.getBatchSize();

		final Builder indexerCreationDataBuilder = CollectionBasedCreationData.builder().withElements(pks)
				.withProcessId(String.valueOf(indexProcessId)).withHandlerId(DISTRIBUTED_PROCESS_HANDLER).withBatchSize(batchSize)
				.withNumOfRetries(0).withProcessModelClass(VisualSearchIndexerDistributedProcessModel.class);
		if (StringUtils.isNotEmpty(indexConfig.getNodeGroup()))
		{
			indexerCreationDataBuilder.withNodeGroup(indexConfig.getNodeGroup());
		}

		return indexerCreationDataBuilder.build();
	}

	protected VisualSearchIndexerDistributedProcessModel createDistributedIndexerProcess(
			final CollectionBasedCreationData indexerProcessData, final VisualSearchConfigModel visualSearchConfig,
			final long indexProcessId, final String exportPath)
	{
		final UserModel sessionUser = resolveSessionUser();

		final VisualSearchIndexerDistributedProcessModel distributedIndexerProcess = distributedProcessService
				.create(indexerProcessData);
		distributedIndexerProcess.setIndexOperationId(indexProcessId);
		distributedIndexerProcess.setVisualSearchConfig(visualSearchConfig.getName());
		distributedIndexerProcess.setExportPath(exportPath);

		// session related parameters
		distributedIndexerProcess.setSessionUser(sessionUser.getUid());

		modelService.save(distributedIndexerProcess);

		return distributedIndexerProcess;
	}

	protected void waitForDistributedIndexer(final String processCode) throws VisualSearchIndexerException, InterruptedException
	{
		do
		{
			try
			{
				// await 'finished' state of process first
				final VisualSearchIndexerDistributedProcessModel process = distributedProcessService.wait(processCode, 5);

				if (DistributedProcessHelper.isFinished(process) && DistributedProcessState.FAILED.equals(process.getState()))
				{
					throw new VisualSearchIndexerException("Visual Search Indexing process has failed");
				}
				if (DistributedProcessHelper.isFinished(process))
				{
					return;
				}
			}
			catch (final SystemIsSuspendedException e)
			{
				if (LOG.isDebugEnabled())
				{
					LOG.debug("The system has been suspended. Retrying in 5 seconds.", e);
				}

				Thread.sleep(5000);
			}
		}
		while (true);
	}

	protected void triggerSynchronization(final VisualSearchConfigModel visualSearchConfig) throws VisualSearchIndexerException
	{
		final String url = visualSearchConfig.getServerConfig().getTriggerSyncUrl();
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		final ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

		if (!response.getStatusCode().is2xxSuccessful())
		{
			throw new VisualSearchIndexerException("Error while triggering synchronization.");
		}
	}

	protected void createSyncStatusCronJob(final VisualSearchConfigModel visualSearchConfig)
	{
		final VisualSearchSyncStatusCronJobModel cronJob = modelService.create(VisualSearchSyncStatusCronJobModel.class);
		final String cronJobCode = VisualsearchConstants.VS_SYNC_STATUS_JOB_SPRING_ID + Calendar.getInstance().getTimeInMillis();
		cronJob.setCode(cronJobCode);

		final JobModel jobModel = cronJobService.getJob(VisualsearchConstants.VS_SYNC_STATUS_JOB_SPRING_ID);
		cronJob.setJob(jobModel);
		cronJob.setVisualSearchConfig(visualSearchConfig);
		cronJob.setLogToDatabase(Boolean.TRUE);
		modelService.save(cronJob);

		final Calendar calendar = Calendar.getInstance(i18NService.getCurrentTimeZone(), i18NService.getCurrentLocale());
		calendar.add(Calendar.HOUR, 2);
		final Date activationTime = new Date(calendar.getTimeInMillis());

		final TriggerModel trigger = modelService.create(TriggerModel.class);
		trigger.setActivationTime(activationTime);
		trigger.setActive(Boolean.TRUE);
		trigger.setCronJob(cronJob);
		cronJob.setTriggers(Collections.singletonList(trigger));

		modelService.saveAll(trigger, cronJob);
	}

	protected String resolveTenantId()
	{
		return tenantService.getCurrentTenantId();
	}

	protected UserModel resolveSessionUser()
	{
		return userService.getCurrentUser();
	}

	protected long generateIndexId()
	{
		final long timestamp = System.currentTimeMillis();
		final long sequence = Long.parseLong((String) indexOperationIdKeyGenerator.generate());

		return (timestamp << 16) + sequence;
	}

	protected String resolveFileDir(final VisualSearchIndexConfigModel indexConfig) throws VisualSearchIndexerException
	{
		String confExportDirPath = indexConfig.getExportPath();
		if (StringUtils.isEmpty(confExportDirPath))
		{
			LOG.info("Export path was not set in configuration.");
			final String platformTempDir = Utilities.getPlatformConfig().getSystemConfig().getTempDir().getPath();
			if (StringUtils.isEmpty(platformTempDir))
			{
				LOG.error("Export path was not specified neither in configuration nor in HYBRIS_TEMP_DIR");
				throw new VisualSearchIndexerException("Unspecified export path.");
			}

			confExportDirPath = platformTempDir + File.separator + EXPORT_SUB_DIR;
		}
		LOG.info("Exporter dir path: " + confExportDirPath);

		verifyCreateFolder(confExportDirPath);
		return confExportDirPath;
	}

	protected void verifyCreateFolder(final String path) throws VisualSearchIndexerException
	{

		try
		{
			if (path != null)
			{
				final File folder = new File(path);
				if (!(folder.exists() && folder.isDirectory() && folder.canWrite()) && !folder.mkdirs())
				{
					throw new IOException("Failed to create Directory: " + folder.getPath());
				}
			}
		}
		catch (final Exception e)
		{
			LOG.error("Problem accessing/creating the folder: \"" + path + "\"");
			throw new VisualSearchIndexerException("Uncorrect destination folder for indexer files. " + path, e);
		}

	}

	@Required
	public void setDistributedProcessService(final DistributedProcessService distributedProcessService)
	{
		this.distributedProcessService = distributedProcessService;
	}

	@Required
	public void setIndexOperationIdKeyGenerator(final KeyGenerator indexOperationIdKeyGenerator)
	{
		this.indexOperationIdKeyGenerator = indexOperationIdKeyGenerator;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	@Required
	public void setTenantService(final TenantService tenantService)
	{
		this.tenantService = tenantService;
	}

	@Required
	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	@Required
	public void setCronJobService(final CronJobService cronJobService)
	{
		this.cronJobService = cronJobService;
	}

	@Required
	public void setI18NService(final I18NService i18nService)
	{
		i18NService = i18nService;
	}

	@Required
	public void setRestTemplate(final RestTemplate restTemplate)
	{
		this.restTemplate = restTemplate;
	}
}
