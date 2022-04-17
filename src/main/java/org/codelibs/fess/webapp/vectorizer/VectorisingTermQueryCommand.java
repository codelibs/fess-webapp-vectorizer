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

import static org.codelibs.core.stream.StreamUtil.stream;
import static org.codelibs.fess.Constants.DEFAULT_FIELD;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.TermQuery;
import org.codelibs.fesen.client.EngineInfo.EngineType;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.exception.InvalidQueryException;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.query.TermQueryCommand;
import org.codelibs.fess.util.ComponentUtil;
import org.lastaflute.core.message.UserMessages;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.script.Script;
import org.opensearch.script.ScriptType;

public class VectorisingTermQueryCommand extends TermQueryCommand {

    private static final Logger logger = LogManager.getLogger(VectorisingTermQueryCommand.class);

    private static final String CONTENT_FIELD = "content";

    private static final String LANG_FIELD = "lang";

    private static final String SEMANTIC_FIELD = "semantic";

    protected Vectorizer vectorizer;

    protected String fieldSuffix = "_vector";

    protected String scriptSpaceType = "cosinesimil";

    @PostConstruct
    public void init() {
        final EngineType engineType = getEngineType();
        if (engineType == EngineType.OPENSEARCH1) {
            logger.info("Search Engine: {}", engineType);
            final int dimension =
                    Integer.parseInt(ComponentUtil.getFessConfig().getSystemProperty("semantic_search.vectorizer.dimension", "768"));
            final String url = ComponentUtil.getFessConfig().getSystemProperty("semantic_search.vectorizer.url");
            final String fields = ComponentUtil.getFessConfig().getSystemProperty("semantic_search.vectorizer.fields");
            vectorizer = Vectorizer.create()//
                    .url(url)//
                    .fields(fields)//
                    .dimension(dimension)//
                    .build();
        } else {
            logger.warn("Your search engine is not supported: {}", engineType);
        }
    }

    protected EngineType getEngineType() {
        return ComponentUtil.getSearchEngineClient().getEngineInfo().getType();
    }

    @Override
    protected QueryBuilder convertTermQuery(final FessConfig fessConfig, final QueryContext context, final TermQuery termQuery,
            final float boost, final String field, final String text) {
        if (SEMANTIC_FIELD.equals(field)) {
            if (!(context instanceof final VectorisingQueryContext vectorisingQueryContext)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("context is not VectorisingQueryContext. Query is {}", text);
                }
                return super.convertTermQuery(fessConfig, context, termQuery, boost, DEFAULT_FIELD, text);
            }
            return getQueryLanguages()
                    .map(langs -> stream(langs).get(stream -> stream.filter(vectorizer::isSupportedLanguage).findFirst().get()))
                    .map(lang -> {
                        final Script[] scripts = getScripts(lang, new String[] { CONTENT_FIELD }, text);
                        if (scripts.length > 0) {
                            vectorisingQueryContext.setScripts(scripts);
                            return (QueryBuilder) QueryBuilders.matchAllQuery();
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("A sentence vector is not generated. Query is {}", text);
                        }
                        return super.convertTermQuery(fessConfig, context, termQuery, boost, DEFAULT_FIELD, text);
                    }).orElseGet(() -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Supported languages are not found. Query is {}", text);
                        }
                        return super.convertTermQuery(fessConfig, context, termQuery, boost, DEFAULT_FIELD, text);
                    });
        }
        return super.convertTermQuery(fessConfig, context, termQuery, boost, field, text);
    }

    protected Script[] getScripts(final String lang, final String[] fields, final String text) {
        final Map<String, Object> params = new HashMap<>();
        params.put(LANG_FIELD, lang);
        for (final String field : fields) {
            params.put(field, text);
        }
        final Map<String, float[]> output = vectorizer.vectorize(params);
        final float[] vector = output.get(CONTENT_FIELD);
        if (vector == null) {
            throw new InvalidQueryException(messages -> messages.addErrorsInvalidQueryUnknown(UserMessages.GLOBAL_PROPERTY_KEY),
                    "Failed to get a sentence vector.");
        }
        return stream(fields).get(stream -> stream.map(field -> new Script(ScriptType.INLINE, "knn", "knn_score", //
                Map.of("field", field + "_" + lang + fieldSuffix, //
                        "query_value", vector, //
                        "space_type", scriptSpaceType)))
                .toArray(n -> new Script[n]));
    }

    public void setFieldSuffix(final String fieldSuffix) {
        this.fieldSuffix = fieldSuffix;
    }

    public void setScriptSpaceType(final String scriptSpaceType) {
        this.scriptSpaceType = scriptSpaceType;
    }
}
