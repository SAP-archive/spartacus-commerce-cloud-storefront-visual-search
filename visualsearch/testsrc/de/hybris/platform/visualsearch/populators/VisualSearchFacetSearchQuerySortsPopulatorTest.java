/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.populators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.solrfacetsearch.config.FacetSearchConfig;
import de.hybris.platform.solrfacetsearch.config.IndexedType;
import de.hybris.platform.solrfacetsearch.provider.FieldNameProvider.FieldType;
import de.hybris.platform.solrfacetsearch.search.FieldNameTranslator;
import de.hybris.platform.solrfacetsearch.search.OrderField;
import de.hybris.platform.solrfacetsearch.search.SearchQuery;
import de.hybris.platform.solrfacetsearch.search.context.FacetSearchContext;
import de.hybris.platform.solrfacetsearch.search.impl.SearchQueryConverterData;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class VisualSearchFacetSearchQuerySortsPopulatorTest
{
	public static final String FIELD1 = "field1";
	public static final String TRANSLATED_FIELD1 = "translatedField1";

	public static final String VISUAL_SEARCH_SORT_CODE = "visual-relevance";

	@Mock
	private FieldNameTranslator fieldNameTranslator;

	@Mock
	private FacetSearchContext facetSearchContext;

	private VisualSearchFacetSearchQuerySortsPopulator visualSearchFacetSearchQuerySortsPopulator;
	private SearchQueryConverterData searchQueryConverterData;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		final FacetSearchConfig facetSearchConfig = new FacetSearchConfig();
		final IndexedType indexedType = new IndexedType();
		final SearchQuery searchQuery = new SearchQuery(facetSearchConfig, indexedType);

		visualSearchFacetSearchQuerySortsPopulator = new VisualSearchFacetSearchQuerySortsPopulator();
		visualSearchFacetSearchQuerySortsPopulator.setFieldNameTranslator(fieldNameTranslator);

		searchQueryConverterData = new SearchQueryConverterData();
		searchQueryConverterData.setSearchQuery(searchQuery);
		searchQueryConverterData.setFacetSearchContext(facetSearchContext);

		given(fieldNameTranslator.translate(searchQuery, FIELD1, FieldType.INDEX)).willReturn(TRANSLATED_FIELD1);
	}

	@Test
	public void populateWithEmptySorts()
	{
		// given
		final SolrQuery solrQuery = new SolrQuery();

		// when
		visualSearchFacetSearchQuerySortsPopulator.populate(searchQueryConverterData, solrQuery);

		// then
		final List<SortClause> sorts = solrQuery.getSorts();

		final SortClause scoreClause = new SortClause(OrderField.SCORE, ORDER.desc);
		assertThat(sorts, Matchers.hasSize(0));
	}

	@Test
	public void populateWithVisualSearchSort()
	{
		// given
		final Set<String> productCodes = new LinkedHashSet<>(Arrays.asList("1", "2"));
		final Map<String, Object> contextAttributes = new HashMap<>();
		given(facetSearchContext.getAttributes()).willReturn(contextAttributes);
		contextAttributes.put(VisualsearchConstants.VS_SIMILAR_PRODUCT_IDS, productCodes);
		contextAttributes.put(VisualsearchConstants.VS_FILTER_QUERY_PARAM, FIELD1);

		final SearchQuery searchQuery = searchQueryConverterData.getSearchQuery();
		searchQuery.setNamedSort(VISUAL_SEARCH_SORT_CODE);

		final SolrQuery solrQuery = new SolrQuery();

		// when
		visualSearchFacetSearchQuerySortsPopulator.populate(searchQueryConverterData, solrQuery);

		// then
		final float score = VisualSearchFacetSearchQuerySortsPopulator.MAX_PROMOTED_RESULT_SCORE;
		final String visualSearchSortClauseItem = "query({!v='" + TRANSLATED_FIELD1 + ":1^=" + score + " " + TRANSLATED_FIELD1
				+ ":2^=" + Math.nextDown(score) + "'},1)";

		final SortClause sortClause1 = new SortClause(TRANSLATED_FIELD1, ORDER.desc);
		final SortClause promotedItemClause = new SortClause(visualSearchSortClauseItem, ORDER.desc);

		final List<SortClause> sorts = solrQuery.getSorts();
		assertThat(sorts, Matchers.hasSize(1));
		assertEquals(sorts.get(0), promotedItemClause);
	}
}
