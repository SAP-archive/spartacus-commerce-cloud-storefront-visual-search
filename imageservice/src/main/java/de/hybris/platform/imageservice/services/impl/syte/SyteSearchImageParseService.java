package de.hybris.platform.imageservice.services.impl.syte;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import de.hybris.platform.imageservice.dto.SearchImageItemData;
import de.hybris.platform.imageservice.services.SearchImageParseService;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link SearchImageParseService} for parsing results from Syte.io.
 */
@Service("searchImageParseService")
@Slf4j
public class SyteSearchImageParseService implements SearchImageParseService
{
  private static final String SYTE_RESPONSE_PROPERTY_SKU = "sku";
  private static final String SYTE_REPOSNE_PROPERTY_ADS = "ads";

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public List<SearchImageItemData> parseItemsOnImage(String response)
  {
    log.info("RESPONSE: {}", response);

    JsonParser jsonParser = JsonParserFactory.getJsonParser();
    Map<String, Object> responseMap = jsonParser.parseMap(response);
    List<Map<String, Object>> offers = (List) responseMap.entrySet()
        .iterator()
        .next()
        .getValue();

    return offers.stream()
        .map(this::createSearchImageData)
        .collect(Collectors.toList());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public List<String> parseIds(String response)
  {
    log.info("SIMILAR PRODUCTS RESPONSE: {}", response);

    JsonParser jsonParser = JsonParserFactory.getJsonParser();
    Map<String, Object> responseMap = jsonParser.parseMap(response);
    List<Map<String, Object>> ads = (List) responseMap.get(SYTE_REPOSNE_PROPERTY_ADS);

    return ads.stream()
        .map(ad -> (String) ad.get(SYTE_RESPONSE_PROPERTY_SKU))
        .collect(Collectors.toList());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private SearchImageItemData createSearchImageData(final Map<String, Object> offer)
  {
    SearchImageItemData bb = new SearchImageItemData();
    bb.setId((String) offer.get("offers"));
    bb.setLabel((String) offer.get("label"));
    List<Double> b0 = (List) offer.get("b0");
    bb.setX1(BigDecimal.valueOf(b0.get(0)));
    bb.setY1(BigDecimal.valueOf(b0.get(1)));
    List<Double> b1 = (List) offer.get("b1");
    bb.setX2(BigDecimal.valueOf(b1.get(0)));
    bb.setY2(BigDecimal.valueOf(b1.get(1)));
    return bb;
  }
}
