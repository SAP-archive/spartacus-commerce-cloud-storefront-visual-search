/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.exceptions;

public class VisualSearchIndexerException extends Exception
{

	public VisualSearchIndexerException()
	{
		super();
	}

	public VisualSearchIndexerException(final String message, final Throwable cause)
	{
		super(message, cause);
	}

	public VisualSearchIndexerException(final String message)
	{
		super(message);
	}

	public VisualSearchIndexerException(final Throwable cause)
	{
		super(cause);
	}
}
