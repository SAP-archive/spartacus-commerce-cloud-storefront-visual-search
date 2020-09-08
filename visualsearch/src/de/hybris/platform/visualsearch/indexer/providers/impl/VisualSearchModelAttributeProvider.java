/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.providers.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.session.Session;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.type.TypeService;
import de.hybris.platform.servicelayer.util.ServicesUtil;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.indexer.providers.VisualSearchValueProvider;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Required;


public class VisualSearchModelAttributeProvider implements VisualSearchValueProvider
{
	public static final String ATTRIBUTE_PARAM = "attribute";

	private ModelService modelService;
	private TypeService typeService;
	private SessionService sessionService;
	private I18NService i18nService;
	private CatalogVersionService catalogVersionService;

	@Override
	public String resolveValue(final ItemModel model, final VisualSearchIndexedPropertyModel visualSearchIndexedProperty)
			throws VisualSearchIndexerRuntimeException
	{
		ServicesUtil.validateParameterNotNull("model", "model instance is null");
		if (model instanceof ProductModel)
		{
			final ProductModel product = (ProductModel) model;
			try
			{
				createLocalSessionContext();
				i18nService.setLocalizationFallbackEnabled(true);
				catalogVersionService.setSessionCatalogVersions(Collections.singleton(product.getCatalogVersion()));
				return doResolveValue(product, visualSearchIndexedProperty);
			}
			finally
			{
				removeLocalSessionContext();
			}
		}
		else
		{
			throw new VisualSearchIndexerRuntimeException("Item to be indexed must be of Product type");
		}

	}

	protected String doResolveValue(final ProductModel model, final VisualSearchIndexedPropertyModel visualSearchIndexedProperty)
	{
		Object value = null;

		final String attributeName = getAttributeName(visualSearchIndexedProperty);
		if (StringUtils.isNotEmpty(attributeName))
		{
			final ComposedTypeModel composedType = typeService.getComposedTypeForClass(ProductModel.class);

			if (typeService.hasAttribute(composedType, attributeName))
			{
				value = modelService.getAttributeValue(model, attributeName);
			}
			else
			{
				throw new VisualSearchIndexerRuntimeException("Unsupported attribute " + attributeName);
			}
		}

		return getStringValueOfObject(value);

	}

	protected String getStringValueOfObject(final Object value)
	{
		if (value == null)
		{
			return "";
		}

		if (value instanceof Collection)
		{
			final Collection<Object> fieldValues = (Collection<Object>) value;
			return fieldValues.stream().map(fieldValue -> valueToString(fieldValue)).collect(Collectors.joining(","));
		}
		else
		{
			return valueToString(value);
		}
	}

	protected String valueToString(final Object value)
	{
		return value.toString();
	}

	protected String getAttributeName(final VisualSearchIndexedPropertyModel visualSearchIndexedProperty)
	{
		String attributeName = visualSearchIndexedProperty.getName();
		final Map<String, String> valueProviderParameters = visualSearchIndexedProperty.getValueProviderParameters();

		if (valueProviderParameters != null)
		{
			attributeName = StringUtils.trimToNull(valueProviderParameters.get(ATTRIBUTE_PARAM));
		}

		return attributeName;
	}

	protected void createLocalSessionContext()
	{
		final Session session = sessionService.getCurrentSession();
		final JaloSession jaloSession = (JaloSession) sessionService.getRawSession(session);
		jaloSession.createLocalSessionContext();
	}

	protected void removeLocalSessionContext()
	{
		final Session session = sessionService.getCurrentSession();
		final JaloSession jaloSession = (JaloSession) sessionService.getRawSession(session);
		jaloSession.removeLocalSessionContext();
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	@Required
	public void setTypeService(final TypeService typeService)
	{
		this.typeService = typeService;
	}

	@Required
	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	@Required
	public void setI18nService(final I18NService i18nService)
	{
		this.i18nService = i18nService;
	}

	@Required
	public void setCatalogVersionService(final CatalogVersionService catalogVersionService)
	{
		this.catalogVersionService = catalogVersionService;
	}

}
