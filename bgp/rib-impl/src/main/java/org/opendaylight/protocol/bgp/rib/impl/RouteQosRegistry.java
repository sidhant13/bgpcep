/*
 * Copyright © 2015 For the craic! and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.Qos;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.QosBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.qos.QosStlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RouteQosRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(RouteQosRegistry.class);

    /*
     * At present we are not maintaining history, so only one QoS per advertising peer.
     * It would also at present work for one-to-one mapping between route and peer.
     * i.e one peer per route
     * and one qos per route
     * Although code should handle a route from multiple peers.
     */
    public Map<PeerId, Qos> receivedQosEntries= new HashMap<>();

    public Map<RouteEndpointKey, Qos> localQos;

    public Map<RouteEndpointKey, Qos> advertisedQosEntries= new HashMap<>();

    private Ipv4Address routeNlri;

    public RouteQosRegistry(Ipv4Address routeNlri){
      this.routeNlri= routeNlri;
    }

    public void registerLocalQos(QosStlvBuilder qosStlvBuilder, PeerId originPeer, PeerId advPeer) {
        Qos qos= new QosBuilder().setQosStlv(qosStlvBuilder.build()).build();
        localQos.put(new RouteEndpointKey(originPeer, advPeer),qos);
        LOG.info("Added Local Qos for route {} {}",routeNlri.getValue(),qos.getQosStlv().getMetric().toString());
    }

    public void registerReceivedQos(PeerId peerId, QosStlvBuilder qosStlvBuilder) {
        Qos qos= new QosBuilder().setQosStlv(qosStlvBuilder.build()).build();
        receivedQosEntries.put(peerId, qos);
        LOG.info("Recieved Qos for route {} {} from peer {}",routeNlri.getValue(),qos.getQosStlv().getMetric().toString(),peerId.getValue());
    }


    public class RouteEndpointKey{

        private PeerId originPeer;
        private PeerId advPeer;

        public RouteEndpointKey(PeerId originPeer, PeerId advPeer){
            this.originPeer=originPeer;
            this.advPeer=advPeer;
        }
    }


    public RouteEndpointKey getRouteEndpointKey(PeerId originPeer, PeerId advPeer){
        return new RouteEndpointKey(originPeer, advPeer);
    }

    public Map<PeerId, Qos> getReceivedQosEntries() {
        return receivedQosEntries;
    }

    public Map<RouteEndpointKey, Qos> getLocalQos() {
        return localQos;
    }

    public Map<RouteEndpointKey, Qos> getAdvertisedQosEntries() {
        return advertisedQosEntries;
    }

}
