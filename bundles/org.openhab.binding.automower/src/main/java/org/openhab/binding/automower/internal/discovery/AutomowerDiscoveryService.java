/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.automower.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.automower.internal.dto.Automowers;
import org.openhab.binding.automower.internal.handler.AutomowerBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

import static org.openhab.binding.automower.internal.AutomowerBindingConstants.MOWER_THING_TYPE;
import static org.openhab.binding.automower.internal.AutomowerBindingConstants.SUPPORTED_THING_TYPES_UIDS;

/**
 * The {@link AutomowerDiscoveryService} searches for available
 * mower discoverable through AMCAPI
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Martin Hagelin - Copied to Automower binding
 */
@NonNullByDefault
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
        String[] relations = bridgeHandler.getAutomowerRelationsURL();
        Arrays.stream(relations).forEach(relationURL -> {
            try {
                AccountVehicleRelation accountVehicle = bridgeHandler.getURL(relationURL, AccountVehicleRelation.class);
                logger.debug("Found vehicle : {}", accountVehicle.vehicleId);

                Automowers automower = bridgeHandler.getURL(accountVehicle.vehicleURL, Vehicles.class);
                Attributes attributes = bridgeHandler.getURL(Attributes.class, vehicle.vehicleId);

                thingDiscovered(
                        DiscoveryResultBuilder.create(new ThingUID(MOWER_THING_TYPE, accountVehicle.vehicleId))
                                .withLabel(attributes.vehicleType + " " + attributes.registrationNumber)
                                .withBridge(bridgeHandler.getThing().getUID()).withProperty(VIN, attributes.vin)
                                .withRepresentationProperty(accountVehicle.vehicleId).build());

            } catch (IOException e) {
                logger.warn("Error while discovering automower: {}", e.getMessage());
            }
        });
        stopScan();
    }
}
