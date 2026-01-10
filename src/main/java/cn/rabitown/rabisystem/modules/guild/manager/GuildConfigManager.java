package cn.rabitown.rabisystem.modules.guild.manager;

import cn.rabitown.rabisystem.modules.guild.GuildModule;
import cn.rabitown.rabisystem.modules.guild.data.GuildData;
import cn.rabitown.rabisystem.modules.guild.data.GuildMember;
import cn.rabitown.rabisystem.modules.guild.data.GuildRank;
import cn.rabitown.rabisystem.modules.guild.data.GuildType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class GuildConfigManager {

    private final GuildModule module;
    private File dataFile;
    private FileConfiguration dataConfig;

    public GuildConfigManager(GuildModule module) {
        this.module = module;
        loadDataFile();
    }

    private void loadDataFile() {
        dataFile = new File(module.getPlugin().getDataFolder(), "guild_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadGuilds(Map<UUID, GuildData> guildCache, Map<UUID, UUID> playerMap) {
        guildCache.clear();
        playerMap.clear();

        if (!dataConfig.contains("guilds")) return;

        for (String key : dataConfig.getConfigurationSection("guilds").getKeys(false)) {
            try {
                UUID guildId = UUID.fromString(key);
                String path = "guilds." + key;

                GuildData guild = new GuildData(guildId);
                guild.setName(dataConfig.getString(path + ".name"));
                guild.setIcon(Material.getMaterial(dataConfig.getString(path + ".icon", "GRASS_BLOCK")));
                guild.setType(GuildType.valueOf(dataConfig.getString(path + ".type", "INVITE")));
                guild.setPassword(dataConfig.getString(path + ".password"));

                // 加载成员
                if (dataConfig.contains(path + ".members")) {
                    for (String memberIdStr : dataConfig.getConfigurationSection(path + ".members").getKeys(false)) {
                        UUID memberId = UUID.fromString(memberIdStr);
                        String mPath = path + ".members." + memberIdStr;
                        GuildRank rank = GuildRank.valueOf(dataConfig.getString(mPath + ".rank", "MEMBER"));
                        long joinTime = dataConfig.getLong(mPath + ".joined");

                        guild.getMembers().put(memberId, new GuildMember(memberId, rank, joinTime));
                        playerMap.put(memberId, guildId);
                    }
                }

                // 加载邀请
                if (dataConfig.contains(path + ".invites")) {
                    for (String invIdStr : dataConfig.getConfigurationSection(path + ".invites").getKeys(false)) {
                        long expire = dataConfig.getLong(path + ".invites." + invIdStr);
                        if (System.currentTimeMillis() < expire) {
                            guild.getInvites().put(UUID.fromString(invIdStr), expire);
                        }
                    }
                }

                guildCache.put(guildId, guild);

            } catch (Exception e) {
                module.getPlugin().getLogger().warning("加载公会数据失败: " + key);
                e.printStackTrace();
            }
        }
    }

    public void saveGuilds(Map<UUID, GuildData> guildCache) {
        dataConfig.set("guilds", null);
        for (GuildData guild : guildCache.values()) {
            String path = "guilds." + guild.getId();
            dataConfig.set(path + ".name", guild.getName());
            dataConfig.set(path + ".icon", guild.getIcon().name());
            dataConfig.set(path + ".type", guild.getType().name());
            dataConfig.set(path + ".password", guild.getPassword());

            for (GuildMember member : guild.getMembers().values()) {
                String mPath = path + ".members." + member.getUuid();
                dataConfig.set(mPath + ".rank", member.getRank().name());
                dataConfig.set(mPath + ".joined", member.getJoinTime());
            }

            for (Map.Entry<UUID, Long> entry : guild.getInvites().entrySet()) {
                if (System.currentTimeMillis() < entry.getValue()) {
                    dataConfig.set(path + ".invites." + entry.getKey(), entry.getValue());
                }
            }
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "保存 guild_data.yml 失败", e);
        }
    }
}
