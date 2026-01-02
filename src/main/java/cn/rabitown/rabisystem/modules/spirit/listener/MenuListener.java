package cn.rabitown.rabisystem.modules.spirit.listener;

import cn.rabitown.rabisystem.modules.spirit.achievement.Achievement;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritEffectType;
import cn.rabitown.rabisystem.modules.spirit.data.SpiritProfile;
import cn.rabitown.rabisystem.modules.spirit.manager.AchievementManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SignInManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SkillManager;
import cn.rabitown.rabisystem.modules.spirit.manager.SpiritDisguiseManager;
import cn.rabitown.rabisystem.modules.spirit.skill.SkillType;
import cn.rabitown.rabisystem.modules.spirit.task.LotteryTask;
import cn.rabitown.rabisystem.modules.spirit.ui.SpiritMenus;
import cn.rabitown.rabisystem.modules.spirit.utils.SpiritUtils;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Allay;
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

    // 白名单：仅允许这两个格子进行物理交互
    // 29: 主手装备槽
    // 34: 快捷投喂槽
    private final Set<Integer> ALLOWED_SLOTS = new HashSet<>(Arrays.asList(20, 25));


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
            // 如果不是 BACKPACK 或 FOOD，默认禁止拿取
            if (!type.equals("BACKPACK") && !type.equals("FOOD")) {
                e.setCancelled(true);
            }
        }

        // --- 背包界面 (BACKPACK) ---
        if ("BACKPACK".equals(type)) {
            // 点击了顶部界面 (玩家背包是 Bottom)
            if (clickedInv == e.getView().getTopInventory()) {
                // 最后一格是返回按钮，禁止拿取，点击触发跳转
                if (slot == clickedInv.getSize() - 1) {
                    e.setCancelled(true); // 锁定
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                    SpiritMenus.openMainMenu(p, profile);
                    return;
                }
            }
            // 允许其他操作继续...
        }

        // --- 等级树 (LEVEL_TREE) 逻辑 ---
        if ("LEVEL_TREE".equals(holder.getType())) {
            e.setCancelled(true);
            slot = e.getRawSlot();
//            SpiritProfile profile = plugin.getSpiritManager().getProfile(holder.getOwner());

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
                // 刷新页面 (保持当前页码)
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage());
                return;
            }

            // 2. 星光系操作 (Row 2 + Row 3)
            // 点击 View More (Slot 26)
            if (slot == 26) {
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage() + 1, holder.getShadowPage());
                return;
            }

            // 3. 暗影系操作 (Row 4 + Row 5)
            // 点击 View More (Slot 35)
            if (slot == 35) {
                p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage() + 1);
                return;
            }

            // 4. 点击技能图标 (Slot 18-25 OR 27-34)
            ItemStack item = e.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                NamespacedKey key = new NamespacedKey(SpiritUtils.getPlugin(), "skill_id");
                String skillId = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                if (skillId != null) {
                    handleSkillClick(p, profile, skillId);
                    // 操作后刷新，保持页码
                    SpiritMenus.openLevelTreeMenu(p, profile, holder.getStarPage(), holder.getShadowPage());
                }
            }
        }

        // --- 食物包界面 (FOOD) ---
        if ("FOOD".equals(type)) {
            if (clickedInv == e.getView().getTopInventory()) {
                // 1. 最后一格 (Slot 8): 返回按钮
                if (slot == 8) {
                    e.setCancelled(true);
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
                    SpiritMenus.openMainMenu(p, profile);
                    return;
                }

                // 2. 检查是否点击了未解锁的格子 (白色玻璃板)
                int unlocked = SpiritMenus.getUnlockedFoodSlots(profile.getLevel());
                if (slot >= unlocked && slot < 8) {
                    e.setCancelled(true); // 锁定
                    p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1.5f);
                    // 可以发个提示
                    p.sendActionBar(Component.text("§c此槽位尚未解锁"));
                    return;
                }
            }
        }
        // ---  虚空引力界面(GRAVITY)的处理逻辑提前 ---
        if ("GRAVITY".equals(type)) {
            if (clickedInv == e.getView().getTopInventory()) {
                handleGravityMenuClicks(p, profile, slot, e.getCursor());
            }
            return;
        }


        // 处理 SIGNIN 的部分
        if ("SIGNIN".equals(holder.getType())) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Calendar viewCal = holder.getViewCalendar();

            // 逻辑功能键处理
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

            // --- 签到与补签判定 (金块、纸张、下界之星) ---
            if (clicked.getType() == Material.GOLD_BLOCK
                    || clicked.getType() == Material.PAPER
                    || clicked.getType() == Material.NETHER_STAR) {
                try {
                    // 1. 获取纯文本（已过滤颜色代码）
                    String plainName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                            .serialize(clicked.getItemMeta().displayName());

                    // 2. 精准匹配“第”和“天”之间的数字，防止干扰
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("第\\s*(\\d+)\\s*天").matcher(plainName);

                    if (matcher.find()) {
                        int day = Integer.parseInt(matcher.group(1)); // 获取第一组数字
                        SignInManager.processSignIn(p, profile, day, viewCal);
                        SpiritMenus.openSignInMenu(p, profile, viewCal);
                    }
                } catch (Exception ex) {
                    SpiritUtils.getPlugin().getLogger().warning("签到解析失败: " + ex.getMessage());
                }
            }
            return; // 处理完 SIGNIN 直接返回
        }

        // --- C. 排行榜逻辑 ---
        if ("RANK".equals(holder.getType())) {
            if (slot == 26) {
                SpiritMenus.openSignInMenu(p, profile, Calendar.getInstance());
            }
        }

        // --- 成就界面逻辑 ---
        if ("ACHIEVEMENT".equals(holder.getType())) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            slot = e.getRawSlot();
            int currentPage = holder.getPage();
            SpiritMenus.FilterType currentFilter = holder.getFilterType();

            // 翻页与返回
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

            // --- 成就图标点击 ---
            else if (slot >= 18 && slot < 54) {
                // 1. 直接调用 SpiritMenus 的公开方法获取当前列表（无需重复写排序逻辑）
                List<Achievement> sortedList = SpiritMenus.getSortedAchievements(profile, currentFilter);

                // 2. 计算索引
                int pageSize = 36;
                int startIndex = (currentPage - 1) * pageSize;
                int index = startIndex + (slot - 18);

                // 3. 校验并处理
                if (index >= 0 && index < sortedList.size()) {
                    Achievement ach = sortedList.get(index);

                    boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
                    boolean claimed = profile.isClaimed(ach.getId());

                    if (unlocked && !claimed) {
                        AchievementManager.claimReward(p, profile, ach);
                        // 领取后刷新界面，保持页码和分类不变
                        SpiritMenus.openAchievementMenu(p, profile, currentPage, currentFilter);
                    } else if (!unlocked) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        p.sendActionBar("§c尚未达成此成就，继续加油喵！");
                    } else {
                        // 已领取
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                }
            }
            return;
        }


        // --- 幻化菜单逻辑 (SKINS) ---
        if ("SKINS".equals(holder.getType())) {
            e.setCancelled(true);
            // 1. 处理返回键
            if (e.getRawSlot() == 26) {
                SpiritMenus.openMainMenu(p, profile);
                return;
            }

            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // 2. 【关键】定义并读取 skinId
            // 必须先获取 ItemMeta，才能读取 PDC 数据
            if (!clickedItem.hasItemMeta()) return;

            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(SpiritUtils.getPlugin(), "skin_id");
            String skinId = clickedItem.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);

            // 如果 skinId 为 null，说明点到了背景板或者没有数据的物品，直接忽略
            if (skinId == null) return;

            // 3. 校验解锁状态 (这里 skinId 已经定义了，不会报错了)
            if (!profile.isSkinUnlocked(skinId)) {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                p.sendMessage("§c该外观尚未解锁！");
                return;
            }

            // 4. 执行切换
            profile.setCurrentSkin(skinId);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            p.sendActionBar("§d✨ 幻化形态已更新！");

            // 5. 实时更新伪装 (如果安装了插件)
            org.bukkit.entity.Allay spirit = SpiritUtils.getSpiritManager().getActiveSpirits().get(p.getUniqueId());
            if (spirit != null && org.bukkit.Bukkit.getPluginManager().isPluginEnabled("LibsDisguises")) {
                SpiritDisguiseManager.updateDisguise(spirit, profile);
            }

            // 6. 刷新菜单
            SpiritMenus.openSkinsMenu(p, profile);
            SpiritUtils.getConfigManager().saveProfile(profile);
            return;
        }



        // --- 以下为主菜单(MAIN)的逻辑 ---
        // 背包(BACKPACK) 和 食物袋(FOOD) 是允许自由存取的，所以这里只拦截 MAIN
        if (!"MAIN".equals(holder.getType())) return;
        e.setCancelled(true);

        // 点击底部玩家背包：放行 (禁止Shift)
        if (clickedInv == e.getView().getBottomInventory()) {
            if (!e.isShiftClick()) {
                e.setCancelled(false);
            }
            return;
        }

        // 点击顶部菜单
        if (clickedInv == e.getView().getTopInventory()) {
            // 检查白名单
            if (ALLOWED_SLOTS.contains(slot)) {
                ItemStack item = e.getCurrentItem();
                if (item != null && item.getType() == Material.STRUCTURE_VOID) {
                    p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                    p.sendActionBar("§c该槽位等级不足，尚未解锁！");
                } else {
                    e.setCancelled(false);
                }
                return;
            }

            switch (slot) {
                // --- Row 0: 技能与杂项 ---
                case 0: // 生命反哺
                    boolean healState = !profile.isHealBackEnabled();
                    profile.setHealBackEnabled(healState);
                    if (healState) {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
                        p.sendActionBar("§a§l✨ 生命反哺已开启 §7- 小精灵将自动消耗生命为你治疗。");
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 0.5f);
                        p.sendActionBar("§7[!] 生命反哺已关闭。");
                    }
                    SpiritMenus.openMainMenu(p, profile);
                    break;
                case 1: // 灵力共鸣
                    if (profile.getLevel() >= 30) {
                        boolean resState = !profile.isResonanceEnabled();
                        profile.setResonanceEnabled(resState);
                        if (resState) {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.5f);
                            p.sendActionBar("§b§l⚔ 灵力共鸣已建立 §7- 小精灵的灵力将化作你的锋芒。");
                        } else {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
                            p.sendActionBar("§7[!] 灵力共鸣已关闭。");
                        }
                        SpiritMenus.openMainMenu(p, profile);
                    } else {
                        // 等级不足的提示
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendActionBar("§c[!] 需要 Lv.30 才能解锁灵力共鸣。");
                    }
                    break;
                case 2: // 灵力迸发
                    if (profile.getLevel() >= 50) {
                        boolean burstState = !profile.isBurstEnabled();
                        profile.setBurstEnabled(burstState);
                        if (burstState) {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
                            p.sendActionBar("§6§l💥 灵力迸发已就绪 §7- 蓄积星光，予以痛击。");
                        } else {
                            p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                            p.sendActionBar("§7[!] 灵力迸发已关闭。");
                        }
                        SpiritMenus.openMainMenu(p, profile);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendActionBar("§c[!] 灵力迸发需要等级 Lv.50 解锁");
                    }
                    break;
                case 3: // 灵魂代偿
                    if (profile.getLevel() >= 80) {
                        boolean newState = !profile.isSoulCompensateEnabled();
                        profile.setSoulCompensateEnabled(newState);
                        if (newState) {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
                            p.sendActionBar("§d§l✨ 灵魂代偿已开启 §7- 小精灵将在致命时刻守护你。");
                        } else {
                            p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.5f);
                            p.sendActionBar("§7[!] 灵魂代偿已关闭。");
                        }
                        SpiritMenus.openMainMenu(p, profile);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendMessage("§c[!] 灵魂代偿需要等级 Lv.80 解锁");
                    }
                    break;
                case 5: // 在线排行榜
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    p.closeInventory(); // 先关闭菜单
                    p.performCommand("rpt"); // 执行指令
                    break;
                case 8: // 一键收回
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

                case 6:
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
                    SpiritMenus.openAchievementMenu(p, profile, 1); // 打开第一页
                    break;

                case 7: // 认知干扰开关
                    boolean newState = !profile.isHideOthers();
                    profile.setHideOthers(newState);

                    if (newState) {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                        p.sendActionBar("§b[认知干扰] §7力场已展开，其他人的小精灵已从你的视野中隐去。");
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.5f);
                        p.sendActionBar("§a[认知干扰] §7力场已解除，你可以看到大家的小精灵了。");
                    }

                    // 立即执行可见性刷新
                    SpiritUtils.getSpiritManager().refreshVisibilityForPlayer(p);
                    SpiritMenus.openMainMenu(p, profile);
                    break;
                // --- Row 1

                // --- Row 2: 装备/食物 ---
                case 22: // 签到
                    p.playSound(p.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 1f, 1f);
                    SpiritMenus.openSignInMenu(p, profile, java.util.Calendar.getInstance());
                    break;

                case 26: // 自动饮食开关 (原 31)
                    if (profile.getLevel() >= 10) {
                        profile.setAutoEat(!profile.isAutoEat());
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        SpiritMenus.openMainMenu(p, profile);
                    }
                    break;
                case 24: // 打开食物袋 (原 33)
                    p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_INSERT, 1f, 1f);
                    SpiritMenus.openFoodBag(p, profile);
                    break;

                case 34: // 快捷技能 1
                case 35: // 快捷技能 2
                    int quickIndex = slot - 34; // 34->0, 35->1
                    List<String> quicks = profile.getQuickSkillIds();
                    if (quickIndex >= 0 && quickIndex < quicks.size()) {
                        String skillId = quicks.get(quickIndex);
                        // 关闭菜单以释放技能 (特别是那些需要视野的技能)
                        p.closeInventory();
                        SkillManager.castSkill(p, profile, skillId);
                    } else {
                        // 空槽位点击，播放提示音或提示去技能树装备
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        p.sendActionBar("§7请先在技能树中装备快捷技能");
                    }
                    break;
                case 38: // 真名刻印
                    handleRename(p, profile);
                    break;
                // --- Row 4: 核心功能 ---
                case 39: // 虚空引力 (原 12)
                    if (profile.getLevel() >= 30) {
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                        SpiritMenus.openGravityMenu(p, profile);
                    } else {
                        p.sendMessage("§c[!] 虚空引力需要等级 Lv.30 解锁");
                    }
                    break;
                case 40: // 自定义特效
                    p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1f, 1.2f);
                    SpiritMenus.openEffectsMenu(p, profile);
                    break;
                case 41: // 幻化外观
                    if (profile.hasAnyUnlockedSkin()) {
                        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
                        SpiritMenus.openSkinsMenu(p, profile);
                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendMessage("§c你尚未解锁任何幻化外观！"); // 加个提示更友好
                    }
                    break;
                case 44: // 等级树按钮
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    SpiritMenus.openLevelTreeMenu(p, profile);
                    break;

                // --- Row 5: 背包 ---
                case 49: // 打开背包 (原 4)
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    SpiritMenus.openBackpack(p, profile);
                    break;

                case 53:
                    if (profile.getLotteryChances() > 0) {
                        Allay spirit = SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId());

                        // 校验：小精灵必须在场且活着才能举行仪式
                        if (spirit == null || !spirit.isValid()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            p.sendMessage("§c[!] 你的小精灵不在身边，无法引导星阵。");
                            return;
                        }

                        // 1. 扣除次数并保存
                        profile.setLotteryChances(profile.getLotteryChances() - 1);
                        SpiritUtils.getConfigManager().saveProfile(profile);

                        // 2. 关闭 UI 并开始仪式
                        p.closeInventory();
                        p.sendActionBar("§d§l✨ 仪式开始... 请静候星辰的指引。");

                        // 3. 启动抽奖任务 (假设你已创建此 Task)
                        new LotteryTask(p, spirit, profile).runTaskTimer(SpiritUtils.getPlugin(), 0L, 1L);

                    } else {
                        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
                        p.sendActionBar("§c[!] 你的祈愿能量不足，去多陪陪小精灵提升等级吧。");
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onEffectMenuClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        // 检查 Holder 类型，更加安全
        if (!(e.getView().getTopInventory().getHolder() instanceof SpiritMenus.SpiritHolder holder)) return;
        if (!"EFFECTS".equals(holder.getType())) return;

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        SpiritProfile profile = SpiritUtils.getSpiritManager().getProfile(p.getUniqueId());
        int slot = e.getRawSlot();

        // 1. 处理功能键
        if (slot == 4) { // 开关
            boolean newState = !profile.isEffectsEnabled();
            profile.setEffectsEnabled(newState);
            p.playSound(p.getLocation(), newState ? Sound.BLOCK_NOTE_BLOCK_CHIME : Sound.BLOCK_LEVER_CLICK, 1f, 1.2f);
            SpiritMenus.openEffectsMenu(p, profile); // 刷新
            return;
        }
        if (slot == 8) { // 返回
            p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 1f, 1f);
            SpiritMenus.openMainMenu(p, profile);
            return;
        }

        // 2. 处理特效点击 (自动映射)
        // 起始 Slot 是 18
        int startSlot = 18;
        List<SpiritEffectType> effects = SpiritMenus.getDisplayEffects();

        // 计算点击的是第几个特效
        int index = slot - startSlot;

        // 检查索引有效性
        if (index >= 0 && index < effects.size()) {
            SpiritEffectType targetType = effects.get(index);

            // 检查是否解锁
            if (profile.isEffectUnlocked(targetType)) {
                // 激活特效
                profile.setActiveEffect(targetType);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                p.sendActionBar("§a[灵契] 已切换特效为: " + targetType.getName());

                // 实时保存并刷新界面
                SpiritUtils.getConfigManager().saveProfile(profile);
                SpiritMenus.openEffectsMenu(p, profile);
            } else {
                // 未解锁提示
                p.playSound(p.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);

                // 根据来源提示不同信息
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
                for (int slot : e.getRawSlots()) {
                    if (slot < 54 && !ALLOWED_SLOTS.contains(slot)) {
                        e.setCancelled(true);
                        return;
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
            // 保存主手装备 (原 29 -> 现 20)
            ItemStack item20 = inv.getItem(20);
            if (isSaveable(item20)) profile.setEquipItem(0, item20);
            else profile.setEquipItem(0, null);

            // 保存快捷投喂 (原 34 -> 现 25)
            ItemStack item25 = inv.getItem(25);
            ItemStack[] fb = profile.getFoodBag();
            if (fb.length > 0) {
                if (isSaveable(item25)) fb[0] = item25;
                else if (item25 == null) fb[0] = null;
                profile.setFoodBag(fb);
            }

            // 实时更新实体手中的物品
            SpiritUtils.getSpiritManager().updateSpiritEquip(holder.getOwner());
        }

        // --- 2. 处理背包保存 ---
        else if ("BACKPACK".equals(holder.getType())) {
            ItemStack[] contents = inv.getContents();

            // =========================================================
            // 步骤 1: 【预清洗】
            // 在排序前，先把所有的 GUI 按钮变成 null。
            // 这样做的目的是防止按钮参与排序算法（避免按钮占了物品的位置）。
            // =========================================================
            ItemStack[] cleanInput = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item != null && isGuiButton(item)) {
                    cleanInput[i] = null; // 遇到按钮，直接视为空气
                } else {
                    cleanInput[i] = item; // 普通物品保留
                }
            }

            // ⚙️ 设置保留槽位 (保持原有逻辑)
            int reservedSlots = 0;
            int validSize = contents.length - reservedSlots;

            // ✨ 执行自动整理！
            // 传入已经剔除了按钮的 cleanInput 数组
            // 你的 sortAndStack 方法接收 ItemStack[] 并返回 ItemStack[]，这样就不会报错了
            ItemStack[] sortedContents = sortAndStack(cleanInput, validSize);

            // =========================================================
            // 步骤 2: 【保存数据】
            // sortedContents 已经是排序好且不含按钮的干净数据了，直接保存
            // =========================================================
            profile.setBackpack(sortedContents);

            // 播放整理声音
            if (e.getPlayer() instanceof Player p) {
                p.playSound(p.getLocation(), Sound.ITEM_BUNDLE_REMOVE_ONE, 1f, 1.2f);
            }

            // --- 🏆 满载而归成就检测 ---
            boolean isFull = true;
            for (int i = 0; i < validSize; i++) {
                // 检查整理后的背包是否有空位
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
            contents[0] = vf_inv.getItem(0); // Input
            contents[1] = vf_inv.getItem(1); // Fuel
            contents[2] = vf_inv.getItem(2); // Output

            String type = holder.getType().split("_")[1];
            switch (type) {
                case "FURNACE": profile.setVirtualFurnaceItems(contents); break;
                case "SMOKER": profile.setVirtualSmokerItems(contents); break;
                case "BLAST_FURNACE": profile.setVirtualBlastItems(contents); break;
            }
            // 不需要立即写盘，等自动保存或下线
        }
    }

    /**
     * 辅助方法：判断一个物品是否为 GUI 功能按钮
     * (防止把菜单按钮存进玩家数据)
     */
    private boolean isGuiButton(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;

        // 判定方式 1: 检查是否有特殊的 PDC 标签 (最稳妥，推荐)
        // 需配合 SpiritMenus 修改，见下文
        NamespacedKey key = new NamespacedKey(SpiritUtils.getPlugin(), "lsc_gui_button");
        if (item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
            return true;
        }

        // 判定方式 2: 简单粗暴的特征判断 (懒人补丁)
        // 如果是铁门，且名字里包含 "返回"，就认为是按钮，不保存
        if (item.getType() == Material.IRON_DOOR) {
            String name = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                    .serialize(item.getItemMeta().displayName());
            if (name.contains("返回") || name.contains("主界面") || name.contains("主菜单")) {
                return true;
            }
        }

        // 判定方式 3: 如果是结构空位或屏障 (通常也是按钮)
        if (item.getType() == Material.STRUCTURE_VOID || item.getType() == Material.BARRIER) {
            return true;
        }

        return false;
    }

    /**
     * 处理虚空引力界面的点击逻辑 (不依赖 event 对象)
     * 保护最下面两行 (Slot 36-53) 的 UI 不被玩家拿取
     */
    private void handleGravityMenuClicks(Player player, SpiritProfile profile, int slot, ItemStack cursor) {
        // --- 保护区与功能区：Slot 36-53 ---
        if (slot >= 36) {
            switch (slot) {
                case 45: // 总开关
                    profile.setVoidGravityEnabled(!profile.isVoidGravityEnabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1f, 1f);
                    break;
                case 46: // 过滤开关
                    profile.setFilterEnabled(!profile.isFilterEnabled());
                    player.playSound(player.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1f, 1f);
                    // 触发成就检查：开启过滤
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
                    return; // 点击了黑色或白色玻璃板，不做任何事
            }
            // 刷新当前菜单显示最新状态
            SpiritMenus.openGravityMenu(player, profile);
            return;
        }

        // --- 编辑区：Slot 0-35 ---
        if (slot >= 0 && slot < 36) {
            if (cursor != null && cursor.getType() != Material.AIR) {
                // 拿着物品点击：添加
                Material mat = cursor.getType();
                if (!profile.getFilterList().contains(mat)) {
                    if (profile.getFilterList().size() < 36) {
                        profile.getFilterList().add(mat);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                        SpiritMenus.openGravityMenu(player, profile);
                    }
                }
            } else {
                // 空手点击：移除
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

    /**
     * 处理重命名逻辑 (AnvilGUI)
     */
    private void handleRename(Player p, SpiritProfile profile) {
        // 1. 检查是否有命名牌
        if (!p.getInventory().containsAtLeast(new ItemStack(Material.NAME_TAG), 1)) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            p.sendMessage("§c[!] 你身上没有命名牌，无法进行刻印！");
            p.closeInventory();
            return;
        }

        // 关闭当前菜单，打开铁砧
        p.closeInventory();

        new AnvilGUI.Builder()
                .plugin(SpiritUtils.getPlugin())
                .title("请输入新的名字")
                .itemLeft(new ItemStack(Material.NAME_TAG)) // 左侧放入命名牌图标
                .text("在此输入...") // 预设文本
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }

                    String input = stateSnapshot.getText();

                    // --- 校验 A: 判空 ---
                    if (input == null || input.trim().isEmpty() || "在此输入...".equals(input)) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("名字不能为空"));
                    }

                    // --- 校验 B: 纯符号拦截 ---
                    // 正则含义：字符串中仅包含 标点(P)、符号(S) 或 空白字符(\s)
                    if (input.matches("^[\\p{P}\\p{S}\\s]+$")) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("不能仅包含符号"));
                    }

                    // 转换颜色代码 (支持 & 转 §)
                    String finalName = input.replace("&", "§");

                    // --- 校验 C: 重复名检测 ---
                    if (isNameDuplicate(finalName, p.getUniqueId())) {
                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("该名字已存在"));
                    }

                    // === 校验通过，执行逻辑 ===

                    // 1. 消耗物品
                    p.getInventory().removeItem(new ItemStack(Material.NAME_TAG, 1));

                    // 2. 更新档案
                    profile.setName(finalName);

                    // 3. 如果小精灵在场，实时更新头顶名称
                    org.bukkit.entity.Allay spirit = SpiritUtils.getSpiritManager().getSpiritEntity(p.getUniqueId());
                    if (spirit != null && spirit.isValid()) {
                        spirit.customName(Component.text(finalName));
                    }

                    // 4. 保存数据
                    SpiritUtils.getConfigManager().saveProfile(profile);

                    // 5. 触发成就检查
                    AchievementManager.check(p, profile, "name_update");

                    // 6. 反馈
                    p.playSound(p.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
                    p.sendMessage("§a[✔] 刻印成功！你的小精灵现在叫: §f" + finalName);

                    // 关闭界面
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .open(p);
    }

    /**
     * 检查名字是否重复 (检查所有在线 + 离线数据)
     */
    private boolean isNameDuplicate(String newName, java.util.UUID selfUuid) {
        // 1. 检查在线玩家缓存 (SpiritManager)
        for (SpiritProfile sp : SpiritUtils.getSpiritManager().getLoadedProfiles().values()) {
            if (sp.getOwnerId().equals(selfUuid)) continue; // 跳过自己
            if (sp.getName().equalsIgnoreCase(newName)) return true;
        }

        // 2. 检查离线数据 (data.yml)
        ConfigurationSection spirits = SpiritUtils.getPlugin().getConfig().getConfigurationSection("spirits");
        if (spirits != null) {
            for (String key : spirits.getKeys(false)) {
                if (key.equals(selfUuid.toString())) continue; // 跳过自己

                String savedName = spirits.getString(key + ".name");
                if (newName.equalsIgnoreCase(savedName)) return true;
            }
        }
        return false;
    }

    /**
     * 🎒 自动整理算法：堆叠 + 排序
     * @param contents 原始背包数组
     * @param validSize 有效整理区域的大小（防止打乱末尾的固定功能按钮）
     * @return 整理后的新数组
     */
    private ItemStack[] sortAndStack(ItemStack[] contents, int validSize) {
        // 1. 提取有效区域内的所有非空物品
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < validSize; i++) {
            if (contents[i] != null && contents[i].getType() != Material.AIR) {
                items.add(contents[i]);
            }
        }

        // 2. 执行堆叠合并 (Stacking)
        java.util.List<ItemStack> stackedItems = new java.util.ArrayList<>();
        for (ItemStack item : items) {
            boolean merged = false;
            // 尝试合并到已有堆叠中
            for (ItemStack existing : stackedItems) {
                if (existing.isSimilar(item)) {
                    int maxStack = existing.getMaxStackSize();
                    int space = maxStack - existing.getAmount();
                    if (space > 0) {
                        int toTransfer = Math.min(space, item.getAmount());
                        existing.setAmount(existing.getAmount() + toTransfer);
                        item.setAmount(item.getAmount() - toTransfer);
                        // 如果当前物品被合并完了，标记完成
                        if (item.getAmount() <= 0) {
                            merged = true;
                            break;
                        }
                    }
                }
            }
            // 如果没被完全合并（还有剩余），则作为新的一格加入
            if (!merged || item.getAmount() > 0) {
                stackedItems.add(item);
            }
        }

        // 3. 执行排序 (Sorting)
        // 规则：按材质英文名 A-Z 排序，同类物品数量多的排前面
        stackedItems.sort((a, b) -> {
            int nameCompare = a.getType().name().compareTo(b.getType().name());
            if (nameCompare != 0) return nameCompare;
            return Integer.compare(b.getAmount(), a.getAmount());
        });

        // 4. 重组数组
        ItemStack[] result = new ItemStack[contents.length];

        // 4.1 填入整理好的物品
        for (int i = 0; i < validSize; i++) {
            if (i < stackedItems.size()) {
                result[i] = stackedItems.get(i);
            } else {
                result[i] = null; // 没东西了填空
            }
        }

        // 4.2 还原末尾的固定按钮 (如果有)
        for (int i = validSize; i < contents.length; i++) {
            result[i] = contents[i];
        }

        return result;
    }

    private int getScore(SpiritProfile profile, Achievement ach) {
        boolean unlocked = profile.getUnlockedAchievements().contains(ach.getId());
        boolean claimed = profile.isClaimed(ach.getId());
        if (unlocked && !claimed) return 0;
        if (!unlocked) return 1;
        return 2;
    }

    private void handleSkillClick(Player p, SpiritProfile profile, String skillId) {
        SkillType skill = SkillType.fromId(skillId);
        if (skill == null) return;

        // A. 学习
        if (!profile.isSkillUnlocked(skillId)) {
            // 检查前置技能
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

        // B. 装备/替换
        if (skill.getType() == SkillType.Type.ACTIVE) {
            profile.setActiveSkillId(skillId);
            p.sendActionBar("§a已将主动技能设置为: " + skill.getName());
            p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
        } else if (skill.getType() == SkillType.Type.QUICK) {
            List<String> quicks = profile.getQuickSkillIds();
            if (quicks.contains(skillId)) {
                // 已存在，可以考虑实现卸下功能？或者这里仅仅提示
                // 需求说 "已学习的技能再次点击将会添加或替换快捷技能"
                // 既然已在，我们不做操作，或者移到第一位？
                p.sendActionBar("§e该技能已在快捷栏中。");
            } else {
                if (quicks.size() < 2) {
                    quicks.add(skillId);
                } else {
                    quicks.remove(0); // 挤掉第一个
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

        // 1. 处理便携工坊主菜单 (TOOLS)
        if ("TOOLS".equals(holder.getType())) {
            e.setCancelled(true);
            if (slot == 8) { // 关闭
                p.closeInventory();
                return;
            }
            // 基础功能
            if (slot == 0) p.openWorkbench(null, true);
            else if (slot == 1) SpiritMenus.openVirtualFurnace(p, profile, "FURNACE");
            else if (slot == 2) p.openMerchant(Bukkit.createMerchant("虚拟织布机"), true); // 织布机API较少，通常直接openLoom(1.14+)
                // 注：Spigot API 没有直接 openLoom/openCartography，通常通过 openInventory 打开特定类型
                // 这里的实现需要稍微变通，Purpur 1.21 应该支持 InventoryType
            else if (slot == 2) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.LOOM));
            else if (slot == 3) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.CARTOGRAPHY));

                // 进阶功能
            else if (slot == 4 && e.getCurrentItem().getType() == Material.SMOKER) SpiritMenus.openVirtualFurnace(p, profile, "SMOKER");
            else if (slot == 5 && e.getCurrentItem().getType() == Material.BLAST_FURNACE) SpiritMenus.openVirtualFurnace(p, profile, "BLAST_FURNACE");
            else if (slot == 6 && e.getCurrentItem().getType() == Material.SMITHING_TABLE) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.SMITHING));
            else if (slot == 7 && e.getCurrentItem().getType() == Material.STONECUTTER) p.openInventory(Bukkit.createInventory(p, org.bukkit.event.inventory.InventoryType.STONECUTTER));
        }

        // 2. 处理虚拟熔炉关闭保存 (VF_...)
        // 不需要在这里处理点击，允许玩家自由拿取
        // 只需要在关闭时保存数据
    }
}