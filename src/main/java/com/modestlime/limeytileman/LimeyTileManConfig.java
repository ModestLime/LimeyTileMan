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

import net.runelite.client.config.*;

import java.awt.*;


@ConfigGroup(LimeyTileManConfig.GROUP)
public interface LimeyTileManConfig extends Config{
    String GROUP = "limeytileman";
    String RenderDistance = "renderDistance";
    String AutoMark = "autoMark";


    @ConfigSection(
            name = "Colors",
            description = "Color options for all the different settings",
            closedByDefault = true,
            position = 100
    )
    String colorsSection = "colorsSection";

    @ConfigSection(
            name = "Customize game mode settings",
            description = "Customize everything :)",
            closedByDefault = true,
            position = 200
    )
    String gameModeSection = "gameModeSection";


    @Range(
            max = 70
    )
    @ConfigItem(
            keyName = RenderDistance,
            name = "Tile Render Distance",
            description = "Distance to render in tiles around the player",
            position = 0
    )
    default int renderDistance()
    {
        return 32;
    }

    @ConfigItem(
            keyName = AutoMark,
            name = "Auto Mark tiles",
            description = "Automatically marks tiles as you move",
            position = 1
    )
    default boolean autoMark(){return true;}

    @ConfigItem(
            keyName = "allowDeficit",
            name = "Allow Tile Deficit",
            description = "Lets you go into debt when placing tiles",
            position = 1
    )
    default boolean allowDeficit(){return true;}

    @ConfigItem(
            keyName = "warnToggle",
            name = "Warning when low",
            description = "Change color of tiles when you are low",
            position = 2
    )
    default boolean warnToggle(){return true;}

    @ConfigItem(
            keyName = "warnThreshold",
            name = "Warning Threshold",
            description = "How many tiles left before it changes colors",
            position = 3
    )
    default int warnThreshold(){return 20;}

    @ConfigItem(
            keyName = "oneClickTiles",
            name = "One Clickable Tiles",
            description = "Change tile colors of tiles that can be reached in 1 click",
            position = 4
    )
    default boolean oneClickTiles(){return true;}

    @ConfigItem(
            keyName = "drawMinimap",
            name = "Draw on Minimap",
            description = "Draw's your marked tiles on the minimap",
            position = 5
    )
    default boolean drawMinimap(){return false;}

    @ConfigItem(
            keyName = "drawWorldMap",
            name = "Draw on WorldMap",
            description = "Draw's your marked tiles on the world map",
            position = 6
    )
    default boolean drawWorldMap(){return false;}

    @ConfigItem(
            keyName = "bigMapTiles",
            name = "Big Tiles on World Map",
            description = "Draw's the tiles on the world map to be big when zoomed out",
            position = 7
    )
    default boolean bigMapTiles(){return false;}



    //colors
    @Alpha
    @ConfigItem(
            keyName = "tileColor",
            name = "Tile Color",
            description = "Color of the marked tiles",
            section = colorsSection,
            position = 101
    )
    default Color tileColor() {return new Color(255,255,255, 172);}

    @Alpha
    @ConfigItem(
            keyName = "TileFillColor",
            name = "Tile Fill Color",
            description = "Color of The Fill",
            section = colorsSection,
            position = 102
    )
    default Color tileFillColor(){return new Color(0, 0, 0, 50);}

    @Alpha
    @ConfigItem(
            keyName = "gradualFade",
            name = "Gradual Fade Warning",
            description = "Gradually fades the colors between the tile color->warning color->zero tile color",
            section = colorsSection,
            position = 103
    )
    default boolean gradualFade(){return true;}

    @Alpha
    @ConfigItem(
            keyName = "warnColor",
            name = "Warning Color",
            description = "Color of Tiles when you get low",
            section = colorsSection,
            position = 104
    )
    default Color warnColor() {return new Color(255, 106, 0, 189);}

    @Alpha
    @ConfigItem(
            keyName = "warnColorZero",
            name = "Warning Zero Tiles",
            description = "Color of Tiles when you no long have tiles left",
            section = colorsSection,
            position = 105
    )
    default Color warnColorZero() {return new Color(255, 0, 0, 171);}

    @Alpha
    @ConfigItem(
            keyName = "oneClickTilesColor",
            name = "One Clickable Tiles Color",
            description = "Color of one clickable tiles",
            section = colorsSection,
            position = 106
    )
    default Color oneClickTilesColor(){return new Color(0, 163, 255, 206);}

    @Alpha
    @ConfigItem(
            keyName = "oneClickTilesColorFill",
            name = "One Clickable Tiles Fill Color",
            description = "Color of The Fill",
            section = colorsSection,
            position = 107
    )
    default Color oneClickTilesColorFill(){return new Color(0, 0, 0, 50);}

    @Alpha
    @ConfigItem(
            keyName = "miniMapColor",
            name = "MiniMap Tile Color",
            description = "Color of the tiles on the mini map",
            section = colorsSection,
            position = 108
    )
    default Color miniMapColor(){return new Color(0, 247, 255, 116);}

    @Alpha
    @ConfigItem(
            keyName = "worldMapColor",
            name = "WorldMap Tile Color",
            description = "Color of the tiles on the world map",
            section = colorsSection,
            position = 109
    )
    default Color worldMapColor(){return new Color(255, 0, 251, 116);}


    //game mode settings
    @ConfigItem(
            keyName = "addTiles",
            name = "Add Tiles",
            description = "Add extra tiles!",
            section = gameModeSection,
            position = 201
    )
    default int addTiles()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "subtractTiles",
            name = "Subtract Tiles",
            description = "subtract tiles!",
            section = gameModeSection,
            position = 202
    )
    default int subtractTiles()
    {
        return 0;
    }

    @ConfigItem(
            keyName = "addTilesOnXp",
            name = "Add Tiles With Xp",
            description = "Add Tiles when you gain xp",
            section = gameModeSection,
            position = 203
    )
    default boolean addTilesOnXp()
    {
        return true;
    }

    @Range(
            min = 1
    )
    @ConfigItem(
            keyName = "xpPerTile",
            name = "Xp Per Tile",
            description = "Amount of xp to gain a tile",
            section = gameModeSection,
            position = 204
    )
    default int xpPerTile()
    {
        return 500;
    }

    @ConfigItem(
            keyName = "addTileOnLevel",
            name = "Add Tiles with total level",
            description = "Adds tiles based on your total level",
            section = gameModeSection,
            position = 205
    )
    default boolean addTileOnLevel(){return false;}

    @ConfigItem(
            keyName = "tilesPerLevel",
            name = "Tiles added per level",
            description = "Amount of tiles to add per level up (Add total Levels must be enabled)",
            section = gameModeSection,
            position = 206
    )
    default int tilesPerLevel(){return 1;}




}