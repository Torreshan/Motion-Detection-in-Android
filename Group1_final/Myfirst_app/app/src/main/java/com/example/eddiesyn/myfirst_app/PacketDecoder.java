package com.example.eddiesyn.myfirst_app;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.os.AsyncTask;
import android.util.Log;

import com.example.eddiesyn.myfirst_app.DataProvider.OnDataAvailableListener;

/**
 * This class is used to cut a FIFO of raw bytes into frames, that become {@link Packet}s. Instantiate only one object of this
 * class.
 * <p>
 * Use this class like this:
 * <ol>
 * <li>Implement the {@link OnPacketDecodedListener} interface methods.
 * <li>Instantiate one object of this class, and keep a reference to it in your class.
 * <li>Register as a listener to the object using {@link #registerListener(OnPacketDecodedListener)}.
 * <li>When you received bytes that you want to decode (for example, from a bluetooth connection), pass them to the
 * PacketDecoder object using the {@link #addBytesToQueue(ArrayList)} method.
 * <li>To actually start decoding these bytes (it happens in a parallel thread to keep you UI responsive), call
 * {@link #triggerDecoding()}.
 * <li>When the decoding is done, you will get the resulting Packets via the
 * {@link OnPacketDecodedListener#onNewPacketDecoded(Packet)} callback method.
 * </ol>
 *
 * @author laurenz
 *
 */
public class PacketDecoder
{
    /** Debug Log Tag */
    private final static String TAG_PACKETDECODER = "PacketDecoder";

    // Bluetooth sensor decoding stuff
    private ArrayList<Byte> incomingBytes = new ArrayList<Byte>();
    private ArrayList<Packet> incomingPackets = new ArrayList<Packet>();
    private boolean isDecodingBusy = false;
    private ArrayList<Byte> incomingBytesToBeAdded = new ArrayList<Byte>();

    /**
     * Constructor for PacketDecoder. Nothing happens here.
     */
    public PacketDecoder()
    {
    }

    private ArrayList<OnPacketDecodedListener> listOfRegisteredListeners = new ArrayList<OnPacketDecodedListener>();

    /**
     * Register as a listener to receive Packets and errors via the {@link OnPacketDecodedListener} interface.
     *
     * @param l
     *            the listener to register
     */
    public boolean registerListener(OnPacketDecodedListener l)
    {
        return listOfRegisteredListeners.add(l);
    }

    /**
     * Stop calling the functions of the {@link OnDataAvailableListener} interface for the given listener.
     *
     */
    public boolean unregisterListener(OnPacketDecodedListener l)
    {
        return listOfRegisteredListeners.remove(l);
    }

    private void notifyListeners(Packet newPacket)
    {
        for (OnPacketDecodedListener l : listOfRegisteredListeners)
        {
            l.onNewPacketDecoded(newPacket);
        }
    }

    /**
     * Classes that implement this interface and register as a listener (see
     * {@link PacketDecoder#registerListener(OnPacketDecodedListener)}) will be able to receive {@link Packet} objects and
     * possible decoding errors with this method.
     */
    public static interface OnPacketDecodedListener
    {
        /**
         * This method gets called whenever a Packet was successfully decoded from the incoming FIFO
         *
         * @param newPacket
         *            The decoded {@link Packet}
         */
        void onNewPacketDecoded(Packet newPacket);

        /**
         * If an error occured while decoding a Packet, you will get notified via this method. Check the {@link Packet} to see
         * what is wrong.
         *
         * @param invalidPacket
         *            The Packet with errors
         */
        void onNewPacketError(Packet invalidPacket);
    }

    /**
     * Add some bytes at the end of the decoding queue.
     *
     * @param in
     *            An ArrayList&lt;Byte&gt; of the bytes you want decoded
     */
    public void addBytesToQueue(ArrayList<Byte> in)
    {
        synchronized (incomingBytesToBeAdded)
        {
            incomingBytesToBeAdded.addAll(in);
        }
        // Log.v(MainActivity.TAG_PROTOCOL_DECODE, "AddByteQueue now has " + incomingBytesToBeAdded.size() + "B, ByteQueue has "
        // + incomingBytes.size() + "B (" + ((!arrayModifiedStatus) ? "not " : "") + "transferred)");
        // triggerDecoding();
    }

    /**
     * This method starts decoding a copy of the incoming queue in a parallel thread. If there is already a decoding thread, a
     * second one won't be started, so make sure you call this method at least every time new data is available in the incoming
     * queue.
     */
    public void triggerDecoding()
    {
        if (!isDecodingBusy)
        {
            isDecodingBusy = true;
            // decodes frames in the incomingBytes queue and also prints them
            new Decoder().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, null, null, null);
        } else
        {
            // Log.d(TAG_PACKETDECODER, "decode trigger overrun");
        }
    }

    private class Decoder extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... in)
        {
            do
            {
                // move received bytes to processing queue
                synchronized (incomingBytesToBeAdded)
                {
                    incomingBytes.addAll(new ArrayList<Byte>(incomingBytesToBeAdded));
                    incomingBytesToBeAdded.clear();
                }
                // Log.v(MainActivity.TAG_PROTOCOL_DECODE, "AddByteQueue now has " + incomingBytesToBeAdded.size()
                // + "B, ByteQueue has " + incomingBytes.size() + "B (" + ((!arrayModifiedStatus) ? "not " : "")
                // + "transferred)");

                // get Packets from the byte queue
                ArrayList<Packet> newPackets = decode(incomingBytes);
                incomingPackets.addAll(newPackets);
                int numOfAddedPackets = newPackets.size();

                // print received packages to textview
                ListIterator<Packet> li = incomingPackets.listIterator(incomingPackets.size() - numOfAddedPackets);
                while (li.hasNext())
                    notifyListeners(li.next());

                // we don't actually need the package anymore (atm) so free some memory
                incomingPackets.clear();

            } while (incomingBytesToBeAdded.size() > 0);

            isDecodingBusy = false;

            return null;
        }

        private synchronized ArrayList<Packet> decode(ArrayList<Byte> incomingBytes)
        {

            ArrayList<Packet> newPackets = new ArrayList<Packet>();
            ArrayList<Integer> flagPositions;

            // first, get flag element positions in incomingBytes
            flagPositions = getFlagPositions(incomingBytes);

            // Log.d(MainActivity.TAG_PROTOCOL_DECODE,
            // "Found FlagPositions: "+flagPositions.toString()+" in Array of "+incomingBytes.size()+"B");

            // if there is no start flag: we can't recover that frame, so delete the data.
            if (flagPositions.size() == 0)
            {
                Log.d(TAG_PACKETDECODER, "No start flag found - deleting " + incomingBytes.size() + " Bytes");
                incomingBytes.clear();
                return newPackets;
            }

            // if there is data before the first flag: we can't recover that frame, so delete the data.
            if (flagPositions.get(0) > 0)
            {
                Log.d(TAG_PACKETDECODER, "start flag not at first position - deleting first " + flagPositions.get(0) + " Bytes");
                incomingBytes.subList(0, flagPositions.get(0)).clear();

                // flagPositions change when deleting data
                flagPositions = getFlagPositions(incomingBytes);
            }

            // when get here, the start flag is at the first position of incomingBytes.

            // take all completely received frames over to decoding. (if there is none, just wait for the next call)
            while (flagPositions.size() >= 2)
            {
                int frameLength = flagPositions.get(1) - 1 - flagPositions.get(0);

                // sometimes the frame is too long?
                if (frameLength > 500)
                {
                    Log.d(TAG_PACKETDECODER, "deleted a too long (" + frameLength + ") frame");
                    incomingBytes.subList(flagPositions.get(0), flagPositions.get(1) + 1).clear();

                    // flagPositions change when deleting data
                    flagPositions = getFlagPositions(incomingBytes);
                }
                // ensure the frame is not just two sequential frame flags
                else if (frameLength == 0)
                {
                    // remove the first of two sequential flags at the beginning
                    Log.d(TAG_PACKETDECODER, "deleted the first of two sequential frame flags");
                    incomingBytes.remove(0);

                    // flagPositions change when deleting data
                    flagPositions = getFlagPositions(incomingBytes);
                }
                // this one seems to be good
                else
                {
                    // Log.d(MainActivity.TAG_PROTOCOL_DECODE, "oneFrameInIncomingBytes is at [" + flagPositions.get(0) + ", "
                    // + flagPositions.get(1) + "] in Array of " + incomingBytes.size() + "B");

                    // the elements in oneFrameInIncomingBytes are references to the objects in incomingBytes
                    List<Byte> oneFrameInIncomingBytes;
                    oneFrameInIncomingBytes = incomingBytes.subList(flagPositions.get(0), flagPositions.get(1) + 1);

                    // newFrame is a shallow copy of oneFrameInIncominBytes
                    ArrayList<Byte> newFrame = new ArrayList<Byte>(oneFrameInIncomingBytes);
                    Packet newPacket = new Packet(newFrame);

                    if (newPacket.isValid())
                    {
                        // If the new packet is ok, add it to the list of received packets
                        newPackets.add(newPacket);
                        // Log.d(MainActivity.TAG_PROTOCOL_DECODE, "added packet (" + frameLength + "B)");

                    } else
                    {
                        // We have a corrupt packet, so delete it
                        Log.d(TAG_PACKETDECODER, String.format("packet (" + oneFrameInIncomingBytes.size()
                                + "B) not added (ckecksum 0x%02X)", newPacket.mChecksumCalc));

                        for (OnPacketDecodedListener l : listOfRegisteredListeners)
                        {
                            l.onNewPacketError(newPacket);
                        }

                        newPacket = null; // free resources
                    }

                    // delete the frame in the incoming queue
                    oneFrameInIncomingBytes.clear();

                    // flagPositions change when deleting data
                    flagPositions = getFlagPositions(incomingBytes);

                }
            }

            return newPackets;
        }

        private synchronized ArrayList<Integer> getFlagPositions(ArrayList<Byte> arrayToBeAnalyzed)
        {
            ArrayList<Integer> flagPositions = new ArrayList<Integer>();
            ListIterator<Byte> lI = arrayToBeAnalyzed.listIterator();
            while (lI.hasNext())
            {
                Byte b = lI.next();
                int i = lI.previousIndex();
                if (b != null)
                    if (b.equals(Packet.PACKET_FLAGBYTE))
                        flagPositions.add(i);
            }
            return flagPositions;
        }
    }

}