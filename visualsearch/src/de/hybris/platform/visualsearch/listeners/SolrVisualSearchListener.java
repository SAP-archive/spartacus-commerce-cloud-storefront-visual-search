/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.listeners;

import de.hybris.platform.solrfacetsearch.search.FacetSearchException;
import de.hybris.platform.solrfacetsearch.search.SearchQuery;
import de.hybris.platform.solrfacetsearch.search.context.FacetSearchContext;
import de.hybris.platform.solrfacetsearch.search.context.FacetSearchListener;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;


/**
 * Listener for sorting visual search results
 */
public class SolrVisualSearchListener implements FacetSearchListener
{
	private static final String QUERY_PARAM_CODE = "code";

	@Override
	public void beforeSearch(final FacetSearchContext facetSearchContext) throws FacetSearchException
	{
		final SearchQuery searchQuery = facetSearchContext.getSearchQuery();
		final String sortCode = searchQuery.getNamedSort();
		if (VisualsearchConstants.VISUAL_RELEVANCE.equals(sortCode))
		{
			final Set<String> productIds = getProductIds(searchQuery.getUserQuery());
			searchQuery.addFilterQuery(QUERY_PARAM_CODE, SearchQuery.Operator.OR, productIds);
			searchQuery.setUserQuery("");

			final Map<String, Object> contextAttributes = facetSearchContext.getAttributes();
			contextAttributes.put(VisualsearchConstants.VS_SIMILAR_PRODUCT_IDS, productIds);
			contextAttributes.put(VisualsearchConstants.VS_FILTER_QUERY_PARAM, QUERY_PARAM_CODE);
		}
	}

	private Set<String> getProductIds(final String query)
	{
		final Set<String> result = new LinkedHashSet<>();
		final List<String> productIds = new LinkedList<String>(Arrays.asList(query.split(",")));
		productIds.forEach(id -> result.add(StringUtils.trimWhitespace(id)));
		return result;
	}

	@Override
	public void afterSearch(final FacetSearchContext facetSearchContext) throws FacetSearchException
	{
		// Not implemented
	}

	@Override
	public void afterSearchError(final FacetSearchContext facetSearchContext) throws FacetSearchException
	{
		// Not implemented
	}

}
