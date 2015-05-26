package io.subak.connect.crtp;

/**
 * Created by jeyong on 5/26/15.
 */
import java.nio.ByteBuffer;

/**
 * Packet used for sending control set-points for the roll/pitch/yaw/thrust
 * regulators.
 */
public class CommanderPacket extends CrtpPacket {
    private final float mRoll;
    private final float mPitch;
    private final float mYaw;
    private final char mThrust;

    /**
     * Create a new commander packet.
     *
     * @param roll (Deg.)
     * @param pitch (Deg.)
     * @param yaw (Deg./s)
     * @param thrust (0-65535)
     * @param xmode
     */
    public CommanderPacket(float roll, float pitch, float yaw, char thrust, boolean xmode) {
        super((byte) 0x30);

        if (xmode) {
            this.mPitch = 0.707f * (roll + pitch);
            this.mRoll = 0.707f * (roll - pitch);
        } else {
            this.mPitch = pitch;
            this.mRoll = roll;
        }
        this.mYaw = yaw;
        this.mThrust = thrust;
    }

    @Override
    protected void serializeData(ByteBuffer buffer) {
        buffer.put((byte) getDataByteCount());

        buffer.putFloat(mRoll);
        buffer.putFloat(mPitch);
        buffer.putFloat(mYaw);
        buffer.putChar(mThrust);
    }

    @Override
    protected int getDataByteCount() {
        return 3 * 4 + 1 * 2; // 3 floats with size 4, 1 char (= uint16_t) with
        // size 2
    }
}
