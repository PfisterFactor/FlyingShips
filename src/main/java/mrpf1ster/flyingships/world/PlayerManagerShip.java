package mrpf1ster.flyingships.world;

import com.google.common.collect.Lists;
import mrpf1ster.flyingships.FlyingShips;
import mrpf1ster.flyingships.network.BlockChangedMessage;
import mrpf1ster.flyingships.network.ChunkDataMessage;
import mrpf1ster.flyingships.network.MultipleBlocksChangedMessage;
import mrpf1ster.flyingships.network.UpdateTileEntityMessage;
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
    private final LongHashMap<PlayerManagerShip.PlayerInstance> playerInstances = new LongHashMap();
    private final List<PlayerManagerShip.PlayerInstance> playerInstancesToUpdate = Lists.newArrayList();
    private final List<PlayerManagerShip.PlayerInstance> playerInstanceList = Lists.newArrayList();
    /**
     * time what is using to check if InhabitedTime should be calculated
     */
    private long previousTotalWorldTime;
    /**
     * x, z direction vectors: east, south, west, north
     */
    private final int[][] xzDirectionsConst = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};

    public PlayerManagerShip(ShipWorldServer shipWorldServer) {
        this.theShipWorldServer = shipWorldServer;
        Iterator<ChunkCoordIntPair> iter = this.theShipWorldServer.ChunksOnShip().iterator();
        while (iter.hasNext()) {
            ChunkCoordIntPair coord = iter.next();
            this.getPlayerInstance(coord.chunkXPos, coord.chunkZPos, true);
        }
    }

    public List<EntityPlayer> getPlayers() {
        return this.theShipWorldServer.OriginWorld().playerEntities;
    }
    /**
     * Returns the ShipWorldServer associated with this PlayerManager
     */
    public ShipWorldServer getShipWorldServer() {
        return this.theShipWorldServer;
    }

    public void onWorldChunkLoad(int chunkX, int chunkZ) {
        ChunkCoordIntPair relChunk = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, this.theShipWorldServer.OriginPos());
        this.getPlayerInstance(relChunk.chunkXPos, relChunk.chunkZPos, true);

    }

    public void onWorldChunkUnload(int chunkX, int chunkZ) {
        ChunkCoordIntPair relChunk = BlockUtils.getRelativeChunkFromWorld(chunkX, chunkZ, this.theShipWorldServer.OriginPos());
        PlayerManagerShip.PlayerInstance toBeRemoved = this.getPlayerInstance(relChunk.chunkXPos, relChunk.chunkZPos, false);
        if (toBeRemoved != null) {
            long i = (long) relChunk.chunkXPos + 2147483647L | (long) relChunk.chunkZPos + 2147483647L << 32;
            this.playerInstances.remove(i);
            this.playerInstanceList.remove(toBeRemoved);
            if (toBeRemoved.numBlocksToUpdate > 0)
                this.playerInstancesToUpdate.remove(toBeRemoved);
            this.theShipWorldServer.getChunkProviderServer().dropChunk(relChunk.chunkXPos, relChunk.chunkZPos);
        }
    }
    /**
     * updates all the player instances that need to be updated
     */

    public void updatePlayerInstances() {
        long i = theShipWorldServer.getTotalWorldTime();
        if (i - previousTotalWorldTime > 8000L) {
            previousTotalWorldTime = i;
            for (int j = 0; j < playerInstanceList.size(); ++j) {
                PlayerManagerShip.PlayerInstance playermanager$playerinstance = playerInstanceList.get(j);
                playermanager$playerinstance.onUpdate();
                playermanager$playerinstance.processChunk();
            }
        } else {
            for (int k = 0; k < playerInstancesToUpdate.size(); ++k) {
                PlayerManagerShip.PlayerInstance playermanager$playerinstance1 = playerInstancesToUpdate.get(k);
                playermanager$playerinstance1.onUpdate();
            }
        }

        this.playerInstancesToUpdate.clear();

        if (this.getPlayers().isEmpty()) {
            this.theShipWorldServer.getChunkProviderServer().unloadAllChunks();
        }
    }

    public boolean hasPlayerInstance(int chunkX, int chunkZ) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        return playerInstances.getValueByKey(i) != null;
    }

    /**
     * pass in the chunk x and y and a flag as to whether or not the instance should be made if it doesn't exist
     */
    private PlayerManagerShip.PlayerInstance getPlayerInstance(int chunkX, int chunkZ, boolean createIfAbsent) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        PlayerManagerShip.PlayerInstance playermanager$playerinstance = playerInstances.getValueByKey(i);

        if (playermanager$playerinstance == null && createIfAbsent && this.theShipWorldServer.ChunksOnShip().contains(new ChunkCoordIntPair(chunkX, chunkZ))) {
            playermanager$playerinstance = new PlayerManagerShip.PlayerInstance(chunkX, chunkZ);
            playerInstances.add(i, playermanager$playerinstance);
            playerInstanceList.add(playermanager$playerinstance);
        }

        return playermanager$playerinstance;
    }

    public void markBlockForUpdate(BlockPos pos) {
        int i = pos.getX() >> 4;
        int j = pos.getZ() >> 4;
        PlayerManagerShip.PlayerInstance playermanager$playerinstance = getPlayerInstance(i, j, false);

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
        ChunkCoordIntPair worldChunk = BlockUtils.getWorldChunkFromRelative(chunkX, chunkZ, this.theShipWorldServer.OriginPos());
        return ((WorldServer) this.theShipWorldServer.OriginWorld()).getPlayerManager().isPlayerWatchingChunk(player, worldChunk.chunkXPos, worldChunk.chunkZPos);
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, ChunkCoordIntPair chunkCoords) {
        return this.isPlayerWatchingChunk(player, chunkCoords.chunkXPos, chunkCoords.chunkZPos);
    }

    public boolean isPlayerWatchingPos(EntityPlayerMP player, BlockPos pos) {
        return this.isPlayerWatchingChunk(player, pos.getX() >> 4, pos.getZ() >> 4);
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
            this.chunkCoords = new ChunkCoordIntPair(chunkX, chunkZ);
            PlayerManagerShip.this.getShipWorldServer().getChunkProviderServer().provideChunk(this.chunkCoords.chunkXPos, this.chunkCoords.chunkZPos);
        }

        /**
         * This method currently only increases chunk inhabited time. Extension is possible in next versions
         */
        public void processChunk() {
            increaseInhabitedTime(PlayerManagerShip.this.theShipWorldServer.getChunkFromChunkCoords(chunkCoords.chunkXPos, chunkCoords.chunkZPos));
        }

        /**
         * Increases chunk inhabited time every 8000 ticks
         */
        private void increaseInhabitedTime(Chunk theChunk) {
            theChunk.setInhabitedTime(theChunk.getInhabitedTime() + PlayerManagerShip.this.theShipWorldServer.getTotalWorldTime() - previousWorldTime);
            previousWorldTime = PlayerManagerShip.this.theShipWorldServer.getTotalWorldTime();
        }

        public void flagChunkForUpdate(int x, int y, int z) {
            if (numBlocksToUpdate == 0) {
                PlayerManagerShip.this.playerInstancesToUpdate.add(this);
            }

            flagsYAreasToUpdate |= 1 << (y >> 4);

            //Forge; Cache everything, so always run
            short short1 = (short) (x << 12 | z << 8 | y);

            for (int i = 0; i < this.numBlocksToUpdate; ++i) {
                if (this.locationOfBlockChange[i] == short1) {
                        return;
                    }
                }

            if (numBlocksToUpdate == locationOfBlockChange.length) {
                locationOfBlockChange = Arrays.copyOf(locationOfBlockChange, locationOfBlockChange.length << 1);
                }
            this.locationOfBlockChange[this.numBlocksToUpdate++] = short1;
        }

        public void sendToAllPlayersWatchingChunk(IMessage theMessage) {
            for (EntityPlayer player : PlayerManagerShip.this.theShipWorldServer.OriginWorld().playerEntities) {
                if (PlayerManagerShip.this.isPlayerWatchingChunk((EntityPlayerMP) player, this.chunkCoords)) {
                    FlyingShips.flyingShipPacketHandler().INSTANCE().sendTo(theMessage, (EntityPlayerMP) player);
                }
            }
        }

        @SuppressWarnings("unused")
        public void onUpdate() {
            if (this.numBlocksToUpdate != 0) {
                if (this.numBlocksToUpdate == 1) {
                    int i = (this.locationOfBlockChange[0] >> 12 & 15) + this.chunkCoords.chunkXPos * 16;
                    int j = this.locationOfBlockChange[0] & 255;
                    int k = (this.locationOfBlockChange[0] >> 8 & 15) + this.chunkCoords.chunkZPos * 16;
                    BlockPos blockpos = new BlockPos(i, j, k);
                    BlockChangedMessage message = new BlockChangedMessage(theShipWorldServer.Ship(), blockpos);
                    this.sendToAllPlayersWatchingChunk(message);

                    if (theShipWorldServer.getBlockState(blockpos).getBlock().hasTileEntity(theShipWorldServer.getBlockState(blockpos))) {
                        this.sendTileToAllPlayersWatchingChunk(theShipWorldServer.getTileEntity(blockpos));
                    }
                } else if (this.numBlocksToUpdate >= ForgeModContainer.clumpingThreshold) {
                    int i1 = this.chunkCoords.chunkXPos * 16;
                    int k1 = this.chunkCoords.chunkZPos * 16;
                    ChunkDataMessage message = new ChunkDataMessage(getShipWorldServer().Ship().ShipID(), getShipWorldServer().getChunkFromChunkCoords(this.chunkCoords.chunkXPos, this.chunkCoords.chunkZPos), false, this.flagsYAreasToUpdate);
                    this.sendToAllPlayersWatchingChunk(message);

                    // Forge: Grabs ALL tile entities is costly on a modded server, only send needed ones
                    for (int i2 = 0; false && i2 < 16; ++i2) {
                        if ((this.flagsYAreasToUpdate & 1 << i2) != 0) {
                            int k2 = i2 << 4;
                            List<TileEntity> list = getShipWorldServer().getTileEntitiesIn(i1, k2, k1, i1 + 16, k2 + 16, k1 + 16);

                            for (int l = 0; l < list.size(); ++l) {
                                this.sendTileToAllPlayersWatchingChunk(list.get(l));
                            }
                        }
                    }
                } else {
                    MultipleBlocksChangedMessage message = new MultipleBlocksChangedMessage(theShipWorldServer.Ship().ShipID(), numBlocksToUpdate, locationOfBlockChange, theShipWorldServer.getChunkFromChunkCoords(chunkCoords.chunkXPos, chunkCoords.chunkZPos));
                    this.sendToAllPlayersWatchingChunk(message);
                }
                // Forge: Send only the tile entities that are updated, Adding this brace lets us keep the indent and the patch small
                ShipWorldServer world = PlayerManagerShip.this.theShipWorldServer;
                for (int j1 = 0; j1 < numBlocksToUpdate; ++j1) {
                    int l1 = (locationOfBlockChange[j1] >> 12 & 15) + chunkCoords.chunkXPos * 16;
                    int j2 = locationOfBlockChange[j1] & 255;
                    int l2 = (locationOfBlockChange[j1] >> 8 & 15) + chunkCoords.chunkZPos * 16;
                    BlockPos blockpos1 = new BlockPos(l1, j2, l2);

                    if (world.getBlockState(blockpos1).getBlock().hasTileEntity(world.getBlockState(blockpos1))) {
                        sendTileToAllPlayersWatchingChunk(PlayerManagerShip.this.theShipWorldServer.getTileEntity(blockpos1));
                    }
                }

                numBlocksToUpdate = 0;
                flagsYAreasToUpdate = 0;
            }
        }

        private void sendTileToAllPlayersWatchingChunk(TileEntity theTileEntity) {
            if (theTileEntity != null) {
                Packet packet = theTileEntity.getDescriptionPacket();
                if (packet != null) {
                    UpdateTileEntityMessage message = new UpdateTileEntityMessage(PlayerManagerShip.this.getShipWorldServer().Ship().ShipID(), (S35PacketUpdateTileEntity) packet);
                    sendToAllPlayersWatchingChunk(message);
                }
            }
        }
    }
}