package cn.nukkit.network.protocol;

import cn.nukkit.Server;
import cn.nukkit.network.Network;
import cn.nukkit.utils.Binary;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.Zlib;
import com.nukkitx.network.raknet.RakNetReliability;

/**
 * @author MagicDroidX
 * Nukkit Project
 */
public abstract class DataPacket extends BinaryStream implements Cloneable {

    public int protocol = 999;

    public volatile boolean isEncoded = false;
    private int channel = Network.CHANNEL_NONE;

    public RakNetReliability reliability = RakNetReliability.RELIABLE_ORDERED;

    public abstract byte pid();

    public abstract void decode();

    public abstract void encode();

    @Override
    public DataPacket reset() {
        super.reset();
        if (protocol <= 274) {
            this.putByte(this.pid());
            this.putShort(0);
        } else {
            this.putUnsignedVarInt(this.pid() & 0xff);
        }
        return this;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getChannel() {
        return channel;
    }

    public DataPacket clean() {
        this.setBuffer(null);
        this.setOffset(0);
        this.isEncoded = false;
        return this;
    }

    @Override
    public DataPacket clone() {
        try {
            DataPacket packet = (DataPacket) super.clone();
            // prevent reflecting same buffer instance
            packet.setBuffer(this.count < 0 ? null : this.getBuffer());
            packet.offset = this.offset;
            packet.count = this.count;
            return packet;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public BatchPacket compress() {
        return compress(Server.getInstance().networkCompressionLevel);
    }

    public BatchPacket compress(int level) {
        BatchPacket batch = new BatchPacket();
        byte[][] batchPayload = new byte[2][];
        byte[] buf = getBuffer();
        batchPayload[0] = Binary.writeUnsignedVarInt(buf.length);
        batchPayload[1] = buf;
        byte[] data = Binary.appendBytes(batchPayload);
        try {
            if (protocol >= ProtocolInfo.v1_16_0) {
                batch.payload = Zlib.deflateRaw(data, level);
            } else {
                batch.payload = Zlib.deflate(data, level);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return batch;
    }

    public final void tryEncode() {
        if (!this.isEncoded) {
            this.isEncoded = true;
            this.encode();
        }
    }
}
