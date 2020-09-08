/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class VisualSearchImageUrlValueProviderTest
{
	private static final String SITE_UID_PARAMETER_VALUE = "visualsearch";
	private static final String SITE_UID_PARAMETER = "siteUid";
	private static final String VALUE_PROVIDER = "visualSearchImageUrlValueProvider";
	private static final String IMAGE_URL_PROPERTY = "imageUrl";
	private static final String PRODUCT_URL = "/productUrl";

	@Mock
	private ConfigurationService configurationService;

	private ProductModel product;
	private VisualSearchIndexedPropertyModel indexedProperty;

	private VisualSearchImageUrlValueProvider visualSearchImageUrlValueProvider;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		visualSearchImageUrlValueProvider = new VisualSearchImageUrlValueProvider();
		visualSearchImageUrlValueProvider.setConfigurationService(configurationService);

		final MediaModel media = mock(MediaModel.class);
		given(media.getURL()).willReturn(PRODUCT_URL);

		product = mock(ProductModel.class);
		given(product.getPicture()).willReturn(media);

		indexedProperty = new VisualSearchIndexedPropertyModel();
		indexedProperty.setValueProviderParameters(new HashMap());
		indexedProperty.setName(IMAGE_URL_PROPERTY);
		indexedProperty.setFieldValueProvider(VALUE_PROVIDER);
	}

	@Test
	public void resolveValueWhenNoParameterSetTest()
	{
		// given
		final Configuration configuration = mock(Configuration.class);
		given(configurationService.getConfiguration()).willReturn(configuration);
		given(configuration.getString("tomcat.http.port", null)).willReturn("9001");
		final String expectedValue = "http://localhost:9001" + PRODUCT_URL;

		// when
		final String value = visualSearchImageUrlValueProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, expectedValue);
	}

	@Test
	public void resolveValueWhenParameterIsSet()
	{
		// given
		final Map<String, String> parameters = new HashMap<>();
		parameters.put(SITE_UID_PARAMETER, SITE_UID_PARAMETER_VALUE);
		indexedProperty.setValueProviderParameters(parameters);

		final Configuration configuration = mock(Configuration.class);
		given(configurationService.getConfiguration()).willReturn(configuration);
		final String key = "media." + SITE_UID_PARAMETER_VALUE + ".http";
		given(configuration.getString(key, null)).willReturn("http://visualsearch");
		final String expectedValue = "http://visualsearch" + PRODUCT_URL;

		// when
		final String value = visualSearchImageUrlValueProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, expectedValue);
	}
}
