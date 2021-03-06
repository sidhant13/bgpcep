/*
 * Copyright (c) 2016 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.bgp.parser.spi.pojo;

import com.google.common.base.Preconditions;
import java.util.List;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderActivator;
import org.opendaylight.protocol.bgp.parser.spi.BGPExtensionProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops BGPExtensionProviderActivator instances for a BGPExtensionProviderContext.
 *
 * @author Thomas Pantelis
 */
public class SimpleBGPExtensionProviderContextActivator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleBGPExtensionProviderContextActivator.class);

    private final BGPExtensionProviderContext providerContext;
    private final List<BGPExtensionProviderActivator> extensionActivators;

    public SimpleBGPExtensionProviderContextActivator(final BGPExtensionProviderContext providerContext,
            final List<BGPExtensionProviderActivator> extensionActivators) {
        this.providerContext = Preconditions.checkNotNull(providerContext);
        this.extensionActivators = Preconditions.checkNotNull(extensionActivators);
    }

    public void start() {
        LOG.debug("Starting {} BGPExtensionProviderActivator instances", extensionActivators.size());

        for(final BGPExtensionProviderActivator e : extensionActivators) {
            e.start(providerContext);
        }
    }

    @Override
    public void close() {
        LOG.debug("Stopping {} BGPExtensionProviderActivator instances", extensionActivators.size());

        for(final BGPExtensionProviderActivator e : extensionActivators) {
            e.stop();
        }
    }
}
