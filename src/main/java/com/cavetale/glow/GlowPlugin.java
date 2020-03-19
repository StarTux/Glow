package com.cavetale.glow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Value;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.EnumSkyBlock;
import net.minecraft.server.v1_12_R1.PacketPlayOutMapChunk;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class GlowPlugin extends JavaPlugin implements Listener {
    static final String PERM_GLOW = "glow.glow";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Player player = event.getPlayer();
        if (!player.hasPermission(PERM_GLOW)) return;
        if (!event.hasItem()) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.getType() != Material.GLOWSTONE_DUST) return;
        Block block = event.getClickedBlock();
        getLogger().info("Lighting up " + block.getWorld().getName()
                         + " " + block.getX()
                         + " " + block.getY()
                         + " " + block.getZ());
        player.sendMessage(ChatColor.YELLOW + "Let it glow!");
        glow(block);
    }

    @Value
    static final class Coord {
        public final int x;
        public final int z;
    }

    public void glow(Block block) {
        final int x = block.getX();
        final int y = block.getY();
        final int z = block.getZ();
        Set<Coord> ccs = new HashSet<>();
        ccs.add(new Coord(x >> 4, z >> 4));
        CraftWorld craftWorld = (CraftWorld) block.getWorld();
        net.minecraft.server.v1_12_R1.World nmsWorld = craftWorld.getHandle();
        BlockPosition pos = new BlockPosition(x, y, z);
        nmsWorld.a(EnumSkyBlock.BLOCK, pos, 15);
        nmsWorld.a(EnumSkyBlock.SKY, pos, 15);
        List<BlockPosition> airs = new ArrayList<>();
        airs.add(new BlockPosition(x, y, z + 1));
        airs.add(new BlockPosition(x, y, z - 1));
        airs.add(new BlockPosition(x + 1, y, z));
        airs.add(new BlockPosition(x - 1, y, z));
        if (y < 255) {
            airs.add(new BlockPosition(x, y + 1, z));
        }
        if (y > 0) {
            airs.add(new BlockPosition(x, y - 1, z));
        }
        for (BlockPosition air : airs) {
            ccs.add(new Coord(air.getX() >> 4, air.getZ() >> 4));
            nmsWorld.c(EnumSkyBlock.SKY, air);
            nmsWorld.c(EnumSkyBlock.BLOCK, air);
        }
        int viewDist = getServer().getViewDistance() * 16;
        List<Player> players = new ArrayList<>();
        for (Player player : block.getWorld().getPlayers()) {
            Location loc = player.getLocation();
            if (Math.abs(loc.getBlockX() - x) > viewDist) continue;
            if (Math.abs(loc.getBlockZ() - z) > viewDist) continue;
            players.add(player);
        }
        for (Coord cc : ccs) {
            net.minecraft.server.v1_12_R1.Chunk nmsChunk = nmsWorld
                .getChunkAt(cc.x, cc.z);
            nmsChunk.initLighting();
            nmsChunk.f(true); // setModified
            for (Player player : players) {
                PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, 65535);
                CraftPlayer craftPlayer = (CraftPlayer) player;
                EntityPlayer entityPlayer = craftPlayer.getHandle();
                entityPlayer.playerConnection.sendPacket(packet);
            }
        }
    }
}
