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
package com.norconex.collector.http.url.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.norconex.collector.http.url.ILinkExtractor;
import com.norconex.collector.http.url.Link;
import com.norconex.commons.lang.config.ConfigurationUtil;
import com.norconex.commons.lang.file.ContentType;


/**
 * Tests multiple {@link ILinkExtractor} implementations.
 * @author Pascal Essiembre
 */
public class LinkExtractorTest {

    @Test
    public void testHtmlLinkExtractor() throws IOException {
        testLinkExtraction(new HtmlLinkExtractor());
    }
    @Test
    public void testTikaLinkExtractor() throws IOException {
        testLinkExtraction(new TikaLinkExtractor());
    }

    @Test
    public void testHtmlWriteRead() throws IOException {
        HtmlLinkExtractor extractor = new HtmlLinkExtractor();
        extractor.setContentTypes(ContentType.HTML, ContentType.XML);
        extractor.setIgnoreNofollow(true);
        extractor.setKeepReferrerData(true);
        extractor.addLinkTag("food", "chocolate");
        extractor.addLinkTag("friend", "Thor");
        System.out.println("Writing/Reading this: " + extractor);
        ConfigurationUtil.assertWriteRead(extractor);
    }

    
    @Test
    public void testTikaWriteRead() throws IOException {
        TikaLinkExtractor extractor = new TikaLinkExtractor();
        extractor.setContentTypes(ContentType.HTML, ContentType.XML);
        extractor.setIgnoreNofollow(true);
        extractor.setKeepReferrerData(true);
        System.out.println("Writing/Reading this: " + extractor);
        ConfigurationUtil.assertWriteRead(extractor);
    }
    
    private void testLinkExtraction(ILinkExtractor extractor) 
            throws IOException {
        String baseURL = "http://www.example.com/";
        String baseDir = baseURL + "test/";
        String docURL = baseDir + "LinkExtractorTest.html";

        // All these must be found
        String[] expectedURLs = {
                baseURL + "meta-redirect.html",
                baseURL + "startWithDoubleslash.html",
                docURL + "?startWith=questionmark",
                docURL, // <-- "#startWithHashMark" (hash is stripped)
                baseURL + "startWithSlash.html",
                baseDir + "relativeToLastSegment.html",
                "http://www.sample.com/blah.html",
                baseURL + "onTwoLines.html",
                baseURL + "imageSlash.gif",
                baseURL + "imageNoSlash.gif",
        };
        
        // All these must NOT be found
        String[] unexpectedURLs = {
                baseURL + "badhref.html",
                baseURL + "nofollow.html",
        };

        InputStream is = getClass().getResourceAsStream(
                "LinkExtractorTest.html");

        Set<Link> links = extractor.extractLinks(
                is, docURL, ContentType.HTML);

        for (String expectedURL : expectedURLs) {
            assertTrue("Could not find expected URL: " + expectedURL, 
                    contains(links, expectedURL));
        }
        for (String unexpectedURL : unexpectedURLs) {
            assertFalse("Found unexpected URL: " + unexpectedURL, 
                    contains(links, unexpectedURL));
        }
        IOUtils.closeQuietly(is);
    }
    
    private boolean contains(Set<Link> links, String url) {
        for (Link link : links) {
            if (url.equals(link.getUrl())) {
                return true;
            }
        }
        return false;
    }
}
