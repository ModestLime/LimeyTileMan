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
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;


@Slf4j
public class LimeyTileManTileInfoOverlay extends OverlayPanel {

    final private LimeyTileManConfig config;
    final private LimeyTileManPlugin plugin;

    private final static String availableTilesString = "Available Tiles:";
    private final static String  xpToNextString = "XP Until Next Tile:";
    private final static String  unlockedTilesString = "Tiles Unlocked:";
    private static final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.ENGLISH);


    @Inject
    private LimeyTileManTileInfoOverlay(LimeyTileManConfig config, LimeyTileManPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(Overlay.PRIORITY_MED);


    }


    @Override
    public Dimension render(Graphics2D graphics){
        panelComponent.getChildren().clear();

        String availableTilesNum = numberFormatter.format(plugin.availableTiles);
        String xpToNextNum = numberFormatter.format(plugin.xpToNextTile);
        String unlockedTilesNum = numberFormatter.format(plugin.markedTileCount);

        FontMetrics fm = graphics.getFontMetrics();
        int padding = 10 * 2;
        int maxWidth = fm.stringWidth(availableTilesString + " " + availableTilesNum);
        maxWidth = Math.max(maxWidth, fm.stringWidth(xpToNextString + " " + xpToNextNum));
        maxWidth = Math.max(maxWidth, fm.stringWidth(unlockedTilesString + " " + unlockedTilesNum));
        if(plugin.easyDeleteMode){maxWidth = Math.max(maxWidth, fm.stringWidth("Easy Tile Deletion Active!"));}

        maxWidth += padding;
        panelComponent.setPreferredSize(new Dimension(maxWidth, 0));


        Color availableTilesColor = Color.white;
        if(plugin.availableTiles <= 0){
            availableTilesColor = Color.red;
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left(availableTilesString)
                .right(availableTilesNum)
                .leftColor(availableTilesColor) // Color for the left part of the line
                .rightColor(availableTilesColor) // Color for the right part of the line
                .build());

        if(config.addTilesOnXp()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(xpToNextString)
                    .right(xpToNextNum)
                    .leftColor(Color.WHITE) // Color for the left part of the line
                    .rightColor(Color.WHITE) // Color for the right part of the line
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left(unlockedTilesString)
                .right(unlockedTilesNum)
                .leftColor(Color.WHITE) // Color for the left part of the line
                .rightColor(Color.WHITE) // Color for the right part of the line
                .build());

        if(plugin.easyDeleteMode){
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Easy Tile Deletion Active!")
                    .color(Color.red)
                    .build());
        }

        return super.render(graphics);
    }
}
