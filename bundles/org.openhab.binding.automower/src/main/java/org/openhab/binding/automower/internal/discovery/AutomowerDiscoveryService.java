/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.automower.internal.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.openhab.binding.automower.internal.handler.AutomowerBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.openhab.binding.automower.internal.AutomowerBindingConstants.SUPPORTED_THING_TYPES_UIDS;

/**
 * The {@link AutomowerDiscoveryService} searches for available
 * mower discoverable through AMCAPI
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Martin Hagelin - Copied to Automower binding
 */
public class AutomowerDiscoveryService extends AbstractDiscoveryService {
    private static final int SEARCH_TIME = 2;
    private final Logger logger = LoggerFactory.getLogger(AutomowerDiscoveryService.class);
    private final AutomowerBridgeHandler bridgeHandler;

    public AutomowerDiscoveryService(AutomowerBridgeHandler bridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    public void startScan() {

        stopScan();
    }
}
