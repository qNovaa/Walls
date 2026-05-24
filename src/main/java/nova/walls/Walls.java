package nova.walls;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import nova.walls.Commands.WallsCommand;
import nova.walls.Controllers.GameController;
import nova.walls.Events.*;
import nova.walls.Models.Wall;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Walls extends JavaPlugin {

    // server settings
    public final static String WORLD_NAME = "world";

    //game settings
    public final static double WORLD_BORDER_SIZE = 1000;
    public final static Material WALL_MATERIAL = Material.BARRIER;
    public final static boolean WALL_PARTICLES = true;
    public final static int WALL_LOCATION = 0;
    public final static int WALL_DISTANCE = (int) WORLD_BORDER_SIZE;
    public final static Wall.Orientation WALL_ORIENTATION = Wall.Orientation.NORTHSOUTH;
    public final static int RESPAWN_TIMER_MAX = 60;
    public final static int RESPAWN_TIMER_MIN = 5;
    public final static int WALL_TIMER_MINUTES = 15;

    public static World world;
    public static GameController gameController;
    public static Walls instance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        world = Bukkit.getWorld(WORLD_NAME);

        gameController = new GameController(this);

        // register commands
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(WallsCommand.createCommand(this));
        });

        // register events
        getServer().getPluginManager().registerEvents(new BreakBorder(), this);
        getServer().getPluginManager().registerEvents(new TeamSelector(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
        getServer().getPluginManager().registerEvents(new BeforeGameStart(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerDeath(), this);
        getServer().getPluginManager().registerEvents(new RespawnAnchorListener(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerEatHead(), this);

        System.out.println("[Walls] Plugin enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("[Walls] Plugin disabled");
    }
}
