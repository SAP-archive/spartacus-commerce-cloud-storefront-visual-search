package de.hybris.platform.imageservice.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchImageItemData
{
  private String id;
  private String label;
  private BigDecimal x1;
  private BigDecimal y1;
  private BigDecimal x2;
  private BigDecimal y2;
}
