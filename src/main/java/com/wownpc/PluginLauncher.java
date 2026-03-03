package com.wownpc;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PluginLauncher
{
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception
    {
        Class<? extends net.runelite.client.plugins.Plugin>[] plugins =
            (Class<? extends net.runelite.client.plugins.Plugin>[]) new Class[]{ WoWStyleNametagsPlugin.class };
        ExternalPluginManager.loadBuiltin(plugins);
        RuneLite.main(args);
    }
}