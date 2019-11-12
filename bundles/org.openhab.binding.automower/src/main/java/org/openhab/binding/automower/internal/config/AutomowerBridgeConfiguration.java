package org.openhab.binding.automower.internal.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The {@link AutomowerBridgeConfiguration} is responsible for holding
 * configuration information needed to access AMC API
 *
 * @author Martin Hagelin - Initial contribution
 */
public class AutomowerBridgeConfiguration {
    public String username;
    public String password;

    public String getAuthorization() {
        byte[] authorization = Base64.getEncoder().encode((username + ":" + password).getBytes());
        return "Basic " + new String(authorization, StandardCharsets.UTF_8);
    }
}
