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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.Query;
import org.codelibs.fess.entity.QueryContext;
import org.codelibs.fess.query.QueryProcessor;
import org.codelibs.fess.query.QueryProcessor.FilterChain;
import org.codelibs.fess.util.ComponentUtil;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.script.Script;

public class VectorisingQueryFilter implements QueryProcessor.Filter {

    private static final Logger logger = LogManager.getLogger(VectorisingQueryFilter.class);

    @Override
    public QueryBuilder execute(final QueryContext context, final Query query, final float boost, final FilterChain chain) {
        final VectorisingQueryContext vectorisingQueryContext = new VectorisingQueryContext(context);
        final QueryBuilder queryBuilder = chain.execute(vectorisingQueryContext, query, boost);
        final Script[] scripts = vectorisingQueryContext.getScripts();
        if (scripts != null) {
            final QueryBuilder newQueryBuilder = scripts.length > 1
                    ? QueryBuilders.functionScoreQuery(queryBuilder,
                            (FunctionScoreQueryBuilder.FilterFunctionBuilder[]) stream(scripts).get(stream -> stream
                                    .map(script -> new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                            ScoreFunctionBuilders.scriptFunction(script)))
                                    .toArray(n -> new FunctionScoreQueryBuilder.FilterFunctionBuilder[n])))
                    : QueryBuilders.scriptScoreQuery(queryBuilder, scripts[0]);
            if (logger.isDebugEnabled()) {
                logger.debug("QUERY: {}", newQueryBuilder);
            }
            return newQueryBuilder;
        }
        return queryBuilder;
    }

    public void register() {
        ComponentUtil.getQueryProcessor().add(null, null);
    }

}
