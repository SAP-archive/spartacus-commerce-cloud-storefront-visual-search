package de.hybris.platform.imageservice.services;

import java.util.Collection;
import de.hybris.platform.imageservice.dto.SearchImageItemData;

public interface SearchImageParseService
{
  /**
   * Parses the response from search image provider.
   * 
   * @param response
   * @return {@link SearchImageItemData} information on items on image
   */
  public Collection<SearchImageItemData> parseItemsOnImage(final String response);

  /**
   * Parses the response from search image provider.
   * 
   * @param response
   * @return list of similar products
   */
  public Collection<String> parseIds(final String response);
}
