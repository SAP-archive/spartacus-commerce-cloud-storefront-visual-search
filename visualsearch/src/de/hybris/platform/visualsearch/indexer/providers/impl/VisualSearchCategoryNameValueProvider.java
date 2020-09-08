/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;


public class VisualSearchCategoryNameValueProvider extends VisualSearchModelAttributeProvider
{
	@Override
	protected String valueToString(final Object value)
	{
		if (value instanceof CategoryModel)
		{
			final CategoryModel category = (CategoryModel) value;
			return category.getName();
		}
		else
		{
			throw new VisualSearchIndexerRuntimeException("Value must be of type CategoryModel");
		}
	}
}
