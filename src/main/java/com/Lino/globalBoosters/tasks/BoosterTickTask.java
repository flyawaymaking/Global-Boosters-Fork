package com.Lino.globalBoosters.tasks;

import com.Lino.globalBoosters.GlobalBoosters;
import com.Lino.globalBoosters.boosters.ActiveBooster;
import com.Lino.globalBoosters.boosters.BoosterType;
import org.bukkit.scheduler.BukkitRunnable;

public class BoosterTickTask extends BukkitRunnable {

    private final GlobalBoosters plugin;
    private int updateCounter = 0;

    public BoosterTickTask(GlobalBoosters plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.getBoosterManager().tickAllBoosters();

        plugin.getBossBarManager().sendActionBarUpdate();

        updateCounter++;
        if (updateCounter >= 10) {
            updateCounter = 0;

            for (ActiveBooster booster : plugin.getBoosterManager().getActiveBoosters()) {
                if (booster.getType().isEffectBooster()) {
                    if (booster.isExpired()) {
                        plugin.getEffectBoosterListener().removeEffectFromAll(booster.getType());
                    } else {
                        plugin.getEffectBoosterListener().updateEffectDuration(booster.getType());
                    }
                }
            }
        }
    }
}