/* Copyright 2010-2014 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.collector.http.pipeline.importer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.http.crawler.HttpCrawlerEvent;
import com.norconex.collector.http.data.HttpCrawlData;
import com.norconex.collector.http.doc.HttpMetadata;
import com.norconex.collector.http.pipeline.queue.HttpQueuePipeline;
import com.norconex.collector.http.pipeline.queue.HttpQueuePipelineContext;
import com.norconex.collector.http.url.ILinkExtractor;
import com.norconex.collector.http.url.Link;
import com.norconex.commons.lang.file.ContentType;
import com.norconex.commons.lang.io.CachedInputStream;

/**
 * Extract URLs before sending to importer (because the importer may
 * strip some "valid" urls in producing content-centric material.
 * Plus, any additional urls could be added to Metadata and they will
 * be considered.
 */
/*default*/ class LinkExtractorStage extends AbstractImporterStage {

    private static final Logger LOG = LogManager
            .getLogger(LinkExtractorStage.class);
    
    @Override
    public boolean executeStage(HttpImporterPipelineContext ctx) {
        ILinkExtractor[] extractors = ctx.getConfig().getLinkExtractors();
        if (ArrayUtils.isEmpty(extractors)) {
            LOG.debug("No configured link extractor.  No links will be "
                    + "detected.");
            return true;
        }
        
        if (ctx.getRobotsMeta() != null 
                && ctx.getRobotsMeta().isNofollow()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No URLs extracted due to Robots nofollow rule "
                        + "for URL: " + ctx.getCrawlData().getReference());
            }
            return true;
        }
        
        Set<Link> links = new HashSet<>();
        CachedInputStream is = ctx.getContent();
        String reference = ctx.getCrawlData().getReference();
        ContentType ct = ctx.getDocument().getContentType();
        for (ILinkExtractor extractor : extractors) {
            if (extractor.accepts(reference, ct)) {
                try {
                    links.addAll(extractor.extractLinks(is, reference, ct));
                } catch (IOException e) {
                    LOG.error("Could not extract links from: " + reference, e);
                } finally {
                    is.rewind();
                }
            }
        }
        
        Set<String> uniqueURLs = new HashSet<String>();
        if (links != null) {
            for (Link link : links) {
                HttpCrawlData newURL = new HttpCrawlData(
                        link.getUrl(), ctx.getCrawlData().getDepth() + 1);
                newURL.setReferrerReference(link.getReferrer());
                newURL.setReferrerLinkTag(link.getTag());
                newURL.setReferrerLinkText(link.getText());
                newURL.setReferrerLinkTitle(link.getTitle());
                HttpQueuePipelineContext newContext = 
                        new HttpQueuePipelineContext(ctx.getCrawler(), 
                                ctx.getCrawlDataStore(), newURL);
                //TODO do we want to capture them all or just the valid ones?
                if (uniqueURLs.add(newURL.getReference())) {
                    new HttpQueuePipeline().execute(newContext);
                }
            }
        }
        if (!uniqueURLs.isEmpty()) {
            ctx.getMetadata().addString(HttpMetadata.COLLECTOR_REFERNCED_URLS, 
                    uniqueURLs.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        }
        
        ctx.fireCrawlerEvent(HttpCrawlerEvent.URLS_EXTRACTED, 
                ctx.getCrawlData(), uniqueURLs);
        return true;
    }
}