/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.backoffice.actions.renderers;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.impl.DefaultActionRenderer;


public class VisualSearchIndexerActionRenderer extends DefaultActionRenderer<String, Object>
{
	protected static final String ACTION_NAME = "vssynccatalog.action.name";

	@Override
	protected String getLocalizedName(final ActionContext<?> context)
	{
		return context.getLabel(ACTION_NAME);
	}
}
