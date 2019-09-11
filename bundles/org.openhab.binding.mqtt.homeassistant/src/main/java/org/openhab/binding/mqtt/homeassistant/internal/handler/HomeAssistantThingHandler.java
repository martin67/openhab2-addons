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
package org.openhab.binding.mqtt.homeassistant.internal.handler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.binding.mqtt.generic.AbstractMQTTThingHandler;
import org.openhab.binding.mqtt.generic.ChannelState;
import org.openhab.binding.mqtt.generic.MqttChannelTypeProvider;
import org.openhab.binding.mqtt.generic.TransformationServiceProvider;
import org.openhab.binding.mqtt.generic.tools.DelayedBatchProcessing;
import org.openhab.binding.mqtt.homeassistant.generic.internal.MqttBindingConstants;
import org.openhab.binding.mqtt.homeassistant.internal.AbstractComponent;
import org.openhab.binding.mqtt.homeassistant.internal.CChannel;
import org.openhab.binding.mqtt.homeassistant.internal.CFactory;
import org.openhab.binding.mqtt.homeassistant.internal.ChannelConfigurationTypeAdapterFactory;
import org.openhab.binding.mqtt.homeassistant.internal.DiscoverComponents;
import org.openhab.binding.mqtt.homeassistant.internal.DiscoverComponents.ComponentDiscovered;
import org.openhab.binding.mqtt.homeassistant.internal.HaID;
import org.openhab.binding.mqtt.homeassistant.internal.HandlerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Handles HomeAssistant MQTT object things. Such an HA Object can have multiple HA Components with different instances
 * of those Components. This handler auto-discovers all available Components and Component Instances and
 * adds any new appearing components over time.<br>
 * <br>
 *
 * The specification does not cover the case of disappearing Components. This handler doesn't as well therefore.<br>
 * <br>
 *
 * A Component Instance equals an ESH Channel Group and the Component parts equal ESH Channels.<br>
 * <br>
 *
 * If a Components configuration changes, the known ChannelGroupType and ChannelTypes are replaced with the new ones.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class HomeAssistantThingHandler extends AbstractMQTTThingHandler
        implements ComponentDiscovered, Consumer<List<AbstractComponent<?>>> {
    public static final String AVAILABILITY_CHANNEL = "availability";

    private final Logger logger = LoggerFactory.getLogger(HomeAssistantThingHandler.class);

    protected final MqttChannelTypeProvider channelTypeProvider;
    public final int attributeReceiveTimeout;
    protected final DelayedBatchProcessing<AbstractComponent<?>> delayedProcessing;
    protected final DiscoverComponents discoverComponents;

    private final Gson gson;
    protected final Map<String, AbstractComponent<?>> haComponents = new HashMap<>();

    protected HandlerConfiguration config = new HandlerConfiguration();
    private Set<HaID> discoveryHomeAssistantIDs = new HashSet<>();

    protected final TransformationServiceProvider transformationServiceProvider;

    /**
     * Create a new thing handler for HomeAssistant MQTT components.
     * A channel type provider and a topic value receive timeout must be provided.
     *
     * @param thing The thing of this handler
     * @param channelTypeProvider A channel type provider
     * @param subscribeTimeout Timeout for the entire tree parsing and subscription. In milliseconds.
     * @param attributeReceiveTimeout The timeout per attribute field subscription. In milliseconds.
     */
    public HomeAssistantThingHandler(Thing thing, MqttChannelTypeProvider channelTypeProvider,
            TransformationServiceProvider transformationServiceProvider, int subscribeTimeout,
            int attributeReceiveTimeout) {
        super(thing, subscribeTimeout);
        this.gson = new GsonBuilder().registerTypeAdapterFactory(new ChannelConfigurationTypeAdapterFactory()).create();
        this.channelTypeProvider = channelTypeProvider;
        this.transformationServiceProvider = transformationServiceProvider;
        this.attributeReceiveTimeout = attributeReceiveTimeout;
        this.delayedProcessing = new DelayedBatchProcessing<>(attributeReceiveTimeout, this, scheduler);
        this.discoverComponents = new DiscoverComponents(thing.getUID(), scheduler, this, gson,
                this.transformationServiceProvider);
    }

    @SuppressWarnings({ "null", "unused" })
    @Override
    public void initialize() {
        config = getConfigAs(HandlerConfiguration.class);
        if (CollectionUtils.isEmpty(config.topics)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Device topics unknown");
            return;
        }
        discoveryHomeAssistantIDs.addAll(HaID.fromConfig(config));

        for (Channel channel : thing.getChannels()) {
            final String groupID = channel.getUID().getGroupId();
            if (groupID == null) {
                logger.warn("Channel {} has no groupd ID", channel.getLabel());
                continue;
            }
            // Already restored component?
            @Nullable
            AbstractComponent<?> component = haComponents.get(groupID);
            if (component != null) {
                // the types may have been removed in dispose() so we need to add them again
                component.addChannelTypes(channelTypeProvider);
                continue;
            }

            HaID haID = HaID.fromConfig(config.basetopic, channel.getConfiguration());
            discoveryHomeAssistantIDs.add(haID);
            ThingUID thingUID = channel.getUID().getThingUID();
            String channelConfigurationJSON = (String) channel.getConfiguration().get("config");
            if (channelConfigurationJSON == null) {
                logger.warn("Provided channel does not have a 'config' configuration key!");
            } else {
                component = CFactory.createComponent(thingUID, haID, channelConfigurationJSON, this, gson,
                        transformationServiceProvider);
            }

            if (component != null) {
                haComponents.put(component.uid().getId(), component);
                component.addChannelTypes(channelTypeProvider);
            } else {
                logger.warn("Could not restore component {}", thing);
            }
        }
        updateThingType();

        super.initialize();
    }

    @Override
    public void dispose() {
        // super.dispose() calls stop()
        super.dispose();
        haComponents.values().forEach(c -> c.removeChannelTypes(channelTypeProvider));
    }

    @Override
    public CompletableFuture<Void> unsubscribeAll() {
        // already unsubscribed everything by calling stop()
        return CompletableFuture.allOf();
    }

    /**
     * Start a background discovery for the configured HA MQTT object-id.
     */
    @Override
    protected CompletableFuture<@Nullable Void> start(MqttBrokerConnection connection) {
        connection.setRetain(true);
        connection.setQos(1);

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "No response from the device yet");

        // Start all known components and channels within the components and put the Thing offline
        // if any subscribing failed ( == broker connection lost)
        CompletableFuture<@Nullable Void> future = haComponents.values().stream()
                .map(e -> e.start(connection, scheduler, attributeReceiveTimeout))
                .reduce(CompletableFuture.completedFuture(null), (a, v) -> a.thenCompose(b -> v)) // reduce to one
                .exceptionally(e -> {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
                    return null;
                });

        return future
                .thenCompose(b -> discoverComponents.startDiscovery(connection, 0, discoveryHomeAssistantIDs, this));
    }

    @Override
    protected void stop() {
        discoverComponents.stopDiscovery();
        delayedProcessing.join();
        // haComponents does not need to be synchronised -> the discovery thread is disabled
        haComponents.values().stream().map(e -> e.stop());
    }

    @SuppressWarnings({ "null", "unused" })
    @Override
    public @Nullable ChannelState getChannelState(ChannelUID channelUID) {
        String groupID = channelUID.getGroupId();
        if (groupID == null) {
            return null;
        }
        AbstractComponent<?> component;
        synchronized (haComponents) { // sync whenever discoverComponents is started
            component = haComponents.get(groupID);
        }
        if (component == null) {
            return null;
        }
        CChannel componentChannel = component.channel(channelUID.getIdWithoutGroup());
        if (componentChannel == null) {
            return null;
        }
        return componentChannel.getState();
    }

    /**
     * Callback of {@link DiscoverComponents}. Add to a delayed batch processor.
     */
    @Override
    public void componentDiscovered(HaID homeAssistantTopicID, AbstractComponent<?> component) {
        delayedProcessing.accept(component);
    }

    /**
     * Callback of {@link DelayedBatchProcessing}.
     * Add all newly discovered components to the Thing and start the components.
     */
    @SuppressWarnings("null")
    @Override
    public void accept(List<AbstractComponent<?>> discoveredComponentsList) {
        MqttBrokerConnection connection = this.connection;
        if (connection == null) {
            return;
        }

        List<Channel> channels;
        synchronized (haComponents) { // sync whenever discoverComponents is started
            for (AbstractComponent<?> discovered : discoveredComponentsList) {
                AbstractComponent<?> known = haComponents.get(discovered.uid().getId());
                // Is component already known?
                if (known != null) {
                    if (discovered.getConfigHash() != known.getConfigHash()) {
                        // Don't wait for the future to complete. We are also not interested in failures.
                        // The component will be replaced in a moment.
                        known.stop();
                    } else {
                        known.setConfigSeen();
                        continue;
                    }
                }

                // Add channel and group types to the types registry
                discovered.addChannelTypes(channelTypeProvider);
                // Add component to the component map
                haComponents.put(discovered.uid().getId(), discovered);
                // Start component / Subscribe to channel topics
                discovered.start(connection, scheduler, 0).exceptionally(e -> {
                    logger.warn("Failed to start component {}", discovered.uid(), e);
                    return null;
                });
            }
            // Add channels to Thing
            channels = haComponents.values().stream().map(c -> c.channelTypes().values()).flatMap(Collection::stream)
                    .map(c -> c.getChannel()).collect(Collectors.toList());
        }

        updateThingType();
        updateThing(editThing().withChannels(channels).build());
        updateThingStatus();
    }

    private void updateThingStatus() {

        boolean allActive;
        synchronized (haComponents) { // sync whenever discoverComponents is started
            allActive = haComponents.values().stream().allMatch(comp -> comp.isActive());
        }

        if (allActive) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.GONE, "At least one component not active");
        }
    }

    private void updateThingType() {
        // if this is a dynamic type, then we update the type
        ThingTypeUID typeID = thing.getThingTypeUID();
        if (!MqttBindingConstants.HOMEASSISTANT_MQTT_THING.equals(typeID)) {

            List<ChannelGroupDefinition> groupDefs;
            List<ChannelDefinition> channelDefs;
            synchronized (haComponents) { // sync whenever discoverComponents is started
                groupDefs = haComponents.values().stream().map(c -> c.getGroupDefinition())
                        .collect(Collectors.toList());
                channelDefs = haComponents.values().stream().map(c -> c.type()).map(t -> t.getChannelDefinitions())
                        .flatMap(List::stream).collect(Collectors.toList());
            }
            ThingType thingType = channelTypeProvider.derive(typeID, MqttBindingConstants.HOMEASSISTANT_MQTT_THING)
                    .withChannelDefinitions(channelDefs).withChannelGroupDefinitions(groupDefs).build();

            channelTypeProvider.setThingType(typeID, thingType);
        }
    }

    @Override
    public void updateChannelState(ChannelUID channelUID, State value) {

        if (StringUtils.equals(channelUID.getIdWithoutGroup(), AVAILABILITY_CHANNEL)) {
            updateThingStatus();
            return;
        }
        super.updateChannelState(channelUID, value);
    }
}
