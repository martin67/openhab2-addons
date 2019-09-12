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
package org.openhab.binding.automower.internal;

import static org.openhab.binding.automower.internal.AutomowerBindingConstants.*;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthClientService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.automower.internal.discovery.AutomowerDiscoveryService;
import org.openhab.binding.automower.internal.handler.AutomowerBridgeHandler;
import org.openhab.binding.automower.internal.handler.MowerHandler;
import org.openhab.binding.automower.internal.handler.MowerStateDescriptionProvider;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AutomowerHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Martin Hagelin - C
 */
@NonNullByDefault
@Component(configurationPid = "binding.automower", service = ThingHandlerFactory.class)
public class AutomowerHandlerFactory extends BaseThingHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(AutomowerHandlerFactory.class);
    private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();
    private @NonNullByDefault({}) MowerStateDescriptionProvider stateDescriptionProvider;
    private @NonNullByDefault({}) OAuthClientService oAuthService;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(APIBRIDGE_THING_TYPE)) {
            AutomowerBridgeHandler bridgeHandler = new AutomowerBridgeHandler((Bridge) thing);
            registerDeviceDiscoveryService(bridgeHandler);
            return bridgeHandler;
        } else if (thingTypeUID.equals(MOWER_THING_TYPE) && stateDescriptionProvider != null) {
            return new MowerHandler(thing, stateDescriptionProvider);
        } else {
            logger.warn("ThingHandler not found for {}", thing.getThingTypeUID());
            return null;
        }
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof AutomowerBridgeHandler) {
            ThingUID thingUID = thingHandler.getThing().getUID();
            unregisterDeviceDiscoveryService(thingUID);
        }
        super.removeHandler(thingHandler);
    }

    private void registerDeviceDiscoveryService(AutomowerBridgeHandler bridgeHandler) {
        AutomowerDiscoveryService discoveryService = new AutomowerDiscoveryService(bridgeHandler);
        discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    private void unregisterDeviceDiscoveryService(ThingUID thingUID) {
        if (discoveryServiceRegs.containsKey(thingUID)) {
            ServiceRegistration<?> serviceReg = discoveryServiceRegs.get(thingUID);
            serviceReg.unregister();
            discoveryServiceRegs.remove(thingUID);
        }
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(MowerStateDescriptionProvider provider) {
        this.stateDescriptionProvider = provider;
    }

    protected void unsetDynamicStateDescriptionProvider(MowerStateDescriptionProvider provider) {
        this.stateDescriptionProvider = null;
    }
}
