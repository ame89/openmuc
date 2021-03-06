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
package org.openmuc.framework.datalogger.ascii;

import org.openmuc.framework.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class LogFileReader {

    private static final Logger logger = LoggerFactory.getLogger(LogFileReader.class);

    private final String channelId;
    private final String path;
    private final int loggingInterval;
    private int unixTimestampColumn;
    private long startTimestamp;
    private long endTimestamp;
    private long firstTimestampFromFile;

    /**
     * Constructor
     *
     * @param path
     * @param id
     * @param loggingInterval
     */
    public LogFileReader(String path, String id, int loggingInterval) {
        this.path = path;
        channelId = id;
        this.loggingInterval = loggingInterval;
        firstTimestampFromFile = -1;
    }

    /**
     * @return All records of the given time span
     */
    public List<Record> getValues(long startTimestamp, long endTimestamp) {

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;

        List<Record> allRecords = new ArrayList<Record>();

        List<String> filenames = LoggerUtils.getFilenames(loggingInterval, this.startTimestamp, this.endTimestamp);

        for (int i = 0; i < filenames.size(); i++) {
            Boolean nextFile = false;
            logger.debug("using " + filenames.get(i));

            String filepath;
            if (path.endsWith("/")) {
                filepath = path + filenames.get(i);
            } else {
                filepath = path + "/" + filenames.get(i);
            }

            if (i > 0) {
                nextFile = true;
            }
            List<Record> fileRecords = processFile(filepath, nextFile);
            if (fileRecords != null) {
                allRecords.addAll(fileRecords);
                logger.debug("read records: " + fileRecords.size());
            } else {
                // some error occurred while processing the file so no records will be added
            }

        }
        return allRecords;
    }

    /**
     * @param timestamp
     * @return Record on success, otherwise null
     */
    public Record getValue(long timestamp) {
        // Returns a record which lays within the interval [timestamp, timestamp + loggingInterval]
        // The interval is necessary for a requested time stamp which lays between the time stamps of two logged values
        // e.g.: t_request = 7, t1_logged = 5, t2_logged = 10, loggingInterval = 5
        // method will return the record of t2_logged because this lays within the interval [7,12]
        // If the t_request matches exactly a logged time stamp, then the according record is returned.

        Record record = null;
        List<Record> records = getValues(timestamp, timestamp);// + loggingInterval);
        if (records.size() == 0) {
            // no record found for requested timestamp

            // TODO statt null flag 3 setzen!
            record = null;
        } else if (records.size() == 1) {
            // t_request lays between two logged values
            record = records.get(0);
        } else if (records.size() == 2) {
            // t_request matches exactly a logged value
            // so getVaules returns a record for t_request and one for t_request+loggingInterval
            record = records.get(0);
        }

        // nur wert zurückgeben wenn zeitstempel identisch ist
        // sonst

        return record;
    }

    /**
     * Reads the file line by line
     *
     * @param br
     * @return records on success, otherwise null
     */
    private List<Record> processFile(String filepath, Boolean nextFile) {

        List<Record> records = new ArrayList<Record>();
        String line = null;
        long currentPosition = 0;
        long rowSize;
        long firstTimestamp = 0;
        String firstValueLine = null;
        long actualTimestamp = 0;

        RandomAccessFile raf = getRandomAccessFile(filepath);
        if (raf == null) {
            return null;
        }
        try {
            int channelColumn = -1;
            while (channelColumn <= 0) {
                line = raf.readLine();
                channelColumn = LoggerUtils.getColumnNumberByName(line, channelId);
                unixTimestampColumn = LoggerUtils.getColumnNumberByName(line, LoggerUtils.TIMESTAMP_STRING);
            }

            firstValueLine = raf.readLine();
            rowSize = firstValueLine.length() + 1;
            currentPosition = raf.getFilePointer() - rowSize;
            firstTimestamp = (long) (Double.valueOf((firstValueLine.split(IESDataFormatUtils.SEPARATOR))[unixTimestampColumn]) * 1000);

            if (nextFile || startTimestamp < firstTimestamp) {
                startTimestamp = firstTimestamp;
            }

            if (startTimestamp >= firstTimestamp) {
                long filepos = getFilePosition(loggingInterval, startTimestamp, firstTimestamp, currentPosition, rowSize);
                raf.seek(filepos);
                actualTimestamp = startTimestamp;
                while ((line = raf.readLine()) != null && actualTimestamp <= endTimestamp) {
                    processLine(line, channelColumn, records);
                    actualTimestamp += loggingInterval;
                }
                raf.close();
            } else {
                records = null; // because the column of the channel was not identified
            }
        } catch (IOException e) {
            e.printStackTrace();
            records = null;
        }
        return records;
    }

    /**
     * Process the line: ignore comments, read records
     *
     * @param line
     */
    private void processLine(String line, int channelColumn, List<Record> records) {
        if (!line.startsWith(LoggerUtils.COMMENT_SIGN)) {
            readRecordFromLine(line, channelColumn, records);
        }
    }

    /**
     * Returns a RandomAccessFile of the specified file
     *
     * @param filepath
     * @return the RandomAccessFile of the specified file
     */
    private RandomAccessFile getRandomAccessFile(String filepath) {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(new File(filepath), "r");
        } catch (FileNotFoundException e) {

            logger.warn("Requested logfile: '" + filepath + "' not found.");
            // e.printStackTrace();
        }
        return raf;
    }

    /**
     * @param line   to read
     * @param column of the channelId
     * @return Record read from line
     */
    private void readRecordFromLine(String line, int channelColumn, List<Record> records) {
        String columnValue[] = line.split(IESDataFormatUtils.SEPARATOR);

        try {
            Double timestampS = Double.parseDouble(columnValue[unixTimestampColumn]);

            long timestampMS = ((Double) (timestampS * (1000))).longValue();

            if (isTimestampPartOfRequestedInterval(timestampMS)) {
                Record record = convertLogfileEntryToRecord(columnValue[channelColumn], timestampMS);
                records.add(record);
            }
        } catch (NumberFormatException e) {
            logger.debug("It's not a timestamp.");
            e.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if the timestamp read from file is part of the requested logging interval
     *
     * @param lineTimestamp
     * @return true if it is a part of the requested interval, if not false.
     */
    private boolean isTimestampPartOfRequestedInterval(long lineTimestamp) {
        boolean result = false;

        // TODO tidy up, move to better place, is asked each line!
        if (firstTimestampFromFile == -1) {
            firstTimestampFromFile = lineTimestamp;
        }

        if (lineTimestamp >= startTimestamp && lineTimestamp <= endTimestamp) {
            result = true;
        }
        return result;
    }

    /**
     * Get the position of the startTimestamp, without Header.
     *
     * @param loggingInterval
     * @param startTimestamp
     * @return the position of the start timestamp as long.
     */
    private long getFilePosition(int loggingInterval, long startTimestamp, long firstTimestamp, long firstValuePos, long rowSize) {

        long pos = 0;

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTimeInMillis(startTimestamp);

        pos = (calendar.getTimeInMillis() - firstTimestamp) / loggingInterval * rowSize + firstValuePos;

        // System.out.println("loggingInterval = " + loggingInterval + ", startTimestamp = " + startTimestamp
        // + ", firstTimestamp = " + firstTimestamp + ", firstValuePos = " + firstValuePos + ", rowSize = "
        // + rowSize + ", pos = " + pos);
        return pos;
    }

    // TODO support ints, booleans, ...

    /**
     * Converts an entry from the logfile into a record
     *
     * @param strValue
     * @param timestamp
     * @return the converted logfile entry.
     */
    private Record convertLogfileEntryToRecord(String strValue, long timestamp) {
        Record record = null;
        if (isNumber(strValue)) {
            record = new Record(new DoubleValue(Double.parseDouble(strValue)), timestamp, Flag.VALID);
        } else {
            // fehlerfall, wenn errors "errxx" geloggt wurden
            // record = new Record(null, timestamp, Flag);
            record = getRecordFromNonNumberValue(strValue, timestamp);

        }
        return record;
    }

    /**
     * Returns the record from a non number value read from the logfile. This is the case if the value is an error like
     * "e0" or a normal ByteArrayValue
     *
     * @param strValue
     * @param timestamp
     * @return the value in a record.
     */
    private Record getRecordFromNonNumberValue(String strValue, long timestamp) {
        Record record = null;

        if (strValue.trim().startsWith(IESDataFormatUtils.ERROR)) {
            int errorSize = IESDataFormatUtils.ERROR.length();
            String errorFlag = strValue.substring(errorSize, errorSize + 2);
            if (isNumber(errorFlag)) {
                record = new Record(null, timestamp, Flag.newFlag(Integer.parseInt(errorFlag.trim())));
            } else {
                record = new Record(null, timestamp, Flag.NO_VALUE_RECEIVED_YET);
            }
        } else if (strValue.trim().startsWith(IESDataFormatUtils.HEXADECIMAL)) {
            try {
                record = new Record(new ByteArrayValue(strValue.trim().getBytes("US-ASCII")), timestamp, Flag.VALID);
            } catch (UnsupportedEncodingException e) {
                record = new Record(Flag.UNKNOWN_ERROR);
                logger.error("Hexadecimal value is non US-ASCII decoded, value is: " + strValue.trim());
            }
        } else {
            record = new Record(new StringValue(strValue.trim()), timestamp, Flag.VALID);
        }
        return record;
    }

    // TODO this might be not the best solution

    /**
     * Checks if the string value is a number
     *
     * @param strValue
     * @return True on success, otherwise false
     */
    private boolean isNumber(String strValue) {
        boolean result = true;

        try {
            Double.parseDouble(strValue);
        } catch (NumberFormatException e) {
            result = false;
        }

        return result;
    }
}
