/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import de.hybris.platform.core.PK;
import de.hybris.platform.core.threadregistry.OperationInfo;
import de.hybris.platform.core.threadregistry.RegistrableThread;
import de.hybris.platform.core.threadregistry.RevertibleUpdate;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.session.Session;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.tenant.TenantService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.visualsearch.enums.VisualSearchSyncResult;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexerService;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.strategies.VisualSearchIndexerStrategy;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.StopWatch;


public class DefaultVisualSearchIndexerService implements VisualSearchIndexerService
{
	private static final Logger LOG = Logger.getLogger(DefaultVisualSearchIndexerService.class);

	private SessionService sessionService;
	private TenantService tenantService;
	private ModelService modelService;
	private UserService userService;
	private FlexibleSearchService flexibleSearchService;
	private VisualSearchIndexerStrategy visualSearchIndexerStrategy;

	@Override
	public void performVisualSearchIndex(final VisualSearchConfigModel visualSearchConfig) throws VisualSearchIndexerException
	{
		if (visualSearchConfig == null)
		{
			throw new IllegalStateException("Visual search configuration must not be null");
		}

		checkIfTheProcessIsAlreadyRunning(visualSearchConfig);

		RevertibleUpdate revertibleInfo = null;

		final StopWatch operationTimer = new StopWatch();
		operationTimer.start();

		logStrategyStart(visualSearchConfig.getName());

		try
		{
			revertibleInfo = registerOrUpdateNonSuspendableThread();

			createLocalSessionContext();
			visualSearchConfig.setStatus(VisualSearchSyncResult.RUNNING);
			modelService.save(visualSearchConfig);
			userService.setCurrentUser(visualSearchConfig.getIndexConfig().getUser());

			final List<PK> pks = resolvePks(visualSearchConfig.getIndexConfig());
			visualSearchIndexerStrategy.execute(visualSearchConfig, pks);

			operationTimer.stop();
			logStrategySuccess(operationTimer, visualSearchConfig.getName());
		}
		catch (final VisualSearchIndexerException | RuntimeException e)
		{
			operationTimer.stop();
			logStrategyError(operationTimer, visualSearchConfig.getName());

			visualSearchConfig.setStatus(VisualSearchSyncResult.FAILURE);
			modelService.save(visualSearchConfig);
			throw e;
		}
		finally
		{
			revertOperationInfo(revertibleInfo);
			removeLocalSessionContext();
		}

	}

	protected void checkIfTheProcessIsAlreadyRunning(final VisualSearchConfigModel visualSearchConfig)
			throws VisualSearchIndexerException
	{
		if (VisualSearchSyncResult.RUNNING.equals(visualSearchConfig.getStatus()))
		{
			throw new VisualSearchIndexerException("Datafeed syncronization process is already running.");
		}
	}

	protected void logStrategyStart(final String name)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("Visual Search indexing started on " + name + ": " + new Date());
		}
	}

	protected void logStrategySuccess(final StopWatch operationTimer, final String name)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("Visual Search indexing finished on " + name + ": " + new Date() + ", total time: "
					+ operationTimer.getTotalTimeSeconds() + "s.");
		}
	}

	protected void logStrategyError(final StopWatch operationTimer, final String name)
	{
		if (LOG.isDebugEnabled())
		{
			LOG.debug("Visual Search indexing failed on " + name + ": " + new Date() + ", total time: "
					+ operationTimer.getTotalTimeSeconds() + "s.");
		}
	}

	protected RevertibleUpdate registerOrUpdateNonSuspendableThread()
	{
		RevertibleUpdate revertibleInfo = null;

		final OperationInfo operationInfo = OperationInfo.builder().withTenant(tenantService.getCurrentTenantId())
				.withStatusInfo("Creating a context for visual search indexing as non suspendable...").asNotSuspendableOperation()
				.build();

		try
		{
			RegistrableThread.registerThread(operationInfo);
		}
		catch (final IllegalStateException e)
		{
			LOG.error("Thread has already been registered. Updating operation info...", e);

			revertibleInfo = OperationInfo.updateThread(operationInfo);
		}

		return revertibleInfo;
	}

	protected void revertOperationInfo(final RevertibleUpdate revertibleInfo)
	{
		if (revertibleInfo == null)
		{
			RegistrableThread.unregisterThread();
		}
		else
		{
			revertibleInfo.revert();
		}
	}

	protected void createLocalSessionContext()
	{
		final Session session = sessionService.getCurrentSession();
		final JaloSession jaloSession = (JaloSession) sessionService.getRawSession(session);
		jaloSession.createLocalSessionContext();
	}

	protected void removeLocalSessionContext()
	{
		final Session session = sessionService.getCurrentSession();
		final JaloSession jaloSession = (JaloSession) sessionService.getRawSession(session);
		jaloSession.removeLocalSessionContext();
	}

	protected List<PK> resolvePks(final VisualSearchIndexConfigModel visualSearchIndexConfig) throws VisualSearchIndexerException
	{
		try
		{
			final String query = visualSearchIndexConfig.getQuery();
			final FlexibleSearchQuery fsQuery = new FlexibleSearchQuery(query);
			fsQuery.setUser(visualSearchIndexConfig.getUser());
			fsQuery.setCatalogVersions(Collections.singleton(visualSearchIndexConfig.getCatalogVersion()));
			fsQuery.setResultClassList(Arrays.asList(PK.class));
			final SearchResult<PK> fsResult = flexibleSearchService.search(fsQuery);
			return fsResult.getResult();
		}
		catch (final Exception e)
		{
			throw new VisualSearchIndexerException("Error resolving PKs");
		}
	}

	@Required
	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
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
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}

	@Required
	public void setVisualSearchIndexerStrategy(final VisualSearchIndexerStrategy visualSearchIndexerStrategy)
	{
		this.visualSearchIndexerStrategy = visualSearchIndexerStrategy;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

}
