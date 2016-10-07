/*
 * Copyright © 2015 For the craic! and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.parser.impl.message.update;

import com.google.common.base.Preconditions;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.protocol.bgp.parser.BGPDocumentedException;
import org.opendaylight.protocol.bgp.parser.BGPParsingException;
import org.opendaylight.protocol.bgp.parser.spi.AttributeParser;
import org.opendaylight.protocol.bgp.parser.spi.AttributeSerializer;
import org.opendaylight.protocol.bgp.parser.spi.AttributeUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Qos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.QosBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.qos.QosStlv;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.qos.QosStlvBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QosAttributeParser implements AttributeParser, AttributeSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(QosAttributeParser.class);

    public static final int TYPE = 32;
    private static final int QOS_STLV_SIZE_IN_BYTES = 2;
    private static final byte QOS_STLV_TYPE = 3;
    //private static final short AIGP_TLV_SIZE = 11;
    //private static final int TLV_SIZE_IN_BYTES = 2;

    @Override
    public void serializeAttribute(DataObject attribute, ByteBuf byteAggregator) {
        LOG.debug("Serializing qos attribute");
        Preconditions.checkArgument(attribute instanceof Attributes,
                "Attribute parameter is not a PathAttribute object.");
        final Qos qosAttribute = ((Attributes) attribute).getQos();
        if (qosAttribute == null) {
            return;
        }
        AttributeUtil.formatAttribute(AttributeUtil.OPTIONAL_TRANSITIVE, TYPE, serializeQosSTLV(qosAttribute), byteAggregator);
    }

    private ByteBuf serializeQosSTLV(Qos qosAttribute) {
        QosStlv qosStlv= qosAttribute.getQosStlv();
        final ByteBuf value = Unpooled.buffer();
        value.writeByte(QOS_STLV_TYPE);
        StringBuilder aggMetric= new StringBuilder();
        for(String metric : qosStlv.getMetric()){
            aggMetric.append(metric);
            if(qosStlv.getMetric().indexOf(metric)!=qosStlv.getMetric().size())
                aggMetric.append(",");
            //LOG.info("{}",metric);
        }
        byte[] aggMetricBytes = aggMetric.toString().getBytes();
        value.writeShort(aggMetricBytes.length);
        value.writeBytes(aggMetricBytes);
        LOG.debug("{} {} bytes written into qos attribute stv value",aggMetric.toString(),aggMetricBytes.length);
        return value;
    }

    @Override
    public void parseAttribute(ByteBuf buffer, AttributesBuilder builder)
            throws BGPDocumentedException, BGPParsingException {
        LOG.info("Not Supported");
        if(!buffer.isReadable()) {
            return;
        }
        builder.setQos(new QosBuilder().setQosStlv(parseQosStlv(buffer)).build());
    }

    private QosStlv parseQosStlv(ByteBuf buffer) {
        final int tlvType = buffer.readByte();
        int valueLength= buffer.readShort();
        LOG.info("xd {} {}",buffer.capacity(),valueLength);
        List<String> metricList= new ArrayList<>();
        if (tlvType == QOS_STLV_TYPE) {
            String metricValue= buffer.readBytes(valueLength).toString();
            LOG.info("QOSSTLV attribute {}",metricValue);
            String[] values= metricValue.split(",");
            for(String s : values)
                metricList.add(s);
        }
        return new QosStlvBuilder().setMetric(metricList).setType(Short.valueOf(QOS_STLV_TYPE)).build();
    }
}
