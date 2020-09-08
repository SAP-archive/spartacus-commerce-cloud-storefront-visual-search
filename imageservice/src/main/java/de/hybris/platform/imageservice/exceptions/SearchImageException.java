package de.hybris.platform.imageservice.exceptions;

/**
 * Exception occurred during image search process.
 */
public class SearchImageException extends RuntimeException
{
  private static final long serialVersionUID = -7401982096323330675L;

  public SearchImageException(final String message)
  {
    super(message);
  }

  public SearchImageException(final Throwable t)
  {
    super(t);
  }

  public SearchImageException(final String message, final Throwable t)
  {
    super(message, t);
  }
}
