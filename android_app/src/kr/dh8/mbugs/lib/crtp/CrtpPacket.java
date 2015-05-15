package kr.dh8.mbugs.lib.crtp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

/**
 * Packet of data which can be sent/received from/to the Crazyflie. All packet
 * implementations must be immutable to avoid issues with modifying packets via
 * references, e.g. in a send queue.
 */
public abstract class CrtpPacket {

    /**
     * Byte order used when serializing/deserializing packets.
     */
    public static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * NULL packet. Header is 0xFF without any data.
     */
    public static final CrtpPacket NULL_PACKET = new CrtpPacket((byte) 0xff) {
        @Override
        protected void serializeData(ByteBuffer buffer) {
        }

        @Override
        protected int getDataByteCount() {
            return 0;
        }
    };

    private final byte mPacketHeader;
    private byte[] mSerializedPacket;

    /**
     * Create a new packet.
     * 
     * @param channel channel to set in the header.
     * @param port port to set in the header.
     */
    public CrtpPacket(int channel, int port) {
        this((byte) (((port & 0x0F) << 4) | (channel & 0x03)));
    }

    /**
     * Create a new packet.
     * 
     * @param packetHeader header of the packet.
     */
    public CrtpPacket(byte packetHeader) {
        this.mPacketHeader = packetHeader;
        this.mSerializedPacket = null;
    }

    /**
     * Get the header of the packet.
     * 
     * @return the header of the packet.
     */
    public byte getHeader() {
        return mPacketHeader;
    }

    /**
     * Serialize the data of the packet. Must not include the header.
     * 
     * @param buffer the target buffer for serialization.
     */
    protected abstract void serializeData(ByteBuffer buffer);

    /**
     * Get the number of bytes used when serializing the data.
     * 
     * @return number of bytes required by the serialized data.
     */
    protected abstract int getDataByteCount();

    /**
     * Convert the packet to a byte array suitable for transmission.
     * 
     * @return byte array containing the header and packet data.
     */
    
    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
      }
    
    private byte getCrc(byte[] buffer, int offset, int size) 
    {
    	int cksum = 0; 
    	for (int i = 0; i < size ; i++) {
    		cksum = (unsignedToBytes(buffer[offset + i])) + cksum;
    	}
    	cksum = cksum % 255;
//        Log.d("CRC: ", Integer.toString(cksum));
    	return (byte)(cksum);
    }
    
    public byte[] toByteArray() {
        // if it's the first call, serialize the packet and cache it
    	byte crc = 0;
        if (mSerializedPacket == null) {
            //ByteBuffer buffer = ByteBuffer.allocate(getDataByteCount() + 1).order(BYTE_ORDER);
            ByteBuffer buffer = ByteBuffer.allocate(getDataByteCount() + 5).order(BYTE_ORDER); // 0xaa, 0xaa, command, data_size, crc
            
            buffer.put(((byte) 0xaa));
            buffer.put(((byte) 0xaa));
            
            buffer.put(mPacketHeader);

            serializeData(buffer);
            
            crc = getCrc(buffer.array(), 2, getDataByteCount()+2);
            buffer.put(crc);
            
            mSerializedPacket = buffer.array();
        }

        return mSerializedPacket;
    }
}
