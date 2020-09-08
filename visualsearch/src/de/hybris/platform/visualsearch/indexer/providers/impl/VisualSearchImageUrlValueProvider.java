/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.indexer.providers.VisualSearchValueProvider;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Required;


public class VisualSearchImageUrlValueProvider implements VisualSearchValueProvider
{
	private static final String SITE_UID_PARAM = "siteUid";

	private ConfigurationService configurationService;

	@Override
	public String resolveValue(final ItemModel model, final VisualSearchIndexedPropertyModel visualSearchIndexedProperty)
			throws VisualSearchIndexerRuntimeException
	{
		if (!(model instanceof ProductModel))
		{
			throw new VisualSearchIndexerRuntimeException("Item to be indexed must be of Product type");
		}

		final Map<String, String> parameters = visualSearchIndexedProperty.getValueProviderParameters();

		final ProductModel product = (ProductModel) model;
		final MediaModel picture = product.getPicture();
		if (picture == null)
		{
			throw new VisualSearchIndexerRuntimeException("Product image cannot be found");
		}

		return getMediaUrlForSite(parameters.get(SITE_UID_PARAM), false) + picture.getURL();
	}


	protected String getMediaUrlForSite(final String siteUid, final boolean secure)
	{
		if (!StringUtils.isBlank(siteUid))
		{
			final String url = cleanupUrl(lookupConfig("media." + siteUid + (secure ? ".https" : ".http")));
			if (url != null)
			{
				return url;
			}
		}
		return getDefaultMediaUrlForSite(secure);
	}

	protected String getDefaultMediaUrlForSite(final boolean secure)
	{
		if (secure)
		{
			return "https://localhost:" + lookupConfig("tomcat.ssl.port");
		}
		else
		{
			return "http://localhost:" + lookupConfig("tomcat.http.port");
		}
	}

	protected String lookupConfig(final String key)
	{
		return configurationService.getConfiguration().getString(key, null);
	}

	protected String cleanupUrl(final String url)
	{
		if (url != null && url.endsWith("/"))
		{
			return url.substring(0, url.length() - 1);
		}
		return url;
	}

	@Required
	public void setConfigurationService(final ConfigurationService configurationService)
	{
		this.configurationService = configurationService;
	}

}
