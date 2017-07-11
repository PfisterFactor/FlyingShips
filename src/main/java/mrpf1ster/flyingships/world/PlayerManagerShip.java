package mrpf1ster.flyingships.world;

import com.google.common.collect.Lists;
import com.unascribed.lambdanetwork.PendingPacket;
import mrpf1ster.flyingships.FlyingShips;
import mrpf1ster.flyingships.entities.EntityShip;
import mrpf1ster.flyingships.network.PacketSender;
import mrpf1ster.flyingships.util.BlockUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import scala.collection.Iterator;

import java.util.Arrays;
import java.util.List;

// The entire PlayerManager class copied here, so we can use it for our ship worlds
public class PlayerManagerShip {
    private final ShipWorldServer theShipWorldServer;
    private final LongHashMap<PlayerInstance> playerInstances = new LongHashMap();
    private final List<PlayerInstance> playerInstancesToUpdate = Lists.newArrayList();
    private final List<PlayerInstance> playerInstanceList = Lists.newArrayList();
    /**
     * time what is using to check if InhabitedTime should be calculated
     */
    private long previousTotalWorldTime;
    /**
     * x, z direction vectors: east, south, west, north
     */
    private final int[][] xzDirectionsConst = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    public PlayerManagerShip(ShipWorldServer shipWorldServer) {
        theShipWorldServer = shipWorldServer;
        Iterator<ChunkCoordIntPair> iter = theShipWorldServer.ChunksOnShip().iterator();
        while (iter.hasNext()) {
            ChunkCoordIntPair coord = iter.next();
            getPlayerInstance(coord.chunkXPos, coord.chunkZPos, true);
        }
        System.out.println();
    }

    public List<EntityPlayer> getPlayers() {
        return theShipWorldServer.OriginWorld().playerEntities;
    }
    /**
     * Returns the ShipWorldServer associated with this PlayerManager
     */
    public ShipWorldServer getShipWorldServer() {
        return theShipWorldServer;
    }

    public void onWorldChunkLoad(int chunkX, int chunkZ) {
        ChunkCoordIntPair relChunk = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, theShipWorldServer.OriginPos());
        getPlayerInstance(relChunk.chunkXPos, relChunk.chunkZPos, true);

    }

    public void onWorldChunkUnload(int chunkX, int chunkZ) {
        ChunkCoordIntPair relChunk = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, theShipWorldServer.OriginPos());
        PlayerInstance toBeRemoved = getPlayerInstance(relChunk.chunkXPos, relChunk.chunkZPos, false);
        if (toBeRemoved != null) {
            long i = (long) relChunk.chunkXPos + 2147483647L | (long) relChunk.chunkZPos + 2147483647L << 32;
            playerInstances.remove(i);
            playerInstanceList.remove(toBeRemoved);
            if (toBeRemoved.numBlocksToUpdate > 0)
                playerInstancesToUpdate.remove(toBeRemoved);
            theShipWorldServer.getChunkProviderServer().dropChunk(relChunk.chunkXPos, relChunk.chunkZPos);
        }
    }
    /**
     * updates all the player instances that need to be updated
     */

    public void updatePlayerInstances() {
        long i = this.theShipWorldServer.getTotalWorldTime();
        if (i - this.previousTotalWorldTime > 8000L) {
            this.previousTotalWorldTime = i;
            for (int j = 0; j < this.playerInstanceList.size(); ++j) {
                PlayerInstance playermanager$playerinstance = this.playerInstanceList.get(j);
                playermanager$playerinstance.onUpdate();
                playermanager$playerinstance.processChunk();
            }
        } else {
            for (int k = 0; k < this.playerInstancesToUpdate.size(); ++k) {
                PlayerInstance playermanager$playerinstance1 = this.playerInstancesToUpdate.get(k);
                playermanager$playerinstance1.onUpdate();
            }
        }

        playerInstancesToUpdate.clear();

        if (getPlayers().isEmpty()) {
            theShipWorldServer.getChunkProviderServer().unloadAllChunks();
        }
    }

    public boolean hasPlayerInstance(int chunkX, int chunkZ) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        return this.playerInstances.getValueByKey(i) != null;
    }

    /**
     * pass in the chunk x and y and a flag as to whether or not the instance should be made if it doesn't exist
     */
    private PlayerInstance getPlayerInstance(int chunkX, int chunkZ, boolean createIfAbsent) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        PlayerInstance playermanager$playerinstance = this.playerInstances.getValueByKey(i);

        if (playermanager$playerinstance == null && createIfAbsent && theShipWorldServer.ChunksOnShip().contains(new ChunkCoordIntPair(chunkX, chunkZ))) {
            playermanager$playerinstance = new PlayerInstance(chunkX, chunkZ);
            this.playerInstances.add(i, playermanager$playerinstance);
            this.playerInstanceList.add(playermanager$playerinstance);
        }

        return playermanager$playerinstance;
    }

    public void markBlockForUpdate(BlockPos pos) {
        int i = pos.getX() >> 4;
        int j = pos.getZ() >> 4;
        PlayerInstance playermanager$playerinstance = this.getPlayerInstance(i, j, false);

        if (playermanager$playerinstance != null) {
            playermanager$playerinstance.flagChunkForUpdate(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        }
    }




    /**
     * Determine if two rectangles centered at the given points overlap for the provided radius. Arguments: x1, z1, x2,
     * z2, radius.
     */
    private boolean overlaps(int x1, int z1, int x2, int z2, int radius) {
        int i = x1 - x2;
        int j = z1 - z2;
        return i >= -radius && i <= radius && j >= -radius && j <= radius;
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int chunkX, int chunkZ) {
        ChunkCoordIntPair worldChunk = BlockUtils.getWorldChunkFromRelative(chunkX, chunkZ, theShipWorldServer.OriginPos());
        return ((WorldServer) theShipWorldServer.OriginWorld()).getPlayerManager().isPlayerWatchingChunk(player, worldChunk.chunkXPos, worldChunk.chunkZPos);
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, ChunkCoordIntPair chunkCoords) {
        return isPlayerWatchingChunk(player, chunkCoords.chunkXPos, chunkCoords.chunkZPos);
    }

    public boolean isPlayerWatchingPos(EntityPlayerMP player, BlockPos pos) {
        return isPlayerWatchingChunk(player, pos.getX() >> 4, pos.getZ() >> 4);
    }


    class PlayerInstance {
        /**
         * The chunk coordinates
         */
        private final ChunkCoordIntPair chunkCoords;
        private short[] locationOfBlockChange = new short[64];
        /**
         * the number of blocks that need to be updated next tick
         */
        private int numBlocksToUpdate;
        /**
         * Integer field where each bit means to make update 16x16x16 division of chunk (from bottom).
         */
        private int flagsYAreasToUpdate;
        /**
         * time what is using when chunk InhabitedTime is being calculated
         */
        private long previousWorldTime;

        public PlayerInstance(int chunkX, int chunkZ) {
            chunkCoords = new ChunkCoordIntPair(chunkX, chunkZ);
            getShipWorldServer().getChunkProviderServer().provideChunk(chunkCoords.chunkXPos, chunkCoords.chunkZPos);
        }

        /**
         * This method currently only increases chunk inhabited time. Extension is possible in next versions
         */
        public void processChunk() {
            this.increaseInhabitedTime(theShipWorldServer.getChunkFromChunkCoords(this.chunkCoords.chunkXPos, this.chunkCoords.chunkZPos));
        }

        /**
         * Increases chunk inhabited time every 8000 ticks
         */
        private void increaseInhabitedTime(Chunk theChunk) {
            theChunk.setInhabitedTime(theChunk.getInhabitedTime() + theShipWorldServer.getTotalWorldTime() - this.previousWorldTime);
            this.previousWorldTime = theShipWorldServer.getTotalWorldTime();
        }

        public void flagChunkForUpdate(int x, int y, int z) {
            if (this.numBlocksToUpdate == 0) {
                PlayerManagerShip.this.playerInstancesToUpdate.add(this);
            }

            this.flagsYAreasToUpdate |= 1 << (y >> 4);

            //Forge; Cache everything, so always run
            short short1 = (short) (x << 12 | z << 8 | y);

            for (int i = 0; i < numBlocksToUpdate; ++i) {
                if (locationOfBlockChange[i] == short1) {
                        return;
                    }
                }

            if (this.numBlocksToUpdate == this.locationOfBlockChange.length) {
                this.locationOfBlockChange = Arrays.copyOf(this.locationOfBlockChange, this.locationOfBlockChange.length << 1);
                }
            locationOfBlockChange[numBlocksToUpdate++] = short1;
        }

        public void sendToAllPlayersWatchingChunk(PendingPacket packet) {
            for (EntityPlayer player : PlayerManagerShip.this.theShipWorldServer.OriginWorld().playerEntities) {
                if (PlayerManagerShip.this.isPlayerWatchingChunk((EntityPlayerMP) player, chunkCoords)) {
                    packet.to(player);
                }
            }
        }
        public void sendToAllPlayersWatchingChunk(IMessage packet) {
            for (EntityPlayer player : PlayerManagerShip.this.theShipWorldServer.OriginWorld().playerEntities) {
                if (PlayerManagerShip.this.isPlayerWatchingChunk((EntityPlayerMP) player, chunkCoords)) {
                    FlyingShips.flyingShipPacketHandler().INSTANCE().sendTo(packet, (EntityPlayerMP) player);
                }
            }
        }

        @SuppressWarnings("unused")
        public void onUpdate() {
            if (numBlocksToUpdate != 0) {
                if (numBlocksToUpdate == 1) {
                    int i = (locationOfBlockChange[0] >> 12 & 15) + chunkCoords.chunkXPos * 16;
                    int j = locationOfBlockChange[0] & 255;
                    int k = (locationOfBlockChange[0] >> 8 & 15) + chunkCoords.chunkZPos * 16;
                    BlockPos blockpos = new BlockPos(i, j, k);


                    EntityShip ship = theShipWorldServer.Ship();

                    sendToAllPlayersWatchingChunk(PacketSender.sendBlockChangedPacket(ship.Shipworld(),blockpos));



                    if (PlayerManagerShip.this.theShipWorldServer.getBlockState(blockpos).getBlock().hasTileEntity(PlayerManagerShip.this.theShipWorldServer.getBlockState(blockpos))) {
                        sendTileToAllPlayersWatchingChunk(PlayerManagerShip.this.theShipWorldServer.getTileEntity(blockpos));
                    }
                } else if (numBlocksToUpdate >= ForgeModContainer.clumpingThreshold) {
                    int i1 = chunkCoords.chunkXPos * 16;
                    int k1 = chunkCoords.chunkZPos * 16;
                    //ChunkDataMessage message = new ChunkDataMessage(PlayerManagerShip.this.getShipWorldServer().Ship().ShipID(), PlayerManagerShip.this.getShipWorldServer().getChunkFromChunkCoords(chunkCoords.chunkXPos, chunkCoords.chunkZPos), false, flagsYAreasToUpdate);
                    PendingPacket p = PacketSender.sendChunkDataPacket(PlayerManagerShip.this.getShipWorldServer().Ship().ShipID(),PlayerManagerShip.this.getShipWorldServer().getChunkFromChunkCoords(chunkCoords.chunkXPos, chunkCoords.chunkZPos), false, flagsYAreasToUpdate);
                    sendToAllPlayersWatchingChunk(p);

                    // Forge: Grabs ALL tile entities is costly on a modded server, only send needed ones
                    for (int i2 = 0; false && i2 < 16; ++i2) {
                        if ((flagsYAreasToUpdate & 1 << i2) != 0) {
                            int k2 = i2 << 4;
                            List<TileEntity> list = PlayerManagerShip.this.getShipWorldServer().getTileEntitiesIn(i1, k2, k1, i1 + 16, k2 + 16, k1 + 16);

                            for (int l = 0; l < list.size(); ++l) {
                                sendTileToAllPlayersWatchingChunk(list.get(l));
                            }
                        }
                    }
                } else {
                    PendingPacket p = PacketSender.sendMultipleBlocksChangedPacket(PlayerManagerShip.this.theShipWorldServer.Ship().ShipID(), this.numBlocksToUpdate, this.locationOfBlockChange, PlayerManagerShip.this.theShipWorldServer.getChunkFromChunkCoords(this.chunkCoords.chunkXPos, this.chunkCoords.chunkZPos));
                    sendToAllPlayersWatchingChunk(p);
                }
                // Forge: Send only the tile entities that are updated, Adding this brace lets us keep the indent and the patch small
                ShipWorldServer world = theShipWorldServer;
                for (int j1 = 0; j1 < this.numBlocksToUpdate; ++j1) {
                    int l1 = (this.locationOfBlockChange[j1] >> 12 & 15) + this.chunkCoords.chunkXPos * 16;
                    int j2 = this.locationOfBlockChange[j1] & 255;
                    int l2 = (this.locationOfBlockChange[j1] >> 8 & 15) + this.chunkCoords.chunkZPos * 16;
                    BlockPos blockpos1 = new BlockPos(l1, j2, l2);

                    if (world.getBlockState(blockpos1).getBlock().hasTileEntity(world.getBlockState(blockpos1))) {
                        this.sendTileToAllPlayersWatchingChunk(theShipWorldServer.getTileEntity(blockpos1));
                    }
                }

                this.numBlocksToUpdate = 0;
                this.flagsYAreasToUpdate = 0;
            }
        }

        private void sendTileToAllPlayersWatchingChunk(TileEntity theTileEntity) {
            if (theTileEntity != null) {
                Packet packet = theTileEntity.getDescriptionPacket();
                if (packet != null) {
                    PendingPacket p = PacketSender.sendUpdateTileEntityPacket(getShipWorldServer().Ship().ShipID(),(S35PacketUpdateTileEntity) packet);
                    //UpdateTileEntityMessage message = new UpdateTileEntityMessage(getShipWorldServer().Ship().ShipID(), (S35PacketUpdateTileEntity) packet);
                    this.sendToAllPlayersWatchingChunk(p);
                }
            }
        }
    }
}