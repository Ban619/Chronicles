package com.abyss.world;

import com.abyss.campaign.ChapterOneProgress;
import com.abyss.entities.*;
import com.abyss.items.Consumable;
import com.abyss.items.Item;
import com.abyss.items.Material;
import com.abyss.utils.AssetLoader;
import com.abyss.utils.DialogueHandler;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * GameMap — Enhanced with:
 *   Multi-biome procedural generators (VILLAGE, FOREST, DUNGEON, BOSS_ARENA)
 *   MapTransition trigger zones + cooldown management
 *   LightSource list populated from torch/lava tiles, passed to LightingEngine
 *   resetAllTransitionCooldowns() for safe arrival on new map
 */
public class GameMap {

    public enum Biome { VILLAGE, FOREST, DUNGEON, BOSS_ARENA }

    private static final int TILES_PER_ROW = 16;

    private final int cols, rows;
    private int[][] groundLayer, objectLayer, overlayLayer;
    private final Tile[][] tileCache;

    private float  startX = 5 * Tile.SIZE;
    private float  startY = 5 * Tile.SIZE;
    private String mapName = "Unknown";
    private Biome  biome   = Biome.VILLAGE;

    private final List<NPC>          npcs        = new ArrayList<>();
    private final List<Enemy>        enemies     = new ArrayList<>();
    private final List<ItemDrop>     drops       = new ArrayList<>();
    private final List<MapTransition>transitions = new ArrayList<>();
    private final List<LightSource>  lightSources= new ArrayList<>();
    private final List<Particle>     particles   = new ArrayList<>();

    private final AssetLoader assetLoader;
    private Image tileset;
    private final Random layoutRandom;
    private int relicCol = -1;
    private int relicRow = -1;

    // ─────────────────────────────────────────────────────────────────────
    public GameMap(String mapPath, AssetLoader loader) {
        this.assetLoader = loader;
        this.tileset     = loader.getImage("tiles/tileset.png");
        this.layoutRandom = new Random(System.nanoTime() ^ mapPath.hashCode());
        int[] dims = loader.getMapDimensions(mapPath);
        this.cols = dims[0]; this.rows = dims[1];
        this.tileCache = new Tile[rows][cols];

        biome = detectBiome(mapPath);
        generateForBiome(biome);

        populateTileCache();
        spawnEntitiesForBiome(biome, loader);
        buildTransitionsForBiome(biome);
        buildLightSourcesFromTiles();
    }

    // ── Biome detection ───────────────────────────────────────────────────
    private Biome detectBiome(String path) {
        if (path.contains("forest") || path.contains("thornwood")) return Biome.FOREST;
        if (path.contains("dungeon") || path.contains("crypt"))    return Biome.DUNGEON;
        if (path.contains("boss")   || path.contains("maw"))       return Biome.BOSS_ARENA;
        return Biome.VILLAGE;
    }

    // ── Procedural generation dispatch ────────────────────────────────────
    private void generateForBiome(Biome b) {
        groundLayer = new int[rows][cols];
        objectLayer = new int[rows][cols];
        overlayLayer= new int[rows][cols];
        switch (b) {
            case VILLAGE    -> generateVillage();
            case FOREST     -> generateForest();
            case DUNGEON    -> generateDungeon();
            case BOSS_ARENA -> generateBossArena();
        }
    }

    // ── VILLAGE ───────────────────────────────────────────────────────────
    private void generateVillage() {
        mapName = "Ashenveil";
        fill(Tile.GRASS);
        border(Tile.WALL_STONE);
        int mid = rows / 2;
        for (int c = 4; c < cols-4; c++) { groundLayer[mid][c]=Tile.WATER; groundLayer[mid+1][c]=Tile.WATER; }
        for (int c = cols/2-1; c <= cols/2+1; c++) { groundLayer[mid][c]=Tile.BRIDGE; groundLayer[mid+1][c]=Tile.BRIDGE; }
            for (int c=5; c<cols-5; c++) { groundLayer[mid-2][c] = Tile.FLOOR_STONE; }
            for (int c=cols/2-1;c<cols;c++) {
                groundLayer[mid][c]=Tile.BRIDGE;
                groundLayer[mid+1][c]=Tile.BRIDGE;
            }
            for (int r=mid;r<rows;r++) {
                groundLayer[r][cols/2-1]=Tile.DIRT;
                groundLayer[r][cols/2]=Tile.DIRT;
                groundLayer[r][cols/2+1]=Tile.DIRT;
                objectLayer[r][cols/2-1]=0;
                objectLayer[r][cols/2]=0;
                objectLayer[r][cols/2+1]=0;
            }
            for (int r=mid-4;r<=mid+4;r++) objectLayer[r][cols-1]=0;
        int[][] trees = {{3,3},{4,3},{8,2},{9,2},{cols-4,3},{cols-5,4},{2,rows-3},{cols-3,rows-4}};
        for (int[] t : trees) {
            if (inBounds(t[1],t[0])) { objectLayer[t[1]][t[0]]=Tile.TREE_BASE; if(inBounds(t[1]-1,t[0])) overlayLayer[t[1]-1][t[0]]=Tile.TREE_TOP; }
        }
        placeHouse(2,rows-9,5,4); placeHouse(cols-8,3,5,4);
        objectLayer[rows-9][2]=Tile.TORCH; objectLayer[rows-9][6]=Tile.TORCH;
        objectLayer[3][cols-8]=Tile.TORCH; objectLayer[3][cols-4]=Tile.TORCH;
        objectLayer[4][cols-6]=Tile.CHEST; objectLayer[rows-6][4]=Tile.CHEST;
        startX=7*Tile.SIZE; startY=(mid-4)*Tile.SIZE;
    }

    // ── THORNWOOD FOREST ──────────────────────────────────────────────────
    private void generateForest() {
        mapName = "Thornwood Forest";
        Random rng = layoutRandom;
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++)
            groundLayer[r][c] = rng.nextDouble()<0.15 ? Tile.DIRT : Tile.GRASS;
        for (int r=1;r<rows-1;r++) for (int c=1;c<cols-1;c++) {
            if (rng.nextDouble()<0.30 && groundLayer[r][c]==Tile.GRASS) {
                objectLayer[r][c]=Tile.TREE_BASE;
                if (r>0) overlayLayer[r-1][c]=Tile.TREE_TOP;
            }
        }
        int pathCol=cols/2;
        for (int r=0;r<rows;r++) for (int dc=-1;dc<=1;dc++) {
            int c=pathCol+dc;
            if (inBounds(r,c)) { groundLayer[r][c]=Tile.DIRT; objectLayer[r][c]=0; overlayLayer[r][c]=0; }
        }
            for (int c=0;c<=pathCol;c++) {
                groundLayer[rows/2][c]=Tile.DIRT;
                objectLayer[rows/2][c]=0;
                overlayLayer[rows/2][c]=0;
            }
        // Glowing mushrooms as dim lights
        int mushroomCount = 7 + rng.nextInt(8);
        for (int i=0;i<mushroomCount;i++) {
            int mr=3+rng.nextInt(rows-6), mc=3+rng.nextInt(cols-6);
            if (objectLayer[mr][mc]==0) objectLayer[mr][mc]=Tile.TORCH;
        }
        // East stream
        int streamCol = Math.max(pathCol + 5, cols - 4 - rng.nextInt(4));
        int streamStart = rows / 4 + rng.nextInt(Math.max(1, rows / 10));
        int streamEnd = Math.min(rows - 1, rows * 3 / 4 + rng.nextInt(Math.max(1, rows / 10)));
        for (int r=streamStart;r<streamEnd;r++) {
            groundLayer[r][streamCol]=Tile.WATER;
            if (streamCol > 0) groundLayer[r][streamCol-1]=Tile.WATER;
        }
        // Stone ruins cluster
        int ruinRow = Math.min(rows - 8, rows / 2 + 2 + rng.nextInt(7));
        int ruinCol = Math.max(2, pathCol - 12 + rng.nextInt(6));
        for (int r=ruinRow;r<ruinRow+4;r++) for (int c=ruinCol;c<ruinCol+4;c++)
            if (rng.nextBoolean()) objectLayer[r][c]=Tile.WALL_STONE;
        startX=pathCol*Tile.SIZE; startY=2*Tile.SIZE;
    }

    // ── CRYPT OF ECHOES (Dungeon) ─────────────────────────────────────────
    private void generateDungeon() {
        mapName = "Crypt of Echoes";
        int sx = Math.max(1, cols / 32);
        int sy = Math.max(1, rows / 28);
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            groundLayer[r][c]=Tile.FLOOR_STONE; objectLayer[r][c]=Tile.WALL_STONE;
        }
        carveRoom(2*sx,2*sy,8*sx,6*sy);  carveRoom(12*sx,2*sy,8*sx,6*sy);
        carveRoom(24*sx,2*sy,8*sx,6*sy);
        carveRoom(2*sx,12*sy,10*sx,7*sy); carveRoom(14*sx,12*sy,12*sx,7*sy);
        carveRoom(2*sx,22*sy,8*sx,5*sy); carveRoom(14*sx,22*sy,10*sx,5*sy);
        carveHall(10*sx,5*sy,12*sx,5*sy); carveHall(20*sx,5*sy,24*sx,5*sy);
        carveHall(7*sx,8*sy,7*sx,12*sy); carveHall(18*sx,8*sy,18*sx,12*sy);
        carveHall(18*sx,12*sy,20*sx,12*sy);
        carveHall(7*sx,19*sy,7*sx,22*sy); carveHall(7*sx,25*sy,14*sx,25*sy);
        carveHall(6*sx,2*sy,6*sx,0);
        if (inBounds(8*sy,10*sx))  objectLayer[8*sy][10*sx] =Tile.DOOR_CLOSED;
        if (inBounds(8*sy,20*sx))  objectLayer[8*sy][20*sx] =Tile.DOOR_CLOSED;
        if (inBounds(12*sy,7*sx))  objectLayer[12*sy][7*sx] =Tile.DOOR_CLOSED;
        for (int[] p : new int[][]{{3,3},{14,3},{26,3},{3,14},{16,14},{3,23},{15,23}}) {
            int col = p[0] * sx;
            int row = p[1] * sy;
            if (inBounds(row,col)) objectLayer[row][col]=Tile.TORCH;
        }
        if (inBounds(4*sy,26*sx))  objectLayer[4*sy][26*sx] =Tile.CHEST;
        if (inBounds(4*sy,23*sx))  objectLayer[4*sy][23*sx] =Tile.CHEST;
        if (inBounds(15*sy,22*sx)) objectLayer[15*sy][22*sx]=Tile.CHEST;
        relicCol = 20 * sx;
        relicRow = 24 * sy;
        if (inBounds(relicRow, relicCol)) objectLayer[relicRow][relicCol] = Tile.CHEST;
        startX=5*sx*Tile.SIZE; startY=4*sy*Tile.SIZE;
    }

    private void carveRoom(int col,int row,int w,int h) {
        for (int r=row;r<row+h&&r<rows;r++) for (int c=col;c<col+w&&c<cols;c++) objectLayer[r][c]=0;
    }
    private void carveHall(int c1,int r1,int c2,int r2) {
        for (int c=Math.min(c1,c2);c<=Math.max(c1,c2);c++) if (inBounds(r1,c)) objectLayer[r1][c]=0;
        for (int r=Math.min(r1,r2);r<=Math.max(r1,r2);r++) if (inBounds(r,c2)) objectLayer[r][c2]=0;
    }

    // ── DRAGON'S MAW (Boss Arena) ─────────────────────────────────────────
    private void generateBossArena() {
        mapName = "Dragon's Maw";
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            groundLayer[r][c]=Tile.FLOOR_STONE; objectLayer[r][c]=Tile.WALL_STONE;
        }
        int cx=cols/2, cy=rows/2, radius=Math.min(cols,rows)/2-3;
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            double dist=Math.sqrt((c-cx)*(c-cx)+(r-cy)*(r-cy));
            if (dist<radius*0.25)   groundLayer[r][c]=Tile.LAVA;
            else if (dist<radius)   { groundLayer[r][c]=Tile.FLOOR_STONE; objectLayer[r][c]=0; }
        }
        for (int angle=0;angle<360;angle+=45) {
            int pc=(int)(cx+(radius-2)*Math.cos(Math.toRadians(angle)));
            int pr=(int)(cy+(radius-2)*Math.sin(Math.toRadians(angle)));
            if (inBounds(pr,pc)) objectLayer[pr][pc]=Tile.TORCH;
        }
        // Entrance corridor north
        for (int r=0;r<cy-radius+1;r++) { if(inBounds(r,cx)) objectLayer[r][cx]=0; if(inBounds(r,cx+1)) objectLayer[r][cx+1]=0; }
        startX=cx*Tile.SIZE; startY=(cy+radius-3)*Tile.SIZE;
    }

    // ── Tile helpers ──────────────────────────────────────────────────────
    private void fill(int id) { for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) groundLayer[r][c]=id; }
    private void border(int id) {
        for(int r=0;r<rows;r++){objectLayer[r][0]=id;objectLayer[r][cols-1]=id;}
        for(int c=0;c<cols;c++){objectLayer[0][c]=id;objectLayer[rows-1][c]=id;}
    }
    private void placeHouse(int col,int row,int w,int h) {
        for(int r=row;r<row+h&&r<rows;r++) for(int c=col;c<col+w&&c<cols;c++) if(inBounds(r,c)) groundLayer[r][c]=Tile.FLOOR_WOOD;
        for(int c=col;c<col+w&&c<cols;c++){if(inBounds(row,c))objectLayer[row][c]=Tile.WALL_BRICK;if(inBounds(row+h-1,c))objectLayer[row+h-1][c]=Tile.WALL_BRICK;}
        for(int r=row;r<row+h&&r<rows;r++){if(inBounds(r,col))objectLayer[r][col]=Tile.WALL_BRICK;if(inBounds(r,col+w-1))objectLayer[r][col+w-1]=Tile.WALL_BRICK;}
        if(inBounds(row+h-1,col+w/2)) objectLayer[row+h-1][col+w/2]=Tile.DOOR_CLOSED;
    }
    private void populateTileCache() {
        for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) {
            int id=objectLayer[r][c]!=0?objectLayer[r][c]:groundLayer[r][c];
            tileCache[r][c]=new Tile(id);
        }
    }

    // ── Light sources ─────────────────────────────────────────────────────
    private void buildLightSourcesFromTiles() {
        lightSources.clear();
        // Slot 0 reserved for player light, added last
        for (int r=0;r<rows;r++) for (int c=0;c<cols;c++) {
            int id=objectLayer[r][c];
            if (id==Tile.TORCH)
                lightSources.add(new LightSource(c*Tile.SIZE+16,r*Tile.SIZE+16,110,
                        Color.color(1.0,0.70,0.25,1.0),LightSource.Type.FLICKER));
            else if (id==Tile.LAVA || groundLayer[r][c]==Tile.LAVA)
                lightSources.add(new LightSource(c*Tile.SIZE+16,r*Tile.SIZE+16,80,
                        Color.color(1.0,0.35,0.05,1.0),LightSource.Type.PULSE));
        }
        // Player carry-light always at index 0
        lightSources.add(0, new LightSource(0,0,75,
                Color.color(0.9,0.85,0.6,1.0),LightSource.Type.STEADY));
    }

    // ── Transitions ───────────────────────────────────────────────────────
    private void buildTransitionsForBiome(Biome b) {
        switch (b) {
            case VILLAGE -> {
                transitions.add(new MapTransition(
                    (cols-1)*Tile.SIZE, (rows/2-4)*Tile.SIZE, Tile.SIZE, 8*Tile.SIZE,
                    "maps/thornwood_forest.json", 2, rows/2f,
                    MapTransition.Direction.EAST, "Thornwood Forest"));
                transitions.add(new MapTransition(
                    cols/2f*Tile.SIZE-Tile.SIZE, (rows-1)*Tile.SIZE, 3*Tile.SIZE, Tile.SIZE,
                    "maps/crypt_of_echoes.json", 5, 4,
                    MapTransition.Direction.SOUTH, "Crypt of Echoes"));
            }
            case FOREST -> {
                transitions.add(new MapTransition(
                    0, (rows/2-4)*Tile.SIZE, Tile.SIZE, 8*Tile.SIZE,
                        "maps/world_map.json", cols-3f, rows/2f-3,
                    MapTransition.Direction.WEST, "Ashenveil"));
                transitions.add(new MapTransition(
                    cols/2f*Tile.SIZE-Tile.SIZE, (rows-1)*Tile.SIZE, 3*Tile.SIZE, Tile.SIZE,
                        "maps/dragons_maw.json", 28, 2,
                    MapTransition.Direction.SOUTH, "Dragon's Maw", "crypt_relic"));
            }
            case DUNGEON -> {
                transitions.add(new MapTransition(
                    5*Tile.SIZE, 0, 3*Tile.SIZE, Tile.SIZE,
                        "maps/world_map.json", 40, rows-3f,
                    MapTransition.Direction.NORTH, "Ashenveil"));
            }
            case BOSS_ARENA -> {
                transitions.add(new MapTransition(
                    cols/2f*Tile.SIZE-Tile.SIZE, 0, 3*Tile.SIZE, Tile.SIZE,
                        "maps/thornwood_forest.json", 40, 57,
                    MapTransition.Direction.NORTH, "Thornwood Forest"));
            }
        }
    }

    // ── Entity spawning ───────────────────────────────────────────────────
    private void spawnEntitiesForBiome(Biome b, AssetLoader loader) {
        switch (b) {
            case VILLAGE    -> spawnVillage(loader);
            case FOREST     -> spawnForest(loader);
            case DUNGEON    -> spawnDungeon(loader);
            case BOSS_ARENA -> spawnBoss(loader);
        }
    }

    private void spawnVillage(AssetLoader l) {
        int mid=rows/2;
        addNPC("Elder Aldric",NPC.NPCType.QUEST_GIVER,9,mid-6,"elder_aldric",l,new int[][]{{9,mid-6},{11,mid-6}});
        addNPC("Mira the Merchant",NPC.NPCType.MERCHANT,12,mid-7,"merchant_mira",l,null);
        addNPC("Town Guard",NPC.NPCType.GUARD,6,mid-5,"guard_generic",l,new int[][]{{6,mid-5},{14,mid-5}});
        npcs.add(new CompanionNPC("Lyra",13*Tile.SIZE,(mid-8)*Tile.SIZE,"lyra_companion",true,0,0,PartyMember.Archetype.ROGUE,l));
        addNPC("Daisy the Healer",NPC.NPCType.HEALER,19,mid-4,"healer_daisy",l,null);
        addNPC("Bram the Smith",NPC.NPCType.BLACKSMITH,24,mid-4,"smith_bram",l,null);
        addNPC("Pip the Baker",NPC.NPCType.BAKER,17,mid-8,"baker_pip",l,null);
        addNPC("Nell the Gardener",NPC.NPCType.VILLAGER,29,mid-6,"gardener_nell",l,null);
        spawnE("Shadow Wolf",20,mid+4,60,12,4,5,30,10,false,"enemy_wolf",l);
        spawnE("Dark Golem",26,mid+5,100,18,12,3,65,25,false,"enemy_golem",l);
        spawnE("Void Wraith",19,mid+6,80,15,6,7,50,18,false,"enemy_wraith",l);
    }
    private void spawnForest(AssetLoader l) {
        int mid=cols/2;
        addNPC("Sylvan Hermit",NPC.NPCType.QUEST_GIVER,mid+3,6,"elder_aldric",l,null);
        addNPC("Forest Trader",NPC.NPCType.MERCHANT,mid-3,8,"merchant_mira",l,null);
        addNPC("Fern the Guide",NPC.NPCType.VILLAGER,cols/4,rows/2,"forest_guide",l,null);
        addNPC("Moss",NPC.NPCType.VILLAGER,cols/3,rows/2,"forest_moss",l,null);
        spawnE("Corrupted Treant",mid+5,10,90,16,8,3,55,20,false,"enemy_golem",l);
        spawnE("Darkspawn",mid-5,8,70,14,5,7,45,15,false,"enemy_wolf",l);
        spawnE("Venomfang Spider",mid+8,15,65,12,4,9,40,12,false,"enemy_wraith",l);
        spawnE("Forest Wraith",mid-7,18,85,18,7,6,60,22,false,"enemy_wraith",l);
        spawnE("Alpha Wolf",mid+2,22,120,20,10,7,90,35,false,"enemy_wolf",l);
    }
    private void spawnDungeon(AssetLoader l) {
        int sx = Math.max(1, cols / 32);
        int sy = Math.max(1, rows / 28);
        spawnE("Skeleton Warrior",5*sx,14*sy,75,14,8,4,45,15,false,"enemy_golem",l);
        spawnE("Revenant",15*sx,5*sy,80,16,6,5,50,18,false,"enemy_wraith",l);
        spawnE("Bone Archer",27*sx,4*sy,60,18,4,6,40,14,false,"enemy_wolf",l);
        spawnE("Crypt Guardian",16*sx,15*sy,140,22,14,4,100,40,false,"enemy_golem",l);
        spawnE("Lich Acolyte",5*sx,24*sy,90,20,8,5,70,28,false,"enemy_wraith",l);
    }
    private void spawnBoss(AssetLoader l) {
        spawnE("Lord Malachar",cols/2,rows/2,350,28,18,5,300,150,true,"boss_malachar",l);
    }

    private void addNPC(String name,NPC.NPCType type,int tx,int ty,String key,AssetLoader l,int[][] wps) {
        NPC npc=new NPC(name,type,tx*Tile.SIZE,ty*Tile.SIZE,key,l);
        if(wps!=null) for(int[] wp:wps) npc.addWaypoint(wp[0]*Tile.SIZE,wp[1]*Tile.SIZE);
        npcs.add(npc);
    }
    private void spawnE(String name,int tx,int ty,int hp,int str,int def,int spd,int xp,int gold,boolean boss,String spr,AssetLoader l) {
        enemies.add(new Enemy(name,tx*Tile.SIZE,ty*Tile.SIZE,hp,str,def,spd,xp,gold,boss,spr,l));
    }

    // ── Transition API ────────────────────────────────────────────────────
    public void updateTransitions(double dt) { transitions.forEach(t->t.update(dt)); }
    public MapTransition checkTransitionTrigger(Player player) {
        for (MapTransition t:transitions)
            if (t.isTriggered(player.getX(),player.getY(),player.getWidth(),player.getHeight())) return t;
        return null;
    }
    public void resetAllTransitionCooldowns() { transitions.forEach(MapTransition::resetCooldown); }

    // ── Collision ─────────────────────────────────────────────────────────
    public boolean isBlocked(float px,float py,int w,int h) {
        return isBlocked(px, py, w, h, null);
    }

    /** Collision check for a moving NPC, excluding that NPC's own body. */
    public boolean isBlocked(float px,float py,int w,int h,NPC ignoredNpc) {
        int[] xs={(int)px,(int)(px+w-1)}, ys={(int)py,(int)(py+h-1)};
        for(int tx:xs) for(int ty:ys){
            int c=tx/Tile.SIZE,r=ty/Tile.SIZE;
            if(c<0||c>=cols||r<0||r>=rows) return true;
            if(!tileCache[r][c].isWalkable()) return true;
        }
        for(NPC n:npcs){if(n==ignoredNpc||!n.isActive())continue;if(px<n.getX()+n.getWidth()&&px+w>n.getX()&&py<n.getY()+n.getHeight()&&py+h>n.getY())return true;}
        return false;
    }

    // ── Update ────────────────────────────────────────────────────────────
    public void updateNPCs(double dt,Player player,DayNightCycle dn) {
        for(NPC n:npcs) n.update(dt,this,player,dn);
        for(Enemy enemy:enemies) {
            enemy.update(dt,this,player);
            resolveEnemyDefeat(enemy, player);
        }
        for(PartyMember pm:player.getParty()){pm.setFormationTarget(player);pm.update(dt,this);}
    }

    public boolean resolveEnemyDefeat(Enemy enemy, Player player) {
        if (!enemy.markDefeated()) return false;
        player.gainXp(enemy.getXpReward());
        player.addGold(enemy.getGoldReward());
        if (player.getChapterOne().recordForestDefeat(
                biome == Biome.FOREST, enemy.isBoss())) {
            spawnParticle(player.getX(), player.getY() - 30,
                    "Thornwood: " + player.getChapterOne().getForestDefeats() + "/3 foes",
                    Color.web("#f0c040"));
        }
        if (biome == Biome.BOSS_ARENA && enemy.isBoss()
                && player.getChapterOne().defeatMalachar()) {
            player.addGold(200);
            player.gainXp(250);
            spawnParticle(player.getX(), player.getY() - 30,
                    "Malachar falls! Return to Elder Aldric.", Color.web("#f0c040"));
        }
        dropCraftingLoot(enemy);
        spawnParticle(enemy.getX(), enemy.getY() - 12,
                "+" + enemy.getXpReward() + " XP  +" + enemy.getGoldReward() + "g",
                Color.web("#f0c040"));
        return true;
    }

    private void dropCraftingLoot(Enemy enemy) {
        if (Math.random() > 0.72) return;
        String[] loot = switch (biome) {
            case FOREST -> new String[]{"herb", "mushroom", "hide", "wood", "thread", "flask"};
            case DUNGEON -> new String[]{"iron_ore", "mushroom", "thread", "flask", "herb"};
            case BOSS_ARENA -> new String[]{"iron_ore", "hide", "thread", "flask"};
            case VILLAGE -> new String[]{"herb", "flask", "hide"};
        };
        String id = loot[(int) (Math.random() * loot.length)];
        Material material = switch (id) {
            case "herb" -> new Material(id, "Duskleaf Herb", "A medicinal leaf that grows in the shade.", id, 1, 99, 4, Item.Rarity.COMMON);
            case "flask" -> new Material(id, "Glass Flask", "An empty bottle suitable for brewing.", id, 1, 99, 3, Item.Rarity.COMMON);
            case "mushroom" -> new Material(id, "Gloomcap Mushroom", "A mana-rich mushroom found in dark places.", id, 1, 99, 5, Item.Rarity.COMMON);
            case "iron_ore" -> new Material(id, "Iron Ore", "Raw ore that can be worked into a blade.", id, 1, 99, 8, Item.Rarity.COMMON);
            case "wood" -> new Material(id, "Ashwood", "Hard wood for a weapon handle.", id, 1, 99, 4, Item.Rarity.COMMON);
            case "hide" -> new Material(id, "Beast Hide", "Tough hide useful for armour.", id, 1, 99, 6, Item.Rarity.COMMON);
            default -> new Material(id, "Moon Thread", "Strong thread used to reinforce leather.", id, 1, 99, 5, Item.Rarity.COMMON);
        };
        dropItem(material, enemy.getX() + 8, enemy.getY() + 8);
    }
    public void updateParticles(double dt) {
        for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) tileCache[r][c].update(dt);
        particles.removeIf(p->!p.update(dt));
        long now=System.currentTimeMillis();
        lightSources.forEach(ls->ls.update(now));
    }
    public void checkEvents(Player player) {
        drops.removeIf(drop -> {
            if (drop.distanceTo(player) >= 24 || !player.addItem(drop.item)) return false;
            assetLoader.playSfx("audio/item_pickup.mp3");
            spawnParticle(drop.x, drop.y, "Item collected", Color.web("#f0c040"));
            return true;
        });
    }

    public String interact(Player player, DialogueHandler dialogueHandler) {
        NPC nearestNpc = null;
        float nearestNpcDistance = 56f;
        for (NPC npc : npcs) {
            if (!npc.isActive()) continue;
            float distance = npc.distanceTo(player);
            if (distance <= nearestNpcDistance) {
                nearestNpc = npc;
                nearestNpcDistance = distance;
            }
        }

        double playerX = player.getCenterX();
        double playerY = player.getCenterY();
        int minCol = Math.max(0, (int) ((playerX - 56) / Tile.SIZE));
        int maxCol = Math.min(cols - 1, (int) ((playerX + 56) / Tile.SIZE));
        int minRow = Math.max(0, (int) ((playerY - 56) / Tile.SIZE));
        int maxRow = Math.min(rows - 1, (int) ((playerY + 56) / Tile.SIZE));
        Tile nearestTile = null;
        int nearestRow = -1;
        int nearestCol = -1;
        double nearestTileDistance = 56 * 56;

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                Tile tile = tileCache[row][col];
                if (!tile.isInteractive()) continue;
                double dx = playerX - (col * Tile.SIZE + Tile.SIZE / 2.0);
                double dy = playerY - (row * Tile.SIZE + Tile.SIZE / 2.0);
                double distanceSquared = dx * dx + dy * dy;
                if (distanceSquared <= nearestTileDistance) {
                    nearestTile = tile;
                    nearestRow = row;
                    nearestCol = col;
                    nearestTileDistance = distanceSquared;
                }
            }
        }

        if (nearestTile == null || nearestNpcDistance * nearestNpcDistance < nearestTileDistance) {
            if (nearestNpc instanceof CompanionNPC companion) {
                if (player.getParty().size() >= Player.MAX_PARTY) return "Your party is full.";
                if (player.getReputation() < companion.getMinReputation()) return "Your reputation is too low.";
                if (player.getGold() < companion.getHireCost()) return "You need more gold.";
                if (companion.tryRecruit(player)) return nearestNpc.getName() + " joined your party.";
            }
            if (nearestNpc != null) {
                if ("Elder Aldric".equals(nearestNpc.getName())) {
                    ChapterOneProgress progress = player.getChapterOne();
                    switch (progress.getStage()) {
                        case MEET_ELDER -> {
                            progress.acceptQuest();
                            return "Elder Aldric: Thornwood's road is overrun. Defeat 3 enemies there, then return. "
                                    + "The southern Crypt may hold the seal we need.";
                        }
                        case CLEAR_THORNWOOD -> {
                            return "Elder Aldric: Thornwood is still dangerous. Foes defeated: "
                                    + progress.getForestDefeats() + "/3.";
                        }
                        case RETURN_TO_ELDER -> {
                            progress.turnInRoadQuest();
                            player.addGold(120);
                            player.gainXp(100);
                            player.adjustReputation(15);
                            return "Quest complete! You earned 120 gold, 100 XP, and 15 reputation. "
                                    + "Enter the southern Crypt and recover the Dawn Seal.";
                        }
                        case FIND_CRYPT_RELIC -> {
                            return "Elder Aldric: The Dawn Seal lies in the deepest Crypt chamber. "
                                    + "Bring it through Thornwood to Dragon's Maw.";
                        }
                        case DEFEAT_MALACHAR -> {
                            return "Elder Aldric: The Dawn Seal will open Dragon's Maw. "
                                    + "End Malachar's reign.";
                        }
                        case RETURN_AFTER_BOSS -> {
                            progress.completeChapter();
                            player.addGold(300);
                            player.gainXp(200);
                            player.adjustReputation(25);
                            return "The Abyss recedes. Ashenveil's lanterns shine again. "
                                    + "Chapter 1 complete! You earned 300 gold, 200 XP, and 25 reputation.";
                        }
                        case COMPLETE -> {
                            return "Elder Aldric: The village will remember what you did for us.";
                        }
                    }
                }
                if ("Sylvan Hermit".equals(nearestNpc.getName())) {
                    return switch (player.getChapterOne().getStage()) {
                        case CLEAR_THORNWOOD -> "Sylvan Hermit: Drive back three corrupted foes on this road.";
                        case RETURN_TO_ELDER -> "Sylvan Hermit: The road is clear. Tell Aldric.";
                        case FIND_CRYPT_RELIC -> "Sylvan Hermit: The Crypt entrance lies south of Ashenveil.";
                        case DEFEAT_MALACHAR -> "Sylvan Hermit: Carry the Dawn Seal south. The Maw is open.";
                        case RETURN_AFTER_BOSS, COMPLETE -> "Sylvan Hermit: The forest can breathe again.";
                        case MEET_ELDER -> "Sylvan Hermit: Speak with Elder Aldric in Ashenveil first.";
                    };
                }
                if (nearestNpc.getType() == NPC.NPCType.HEALER) {
                    return receiveHealerTreatment(player, player.getReputation() >= 50 ? 0 : 25);
                }
                if (nearestNpc.getType() == NPC.NPCType.BAKER) {
                    return buyHoneyBun(player);
                }

                return nearestNpc.getName() + ": " + nearestNpc.getDialogue(player, dialogueHandler);
            }
            return "Nothing nearby to interact with.";
        }

        switch (nearestTile.getId()) {
            case Tile.DOOR_CLOSED -> {
                nearestTile.toggleDoor();
                objectLayer[nearestRow][nearestCol] = nearestTile.getId();
                assetLoader.playSfx("audio/door_open.mp3");
                return "You open the door.";
            }
            case Tile.DOOR_OPEN -> {
                float tileX = nearestCol * Tile.SIZE;
                float tileY = nearestRow * Tile.SIZE;
                boolean standingInDoor = player.getX() < tileX + Tile.SIZE
                        && player.getX() + player.getWidth() > tileX
                        && player.getY() < tileY + Tile.SIZE
                        && player.getY() + player.getHeight() > tileY;
                if (standingInDoor) return "Step clear of the doorway first.";
                nearestTile.toggleDoor();
                objectLayer[nearestRow][nearestCol] = nearestTile.getId();
                assetLoader.playSfx("audio/door_open.mp3");
                return "You close the door.";
            }
            case Tile.CHEST -> {
                if (biome == Biome.DUNGEON && nearestCol == relicCol && nearestRow == relicRow) {
                    ChapterOneProgress progress = player.getChapterOne();
                    if (player.hasItem("crypt_relic")) {
                        nearestTile.openChest();
                        objectLayer[nearestRow][nearestCol] = nearestTile.getId();
                        return "The Dawn Seal is already in your pack. Take it to Dragon's Maw.";
                    }
                    if (progress.getStage() != ChapterOneProgress.Stage.FIND_CRYPT_RELIC)
                        return "The sealed reliquary does not respond. Elder Aldric may know what it needs.";
                    Item relic = new Material("crypt_relic", "Dawn Seal",
                            "An ancient seal that opens the path to Dragon's Maw.",
                            "crypt_relic", 1, 1, 0, Item.Rarity.EPIC);
                    if (!player.canAddItem(relic) || !player.addItem(relic))
                        return "Your pack is full. Make room before taking the Dawn Seal.";
                    if (!progress.recoverCryptRelic())
                        return "The Dawn Seal remains bound. Speak with Elder Aldric first.";
                    nearestTile.openChest();
                    objectLayer[nearestRow][nearestCol] = nearestTile.getId();
                    assetLoader.playSfx("audio/item_pickup.mp3");
                    return "You recovered the Dawn Seal. Carry it through Thornwood to Dragon's Maw.";
                }
                nearestTile.openChest();
                objectLayer[nearestRow][nearestCol] = nearestTile.getId();
                player.addGold(25);
                assetLoader.playSfx("audio/item_pickup.mp3");
                return "Chest opened. You found 25 gold.";
            }
            case Tile.SIGN -> {
                return mapName + ". Keep to the lit roads beyond the village.";
            }
            default -> {
                return "Nothing nearby to interact with.";
            }
        }
    }

    public boolean isNearMerchant(Player player) {
        return npcs.stream().anyMatch(npc -> npc.isActive()
                && npc.getType() == NPC.NPCType.MERCHANT
                && npc.distanceTo(player) <= 56f);
    }

    private String receiveHealerTreatment(Player player, int cost) {
        boolean needsCare = player.getHp() < player.getMaxHp() || player.getMp() < player.getMaxMp();
        for (PartyMember member : player.getParty()) {
            needsCare |= member.getHp() < member.getMaxHp() || member.getMp() < member.getMaxMp();
        }
        if (!needsCare) return "Daisy smiles. \"Everyone's in tip-top shape already!\"";
        if (player.getGold() < cost) return "Daisy checks her remedy basket. \"I can help for " + cost + " gold.\"";

        player.spendGold(cost);
        player.heal(player.getMaxHp() - player.getHp());
        player.restoreMp(player.getMaxMp() - player.getMp());
        for (PartyMember member : player.getParty()) {
            member.heal(member.getMaxHp() - member.getHp());
            member.restoreMp(member.getMaxMp() - member.getMp());
        }
        return cost == 0
                ? "Daisy patches up your whole party for free. The village is glad to have you!"
                : "Daisy patches up your whole party for " + cost + " gold. Everyone's ready for adventure!";
    }

    private String buyHoneyBun(Player player) {
        final int cost = 8;
        if (player.getGold() < cost) return "Pip's honey buns cost 8 gold. Come back when you have enough!";
        Item bun = new Consumable("Honey Bun", "A warm, sweet bun. Restores 35 HP.", "honey_bun", 35, 0, cost);
        if (!player.addItem(bun)) return "Your pack is full. Make room for Pip's honey bun first.";
        player.spendGold(cost);
        return "You bought a fresh honey bun for 8 gold. Use it from your inventory when you need a snack!";
    }

    // ── Particles ─────────────────────────────────────────────────────────
    public void spawnParticle(float x,float y,String text,Color color){particles.add(new Particle(x,y,text,color));}
    public void spawnDamageNumber(float x,float y,int dmg,boolean crit){spawnParticle(x,y,(crit?"✦":"")+dmg,crit?Color.web("#f0c040"):Color.web("#e74c3c"));}
    public void dropItem(Item item,float x,float y){drops.add(new ItemDrop(item,x,y));}
    public void removeEnemy(Enemy e){e.setActive(false);e.setVisible(false);}

    // ── Render ────────────────────────────────────────────────────────────
    public void renderGroundLayer(GraphicsContext gc,Camera cam) {
        int sc=Math.max(0,cam.getOffsetX()/Tile.SIZE),sr=Math.max(0,cam.getOffsetY()/Tile.SIZE);
        int ec=Math.min(cols,sc+cam.getViewCols()+2),er=Math.min(rows,sr+cam.getViewRows()+2);
        for(int r=sr;r<er;r++) for(int c=sc;c<ec;c++) {
            int tileId = groundLayer[r][c];
            new Tile(tileId).render(gc,tileset,c*Tile.SIZE,r*Tile.SIZE,TILES_PER_ROW);
            if (tileset == null) renderBiomeDetail(gc, tileId, c * Tile.SIZE, r * Tile.SIZE, r, c);
        }
    }

    private void renderBiomeDetail(GraphicsContext gc, int tileId, int x, int y, int row, int col) {
        int mark = Math.floorMod(row * 73856093 ^ col * 19349663 ^ tileId * 83492791, 13);
        if (mark > 2 || tileId == Tile.WATER || tileId == Tile.LAVA) return;
        gc.save();
        switch (biome) {
            case FOREST -> {
                gc.setFill(Color.color(0.52, 0.78, 0.35, 0.38));
                gc.fillOval(x + 6 + mark * 3, y + 9, 3, 5);
                gc.fillOval(x + 11 + mark * 2, y + 13, 3, 4);
                if (mark == 0) {
                    gc.setFill(Color.web("#dbbf75", 0.75));
                    gc.fillOval(x + 23, y + 7, 3, 3);
                }
            }
            case DUNGEON -> {
                gc.setStroke(Color.color(0.08, 0.10, 0.12, 0.44));
                gc.setLineWidth(1.2);
                gc.strokeLine(x + 7, y + 8, x + 12, y + 13);
                gc.strokeLine(x + 12, y + 13, x + 10, y + 19);
                gc.strokeLine(x + 12, y + 13, x + 18, y + 12);
            }
            case BOSS_ARENA -> {
                gc.setFill(Color.color(1, 0.36, 0.08, 0.55));
                gc.fillOval(x + 7 + mark * 4, y + 20, 2.5, 2.5);
                gc.setStroke(Color.color(0.35, 0.15, 0.12, 0.5));
                gc.strokeLine(x + 20, y + 7, x + 25, y + 12);
            }
            case VILLAGE -> {
                if (tileId == Tile.GRASS && mark == 0) {
                    gc.setFill(Color.color(0.96, 0.81, 0.38, 0.7));
                    gc.fillOval(x + 17, y + 10, 3, 3);
                    gc.setFill(Color.color(0.9, 0.7, 0.35, 0.55));
                    gc.fillOval(x + 20, y + 12, 2, 2);
                }
            }
        }
        gc.restore();
    }
    public void renderObjectLayer(GraphicsContext gc,Camera cam) {
        int sc=Math.max(0,cam.getOffsetX()/Tile.SIZE),sr=Math.max(0,cam.getOffsetY()/Tile.SIZE);
        int ec=Math.min(cols,sc+cam.getViewCols()+2),er=Math.min(rows,sr+cam.getViewRows()+2);
        for(int r=sr;r<er;r++) for(int c=sc;c<ec;c++){
            if(objectLayer[r][c]==0) continue;
            tileCache[r][c].render(gc,tileset,c*Tile.SIZE,r*Tile.SIZE,TILES_PER_ROW);
            if (biome == Biome.DUNGEON && c == relicCol && r == relicRow
                    && objectLayer[r][c] == Tile.CHEST) {
                double glow = 0.55 + 0.35 * Math.sin(System.currentTimeMillis() / 260.0);
                gc.setFill(Color.color(1, 0.78, 0.28, glow));
                gc.setFont(javafx.scene.text.Font.font("Serif",
                        javafx.scene.text.FontWeight.BOLD, 15));
                gc.fillText("✦ Dawn Seal", c * Tile.SIZE - 18, r * Tile.SIZE - 5);
            }
        }
        for (ItemDrop drop : drops) {
            gc.setFill(drop.item instanceof Material ? Color.web("#62d8e8") : Color.web("#f0c040"));
            gc.fillOval(drop.x + 8, drop.y + 8, 16, 16);
            gc.setFill(Color.web("#15151d"));
            gc.setFont(javafx.scene.text.Font.font("Serif", javafx.scene.text.FontWeight.BOLD, 9));
            gc.fillText(drop.item instanceof Material ? "M" : "*", drop.x + 13, drop.y + 19);
        }
        renderTransitionHints(gc);
    }
    public void renderEntities(GraphicsContext gc,Camera cam,Player player) {
        List<Entity> all=new ArrayList<>();
        all.add(player); all.addAll(npcs); all.addAll(enemies); all.addAll(player.getParty());
        all.sort(Comparator.comparingDouble(e->e.getY()+e.getHeight()));
        for(Entity e:all){
            if(!cam.isInView(e.getX(),e.getY(),e.getWidth(),e.getHeight())) continue;
            e.render(gc);
            if(e instanceof NPC n) n.renderNameTag(gc,player);
        }
        for(Particle p:particles) p.render(gc);
    }
    public void renderOverlayLayer(GraphicsContext gc,Camera cam) {
        int sc=Math.max(0,cam.getOffsetX()/Tile.SIZE),sr=Math.max(0,cam.getOffsetY()/Tile.SIZE);
        int ec=Math.min(cols,sc+cam.getViewCols()+2),er=Math.min(rows,sr+cam.getViewRows()+2);
        for(int r=sr;r<er;r++) for(int c=sc;c<ec;c++){
            if(overlayLayer[r][c]==0) continue;
            new Tile(overlayLayer[r][c]).render(gc,tileset,c*Tile.SIZE,r*Tile.SIZE,TILES_PER_ROW);
        }
    }
    private void renderTransitionHints(GraphicsContext gc) {
        for(MapTransition t:transitions){
            double pulse=0.25+0.15*Math.sin(System.currentTimeMillis()*0.003);
            gc.setFill(Color.color(0.85,0.78,0.3,pulse));
            gc.fillRect(t.getTriggerX(),t.getTriggerY(),t.getTriggerW(),t.getTriggerH());
            gc.setFill(Color.color(1.0,0.9,0.4,pulse+0.25));
            String arrow=switch(t.getDirection()){case NORTH->"▲";case SOUTH->"▼";case EAST->"▶";case WEST->"◀";};
            gc.setFont(javafx.scene.text.Font.font("Serif",20));
            gc.fillText(arrow,t.getTriggerX()+t.getTriggerW()/2-10,t.getTriggerY()+t.getTriggerH()/2+8);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private boolean inBounds(int r,int c){return r>=0&&r<rows&&c>=0&&c<cols;}
    private void loadFromJson(String path,AssetLoader loader) {
        int[][][]layers=loader.loadMapLayers(path);
        groundLayer=layers[0];objectLayer=layers[1];overlayLayer=layers[2];
        float[]start=loader.loadMapStart(path);startX=start[0];startY=start[1];
        mapName=loader.loadMapName(path);
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getGroundTileId(int row, int col) { return groundLayer[row][col]; }
    public int getObjectTileId(int row, int col) { return objectLayer[row][col]; }
    public int getOverlayTileId(int row, int col) { return overlayLayer[row][col]; }

    public int getWidthPx()  {return cols*Tile.SIZE;}
    public int getHeightPx() {return rows*Tile.SIZE;}
    public float  getStartX()      {return startX;}
    public float  getStartY()      {return startY;}
    public String getMapName()     {return mapName;}
    public Biome  getBiome()       {return biome;}
    public List<NPC>          getNpcs()         {return npcs;}
    public List<Enemy>        getEnemies()      {return enemies;}
    public List<LightSource>  getLightSources() {return lightSources;}

    // ── Inner types ───────────────────────────────────────────────────────
    private static class ItemDrop {
        Item item; float x,y;
        ItemDrop(Item i,float x,float y){item=i;this.x=x;this.y=y;}
        float distanceTo(Entity e){float dx=x-e.getX(),dy=y-e.getY();return(float)Math.sqrt(dx*dx+dy*dy);}
    }
    public static class Particle {
        float x,y; double life=1.2; String text; Color color; float vy=-30f;
        Particle(float x,float y,String t,Color c){this.x=x;this.y=y;text=t;color=c;}
        boolean update(double dt){y+=vy*dt;vy*=0.96f;life-=dt;return life>0;}
        void render(GraphicsContext gc){
            gc.setGlobalAlpha(Math.min(1,life*2));gc.setFill(color);
            gc.setFont(javafx.scene.text.Font.font("Serif",13));gc.fillText(text,x,y);gc.setGlobalAlpha(1.0);
        }
    }
}
