package com.abyss.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Font;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * AssetLoader — Singleton that loads and caches all game assets.
 *
 * Asset types:
 *   Image      – sprites, tiles, UI elements
 *   AudioClip  – short SFX (attack hit, UI click, step)
 *   MediaPlayer– background music (streamed)
 *   Font       – custom game fonts
 *
 * All assets are loaded from the classpath (src/main/resources/).
 * Missing assets return null / fallback gracefully so the game
 * can run without a complete asset set (development mode).
 *
 * Usage:
 *   Image img = AssetLoader.getInstance().getImage("sprites/player.png");
 */
public class AssetLoader {

    private static final Logger LOG = Logger.getLogger(AssetLoader.class.getName());
    private static final ObjectMapper MAP_JSON = new ObjectMapper();
    private static AssetLoader instance;

    // ── Caches ────────────────────────────────────────────────────────────
    private final Map<String, Image>       images      = new HashMap<>();
    private final Map<String, AudioClip>   sfx         = new HashMap<>();
    private final Map<String, MediaPlayer> music       = new HashMap<>();
    private final Map<String, Font>        fonts       = new HashMap<>();
    private final BlockingQueue<String> fallbackSfxQueue = new LinkedBlockingQueue<>();
    private volatile boolean fallbackSfxWorkerStarted;

    // Current music player
    private MediaPlayer currentMusic;
    private double      musicVolume = 0.7;
    private double      sfxVolume   = 0.9;

    // ── Singleton ─────────────────────────────────────────────────────────
    private AssetLoader() {}

    public static AssetLoader getInstance() {
        if (instance == null) instance = new AssetLoader();
        return instance;
    }

    // ── Load all ──────────────────────────────────────────────────────────
    /**
     * Pre-loads all known assets at game startup.
     * Unknown assets can still be loaded on-demand via getImage() etc.
     */
    public void loadAll() {
        // Sprites
        preloadImage("sprites/player.png");
        preloadImage("sprites/enemy_wolf.png");
        preloadImage("sprites/enemy_golem.png");
        preloadImage("sprites/enemy_wraith.png");
        preloadImage("sprites/boss_malachar.png");

        // Tiles
        preloadImage("tiles/tileset.png");

        // UI
        preloadImage("ui-elements/hotbar_slot.png");
        preloadImage("ui-elements/panel_bg.png");

        // Audio (graceful failure if files absent)
        preloadSfx("audio/attack_hit.mp3");
        preloadSfx("audio/ui_click.mp3");
        preloadSfx("audio/level_up.mp3");
        preloadSfx("audio/item_pickup.mp3");
        preloadSfx("audio/door_open.mp3");
        preloadSfx("audio/step.mp3");

        preloadMusic("audio/theme_village.mp3");
        preloadMusic("audio/theme_battle.mp3");
        preloadMusic("audio/theme_boss.mp3");
        preloadMusic("audio/rain.mp3");

        // Fonts
        preloadFont("fonts/cinzel.ttf", 14);

        LOG.info("[AssetLoader] Assets loaded: " + images.size() + " images, "
                 + sfx.size() + " sfx, " + music.size() + " tracks.");
    }

    // ── Pre-load helpers ──────────────────────────────────────────────────
    private void preloadImage(String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            if (url == null) { LOG.fine("[AssetLoader] Missing: " + path); return; }
            images.put(path, new Image(url.toExternalForm(), false));
        } catch (Exception ex) {
            LOG.fine("[AssetLoader] Failed to load image: " + path + " — " + ex.getMessage());
        }
    }

    private void preloadSfx(String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return;
            sfx.put(path, new AudioClip(url.toExternalForm()));
        } catch (Exception ex) {
            LOG.fine("[AssetLoader] SFX load failed: " + path);
        }
    }

    private void preloadMusic(String path) {
        try {
            URL url = getClass().getClassLoader().getResource(path);
            if (url == null) return;
            Media media = new Media(url.toExternalForm());
            MediaPlayer player = new MediaPlayer(media);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setVolume(musicVolume);
            music.put(path, player);
        } catch (Exception ex) {
            LOG.fine("[AssetLoader] Music load failed: " + path);
        }
    }

    private void preloadFont(String path, double size) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(path);
            if (is == null) return;
            Font f = Font.loadFont(is, size);
            if (f != null) fonts.put(path, f);
        } catch (Exception ex) {
            LOG.fine("[AssetLoader] Font load failed: " + path);
        }
    }

    // ── Public accessors ──────────────────────────────────────────────────
    /**
     * Returns a cached Image, or null if the asset is missing.
     * A missing asset will NOT crash the game — rendering code must handle null.
     */
    public Image getImage(String path) {
        if (images.containsKey(path)) return images.get(path);
        preloadImage(path);
        return images.get(path);
    }

    public void playSfx(String path) {
        AudioClip clip = sfx.get(path);
        if (clip != null) { clip.setVolume(sfxVolume); clip.play(); }
        else enqueueFallbackSfx(path);
    }

    private synchronized void enqueueFallbackSfx(String path) {
        fallbackSfxQueue.offer(path);
        if (fallbackSfxWorkerStarted) return;
        fallbackSfxWorkerStarted = true;
        Thread worker = new Thread(this::runFallbackSfx, "game-sfx");
        worker.setDaemon(true);
        worker.start();
    }

    private void runFallbackSfx() {
        AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
        try {
            while (true) playFallbackTone(fallbackSfxQueue.take(), format);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void playFallbackTone(String path, AudioFormat format) {
        double[] notes;
        double noteSeconds;
        double amplitude;
        switch (path) {
            case "audio/step.mp3" -> { notes = new double[]{105}; noteSeconds = 0.045; amplitude = 0.07; }
            case "audio/ui_click.mp3" -> { notes = new double[]{760}; noteSeconds = 0.055; amplitude = 0.12; }
            case "audio/item_pickup.mp3" -> { notes = new double[]{660, 880}; noteSeconds = 0.09; amplitude = 0.14; }
            case "audio/level_up.mp3" -> { notes = new double[]{523, 659, 784}; noteSeconds = 0.11; amplitude = 0.16; }
            case "audio/door_open.mp3" -> { notes = new double[]{180, 245}; noteSeconds = 0.12; amplitude = 0.12; }
            default -> { notes = new double[]{190, 110}; noteSeconds = 0.07; amplitude = 0.11; }
        }

        int sampleRate = (int) format.getSampleRate();
        int samplesPerNote = (int) (sampleRate * noteSeconds);
        byte[] pcm = new byte[samplesPerNote * notes.length * 2];
        int offset = 0;
        for (double frequency : notes) {
            for (int sample = 0; sample < samplesPerNote; sample++) {
                double attack = Math.min(1.0, sample / 180.0);
                double release = Math.min(1.0, (samplesPerNote - sample) / 600.0);
                double envelope = Math.min(attack, release);
                short value = (short) (Math.sin(2 * Math.PI * frequency * sample / sampleRate)
                        * amplitude * sfxVolume * envelope * Short.MAX_VALUE);
                pcm[offset++] = (byte) value;
                pcm[offset++] = (byte) (value >> 8);
            }
        }

        SourceDataLine line = null;
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            line.start();
            line.write(pcm, 0, pcm.length);
            line.drain();
        } catch (Exception ex) {
            LOG.fine("[AssetLoader] Procedural SFX unavailable: " + ex.getMessage());
        } finally {
            if (line != null) line.close();
        }
    }

    public void playMusic(String path) {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        MediaPlayer player = music.get(path);
        if (player != null) {
            currentMusic = player;
            player.play();
        }
    }

    public void stopMusic() {
        if (currentMusic != null) currentMusic.stop();
    }

    public Font getFont(String path) { return fonts.getOrDefault(path, Font.getDefault()); }

    // ── Volume control ────────────────────────────────────────────────────
    public void setMusicVolume(double v) {
        musicVolume = v;
        music.values().forEach(p -> p.setVolume(v));
    }

    public void setSfxVolume(double v) {
        sfxVolume = v;
        sfx.values().forEach(clip -> clip.setVolume(v));
    }

    // ── Map helpers ───────────────────────────────────────────────────────
    /** Returns {cols, rows} from the map metadata, or a default. */
    public int[] getMapDimensions(String path) {
        JsonNode header = readMapHeader(path);
        return new int[]{positiveInt(header, "cols", 40), positiveInt(header, "rows", 30)};
    }

    public boolean mapExists(String path) {
        return getClass().getClassLoader().getResource(path) != null;
    }

    public int[][][] loadMapLayers(String path) {
        // Delegate to a JSON parser (Jackson); stub returns empty arrays
        int[] dimensions = getMapDimensions(path);
        int cols = dimensions[0], rows = dimensions[1];
        return new int[3][rows][cols];
    }

    public float[] loadMapStart(String path) {
        JsonNode header = readMapHeader(path);
        return new float[]{positiveInt(header, "startX", 5) * 32f,
                positiveInt(header, "startY", 5) * 32f};
    }

    public String loadMapName(String path) {
        JsonNode header = readMapHeader(path);
        return header == null ? "Unknown" : header.path("name").asText("Unknown");
    }

    private JsonNode readMapHeader(String path) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            return stream == null ? null : MAP_JSON.readTree(stream);
        } catch (IOException ex) {
            LOG.warning("Unable to read map metadata " + path + ": " + ex.getMessage());
            return null;
        }
    }

    private int positiveInt(JsonNode node, String field, int fallback) {
        if (node == null) return fallback;
        int value = node.path(field).asInt(fallback);
        return value > 0 ? value : fallback;
    }
}
