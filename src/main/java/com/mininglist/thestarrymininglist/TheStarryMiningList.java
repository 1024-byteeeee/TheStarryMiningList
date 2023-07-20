package com.mininglist.thestarrymininglist;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.fabricmc.api.ModInitializer;
//#if MC < 11900
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
//#else
//$$ import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//#endif
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardPlayerScore;
import net.minecraft.server.command.ServerCommandSource;
//#if MC >= 11900
//$$ import net.minecraft.text.Text;
//#else
import net.minecraft.text.LiteralText;
//#endif
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.io.File;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;


public class TheStarryMiningList implements ModInitializer {
    @Override
    public void onInitialize() {
        // 注册命令以切换计分板的可见/隐藏状态
        //#if MC < 11900
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(literal("TheStarryMiningListSwitch")
                //#else
                //$$ CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(literal("TheStarryMiningListSwitch")
                //#endif
                .executes(context -> {
                    isScoreboardVisible = !isScoreboardVisible;
                    if (isScoreboardVisible) {
                        this.mScoreboard.setObjectiveSlot(1, this.mScoreboardObj); // 显示计分板
                        context.getSource().getPlayer().sendMessage(Text.of("已启用计分板"),true);
                    } else {
                        this.mScoreboard.setObjectiveSlot(1, null); // 隐藏计分板
                        context.getSource().getPlayer().sendMessage(Text.of("已关闭计分板"),true);
                    }
                    return 1;
                })));

        // 注册命令以更改计分板的显示名称
        //#if MC < 11900
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            //#else
        //$$ CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            //#endif
            LiteralCommandNode<ServerCommandSource> command = literal("setScoreboardDisplayName")
                    .then(argument("scoreboardName", StringArgumentType.word())
                            .then(argument("displayName", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String scoreboardName = StringArgumentType.getString(context, "scoreboardName");
                                        String displayName = StringArgumentType.getString(context, "displayName");
                                        setScoreboardDisplayName(scoreboardName, displayName, context.getSource());
                                        return 1;
                                    })
                            )
                    ).build();
            dispatcher.getRoot().addChild(command);
        });

        FabricLoader loader = FabricLoader.getInstance();//获取加载器的实例
        File config_file_path = loader.getConfigDir().toFile();//获取配置文件
        Config config = new Config(config_file_path.getPath());//读取配置文件
        String name = config.GetValue("ScoreboardName");//获取计分板的名字
        String disPlayName = config.GetValue("ScoreboardDisplayName");//获取计分板显示的名字
        CreateScoreboard(name, disPlayName);//创建计分板
        HookPlayerBreakBlockEvent();//设置玩家破坏方块事件的回调
    }

    private Scoreboard mScoreboard; //计分板对象
    private ScoreboardObjective mScoreboardObj; //计分板的计分对象
    private boolean isScoreboardVisible = false; //计分板的开关状态

    //更改计分板的显示名称
    private void setScoreboardDisplayName(String scoreboardName, String displayName, ServerCommandSource source) throws CommandSyntaxException {
        //#if MC < 11900
        Scoreboard scoreboard = source.getMinecraftServer().getScoreboard();
        //#else
        //$$ Scoreboard scoreboard = source.getServer().getScoreboard();
        //#endif
        ScoreboardObjective objective = scoreboard.getNullableObjective(scoreboardName);
        if (objective != null) {
            //#if MC < 11900
            objective.setDisplayName(new LiteralText(displayName));
            //#else
            //$$ objective.setDisplayName(Text.literal(displayName));
            //#endif
        }
        scoreboard.setObjectiveSlot(1, objective);
        source.getPlayer().sendMessage(Text.of(
                "计分板 [" + scoreboardName + "] 显示名称已更改为: [" + displayName + "]"
        ), true);
    }

    //创建计分板
    private void CreateScoreboard(final String name, final String display_name) {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.mScoreboard = Objects.requireNonNull(server.getWorld(World.OVERWORLD)).getScoreboard();//获取世界的计分板
            this.mScoreboardObj = mScoreboard.getObjective(name);//获取服务器的计分板对象
            if (mScoreboardObj == null) {//判断是否为空对象
                //#if MC < 11900
                this.mScoreboardObj = mScoreboard.addObjective(name, ScoreboardCriterion.DUMMY, new LiteralText(display_name),
                        //#else
                        //$$ this.mScoreboardObj = mScoreboard.addObjective(name, ScoreboardCriterion.DUMMY, Text.literal(display_name),
                        //#endif
                        ScoreboardCriterion.RenderType.INTEGER);
                this.mScoreboard.setObjectiveSlot(1, null);
            }
        });
    }

    //设置玩家破坏方块事件的回调
    private void HookPlayerBreakBlockEvent() {
        PlayerBlockBreakEvents.AFTER.register(((world, player, pos, state, blockEntity) -> {
            ScoreboardPlayerScore score = this.mScoreboard.getPlayerScore(player.getName().getString(),
                    this.mScoreboardObj);//获取玩家计分对象
            int player_score = score.getScore();//获取玩家当前的计分数
            player_score++;//分数递增
            score.setScore(player_score);//更新玩家的分数
        }));
    }
}