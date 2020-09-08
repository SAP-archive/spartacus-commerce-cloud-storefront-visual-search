/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.tenant.TenantService;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.visualsearch.enums.VisualSearchSyncResult;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.strategies.VisualSearchIndexerStrategy;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class DefaultVisualSearchIndexerServiceTest
{
	private static final String INDEX_CONFIG_QUERY = "query";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private SessionService sessionService;
	@Mock
	private TenantService tenantService;
	@Mock
	private ModelService modelService;
	@Mock
	private UserService userService;
	@Mock
	private FlexibleSearchService flexibleSearchService;
	@Mock
	private VisualSearchIndexerStrategy visualSearchIndexerStrategy;

	@Mock
	private JaloSession jaloSession;

	private VisualSearchConfigModel visualSearchConfig;
	private List<PK> pks;
	private DefaultVisualSearchIndexerService visualSearchIndexerService;

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);

		visualSearchIndexerService = new DefaultVisualSearchIndexerService();
		visualSearchIndexerService.setFlexibleSearchService(flexibleSearchService);
		visualSearchIndexerService.setModelService(modelService);
		visualSearchIndexerService.setSessionService(sessionService);
		visualSearchIndexerService.setTenantService(tenantService);
		visualSearchIndexerService.setUserService(userService);
		visualSearchIndexerService.setVisualSearchIndexerStrategy(visualSearchIndexerStrategy);

		given(sessionService.getRawSession(any())).willReturn(jaloSession);

		visualSearchConfig = new VisualSearchConfigModel();

		final VisualSearchIndexConfigModel indexConfig = new VisualSearchIndexConfigModel();
		indexConfig.setUser(new UserModel());
		indexConfig.setQuery(INDEX_CONFIG_QUERY);
		indexConfig.setCatalogVersion(new CatalogVersionModel());
		visualSearchConfig.setIndexConfig(indexConfig);


		pks = new ArrayList<PK>(10);
		for (int i = 0; i < 10; i++)
		{
			pks.add(PK.createFixedCounterPK(1, i + 1));
		}
		final SearchResult searchResult = mock(SearchResult.class);
		given(searchResult.getResult()).willReturn(pks);
		given(flexibleSearchService.search(any(FlexibleSearchQuery.class))).willReturn(searchResult);
	}

	@Test
	public void performVisualSearchIndexWhenAlreadyRunning() throws Exception
	{
		// when
		visualSearchConfig.setStatus(VisualSearchSyncResult.RUNNING);

		// expect
		expectedException.expect(VisualSearchIndexerException.class);
		expectedException.expectMessage("Datafeed syncronization process is already running.");

		// when
		visualSearchIndexerService.performVisualSearchIndex(visualSearchConfig);

		// then
		verify(jaloSession, times(1)).removeLocalSessionContext();
	}

	@Test
	public void performVisualSearchIndex() throws Exception
	{
		// given
		willDoNothing().given(visualSearchIndexerStrategy).execute(visualSearchConfig, pks);

		// when
		visualSearchIndexerService.performVisualSearchIndex(visualSearchConfig);

		// then
		assertEquals(VisualSearchSyncResult.RUNNING, visualSearchConfig.getStatus());
		verify(visualSearchIndexerStrategy, times(1)).execute(visualSearchConfig, pks);
		verify(jaloSession, times(1)).removeLocalSessionContext();
	}

	@Test
	public void performVisualSearchIndexException() throws Exception
	{
		// given
		willThrow(VisualSearchIndexerException.class).given(visualSearchIndexerStrategy).execute(visualSearchConfig, pks);

		// expect
		expectedException.expect(VisualSearchIndexerException.class);

		// when
		visualSearchIndexerService.performVisualSearchIndex(visualSearchConfig);

		// then
		assertEquals(VisualSearchSyncResult.FAILURE, visualSearchConfig.getStatus());
		verify(visualSearchIndexerStrategy, times(1)).execute(visualSearchConfig, pks);
		verify(jaloSession, times(1)).removeLocalSessionContext();
	}
}
