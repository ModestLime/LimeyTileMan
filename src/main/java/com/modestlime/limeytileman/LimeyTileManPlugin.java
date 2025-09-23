/*
 * Copyright (c) 2025, ModestLime <ModestLime@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.modestlime.limeytileman;
import com.google.inject.Provides;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.util.ImageUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Timer;
import java.util.stream.Collectors;


@PluginDescriptor(
        name = "Limey Tile Man",
        description = "A Fast and Efficient TileMan Plugin With Extra Features"
)
@Slf4j
public class LimeyTileManPlugin extends Plugin{

    @Provides
    LimeyTileManConfig provideConfig(ConfigManager configManager){return configManager.getConfig(LimeyTileManConfig.class);}

    @Inject
    private LimeyTileManConfig config;

    @Inject
    private LimeyTileManOverlay overlay;

    @Inject
    private LimeyTileManTileInfoOverlay tileInfoOverlay;

    @Inject
    private LimeyTileManMiniMapOverlay limeyTileManMiniMapOverlay;

    @Inject
    private LimeyTileManWorldMapOverlay limeyTileManWorldMapOverlay;

    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private Gson gson;

    @Inject
    private ConfigManager configManager;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClientThread clientThread;

    public List<WorldPoint> toRender;

    public List<WorldPoint> oneClickTiles;

    public long markedTileCount;

    public long xpToNextTile;

    public long availableTiles;

    private WorldPoint lastWorldPoint;

    public boolean easyDeleteMode;

    public HashMap<Integer, List<WorldPoint>> markedTiles;

    private static final Path tileManDir = RuneLite.RUNELITE_DIR.toPath().resolve("TileMan");

    private Path currentProfileFile;

    public Color tileColor;

    private static final ColorSpace linearRgbCS = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);

    private NavigationButton navButton;

    private LimeyTileManPanel limeyTileManPanel;

    private java.util.Timer autoSaveTimer;

    @Inject
    private PluginManager pluginManager;

    private Plugin gpuPlugin;

    private Plugin hdGpuPlugin;

    public boolean gpuEnabled;

    private static final String gpuPluginName = "GPU";
    private static final String hdGpuPluginName = "117 HD";


    @Override
    protected void startUp() throws Exception
    {

        log.warn("limey tile man started!");
//        log.info(String.valueOf(configManager.getProfile().getId()));

        currentProfileFile = tileManDir.resolve(configManager.getProfile().getId() + ".json");
        lastWorldPoint = null;
        markedTiles = new HashMap<>();
        toRender = new ArrayList<>();
        oneClickTiles = new ArrayList<>();
        markedTileCount = 0;
        xpToNextTile = 0;
        availableTiles = 0;
        easyDeleteMode = false;
        tileColor = config.tileColor();


        loadPoints();
        overlayManager.add(overlay);
        overlayManager.add(tileInfoOverlay);
        overlayManager.add(limeyTileManMiniMapOverlay);
        overlayManager.add(limeyTileManWorldMapOverlay);

        limeyTileManPanel = injector.getInstance(LimeyTileManPanel.class);

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "Lime.png");
        navButton = NavigationButton.builder()
                .tooltip("Limey Tile Man")
                .icon(icon)
                .priority(7)
                .panel(limeyTileManPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        eventBus.register(limeyTileManPanel);

        Files.createDirectories(tileManDir);

        autoSaveTimer = new Timer();
        long fiveMinutesInMillis = 5 * 60 * 1000;
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clientThread.invokeLater(LimeyTileManPlugin.this::savePoints);
            }
        },fiveMinutesInMillis,fiveMinutesInMillis);


        Collection<Plugin> pluginList = pluginManager.getPlugins();
        for(Plugin plugin : pluginList){
            if(plugin.getName().equals(gpuPluginName)){
                gpuPlugin = plugin;
            }
            if(plugin.getName().equals(hdGpuPluginName)){
                hdGpuPlugin = plugin;
            }
        }
        gpuEnabled = (gpuPlugin != null && pluginManager.isPluginEnabled(gpuPlugin)) || (hdGpuPlugin != null && pluginManager.isPluginEnabled(hdGpuPlugin));

    }


    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(tileInfoOverlay);
        overlayManager.remove(limeyTileManMiniMapOverlay);
        overlayManager.remove(limeyTileManWorldMapOverlay);
        clientToolbar.removeNavigation(navButton);
        eventBus.unregister(limeyTileManPanel);
        autoSaveTimer.cancel();
        savePoints(); //save tiles when plugin is disabled
    }


    @Subscribe
    public void onClientShutdown(ClientShutdown event){
        savePoints();  //save tiles when game is closed
    }


    @Subscribe
    public void onProfileChanged(ProfileChanged event)
    {
        savePoints();
        currentProfileFile = tileManDir.resolve(configManager.getProfile().getId() + ".json");
        markedTiles = new HashMap<>();
        loadPoints();
    }


    @Subscribe
    public void onPluginChanged(PluginChanged pluginChanged){
        if(pluginChanged.getPlugin().equals(gpuPlugin)){
            gpuEnabled = pluginChanged.isLoaded() || (hdGpuPlugin != null && pluginManager.isPluginEnabled(hdGpuPlugin));
        }
        if(pluginChanged.getPlugin().getName().equals(hdGpuPluginName)){
            if(!pluginChanged.getPlugin().equals(hdGpuPlugin)){
                hdGpuPlugin = pluginChanged.getPlugin();
            }
        }
        if(pluginChanged.getPlugin().getName().equals(hdGpuPluginName)){  //if 117hd gets uninstalled then set it to null
            if(!pluginChanged.isLoaded() && pluginManager.isPluginEnabled(hdGpuPlugin)){
                hdGpuPlugin = null;
                gpuEnabled = false;
            }
        }

        if(pluginChanged.getPlugin().equals(hdGpuPlugin)){
            gpuEnabled = pluginChanged.isLoaded() || (gpuPlugin != null && pluginManager.isPluginEnabled(gpuPlugin));
        }

    }


    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged){    //onConfigChanged isn't on the client thread
        if(configChanged.getGroup().equals(config.GROUP)){
            updateTileInfo();
            if(configChanged.getKey().equals(config.AutoMark)){
                if(configChanged.getNewValue() == null){ //this happens on a brand-new profile creation
                    return;
                }
                if(configChanged.getNewValue().equals("true")){
                    //this avoids a race condition
                    clientThread.invokeLater(this::placeTileUnderPlayer); //overkill but now it marks the tile right away instead of after you move when you just enable it
                }
            }
            if(configChanged.getKey().equals(config.RenderDistance)){
                clientThread.invokeLater((Runnable) this::generateToRender);
            }
        }
    }


    private void placeTileUnderPlayer(){
        boolean allowedToMark = config.allowDeficit() || availableTiles > 0;
        if(allowedToMark) {
            Player localPlayer = client.getLocalPlayer();
            final WorldPoint playerPos = localPlayer.getWorldLocation();
            addToMarked(playerPos);   //overkill but now it marks the tile right away instead of after you move when you just enable it
        }
    }


    public void deleteAllTiles(){
        markedTiles = new HashMap<>();
        markedTileCount = 0;
        updateTileInfo();
        SwingUtilities.invokeLater(() -> limeyTileManPanel.tilesDeleteSuccess());
    }


    @Subscribe
    public void onGameTick(GameTick tick) {
        Player localPlayer = client.getLocalPlayer();
        final WorldPoint playerPos = localPlayer.getWorldLocation();

        boolean allowedToMark = config.allowDeficit() || availableTiles > 0;
        if(config.autoMark() & allowedToMark) {
            if (!playerPos.equals(lastWorldPoint)) {
                List<WorldPoint> path = bfsPathFinding(lastWorldPoint, playerPos, 3);
                if (path.isEmpty()) { //we teleported
                    addToMarked(playerPos);
                }
                for (WorldPoint point : path) {
                    addToMarked(point);
                    if(availableTiles == 0 && !config.allowDeficit()){break;}
                }
            }
        }

        updateTileInfo();

        generateToRender(playerPos);

        lastWorldPoint = playerPos;
    }


    public void generateToRender(){
        Player player = client.getLocalPlayer();
        if(player != null) {
            generateToRender(player.getWorldLocation());
        }
    }


    public void generateToRender(WorldPoint playerPos){
        WorldView wv = client.getTopLevelWorldView();
        LocalPoint lp = LocalPoint.fromWorld(wv, playerPos);
        if(lp == null){return;}
        WorldPoint playerPosFromLocal = WorldPoint.fromLocalInstance(client, lp, wv.getPlane());

        int regionLocalX = playerPosFromLocal.getX() & 7; //my regions are 8x8 and doing & 7 give us the remainder
        int regionLocalY = playerPosFromLocal.getY() & 7;

        int westRegionAmount = ((config.renderDistance() - regionLocalX) + 7) >> 3;  //Math.ceil((double) number / 8)
        int eastRegionAmount = (config.renderDistance() + regionLocalX) >> 3;

        int northRegionAmount = (config.renderDistance() + regionLocalY) >> 3;
        int southRegionAmount = ((config.renderDistance() - regionLocalY) + 7) >> 3;

        int maxLength = Math.max(westRegionAmount,
                        Math.max(eastRegionAmount,
                        Math.max(northRegionAmount, southRegionAmount)));


        List<WorldPoint> regionsToRender = new ArrayList<>();
        for (int i = maxLength; i >= 0; i--) {
            regionsToRender.addAll(regionSquareEdge(playerPosFromLocal, i)); //list of all regions to render from farthest to closest
        }

        List<WorldPoint> allTilesToRender = new ArrayList<>();
        for(WorldPoint region: regionsToRender){
            List<WorldPoint> regionTiles = markedTiles.get(toHash(region));
            if(regionTiles != null){
                allTilesToRender.addAll(regionTiles);
            }
        }

        int minX = playerPosFromLocal.getX() - config.renderDistance();
        int maxX = playerPosFromLocal.getX() + config.renderDistance();
        int minY = playerPosFromLocal.getY() - config.renderDistance();
        int maxY = playerPosFromLocal.getY() + config.renderDistance();

        allTilesToRender = allTilesToRender.stream().filter(worldPoint ->
                (worldPoint.getX() >= minX) && (worldPoint.getX() <= maxX) && (worldPoint.getY() >= minY) && (worldPoint.getY() <= maxY)
            ).flatMap(worldPoint -> {
                Collection<WorldPoint> localInstances = WorldPoint.toLocalInstance(wv, worldPoint);
                return localInstances.stream();
            }).collect(Collectors.toList());

        oneClickTiles = new ArrayList<>();
        toRender = new ArrayList<>();
        if(config.oneClickTiles()){
            WorldPoint[][] parentMap = getBfsPathFindingParentMap(playerPos);

            while(!allTilesToRender.isEmpty()){                       // I could probably make a 104x104 boolean "true if already checked" to speed it up instead of
                List<WorldPoint> currentPath = new ArrayList<>();     //doing a bunch of .contains / .indexOf operations;
                WorldPoint connectingTile = allTilesToRender.get(0);
                currentPath.add(allTilesToRender.remove(0));

                while(true) {
                    LocalPoint tileLocal = LocalPoint.fromWorld(wv, connectingTile);
                    if(tileLocal == null){
                        toRender.addAll(currentPath);
                        break;
                    }

                    connectingTile = parentMap[tileLocal.getSceneX()][tileLocal.getSceneY()];

                    if(connectingTile == null){
                        toRender.addAll(currentPath);
                        setParentPath(wv, currentPath, parentMap, null);
                        break;
                    }
                    if(connectingTile.equals(playerPos)){
                        oneClickTiles.addAll(currentPath);
                        setParentPath(wv, currentPath, parentMap, playerPos);
                        break;
                    }

                    int index = allTilesToRender.indexOf(connectingTile);
                    if(index != -1){
                        currentPath.add(allTilesToRender.remove(index));
                    }else if (oneClickTiles.contains(connectingTile)) {
                        oneClickTiles.addAll(currentPath);
                        setParentPath(wv, currentPath, parentMap, playerPos);
                        break;
                    }else{
                        toRender.addAll(currentPath);
                        setParentPath(wv, currentPath, parentMap, null);
                        break;
                    }
                }

            }
        }else{
            toRender = allTilesToRender;
        }
        generateTileColors();
    }



    private void generateTileColors(){
        tileColor = config.tileColor();
        if(config.warnToggle()){
            if(availableTiles <= config.warnThreshold()){
                tileColor = config.warnColor();
            }
            if(availableTiles <= 0){
                tileColor = config.warnColorZero();
            }
        }
        if(config.gradualFade() && config.warnToggle()){
            Color color1;
            Color color2;
            float ratio1;
            float ratio2;
            if(availableTiles > config.warnThreshold()){
                ratio1 = (availableTiles - config.warnThreshold()) / 10f; //start to fade the normal color to the warning when we are with in 10tiles
                ratio1 = Math.min(1f, ratio1);
                ratio2 = 1f - ratio1;
                color1 = config.tileColor();
                color2  = config.warnColor();

                //mix tile and warn color
            }else {
                ratio1 = availableTiles / (float)config.warnThreshold();
                ratio1 = Math.max(0f, ratio1);
                ratio2 = 1f - ratio1;
                color1 = config.warnColor();
                color2  = config.warnColorZero();
                //mix warn color and zero tile
            }

            int alpha = (int)(color1.getAlpha() * ratio1 + color2.getAlpha() * ratio2);
            assert alpha <= 255;

            float[] color1RGBColorComponents = color1.getRGBColorComponents(null);
            float[] color2RGBColorComponents = color2.getRGBColorComponents(null);

            float[] color1LinearColorComponents = linearRgbCS.fromRGB(color1RGBColorComponents);
            float[] color2LinearColorComponents = linearRgbCS.fromRGB(color2RGBColorComponents);

            float[] blendedLinearRgb = new float[3];
            blendedLinearRgb[0] = color1LinearColorComponents[0] * ratio1 + color2LinearColorComponents[0] * ratio2; // Red
            blendedLinearRgb[1] = color1LinearColorComponents[1] * ratio1 + color2LinearColorComponents[1] * ratio2; // Green
            blendedLinearRgb[2] = color1LinearColorComponents[2] * ratio1 + color2LinearColorComponents[2] * ratio2; // Blue

            float[] blendedRgb = linearRgbCS.toRGB(blendedLinearRgb);

            tileColor = new Color(blendedRgb[0], blendedRgb[1], blendedRgb[2], alpha / 255f);

        }
    }


    private void setParentPath(WorldView wv, List<WorldPoint> tiles, WorldPoint[][] parentMap, WorldPoint setTo){
        for(WorldPoint tile : tiles){
            LocalPoint tileLocal = LocalPoint.fromWorld(wv, tile);
            assert tileLocal != null;
            parentMap[tileLocal.getSceneX()][tileLocal.getSceneY()] = setTo;
        }
    }


    @Subscribe
    public void onClientTick(ClientTick tick){
        if(easyDeleteMode){
            WorldView wv = client.getTopLevelWorldView();
            Tile currentTile = wv.getSelectedSceneTile();
            if(currentTile != null) {
                WorldPoint mouseTile = currentTile.getWorldLocation();
                long oldMarkedCount = markedTileCount;
                if(client.isKeyPressed(KeyCode.KC_CONTROL)){
                    if (config.allowDeficit() || availableTiles > 0) {
                        addToMarked(mouseTile);
                    }
                }
                if(client.isKeyPressed(KeyCode.KC_ALT)){
                    removeFromMarked(mouseTile);
                }
                if(markedTileCount != oldMarkedCount){
                    generateToRender();
                    updateTileInfo();
                }
            }
        }
    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event){
        final boolean holdingShiftKey = client.isKeyPressed(KeyCode.KC_SHIFT);
        if (holdingShiftKey && event.getOption().equals("Walk here")) {
            WorldView wv = client.getTopLevelWorldView();
            final Tile selectedSceneTile = wv.getSelectedSceneTile();
            if (selectedSceneTile == null)
            {
                return;
            }

            WorldPoint point = selectedSceneTile.getWorldLocation();
            LocalPoint lp = LocalPoint.fromWorld(wv, point);
            if(lp == null){return;}
            WorldPoint realPoint = WorldPoint.fromLocalInstance(client, lp, wv.getPlane());
            WorldPoint regionID = toRegion(realPoint);
            List<WorldPoint> regionTiles = markedTiles.get(toHash(regionID));
            if(regionTiles == null){
                regionTiles = new ArrayList<>(0); //empty just so we can call contains
            }
            boolean hasMarkedTile = regionTiles.contains(realPoint);

            Menu menu = client.getMenu();
            menu.createMenuEntry(-1)
                .setOption(hasMarkedTile ? "Remove TileMan" : "Add TileMan")
                .setTarget("<col=09ff00>Tile")
                .setType(MenuAction.RUNELITE).onClick(menuEntry -> {
                    if(hasMarkedTile){
                        removeFromMarked(point);
                    }else{
                        if (config.allowDeficit() || availableTiles > 0) {
                            addToMarked(point);
                        }
                    }
                    generateToRender();
                    updateTileInfo();
                });
        }
        final boolean holdingDeleteKey = client.isKeyPressed(KeyCode.KC_DELETE);
        if (holdingDeleteKey && event.getOption().equals("Walk here")) {
            Menu menu = client.getMenu();
            menu.createMenuEntry(-1)
                .setOption(easyDeleteMode ? "Disable" : "Enable")
                .setTarget("<col=ff00e6>Easy Tile Deletion")
                .setType(MenuAction.RUNELITE).onClick(menuEntry -> easyDeleteMode = !easyDeleteMode);
        }

    }


    private void updateTileInfo(){
        availableTiles = 0;

        long totalPlayerXp = client.getOverallExperience();
        long totalTiles = (totalPlayerXp / config.xpPerTile());
        xpToNextTile = config.xpPerTile() - (totalPlayerXp % config.xpPerTile());

        if(config.addTilesOnXp()) {
            availableTiles += totalTiles;
        }
        if(config.addTileOnLevel()){
            availableTiles += (long) client.getTotalLevel() * config.tilesPerLevel();
        }
        availableTiles += config.addTiles();
        availableTiles -= config.subtractTiles();
        availableTiles -= markedTileCount;
    }

    private boolean isBlocked(WorldView wv,LocalPoint lp){
        CollisionData[] collisionData = wv.getCollisionMaps();
        if(collisionData != null) {
            int[][] flags = collisionData[wv.getPlane()].getFlags();
            int movementFlag = flags[lp.getSceneX()][lp.getSceneY()];
            return (movementFlag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
        }
        return true;
    }

    /**
     *
     * @param point a normal {@link WorldPoint} to be added to markedTiles
     */
    private void addToMarked(WorldPoint point){
        WorldView wv = client.getTopLevelWorldView();
        LocalPoint lp = LocalPoint.fromWorld(wv, point);
        if(lp == null){return;}
        if(isBlocked(wv,lp)){return;}
        point = WorldPoint.fromLocalInstance(client, lp, wv.getPlane());
        WorldPoint regionID = toRegion(point);
        List<WorldPoint> regionTiles = markedTiles.get(toHash(regionID));
        if(regionTiles == null){
            regionTiles = new ArrayList<>(8 * 8);
        }
        if(!regionTiles.contains(point)){
            regionTiles.add(point);
            markedTileCount++;
            availableTiles--;
        }
        markedTiles.put(toHash(regionID), regionTiles);
    }


    private void removeFromMarked(WorldPoint point){
        WorldView wv = client.getTopLevelWorldView();
        LocalPoint lp = LocalPoint.fromWorld(wv, point);
        if(lp == null){return;}
        point = WorldPoint.fromLocalInstance(client, lp, wv.getPlane());

        WorldPoint regionID = toRegion(point);
        List<WorldPoint> regionTiles = markedTiles.get(toHash(regionID));
        if(regionTiles == null){
            return;
        }
        if(regionTiles.contains(point)){
            regionTiles.remove(point);
            markedTileCount--;
            availableTiles++;
        }
        markedTiles.put(toHash(regionID), regionTiles);
    }


    private List<WorldPoint> regionSquareEdge(WorldPoint center, int length){
        List<WorldPoint> pointsOnEdge = new ArrayList<>();
        int plane = center.getPlane();
        WorldPoint regionCenter = toRegion(center);
        int xRegion = regionCenter.getX();
        int yRegion = regionCenter.getY();

        if (length == 0) { // Special case for a 1x1 square
            pointsOnEdge.add(new WorldPoint(xRegion, yRegion, plane));
            return pointsOnEdge;
        }

        int minX = xRegion - length;
        int maxX = xRegion + length;
        int minY = yRegion - length;
        int maxY = yRegion + length;



        // Add top and bottom horizontal lines
        for (int x = minX; x <= maxX; x++) {
            pointsOnEdge.add(new WorldPoint(x, minY, plane));
            pointsOnEdge.add(new WorldPoint(x, maxY, plane));
        }

        // Add left and right vertical lines (excluding corners to avoid duplicates)
        for (int y = minY + 1; y < maxY; y++) { // Start y from minY + 1 and end at maxY - 1
            pointsOnEdge.add(new WorldPoint(minX, y, plane));
            pointsOnEdge.add(new WorldPoint(maxX, y, plane));
        }
        return pointsOnEdge;
    }

    /**
     *
     * @param wp takes a regioned WorldPoint, see {@link LimeyTileManPlugin#toRegion(WorldPoint)}
     * @return returns a unique int representing the regioned WorldPoint
     */
    public int toHash(WorldPoint wp){
        int x = wp.getX();
        int y = wp.getY();
        int plane = wp.getPlane();

        return (x << 16) | (y << 4) | plane;
    }


    private void savePoints(){
        String json = gson.toJson(markedTiles);
        try{
            Files.writeString(currentProfileFile, json);
        }catch (IOException e){
            log.error("error writing to file ", e);
        }

    }


    private void loadPoints(){
        if(!Files.exists(currentProfileFile)){
            savePoints();
        }
        String json = "";
        try {
            json = Files.readString(currentProfileFile);
        }catch (IOException e){
            log.error("error reading file ", e);
        }

        markedTiles = gson.fromJson(json, new TypeToken<HashMap<Integer, List<WorldPoint>>>(){}.getType());
        for( List<WorldPoint> region: markedTiles.values()){
            markedTileCount += region.size();

        }
    }


    public WorldPoint toRegion(WorldPoint wp){
        int x = wp.getX();
        int y = wp.getY();
        int plane = wp.getPlane();

        int xRegion = x >> 3;
        int yRegion = y >> 3;

        return new WorldPoint(xRegion, yRegion, plane);
    }


    public List<WorldPoint> bfsPathFinding(WorldPoint start, @Nonnull WorldPoint end, int stopDepth){
        if(start == null) { //plugin has just started null will always be passed in
            return List.of(end);
        }
        if(start.equals(end)){
            return List.of(start);
        }
        WorldArea startArea = start.toWorldArea();
        WorldView wv =  client.getTopLevelWorldView();
        int plane = wv.getPlane();

        Queue<WorldArea> queue = new ArrayDeque<>();
        WorldPoint[][] parentMap = new WorldPoint[104][104];

        queue.offer(startArea);
        LocalPoint lp = LocalPoint.fromWorld(wv,startArea.toWorldPoint());
        if(lp == null){
            return new ArrayList<>();
        }
        parentMap[lp.getSceneX()][lp.getSceneY()] = null;

        WorldArea currentNode = null;
        int currentDepth = 0;
        whileLoop:
        while (!queue.isEmpty()) {
            if(currentDepth == stopDepth){
                break;
            }
            int depthSize = queue.size();

            for (int i = 0; i < depthSize; i++) {

                currentNode = queue.poll();

                assert currentNode != null;
                if (currentNode.toWorldPoint().equals(end)) {
                    break whileLoop;
                }

                int xCord = currentNode.getX();
                int yCord = currentNode.getY();
                WorldPoint currentWP = currentNode.toWorldPoint();
                lp = LocalPoint.fromWorld(wv, currentWP);
                assert lp != null;
                int localX = lp.getSceneX();
                int localY = lp.getSceneY();

                if (parentMap[localX - 1][localY] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, 0)) {   //west
                        queue.add(new WorldArea(xCord - 1, yCord, 1, 1, plane));
                        parentMap[localX - 1][localY] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, 0)) {   //east
                        queue.add(new WorldArea(xCord + 1, yCord, 1, 1, plane));
                        parentMap[localX + 1][localY] = currentWP;
                    }
                }
                if (parentMap[localX][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 0, -1)) {   //south
                        queue.add(new WorldArea(xCord, yCord - 1, 1, 1, plane));
                        parentMap[localX][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 0, 1)) {   //north
                        queue.add(new WorldArea(xCord, yCord + 1, 1, 1, plane));
                        parentMap[localX][localY + 1] = currentWP;
                    }
                }
                if (parentMap[localX - 1][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, -1)) {   //south-west
                        queue.add(new WorldArea(xCord - 1, yCord - 1, 1, 1, plane));
                        parentMap[localX - 1][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, -1)) {   //south-east
                        queue.add(new WorldArea(xCord + 1, yCord - 1, 1, 1, plane));
                        parentMap[localX + 1][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX - 1][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, 1)) {   //north-west
                        queue.add(new WorldArea(xCord - 1, yCord + 1, 1, 1, plane));
                        parentMap[localX - 1][localY + 1] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, 1)) {   //north-east
                        queue.add(new WorldArea(xCord + 1, yCord + 1, 1, 1, plane));
                        parentMap[localX + 1][localY + 1] = currentWP;
                    }
                }
            }
            currentDepth++;
        }

        assert currentNode != null;
        if(currentNode.toWorldPoint().equals(end)){
            List<WorldPoint> path = new ArrayList<>();
            WorldPoint wp = end;
            path.add(wp);
            while (true){
                lp = LocalPoint.fromWorld(wv,wp);
                assert lp != null;
                wp = parentMap[lp.getSceneX()][lp.getSceneY()];
                path.add(wp);
                if(wp.equals(start)){
                    Collections.reverse(path);
                    return path;
                }
            }

        }
        return new ArrayList<>();
    }


    public WorldPoint[][] getBfsPathFindingParentMap(@Nonnull WorldPoint start){

        WorldArea startArea = start.toWorldArea();
        WorldView wv =  client.getTopLevelWorldView();
        int plane = wv.getPlane();

        Queue<WorldArea> queue = new ArrayDeque<>();
        WorldPoint[][] parentMap = new WorldPoint[104][104];

        queue.offer(startArea);
        LocalPoint lp = LocalPoint.fromWorld(wv,startArea.toWorldPoint());
        if(lp == null){
            return null;
        }
        parentMap[lp.getSceneX()][lp.getSceneY()] = start;

        WorldArea currentNode;

        while (!queue.isEmpty()) {
            int depthSize = queue.size();

            for (int i = 0; i < depthSize; i++) {

                currentNode = queue.poll();

                assert currentNode != null;

                int xCord = currentNode.getX();
                int yCord = currentNode.getY();
                WorldPoint currentWP = currentNode.toWorldPoint();
                lp = LocalPoint.fromWorld(wv, currentWP);
                assert lp != null;  //localPoint will always be valid as we cant leave the 104x104 collision map area
                int localX = lp.getSceneX();
                int localY = lp.getSceneY();


                if (parentMap[localX - 1][localY] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, 0)) {   //west
                        queue.add(new WorldArea(xCord - 1, yCord, 1, 1, plane));
                        parentMap[localX - 1][localY] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, 0)) {   //east
                        queue.add(new WorldArea(xCord + 1, yCord, 1, 1, plane));
                        parentMap[localX + 1][localY] = currentWP;
                    }
                }
                if (parentMap[localX][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 0, -1)) {   //south
                        queue.add(new WorldArea(xCord, yCord - 1, 1, 1, plane));
                        parentMap[localX][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 0, 1)) {   //north
                        queue.add(new WorldArea(xCord, yCord + 1, 1, 1, plane));
                        parentMap[localX][localY + 1] = currentWP;
                    }
                }
                if (parentMap[localX - 1][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, -1)) {   //south-west
                        queue.add(new WorldArea(xCord - 1, yCord - 1, 1, 1, plane));
                        parentMap[localX - 1][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY - 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, -1)) {   //south-east
                        queue.add(new WorldArea(xCord + 1, yCord - 1, 1, 1, plane));
                        parentMap[localX + 1][localY - 1] = currentWP;
                    }
                }
                if (parentMap[localX - 1][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, -1, 1)) {   //north-west
                        queue.add(new WorldArea(xCord - 1, yCord + 1, 1, 1, plane));
                        parentMap[localX - 1][localY + 1] = currentWP;
                    }
                }
                if (parentMap[localX + 1][localY + 1] == null) {
                    if (currentNode.canTravelInDirection(wv, 1, 1)) {   //north-east
                        queue.add(new WorldArea(xCord + 1, yCord + 1, 1, 1, plane));
                        parentMap[localX + 1][localY + 1] = currentWP;
                    }
                }
            }
        }
        return parentMap;
    }
}
