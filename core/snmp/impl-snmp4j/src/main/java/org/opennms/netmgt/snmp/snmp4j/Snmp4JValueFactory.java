/*******************************************************************************
 * This file is part of the OpenNMS(R) Application.
 *
 * OpenNMS(R) is Copyright (C) 2011 The OpenNMS Group, Inc.  All rights reserved.
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 *     along with OpenNMS(R).  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information contact: 
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.netmgt.snmp.snmp4j;

import java.math.BigInteger;
import java.net.InetAddress;

import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.SnmpValueFactory;
import org.snmp4j.smi.Counter32;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Gauge32;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Opaque;
import org.snmp4j.smi.TimeTicks;

public class Snmp4JValueFactory implements SnmpValueFactory {

    public SnmpValue getOctetString(byte[] bytes) {
        return new Snmp4JValue(new OctetString(bytes));
    }

    public SnmpValue getCounter32(long val) {
        return new Snmp4JValue(new Counter32(val));
    }

    public SnmpValue getCounter64(BigInteger bigInt) {
        return new Snmp4JValue(new Counter64(bigInt.longValue()));
    }

    public SnmpValue getGauge32(long val) {
        return new Snmp4JValue(new Gauge32(val));
    }

    public SnmpValue getInt32(int val) {
        return new Snmp4JValue(new Integer32(val));
    }

    public SnmpValue getIpAddress(InetAddress val) {
        return new Snmp4JValue(new IpAddress(val));
    }

    public SnmpValue getObjectId(SnmpObjId objId) {
        return new Snmp4JValue(new OID(objId.getIds()));
    }

    public SnmpValue getTimeTicks(long val) {
        return new Snmp4JValue(new TimeTicks(val));
    }

    public SnmpValue getNull() {
        return new Snmp4JValue(new Null());
    }

    public SnmpValue getValue(int type, byte[] bytes) {
        return new Snmp4JValue(type, bytes);
    }

    public SnmpValue getOpaque(byte[] bs) {
        return new Snmp4JValue(new Opaque(bs));
    }


}
