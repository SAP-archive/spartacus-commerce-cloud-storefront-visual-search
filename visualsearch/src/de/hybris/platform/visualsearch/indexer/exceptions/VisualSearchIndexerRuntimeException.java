/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.exceptions;

/**
 * Common runtime exception for all visual search indexing operations.
 */
public class VisualSearchIndexerRuntimeException extends RuntimeException
{

	public VisualSearchIndexerRuntimeException()
	{
		super();
	}

	public VisualSearchIndexerRuntimeException(final String message)
	{
		super(message);
	}

	public VisualSearchIndexerRuntimeException(final Throwable cause)
	{
		super(cause);
	}

	public VisualSearchIndexerRuntimeException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
}
