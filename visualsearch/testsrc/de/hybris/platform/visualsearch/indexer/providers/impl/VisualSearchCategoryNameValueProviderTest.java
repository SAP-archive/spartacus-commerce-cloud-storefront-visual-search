/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

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
public class VisualSearchCategoryNameValueProviderTest
{
	private static final String CATEGORY_NAME = "category 1";
	private static final String CATEGORY_NAME_2 = "category 2";
	private static final String INDEXED_PROPERTY_NAME = "supercategories";

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
	private VisualSearchCategoryNameValueProvider visualSearchModelAttributeProvider;

	@Before
	public void setUp()
	{
		MockitoAnnotations.initMocks(this);

		visualSearchModelAttributeProvider = new VisualSearchCategoryNameValueProvider();
		visualSearchModelAttributeProvider.setModelService(modelService);
		visualSearchModelAttributeProvider.setTypeService(typeService);
		visualSearchModelAttributeProvider.setSessionService(sessionService);
		visualSearchModelAttributeProvider.setI18nService(i18nService);
		visualSearchModelAttributeProvider.setCatalogVersionService(catalogVersionService);

		given(typeService.getComposedTypeForClass(ProductModel.class)).willReturn(composedType);
		given(Boolean.valueOf(typeService.hasAttribute(eq(composedType), any(String.class)))).willReturn(Boolean.TRUE);
		given(Boolean.valueOf(typeService.hasAttribute(composedType, INDEXED_PROPERTY_NAME))).willReturn(Boolean.TRUE);

		given(sessionService.getRawSession(any(Session.class))).willReturn(jaloSession);

		given(product.getCatalogVersion()).willReturn(catalogVersion);

		indexedProperty = new VisualSearchIndexedPropertyModel();
		indexedProperty.setName(INDEXED_PROPERTY_NAME);
	}

	@Test
	public void shouldFailIfValueIsNotCategory()
	{
		// given
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(new String());

		// then
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Value must be of type CategoryModel");

		// when
		visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);
	}

	@Test
	public void resolveAttributeValue() throws Exception
	{
		// given
		final CategoryModel category = mock(CategoryModel.class);
		given(category.getName()).willReturn(CATEGORY_NAME);
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(category);

		// when
		final String value = visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, CATEGORY_NAME);
	}

	@Test
	public void resolveCollectionAttributeValue() throws Exception
	{
		// given
		final CategoryModel category1 = mock(CategoryModel.class);
		given(category1.getName()).willReturn(CATEGORY_NAME);
		final CategoryModel category2 = mock(CategoryModel.class);
		given(category2.getName()).willReturn(CATEGORY_NAME_2);
		given(modelService.getAttributeValue(product, INDEXED_PROPERTY_NAME)).willReturn(Arrays.asList(category1, category2));

		final String expectedValue = CATEGORY_NAME + "," + CATEGORY_NAME_2;

		// when
		final String value = visualSearchModelAttributeProvider.resolveValue(product, indexedProperty);

		// then
		assertEquals(value, expectedValue);
	}
}
