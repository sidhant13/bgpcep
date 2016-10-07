/*
 * Copyright © 2015 For the craic! and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.protocol.bgp.rib.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.qos.QosStlvBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.PeerRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.network.paths.rev150105.QosPaths;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.network.paths.rev150105.path.qos.group.PathQosValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.network.paths.rev150105.qos.paths.Path;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.network.paths.rev150105.qos.paths.PathKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.openflow.linkstate.rev150105.OflsTopologyAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.openflow.linkstate.rev150105.linkstate.topology.attributes.Host;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.pathman.openflow.linkstate.rev150105.linkstate.topology.attributes.HostKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QosSpeaker {

    private static final Logger LOG = LoggerFactory.getLogger(QosSpeaker.class);
    private static Map<PathArgument,QosSpeaker> routeEntries= new HashMap<>();
    private static List<PathArgument> localRoutes= new ArrayList<>();
    private static final short qosSubAttributeType= new Integer(3).shortValue();
    private static InstanceIdentifier<OflsTopologyAttributes> topoIid= InstanceIdentifier.builder(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("example-linkstate-topology")))
            .augmentation(OflsTopologyAttributes.class)
            .build();
    private static InstanceIdentifier<QosPaths> qosPathIid=  InstanceIdentifier.builder(QosPaths.class).build();

    private PathArgument routeId;
    private RouteQosRegistry routeQosRegistry;
    private boolean isLocalRoute;
    private ReadOnlyTransaction readTx;
    private Ipv4Address ipv4Address;

    public static QosSpeaker getInstance(PathArgument routeId){
        if(routeEntries.containsKey(routeId))
            return routeEntries.get(routeId);
        else{
            routeEntries.put(routeId, new QosSpeaker(routeId));
            return routeEntries.get(routeId);
        }
    }

    private QosSpeaker(PathArgument routeId){
        this.routeId=routeId;
        String[] strArr= routeId.toString().split("prefix=");
        String ipstring=strArr[1].substring(0, strArr[1].indexOf("/"));
        this.ipv4Address= new Ipv4Address(ipstring);
        routeQosRegistry = new RouteQosRegistry(ipv4Address);
    }

    /*
     * TODO
     * Separate the QosReader and QosWriter Functionality
     */
    public Optional<QosStlvBuilder> getQosSubAttribute(PeerId peerDestiny, PeerId origin, PeerRole advPeerRole,
            boolean ifInternalRoute, Map<PeerId, NodeId> nodeMap, ReadOnlyTransaction readOnlyTransaction){

        this.readTx=readOnlyTransaction;
        Optional<NodeId> originNodeId= getRouteNodeId(origin, ifInternalRoute, nodeMap);
        Optional<NodeId> destNodeId= getRouteNodeId(peerDestiny, false, nodeMap);

        if(originNodeId.isPresent() && destNodeId.isPresent() ){
            QosStlvBuilder qosStlvBuilder= new QosStlvBuilder();
            qosStlvBuilder.setType(qosSubAttributeType);
            Optional<List<String>> pathQosString=getPathQos(originNodeId.get(),destNodeId.get());
            if(pathQosString.isPresent()){
                qosStlvBuilder.setMetric(pathQosString.get());
                LOG.debug("Updating Qos for {} {}",peerDestiny.getValue(),qosStlvBuilder.getMetric());
                return Optional.fromNullable(qosStlvBuilder);
            }
        }
        return Optional.absent();
    }

    /*
     * Gets QoS from Path Module
     */
    private Optional<List<String>> getPathQos(NodeId originNodeId, NodeId destNodeId) {
        PathKey pk= new PathKey(new Uri(destNodeId.getValue() + "->" + originNodeId.getValue()));
        CheckedFuture<Optional<Path>, ReadFailedException> future= readTx.read(LogicalDatastoreType.CONFIGURATION,qosPathIid.child(Path.class, pk));
        Optional<Path> maybePath;
        try{
            maybePath=future.checkedGet();
            if(maybePath.isPresent()){
                Path path = maybePath.get();
                List<PathQosValue> pathQosValues= path.getPathQosValue();
                for(PathQosValue pathQosValue: pathQosValues){
                    if(pathQosValue.getQosTypeId().equals(qosSubAttributeType)) {
                        List<String> qosList= new ArrayList<>();
                        qosList.add(pathQosValue.getQosValue());
                        return Optional.fromNullable(qosList);
                    }
                }
            }
        } catch (ReadFailedException e) {
            LOG.warn("Read Fail Exception");
        }
        LOG.warn("Cant find Qos for {}",pk.toString());
        return Optional.absent();
    }

    /*
     * gets nodeid from routeid
     */
    private Optional<NodeId> getRouteNodeId(PeerId origin, boolean ifInternalRoute, Map<PeerId, NodeId> nodeMap) {
        if(!ifInternalRoute){
            if(nodeMap.containsKey(origin)){
                NodeId nodeid= nodeMap.get(origin);
                //LOG.info("{} {}",nodeid.getValue(),origin.getValue());
                return Optional.fromNullable(nodeid);
            }
            LOG.warn("No node id found for peer {}",origin.getValue());
            return Optional.absent();
        }
        else{
            //Ipv4Address ipv4Address=new Ipv4Address("10.0.10.1");
            HostKey hk= new HostKey(ipv4Address);
            CheckedFuture<Optional<Host>, ReadFailedException> future= readTx.read(LogicalDatastoreType.OPERATIONAL,topoIid.child(Host.class, hk));
            Optional<Host> maybeHost;
            try {
                maybeHost = future.checkedGet();
                if(maybeHost.isPresent()){
                    String hostNode =maybeHost.get().getNodeAtt().getNodeId().getValue();
                    //LOG.info("{} {} {}",hostNode,origin.getValue(),ipv4Address.getValue());
                    return Optional.fromNullable(new NodeId(hostNode));
                }
            } catch (ReadFailedException e) {
            }
            isLocalRoute=true;
            LOG.warn("No node id found for peer {} ip{}",origin.getValue(),ipv4Address.getValue());
            return Optional.absent();
        }
    }

    public Optional<QosStlvBuilder> aggregateQos(Optional<QosStlvBuilder> maybeLocalQosStlvBuilder, PeerId routePeerId, PeerId peerDestiny) {
        if(maybeLocalQosStlvBuilder.isPresent()){
            routeQosRegistry.registerLocalQos(maybeLocalQosStlvBuilder.get(), routePeerId, peerDestiny);
            if(routeQosRegistry.getReceivedQosEntry(routePeerId).isPresent()){
                List<String> metricList= routeQosRegistry.getReceivedQosEntry(routePeerId).get().getQosStlv().getMetric();
                List<String> aggMetricList= maybeLocalQosStlvBuilder.get().getMetric();
                aggMetricList.addAll(metricList);
                QosStlvBuilder aggQosStlvBuilder= new QosStlvBuilder(maybeLocalQosStlvBuilder.get().build()).setMetric(aggMetricList);
                routeQosRegistry.registerAdvertisedQos(routePeerId, peerDestiny, aggQosStlvBuilder);
                return Optional.of(aggQosStlvBuilder);
            }
            else{
                LOG.info("Nothing to aggregate. Returning original");
                return maybeLocalQosStlvBuilder;
            }
        }
        LOG.info("Cannot aggregate. LocalQoS not present");
        return maybeLocalQosStlvBuilder;
    }

    public RouteQosRegistry getRouteQosRegistry() {
        return routeQosRegistry;
    }
}
