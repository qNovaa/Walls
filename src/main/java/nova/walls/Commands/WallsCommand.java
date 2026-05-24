package nova.walls.Commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import nova.walls.Controllers.GameController;
import nova.walls.Models.Wall;
import nova.walls.Walls;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WallsCommand {

    private static Walls plugin;

    public static LiteralCommandNode<CommandSourceStack> createCommand(Walls plugin) {
        WallsCommand.plugin = plugin;
        return Commands.literal("walls")
                .then(Commands.literal("start")
                        .executes(WallsCommand::start)
                )
                .then(Commands.literal("setup")
                        .executes(WallsCommand::setup)
                )
                .then(Commands.literal("wall")
                        .then(Commands.literal("create")
                                .executes(WallsCommand::createWall)
                                .then(Commands.argument("location", DoubleArgumentType.doubleArg())
                                        .then(Commands.argument("distance", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("orientation", BoolArgumentType.bool())
                                                        .executes(WallsCommand::createWall)
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("remove")
                                .executes(WallsCommand::removeWall)
                        )
                )
                .then(Commands.literal("lobby")
                        .executes(WallsCommand::lobby)
                )
                .then(Commands.literal("anchor")
                        .executes(WallsCommand::anchor)
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(WallsCommand::anchor)
                        )
                )
                .then(Commands.literal("spawn")
                        .executes(WallsCommand::spawn)
                        .then(Commands.argument("team", StringArgumentType.word())
                                .executes(WallsCommand::spawn)
                        )
                )
                .then(Commands.literal("stop")
                        .executes(WallsCommand::stop)
                )
                .build();
    }

    public static int lobby(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendRichMessage("<green>[Walls] Setting up lobby...");
        Walls.gameController.lobby();
        return 1;
    }

    public static int start(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendRichMessage("<green>[Walls] Starting game...");
        Walls.gameController.start();
        return 1;
    }

    public static int setup(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Walls.gameController.setup();
        sender.sendRichMessage("<green>[Walls] Game setup complete");
        return 1;
    }

    public static int createWall(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (Walls.gameController.getWall() != null) {
            sender.sendRichMessage("<red>[Walls] A wall already exists");
            return 1;
        }

        double loc;
        int distance;
        boolean orienation;
        try {
            loc = DoubleArgumentType.getDouble(ctx, "location");
        } catch (IllegalArgumentException e) {
            loc = Walls.WALL_LOCATION;
        }
        try {
            distance = IntegerArgumentType.getInteger(ctx, "distance");
        } catch (IllegalArgumentException e) {
            distance = Walls.WALL_DISTANCE;
        }
        try {
            orienation = BoolArgumentType.getBool(ctx, "orientation");
        } catch (IllegalArgumentException e) {
            orienation = false;
        }

        Walls.gameController.setWall(new Wall(plugin, Walls.WALL_MATERIAL, (int) loc, distance, orienation ? Wall.Orientation.EASTWEST : Wall.Orientation.NORTHSOUTH));
        Walls.gameController.getWall().createWall();

        sender.sendRichMessage("<green>[Walls] Wall created");

        return 1;
    }

    public static int removeWall(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();

        if (Walls.gameController.getWall() == null) {
            sender.sendRichMessage("<red>[Walls] No wall exists");
            return 1;
        }

        Walls.gameController.getWall().restoreWall();
        Walls.gameController.setWall(null);
        sender.sendRichMessage("<green>[Walls] Wall removed");

        return 1;
    }

    public static int anchor(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("<red>[Walls] Only players can get anchors");
            return 1;
        }
        if (Walls.gameController.getStatus() != GameController.Status.ACTIVE) {
            sender.sendRichMessage("<red>[Walls] Game is not active. Game must be active to give an anchor");
            return 1;
        }
        String team;
        try {
            team = StringArgumentType.getString(ctx, "team");
        } catch (IllegalArgumentException e) {
            if (Walls.gameController.getTeam1().hasPlayer(player)) team = "red";
            else if (Walls.gameController.getTeam2().hasPlayer(player)) team = "blue";
            else {
                sender.sendRichMessage("<red>[Walls] You are not on a team, so you must specify one");
                return 1;
            }
        }
        if (team.equals("red")) {
            Walls.gameController.getTeam1Anchor().giveRespawnAnchor(player);
        }
        if (team.equals("blue")) {
            Walls.gameController.getTeam2Anchor().giveRespawnAnchor(player);
        }

        return 1;
    }

    public static int spawn(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player player)) {
            sender.sendRichMessage("<red>[Walls] Only players can respawn");
            return 1;
        }
        if (Walls.gameController.getStatus() != GameController.Status.ACTIVE) {
            sender.sendRichMessage("<red>[Walls] Game is not active. Game must be active to respawn");
            return 1;
        }
        String team;
        try {
            team = StringArgumentType.getString(ctx, "team");
        } catch (IllegalArgumentException e) {
            if (Walls.gameController.getTeam1().hasPlayer(player)) team = "red";
            else if (Walls.gameController.getTeam2().hasPlayer(player)) team = "blue";
            else {
                sender.sendRichMessage("<red>[Walls] You are not on a team, so you must specify one");
                return 1;
            }
        }
        if (team.equals("red")) {
            if (!Walls.gameController.getTeam1Anchor().teleportToAnchor(player)) {
                sender.sendRichMessage("<red>[Walls] Respawn anchor doesn't exist, or was destroyed");
                return 1;
            }
        }
        if (team.equals("blue")) {
            if (!Walls.gameController.getTeam2Anchor().teleportToAnchor(player)) {
                sender.sendRichMessage("<red>[Walls] Respawn anchor doesn't exist, or was destroyed");
                return 1;
            }
        }

        return 1;
    }

    public static int stop(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        sender.sendRichMessage("<green>[Walls] Stopping game...");
        Walls.gameController.stop();
        return 1;
    }
}
