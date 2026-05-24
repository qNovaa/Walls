package nova.walls.Models;

import nova.walls.Tasks.DisplayWall;
import nova.walls.Walls;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.ArrayList;

public class Wall {
    // what is the block made of?
    public Material blockMaterial;
    ArrayList<Block> createdBlocks;
    ArrayList<Block> deadBlocks;
    ArrayList<BlockState> oldBlocks;
    int loc;
    int distance;
    public enum Orientation {NORTHSOUTH, EASTWEST};
    Orientation orientation;
    boolean particlesEnabled;
    boolean wallCreated;

    private Walls plugin;
    private DisplayWall displayWallTask;

    public Wall(Walls plugin, Material blockMaterial, int loc, int distance, Orientation orientation) {
        this.blockMaterial = blockMaterial;
        this.loc = loc;
        this.distance = distance;
        this.orientation = orientation;
        createdBlocks = new ArrayList<>();
        deadBlocks = new ArrayList<>();
        oldBlocks = new ArrayList<>();
        this.plugin = plugin;
        displayWallTask = new DisplayWall(plugin);
    }

    public Wall(Walls plugin, Material blockMaterial, int loc, int distance, Orientation orientation, boolean particlesEnabled) {
        this(plugin, blockMaterial, loc, distance, orientation);
        this.particlesEnabled = particlesEnabled;
    }

    public void createWall() {
        // save all block locations
        if (orientation == Orientation.NORTHSOUTH) {
            for (int i = -distance/2; i <= distance/2; i++) {
                for (int j = 320; j >= -64; j--) {
                    addBlock(Walls.world.getBlockAt(i, j, loc));
                }
            }
        } else {
            for (int i = -distance/2; i <= distance/2; i++) {
                for (int j = 320; j >= -64; j--) {
                    addBlock(Walls.world.getBlockAt(loc, j, i));
                }
            }
        }

        // replace all passable blocks with material and save their old state
        for (Block block : createdBlocks) {
            oldBlocks.add(block.getState());
            block.setType(blockMaterial);
        }

        // start display wall task
        displayWallTask.startTask();

        wallCreated = true;
    }

    private void addBlock(Block block) {
        if (block.isPassable()) {
            createdBlocks.add(block);
        } else {
            deadBlocks.add(block);
        }
    }

    // restore the state of all blocks that were changed by the wall and remove wall
    public void restoreWall() {
        for (BlockState state : oldBlocks) {
            state.update(true, false);
        }
        createdBlocks.clear();
        oldBlocks.clear();
        deadBlocks.clear();
        displayWallTask.cancel();
        wallCreated = false;
    }

    public ArrayList<Block> getCreatedBlocks() {
        return createdBlocks;
    }

    public void setCreatedBlocks(ArrayList<Block> createdBlocks) {
        this.createdBlocks = createdBlocks;
    }

    public ArrayList<Block> getDeadBlocks() {
        return deadBlocks;
    }

    public void setDeadBlocks(ArrayList<Block> deadBlocks) {
        this.deadBlocks = deadBlocks;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public int getLoc() {
        return loc;
    }

    public boolean isWallCreated() {
        return wallCreated;
    }
}
