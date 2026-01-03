package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.RabiSystem;
import cn.rabitown.rabisystem.modules.afk.AFKModule;
import cn.rabitown.rabisystem.modules.corpse.CorpseModule;
import cn.rabitown.rabisystem.modules.playtime.PlayTimeModule;
import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SignInManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SkillManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritDisguiseManager;
import cn.rabitown.rabisystem.modules.spirit.skill.SkillType;
import cn.rabitown.rabisystem.modules.spirit.task.LotteryTask;
import cn.rabitown.rabisystem.modules.spirit.manager.LotteryManager;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import cn.rabitown.rabisystem.modules.warpStone.WarpStoneModule;
import cn.rabitown.rabisystem.modules.warpStone.ui.WarpMenus;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Allay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class MenuListener implements Listener {

    // 白名单：仅允许这两个格子进行物理交互 (P1)
    // 28: 主手装备槽
    // 35: 快捷投喂槽
    private final Set<Integer> ALLOWED_SLOTS_P1 = new HashSet<>(Arrays.asList(28, 35));

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof SpiritMenus.SpiritHolder)) return;
        SpiritMenus.SpiritHolder holder = (SpiritMenus.SpiritHolder) e.getView().getTopInventory().getHolder();

        Player p = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        int slot = e.getRawSlot();
        if (clickedInv == null) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(holder.getOwner());
        String type = holder.getType();

        // --- 统一拦截所有灵契菜单的顶部点击，防止图标被拿走 ---
        if (clickedInv == e.getView().getTopInventory()) {
            if ("MAIN".equals(type)) {
                // 主菜单特殊逻辑：只有 P1 的特定槽位允许拿取
                if (holder.getPage() == 1 && ALLOWED_SLOTS_P1.contains(slot)) {
                    ItemStack item = e.getCurrentItem();
                    // 如果是结构空位（未解锁提示），禁止拿取
                    if (item != null && item.getType() == Material.STRUCTURE_VOID) {
                        e.setCancelled(true);
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendActionBar("§c该槽位等级不足，尚未解锁！");
                    } else {
                        // 允许操作
                        e.setCancelled(false);
                    }
                } else {
                    e.setCancelled(true);
                }
            } else if (!type.equals("BACKPACK") && !type.equals("FOOD")) {
                // 其他非背包/食物袋界面，一律禁止
                e.setCancelled(true);
            }
        }

        // --- 背包界面 (BACKPACK) ---
        if ("BACKPACK".equals(type)) {
            // 点击了顶部界面
            if (clickedInv == e.getView().getTopInventory()) {
                // 最后一格是返回按钮，禁止拿取，点击触发跳转
                if (slot == clickedInv.getSize() - 1) {
                    e.setCancelled(true); // 锁定
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                    SpiritMenus.openMainMenu(p, profile);
                    return;
                }
            }
        }

        // --- 等级树 (LEVEL_TREE) ---
        if ("LEVEL_TREE".equals(holder.getType())) {
            e.setCancelled(true);
            slot = e.getRawSlot();

            // 1. 顶部功能
            if (slot == 4) { // 返回
                SpiritMenus.openMainMenu(p, profile);
                p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                return;
            }
            if (slot == 8) { // 重置
                if (profile.getSpentSkillPoints() == 0) return;
                profile.resetSkills();
                p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                p.sendMessage("§c技能树已重置，点数已返还。");
                SpiritUtils.getConfigManager().saveProfile(profile);
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage());
                return;
            }

            // 2. 星光系操作
            if (slot == 26) { // View More
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage() + 1, holder.getShadowPage());
                return;
            }

            // 3. 暗影系操作
            if (slot == 35) { // View More
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage() + 1);
                return;
            }

            // 4. 点击技能图标
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                NamespacedKey key = new NamespacedKey(SpiritUtils.getPlugin(), "skill_id");
                String skillId = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                if (skillId != null) {
                    handleSkillClick(p, profile, skillId);
                    SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage());
                }
            }
        }

        // --- 食物包界面 (FOOD) ---
        if ("FOOD".equals(type)) {
            if (clickedInv == e.getView().getTopInventory()) {
                if (slot == 8) { // 返回按钮
                    e.setCancelled(true);
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                    SpiritMenus.openMainMenu(p, profile);
                    return;
                }
                int unlocked = SpiritMenus.getUnlockedFoodSlots(profile.getLevel());
                if (slot >= unlocked && slot < 8) {
                    e.setCancelled(true);
                    p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1.5f);
                    p.sendActionBar(Component.text("§c此槽位尚未解锁"));
                    return;
                }
            }
        }

        // --- 虚空引力界面 (GRAVITY) ---
        if ("GRAVITY".equals(type)) {
            if (clickedInv == e.getView().getTopInventory()) {
                handleGravityMenuClicks(p, profile, slot, e.getCursor());
            }
            return;
        }

        // --- 签到界面 (SIGNIN) ---
        if ("SIGNIN".equals(holder.getType())) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Calendar viewCal = holder.getViewCalendar();

            if (slot == 0) { // 上一月
                viewCal.add(Calendar.MONTH, -1);
                SpiritMenus.openSignInMenu(p, profile, viewCal);
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 0.8f);
                return;
            }
            if (slot == 8) { // 下一月
                viewCal.add(Calendar.MONTH, 1);
                SpiritMenus.openSignInMenu(p, profile, viewCal);
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1.2f);
                return;
            }
            if (slot == 4) { // 返回
                SpiritMenus.openMainMenu(p, profile);
                p.playSound(p.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_CLOSE, 1f, 1.2f);
                return;
            }
            if (slot == 5) { // 排行榜入口 (对应原代码逻辑可能不同，这里按新菜单逻辑)
                SpiritMenus.openLeaderboardMenu(p, profile);
                return;
            }

            // 签到判定
            if (clicked.getType() == Material.GOLD_BLOCK || clicked.getType() == Material.PAPER || clicked.getType() == Material.NETHER_STAR) {
                try {
                    String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("第\\s*(\\d+)\\s*天").matcher(plainName);
                    if (matcher.find()) {
                        int day = Integer.parseInt(matcher.group(1));
                        SignInManager.processSignIn(p, profile, day, viewCal);
                        SpiritMenus.openSignInMenu(p, profile, viewCal);
                    }
                } catch (Exception ex) {
                    // ignore
                }
            }
            return;
        }

        // --- 排行榜 (RANK) ---
        if ("RANK".equals(holder.getType())) {
            e.setCancelled(true);
            if (slot == 26) {
                SpiritMenus.openSignInMenu(p, profile, Calendar.getInstance());
            }
            return;
        }

        // --- 成就界面 (ACHIEVEMENT) ---
        if ("ACHIEVEMENT".equals(holder.getType())) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            slot = e.getRawSlot();
            int currentPage = holder.getPage();
            SpiritMenus.FilterType currentFilter = holder.getFilterType();

            if (slot == 0 && clicked.getType() == Material.ARROW) {
                SpiritMenus.openAchievementMenu(p, profile, currentPage - 1, currentFilter);
            } else if (slot == 4) {
                p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                SpiritMenus.openMainMenu(p, profile);
            } else if (slot == 5) {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                SpiritMenus.openAchievementMenu(p, profile, 1, currentFilter.next());
            } else if (slot == 8 && clicked.getType() == Material.ARROW) {
                SpiritMenus.openAchievementMenu(p, profile, currentPage + 1, currentFilter);
            }
            else if (slot >= 18 && slot < 54) {
                List<Achievement> sortedList = SpiritMenus.getSortedAchievements(profile, currentFilter);
                int pageSize = 36;
                int startIndex = (currentPage - 1) * pageSize;
                int index = startIndex + (slot - 18);

                if (index >= 0 && index < sortedList.size()) {
                    Achievement ach = sortedList.get(index);
                    boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
                    boolean claimed = profile.isClaimed(ach.getId());

                    if (unlocked && !claimed) {
                        AchievementManager.claimReward(p, profile, ach);
                        SpiritMenus.openAchievementMenu(p, profile, currentPage, currentFilter);
                    } else if (!unlocked) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        p.sendActionBar("§c尚未达成此成就，继续加油喵！");
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                }
            }
            return;
        }

        // --- 幻化菜单 (SKINS) ---
        if ("SKINS".equals(holder.getType())) {
            e.setCancelled(true);
            if (e.getRawSlot() == 26) {
                SpiritMenus.openMainMenu(p, profile);
                return;
            }
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
            if (!clickedItem.hasItemMeta()) return;

            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(SpiritUtils.getPlugin(), "skin_id");
            String skinId = clickedItem.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
            if (skinId == null) return;

            if (!profile.isSkinUnlocked(skinId)) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                p.sendMessage("§c该外观尚未解锁！");
                return;
            }

            profile.setCurrentSkin(skinId);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            p.sendActionBar("§d✨ 幻化形态已更新！");

            org.bukkit.entity.Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(p.getUniqueId());
            if (spirit != null && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
                SpiritDisguiseManager.updateDisguise(spirit, profile);
            }
            SpiritMenus.openSkinsMenu(p, profile);
            SpiritUtils.getConfigManager().saveProfile(profile);
            return;
        }

        // --- 主菜单 (MAIN) 分页逻辑 ---
        if ("MAIN".equals(holder.getType())) {
            // 点击底部玩家背包：放行 (禁止Shift)
            if (clickedInv == e.getView().getBottomInventory()) {
                if (e.isShiftClick()) e.setCancelled(true);
                return;
            }

            // 顶部点击逻辑
            e.setCancelled(true);
            int page = holder.getPage();

            // P1 (第一页)
            if (page == 1) {
                // 物理交互判定
                if (ALLOWED_SLOTS_P1.contains(slot)) {
                    ItemStack item = e.getCurrentItem();
                    if (item != null && item.getType() == Material.STRUCTURE_VOID) {
                        e.setCancelled(true);
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendActionBar("§c该槽位等级不足，尚未解锁！");
                    } else {
                        e.setCancelled(false); // 允许放入/取出
                    }
                    return;
                }

                switch (slot) {
                    case 4: // 背包
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        SpiritMenus.openBackpack(p, profile);
                        break;
                    case 7: // 签到
                        p.playSound(p.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 1f);
                        SpiritMenus.openSignInMenu(p, profile, java.util.Calendar.getInstance());
                        break;
                    case 8: // 祈愿 (逻辑复用原 handleLottery)
                        handleLottery(p, profile);
                        break;
                    case 9: // 技能树
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        SpiritMenus.openLevelTreeMenu(p, profile);
                        break;
                    case 12: // 真名刻印
                        handleRename(p, profile);
                        break;
                    case 13: // 特效
                        p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1f, 1.2f);
                        SpiritMenus.openEffectsMenu(p, profile);
                        break;
                    case 14: // 幻化
                        if (profile.hasAnyUnlockedSkin()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                            SpiritMenus.openSkinsMenu(p, profile);
                        } else {
                            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                            p.sendMessage("§c你尚未解锁任何幻化外观！");
                        }
                        break;
                    case 17: // 成就
                        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                        SpiritMenus.openAchievementMenu(p, profile, 1);
                        break;
                    case 30: // 主动技能
                        // 这里只是展示，或者可以添加卸载逻辑？原代码逻辑是点击33释放
                        // 但在菜单里点主动技能槽，通常不做操作，或打开技能树
                        p.sendMessage("§7请在技能树中配置主动技能。");
                        break;
                    case 31: case 32: // 快捷技能
                        int quickIndex = slot - 31;
                        List<String> quicks = profile.getQuickSkillIds();
                        if (quickIndex >= 0 && quickIndex < quicks.size()) {
                            String skillId = quicks.get(quickIndex);
                            p.closeInventory();
                            SkillManager.castSkill(p, profile, skillId);
                        } else {
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                            p.sendActionBar("§7请先在技能树中装备快捷技能");
                        }
                        break;
                    case 34: // 打开食物袋
                        p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1f, 1f);
                        SpiritMenus.openFoodBag(p, profile);
                        break;
                    case 49: // 传送石界面
                        WarpStoneModule warpModule = (WarpStoneModule) RabiSystem.getModuleManager().getModule("warp");
                        if (warpModule != null && warpModule.isEnabled()) {
                            // true 表示是从 Spirit 菜单进入
                            new WarpMenus(warpModule.getWarpManager(), warpModule.getPlugin()).openWarpMenu(p, 1, true);
                        } else {
                            p.sendMessage("§c传送石模块未启用。");
                        }
                        break;
                    case 53: // 下一页
                        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                        SpiritMenus.openMainMenu(p, profile, 2);
                        break;
                }
            }
            // P2 (第二页)
            else if (page == 2) {
                switch (slot) {
                    case 9: // 生命反哺
                        profile.setHealBackEnabled(!profile.isHealBackEnabled());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        SpiritMenus.openMainMenu(p, profile, 2);
                        break;
                    case 10: // 灵力共鸣
                        if (profile.getLevel() >= 30) {
                            profile.setResonanceEnabled(!profile.isResonanceEnabled());
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                            SpiritMenus.openMainMenu(p, profile, 2);
                        }
                        break;
                    case 11: // 灵力迸发
                        if (profile.getLevel() >= 50) {
                            profile.setBurstEnabled(!profile.isBurstEnabled());
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                            SpiritMenus.openMainMenu(p, profile, 2);
                        }
                        break;
                    case 12: // 灵魂代偿
                        if (profile.getLevel() >= 80) {
                            profile.setSoulCompensateEnabled(!profile.isSoulCompensateEnabled());
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                            SpiritMenus.openMainMenu(p, profile, 2);
                        }
                        break;
                    case 17: // 虚空引力
                        if (profile.getLevel() >= 30) {
                            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                            SpiritMenus.openGravityMenu(p, profile);
                        }
                        break;
                    case 36: // 在线时长
                        PlayTimeModule ptModule = (PlayTimeModule) RabiSystem.getModuleManager().getModule("playtime");
                        if (ptModule != null && ptModule.isEnabled()) {
                            ptModule.getManager().openLeaderboard(p, 1, true);
                        }
                        break;
                    case 37: // 摸鱼排行
                        AFKModule afkModule = (AFKModule) RabiSystem.getModuleManager().getModule("afk");
                        if (afkModule != null && afkModule.isEnabled()) {
                            afkModule.getManager().openLeaderboard(p, 1, true);
                        }
                        break;
                    case 43: // 认知干扰
                        profile.setHideOthers(!profile.isHideOthers());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        SpiritUtils.getSpiritManager().refreshVisibilityForPlayer(p);
                        SpiritMenus.openMainMenu(p, profile, 2);
                        break;
                    case 44: // 收回
                        if (p.getInventory().firstEmpty() == -1) {
                            boolean canFit = false;
                            for (ItemStack i : p.getInventory().getContents()) {
                                if (i != null && i.getType() == Material.SOUL_LANTERN && i.getAmount() < i.getMaxStackSize()) {
                                    canFit = true;
                                    break;
                                }
                            }
                            if (!canFit) {
                                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                                p.sendMessage("§c收回失败：你的背包已满，请至少腾出一个空位！");
                                return;
                            }
                        }
                        p.closeInventory();
                        SpiritUtils.getSpiritManager().despawnSpirit(p.getUniqueId());
                        profile.setSummoned(false);
                        SpiritUtils.getConfigManager().saveProfile(profile);
                        p.sendMessage("§b已将小精灵收回灵契空间。");
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 2f);
                        break;
                    case 52: // 上一页
                        p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                        SpiritMenus.openMainMenu(p, profile, 1);
                        break;
                }
            }
        }
    }

    private void handleLottery(Player p, SpiritProfile profile) {
        if (profile.getLotteryChances() > 0) {
            Allay spirit = SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId());
            if (spirit == null || !spirit.isValid()) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                p.sendMessage("§c[!] 你的小精灵不在身边，无法引导星阵。");
                return;
            }
            profile.setLotteryChances(profile.getLotteryChances() - 1);
            SpiritUtils.getConfigManager().saveProfile(profile);
            p.closeInventory();
            p.sendActionBar("§d§l✨ 仪式开始... 请静候星辰的指引。");
            new LotteryTask(p, spirit, profile).runTaskTimer(SpiritUtils.getPlugin(), 0L, 1L);
        } else {
            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            p.sendActionBar("§c[!] 你的祈愿能量不足，去多陪陪小精灵提升等级吧。");
        }
    }

    @EventHandler
    public void onEffectMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof SpiritMenus.SpiritHolder holder)) return;
        if (!"EFFECTS".equals(holder.getType())) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        int slot = e.getRawSlot();

        if (slot == 4) { // 开关
            boolean newState = !profile.isEffectsEnabled();
            profile.setEffectsEnabled(newState);
            p.playSound(p.getLocation(), newState ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
            SpiritMenus.openEffectsMenu(p, profile);
            return;
        }
        if (slot == 8) { // 返回
            p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
            SpiritMenus.openMainMenu(p, profile);
            return;
        }

        int startSlot = 18;
        List<SpiritEffectType> effects = SpiritMenus.getDisplayEffects();
        int index = slot - startSlot;

        if (index >= 0 && index < effects.size()) {
            SpiritEffectType targetType = effects.get(index);
            if (profile.isEffectUnlocked(targetType)) {
                profile.setActiveEffect(targetType);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                p.sendActionBar("§a[灵契] 已切换特效为: " + targetType.getName());
                SpiritUtils.getConfigManager().saveProfile(profile);
                SpiritMenus.openEffectsMenu(p, profile);
            } else {
                p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                String reason = switch (targetType.getSource()) {
                    case LEVEL -> "等级不足 Lv." + targetType.getRequiredLevel();
                    case LOTTERY -> "需通过 [星界祈愿] 抽取";
                    default -> "尚未解锁";
                };
                p.sendActionBar("§c[!] 无法使用: " + reason);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SpiritMenus.SpiritHolder) {
            SpiritMenus.SpiritHolder holder = (SpiritMenus.SpiritHolder) e.getInventory().getHolder();
            if ("MAIN".equals(holder.getType())) {
                // 只有 P1 的 28, 35 允许 Drag
                if (holder.getPage() == 1) {
                    for (int slot : e.getRawSlots()) {
                        if (slot < 54 && !ALLOWED_SLOTS_P1.contains(slot)) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                } else {
                    // P2 全禁
                    for (int slot : e.getRawSlots()) {
                        if (slot < 54) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SpiritMenus.SpiritHolder)) return;
        SpiritMenus.SpiritHolder holder = (SpiritMenus.SpiritHolder) e.getInventory().getHolder();
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(holder.getOwner());
        Inventory inv = e.getInventory();

        // --- 1. 处理主菜单保存 ---
        if ("MAIN".equals(holder.getType())) {
            // 只有 Page 1 有物理槽位，需要保存
            if (holder.getPage() == 1) {
                // 保存主手装备 (Slot 28)
                ItemStack item28 = inv.getItem(28);
                if (isSaveable(item28)) profile.setEquipItem(0, item28);
                else profile.setEquipItem(0, null);

                // 保存快捷投喂 (Slot 35)
                ItemStack item35 = inv.getItem(35);
                ItemStack[] fb = profile.getFoodBag();
                if (fb.length > 0) {
                    if (isSaveable(item35)) fb[0] = item35;
                    else if (item35 == null) fb[0] = null;
                    profile.setFoodBag(fb);
                }
                // 实时更新实体
                SpiritUtils.getSpiritManager().updateSpiritEquip(holder.getOwner());
            }
        }

        // --- 2. 处理背包保存 ---
        else if ("BACKPACK".equals(holder.getType())) {
            ItemStack[] contents = inv.getContents();
            ItemStack[] cleanInput = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && isGuiButton(item)) {
                    cleanInput[i] = null;
                } else {
                    cleanInput[i] = item;
                }
            }
            int reservedSlots = 0;
            int validSize = contents.length - reservedSlots;
            ItemStack[] sortedContents = sortAndStack(cleanInput, validSize);
            profile.setBackpack(sortedContents);

            if (e.getPlayer() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1f, 1.2f);
            }
            boolean isFull = true;
            for (int i = 0; i < validSize; i++) {
                if (sortedContents[i] == null || sortedContents[i].getType() == Material.AIR) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                AchievementManager.check((Player) e.getPlayer(), profile, "backpack_full");
            }
        }

        // --- 容器保存 ---
        else if (holder.getType().startsWith("VF_")) {
            Inventory vf_inv = e.getInventory();
            ItemStack[] contents = new ItemStack[3];
            contents[0] = vf_inv.getItem(0);
            contents[1] = vf_inv.getItem(1);
            contents[2] = vf_inv.getItem(2);
            String type = holder.getType().split("_")[1];
            switch (type) {
                case "FURNACE": profile.setVirtualFurnaceItems(contents); break;
                case "SMOKER": profile.setVirtualSmokerItems(contents); break;
                case "BLAST_FURNACE": profile.setVirtualBlastItems(contents); break;
            }
        }
    }

    private boolean isGuiButton(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(SpiritUtils.getPlugin(), "lsc_gui_button");
        if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return true;
        }
        if (item.getType() == Material.IRON_DOOR) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
            if (name.contains("返回") || name.contains("主界面") || name.contains("主菜单")) {
                return true;
            }
        }
        if (item.getType() == Material.STRUCTURE_VOID || item.getType() == Material.BARRIER) {
            return true;
        }
        return false;
    }

    private void handleGravityMenuClicks(Player player, SpiritProfile profile, int slot, ItemStack cursor) {
        if (slot >= 36) {
            switch (slot) {
                case 45: // 总开关
                    profile.setVoidGravityEnabled(!profile.isVoidGravityEnabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1f, 1f);
                    break;
                case 46: // 过滤开关
                    profile.setFilterEnabled(!profile.isFilterEnabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1f, 1f);
                    if (profile.isFilterEnabled()) {
                        AchievementManager.check(player, profile, "gravity_filter");
                    }
                    break;
                case 47: // 模式切换
                    profile.setWhitelistMode(!profile.isWhitelistMode());
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);
                    break;
                case 53: // 返回主界面
                    player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1.2f);
                    SpiritMenus.openMainMenu(player, profile);
                    return;
                default:
                    return;
            }
            SpiritMenus.openGravityMenu(player, profile);
            return;
        }

        if (slot >= 0 && slot < 36) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                Material mat = cursor.getType();
                if (!profile.getFilterList().contains(mat)) {
                    if (profile.getFilterList().size() < 36) {
                        profile.getFilterList().add(mat);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                        SpiritMenus.openGravityMenu(player, profile);
                    }
                }
            } else {
                if (slot < profile.getFilterList().size()) {
                    profile.getFilterList().remove(slot);
                    player.playSound(player.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.5f, 0.5f);
                    SpiritMenus.openGravityMenu(player, profile);
                }
            }
        }
    }

    private boolean isSaveable(ItemStack item) {
        return item != null && item.getType() != Material.STRUCTURE_VOID;
    }

    private void handleRename(Player p, SpiritProfile profile) {
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.NAME_TAG), 1)) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            p.sendMessage("§c[!] 你身上没有命名牌，无法进行刻印！");
            p.closeInventory();
            return;
        }
        p.closeInventory();
        new AnvilGUI.Builder()
                .plugin(SpiritUtils.getPlugin())
                .title("请输入新的名字")
                .itemLeft(new ItemStack(Material.NAME_TAG))
                .text("在此输入...")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    String input = stateSnapshot.getText();
                    if (input == null || input.trim().isEmpty() || "在此输入...".equals(input)) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("名字不能为空"));
                    }
                    if (input.matches("^[\\p{P}\\p{S}\\s]+$")) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("不能仅包含符号"));
                    }
                    String finalName = input.replace("&", "§");
                    if (isNameDuplicate(finalName, p.getUniqueId())) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("该名字已存在"));
                    }
                    p.getInventory().removeItem(new ItemStack(Material.NAME_TAG, 1));
                    profile.setName(finalName);
                    org.bukkit.entity.Allay spirit = SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId());
                    if (spirit != null && spirit.isValid()) {
                        spirit.customName(Component.text(finalName));
                    }
                    SpiritUtils.getConfigManager().saveProfile(profile);
                    AchievementManager.check(p, profile, "name_update");
                    p.playSound(p.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
                    p.sendMessage("§a[✔] 刻印成功！你的小精灵现在叫: §f" + finalName);
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .open(p);
    }

    private boolean isNameDuplicate(String newName, java.util.UUID selfUuid) {
        for (SpiritProfile sp : SpiritUtils.getSpiritManager().getLoadedProfiles().values()) {
            if (sp.getOwnerId().equals(selfUuid)) continue;
            if (sp.getName().equalsIgnoreCase(newName)) return true;
        }
        ConfigurationSection spirits = SpiritUtils.getPlugin().getConfig().getConfigurationSection("spirits");
        if (spirits != null) {
            for (String key : spirits.getKeys(false)) {
                if (key.equals(selfUuid.toString())) continue;
                String savedName = spirits.getString(key + ".name");
                if (newName.equalsIgnoreCase(savedName)) return true;
            }
        }
        return false;
    }

    private ItemStack[] sortAndStack(ItemStack[] contents, int validSize) {
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < validSize; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                items.add(contents[i]);
            }
        }
        java.util.List<ItemStack> stackedItems = new java.util.ArrayList<>();
        for (ItemStack item : items) {
            boolean merged = false;
            for (ItemStack existing : stackedItems) {
                if (existing.isSimilar(item)) {
                    int maxStack = existing.getMaxStackSize();
                    int space = maxStack - existing.getAmount();
                    if (space > 0) {
                        int toTransfer = Math.min(space, item.getAmount());
                        existing.setAmount(existing.getAmount() + toTransfer);
                        item.setAmount(item.getAmount() - toTransfer);
                        if (item.getAmount() <= 0) {
                            merged = true;
                            break;
                        }
                    }
                }
            }
            if (!merged || item.getAmount() > 0) {
                stackedItems.add(item);
            }
        }
        stackedItems.sort((a, b) -> {
            int nameCompare = a.getType().name().compareTo(b.getType().name());
            if (nameCompare != 0) return nameCompare;
            return Integer.compare(b.getAmount(), a.getAmount());
        });
        ItemStack[] result = new ItemStack[contents.length];
        for (int i = 0; i < validSize; i++) {
            if (i < stackedItems.size()) {
                result[i] = stackedItems.get(i);
            } else {
                result[i] = null;
            }
        }
        for (int i = validSize; i < contents.length; i++) {
            result[i] = contents[i];
        }
        return result;
    }

    private void handleSkillClick(Player p, SpiritProfile profile, String skillId) {
        SkillType skill = SkillType.fromId(skillId);
        if (skill == null) return;

        if (!profile.isSkillUnlocked(skillId)) {
            if (skill.getPrerequisite() != null) {
                if (!profile.isSkillUnlocked(skill.getPrerequisite())) {
                    SkillType preSkill = SkillType.fromId(skill.getPrerequisite());
                    String preName = (preSkill != null) ? preSkill.getName() : skill.getPrerequisite();
                    p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 0.5f);
                    p.sendActionBar("§c[!] 无法学习：你需要先学会前置技能 [" + preName + "]。");
                    return;
                }
            }
            if (profile.getAvailableSkillPoints() > 0) {
                profile.unlockSkill(skillId);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
                p.sendActionBar("§a成功学习技能: " + skill.getName());
                SpiritUtils.getConfigManager().saveProfile(profile);
            } else {
                p.sendActionBar("§c技能点不足！");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            }
            return;
        }

        if (skill.getType() == SkillType.Type.ACTIVE) {
            profile.setActiveSkillId(skillId);
            p.sendActionBar("§a已将主动技能设置为: " + skill.getName());
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
        } else if (skill.getType() == SkillType.Type.QUICK) {
            List<String> quicks = profile.getQuickSkillIds();
            if (quicks.contains(skillId)) {
                p.sendActionBar("§e该技能已在快捷栏中。");
            } else {
                if (quicks.size() < 2) {
                    quicks.add(skillId);
                } else {
                    quicks.remove(0);
                    quicks.add(skillId);
                }
                profile.setQuickSkillIds(quicks);
                p.sendActionBar("§a快捷技能已装备: " + skill.getName());
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1f, 1f);
            }
        }
        SpiritUtils.getConfigManager().saveProfile(profile);
    }

    @EventHandler
    public void onToolClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof SpiritMenus.SpiritHolder holder)) return;
        Player p = (Player) e.getWhoClicked();
        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(holder.getOwner());
        int slot = e.getRawSlot();

        if ("TOOLS".equals(holder.getType())) {
            e.setCancelled(true);
            if (slot == 8) {
                p.closeInventory();
                return;
            }
            if (slot == 0) p.openWorkbench(null, true);
            else if (slot == 1) SpiritMenus.openVirtualFurnace(p, profile, "FURNACE");
            else if (slot == 2) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.LOOM));
            else if (slot == 3) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.CARTOGRAPHY));
            else if (slot == 4 && e.getCurrentItem().getType() == Material.SMOKER) SpiritMenus.openVirtualFurnace(p, profile, "SMOKER");
            else if (slot == 5 && e.getCurrentItem().getType() == Material.BLAST_FURNACE) SpiritMenus.openVirtualFurnace(p, profile, "BLAST_FURNACE");
            else if (slot == 6 && e.getCurrentItem().getType() == Material.SMITHING_TABLE) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.SMITHING));
            else if (slot == 7 && e.getCurrentItem().getType() == Material.STONECUTTER) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.STONECUTTER));
        }
    }
}