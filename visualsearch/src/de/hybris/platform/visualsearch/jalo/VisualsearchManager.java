/*
 *  
 * [y] hybris Platform
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.visualsearch.jalo;

import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import de.hybris.platform.visualsearch.constants.VisualsearchConstants;
import org.apache.log4j.Logger;

public class VisualsearchManager extends GeneratedVisualsearchManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( VisualsearchManager.class.getName() );
	
	public static final VisualsearchManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (VisualsearchManager) em.getExtension(VisualsearchConstants.EXTENSIONNAME);
	}
	
}
