/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.elasticsearch.river.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.xcontent.XContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentGenerator;
import org.elasticsearch.river.RiverName;
import org.jboss.elasticsearch.river.remote.testtools.TestUtils;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DocumentWithCommentsIndexStructureBuilder}.
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 * @author Lukáš Vlček (lvlcek@redhat.com)
 */
public class DocumentWithCommentsIndexStructureBuilderTest {

	private static ObjectMapper mapper;

	private JsonNode toJsonNode(String source) {
		JsonNode node = null;
		try {
			node = mapper.readValue(source, JsonNode.class);
		} catch (IOException e) {
			fail("Exception while parsing!: " + e);
		}
		return node;
	}

	@BeforeClass
	public static void setUp() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Test
	public void configuration_read_ok() {

		IESIntegration esIntegrationMock = mockEsIntegrationComponent();

		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(esIntegrationMock,
				"index_name", "type_name", loadTestSettings("/index_structure_configuration_test_ok.json"), true);
		Assert.assertEquals("river_name", tested.riverName);
		Assert.assertEquals("index_name", tested.indexName);
		Assert.assertEquals("type_name", tested.issueTypeName);
		Assert.assertEquals("document_id", tested.remoteDataFieldForDocumentId);
		Assert.assertEquals("updated", tested.remoteDataFieldForUpdated);

		Assert.assertEquals("delFlag", tested.remoteDataFieldForDeleted);
		Assert.assertEquals("true", tested.remoteDataValueForDeleted);

		Assert.assertEquals("river_name", tested.indexFieldForRiverName);
		Assert.assertEquals("space_key_field", tested.indexFieldForSpaceKey);
		Assert.assertEquals("document_id_field", tested.indexFieldForRemoteDocumentId);
		Assert.assertEquals(CommentIndexingMode.CHILD, tested.commentIndexingMode);
		Assert.assertEquals("all_comments", tested.indexFieldForComments);
		Assert.assertEquals("jira_issue_comment_type", tested.commentTypeName);

		Assert.assertEquals(5, tested.fieldsConfig.size());
		assertFieldConfiguration(tested.fieldsConfig, "created", "fields.created", null);
		assertFieldConfiguration(tested.fieldsConfig, "reporter", "fields.reporter", "user2");
		assertFieldConfiguration(tested.fieldsConfig, "assignee", "fields.assignee", "user2");
		assertFieldConfiguration(tested.fieldsConfig, "fix_versions", "fields.fixVersions", "name2");
		assertFieldConfiguration(tested.fieldsConfig, "components", "fields.components", "name2");

		Assert.assertEquals(2, tested.filtersConfig.size());
		Assert.assertTrue(tested.filtersConfig.containsKey("user2"));
		Assert.assertTrue(tested.filtersConfig.containsKey("name2"));

		Map<String, String> userFilter = tested.filtersConfig.get("user2");
		Assert.assertEquals(2, userFilter.size());
		Assert.assertEquals("username2", userFilter.get("name"));
		Assert.assertEquals("display_name2", userFilter.get("displayName"));

		Assert.assertEquals(4, tested.commentFieldsConfig.size());
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_body", "body", null);
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_author2", "author", "user2");
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_updater", "updateAuthor", "user2");
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_created", "created", null);

		Mockito.verify(esIntegrationMock).createLogger(DocumentWithCommentsIndexStructureBuilder.class);

	}

	@Test
	public void configuration_read_ok_noupdatedmandatory() {

		Map<String, Object> settings = loadTestSettings("/index_structure_configuration_test_ok.json");
		settings.remove(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_UPDATED);
		settings.remove(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DELETED);
		settings.remove(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DELETEDVALUE);

		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "index_name", "type_name", settings, false);
		Assert.assertEquals("river_name", tested.riverName);
		Assert.assertEquals("index_name", tested.indexName);
		Assert.assertEquals("type_name", tested.issueTypeName);
		Assert.assertEquals("document_id", tested.remoteDataFieldForDocumentId);
		Assert.assertEquals(null, tested.remoteDataFieldForUpdated);
		Assert.assertNull(tested.remoteDataFieldForDeleted);
		Assert.assertNull(tested.remoteDataValueForDeleted);
		Assert.assertEquals("river_name", tested.indexFieldForRiverName);
		Assert.assertEquals("space_key_field", tested.indexFieldForSpaceKey);
		Assert.assertEquals("document_id_field", tested.indexFieldForRemoteDocumentId);
		Assert.assertEquals(CommentIndexingMode.CHILD, tested.commentIndexingMode);
		Assert.assertEquals("all_comments", tested.indexFieldForComments);
		Assert.assertEquals("jira_issue_comment_type", tested.commentTypeName);

		Assert.assertEquals(5, tested.fieldsConfig.size());
		assertFieldConfiguration(tested.fieldsConfig, "created", "fields.created", null);
		assertFieldConfiguration(tested.fieldsConfig, "reporter", "fields.reporter", "user2");
		assertFieldConfiguration(tested.fieldsConfig, "assignee", "fields.assignee", "user2");
		assertFieldConfiguration(tested.fieldsConfig, "fix_versions", "fields.fixVersions", "name2");
		assertFieldConfiguration(tested.fieldsConfig, "components", "fields.components", "name2");

		Assert.assertEquals(2, tested.filtersConfig.size());
		Assert.assertTrue(tested.filtersConfig.containsKey("user2"));
		Assert.assertTrue(tested.filtersConfig.containsKey("name2"));

		Map<String, String> userFilter = tested.filtersConfig.get("user2");
		Assert.assertEquals(2, userFilter.size());
		Assert.assertEquals("username2", userFilter.get("name"));
		Assert.assertEquals("display_name2", userFilter.get("displayName"));

		Assert.assertEquals(4, tested.commentFieldsConfig.size());
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_body", "body", null);
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_author2", "author", "user2");
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_updater", "updateAuthor", "user2");
		assertFieldConfiguration(tested.commentFieldsConfig, "comment_created", "created", null);

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> loadTestSettings(String file) {
		return (Map<String, Object>) Utils.loadJSONFromJarPackagedFile(file).get("index");
	}

	@Test
	public void configuration_read_validation() {

		try {
			Map<String, Object> config = loadTestSettings("/index_structure_configuration_test_ok.json");
			config.remove(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DOCUMENTID);
			new DocumentWithCommentsIndexStructureBuilder(mockEsIntegrationComponent(), "index_name", "type_name", config,
					true);
			Assert.fail("SettingsException must be thrown");
		} catch (SettingsException e) {
			System.out.println(e.getMessage());
			Assert.assertEquals("String value must be provided for 'index/remote_field_document_id' configuration!",
					e.getMessage());
		}

		try {
			Map<String, Object> config = loadTestSettings("/index_structure_configuration_test_ok.json");
			config.put(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DOCUMENTID, " ");
			new DocumentWithCommentsIndexStructureBuilder(mockEsIntegrationComponent(), "index_name", "type_name", config,
					true);
			Assert.fail("SettingsException must be thrown");
		} catch (SettingsException e) {
			System.out.println(e.getMessage());
			Assert.assertEquals("String value must be provided for 'index/remote_field_document_id' configuration!",
					e.getMessage());
		}

		try {
			Map<String, Object> config = loadTestSettings("/index_structure_configuration_test_ok.json");
			config.remove(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DELETEDVALUE);
			new DocumentWithCommentsIndexStructureBuilder(mockEsIntegrationComponent(), "index_name", "type_name", config,
					true);
			Assert.fail("SettingsException must be thrown");
		} catch (SettingsException e) {
			System.out.println(e.getMessage());
			Assert
					.assertEquals(
							"Configuration fields 'index/remote_field_deleted' and 'index/remote_field_deleted_value' must be both set or both empty",
							e.getMessage());
		}
		// TODO other validation tests

	}

	@Test
	public void configuration_defaultLoading() {
		Map<String, Object> settings = createSettingsWithMandatoryFilled();
		assertDefaultConfigurationLoaded(new DocumentWithCommentsIndexStructureBuilder(mockEsIntegrationComponent(),
				"index_name", "type_name", settings, true));

	}

	@SuppressWarnings("rawtypes")
	public static Map<String, Object> createSettingsWithMandatoryFilled() {
		Map<String, Object> settings = new HashMap<String, Object>();

		// fill some mandatory fields not loaded from default
		settings.put(DocumentWithCommentsIndexStructureBuilder.CONFIG_FIELDS, new HashMap());
		settings.put(DocumentWithCommentsIndexStructureBuilder.CONFIG_FILTERS, new HashMap());
		settings.put(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_DOCUMENTID, "docid");
		settings.put(DocumentWithCommentsIndexStructureBuilder.CONFIG_REMOTEFIELD_UPDATED, "up");
		return settings;
	}

	private void assertDefaultConfigurationLoaded(DocumentWithCommentsIndexStructureBuilder tested) {
		Assert.assertEquals("river_name", tested.riverName);
		Assert.assertEquals("index_name", tested.indexName);
		Assert.assertEquals("type_name", tested.issueTypeName);
		Assert.assertEquals("source", tested.indexFieldForRiverName);
		Assert.assertEquals("space_key", tested.indexFieldForSpaceKey);
		Assert.assertEquals("document_id", tested.indexFieldForRemoteDocumentId);
		Assert.assertEquals(CommentIndexingMode.NONE, tested.commentIndexingMode);

		Assert.assertEquals(0, tested.fieldsConfig.size());
		Assert.assertEquals(0, tested.filtersConfig.size());
	}

	private void assertFieldConfiguration(Map<String, Map<String, String>> fieldsConfig, String indexFieldName,
			String jiraFieldName, String filter) {
		Assert.assertTrue(fieldsConfig.containsKey(indexFieldName));
		Map<String, String> field = fieldsConfig.get(indexFieldName);
		Assert.assertEquals(jiraFieldName, field.get(DocumentWithCommentsIndexStructureBuilder.CONFIG_FIELDS_REMOTEFIELD));
		Assert.assertEquals(filter, field.get(DocumentWithCommentsIndexStructureBuilder.CONFIG_FIELDS_VALUEFILTER));
	}

	@Test
	public void addIssueDataPreprocessor() {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), null, null, createSettingsWithMandatoryFilled(), true);

		// case - not NPE
		tested.addDataPreprocessor(null);

		// case - preprocessors adding
		tested.addDataPreprocessor(mock(StructuredContentPreprocessor.class));
		Assert.assertEquals(1, tested.issueDataPreprocessors.size());

		tested.addDataPreprocessor(mock(StructuredContentPreprocessor.class));
		tested.addDataPreprocessor(mock(StructuredContentPreprocessor.class));
		tested.addDataPreprocessor(mock(StructuredContentPreprocessor.class));
		Assert.assertEquals(4, tested.issueDataPreprocessors.size());

	}

	@Test
	public void preprocessIssueData() {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), null, null, createSettingsWithMandatoryFilled(), true);

		Map<String, Object> issue = null;
		// case - no NPE and change when no preprocessors defined and issue data are null
		Assert.assertNull(tested.preprocessDocumentData("ORG", issue));

		// case - no NPE and change when no preprocessors defined and issue data are notnull
		{
			issue = new HashMap<String, Object>();
			issue.put("key", "ORG-1545");

			Map<String, Object> ret = tested.preprocessDocumentData("ORG", issue);
			Assert.assertEquals(issue, ret);
			Assert.assertEquals(1, ret.size());
			Assert.assertEquals("ORG-1545", ret.get("key"));
		}

		// case - all preprocessors called
		{
			StructuredContentPreprocessor idp1 = mock(StructuredContentPreprocessor.class);
			StructuredContentPreprocessor idp2 = mock(StructuredContentPreprocessor.class);
			when(idp1.preprocessData(issue)).thenAnswer(new Answer<Map<String, Object>>() {
				@SuppressWarnings("unchecked")
				@Override
				public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
					Map<String, Object> ret = (Map<String, Object>) invocation.getArguments()[0];
					ret.put("idp1", "called");
					return ret;
				}
			});
			when(idp2.preprocessData(issue)).thenAnswer(new Answer<Map<String, Object>>() {
				@SuppressWarnings("unchecked")
				@Override
				public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
					Map<String, Object> ret = (Map<String, Object>) invocation.getArguments()[0];
					ret.put("idp2", "called");
					return ret;
				}
			});

			tested.addDataPreprocessor(idp1);
			tested.addDataPreprocessor(idp2);
			Map<String, Object> ret = tested.preprocessDocumentData("ORG", issue);
			Assert.assertEquals(issue, ret);
			Assert.assertEquals(3, ret.size());
			Assert.assertEquals("ORG-1545", ret.get("key"));
			Assert.assertEquals("called", ret.get("idp1"));
			Assert.assertEquals("called", ret.get("idp2"));
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void buildSearchForIndexedDocumentsNotUpdatedAfter() throws IOException {

		Client client = Mockito.mock(Client.class);

		Map<String, Object> settings = (Map<String, Object>) Utils.loadJSONFromJarPackagedFile(
				"/index_structure_configuration_test_ok.json").get("index");
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "issue_type", settings, true);
		tested.commentTypeName = "comment_type";

		// case - comments NONE
		{
			tested.commentIndexingMode = CommentIndexingMode.NONE;
			SearchRequestBuilder srb = new SearchRequestBuilder(client);
			tested.buildSearchForIndexedDocumentsNotUpdatedAfter(srb, "ORG",
					DateTimeUtils.parseISODateTime("2012-09-06T12:22:19Z"));
			Assert.assertArrayEquals(new String[] { "issue_type" }, srb.request().types());
			assertTrue(
					"Should equals but is: \n" + srb.toString(),
					toJsonNode(srb.toString()).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/buildSearchForIndexedDocumentsNotUpdatedAfter.json"))));
		}

		// case - comments EMBEDDED
		{
			tested.commentIndexingMode = CommentIndexingMode.EMBEDDED;
			SearchRequestBuilder srb = new SearchRequestBuilder(client);
			tested.buildSearchForIndexedDocumentsNotUpdatedAfter(srb, "ORG",
					DateTimeUtils.parseISODateTime("2012-09-06T12:22:19Z"));
			Assert.assertArrayEquals(new String[] { "issue_type" }, srb.request().types());
			assertTrue(
					"Should equals",
					toJsonNode(srb.toString()).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/buildSearchForIndexedDocumentsNotUpdatedAfter.json"))));
		}

		// case - comments EMBEDDED
		{
			tested.commentIndexingMode = CommentIndexingMode.CHILD;
			SearchRequestBuilder srb = new SearchRequestBuilder(client);
			tested.buildSearchForIndexedDocumentsNotUpdatedAfter(srb, "ORG",
					DateTimeUtils.parseISODateTime("2012-09-06T12:22:19Z"));
			Assert.assertArrayEquals(new String[] { "issue_type", "comment_type" }, srb.request().types());
			assertTrue(
					"Should equals",
					toJsonNode(srb.toString()).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/buildSearchForIndexedDocumentsNotUpdatedAfter.json"))));
		}

		// case - comments EMBEDDED
		{
			tested.commentIndexingMode = CommentIndexingMode.STANDALONE;
			SearchRequestBuilder srb = new SearchRequestBuilder(client);
			tested.buildSearchForIndexedDocumentsNotUpdatedAfter(srb, "ORG",
					DateTimeUtils.parseISODateTime("2012-09-06T12:22:19Z"));
			Assert.assertArrayEquals(new String[] { "issue_type", "comment_type" }, srb.request().types());
			assertTrue(
					"Should equals",
					toJsonNode(srb.toString()).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/buildSearchForIndexedDocumentsNotUpdatedAfter.json"))));

		}

	}

	@Test
	public void prepareIssueIndexedDocument() throws Exception {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "issue_type",
				loadTestSettings("/index_structure_configuration_test_ok.json"), true);

		// case - no comments
		{
			tested.commentIndexingMode = CommentIndexingMode.NONE;

			String res = tested.prepareIndexedDocument("ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"))
					.string();
			assertTrue(
					"Should equals",
					toJsonNode(res).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/prepareIssueIndexedDocument_ORG-1501_NOCOMMENT.json"))));
		}

		tested.remoteDataFieldForComments = "fields.comment.comments";
		// case - comments as CHILD so not in this document
		{
			tested.commentIndexingMode = CommentIndexingMode.CHILD;

			String res = tested.prepareIndexedDocument("ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"))
					.string();
			assertTrue(
					"Should equals",
					toJsonNode(res).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/prepareIssueIndexedDocument_ORG-1501_NOCOMMENT.json"))));
		}

		// case - comments as STANDALONE so not in this document
		{
			tested.commentIndexingMode = CommentIndexingMode.STANDALONE;

			String res = tested.prepareIndexedDocument("ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"))
					.string();
			assertTrue(
					"Should equals",
					toJsonNode(res).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/prepareIssueIndexedDocument_ORG-1501_NOCOMMENT.json"))));
		}

		// case - comments as EMBEDDED so present in this document
		{
			tested.commentIndexingMode = CommentIndexingMode.EMBEDDED;

			String res = tested.prepareIndexedDocument("ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"))
					.string();
			assertTrue(
					"Should equals",
					toJsonNode(res).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/prepareIssueIndexedDocument_ORG-1501_COMMENTS.json"))));
		}

		// case - comments as EMBEDDED but not in source so no present in this document
		{
			tested.commentIndexingMode = CommentIndexingMode.EMBEDDED;

			String res = tested.prepareIndexedDocument("ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1523"))
					.string();
			System.out.println(res);
			assertTrue(
					"Should equals",
					toJsonNode(res).equals(
							toJsonNode(TestUtils
									.readStringFromClasspathFile("/asserts/prepareIssueIndexedDocument_ORG-1523_NOCOMMENTS.json"))));
		}

	}

	@Test
	public void prepareCommentIndexedDocument() throws Exception {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "issue_type",
				loadTestSettings("/index_structure_configuration_test_ok.json"), true);
		tested.remoteDataFieldForComments = "fields.comment.comments";
		Map<String, Object> issue = TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501");
		List<Map<String, Object>> comments = tested.extractComments(issue);

		String res = tested.prepareCommentIndexedDocument("ORG", "ORG-1501", comments.get(0)).string();
		assertTrue(
				"Should equals",
				toJsonNode(res)
						.equals(
								toJsonNode(TestUtils
										.readStringFromClasspathFile("/asserts/prepareCommentIndexedDocument_ORG-1501_1.json"))));
	}

	@Test
	public void extractDocumentId() {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "doc_type", createSettingsWithMandatoryFilled(), true);
		tested.remoteDataFieldForDocumentId = null;

		Map<String, Object> document = new HashMap<String, Object>();
		document.put("key", "ORG-15");
		document.put("key2", new Integer(10));
		Utils.putValueIntoMapOfMaps(document, "key3.value", "ORG-17");

		// case - normal key extraction from String
		tested.remoteDataFieldForDocumentId = "key";
		Assert.assertEquals("ORG-15", tested.extractDocumentId(document));

		// case - normal key extraction from Integer
		tested.remoteDataFieldForDocumentId = "key2";
		Assert.assertEquals("10", tested.extractDocumentId(document));

		// case - dot notation
		tested.remoteDataFieldForDocumentId = "key3.value";
		Assert.assertEquals("ORG-17", tested.extractDocumentId(document));

		// case - id not found so null is returned
		tested.remoteDataFieldForDocumentId = "key_unknown";
		Assert.assertEquals(null, tested.extractDocumentId(document));

		// case - bad type for id
		try {
			tested.remoteDataFieldForDocumentId = "key3";
			tested.extractDocumentId(document);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
	}

	@Test
	public void extractDocumentUpdated() {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "doc_type", createSettingsWithMandatoryFilled(), true);
		tested.remoteDataFieldForUpdated = null;

		Map<String, Object> document = new HashMap<String, Object>();
		document.put("key", "12564");
		document.put("key2", new Integer(10));
		document.put("key3", new Long(11));
		Utils.putValueIntoMapOfMaps(document, "key4.value", "2012-09-06T02:26:53.000-0400");
		document.put("badformat", "adafsf");

		// case - date extraction from String with number
		tested.remoteDataFieldForUpdated = "key";
		Assert.assertEquals(new Date(12564l), tested.extractDocumentUpdated(document));

		// case - date extraction from Integer
		tested.remoteDataFieldForUpdated = "key2";
		Assert.assertEquals(new Date(10l), tested.extractDocumentUpdated(document));

		// case - date extraction from Integer
		tested.remoteDataFieldForUpdated = "key3";
		Assert.assertEquals(new Date(11l), tested.extractDocumentUpdated(document));

		// case - dot notation and extraction from ISO format
		tested.remoteDataFieldForUpdated = "key4.value";
		Assert.assertEquals(DateTimeUtils.parseISODateTime("2012-09-06T02:26:53.000-0400"),
				tested.extractDocumentUpdated(document));

		// case - value not found so null is returned
		tested.remoteDataFieldForUpdated = "key_unknown";
		Assert.assertEquals(null, tested.extractDocumentUpdated(document));

		// case - bad format not parseable to ISO date nor number
		try {
			tested.remoteDataFieldForUpdated = "badformat";
			tested.extractDocumentUpdated(document);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

		// case - bad type ov value in map
		try {
			tested.remoteDataFieldForUpdated = "key4";
			tested.extractDocumentUpdated(document);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

	}

	@Test
	public void extractDocumentDeleted() {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "doc_type", createSettingsWithMandatoryFilled(), true);
		tested.remoteDataFieldForDeleted = null;
		tested.remoteDataValueForDeleted = null;
		Map<String, Object> document = new HashMap<String, Object>();

		Assert.assertFalse(tested.extractDocumentDeleted(null));
		Assert.assertFalse(tested.extractDocumentDeleted(document));

		tested.remoteDataFieldForDeleted = "delflag";
		tested.remoteDataValueForDeleted = "true";

		Assert.assertFalse(tested.extractDocumentDeleted(null));
		Assert.assertFalse(tested.extractDocumentDeleted(document));

		document.put(tested.remoteDataFieldForDeleted, Boolean.TRUE);
		Assert.assertTrue(tested.extractDocumentDeleted(document));

		document.put(tested.remoteDataFieldForDeleted, tested.remoteDataValueForDeleted);
		Assert.assertTrue(tested.extractDocumentDeleted(document));

		// case sensitive evaluation of the value
		document.put(tested.remoteDataFieldForDeleted, "True");
		Assert.assertFalse(tested.extractDocumentDeleted(document));

		// Integer value from remote data
		tested.remoteDataValueForDeleted = "1";
		document.put(tested.remoteDataFieldForDeleted, new Integer(0));
		Assert.assertFalse(tested.extractDocumentDeleted(document));
		document.put(tested.remoteDataFieldForDeleted, new Integer(1));
		Assert.assertTrue(tested.extractDocumentDeleted(document));

		// bad type in remote data
		try {
			document.put(tested.remoteDataFieldForDeleted, new ArrayList<>());
			tested.extractDocumentDeleted(document);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}
		try {
			document.put(tested.remoteDataFieldForDeleted, new HashMap<>());
			tested.extractDocumentDeleted(document);
			Assert.fail("SettingsException expected");
		} catch (SettingsException e) {
			// OK
		}

	}

	@SuppressWarnings("unchecked")
	@Test
	public void indexDocument() throws Exception {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), "search_index", "issue_type",
				loadTestSettings("/index_structure_configuration_test_ok.json"), true);
		tested.remoteDataFieldForComments = "fields.comment.comments";
		Client client = Mockito.mock(Client.class);

		// case - comments NONE
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.NONE;
			Map<String, Object> document = TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501");
			tested.indexDocument(esBulk, "ORG", document);
			Assert.assertEquals("ORG", document.get("spaceKey"));
			Assert.assertEquals(1, esBulk.request().numberOfActions());
		}

		// case - comments EMBEDDED
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.EMBEDDED;
			tested.indexDocument(esBulk, "ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"));
			Assert.assertEquals(1, esBulk.request().numberOfActions());
		}

		// case - comments CHILD
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.CHILD;
			tested.indexDocument(esBulk, "ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"));
			Assert.assertEquals(3, esBulk.request().numberOfActions());
		}

		// case - comments STANDALONE with comments in issue
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.STANDALONE;
			tested.indexDocument(esBulk, "ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-1501"));
			Assert.assertEquals(3, esBulk.request().numberOfActions());
		}

		// case - comments STANDALONE without comments in issue
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.STANDALONE;
			tested.indexDocument(esBulk, "ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-15013"));
			Assert.assertEquals(1, esBulk.request().numberOfActions());
		}

		// case - preprocessor called
		{
			BulkRequestBuilder esBulk = new BulkRequestBuilder(client);
			tested.commentIndexingMode = CommentIndexingMode.STANDALONE;
			StructuredContentPreprocessor idp1 = mock(StructuredContentPreprocessor.class);
			when(idp1.preprocessData(Mockito.anyMap())).thenAnswer(new Answer<Map<String, Object>>() {
				@Override
				public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
					return (Map<String, Object>) invocation.getArguments()[0];
				}
			});
			tested.addDataPreprocessor(idp1);

			tested.indexDocument(esBulk, "ORG", TestUtils.readDocumentJsonDataFromClasspathFile("ORG-15013"));
			Assert.assertEquals(1, esBulk.request().numberOfActions());
			verify(idp1, times(1)).preprocessData(Mockito.anyMap());
		}

	}

	@Test
	public void addValueToTheIndex() throws Exception {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), null, null, createSettingsWithMandatoryFilled(), true);

		XContentGenerator xContentGeneratorMock = mock(XContentGenerator.class);
		XContentBuilder out = XContentBuilder.builder(preparexContentMock(xContentGeneratorMock));

		// case - no exception if values parameter is null
		tested.addValueToTheIndex(out, "testfield", "testpath", null, (Map<String, String>) null);
		verify(xContentGeneratorMock, times(0)).writeFieldName(Mockito.anyString());

		// case - no exception if value is not found
		reset(xContentGeneratorMock);
		Map<String, Object> values = new HashMap<String, Object>();
		tested.addValueToTheIndex(out, "testfield", "testpath", values, (Map<String, String>) null);
		verify(xContentGeneratorMock, times(0)).writeFieldName(Mockito.anyString());

		// case - get correctly value from first level of nesting, no filtering on null filter
		reset(xContentGeneratorMock);
		values.put("myKey", "myValue");
		values.put("myKey2", "myValue2");
		tested.addValueToTheIndex(out, "testfield", "myKey2", values, (Map<String, String>) null);
		verify(xContentGeneratorMock, times(1)).writeFieldName(Mockito.anyString());
		verify(xContentGeneratorMock).writeFieldName("testfield");
		verify(xContentGeneratorMock).writeString("myValue2");
		Mockito.verifyNoMoreInteractions(xContentGeneratorMock);

		// case - get correctly value from deeper level of nesting, no filtering with empty filter
		reset(xContentGeneratorMock);
		values.put("myKey", "myValue");
		values.put("myKey2", "myValue2");
		Map<String, Object> parent3 = new HashMap<String, Object>();
		values.put("parent3", parent3);
		parent3.put("myKey3", "myValue3");
		Map<String, String> filter = new HashMap<String, String>();

		tested.addValueToTheIndex(out, "testfield3", "parent3.myKey3", values, filter);
		verify(xContentGeneratorMock, times(1)).writeFieldName(Mockito.anyString());
		verify(xContentGeneratorMock).writeFieldName("testfield3");
		verify(xContentGeneratorMock).writeString("myValue3");
		Mockito.verifyNoMoreInteractions(xContentGeneratorMock);

		// case - no error when filter on filtering unsupported value
		reset(xContentGeneratorMock);
		values.clear();
		values.put("myKey", "myValue");
		values.put("myKey2", "myValue2");
		filter.put("myKeyFilter", "myKeyFilter");
		tested.addValueToTheIndex(out, "testfield", "myKey2", values, filter);
		verify(xContentGeneratorMock).writeFieldName("testfield");
		verify(xContentGeneratorMock).writeString("myValue2");
		Mockito.verifyNoMoreInteractions(xContentGeneratorMock);

		// case - get correctly value from first level of nesting, filtering on Map
		reset(xContentGeneratorMock);
		values.clear();
		values.put("myKey", "myValue");
		values.put("myKey2", "myValue2");
		parent3 = new HashMap<String, Object>();
		values.put("parent3", parent3);
		parent3.put("myKey3", "myValue3");
		parent3.put("myKey4", "myValue4");

		filter.clear();
		filter.put("myKey3", "myKey1");

		tested.addValueToTheIndex(out, "testfield", "parent3", values, filter);

		verify(xContentGeneratorMock).writeFieldName("testfield");
		verify(xContentGeneratorMock).writeStartObject();
		verify(xContentGeneratorMock).writeFieldName("myKey1");
		verify(xContentGeneratorMock).writeString("myValue3");
		verify(xContentGeneratorMock).writeEndObject();

		Mockito.verifyNoMoreInteractions(xContentGeneratorMock);

		// case - filtering on List of Maps
		reset(xContentGeneratorMock);
		values.clear();
		values.put("myKey", "myValue");
		values.put("myKey2", "myValue2");

		List<Object> parent3list = new ArrayList<Object>();
		values.put("parent3", parent3list);

		Map<String, Object> obj31 = new HashMap<String, Object>();
		parent3list.add(obj31);
		obj31.put("myKey3", "myValue31");
		obj31.put("myKey4", "myValue41");

		Map<String, Object> obj32 = new HashMap<String, Object>();
		parent3list.add(obj32);
		obj32.put("myKey3", "myValue32");
		obj32.put("myKey4", "myValue42");

		filter.clear();
		filter.put("myKey3", "myKey3");

		tested.addValueToTheIndex(out, "testfield", "parent3", values, filter);

		verify(xContentGeneratorMock).writeFieldName("testfield");
		verify(xContentGeneratorMock, times(1)).writeStartArray();
		verify(xContentGeneratorMock, times(2)).writeStartObject();
		verify(xContentGeneratorMock, times(2)).writeFieldName("myKey3");
		verify(xContentGeneratorMock).writeString("myValue31");
		verify(xContentGeneratorMock).writeString("myValue32");
		verify(xContentGeneratorMock, times(2)).writeEndObject();
		verify(xContentGeneratorMock, times(1)).writeEndArray();

		Mockito.verifyNoMoreInteractions(xContentGeneratorMock);

	}

	@Test
	public void addValueToTheIndexField() throws Exception {
		DocumentWithCommentsIndexStructureBuilder tested = new DocumentWithCommentsIndexStructureBuilder(
				mockEsIntegrationComponent(), null, null, createSettingsWithMandatoryFilled(), true);

		XContentGenerator xContentGeneratorMock = mock(XContentGenerator.class);
		XContentBuilder out = XContentBuilder.builder(preparexContentMock(xContentGeneratorMock));

		// case - string field
		tested.addValueToTheIndexField(out, "test", "testvalue");
		verify(xContentGeneratorMock).writeFieldName("test");
		verify(xContentGeneratorMock).writeString("testvalue");

		// case - integer field
		reset(xContentGeneratorMock);
		tested.addValueToTheIndexField(out, "testint", new Integer(10));
		verify(xContentGeneratorMock).writeFieldName("testint");
		verify(xContentGeneratorMock).writeNumber(10);

		// case - nothing added if value is null
		reset(xContentGeneratorMock);
		tested.addValueToTheIndexField(out, "testnull", null);
		Mockito.verifyZeroInteractions(xContentGeneratorMock);
	}

	/**
	 * Prepare {@link XContent} mock to be used for {@link XContentBuilder} test instance creation.
	 * 
	 * @param xContentGeneratorMock to be returned from XContent mock
	 * @return XContent mock instance
	 * @throws IOException
	 */
	protected XContent preparexContentMock(XContentGenerator xContentGeneratorMock) throws IOException {
		XContent xContentMock = mock(XContent.class);
		when(xContentMock.createGenerator(Mockito.any(OutputStream.class))).thenReturn(xContentGeneratorMock);
		return xContentMock;
	}

	protected IESIntegration mockEsIntegrationComponent() {
		IESIntegration esIntegrationMock = mock(IESIntegration.class);
		Mockito.when(esIntegrationMock.createLogger(Mockito.any(Class.class))).thenReturn(
				ESLoggerFactory.getLogger(SpaceIndexerCoordinator.class.getName()));
		RiverName riverName = new RiverName("remote", "river_name");
		Mockito.when(esIntegrationMock.riverName()).thenReturn(riverName);
		return esIntegrationMock;
	}
}
