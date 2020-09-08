/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;


/**
 * Implementors for this interface should provide the field values to be indexed.
 */
public interface VisualSearchValueProvider
{
	/**
	 * Resolves the values to be indexed.
	 *
	 * @param model
	 *           the values should be resolved for this model instance
	 * @param visualSearchIndexedProperty
	 *           the visual search indexed property that use the value provider
	 * @return string value of model's attribute
	 * @throws VisualSearchIndexerRuntimeException
	 *            if an error occurs
	 */
	public String resolveValue(final ItemModel model, final VisualSearchIndexedPropertyModel visualSearchIndexedProperty)
			throws VisualSearchIndexerRuntimeException;
}
