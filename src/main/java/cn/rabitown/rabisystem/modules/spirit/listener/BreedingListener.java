package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.SpiritModule;
import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

public class BreedingListener implements Listener {

    @EventHandler
    public void onBreed(EntityBreedEvent e) {
        if (!(e.getBreeder() instanceof Player p)) return;
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());

        // 确保玩家已经开启了灵契（可选，看你是否希望没小精灵也能累积次数）
        if (profile == null) return;

        EntityType type = e.getEntity().getType();

        // 1. 狐狸
        if (type == EntityType.FOX && e.getEntity() instanceof Fox fox) {
            if (fox.getFoxType() == Fox.Type.RED) {
                handleUnlock(p, profile, "breed_fox_red", 50, Achievement.BREED_FOX_RED, "FOX_RED", "§c红狐");
            } else {
                handleUnlock(p, profile, "breed_fox_snow", 100, Achievement.BREED_FOX_SNOW, "FOX_SNOW", "§f雪狐");
            }
        }
        // 2. 鹦鹉
        else if (type == EntityType.PARROT) {
            handleUnlock(p, profile, "breed_parrot", 50, Achievement.BREED_PARROT, "PARROT", "§a鹦鹉");
        }
        // 3. 美西螈
        else if (type == EntityType.AXOLOTL) {
            handleUnlock(p, profile, "breed_axolotl", 50, Achievement.BREED_AXOLOTL, "AXOLOTL", "§d美西螈");
        }

        SpiritUtils.getConfigManager().saveProfile(profile);
    }

    private void handleUnlock(Player p, SpiritProfile profile, String statKey, int target, Achievement ach, String skinId, String skinName) {
        profile.addStat(statKey, 1);
        // 如果达到目标，解锁成就
        if (profile.getStat(statKey) >= target) {
            AchievementManager.unlock(p, profile, ach);

            // 额外检查皮肤是否解锁
            if (!profile.getUnlockedSkins().contains(skinId)) {
                profile.addUnlockedSkin(skinId);
                p.sendMessage("§d§l🦋 幻化解锁！§f你的小精灵现在可以变身为 " + skinName + " 了！");
            }
        }
    }
}