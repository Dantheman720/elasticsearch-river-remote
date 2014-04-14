/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.river.remote.testtools;

import java.util.Map;

import org.elasticsearch.common.settings.SettingsException;
import org.jboss.elasticsearch.tools.content.PreprocessChainContext;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorBase;

/**
 * Implementation of {@link StructuredContentPreprocessorBase} for configuration loading tests.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class DataPreprocessorMock extends StructuredContentPreprocessorBase {

	public Map<String, Object> settings = null;

	@Override
	public void init(Map<String, Object> settings) throws SettingsException {
		this.settings = settings;
	}

	@Override
	public Map<String, Object> preprocessData(Map<String, Object> issueData, PreprocessChainContext chainContext) {
		return issueData;
	}

}
