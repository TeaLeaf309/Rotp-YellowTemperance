package com.TheChaYe.rotp_yellowtemperance.command;

import com.TheChaYe.rotp_yellowtemperance.RotPYellowTemperanceAddon;
import com.TheChaYe.rotp_yellowtemperance.config.YellowTemperanceConfig;
import com.TheChaYe.rotp_yellowtemperance.init.InitTags;
import com.TheChaYe.rotp_yellowtemperance.network.PacketHandler;
import com.TheChaYe.rotp_yellowtemperance.network.packets.server.ClearDisguiseCachePacket;
import com.TheChaYe.rotp_yellowtemperance.util.DisguiseManager;
import com.TheChaYe.rotp_yellowtemperance.util.GameProfileFetcher;
import com.TheChaYe.rotp_yellowtemperance.util.SkinModelUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 管理员指令：伪装皮肤缓存管理。
 *
 * <p>权限等级 2（OP）。所有指令都在 {@code /yellowtemperance cache ...} 下：</p>
 * <ul>
 *   <li>{@code status}：查看服务端缓存条数 / 外部查询开关；</li>
 *   <li>{@code list}：列出服务端缓存过的名字；</li>
 *   <li>{@code clear}：清空服务端缓存 + 通知所有客户端清空伪装皮肤缓存；</li>
 *   <li>{@code forget <name>}：删掉某个伪装目标的缓存（服务端 + 所有客户端）；</li>
 *   <li>{@code lookup <name>}：强制重新查一次并报告结果（忽略缓存）；</li>
 *   <li>{@code refresh <player>}：清掉该玩家的客户端缓存并重新走一遍查询/下发。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RotPYellowTemperanceAddon.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YellowTemperanceCommand {
    private static final int REQUIRED_PERMISSION_LEVEL = 2;

    private YellowTemperanceCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(buildRoot("yellowtemperance"));
        // 短别名，方便管理员日常使用
        event.getDispatcher().register(buildRoot("ytcache"));
    }

    private static LiteralArgumentBuilder<CommandSource> buildRoot(String literal) {
        return Commands.literal(literal)
                .requires(source -> source.hasPermission(REQUIRED_PERMISSION_LEVEL))
                .then(Commands.literal("cache")
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
                        .then(Commands.literal("list")
                                .executes(context -> list(context.getSource())))
                        .then(Commands.literal("clear")
                                .executes(context -> clearAll(context.getSource())))
                        .then(Commands.literal("forget")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> forget(context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("lookup")
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(context -> lookup(context.getSource(),
                                                StringArgumentType.getString(context, "name")))))
                        .then(Commands.literal("refresh")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> refresh(context.getSource(),
                                                StringArgumentType.getString(context, "player"))))));
    }

    private static int status(CommandSource source) {
        feedback(source, "伪装缓存状态 / disguise cache status:");
        feedback(source, "  服务端真档案 profiles = " + DisguiseManager.getProfileCacheSize());
        feedback(source, "  名字->真档案 nameLookups = " + GameProfileFetcher.getNameLookupCacheSize());
        feedback(source, "  名字->UUID uuidLookups = " + GameProfileFetcher.getUuidCacheSize());
        feedback(source, "  Mojang 外部查询 = " + (YellowTemperanceConfig.isExternalProfileLookupAllowed() ? "开启" : "关闭"));
        return 1;
    }

    private static int list(CommandSource source) {
        List<String> names = GameProfileFetcher.getCachedNames();
        if (names.isEmpty()) {
            feedback(source, "服务端档案缓存为空");
            return 0;
        }
        feedback(source, "服务端档案缓存 " + names.size() + " 条：");
        int shown = 0;
        for (String name : names) {
            if (shown >= 40) {
                feedback(source, "  ...（只显示前 40 条）");
                break;
            }
            feedback(source, "  " + name);
            shown++;
        }
        return 1;
    }

    private static int clearAll(CommandSource source) {
        GameProfileFetcher.clearCaches();
        DisguiseManager.clearProfileCache();
        PacketHandler.sendToAllClients(ClearDisguiseCachePacket.all());
        feedback(source, "已清空服务端档案缓存，并通知所有客户端清空伪装皮肤缓存");
        return 1;
    }

    private static int forget(CommandSource source, String name) {
        boolean removedFromProfiles = GameProfileFetcher.forget(name);
        boolean removedFromCache = DisguiseManager.forgetProfile(name);
        PacketHandler.sendToAllClients(ClearDisguiseCachePacket.target(name));
        if (removedFromProfiles || removedFromCache) {
            feedback(source, "已删除伪装目标 '" + name + "' 的缓存；已通知客户端清空该目标皮肤缓存");
            return 1;
        }
        feedback(source, "缓存里没有 '" + name + "'（仍已通知客户端清空该目标的皮肤缓存）");
        return 0;
    }

    private static int lookup(CommandSource source, String name) {
        feedback(source, "正在查询 '" + name + "' ...（忽略缓存，注意 Mojang API 限速）");
        GameProfileFetcher.lookupFreshAsync(name).whenComplete((profile, error) -> {
            String message;
            if (error != null) {
                message = "查询 '" + name + "' 失败: " + error.getMessage();
            } else if (profile != null && SkinModelUtil.hasSkinTexture(profile)) {
                message = "查询 '" + name + "' 成功：UUID " + profile.getId() + "，皮肤：有";
            } else if (profile != null && profile.getId() != null) {
                message = "查询 '" + name + "' 只拿到 UUID " + profile.getId() + "，皮肤：未拿到（会用默认/原皮肤）";
            } else {
                message = "查询 '" + name + "' 未找到";
            }
            deliver(source, message);
        });
        return 1;
    }

    private static int refresh(CommandSource source, String playerName) {
        MinecraftServer server = source.getServer();
        if (server == null) {
            feedback(source, "该指令只能在服务器环境下使用");
            return 0;
        }
        ServerPlayerEntity target = server.getPlayerList().getPlayerByName(playerName);
        if (target == null) {
            feedback(source, "找不到在线玩家: " + playerName);
            return 0;
        }
        CompoundNBT nbt = target.getPersistentData();
        if (!nbt.getBoolean(InitTags.YT_HAS_DISGUISE_TAG)) {
            feedback(source, playerName + " 当前没有玩家名伪装");
            return 0;
        }
        String disguiseName = nbt.getString(InitTags.YT_DISGUISE_NAME_TAG);
        if (disguiseName.isEmpty()) {
            feedback(source, playerName + " 的伪装目标名为空，无法刷新");
            return 0;
        }

        // 客户端先清（否则会静默复用旧皮肤），服务端也忘掉旧缓存，然后重新走完整查询/下发
        PacketHandler.sendToAllClients(ClearDisguiseCachePacket.player(target.getUUID()));
        PacketHandler.sendToAllClients(ClearDisguiseCachePacket.target(disguiseName));
        GameProfileFetcher.forget(disguiseName);
        DisguiseManager.forgetProfile(disguiseName);
        DisguiseManager.applyDisguise(target, disguiseName);
        feedback(source, "已刷新 " + playerName + " 的伪装 '" + disguiseName + "'，正在重新查询并下发");
        return 1;
    }

    private static void feedback(CommandSource source, String message) {
        source.sendSuccess(new StringTextComponent("[YT] " + message), false);
    }

    private static void error(CommandSource source, String message) {
        source.sendFailure(new StringTextComponent("[YT] " + message));
    }

    /**
     * 异步查询完成后必须回服务器主线程再发反馈（玩家连接队列不是线程安全的）。
     */
    private static void deliver(CommandSource source, String message) {
        MinecraftServer server = source.getServer();
        if (server != null) {
            server.execute(() -> {
                try {
                    feedback(source, message);
                } catch (Throwable ignored) {
                    // 玩家可能在查询期间断线/换维度，忽略即可
                }
            });
        } else {
            try {
                feedback(source, message);
            } catch (Throwable ignored) {
            }
        }
    }
}