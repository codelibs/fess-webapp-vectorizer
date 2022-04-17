/*
 * Copyright 2012-2022 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.webapp.vectorizer;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser.Operator;
import org.apache.lucene.queryparser.ext.ExtendableQueryParser;
import org.apache.lucene.search.TermQuery;
import org.codelibs.fess.Constants;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.exception.InvalidQueryException;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.query.QueryFieldConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.lastadi.LastaDiTestCase;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.MatchAllQueryBuilder;
import org.opensearch.index.query.MatchPhraseQueryBuilder;
import org.opensearch.index.query.PrefixQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.WildcardQueryBuilder;
import org.opensearch.search.sort.SortBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class VectorisingTermQueryCommandTest extends LastaDiTestCase {
    private static final Logger logger = LogManager.getLogger(VectorisingTermQueryCommandTest.class);

    private static final String VERSION = "snapshot";

    private static final String IMAGE_TAG = "ghcr.io/codelibs/fess-text-vectorizer:" + VERSION;

    private VectorisingTermQueryCommand queryCommand;

    GenericContainer vectorizingServer;

    ThreadLocal<String[]> currentLangs = new ThreadLocal<>();

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        vectorizingServer = new GenericContainer<>(DockerImageName.parse(IMAGE_TAG))//
                .withExposedPorts(8900);
        vectorizingServer.start();

        final QueryFieldConfig queryFieldConfig = new QueryFieldConfig();
        queryFieldConfig.init();
        ComponentUtil.register(queryFieldConfig, "queryFieldConfig");

        final ExtendableQueryParser queryParser = new ExtendableQueryParser(Constants.DEFAULT_FIELD, new WhitespaceAnalyzer());
        queryParser.setAllowLeadingWildcard(true);
        queryParser.setDefaultOperator(Operator.AND);
        ComponentUtil.register(queryParser, "queryParser");

        queryCommand = new VectorisingTermQueryCommand() {
            @Override
            protected OptionalThing<String[]> getQueryLanguages() {
                final String[] values = currentLangs.get();
                if (values == null) {
                    return OptionalThing.empty();
                }
                return OptionalThing.of(values);
            }
        };
        queryCommand.vectorizer = Vectorizer.create()//
                .url(getServerUrl())//
                .build();
    }

    private String getServerUrl() {
        return "http://" + vectorizingServer.getHost() + ":" + vectorizingServer.getFirstMappedPort();
    }

    @Override
    public void tearDown() throws Exception {
        vectorizingServer.stop();
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_convertTermQueryWithSemanticEn() throws Exception {
        currentLangs.set(new String[] { "en" });
        try {
            final VectorisingQueryContext context =
                    assertQueryBuilder(MatchAllQueryBuilder.class, "{\"match_all\":{\"boost\":1.0}}", "semantic:aaa");
            assertEquals(1, context.getScripts().length);
        } finally {
            currentLangs.set(null);
        }
    }

    public void test_convertTermQueryWithSemanticJa() throws Exception {
        currentLangs.set(new String[] { "en" });
        try {
            final VectorisingQueryContext context =
                    assertQueryBuilder(MatchAllQueryBuilder.class, "{\"match_all\":{\"boost\":1.0}}", "semantic:aaa");
            assertEquals(1, context.getScripts().length);
        } finally {
            currentLangs.set(null);
        }
    }

    public void test_convertTermQuery() throws Exception {
        assertQueryBuilder(BoolQueryBuilder.class,
                "{\"bool\":{\"should\":[{\"match_phrase\":{\"title\":{\"query\":\"aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":0.5}}},{\"match_phrase\":{\"content\":{\"query\":\"aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":0.05}}}],\"adjust_pure_negative\":true,\"boost\":1.0}}",
                "aaa");
        assertQueryBuilder(MatchPhraseQueryBuilder.class,
                "{\"match_phrase\":{\"title\":{\"query\":\"aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":1.0}}}", //
                "title:aaa");
        assertQueryBuilder(MatchPhraseQueryBuilder.class,
                "{\"match_phrase\":{\"content\":{\"query\":\"aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":1.0}}}", //
                "content:aaa");
        assertQueryBuilder(BoolQueryBuilder.class,
                "{\"bool\":{\"should\":[{\"match_phrase\":{\"title\":{\"query\":\"xxx:aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":0.5}}},{\"match_phrase\":{\"content\":{\"query\":\"xxx:aaa\",\"slop\":0,\"zero_terms_query\":\"NONE\",\"boost\":0.05}}},{\"fuzzy\":{\"title\":{\"value\":\"xxx:aaa\",\"fuzziness\":\"AUTO\",\"prefix_length\":0,\"max_expansions\":10,\"transpositions\":true,\"boost\":0.01}}},{\"fuzzy\":{\"content\":{\"value\":\"xxx:aaa\",\"fuzziness\":\"AUTO\",\"prefix_length\":0,\"max_expansions\":10,\"transpositions\":true,\"boost\":0.005}}}],\"adjust_pure_negative\":true,\"boost\":1.0}}",
                "xxx:aaa");
        assertQueryBuilder(WildcardQueryBuilder.class, //
                "{\"wildcard\":{\"url\":{\"wildcard\":\"*aaa*\",\"boost\":1.0}}}", //
                "inurl:aaa");
        assertQueryBuilder(TermQueryBuilder.class, //
                "{\"term\":{\"url\":{\"value\":\"aaa\",\"boost\":1.0}}}", //
                "url:aaa");
        assertQueryBuilder(PrefixQueryBuilder.class, //
                "{\"prefix\":{\"site\":{\"value\":\"aaa\",\"boost\":1.0}}}", //
                "site:aaa");

        assertQueryBuilder("{\"timestamp\":{\"order\":\"asc\"}}", "sort:timestamp");
        assertQueryBuilder("{\"timestamp\":{\"order\":\"asc\"}}", "sort:timestamp.asc");
        assertQueryBuilder("{\"timestamp\":{\"order\":\"desc\"}}", "sort:timestamp.desc");

        try {
            assertQueryBuilder("", "sort:xxx");
            fail();
        } catch (final InvalidQueryException e) {
            // nothing
        }
    }

    private void assertQueryBuilder(final String expect, final String text) throws Exception {
        final QueryContext queryContext = assertQueryBuilder(null, null, text);
        final List<SortBuilder<?>> sortBuilders = queryContext.sortBuilders();
        assertEquals(1, sortBuilders.size());
        logger.info("{} => {}", text, sortBuilders.get(0).toString());
        assertEquals(expect, sortBuilders.get(0).toString().replaceAll("[\s\n]", ""));
    }

    private VectorisingQueryContext assertQueryBuilder(final Class<?> expectedClass, final String expectedQuery, final String text)
            throws Exception {
        final VectorisingQueryContext queryContext = new VectorisingQueryContext(new QueryContext(text, false));
        final TermQuery query = (TermQuery) ComponentUtil.getQueryParser().parse(queryContext.getQueryString());
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final QueryBuilder builder = queryCommand.convertTermQuery(fessConfig, queryContext, query, 1.0f,
                getSearchField(queryContext.getDefaultField(), query.getTerm().field()), query.getTerm().text());
        if (text.startsWith("sort:")) {
            assertNull(builder);
        } else {
            logger.info("{} => {}", text, builder.toString());
            assertEquals(expectedClass, builder.getClass());
            assertEquals(expectedQuery, builder.toString().replaceAll("[\s\n]", ""));
        }
        return queryContext;
    }

    private String getSearchField(final String defaultField, final String field) {
        if (Constants.DEFAULT_FIELD.equals(field) && defaultField != null) {
            return defaultField;
        }
        return field;
    }
}