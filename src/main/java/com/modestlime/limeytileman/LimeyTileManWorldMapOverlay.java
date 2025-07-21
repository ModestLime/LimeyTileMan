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

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LimeyTileManWorldMapOverlay extends Overlay {

    final private Client client;
    final private LimeyTileManConfig config;
    final private LimeyTileManPlugin plugin;


    @Inject
    private LimeyTileManWorldMapOverlay(Client client, LimeyTileManConfig config, LimeyTileManPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_LOW);


    }


    @Override
    public Dimension render(Graphics2D graphics) {

        if(!config.drawWorldMap()){
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        Widget worldMapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);

        if (worldMap == null || worldMapWidget == null)
        {
            return null;
        }

        float mapTileSize = worldMap.getWorldMapZoom();
        Rectangle worldMapRectangle = worldMapWidget.getBounds();
        graphics.setClip(worldMapRectangle);


        Point mapCenterWorldpoint = worldMap.getWorldMapPosition();
        int heightInTiles = (int)Math.ceil(worldMapRectangle.height / mapTileSize);
        int widthInTiles = (int)Math.ceil(worldMapRectangle.width / mapTileSize);

        //bottom left tile on the world map
        Point bottomLeftWorldPoint = new Point(mapCenterWorldpoint.getX() - (widthInTiles / 2), mapCenterWorldpoint.getY() - (heightInTiles / 2));

        graphics.setColor(config.worldMapColor());

        List<WorldPoint> tilesToRender = new ArrayList<>();
        int plane = client.getTopLevelWorldView().getPlane();
        for(int y = 0; y < heightInTiles; y++){
            for (int x = 0; x < widthInTiles; x++) {
                int hash = plugin.toHash(plugin.toRegion(new WorldPoint(bottomLeftWorldPoint.getX() + (x << 3), bottomLeftWorldPoint.getY() + (y << 3), plane)));
                List<WorldPoint> regionChunk = plugin.markedTiles.get(hash);
                if(regionChunk != null) {
                    tilesToRender.addAll(regionChunk);
                }
            }
        }

        float tileRenderSize = mapTileSize;
        if(config.bigMapTiles()){tileRenderSize = Math.max(tileRenderSize, 4.0f);}



        for(WorldPoint wp : tilesToRender) {

            int diffX = wp.getX() - bottomLeftWorldPoint.getX();
            int diffY = wp.getY() - bottomLeftWorldPoint.getY();

            int screenCordX = (int) ((diffX * mapTileSize) + worldMapRectangle.getX());
            int screenCordY = (int) (worldMapRectangle.getY() + worldMapRectangle.height - (diffY * mapTileSize) - mapTileSize);
            graphics.fillRect(screenCordX, screenCordY, (int) tileRenderSize, (int) tileRenderSize);
        }

        return null;
    }
}
