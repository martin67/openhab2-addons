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
package org.openhab.binding.automower.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The {@link AutomowerBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Martin Hagelin - Initial contribution
 */
@NonNullByDefault
public class AutomowerBindingConstants {

    private static final String BINDING_ID = "automower";

    // The URL to use to connect to AMC API with.
    public static final String SERVICE_URL = "https://api.amc.husqvarna.dev/v1/";

    // List of all Thing Type UIDs
    public static final ThingTypeUID APIBRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, "amcapi");
    public static final ThingTypeUID MOWER_THING_TYPE = new ThingTypeUID(BINDING_ID, "mower");

    // List of all Channel ids
    public static final String NAME = "name";
    public static final String MODEL = "model";

    // List of all adressable things in OH = SUPPORTED_DEVICE_THING_TYPES_UIDS + the virtual bridge
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Stream
            .of(APIBRIDGE_THING_TYPE, MOWER_THING_TYPE).collect(Collectors.toSet());
}
