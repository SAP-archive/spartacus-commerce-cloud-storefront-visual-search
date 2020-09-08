/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.listeners;

import static org.junit.Assert.assertThat;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.solrfacetsearch.config.FacetSearchConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedType;
import de.hybris.platform.solrfacetsearch.search.SearchQuery;
import de.hybris.platform.solrfacetsearch.search.context.impl.DefaultFacetSearchContext;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;

import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;


@UnitTest
public class SolrVisualSearchListenerTest
{
	private static final String QUERY_PARAM_CODE = "code";

	private DefaultFacetSearchContext facetSearchContext;
	private SolrVisualSearchListener listener;

	@Test
	public void addToContextIfSortCodeIsVisualRelevance() throws Exception
	{
		// given
		final IndexedType indexedType = new IndexedType();
		final FacetSearchConfig facetSearchConfig = new FacetSearchConfig();
		final SearchQuery searchQuery = new SearchQuery(facetSearchConfig, indexedType);
		searchQuery.setNamedSort(VisualsearchConstants.VISUAL_RELEVANCE);
		searchQuery.setUserQuery("1,2,3");

		facetSearchContext = new DefaultFacetSearchContext();
		facetSearchContext.setFacetSearchConfig(facetSearchConfig);
		facetSearchContext.setIndexedType(indexedType);
		facetSearchContext.setSearchQuery(searchQuery);
		listener = new SolrVisualSearchListener();

		// when
		listener.beforeSearch(facetSearchContext);

		// then
		final Map<String, Object> attributes = facetSearchContext.getAttributes();
		final String field = (String) attributes.get(VisualsearchConstants.VS_FILTER_QUERY_PARAM);
		final Set<String> productCodes = (Set<String>) attributes.get(VisualsearchConstants.VS_SIMILAR_PRODUCT_IDS);

		assertThat(field, Matchers.equalTo(QUERY_PARAM_CODE));
		assertThat(productCodes, Matchers.hasSize(3));
		assertThat(productCodes, Matchers.hasItem("1"));
		assertThat(productCodes, Matchers.hasItem("2"));
		assertThat(productCodes, Matchers.hasItem("3"));

	}
}
