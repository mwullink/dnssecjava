/*
 * dnssecjava - a DNSSEC validating stub resolver for Java
 * Copyright (C) 2013 Ingo Bauersachs. All rights reserved.
 *
 * This file is part of dnssecjava.
 *
 * Dnssecjava is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Dnssecjava is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with dnssecjava.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jitsi.dnssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

public class TestDNames extends TestBase {
    @Test
    public void testDNameToExistingIsValid() throws IOException {
        Message response = resolver.send(createMessage("www.alias.ingotronic.ch./A"));
        assertTrue("AD flag must be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }

    @Test
    public void testDNameToNoDataIsValid() throws IOException {
        Message response = resolver.send(createMessage("www.alias.ingotronic.ch./MX"));
        assertTrue("AD flag must be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }

    @Test
    public void testDNameToNxDomainIsValid() throws IOException {
        Message response = resolver.send(createMessage("x.alias.ingotronic.ch./A"));
        assertTrue("AD flag must be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.NXDOMAIN, response.getRcode());
    }

    @Test
    public void testDNameWithFakedCnameIsInvalid() throws IOException {
        Message m = resolver.send(createMessage("www.alias.ingotronic.ch./A"));
        Message message = messageFromString(m.toString().replaceAll("(.*CNAME\\s+)(.*)", "$1 www.isc.org."));
        add("www.alias.ingotronic.ch./A", message);

        Message response = resolver.send(createMessage("www.alias.ingotronic.ch./A"));
        assertFalse("AD flag must not be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.SERVFAIL, response.getRcode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDNameInNsecIsUnderstood_Rfc6672_5_3_4_1() throws IOException {
        Message nsecs = resolver.send(createMessage("alias.ingotronic.ch./NS"));
        RRset nsecSet = null;
        for (RRset set : nsecs.getSectionRRsets(Section.AUTHORITY)) {
            if (set.getName().equals(Name.fromString("alias.ingotronic.ch."))) {
                nsecSet = set;
                break;
            }
        }

        Message message = new Message();
        message.getHeader().setRcode(Rcode.NXDOMAIN);
        message.addRecord(Record.newRecord(Name.fromString("www.alias.ingotronic.ch."), Type.A, DClass.IN), Section.QUESTION);
        Iterator<Record> rrs = nsecSet.rrs();
        while (rrs.hasNext()) {
            message.addRecord(rrs.next(), Section.AUTHORITY);
        }
        Iterator<Record> sigs = nsecSet.sigs();
        while (sigs.hasNext()) {
            message.addRecord(sigs.next(), Section.AUTHORITY);
        }

        add("www.alias.ingotronic.ch./A", message);

        Message response = resolver.send(createMessage("www.alias.ingotronic.ch./A"));
        assertFalse("AD flag must not be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.SERVFAIL, response.getRcode());
    }

    @Test
    public void testDNameToExternal() throws IOException {
        Message response = resolver.send(createMessage("www.isc.ingotronic.ch./A"));
        assertTrue("AD flag must be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }

    @Test
    public void testDNameChain() throws IOException {
        Message response = resolver.send(createMessage("www.alias.nsec3.ingotronic.ch./A"));
        assertTrue("AD flag must be set", response.getHeader().getFlag(Flags.AD));
        assertEquals(Rcode.NOERROR, response.getRcode());
    }
}