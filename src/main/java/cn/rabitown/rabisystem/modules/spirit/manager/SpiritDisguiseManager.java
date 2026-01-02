package cn.rabitown.rabisystem.modules.spirit.manager;

import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritSkin;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.AxolotlWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.FoxWatcher;
import me.libraryaddict.disguise.disguisetypes.watchers.ParrotWatcher;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Parrot;

public class SpiritDisguiseManager {

    public static void updateDisguise(Allay spirit, SpiritProfile profile) {
        if (spirit == null || !spirit.isValid()) return;

        // 使用枚举查找
        SpiritSkin skin = SpiritSkin.fromId(profile.getCurrentSkin());

        // 默认皮肤：移除伪装
        if (skin == SpiritSkin.DEFAULT) {
            DisguiseAPI.undisguiseToAll(spirit);
            return;
        }

        MobDisguise disguise = null;

        // 这里依然需要 switch，因为每种实体的 Watcher 设置不一样
        // 但使用枚举的 switch 比 string 更快更安全
        switch (skin) {
            case FOX_RED:
                disguise = new MobDisguise(DisguiseType.FOX);
                FoxWatcher redFox = (FoxWatcher) disguise.getWatcher();
                redFox.setType(Fox.Type.RED);
                break;
            case FOX_SNOW:
                disguise = new MobDisguise(DisguiseType.FOX);
                FoxWatcher snowFox = (FoxWatcher) disguise.getWatcher();
                snowFox.setType(Fox.Type.SNOW);
                break;
            case PARROT:
                disguise = new MobDisguise(DisguiseType.PARROT);
                ParrotWatcher parrot = (ParrotWatcher) disguise.getWatcher();
                parrot.setVariant(Parrot.Variant.RED);
                break;
            case AXOLOTL:
                disguise = new MobDisguise(DisguiseType.AXOLOTL);
                AxolotlWatcher axolotl = (AxolotlWatcher) disguise.getWatcher();
                axolotl.setVariant(Axolotl.Variant.LUCY);
                break;
            default:
                break;
        }

        if (disguise != null) {
            disguise.setEntity(spirit);
            disguise.setCustomDisguiseName(true);
            disguise.getWatcher().setCustomNameVisible(true);
            disguise.startDisguise();
        }
    }
}