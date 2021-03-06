package mcjty.rftools.blocks.teleporter;

import io.netty.buffer.ByteBuf;
import mcjty.lib.network.PacketListFromServer;

import java.util.List;

public class PacketReceiversReady extends PacketListFromServer<PacketReceiversReady,TeleportDestinationClientInfo> {

    public PacketReceiversReady() {
    }

    public PacketReceiversReady(int x, int y, int z, String command, List<TeleportDestinationClientInfo> list) {
        super(x, y, z, command, list);
    }

    @Override
    protected TeleportDestinationClientInfo createItem(ByteBuf buf) {
        return new TeleportDestinationClientInfo(buf);
    }
}
