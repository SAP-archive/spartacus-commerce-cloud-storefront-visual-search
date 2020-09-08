/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.util.Utilities;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.indexer.providers.impl.VisualSearchCategoryNameValueProvider;
import de.hybris.platform.visualsearch.indexer.providers.impl.VisualSearchImageUrlValueProvider;
import de.hybris.platform.visualsearch.indexer.providers.impl.VisualSearchModelAttributeProvider;
import de.hybris.platform.visualsearch.indexer.sftp.VisualSearchSSHDPool;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.jcraft.jsch.ChannelSftp;


@UnitTest
public class DefaultVisualSearchIndexerTest
{
	private static final String PRODUCT_1_CODE_VALUE = "product1";
	private static final String PRODUCT_2_CODE_VALUE = "product2";
	private static final String PRODUCT_1_CATEGORY_VALUE = "category1";
	private static final String PRODUCT_2_CATEGORY_VALUE = "category2";
	private static final String PRODUCT_1_IMAGE_URL_VALUE = "url1";
	private static final String PRODUCT_2_IMAGE_URL_VALUE = "url2";

	private static final String CODE_VALUE_PROVIDER = "codeValueProvider";
	private static final String CATEGORY_VALUE_PROVIDER = "categoryValueProvider";
	private static final String URL_VALUE_PROVIDER = "urlValueProvider";

	private static final String CODE_VS_INDEXED_PROPERTY = "code";
	private static final String CATEGORY_VS_INDEXED_PROPERTY = "category";
	private static final String IMAGE_URL_VS_INDEXED_PROPERTY = "image url";

	public static final String EXPORT_SUB_DIR = "visualsearch";
	private static final String VISUAL_SEARCH_CONFIG = "visualSearchConfig";

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Mock
	private VisualSearchSSHDPool sshdPool;
	@Mock
	private ApplicationContext applicationContext;

	@Mock
	private VisualSearchModelAttributeProvider codeProvider;
	@Mock
	private VisualSearchCategoryNameValueProvider categoryProvider;
	@Mock
	private VisualSearchImageUrlValueProvider urlProvider;

	private VisualSearchConfigModel visualSearchConfig;
	private Collection<ItemModel> items;
	private String filePath;
	private DefaultVisualSearchIndexer indexer;

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);

		indexer = new DefaultVisualSearchIndexer();
		indexer.setSshdPool(sshdPool);
		indexer.setApplicationContext(applicationContext);

		final String platformTempDir = Utilities.getPlatformConfig().getSystemConfig().getTempDir().getPath() + File.separator
				+ EXPORT_SUB_DIR;
		filePath = platformTempDir + File.separator + DefaultVisualSearchIndexerBatchProcessor.VS_FILE_PREFIX + "_1.csv";
		final File file = new File(filePath);
		if (file.exists())
		{
			file.delete();
		}

		visualSearchConfig = new VisualSearchConfigModel();
		visualSearchConfig.setName(VISUAL_SEARCH_CONFIG);
		visualSearchConfig.setServerConfig(new VisualSearchServerConfigModel());

		final VisualSearchIndexConfigModel indexConfig = new VisualSearchIndexConfigModel();
		final VisualSearchIndexedPropertyModel codeProperty = new VisualSearchIndexedPropertyModel();
		codeProperty.setDisplayName(CODE_VS_INDEXED_PROPERTY);
		codeProperty.setFieldValueProvider(CODE_VALUE_PROVIDER);

		final VisualSearchIndexedPropertyModel categoryProperty = new VisualSearchIndexedPropertyModel();
		categoryProperty.setDisplayName(CATEGORY_VS_INDEXED_PROPERTY);
		categoryProperty.setFieldValueProvider(CATEGORY_VALUE_PROVIDER);

		final VisualSearchIndexedPropertyModel urlProperty = new VisualSearchIndexedPropertyModel();
		urlProperty.setDisplayName(IMAGE_URL_VS_INDEXED_PROPERTY);
		urlProperty.setFieldValueProvider(URL_VALUE_PROVIDER);

		indexConfig.setVisualSearchIndexedProperties(Arrays.asList(codeProperty, categoryProperty, urlProperty));
		visualSearchConfig.setIndexConfig(indexConfig);

		final ProductModel product1 = new ProductModel();
		product1.setCode(PRODUCT_1_CODE_VALUE);

		final ProductModel product2 = new ProductModel();
		product2.setCode(PRODUCT_2_CODE_VALUE);

		items = Arrays.asList(product1, product2);

		given(codeProvider.resolveValue(product1, codeProperty)).willReturn(PRODUCT_1_CODE_VALUE);
		given(codeProvider.resolveValue(product2, codeProperty)).willReturn(PRODUCT_2_CODE_VALUE);
		given(categoryProvider.resolveValue(product1, categoryProperty)).willReturn(PRODUCT_1_CATEGORY_VALUE);
		given(categoryProvider.resolveValue(product2, categoryProperty)).willReturn(PRODUCT_2_CATEGORY_VALUE);
		given(urlProvider.resolveValue(product1, urlProperty)).willReturn(PRODUCT_1_IMAGE_URL_VALUE);
		given(urlProvider.resolveValue(product2, urlProperty)).willReturn(PRODUCT_2_IMAGE_URL_VALUE);

		given(sshdPool.getConnection(visualSearchConfig.getServerConfig())).willReturn(mock(ChannelSftp.class));
	}

	@Test
	public void generateDataFeedNoBeanValueProvider() throws Exception
	{
		// given
		given(applicationContext.getBean(CODE_VALUE_PROVIDER)).willThrow(BeansException.class);

		// expect
		expectedException.expect(InstantiationError.class);

		// when
		indexer.generateDataFeed(items, visualSearchConfig, filePath);
	}

	@Test
	public void generateDataFeedValueProviderWrongType() throws Exception
	{
		// given
		given(applicationContext.getBean(CODE_VALUE_PROVIDER)).willReturn(mock(VisualSearchIndexedPropertyModel.class));

		// expect
		expectedException.expect(VisualSearchIndexerRuntimeException.class);
		expectedException.expectMessage("Value provider is not of an expected type: " + CODE_VALUE_PROVIDER);

		// when
		indexer.generateDataFeed(items, visualSearchConfig, filePath);
	}

	@Test
	public void generateDataFeedSuccess() throws Exception
	{
		// given
		given(applicationContext.getBean(CODE_VALUE_PROVIDER)).willReturn(codeProvider);
		given(applicationContext.getBean(CATEGORY_VALUE_PROVIDER)).willReturn(categoryProvider);
		given(applicationContext.getBean(URL_VALUE_PROVIDER)).willReturn(urlProvider);

		// when
		indexer.generateDataFeed(items, visualSearchConfig, filePath);

		// then
		final File file = new File(filePath);
		assertTrue(file.exists());
		assertTrue(file.canExecute());
		assertTrue(file.canRead());

		final String content = Files.readString(Paths.get(file.getPath()), StandardCharsets.US_ASCII);
		final String expected = getFileContent();
		assertEquals(expected, content);

		file.delete();
	}

	@Test
	public void uploadDataFeedNoFile() throws Exception
	{
		// expect
		expectedException.expect(VisualSearchIndexerException.class);
		expectedException
				.expectMessage(String.format("Error while uploading dataset to sftp server. File [%s] doesn't exist.", filePath));

		// when
		indexer.uploadDataFeed(filePath, visualSearchConfig);
	}

	@Test
	public void uploadDataFeed() throws Exception
	{
		// given
		given(applicationContext.getBean(CODE_VALUE_PROVIDER)).willReturn(codeProvider);
		given(applicationContext.getBean(CATEGORY_VALUE_PROVIDER)).willReturn(categoryProvider);
		given(applicationContext.getBean(URL_VALUE_PROVIDER)).willReturn(urlProvider);
		indexer.generateDataFeed(items, visualSearchConfig, filePath);

		// when
		indexer.uploadDataFeed(filePath, visualSearchConfig);

		Files.deleteIfExists(Paths.get(filePath));
	}

	private String getFileContent()
	{
		final StringBuffer buffer = new StringBuffer();
		buffer.append(CODE_VS_INDEXED_PROPERTY).append(';').append(CATEGORY_VS_INDEXED_PROPERTY).append(';')
				.append(IMAGE_URL_VS_INDEXED_PROPERTY).append(';').append('\n');
		buffer.append(PRODUCT_1_CODE_VALUE).append(';').append(PRODUCT_1_CATEGORY_VALUE).append(';')
				.append(PRODUCT_1_IMAGE_URL_VALUE).append(';').append('\n');
		buffer.append(PRODUCT_2_CODE_VALUE).append(';').append(PRODUCT_2_CATEGORY_VALUE).append(';')
				.append(PRODUCT_2_IMAGE_URL_VALUE).append(';').append('\n');

		return buffer.toString();
	}
}
