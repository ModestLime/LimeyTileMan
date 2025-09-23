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
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.awt.geom.Path2D;


@Slf4j
public class LimeyTileManOverlay extends Overlay{

    final private Client client;
    final private LimeyTileManConfig config;
    final private LimeyTileManPlugin plugin;

    private final Path2D combinedToRender;
    private final Path2D combinedOneClickTiles;
    private final BasicStroke stroke =  new BasicStroke(2);

    @Inject
    private LimeyTileManOverlay(Client client, LimeyTileManConfig config, LimeyTileManPlugin plugin) {
        this.client = client;
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(-1.0f);

        combinedToRender = new Path2D.Float();
        combinedOneClickTiles = new Path2D.Float();


    }


    @Override
    public Dimension render(Graphics2D graphics){
        combinedToRender.reset();
        WorldView wv = client.getTopLevelWorldView();
        for (WorldPoint point : plugin.toRender) {
            final LocalPoint tilePosLocal = LocalPoint.fromWorld(wv, point);
            if (tilePosLocal != null) {
                final Polygon poly = Perspective.getCanvasTilePoly(client, tilePosLocal);
                if (poly != null) {
                    combinedToRender.append(poly, false);
                }
            }
        }

        combinedOneClickTiles.reset();
        for (WorldPoint point : plugin.oneClickTiles) {
            final LocalPoint tilePosLocal = LocalPoint.fromWorld(wv, point);
            if (tilePosLocal != null) {
                final Polygon poly = Perspective.getCanvasTilePoly(client, tilePosLocal);
                if (poly != null) {
                    combinedOneClickTiles.append(poly, false);
                }
            }
        }

        if(plugin.gpuEnabled) {
            //speeds up rendering and makes it look better by not mixing colors when drawing tiles
            //but when not rendering on the gpu our Graphics2D objects image buffer will contain our runescape scene instead of being blank,
            //so we don't want to overwrite pixels and always want to mix with the background
            graphics.setComposite(AlphaComposite.Src);
        }

        graphics.setColor(config.tileFillColor());
        graphics.fill(combinedToRender);
        graphics.setColor(config.oneClickTilesColorFill());
        graphics.fill(combinedOneClickTiles);

        graphics.setStroke(stroke);

        graphics.setColor(plugin.tileColor);
        graphics.draw(combinedToRender);

        graphics.setColor(config.oneClickTilesColor());
        graphics.draw(combinedOneClickTiles);




        return null;
    }
}
