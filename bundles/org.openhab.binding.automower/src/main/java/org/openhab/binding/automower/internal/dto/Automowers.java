package org.openhab.binding.automower.internal.dto;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The{@link Automowers}is responsible for storing
 * informations returned by list mowers rest answer
 *
 * @author Martin Hagelin
 */

@NonNullByDefault
public class Automowers {
    public @NonNullByDefault({})
    String mowerId;
}
