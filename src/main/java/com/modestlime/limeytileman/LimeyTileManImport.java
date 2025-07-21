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

import com.google.gson.reflect.TypeToken;
import com.modestlime.limeytileman.events.*;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import com.google.gson.Gson;
import net.runelite.client.eventbus.EventBus;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;



@Slf4j
public class LimeyTileManImport {

    @Inject
    private ConfigManager configManager;

    final private LimeyTileManPlugin plugin;

    final private EventBus eventBus;

    @Inject
    private Gson gson;

    private static final Type TILE_LIST_TYPE = new TypeToken<List<TileManModeTile>>() {}.getType();

    @Inject
    LimeyTileManImport(LimeyTileManPlugin plugin, EventBus eventBus){
        this.plugin = plugin;
        this.eventBus = eventBus;
    }


    public void importTilesFromLeckey(){
        List<String> tileManModeKeys = configManager.getConfigurationKeys("tilemanMode.region");
        List<String> regionKeys = tileManModeKeys.stream().map(key -> key.substring(12)).collect(Collectors.toList());

        List<TileManModeTile> tileManModeTiles = new ArrayList<>();
        for (String subKey: regionKeys) {
            String json = configManager.getConfiguration("tilemanMode", subKey);
            try{
                tileManModeTiles.addAll(gson.fromJson(json, TILE_LIST_TYPE));
            }catch (Exception ignored){}
        }

        if(tileManModeTiles.isEmpty()){
            eventBus.post(new LeckeyTilesImportFailed());
            return;
        }

        List<WorldPoint> worldPoints = tileManModeTiles.stream().map(tileManModeTile ->
                WorldPoint.fromRegion(tileManModeTile.getRegionId(), tileManModeTile.getRegionX(), tileManModeTile.getRegionY(), tileManModeTile.getZ())
        ).collect(Collectors.toList());


        for(WorldPoint point : worldPoints) {
            WorldPoint regionID = plugin.toRegion(point);
            List<WorldPoint> regionTiles = plugin.markedTiles.get(plugin.toHash(regionID));
            if (regionTiles == null) {
                regionTiles = new ArrayList<>(8 * 8);
            }
            if (!regionTiles.contains(point)) {
                regionTiles.add(point);
                plugin.markedTileCount++;
                plugin.availableTiles--;
            }
            plugin.markedTiles.put(plugin.toHash(regionID), regionTiles);
        }
        eventBus.post(new LeckeyTilesImported());
    }


    public void copyTilesToClipBoard(){
        String json = gson.toJson(plugin.markedTiles);
        StringSelection transferableJson = new StringSelection(json);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableJson,null);
        eventBus.post(new TilesCopied());
    }


    public void importFromClipBoard(){
        String json = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)){
            try {
            json = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                eventBus.post(new ClipboardImportFailed());
                return;
            }
        }
        if(json.isBlank()){eventBus.post(new ClipboardImportFailed());}

        HashMap<Integer, List<WorldPoint>> importedTiles;
        try {
            importedTiles = gson.fromJson(json, new TypeToken<HashMap<Integer, List<WorldPoint>>>() {}.getType());
        } catch (Exception e){
            eventBus.post(new ClipboardImportFailed());
            return;
        }

        List<WorldPoint> combindedList = importedTiles.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        for(WorldPoint point : combindedList) {
            WorldPoint regionID = plugin.toRegion(point);
            List<WorldPoint> regionTiles = plugin.markedTiles.get(plugin.toHash(regionID));
            if (regionTiles == null) {
                regionTiles = new ArrayList<>(8 * 8);
            }
            if (!regionTiles.contains(point)) {
                regionTiles.add(point);
                plugin.markedTileCount++;
                plugin.availableTiles--;
            }
            plugin.markedTiles.put(plugin.toHash(regionID), regionTiles);
        }
        eventBus.post(new ClipboardImported());
    }
}


@Value
class TileManModeTile
{
    int regionId;
    int regionX;
    int regionY;
    int z;
}