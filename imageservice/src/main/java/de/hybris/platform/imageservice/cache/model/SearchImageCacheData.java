package de.hybris.platform.imageservice.cache.model;

import java.io.Serializable;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchImageCacheData implements Serializable
{
  private static final long serialVersionUID = -4873254992661735776L;
  private String key;
  private String link;
  private Collection<String> ids;

}
