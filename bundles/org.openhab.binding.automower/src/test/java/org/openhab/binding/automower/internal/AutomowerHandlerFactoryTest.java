package org.openhab.binding.automower.internal;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

public class AutomowerHandlerFactoryTest {

    @Test
    public void testLogon() {
        AutomowerHandlerFactory automowerHandlerFactory = new AutomowerHandlerFactory();

        ThingUID thingUID = new ThingUID(AutomowerBindingConstants.APIBRIDGE_THING_TYPE, "HEJ");
        Thing thing = ThingBuilder.create(AutomowerBindingConstants.APIBRIDGE_THING_TYPE, thingUID).build();
        //automowerHandlerFactory.createHandler(thing);
    }

}