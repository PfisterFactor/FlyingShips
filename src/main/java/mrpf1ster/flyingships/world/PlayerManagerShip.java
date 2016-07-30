package mrpf1ster.flyingships.world;

import com.google.common.collect.Lists;
import mrpf1ster.flyingships.FlyingShips;
import mrpf1ster.flyingships.network.BlockChangedMessage;
import mrpf1ster.flyingships.network.ChunkDataMessage;
import mrpf1ster.flyingships.network.MultipleBlocksChangedMessage;
import mrpf1ster.flyingships.network.UpdateTileEntityMessage;
import mrpf1ster.flyingships.world.chunk.ChunkProviderShip;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.LongHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.ChunkCoordComparator;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

// The entire PlayerManager class copied here, so we can use it for our ship worlds
public class PlayerManagerShip {
    private static final Logger pmLogger = LogManager.getLogger();
    private final ShipWorldServer theShipWorldServer;
    private final List<EntityPlayerMP> players = Lists.newArrayList();
    private final LongHashMap<PlayerInstance> playerInstances = new LongHashMap();
    private final List<PlayerInstance> playerInstancesToUpdate = Lists.newArrayList();
    private final List<PlayerInstance> playerInstanceList = Lists.newArrayList();
    /**
     * Number of chunks the server sends to the client. Valid 3<=x<=15. In server.properties.
     */
    private int playerViewRadius;
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
        this.setPlayerViewRadius(MinecraftServer.getServer().getConfigurationManager().getViewDistance());
        for (EntityPlayer player : this.getShipWorldServer().OriginWorld().playerEntities) {
            this.addPlayer((EntityPlayerMP) player);
        }

    }

    /**
     * Returns the WorldServer associated with this PlayerManager
     */
    public ShipWorldServer getShipWorldServer() {
        return this.theShipWorldServer;
    }

    /**
     * updates all the player instances that need to be updated
     */
    public void updatePlayerInstances() {
        long i = theShipWorldServer.getTotalWorldTime();

        if (i - previousTotalWorldTime > 8000L) {
            previousTotalWorldTime = i;

            for (int j = 0; j < playerInstanceList.size(); ++j) {
                PlayerInstance playermanager$playerinstance = playerInstanceList.get(j);
                playermanager$playerinstance.onUpdate();
                playermanager$playerinstance.processChunk();
            }
        } else {
            for (int k = 0; k < playerInstancesToUpdate.size(); ++k) {
                PlayerInstance playermanager$playerinstance1 = playerInstancesToUpdate.get(k);
                playermanager$playerinstance1.onUpdate();
            }
        }

        playerInstancesToUpdate.clear();

        if (players.isEmpty()) {
            WorldProvider worldprovider = theShipWorldServer.provider;

            if (!worldprovider.canRespawnHere()) {
                theShipWorldServer.getChunkProviderServer().unloadAllChunks();
            }
        }
    }

    public boolean hasPlayerInstance(int chunkX, int chunkZ) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        return playerInstances.getValueByKey(i) != null;
    }

    /**
     * pass in the chunk x and y and a flag as to whether or not the instance should be made if it doesn't exist
     */
    private PlayerInstance getPlayerInstance(int chunkX, int chunkZ, boolean createIfAbsent) {
        long i = (long) chunkX + 2147483647L | (long) chunkZ + 2147483647L << 32;
        PlayerInstance playermanager$playerinstance = playerInstances.getValueByKey(i);

        if (playermanager$playerinstance == null && createIfAbsent) {
            playermanager$playerinstance = new PlayerInstance(chunkX, chunkZ);
            playerInstances.add(i, playermanager$playerinstance);
            playerInstanceList.add(playermanager$playerinstance);
        }

        return playermanager$playerinstance;
    }

    public void markBlockForUpdate(BlockPos pos) {
        int i = pos.getX() >> 4;
        int j = pos.getZ() >> 4;
        PlayerInstance playermanager$playerinstance = getPlayerInstance(i, j, false);

        if (playermanager$playerinstance != null) {
            playermanager$playerinstance.flagChunkForUpdate(pos.getX() & 15, pos.getY(), pos.getZ() & 15);
        }
    }

    /**
     * Adds an EntityPlayerMP to the PlayerManager and to all player instances within player visibility
     */
    public void addPlayer(EntityPlayerMP player) {
        int i = (int) player.posX >> 4;
        int j = (int) player.posZ >> 4;
        player.managedPosX = player.posX;
        player.managedPosZ = player.posZ;
        // Load nearby chunks first
        List<ChunkCoordIntPair> chunkList = Lists.newArrayList();

        for (int k = i - playerViewRadius; k <= i + playerViewRadius; ++k) {
            for (int l = j - playerViewRadius; l <= j + playerViewRadius; ++l) {
                chunkList.add(new ChunkCoordIntPair(k, l));
            }
        }

        Collections.sort(chunkList, new ChunkCoordComparator(player));

        for (ChunkCoordIntPair pair : chunkList) {
            getPlayerInstance(pair.chunkXPos, pair.chunkZPos, true).addPlayer(player);
        }

        players.add(player);
        filterChunkLoadQueue(player);
    }

    /**
     * Removes all chunks from the given player's chunk load queue that are not in viewing range of the player.
     */
    public void filterChunkLoadQueue(EntityPlayerMP player) {
        List<ChunkCoordIntPair> list = Lists.newArrayList(player.loadedChunks);
        int i = 0;
        int j = playerViewRadius;
        int k = (int) player.posX >> 4;
        int l = (int) player.posZ >> 4;
        int i1 = 0;
        int j1 = 0;
        ChunkCoordIntPair chunkcoordintpair = getPlayerInstance(k, l, true).chunkCoords;
        player.loadedChunks.clear();

        if (list.contains(chunkcoordintpair)) {
            player.loadedChunks.add(chunkcoordintpair);
        }

        for (int k1 = 1; k1 <= j * 2; ++k1) {
            for (int l1 = 0; l1 < 2; ++l1) {
                int[] aint = xzDirectionsConst[i++ % 4];

                for (int i2 = 0; i2 < k1; ++i2) {
                    i1 += aint[0];
                    j1 += aint[1];
                    chunkcoordintpair = getPlayerInstance(k + i1, l + j1, true).chunkCoords;

                    if (list.contains(chunkcoordintpair)) {
                        player.loadedChunks.add(chunkcoordintpair);
                    }
                }
            }
        }

        i = i % 4;

        for (int j2 = 0; j2 < j * 2; ++j2) {
            i1 += xzDirectionsConst[i][0];
            j1 += xzDirectionsConst[i][1];
            chunkcoordintpair = getPlayerInstance(k + i1, l + j1, true).chunkCoords;

            if (list.contains(chunkcoordintpair)) {
                player.loadedChunks.add(chunkcoordintpair);
            }
        }
    }

    /**
     * Removes an EntityPlayerMP from the PlayerManager.
     */
    public void removePlayer(EntityPlayerMP player) {
        int i = (int) player.managedPosX >> 4;
        int j = (int) player.managedPosZ >> 4;

        for (int k = i - playerViewRadius; k <= i + playerViewRadius; ++k) {
            for (int l = j - playerViewRadius; l <= j + playerViewRadius; ++l) {
                PlayerInstance playermanager$playerinstance = getPlayerInstance(k, l, false);

                if (playermanager$playerinstance != null) {
                    playermanager$playerinstance.removePlayer(player);
                }
            }
        }

        players.remove(player);
    }

    /**
     * Determine if two rectangles centered at the given points overlap for the provided radius. Arguments: x1, z1, x2,
     * z2, radius.
     */
    private boolean overlaps(int x1, int z1, int x2, int z2, int radius) {
        int i = x1 - x2;
        int j = z1 - z2;
        return (i >= -radius && i <= radius) && (j >= -radius && j <= radius);
    }

    /**
     * update chunks around a player being moved by server logic (e.g. cart, boat)
     */
    public void updateMountedMovingPlayer(EntityPlayerMP player) {
        int i = (int) player.posX >> 4;
        int j = (int) player.posZ >> 4;
        double d0 = player.managedPosX - player.posX;
        double d1 = player.managedPosZ - player.posZ;
        double d2 = d0 * d0 + d1 * d1;

        if (d2 >= 64.0D) {
            int k = (int) player.managedPosX >> 4;
            int l = (int) player.managedPosZ >> 4;
            int i1 = playerViewRadius;
            int j1 = i - k;
            int k1 = j - l;
            List<ChunkCoordIntPair> chunksToLoad = Lists.newArrayList();

            if (j1 != 0 || k1 != 0) {
                for (int l1 = i - i1; l1 <= i + i1; ++l1) {
                    for (int i2 = j - i1; i2 <= j + i1; ++i2) {
                        if (!overlaps(l1, i2, k, l, i1)) {
                            chunksToLoad.add(new ChunkCoordIntPair(l1, i2));
                        }

                        if (!overlaps(l1 - j1, i2 - k1, i, j, i1)) {
                            PlayerInstance playermanager$playerinstance = getPlayerInstance(l1 - j1, i2 - k1, false);

                            if (playermanager$playerinstance != null) {
                                playermanager$playerinstance.removePlayer(player);
                            }
                        }
                    }
                }

                filterChunkLoadQueue(player);
                player.managedPosX = player.posX;
                player.managedPosZ = player.posZ;
                // send nearest chunks first
                Collections.sort(chunksToLoad, new ChunkCoordComparator(player));

                for (ChunkCoordIntPair pair : chunksToLoad) {
                    getPlayerInstance(pair.chunkXPos, pair.chunkZPos, true).addPlayer(player);
                }

                if (i1 > 1 || i1 < -1 || j1 > 1 || j1 < -1) {
                    Collections.sort(player.loadedChunks, new ChunkCoordComparator(player));
                }
            }
        }
    }

    public boolean isPlayerWatchingChunk(EntityPlayerMP player, int chunkX, int chunkZ) {
        PlayerInstance playermanager$playerinstance = getPlayerInstance(chunkX, chunkZ, false);
        return playermanager$playerinstance != null && playermanager$playerinstance.playersWatchingChunk.contains(player) && !player.loadedChunks.contains(playermanager$playerinstance.chunkCoords);
    }

    public void setPlayerViewRadius(int radius) {
        radius = MathHelper.clamp_int(radius, 3, 32);

        if (radius != playerViewRadius) {
            int i = radius - playerViewRadius;

            for (EntityPlayerMP entityplayermp : Lists.newArrayList(players)) {
                int j = (int) entityplayermp.posX >> 4;
                int k = (int) entityplayermp.posZ >> 4;

                if (i > 0) {
                    for (int j1 = j - radius; j1 <= j + radius; ++j1) {
                        for (int k1 = k - radius; k1 <= k + radius; ++k1) {
                            PlayerInstance playermanager$playerinstance = getPlayerInstance(j1, k1, true);

                            if (!playermanager$playerinstance.playersWatchingChunk.contains(entityplayermp)) {
                                playermanager$playerinstance.addPlayer(entityplayermp);
                            }
                        }
                    }
                } else {
                    for (int l = j - playerViewRadius; l <= j + playerViewRadius; ++l) {
                        for (int i1 = k - playerViewRadius; i1 <= k + playerViewRadius; ++i1) {
                            if (!overlaps(l, i1, j, k, radius)) {
                                getPlayerInstance(l, i1, true).removePlayer(entityplayermp);
                            }
                        }
                    }
                }
            }

            playerViewRadius = radius;
        }
    }

    /**
     * Get the furthest viewable block given player's view distance
     */
    public static int getFurthestViewableBlock(int distance) {
        return distance * 16 - 16;
    }

    class PlayerInstance {
        private final List<EntityPlayerMP> playersWatchingChunk = Lists.newArrayList();
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
        private final HashMap<EntityPlayerMP, Runnable> players = new HashMap<EntityPlayerMP, Runnable>();
        private boolean loaded;
        private final Runnable loadedRunnable = new Runnable() {
            @Override
            public void run() {
                loaded = true;
            }
        };

        public PlayerInstance(int chunkX, int chunkZ) {
            chunkCoords = new ChunkCoordIntPair(chunkX, chunkZ);
            PlayerManagerShip.this.getShipWorldServer().getChunkProviderServer().loadChunk(chunkX, chunkZ, loadedRunnable);
        }

        public void addPlayer(EntityPlayerMP player) {
            if (playersWatchingChunk.contains(player)) {
                pmLogger.debug("Failed to add player. {} already is in chunk {}, {}", player, Integer.valueOf(chunkCoords.chunkXPos), Integer.valueOf(chunkCoords.chunkZPos));
            } else {
                if (playersWatchingChunk.isEmpty()) {
                    previousWorldTime = PlayerManagerShip.this.getShipWorldServer().getTotalWorldTime();
                }

                playersWatchingChunk.add(player);
                Runnable playerRunnable = null;
                if (loaded) {
                    player.loadedChunks.add(chunkCoords);
                } else {
                    final EntityPlayerMP tmp = player;
                    playerRunnable = new Runnable() {
                        @Override
                        public void run() {
                            tmp.loadedChunks.add(chunkCoords);
                        }
                    };
                    PlayerManagerShip.this.getShipWorldServer().getChunkProviderServer().loadChunk(chunkCoords.chunkXPos, chunkCoords.chunkZPos, playerRunnable);
                }
                players.put(player, playerRunnable);
            }
        }

        public void removePlayer(EntityPlayerMP player) {
            if (playersWatchingChunk.contains(player)) {
                // If we haven't loaded yet don't load the chunk just so we can clean it up
                if (!loaded) {
                    ChunkProviderShip.ShipChunkIO().dropQueuedChunkLoad(PlayerManagerShip.this.theShipWorldServer, chunkCoords.chunkXPos, chunkCoords.chunkZPos, players.get(player));
                    playersWatchingChunk.remove(player);
                    players.remove(player);

                    if (playersWatchingChunk.isEmpty()) {
                        ChunkProviderShip.ShipChunkIO().dropQueuedChunkLoad(PlayerManagerShip.this.theShipWorldServer, chunkCoords.chunkXPos, chunkCoords.chunkZPos, loadedRunnable);
                        long i = (long) chunkCoords.chunkXPos + 2147483647L | (long) chunkCoords.chunkZPos + 2147483647L << 32;
                        PlayerManagerShip.this.playerInstances.remove(i);
                        PlayerManagerShip.this.playerInstanceList.remove(this);
                    }

                    return;
                }

                Chunk chunk = PlayerManagerShip.this.theShipWorldServer.getChunkFromChunkCoords(chunkCoords.chunkXPos, chunkCoords.chunkZPos);

                if (chunk.isPopulated()) {
                    //player.playerNetServerHandler.sendPacket(new S21PacketChunkData(chunk, true, 0));
                    System.out.println("Chunk is populated on ShipWorld, panic!");
                }

                players.remove(player);
                playersWatchingChunk.remove(player);
                player.loadedChunks.remove(chunkCoords);
                // TODO: Pass a player object with worldObj changed to the shipworld
                //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkWatchEvent.UnWatch(chunkCoords, player));

                if (playersWatchingChunk.isEmpty()) {
                    long i = (long) chunkCoords.chunkXPos + 2147483647L | (long) chunkCoords.chunkZPos + 2147483647L << 32;
                    this.increaseInhabitedTime(chunk);
                    playerInstances.remove(i);
                    playerInstanceList.remove(this);

                    if (this.numBlocksToUpdate > 0) {
                        playerInstancesToUpdate.remove(this);
                    }

                    getShipWorldServer().getChunkProviderServer().dropChunk(this.chunkCoords.chunkXPos, this.chunkCoords.chunkZPos);
                }
            }
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
                playerInstancesToUpdate.add(this);
            }

            this.flagsYAreasToUpdate |= 1 << (y >> 4);

            //Forge; Cache everything, so always run
            {
                short short1 = (short) (x << 12 | z << 8 | y);

                for (int i = 0; i < this.numBlocksToUpdate; ++i) {
                    if (this.locationOfBlockChange[i] == short1) {
                        return;
                    }
                }

                if (numBlocksToUpdate == locationOfBlockChange.length) {
                    locationOfBlockChange = java.util.Arrays.copyOf(locationOfBlockChange, locationOfBlockChange.length << 1);
                }
                this.locationOfBlockChange[this.numBlocksToUpdate++] = short1;
            }
        }

        public void sendToAllPlayersWatchingChunk(IMessage theMessage) {
            for (int i = 0; i < this.playersWatchingChunk.size(); ++i) {
                EntityPlayerMP entityplayermp = (EntityPlayerMP) this.playersWatchingChunk.get(i);

                if (!entityplayermp.loadedChunks.contains(this.chunkCoords)) {
                    FlyingShips.flyingShipPacketHandler().INSTANCE().sendTo(theMessage, entityplayermp);
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
                } else if (this.numBlocksToUpdate >= net.minecraftforge.common.ForgeModContainer.clumpingThreshold) {
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
                                this.sendTileToAllPlayersWatchingChunk((TileEntity) list.get(l));
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