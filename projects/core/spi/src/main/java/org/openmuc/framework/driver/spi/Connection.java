package org.openmuc.framework.driver.spi;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;

import java.util.List;

/**
 * A connection represents an association to particular device. A driver returns an implementation of this interface
 * when {@link DriverService#connect(String, String)} is called.
 * <p/>
 * The OpenMUC framework can give certain guarantees about the order of the functions it calls:
 * <ul>
 * <li>Communication related functions (e.g. connect,read,write..) are never called concurrently for the same device.</li>
 * <li>The framework calls read,listen,write or channelScan only if a the device is considered connected. The device is
 * only considered connected if the connect function has been called successfully.</li>
 * <li>Before a driver service is unregistered or the data manager is stopped the framework calls disconnect for all
 * connected devices. The disconnect function should do any necessary resource clean up.</li>
 * </ul>
 *
 * @author Stefan Feuerhahn
 */
public interface Connection {

    /**
     * Scan a given communication device for available data channels.
     *
     * @param settings scanning settings. The syntax is driver specific.
     * @return A list of channels that were found.
     * @throws ArgumentSyntaxException       if the syntax of the deviceAddress or settings string is incorrect.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ScanException                 if an error occurs while scanning but the connection is still alive.
     * @throws ConnectionException           if an error occurs while scanning and the connection was closed
     */
    public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException, ArgumentSyntaxException,
            ScanException, ConnectionException;

    /**
     * Reads the data channels that correspond to the given record containers. The read result is returned by setting
     * the record in the containers. If for some reason no value can be read the record should be set anyways. In this
     * case the record constructor that takes only a flag should be used. The flag shall best describe the reason of
     * failure. If no record is set the default Flag is <code>Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE</code>. If the
     * connection to the device is interrupted, then any necessary resources that correspond to this connection should
     * be cleaned up and a <code>ConnectionException</code> shall be thrown.
     *
     * @param containers          the containers hold the information of what channels are to be read. They will be filled by this
     *                            function with the records read.
     * @param containerListHandle the containerListHandle returned by the last read call for this exact list of containers. Will be
     *                            equal to <code>null</code> if this is the first read call for this container list after a connection
     *                            has been established. Driver implementations can optionally use this object to improve the read
     *                            performance.
     * @param samplingGroup       the samplingGroup that was configured for this list of channels that are to be read. Sometimes it may
     *                            be desirable to give the driver a hint on how to group several channels when reading them. This can
     *                            done through the samplingGroup.
     * @return the containerListHandle Object that will passed the next time the same list of channels is to be read.
     * Use this Object as a handle to improve performance or simply return <code>null</code>.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup) throws
            UnsupportedOperationException, ConnectionException;

    /**
     * Starts listening on the given connection for data from the channels that correspond to the given record
     * containers. The list of containers will overwrite the list passed by the previous startListening call. Will
     * notify the given listener of new records that arrive on the data channels.
     *
     * @param containers the containers identify the channels to listen on. They will be filled by this function with the
     *                   records received and passed to the listener.
     * @param listener   the listener to inform that new data has arrived.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) throws
            UnsupportedOperationException, ConnectionException;

    /**
     * Writes the data channels that correspond to the given value containers. The write result is returned by setting
     * the flag in the containers. If the connection to the device is interrupted, then any necessary resources that
     * correspond to this connection should be cleaned up and a <code>ConnectionException</code> shall be thrown.
     *
     * @param containers          the containers hold the information of what channels are to be written and the values that are to
     *                            written. They will be filled by this function with a flag stating whether the write process was
     *                            successful or not.
     * @param containerListHandle the containerListHandle returned by the last write call for this exact list of containers. Will be
     *                            equal to <code>null</code> if this is the first read call for this container list after a connection
     *                            has been established. Driver implementations can optionally use this object to improve the write
     *                            performance.
     * @return the containerListHandle Object that will passed the next time the same list of channels is to be written.
     * Use this Object as a handle to improve performance or simply return <code>null</code>.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle) throws UnsupportedOperationException, ConnectionException;

    /**
     * Disconnects or closes the connection. Cleans up any resources associated with the connection.
     */
    public void disconnect();

}
