package com.example.eddiesyn.myfirst_app;


import java.util.ArrayList;

/**
 * This class is for sending 'Control command packages' to the bluetooth sensor. In the constructor, give the relevant
 * information, and retrieve the encoded frame bytes with the {@link #getFrameBytesArrayList()} method.
 * <p>
 * Mit der SensorID 0xFF können Daten von der steuernden Einheit (Handy, PC, etc.) an den MagicCube geschickt werden. Das
 * Container­Format ist das gleiche wie beim Übertragen der Messwerte, nur dass als SensorID eine 0xFF zur Kennzeichnung von
 * Kommandos verwendet wird. Im Nutzdatenbereich wird das Kommando an sich übertragen (entspricht IMMER dem 1. Byte der
 * Nutzdaten). Aber auch diese Kommandos können Informationen benötigen, die direkt im Anschluss übertragen werden (siehe
 * Kommando 0x6d und die dazugehörige Tabelle 5)!
 *
 * @author laurenz
 *
 */
public class ControlPacket
{
    /**
     * Sende alle auf der SD-­Karte gespeicherten Daten über Bluetooth
     */
    public static final byte INSTRUCTION_SEND_SD = (byte) 0x74;
    /**
     * Starte/Stoppe Messung und speichere Messdaten auf der SD­Karte (keine BT­Verbindung während der Messung notwendig)
     */
    public static final byte INSTRUCTION_START_STOP_MEASURE = (byte) 0x73;
    /**
     * Starte/Stoppe Messung, speichere Messdaten auf SD­Karte UND sende sie über Bluetooth
     */
    public static final byte INSTRUCTION_START_STOP_MEASURE_SEND_BT = (byte) 0x62;
    /**
     * Stoppe jegliche Messung und lege Sensor schlafen
     */
    public static final byte INSTRUCTION_SLEEP = (byte) 0x78;
    /**
     * Lösche SD­Karte
     */
    public static final byte INSTRUCTION_ERASE_SD = (byte) 0x65;
    /**
     * Monitoring­Funktion mit Übergabeparameter
     */
    public static final byte INSTRUCTION_MONITOR = (byte) 0x6D;
    /** No instruction chosen yet */
    private static final byte INSTRUCTION_NONE = 0;

    /**
     * Positiver Nulldurchgang (PZC) ­1 ­> +1
     */
    public static final byte METHOD_PZC = (byte) 0x01;
    /**
     * Negativer Nulldurchgang (NZC) +1 ­> ­1
     */
    public static final byte METHOD_NZC = (byte) 0x02;
    /**
     * Allg. Nulldurchgang (ZC) ­1 ­> +1 & +1 ­> ­1
     */
    public static final byte METHOD_ZC = (byte) 0x03;
    /**
     * Doppelschrittbetrachtung (DSZC)
     */
    public static final byte METHOD_DSZC = (byte) 0x04;
    /**
     * Fläche (Integral) im positiven Signalbereich (PINT)
     */
    public static final byte METHOD_PINT = (byte) 0x05;
    /** No method chosen yet */
    public static final byte METHOD_NINT = 0;
    /**
     * Fläche (Integral) bei Doppelschrittbetrachtung (DSINT)
     */
    public static final byte METHOD_DSINT = (byte) 0x07;
    private static final byte METHOD_NONE = (byte) 0x07;

    private static final byte SENSOR_ID_CONTROL = (byte) 0xFF;

    private static final byte BYTE_START = (byte) 0xA8;
    private static final byte BYTE_CONTROL = (byte) 0xA9;
    private static final byte BYTE_STUFF = (byte) 0xDF;

    private byte frameCounter = (byte) 0x00;
    private byte sensorCounter = (byte) 0x00;
    private byte checksum = (byte) 0x00;
    private int days = 0, millis = 0, framesize;
    private byte instruction = INSTRUCTION_NONE;
    private byte method = METHOD_NONE;
    private int setpoint = 0;
    private int maxdeviation = 0;

    /** this ArrayList holds the data bytes, including frame start/end flags */
    private ArrayList<Byte> frame = new ArrayList<Byte>(20);

    /**
     * Constructor for a control command packet. Don't forget to call {@link #build()}.
     *
     * @param instruction
     *            the instruction to send to the sensor
     * @see #INSTRUCTION_ERASE_SD
     * @see #INSTRUCTION_MONITOR
     * @see #INSTRUCTION_SEND_SD
     * @see #INSTRUCTION_SLEEP
     * @see #INSTRUCTION_START_STOP_MEASURE
     * @see #INSTRUCTION_START_STOP_MEASURE_SEND_BT
     */
    public ControlPacket(byte instruction)
    {
        this.instruction = instruction;
    }

    /**
     * Constructor for a control command packet. This one is thought for the {@link #INSTRUCTION_MONITOR} instruction, but it's
     * ok to use it with the other instructions, just pass something in the other params. Don't forget to call {@link #build()}.
     *
     * @param instruction
     *            the instruction to send to the sensor
     * @param method
     *            method to use for step recognition
     * @param setpoint
     *            setpoint for the monitoring function
     * @param maxdeviation
     *            maximum deviaton from the setpoint
     * @see #INSTRUCTION_ERASE_SD
     * @see #INSTRUCTION_MONITOR
     * @see #INSTRUCTION_SEND_SD
     * @see #INSTRUCTION_SLEEP
     * @see #INSTRUCTION_START_STOP_MEASURE
     * @see #INSTRUCTION_START_STOP_MEASURE_SEND_BT
     * @see #METHOD_DSINT
     * @see #METHOD_DSZC
     * @see #METHOD_NINT
     * @see #METHOD_NZC
     * @see #METHOD_PINT
     * @see #METHOD_PZC
     * @see #METHOD_ZC
     */
    public ControlPacket(byte instruction, byte method, int setpoint, int maxdeviation)
    {
        this.instruction = instruction;
        this.method = method;
        this.setpoint = setpoint;
        this.maxdeviation = maxdeviation;
    }

    /**
     * Sets the ControlPackets Timestamp
     *
     * @param days
     *            days since reset
     * @param millis
     *            milliseconds in current day
     * @return <code>this</code> for method chains
     */
    public ControlPacket setTimestamp(byte days, int millis)
    {
        this.days = days;
        this.millis = millis;
        return this;
    }

    /**
     * Builds the ControlPacket's frame bytes from the given information
     *
     * @return <code>this</code> for method chains
     */
    public ControlPacket build()
    {
        // build basic frame
        ArrayList<Byte> tempFrame = new ArrayList<Byte>();
        tempFrame.add(frameCounter);
        tempFrame.add(SENSOR_ID_CONTROL);
        tempFrame.add(sensorCounter);
        tempFrame.add(getByte(days, 0));
        tempFrame.add(getByte(days, 1));
        tempFrame.add(getByte(millis, 0));
        tempFrame.add(getByte(millis, 1));
        tempFrame.add(getByte(millis, 2));
        tempFrame.add(getByte(millis, 3));
        tempFrame.add(getByte(framesize, 0)); // placeholder
        tempFrame.add(getByte(framesize, 1)); // placeholder
        tempFrame.add(instruction);
        if (instruction == INSTRUCTION_MONITOR)
        {
            tempFrame.add(method);
            tempFrame.add(getByte(setpoint, 0));
            tempFrame.add(getByte(setpoint, 1));
            tempFrame.add(getByte(maxdeviation, 0));
            tempFrame.add(getByte(maxdeviation, 1));
        }
        tempFrame.add(checksum); // placeholder

        // change size
        framesize = tempFrame.size();
        tempFrame.set(9, getByte(framesize, 0));
        tempFrame.set(10, getByte(framesize, 1));

        // change checksum
        for (Byte b : tempFrame)
        {
            checksum = (byte) (checksum ^ b.byteValue());
        }
        tempFrame.set(tempFrame.size() - 1, checksum);

        // start actual frame with start byte
        frame.add(BYTE_START);

        // stuffing and copying to frame
        for (Byte b : tempFrame)
        {
            frame.add(destuff(b));
        }

        // end frame
        frame.add(BYTE_START);

        // return this for method chaining
        return this;
    }

    /**
     * Returns the fully encoded control packet frame bytes (including start/end flags).
     *
     * @return the frame bytes in an ArrayList
     */
    public ArrayList<Byte> getFrameBytesAsArrayList()
    {
        return frame;
    }

    /**
     * Returns the fully encoded control packet frame bytes (including start/end flags).
     *
     * @return the frame bytes in a primitive byte array
     */
    public byte[] getFrameBytesAsByteArray()
    {
        byte[] frameBytes = new byte[frame.size()];
        for (int i = 0; i < frame.size(); i++)
            frameBytes[i] = frame.get(i);
        return frameBytes;
    }

    /**
     * Sends the ControlPacket to a remote device connected via bluetooth
     *
     * @param bluetoothConnectionService
     *            The BT connection to use
     * @return <code>true</code> if write successful, <code>false</code> otherwise.
     */
    public boolean send(BluetoothConnectionService bluetoothConnectionService)
    {
        return bluetoothConnectionService.write(getFrameBytesAsByteArray());
    }

    private byte destuff(byte in)
    {
        if (in == BYTE_CONTROL)
        {
            in = (byte) (BYTE_STUFF & in);
        }
        return in;
    }

    private byte getByte(int in, int pos)
    {
        return (byte) (in & (0xFF << (pos * 8)));
    }
}
