package nova.walls.Controllers;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nova.walls.Models.RespawnAnchor;
import nova.walls.Models.Wall;
import nova.walls.Walls;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.*;

import java.sql.Timestamp;
import java.util.*;

public class GameController {

    private Walls plugin;

    public enum Status {WAITING, ACTIVE, FINISH};
    private Status status;

    private Wall wall;

    // Teams/Scoreboard
    private Scoreboard scoreboard;
    private Team team1;
    private Team team2;
    private Team spectatingTeam;
    private int previousScoreboardLength;

    // Death counters
    private HashMap<UUID, Integer> deathCounters;

    // Team anchors
    private RespawnAnchor team1Anchor;
    private RespawnAnchor team2Anchor;

    // every tick loop
    private int taskId;
    private Timestamp startTime;

    // time markers
    private long timeWallWillDrop;

    public GameController(Walls plugin) {
        this.plugin = plugin;
        status = Status.WAITING;

        // set up scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getMainScoreboard();

        // create teams
        try {
            team1 = scoreboard.registerNewTeam("Red");
        } catch (IllegalArgumentException e) {
            team1 = scoreboard.getTeam("Red");
        }
        team1.color(NamedTextColor.RED);
        try {
            team2 = scoreboard.registerNewTeam("Blue");
        } catch (IllegalArgumentException e) {
            team2 = scoreboard.getTeam("Blue");
        }
        team2.color(NamedTextColor.AQUA);
        try {
            spectatingTeam = scoreboard.registerNewTeam("Spectating");
        } catch (IllegalArgumentException e) {
            spectatingTeam = scoreboard.getTeam("Spectating");
        }
        spectatingTeam.color(NamedTextColor.GRAY);

        deathCounters = new HashMap<>();

        team1Anchor = new RespawnAnchor(1);
        team2Anchor = new RespawnAnchor(2);

        initializeScoreboard();

        startTimer();
    }

    private void initializeScoreboard() {
        Objective objective;
        try {
            objective = scoreboard.registerNewObjective("sidebar", "dummy");
        } catch (IllegalArgumentException e) {
            objective = scoreboard.getObjective("sidebar");
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.displayName(Component.text("The Walls", NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    }

    private void sendAnchorMessages(Team team, RespawnAnchor anchor) {
        int respawningTimer = anchor.getRespawnTime() + 1;
        Queue<UUID> respawningQueue = anchor.getRespawnQueue();
        if (!respawningQueue.isEmpty()) {
            Player respawningPlayer = Bukkit.getPlayer(respawningQueue.peek());
            Component actionbar = Component.text(respawningPlayer.getName() + " respawning in " + respawningTimer, NamedTextColor.GRAY);
            team.sendActionBar(actionbar);
        } else if (respawningTimer > Walls.RESPAWN_TIMER_MIN) {
            Component actionbar = Component.text("Respawn anchor cooldown: " + respawningTimer);
            team.sendActionBar(actionbar);
        }
    }

    public void startTimer() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            loadScoreboard();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setScoreboard(scoreboard);
            }

            sendActionbar();

            // if wall should drop, drop wall (timer has passed)
            if (checkIfWallShouldDrop()) {
                wall.restoreWall();
                ForwardingAudience audience = Bukkit.getServer();
                audience.sendMessage(Component.text("The wall has dropped!", NamedTextColor.RED).decorate(TextDecoration.BOLD));
                audience.playSound(Sound.sound(Key.key("minecraft:entity_wither_spawn"), Sound.Source.HOSTILE, 1f, 1f), Sound.Emitter.self());
            }
        }, 0L, 1L);
    }

    public ArrayList<UUID> getPlayersInGame() {
        ArrayList<UUID> playersInGame = new ArrayList<>();

        for (String entry : team1.getEntries()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry);
            playersInGame.add(offlinePlayer.getUniqueId());
        }
        for (String entry : team2.getEntries()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry);
            playersInGame.add(offlinePlayer.getUniqueId());
        }

        return playersInGame;
    }

    public void lobby() {

        ArrayList<UUID> playersInGame = getPlayersInGame();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!playersInGame.contains(player.getUniqueId()) && !spectatingTeam.hasPlayer(player)) {
                spectatingTeam.addPlayer(player);
            }
            player.getInventory().clear();
        }

        // Give team selection items to all players in game and spectating
        for (UUID uuid : playersInGame) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                giveTeamItems(player);
            }
        }
        for (String entry : spectatingTeam.getEntries()) {
            Player player = Bukkit.getPlayer(entry);
            if (player != null) {
                giveTeamItems(player);
            }
        }
    }

    public void giveTeamItems(Player player) {
        ItemStack redItem, blueItem, specItem;
        // Red team item
        if (team1.hasPlayer(player)) {
            redItem = new ItemStack(Material.RED_WOOL);
            ItemMeta redMeta = redItem.getItemMeta();
            redMeta.setDisplayName("Joined Red Team");
            redMeta.setLore(Arrays.asList("You are on red team"));
            redItem.setItemMeta(redMeta);
        } else {
            redItem = new ItemStack(Material.RED_STAINED_GLASS);
            ItemMeta redMeta = redItem.getItemMeta();
            redMeta.setDisplayName("Join Red Team");
            redMeta.setLore(Arrays.asList("Right-click to join the red team"));
            redItem.setItemMeta(redMeta);
        }

        if (team2.hasPlayer(player)) {
            blueItem = new ItemStack(Material.BLUE_WOOL);
            ItemMeta blueMeta = blueItem.getItemMeta();
            blueMeta.setDisplayName("Joined Blue Team");
            blueMeta.setLore(Arrays.asList("You are on blue team"));
            blueItem.setItemMeta(blueMeta);
        } else {
            blueItem = new ItemStack(Material.BLUE_STAINED_GLASS);
            ItemMeta blueMeta = blueItem.getItemMeta();
            blueMeta.setDisplayName("Join Blue Team");
            blueMeta.setLore(Arrays.asList("Right-click to join the blue team"));
            blueItem.setItemMeta(blueMeta);
        }

        if (spectatingTeam.hasPlayer(player)) {
            specItem = new ItemStack(Material.GRAY_WOOL);
            ItemMeta specMeta = specItem.getItemMeta();
            specMeta.setDisplayName("Currently spectating");
            specMeta.setLore(Arrays.asList("You are currently spectating"));
            specItem.setItemMeta(specMeta);
        } else {
            specItem = new ItemStack(Material.GRAY_STAINED_GLASS);
            ItemMeta specMeta = specItem.getItemMeta();
            specMeta.setDisplayName("Spectate");
            specMeta.setLore(Arrays.asList("Right-click to spectate"));
            specItem.setItemMeta(specMeta);
        }

        // Add items to player's inventory
        player.getInventory().setItem(1, redItem);
        player.getInventory().setItem(2, blueItem);
        player.getInventory().setItem(3, specItem);
    }

    public void setup() {
        // set world border
        WorldBorder border = Walls.world.getWorldBorder();
        border.setSize(Walls.WORLD_BORDER_SIZE);
        border.setCenter(0, 0);
        border.setWarningDistance(0);

        // set up wall if it is not already set up
        if (wall == null) {
            wall = new Wall(plugin, Walls.WALL_MATERIAL, Walls.WALL_LOCATION, Walls.WALL_DISTANCE, Walls.WALL_ORIENTATION, Walls.WALL_PARTICLES);
        }
        wall.createWall();
    }

    public void start() {

        // setup the game
        setup();

        // load timers
        startTime = new Timestamp(System.currentTimeMillis());
        timeWallWillDrop = startTime.getTime() / 1000 + Walls.WALL_TIMER_MINUTES * 60;

        // teleport players to team locations
        // team 1...
        for (String entry : team1.getEntries()) {
            Player player = Bukkit.getPlayer(entry);
            Location loc;
            if (wall.getOrientation() == Wall.Orientation.EASTWEST) {
                loc = new Location(Walls.world, wall.getLoc() - 5, Walls.world.getHighestBlockYAt(wall.getLoc() - 5, 0) + 1, 0);
            } else {
                loc = new Location(Walls.world, 0, Walls.world.getHighestBlockYAt(0, wall.getLoc() - 5) + 1, wall.getLoc() - 5);
            }
            player.teleport(loc);
        }
        // team 2...
        for (String entry : team2.getEntries()) {
            Player player = Bukkit.getPlayer(entry);
            Location loc;
            if (wall.getOrientation() == Wall.Orientation.EASTWEST) {
                loc = new Location(Walls.world, wall.getLoc() + 5, Walls.world.getHighestBlockYAt(wall.getLoc() + 5, 0) + 1, 0);
            } else {
                loc = new Location(Walls.world, 0, Walls.world.getHighestBlockYAt(0, wall.getLoc() + 5) + 1, wall.getLoc() + 5, -180, 0);
            }
            player.teleport(loc);
        }
        // spectators
        for (UUID uuid : getPlayersSpectatingUUID()) {
            Player player = Bukkit.getPlayer(uuid);
            Location loc;
            if (wall.getOrientation() == Wall.Orientation.EASTWEST) {
                loc = new Location(Walls.world, wall.getLoc() - 5, Walls.world.getHighestBlockYAt(wall.getLoc() - 5, 0) + 6, 0);
            } else {
                loc = new Location(Walls.world, 0, Walls.world.getHighestBlockYAt(0, wall.getLoc() - 5) + 6, wall.getLoc() - 5);
            }
            player.teleport(loc);
            player.setGameMode(GameMode.SPECTATOR);
        }

        // reset players/world
        for (UUID uuid : getPlayersInGame()) {
            Player player = Bukkit.getPlayer(uuid);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.setSaturation(20);
            Walls.world.setTime(0);
        }

        // give a random player on each team the anchor
        ArrayList<Player> team1Players = getPlayersOfTeam(team1);
        ArrayList<Player> team2Players = getPlayersOfTeam(team2);
        if (!team1Players.isEmpty()) team1Anchor.giveRespawnAnchor(team1Players.getFirst());
        if (!team2Players.isEmpty()) team2Anchor.giveRespawnAnchor(team2Players.getFirst());

        status = Status.ACTIVE;
    }

    public void stop() {
        status = Status.FINISH;
        if (wall != null && wall.isWallCreated()) wall.restoreWall();
        team1Anchor = null;
        team2Anchor = null;
    }

    public void loadScoreboard() {
        ArrayList<Component> scoreboardComponents = new ArrayList<>();
        Objective objective = scoreboard.getObjective("sidebar");
        // time till wall drops
        if (status == Status.ACTIVE) {
            if (wall != null) {
                if (wall.isWallCreated()) {
                    long timeTillWallDrops = timeWallWillDrop - System.currentTimeMillis() / 1000;
                    long minutes = timeTillWallDrops / 60;
                    long seconds = timeTillWallDrops % 60;
                    scoreboardComponents.add(Component.text(String.format("Wall drops in %02d:%02d", minutes, seconds), NamedTextColor.GRAY));
                } else {
                    scoreboardComponents.add(Component.text("Wall has dropped", NamedTextColor.GRAY));
                }
            }
        }

        // list out all players of teams
        scoreboardComponents.add(Component.text("Red Team").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        for (Player player : getPlayersOfTeam(team1)) {
            if (player == null) continue;
            Component component = player.displayName().color(isDead(player) ? NamedTextColor.GRAY : NamedTextColor.RED);
            if (isDead(player) && team1Anchor.isDestroyed()) {
                component = component.decorate(TextDecoration.STRIKETHROUGH);
            }
            scoreboardComponents.add(component);
        }
        scoreboardComponents.add(Component.text(""));
        scoreboardComponents.add(Component.text("Blue Team").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        for (Player player : getPlayersOfTeam(team2)) {
            if (player == null) continue;
            Component component = player.displayName().color(isDead(player) ? NamedTextColor.GRAY : NamedTextColor.AQUA);
            if (isDead(player) && team2Anchor.isDestroyed()) {
                component = component.decorate(TextDecoration.STRIKETHROUGH);
            }
            scoreboardComponents.add(component);
        }
        scoreboardComponents.add(Component.text(""));

        // time of game
        if (status == Status.ACTIVE) {
            long elapsedTime = (System.currentTimeMillis() - startTime.getTime()) / 1000;
            long minutes = elapsedTime / 60;
            long seconds = elapsedTime % 60;
            scoreboardComponents.add(Component.text(String.format("Time: %02d:%02d", minutes, seconds), NamedTextColor.GRAY));
        } else if (status == Status.WAITING) {
            scoreboardComponents.add(Component.text("Waiting for game to start").color(NamedTextColor.GRAY));
        }

        // reset scoreboard if length shrinks
        if (previousScoreboardLength > scoreboardComponents.size()) {
            for (int i = 0; Math.abs(i) < previousScoreboardLength; i--) {
                scoreboard.resetScores(ChatColor.values()[Math.abs(i)].toString());
            }
        }
        previousScoreboardLength = scoreboardComponents.size();

        // add to scoreboard
        int i = 0;
        for (Component component : scoreboardComponents) {
            Team team = scoreboard.getTeam("line" + i);
            if (team == null) team = scoreboard.registerNewTeam("line" + i);
            team.prefix(component);
            String entry = ChatColor.values()[Math.abs(i)].toString();
            team.addEntry(entry);
            if (objective != null) {
                objective.getScore(entry).setScore(i);
            }
            i--;
        }
    }

    public void sendActionbar() {
        if (status == Status.WAITING) {
            spectatingTeam.sendActionBar(Component.text("If you do not select a team, you will be spectating!", NamedTextColor.GRAY));
            team1.sendActionBar(Component.text("Currently on Red team", NamedTextColor.RED));
            team2.sendActionBar(Component.text("Currently on Blue team", NamedTextColor.AQUA));
        }

        // only run if game is active
        if (status == Status.ACTIVE) {
            // display actionbars for respawn anchor to teams
            if (!team1Anchor.isDestroyed()) sendAnchorMessages(team1, team1Anchor);
            if (!team2Anchor.isDestroyed()) sendAnchorMessages(team2, team2Anchor);
        }
    }

    public boolean checkIfWallShouldDrop() {
        return status == Status.ACTIVE && wall != null && wall.isWallCreated() && System.currentTimeMillis() / 1000 >= timeWallWillDrop;
    }

    public Wall getWall() {
        return wall;
    }

    public void setWall(Wall wall) {
        this.wall = wall;
    }

    public void addToTeam1(UUID player) {
        team1.addPlayer(Bukkit.getPlayer(player));
    }

    public void addToTeam2(UUID player) {
        team2.addPlayer(Bukkit.getPlayer(player));
    }

    public void addToSpectators(UUID player) {
        spectatingTeam.addPlayer(Bukkit.getPlayer(player));
    }

    public ArrayList<UUID> getPlayersSpectatingUUID() {
        ArrayList<UUID> playersSpectating = new ArrayList<>();

        for (String entry : spectatingTeam.getEntries()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry);
            playersSpectating.add(offlinePlayer.getUniqueId());
        }

        return playersSpectating;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public HashMap<UUID, Integer> getDeathCounters() {
        return deathCounters;
    }

    public RespawnAnchor getTeam1Anchor() {
        return team1Anchor;
    }

    public void setTeam1Anchor(RespawnAnchor team1Anchor) {
        this.team1Anchor = team1Anchor;
    }

    public RespawnAnchor getTeam2Anchor() {
        return team2Anchor;
    }

    public void setTeam2Anchor(RespawnAnchor team2Anchor) {
        this.team2Anchor = team2Anchor;
    }

    public Team getTeam1() {
        return team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public Team getTeamOfPlayer(Player player) {
        if (team1.hasPlayer(player)) return team1;
        if (team2.hasPlayer(player)) return team2;
        if (spectatingTeam.hasPlayer(player)) return spectatingTeam;
        return null;
    }

    public RespawnAnchor getRespawnAnchorOfTeam(Team team) {
        if (team == team1) return team1Anchor;
        if (team == team2) return team2Anchor;
        return null;
    }

    public ArrayList<Player> getPlayersOfTeam(Team team) {
        ArrayList<Player> playerList = new ArrayList<>();
        for (String entry : team.getEntries()) {
            playerList.add(Bukkit.getPlayer(entry));
        }
        return playerList;
    }

    public void cancel() {
        Bukkit.getScheduler().cancelTask(taskId);
    }

    public boolean isDead(Player player) {
        Team team = getTeamOfPlayer(player);
        RespawnAnchor anchor = getRespawnAnchorOfTeam(team);
        return anchor.getRespawnQueue().contains(player.getUniqueId());
    }
}
