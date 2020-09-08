package de.hybris.platform.imageservice.dto;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchImageData
{

  private Collection<SearchImageItemData> boundingBoxes;

}
