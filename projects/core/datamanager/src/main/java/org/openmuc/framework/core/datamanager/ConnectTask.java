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

package org.openmuc.framework.core.datamanager;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConnectTask extends DeviceTask {

    private final static Logger logger = LoggerFactory.getLogger(ConnectTask.class);

    public ConnectTask(DriverService driver, Device device, DataManager dataManager) {
        this.driver = driver;
        this.device = device;
        this.dataManager = dataManager;
    }

    @Override
    public void run() {

        try {
            device.connection = driver.connect(device.deviceConfig.deviceAddress, device.deviceConfig.settings);
        } catch (ConnectionException e) {
            logger.warn("Unable to connect to device {} because {}. Will try again in {}ms.", device.deviceConfig.id, e.getMessage(),
                        device.deviceConfig.connectRetryInterval);
            synchronized (dataManager.connectionFailures) {
                dataManager.connectionFailures.add(device);
            }
            dataManager.interrupt();
            return;
        } catch (ArgumentSyntaxException e) {
            logger.warn("Unable to connect to device {} because the address or settings syntax is incorrect: {}. Will try again in {}ms.",
                        device.deviceConfig.id, e.getMessage(), device.deviceConfig.connectRetryInterval);
            synchronized (dataManager.connectionFailures) {
                dataManager.connectionFailures.add(device);
            }
            dataManager.interrupt();
            return;
        } catch (Exception e) {
            logger.warn("unexpected exception thrown by connect funtion of driver", e);
            synchronized (dataManager.connectionFailures) {
                dataManager.connectionFailures.add(device);
            }
            dataManager.interrupt();
            return;
        }

        if (device.connection == null) {
            logger.error("Drivers connect() function returned null");
            synchronized (dataManager.connectionFailures) {
                dataManager.connectionFailures.add(device);
            }
            dataManager.interrupt();
            return;
        }

        synchronized (dataManager.connected) {
            dataManager.connected.add(device);
        }
        dataManager.interrupt();

    }

    @Override
    public DeviceTaskType getType() {
        return DeviceTaskType.CONNECT;
    }

    @Override
    public void setDeviceState() {
        device.state = DeviceState.CONNECTING;
    }

}
