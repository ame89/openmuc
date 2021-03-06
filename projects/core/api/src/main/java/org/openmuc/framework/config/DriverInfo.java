/*
 * Copyright 2011-15 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.config;

public class DriverInfo {

    private final String id;
    private final String description;
    private final String deviceAddressSyntax;
    private final String parametersSyntax;
    private final String channelAddressSyntax;
    private final String deviceScanParametersSyntax;

    public DriverInfo(String id, String description, String deviceAddressSyntax, String parametersSyntax, String channelAddressSyntax,
                      String deviceScanParametersSyntax) {
        this.id = id;
        this.description = description;
        this.deviceAddressSyntax = deviceAddressSyntax;
        this.parametersSyntax = parametersSyntax;
        this.channelAddressSyntax = channelAddressSyntax;
        this.deviceScanParametersSyntax = deviceScanParametersSyntax;
    }

    /**
     * Returns the ID of the driver. The ID may only contain ASCII letters, digits, hyphens and underscores. By
     * convention the ID should be meaningful and all lower case letters (e.g. "mbus", "modbus").
     *
     * @return the unique ID of the driver.
     */
    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDeviceAddressSyntax() {
        return deviceAddressSyntax;
    }

    public String getParametersSyntax() {
        return parametersSyntax;
    }

    public String getChannelAddressSyntax() {
        return channelAddressSyntax;
    }

    public String getDeviceScanParametersSyntax() {
        return deviceScanParametersSyntax;
    }

}
