package com.modestlime.limeytileman;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;


public class LimeyTileManPluginTest{

    public static void main(String[] args) throws Exception{

        ExternalPluginManager.loadBuiltin(LimeyTileManPlugin.class);
        RuneLite.main(args);
    }


}