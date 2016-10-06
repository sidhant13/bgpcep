/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.bgp.rib.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

/**
 * @deprecated Replaced by blueprint wiring
 */
@Deprecated
public class StrictBgpPeerRegistryModuleFactory extends AbstractStrictBgpPeerRegistryModuleFactory {
    @Override
    public StrictBgpPeerRegistryModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            StrictBgpPeerRegistryModule oldModule, AutoCloseable oldInstance, BundleContext bundleContext) {
        StrictBgpPeerRegistryModule module = super.instantiateModule(instanceName, dependencyResolver, oldModule,
                oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public StrictBgpPeerRegistryModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
            BundleContext bundleContext) {
        StrictBgpPeerRegistryModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
