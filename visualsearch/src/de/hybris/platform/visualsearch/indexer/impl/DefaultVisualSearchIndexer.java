/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.visualsearch.indexer.impl;

import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.visualsearch.indexer.VisualSearchIndexer;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerException;
import de.hybris.platform.visualsearch.indexer.exceptions.VisualSearchIndexerRuntimeException;
import de.hybris.platform.visualsearch.indexer.providers.VisualSearchValueProvider;
import de.hybris.platform.visualsearch.indexer.sftp.VisualSearchSSHDPool;
import de.hybris.platform.visualsearch.model.VisualSearchConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexConfigModel;
import de.hybris.platform.visualsearch.model.VisualSearchIndexedPropertyModel;
import de.hybris.platform.visualsearch.model.VisualSearchServerConfigModel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;


public class DefaultVisualSearchIndexer implements VisualSearchIndexer, ApplicationContextAware
{
	private static final Logger LOG = Logger.getLogger(DefaultVisualSearchIndexer.class);

	private VisualSearchSSHDPool sshdPool;
	private ApplicationContext applicationContext;

	@Override
	public void generateDataFeed(final Collection<ItemModel> items, final VisualSearchConfigModel visualSearchConfig,
			final String filePath) throws VisualSearchIndexerException, InterruptedException
	{
		try
		{
			final VisualSearchIndexConfigModel indexConfig = visualSearchConfig.getIndexConfig();
			final List<VisualSearchIndexedPropertyModel> visualSearchIndexedProperties = indexConfig
					.getVisualSearchIndexedProperties();

			final File file = new File(filePath);
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true)))
			{
				generateCSVHeader(writer, visualSearchIndexedProperties);
				generateCSVContent(writer, items, visualSearchIndexedProperties);
				writer.flush();
			}
			setFilePermissions(filePath);
		}
		catch (final IOException e)
		{
			LOG.debug("Error while generating data feed");
			throw new VisualSearchIndexerException(e);
		}
	}

	@Override
	public void uploadDataFeed(final String filePath, final VisualSearchConfigModel visualSearchConfig)
			throws VisualSearchIndexerException, InterruptedException
	{
		final File file = new File(filePath);
		if (!file.exists())
		{
			throw new VisualSearchIndexerException(
					String.format("Error while uploading dataset to sftp server. File [%s] doesn't exist.", filePath));
		}

		ChannelSftp sftpChannel = null;
		final VisualSearchServerConfigModel serverConfig = visualSearchConfig.getServerConfig();
		try
		{
			sftpChannel = sshdPool.getConnection(serverConfig);
			if (!StringUtils.isBlank(serverConfig.getDestinationPath()))
			{
				sftpChannel.cd(serverConfig.getDestinationPath());
			}

			try (FileInputStream inputStream = new FileInputStream(file))
			{
				sftpChannel.put(inputStream, file.getName());
			}
			sshdPool.returnConnection(sftpChannel);
		}
		catch (final VisualSearchIndexerException | IOException | SftpException e)
		{
			LOG.error("Error while uploading datafeed to sftp.", e);
			sshdPool.invalidateObject(sftpChannel);
			throw new VisualSearchIndexerException(e);
		}
	}

	protected void generateCSVHeader(final BufferedWriter writer,
			final List<VisualSearchIndexedPropertyModel> visualSearchIndexedProperties) throws IOException
	{
		final StringBuilder builder = new StringBuilder();
		visualSearchIndexedProperties.forEach(indexProperty -> builder.append(indexProperty.getDisplayName()).append(";"));
		builder.append('\n');
		writer.append(builder.toString());
	}

	protected void generateCSVContent(final BufferedWriter writer, final Collection<ItemModel> items,
			final List<VisualSearchIndexedPropertyModel> visualSearchIndexedProperties) throws VisualSearchIndexerRuntimeException
	{
		items.forEach(item -> generateContentForItem(writer, item, visualSearchIndexedProperties));
	}

	protected void generateContentForItem(final BufferedWriter writer, final ItemModel item,
			final List<VisualSearchIndexedPropertyModel> visualSearchIndexedProperties) throws VisualSearchIndexerRuntimeException
	{
		final StringBuilder builder = new StringBuilder();
		visualSearchIndexedProperties.forEach(property -> {
			final VisualSearchValueProvider provider = getValueProvider(property.getFieldValueProvider());
			final String value = provider.resolveValue(item, property);
			builder.append(value).append(";");
		});
		try
		{
			writer.append(builder.toString()).append('\n');
		}
		catch (final IOException e)
		{
			throw new VisualSearchIndexerRuntimeException(e);
		}
	}

	public VisualSearchValueProvider getValueProvider(final String valueProviderId) throws VisualSearchIndexerRuntimeException
	{
		final Object valueProvider = applicationContext.getBean(valueProviderId);
		if (valueProvider instanceof VisualSearchValueProvider)
		{
			return (VisualSearchValueProvider) valueProvider;
		}
		else
		{
			throw new VisualSearchIndexerRuntimeException("Value provider is not of an expected type: " + valueProviderId);
		}
	}

	protected void setFilePermissions(final String filePath)
	{
		final File file = new File(filePath);
		file.setReadable(true);
		file.setExecutable(true);
	}

	@Override
	public void setApplicationContext(final ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}

	@Required
	public void setSshdPool(final VisualSearchSSHDPool sshdPool)
	{
		this.sshdPool = sshdPool;
	}

}
