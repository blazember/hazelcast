/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class XmlUpdatePoc {
    public static void main(String[] args) throws IOException, XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        File file = new File("/home/blaze/hazelcast.xml");
        // TODO overflow is our best friend...
        int length = (int) file.length();
        byte[] xmlBytes = new byte[length];
        FileInputStream fis = new FileInputStream("/home/blaze/hazelcast.xml");
        fis.read(xmlBytes);

        int insertElementPos = 0;
        int lastCopyPos = 0;
        int mapStartPos = 0;
        boolean mapUpdate = false;

        ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(bais);

        XmlUpdater xmlUpdater = new XmlUpdater("/home/blaze/hazelcast-updated.xml");

        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();

            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if (startElement.getName().getLocalPart().equalsIgnoreCase("map")) {
                    Attribute nameAttr = startElement.getAttributeByName(QName.valueOf("name"));
                    String name = nameAttr.getValue();
                    if (name.equals("map-to-update")) {
                        mapUpdate = true;
                    }
                }
                if (!mapUpdate) {
                    insertElementPos = startElement.getLocation().getCharacterOffset();
                }
            }

            if (nextEvent.getEventType() == XMLStreamConstants.COMMENT
                    || nextEvent.getEventType() == XMLStreamConstants.SPACE) {
                if (!mapUpdate) {
                    insertElementPos = nextEvent.getLocation().getCharacterOffset();
                }
            }

            if (nextEvent.isEndElement()) {
                EndElement endElement = nextEvent.asEndElement();

                if (mapUpdate && endElement.getName().getLocalPart().equalsIgnoreCase("map")) {
                    xmlUpdater.add(new XmlCopyWriteEvent(xmlBytes, lastCopyPos, insertElementPos - lastCopyPos));
                    xmlUpdater.add(new XmlWriteStringEvent(updatedMap()));

                    Location location = endElement.getLocation();
                    lastCopyPos = location.getCharacterOffset();
                    insertElementPos = endElement.getLocation().getCharacterOffset();
                    mapUpdate = false;
                }

                if (endElement.getName().getLocalPart().equalsIgnoreCase("hazelcast")) {
                    xmlUpdater.add(new XmlCopyWriteEvent(xmlBytes, lastCopyPos, insertElementPos - lastCopyPos));
                    xmlUpdater.add(new XmlWriteStringEvent(addedMap()));
                    lastCopyPos = insertElementPos;
                }
                if (!mapUpdate) {
                    insertElementPos = endElement.getLocation().getCharacterOffset();
                }
            }
        }

        xmlUpdater.add(new XmlCopyWriteEvent(xmlBytes, lastCopyPos, xmlBytes.length - lastCopyPos));
        xmlUpdater.update();
    }

    private static String addedMap() {
        return "\n\n" +
                "<!-- AUTO-GENERATED map configuration -->\n" +
                "<map name=\"inserted-map\">\n" +
                "  <in-memory-format>NATIVE</in-memory-format>\n" +
                "</map>\n" +
                "<!-- End of AUTO-GENERATED map configuration -->";
    }

    private static String updatedMap() {
        return "\n" +
                "<!-- AUTO-GENERATED map configuration -->\n" +
                "<map name=\"map-to-update\">\n" +
                "  <in-memory-format>NATIVE</in-memory-format>\n" +
                "</map>\n" +
                "<!-- End of AUTO-GENERATED map configuration -->";
    }

    private static class XmlUpdater {
        private final FileOutputStream fos;
        private final List<XmlWriteEvent> xmlWriteEvents = new LinkedList<>();

        private XmlUpdater(String filename) throws FileNotFoundException {
            fos = new FileOutputStream(filename);
        }

        private void add(XmlWriteEvent writeEvent) {
            xmlWriteEvents.add(writeEvent);
        }

        public void update() throws IOException {
            // TODO if exists, backup-and-rewrite...
            for (XmlWriteEvent event : xmlWriteEvents) {
                System.out.println(event);
                event.write(fos);
                fos.flush();
            }
        }
    }

    private interface XmlWriteEvent {
        void write(OutputStream os) throws IOException;
    }

    private static class XmlCopyWriteEvent implements XmlWriteEvent {
        private final byte[] originalXmlBytes;
        private final int offset;
        private final int length;

        private XmlCopyWriteEvent(byte[] originalXmlBytes, int offset, int length) {
            this.originalXmlBytes = originalXmlBytes;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public void write(OutputStream os) throws IOException {
            os.write(originalXmlBytes, offset, length);
        }

        @Override
        public String toString() {
            return "XmlCopyWriteEvent{" +
                    "offset=" + offset +
                    ", length=" + length +
                    '}';
        }
    }

    private static class XmlWriteStringEvent implements XmlWriteEvent {
        private final String str;

        private XmlWriteStringEvent(String str) {
            this.str = str;
        }

        @Override
        public void write(OutputStream os) throws IOException {
            os.write(str.getBytes());
        }

        @Override
        public String toString() {
            return "XmlWriteStringEvent{" +
                    "str='" + str + '\'' +
                    '}';
        }
    }
}
