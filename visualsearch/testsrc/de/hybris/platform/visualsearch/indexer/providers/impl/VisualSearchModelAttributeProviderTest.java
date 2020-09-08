/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.Session;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.type.TypeService;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@UnitTest
public class VisualSearchModelAttributeProviderTest
{
	private static final String ATTRIBUTE_VALUE = "attributeValue";
	private static final String INDEXED_PROPERTY_NAME = "name";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private ModelService modelService;
	@Mock
	private TypeService typeService;
	@Mock
	private SessionService sessionService;
	@Mock
	private I18NService i18nService;
	@Mock
	private CatalogVersionService catalogVersionService;

	@Mock
	private ComposedTypeModel composedType;
	@Mock
	private JaloSession jaloSession;
	@Mock
	private CatalogVersionModel catalogVersion;
	@Mock
	private ProductModel product;

	private VisualSearchIndexedPropertyModel indexedProperty;
	private VisualSearchModelAttributeProvider visualSearchModelAttributeProvider;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		visualSearchModelAttributeProvider = new VisualSearchModelAttributeProvider();
		visualSearchModelAttributeProvider.setModelService(modelService);
		visualSearchModelAttributeProvider.setTypeService(typeService);
		visualSearchModelAttributeProvider.setSessionService(sessionService);
		visualSearchModelAttributeProvider.setI18nService(i18nService);
		visualSearchModelAttributeProvider.setCatalogVersionService(catalogVersionService);

		given(typeService.getComposedTypeForClass(ProductModel.class)).willReturn(composedType);
		given(Boolean.valueOf(typeService.hasAttribute(eq(composedType), any(String.class)))).willReturn(Boolean.TRUE);

		given(sessionService.getRawSession(any(Session.class))).willReturn(jaloSession);

		given(product.getCatalogVersion()).willReturn(catalogVersion);

		indexedProperty = new VisualSearchIndexedPropertyModel();
		indexedProperty.setName(INDEXED_PROPERTY_NAME);
	}

	@Test
	public void shouldFailIfItemIsNotProduct()
	{
		// expect
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Item to be indexed must be of Product type");

		// when
		visualSearchModelAttributeProvider.resolveValue(new CategoryModel(), indexedProperty);
	}

	@Test
	public void resolveNonSupportedAttributeValue() throws Exception
	{
		// given
		given(Boolean.valueOf(typeService.hasAttribute(composedType, INDEXED_PROPERTY_NAME))).willReturn(Boolean.FALSE);

		// then
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Unsupported attribute " + INDEXED_PROPERTY_NAME);

		// when
		visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);
	}

	@Test
	public void resolveNullAttributeValue() throws Exception
	{
		// given
		given(Boolean.valueOf(typeService.hasAttribute(composedType, INDEXED_PROPERTY_NAME))).willReturn(Boolean.TRUE);
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(null);

		// when
		final String value = visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, "");
	}

	@Test
	public void resolveAttributeValue() throws Exception
	{
		// given
		given(Boolean.valueOf(typeService.hasAttribute(composedType, INDEXED_PROPERTY_NAME))).willReturn(Boolean.TRUE);
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(ATTRIBUTE_VALUE);

		// when
		final String value = visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, ATTRIBUTE_VALUE);
	}

	@Test
	public void resolveCollectionAttributeValue() throws Exception
	{
		// given
		given(Boolean.valueOf(typeService.hasAttribute(composedType, INDEXED_PROPERTY_NAME))).willReturn(Boolean.TRUE);
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(Arrays.asList("attrValue1", "attrValue2"));
		final String expectedValue = "attrValue1,attrValue2";

		// when
		final String value = visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, expectedValue);
	}
}
