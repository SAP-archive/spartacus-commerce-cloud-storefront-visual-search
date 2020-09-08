/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.populators;

import de.hybris.platform.converters.Populator;
import de.hybris.platform.solrfacetsearch.provider.FieldNameProvider.FieldType;
import de.hybris.platform.solrfacetsearch.search.FieldNameTranslator;
import de.hybris.platform.solrfacetsearch.search.SearchQuery;
import de.hybris.platform.solrfacetsearch.search.impl.SearchQueryConverterData;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;

import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.springframework.beans.factory.annotation.Required;


public class VisualSearchFacetSearchQuerySortsPopulator implements Populator<SearchQueryConverterData, SolrQuery>
{
	protected static final int BUFFER_SIZE = 256;
	protected static final float MAX_PROMOTED_RESULT_SCORE = 10000;

	private FieldNameTranslator fieldNameTranslator;

	@Override
	public void populate(final SearchQueryConverterData source, final SolrQuery target)
	{
		final SearchQuery searchQuery = source.getSearchQuery();
		final String sortCode = searchQuery.getNamedSort();
		if (VisualsearchConstants.VISUAL_RELEVANCE.equals(sortCode))
		{
			buildSimilarItemsSort(source, target);
		}
	}

	protected void buildSimilarItemsSort(final SearchQueryConverterData source, final SolrQuery target)
	{
		final Map<String, Object> attributes = source.getFacetSearchContext().getAttributes();
		final String field = (String) attributes.get(VisualsearchConstants.VS_FILTER_QUERY_PARAM);
		final Set<String> productCodes = (Set<String>) attributes.get(VisualsearchConstants.VS_SIMILAR_PRODUCT_IDS);

		final String fieldName = fieldNameTranslator.translate(source.getSearchQuery(), field, FieldType.INDEX);
		final String sortQuery = buildSimilarItemSort(fieldName, productCodes);
		if (!StringUtils.isBlank(sortQuery))
		{
			target.addSort(sortQuery, ORDER.desc);
		}
	}

	protected String buildSimilarItemSort(final String fieldName, final Set<String> productCodes)
	{
		if (CollectionUtils.isEmpty(productCodes))
		{
			return StringUtils.EMPTY;
		}

		final StringBuilder query = new StringBuilder(BUFFER_SIZE);
		query.append("query({!v='");

		float score = MAX_PROMOTED_RESULT_SCORE;
		int index = 0;

		for (final String productCode : productCodes)
		{
			if (index != 0)
			{
				query.append(' ');
			}

			query.append(fieldName);
			query.append(':');
			query.append(productCode);
			query.append("^=");
			query.append(score);

			score = Math.nextDown(score);
			index++;
		}

		query.append("'},1)");

		return query.toString();

	}

	@Required
	public void setFieldNameTranslator(final FieldNameTranslator fieldNameTranslator)
	{
		this.fieldNameTranslator = fieldNameTranslator;
	}
}
