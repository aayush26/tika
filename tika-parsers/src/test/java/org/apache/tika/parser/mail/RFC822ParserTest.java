/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.mail;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

public class RFC822ParserTest extends TestCase {

    public void testSimple() {
        Parser parser = new RFC822Parser();
        Metadata metadata = new Metadata();
        InputStream stream = getStream("test-documents/testRFC822");
        ContentHandler handler = mock(DefaultHandler.class);

        try {
            parser.parse(stream, handler, metadata, new ParseContext());
            verify(handler).startDocument();
            //just one body
            verify(handler).startElement(eq(XHTMLContentHandler.XHTML), eq("p"), eq("p"), any(Attributes.class));
            verify(handler).endElement(XHTMLContentHandler.XHTML, "p", "p");
            //no multi-part body parts
            verify(handler, never()).startElement(eq(XHTMLContentHandler.XHTML), eq("div"), eq("div"), any(Attributes.class));
            verify(handler, never()).endElement(XHTMLContentHandler.XHTML, "div", "div");
            verify(handler).endDocument();
            //note no leading spaces
            assertEquals("\"Julien Nioche (JIRA)\" <jira@apache.org>", metadata.get(Metadata.AUTHOR));
            assertEquals("[jira] Commented: (TIKA-461) RFC822 messages not parsed", metadata.get(Metadata.SUBJECT));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    public void testQuotedPrintable() {
        Parser parser = new RFC822Parser();
        Metadata metadata = new Metadata();
        InputStream stream = getStream("test-documents/testRFC822_quoted");
        ContentHandler handler = mock(DefaultHandler.class);

        try {
            parser.parse(stream, handler, metadata, new ParseContext());
            //tests correct decoding of quoted printable text, including UTF-8 bytes into Unicode
            verify(handler).characters(new String("D\u00FCsseldorf has non-ascii. "
            	+ "Lines can be split like this. Spaces at the end of a line \r\n"
            	+ "must be encoded.\r\n").toCharArray(), 0, 104);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    public void testBase64() {
        Parser parser = new RFC822Parser();
        Metadata metadata = new Metadata();
        InputStream stream = getStream("test-documents/testRFC822_base64");
        ContentHandler handler = mock(DefaultHandler.class);

        try {
            parser.parse(stream, handler, metadata, new ParseContext());
            //tests correct decoding of base64 text, including ISO-8859-1 bytes into Unicode
            verify(handler).characters(new String(
            	"Here is some text, with international characters, voil\u00E0!\r\n"
            	).toCharArray(), 0, 58);
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }
    
    public void testI18NHeaders() {
        Parser parser = new RFC822Parser();
        Metadata metadata = new Metadata();
        InputStream stream = getStream("test-documents/testRFC822_i18nheaders");
        ContentHandler handler = mock(DefaultHandler.class);

        try {
            parser.parse(stream, handler, metadata, new ParseContext());
            //tests correct decoding of internationalized headers, both
            //quoted-printable (Q) and Base64 (B).
            assertEquals("Keld J\u00F8rn Simonsen <keld@dkuug.dk>", metadata.get(Metadata.AUTHOR));
            assertEquals("If you can read this you understand the example.", metadata.get(Metadata.SUBJECT));
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    private static InputStream getStream(String name) {
        return Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(name);
    }

}