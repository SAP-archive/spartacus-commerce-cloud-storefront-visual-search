/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.backoffice.actions;

import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.cronjob.model.CronJobModel;
import de.hybris.platform.cronjob.model.JobModel;
import de.hybris.platform.servicelayer.cronjob.CronJobService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexerCronJobModel;

import java.util.Calendar;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;


public class VisualSearchIndexerAction implements CockpitAction<VisualSearchConfigModel, Object>
{
	private static final String VISUALSEARCHINDEXERACTION_CONFIRM = "visualsearchindexeraction.confirm";
	private static final String VISUALSEARCHINDEXERACTION_PROCESS_ERROR_MESSAGE = "visualsearchindexeraction.process.error.message";
	private static final String VISUALSEARCHINDEXERACTION_PROCESS_MESSAGE = "visualsearchindexeraction.process.message";

	@Resource
	private CronJobService cronJobService;

	@Resource
	private ModelService modelService;

	private String cronJobCode;

	@Override
	public ActionResult<Object> perform(final ActionContext<VisualSearchConfigModel> ctx)
	{
		try
		{
			final VisualSearchConfigModel visualSearchConfigModel = ctx.getData();
			final VisualSearchIndexerCronJobModel cronJob = modelService.create(VisualSearchIndexerCronJobModel.class);
			cronJobCode = VisualsearchConstants.VS_INDEXER_JOB_SPRING_ID + Calendar.getInstance().getTimeInMillis();
			cronJob.setCode(cronJobCode);

			final JobModel jobModel = cronJobService.getJob(VisualsearchConstants.VS_INDEXER_JOB_SPRING_ID);
			cronJob.setJob(jobModel);
			cronJob.setVisualSearchConfig(visualSearchConfigModel);
			cronJob.setLogToDatabase(Boolean.TRUE);
			modelService.save(cronJob);

			cronJobService.performCronJob(cronJob, true);

			return new ActionResult(ActionResult.SUCCESS, ctx.getLabel(VISUALSEARCHINDEXERACTION_PROCESS_MESSAGE));
		}
		catch (final Exception e)
		{
			return new ActionResult<>(ActionResult.ERROR, ctx.getLabel(VISUALSEARCHINDEXERACTION_PROCESS_ERROR_MESSAGE));
		}

	}

	@Override
	public boolean canPerform(final ActionContext<VisualSearchConfigModel> ctx)
	{
		if (!StringUtils.isBlank(cronJobCode))
		{
			final CronJobModel cronJob = cronJobService.getCronJob(cronJobCode);
			if (cronJob != null && cronJob.getStatus() == CronJobStatus.RUNNING)
			{
				return false;
			}
		}

		final VisualSearchConfigModel data = ctx.getData();
		if (data == null)
		{
			return false;
		}

		return true;
	}

	@Override
	public boolean needsConfirmation(final ActionContext<VisualSearchConfigModel> ctx)
	{
		final VisualSearchConfigModel data = ctx.getData();
		if (data == null)
		{
			return false;
		}
		return true;
	}

	@Override
	public String getConfirmationMessage(final ActionContext<VisualSearchConfigModel> ctx)
	{
		return ctx.getLabel(VISUALSEARCHINDEXERACTION_CONFIRM);
	}

}
