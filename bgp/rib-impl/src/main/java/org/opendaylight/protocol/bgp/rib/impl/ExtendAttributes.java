/*
 * Copyright © 2015 For the craic! and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;


import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.AsNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.qos.QosStlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendAttributes {

    private static final Logger LOG = LoggerFactory.getLogger(ExtendAttributes.class);
    private final TablesKey localTablesKey;
    private static final short qosAttributeType= new Integer(32).shortValue();

    public ExtendAttributes(final TablesKey localTablesKey) {
        LOG.info("Initialized ExtendAttributes service for {} {}",localTablesKey.getAfi().getName(),localTablesKey.getSafi().getName());
        this.localTablesKey=localTablesKey;
    }


    public ContainerNode addQosSubAttribute(ContainerNode effectiveAttributes, PeerId routePeerId, PeerId peerDestiny,
            PathArgument routeId, PeerRole advPeerRole, boolean ifInternalRoute, Map<PeerId, NodeId> nodeMap, ReadOnlyTransaction readOnlyTransaction) {
        AttributeOperations attributeOperations= AttributeOperations.getInstance(effectiveAttributes);
        QosSpeaker qosSpeaker= QosSpeaker.getInstance(routeId);
        //asSequence, might not be needed later on
        Optional<List<AsNumber>> AsNumberList= attributeOperations.getAsList(effectiveAttributes);

        //get Qos for the AS, and aggregate it.
        Optional<QosStlvBuilder> maybeLocalQosStlvBuilder= qosSpeaker.getQosSubAttribute(peerDestiny, routePeerId,
                advPeerRole, ifInternalRoute,nodeMap, readOnlyTransaction);
        Optional<QosStlvBuilder> maybeNewQosStlvBuilder= qosSpeaker.aggregateQos(maybeLocalQosStlvBuilder, routePeerId, peerDestiny);

        //if qos object is formed, extend it to attributes
        if(maybeNewQosStlvBuilder.isPresent()){
            ContainerNode extendedAttributes= attributeOperations.getBindingIndependentAttr(effectiveAttributes,maybeNewQosStlvBuilder.get().build());
            //LOG.info("Added QosStlv {} to routeId {} AdvPeerId {}",maybeNewQosStlvBuilder.get().getMetric().toString(), routeId.toString(),peerDestiny.getValue());
            effectiveAttributes= extendedAttributes;
        }
        readOnlyTransaction.close();
        return effectiveAttributes;
    }


    public boolean ifQosExtension(PeerId routePeerId, ContainerNode effectiveAttributes, PeerRole advPeerRole) {
        if(advPeerRole.equals(PeerRole.RrClient))
            return true;
        else if(advPeerRole.equals(PeerRole.Ebgp))
            return true;
        return false;
    }


    public void registerQos(ContainerNode advertisedAttrs, PeerId peerId, PathArgument routeId) {
        Optional<QosStlvBuilder> maybeQosStlv= AttributeOperations.getInstance(advertisedAttrs).getQosAttributeStlv(advertisedAttrs);
        if(maybeQosStlv.isPresent()){
            QosSpeaker.getInstance(routeId).getRouteQosRegistry().registerReceivedQos(peerId,maybeQosStlv.get());
        }
    }
}