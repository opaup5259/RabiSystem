package cn.rabitown.rabisystem.modules.fakeplayer.manager;

import cn.rabitown.rabisystem.modules.fakeplayer.FakePlayerModule;
import cn.rabitown.rabisystem.modules.fakeplayer.data.FakePlayerData;
import cn.rabitown.rabisystem.modules.fakeplayer.utils.NMSHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FakePlayerManager {
    private final FakePlayerModule module;
    private final Map<String, FakePlayerData> fakePlayersData = new HashMap<>();

    public FakePlayerManager(FakePlayerModule module) {
        this.module = module;
    }

    public void load() {
        fakePlayersData.putAll(module.getConfigManager().loadData());
    }

    public void save() {
        module.getConfigManager().saveData(fakePlayersData);
    }

    public void cleanup() {
        // 插件卸载时踢出所有假人
        for (String id : fakePlayersData.keySet()) {
            despawnFakePlayer(id);
        }
    }

    public boolean exists(String id) {
        return fakePlayersData.containsKey(id);
    }

    public FakePlayerData getData(String id) {
        return fakePlayersData.get(id);
    }

    public void createData(String id, Player owner) {
        FakePlayerData data = new FakePlayerData(id, owner.getUniqueId(), owner.getName(), owner.getLocation());
        fakePlayersData.put(id, data);
        save();
    }

    public void removeData(String id) {
        despawnFakePlayer(id);
        fakePlayersData.remove(id);
        save();
    }

    public void spawnFakePlayer(String id) {
        FakePlayerData data = fakePlayersData.get(id);
        if (data == null) return;

        // 检查是否已经在线
        if (Bukkit.getPlayerExact(data.getFullId()) != null) return;

        module.getPlugin().getLogger().info("正在生成假人: " + data.getFullId());
        try {
            // 使用 UUID 生成一个固定的，以免每次 UUID 随机变动
            UUID fakeUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + data.getFullId()).getBytes());

            Player spawnedPlayer = NMSHelper.spawnFakePlayer(data.getLocation(), fakeUUID, data.getFullId());

            if (spawnedPlayer != null) {
                // 生成成功后，应用一些设置
                spawnedPlayer.setInvulnerable(data.isGodMode());
                spawnedPlayer.setCanPickupItems(data.isPickup());

                // 强制修正位置和朝向
                spawnedPlayer.teleport(data.getLocation());

                module.getPlugin().getLogger().info("假人生成成功！");
            } else {
                module.getPlugin().getLogger().warning("假人生成失败，请检查控制台报错。");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void despawnFakePlayer(String id) {
        FakePlayerData data = fakePlayersData.get(id);
        if (data == null) return;
        Player p = Bukkit.getPlayerExact(data.getFullId());
        if (p != null) {
            // 保存位置
            data.setLocation(p.getLocation());
            p.kick(Component.text("Fake Player Removed"));
        }
    }

    public Player getFakePlayerEntity(String id) {
        FakePlayerData data = fakePlayersData.get(id);
        return data == null ? null : Bukkit.getPlayerExact(data.getFullId());
    }

    public Map<String, FakePlayerData> getAllData() {
        return fakePlayersData;
    }

    // 皮肤更新逻辑 (需重连生效)
    public void setSkin(String id, String skinName) {
        FakePlayerData data = fakePlayersData.get(id);
        if(data != null) {
            data.setSkinName(skinName);
            boolean wasOnline = getFakePlayerEntity(id) != null;
            if(wasOnline) {
                despawnFakePlayer(id);
                spawnFakePlayer(id);
            }
        }
    }
}