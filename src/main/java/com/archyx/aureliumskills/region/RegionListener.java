package com.archyx.aureliumskills.region;

import com.archyx.aureliumskills.AureliumSkills;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class RegionListener implements Listener {

    private final AureliumSkills plugin;
    private final RegionManager regionManager;

    public RegionListener(AureliumSkills plugin) {
        this.plugin = plugin;
        regionManager = plugin.getRegionManager();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        int regionX = (int) Math.floor((double) chunk.getX() / 32.0);
        int regionZ = (int) Math.floor((double) chunk.getZ() / 32.0);
        RegionCoordinate regionCoordinate = new RegionCoordinate(event.getWorld(), regionX, regionZ);
        Region region = regionManager.getRegion(regionCoordinate);
        if (region == null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (regionManager.getRegion(regionCoordinate) == null) {
                        regionManager.loadRegion(event.getWorld(), regionX, regionZ);
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent event) {
        new BukkitRunnable() {
            @Override
            public void run() {
                regionManager.saveWorldRegions(event.getWorld(), true);
            }
        }.runTaskAsynchronously(plugin);
    }

}