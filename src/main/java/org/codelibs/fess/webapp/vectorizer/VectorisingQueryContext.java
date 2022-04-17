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
import java.util.function.Consumer;

import org.codelibs.fess.entity.QueryContext;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder.FilterFunctionBuilder;
import org.opensearch.script.Script;
import org.opensearch.search.sort.SortBuilder;

public class VectorisingQueryContext extends QueryContext {

    private final QueryContext context;

    private Script[] scripts;

    public VectorisingQueryContext(final QueryContext context) {
        super(context.getQueryString(), false);
        this.context = context;
    }

    public void setScripts(final Script[] scripts) {
        this.scripts = scripts;
    }

    public Script[] getScripts() {
        return scripts;
    }

    // delegated

    @Override
    public int hashCode() {
        return context.hashCode();
    }

    @Override
    public void addFunctionScore(final Consumer<List<FilterFunctionBuilder>> functionScoreQuery) {
        context.addFunctionScore(functionScoreQuery);
    }

    @Override
    public void addQuery(final Consumer<BoolQueryBuilder> boolQuery) {
        context.addQuery(boolQuery);
    }

    @Override
    public void setQueryBuilder(final QueryBuilder queryBuilder) {
        context.setQueryBuilder(queryBuilder);
    }

    @Override
    public boolean equals(final Object obj) {
        return context.equals(obj);
    }

    @Override
    public void addSorts(final SortBuilder<?>... sortBuilders) {
        context.addSorts(sortBuilders);
    }

    @Override
    public boolean hasSorts() {
        return context.hasSorts();
    }

    @Override
    public List<SortBuilder<?>> sortBuilders() {
        return context.sortBuilders();
    }

    @Override
    public QueryBuilder getQueryBuilder() {
        return context.getQueryBuilder();
    }

    @Override
    public void addFieldLog(final String field, final String text) {
        context.addFieldLog(field, text);
    }

    @Override
    public List<String> getDefaultKeyword() {
        return context.getDefaultKeyword();
    }

    @Override
    public void addHighlightedQuery(final String text) {
        context.addHighlightedQuery(text);
    }

    @Override
    public String getQueryString() {
        return context.getQueryString();
    }

    @Override
    public boolean roleQueryEnabled() {
        return context.roleQueryEnabled();
    }

    @Override
    public void skipRoleQuery() {
        context.skipRoleQuery();
    }

    @Override
    public String getDefaultField() {
        return context.getDefaultField();
    }

    @Override
    public void setDefaultField(final String defaultField) {
        context.setDefaultField(defaultField);
    }

    @Override
    public String toString() {
        return context.toString();
    }
}
