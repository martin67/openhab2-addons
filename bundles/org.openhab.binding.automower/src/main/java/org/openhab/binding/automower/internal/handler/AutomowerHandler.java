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
package org.openhab.binding.automower.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AutomowerHandler} is responsible for handling commands, which are sent
 * to one of the channels.
 *
 * @author Gaël L'hopital - Initial contribution
 * @author Martin Hagelin - Copied to Automower binding
 */
@NonNullByDefault
public class AutomowerHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(AutomowerHandler.class);

    public AutomowerHandler(Thing thing, AutomowerStateDescriptionProvider stateDescriptionProvider) {
        super(thing);
    }

    @Override
    public void handleCommand(
            ChannelUID channelUID, Command command) {
        logger.debug("No commands yet!");
    }
}
