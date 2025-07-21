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
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.AffineTransform;

@Slf4j
public class LimeyTileManMiniMapOverlay extends Overlay {

    final private Client client;
    final private LimeyTileManConfig config;
    final private LimeyTileManPlugin plugin;


    @Inject
    private LimeyTileManMiniMapOverlay(Client client, LimeyTileManConfig config, LimeyTileManPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_LOW);
    }
    
    
    @Override
    public Dimension render(Graphics2D graphics) {

        if(!config.drawMinimap()){
            return null;
        }

        double angle = client.getCameraYawTarget() * Perspective.UNIT;
        double tileSize = client.getMinimapZoom();
        double unCenterOffset = tileSize / 2;
        int size = (int) Math.round(tileSize);
        graphics.setColor(config.miniMapColor());
        AffineTransform originalTransform = graphics.getTransform();

        WorldView wv = client.getTopLevelWorldView();
        for (WorldPoint point : plugin.toRender) {
            final LocalPoint tilePosLocal = LocalPoint.fromWorld(wv, point);
            if (tilePosLocal != null) {


                Point minimapTile = Perspective.localToMinimap(client, tilePosLocal);

                if (minimapTile == null) {
                    continue;
                }

                int x = (int) Math.round(minimapTile.getX() - unCenterOffset);
                int y = (int) Math.round(minimapTile.getY() - unCenterOffset);
                graphics.rotate(angle, minimapTile.getX(), minimapTile.getY());
                graphics.fillRect(x, y, size, size);
                graphics.setTransform(originalTransform);
            }
        }
        for (WorldPoint point : plugin.oneClickTiles) {
            final LocalPoint tilePosLocal = LocalPoint.fromWorld(wv, point);
            if (tilePosLocal != null) {


                Point minimapTile = Perspective.localToMinimap(client, tilePosLocal);

                if (minimapTile == null) {
                    continue;
                }

                int x = (int) Math.round(minimapTile.getX() - unCenterOffset);
                int y = (int) Math.round(minimapTile.getY() - unCenterOffset);
                graphics.rotate(angle, minimapTile.getX(), minimapTile.getY());
                graphics.fillRect(x, y, size, size);
                graphics.setTransform(originalTransform);
            }
        }
        return null;
    }
}


