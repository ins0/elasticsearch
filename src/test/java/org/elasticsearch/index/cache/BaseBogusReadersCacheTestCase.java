/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.cache;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.RAMDirectory;
import org.elasticsearch.Version;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;

/**
 */
public abstract class BaseBogusReadersCacheTestCase extends ElasticsearchTestCase {

    protected AtomicReaderContext bogusContext;
    protected AtomicReaderContext validContext;

    @Before
    public void before() throws Exception {
        bogusContext = SlowCompositeReaderWrapper.wrap(new MultiReader()).getContext();

        RAMDirectory ramDirectory = new RAMDirectory();
        IndexWriterConfig iwc = new IndexWriterConfig(Version.CURRENT.luceneVersion, new KeywordAnalyzer());
        IndexWriter writer = new IndexWriter(ramDirectory, iwc);
        Document document = new Document();
        document.add(new StringField("a", "b", Field.Store.NO));
        writer.addDocument(document);
        writer.close();
        validContext = DirectoryReader.open(ramDirectory).leaves().get(0);
    }

}