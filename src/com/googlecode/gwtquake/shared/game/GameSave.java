/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *  
 */
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.game;

import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.ConsoleVariables;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.server.ServerGame;
import com.googlecode.gwtquake.shared.server.World;
import com.googlecode.gwtquake.shared.util.Lib;
import com.googlecode.gwtquake.shared.util.QuakeFile;


public class GameSave {

    public static void CreateEdicts() {
        GameBase.g_edicts = new Entity[GameBase.game.maxentities];
        for (int i = 0; i < GameBase.game.maxentities; i++)
            GameBase.g_edicts[i] = new Entity(i);
        GameBase.g_edicts = GameBase.g_edicts;
    }

    public static void CreateClients() {
        GameBase.game.clients = new GameClient[GameBase.game.maxclients];
        for (int i = 0; i < GameBase.game.maxclients; i++)
            GameBase.game.clients[i] = new GameClient(i);

    }

    
    /**
     * InitGame
     * 
     * This will be called when the dll is first loaded, which only happens when
     * a new game is started or a save game is loaded. 
     */
    public static void InitGame() {
        ServerGame.PF_dprintf("==== InitGame ====\n");

//        // preload all classes to register the adapters
//        for ( int n=0; n < preloadclasslist.length; n++)
//        {
//        	try
//			{
//        		Class.forName(preloadclasslist[n]);
//			}
//        	catch(Exception e)
//			{
//        		Com.DPrintf("error loading class: " + e.getMessage());
//			}
//        }
//        
        
        GameBase.gun_x = ConsoleVariables.Get("gun_x", "0", 0);
        GameBase.gun_y = ConsoleVariables.Get("gun_y", "0", 0);
        GameBase.gun_z = ConsoleVariables.Get("gun_z", "0", 0);

        //FIXME: sv_ prefix are wrong names for these variables 
        GameBase.sv_rollspeed = ConsoleVariables.Get("sv_rollspeed", "200", 0);
        GameBase.sv_rollangle = ConsoleVariables.Get("sv_rollangle", "2", 0);
        GameBase.sv_maxvelocity = ConsoleVariables.Get("sv_maxvelocity", "2000", 0);
        GameBase.sv_gravity = ConsoleVariables.Get("sv_gravity", "800", 0);

        // noset vars
        Globals.dedicated = ConsoleVariables.Get("dedicated", "0", Constants.CVAR_NOSET);

        // latched vars
        GameBase.sv_cheats = ConsoleVariables.Get("cheats", "0", Constants.CVAR_SERVERINFO | Constants.CVAR_LATCH);
        ConsoleVariables.Get("gamename", Constants.GAMEVERSION, Constants.CVAR_SERVERINFO | Constants.CVAR_LATCH);
        ConsoleVariables.Get("gamedate", Constants.__DATE__, Constants.CVAR_SERVERINFO
        | Constants.CVAR_LATCH);

        GameBase.maxclients = ConsoleVariables.Get("maxclients", "4", Constants.CVAR_SERVERINFO | Constants.CVAR_LATCH);
        GameBase.maxspectators = ConsoleVariables.Get("maxspectators", "4", Constants.CVAR_SERVERINFO);
        GameBase.deathmatch = ConsoleVariables.Get("deathmatch", "0", Constants.CVAR_LATCH);
        GameBase.coop = ConsoleVariables.Get("coop", "0", Constants.CVAR_LATCH);
        GameBase.skill = ConsoleVariables.Get("skill", "0", Constants.CVAR_LATCH);
        GameBase.maxentities = ConsoleVariables.Get("maxentities", "1024", Constants.CVAR_LATCH);

        // change anytime vars
        GameBase.dmflags = ConsoleVariables.Get("dmflags", "0", Constants.CVAR_SERVERINFO);
        GameBase.fraglimit = ConsoleVariables.Get("fraglimit", "0", Constants.CVAR_SERVERINFO);
        GameBase.timelimit = ConsoleVariables.Get("timelimit", "0", Constants.CVAR_SERVERINFO);
        GameBase.password = ConsoleVariables.Get("password", "", Constants.CVAR_USERINFO);
        GameBase.spectator_password = ConsoleVariables.Get("spectator_password", "", Constants.CVAR_USERINFO);
        GameBase.needpass = ConsoleVariables.Get("needpass", "0", Constants.CVAR_SERVERINFO);
        GameBase.filterban = ConsoleVariables.Get("filterban", "1", 0);

        GameBase.g_select_empty = ConsoleVariables.Get("g_select_empty", "0", Constants.CVAR_ARCHIVE);

        GameBase.run_pitch = ConsoleVariables.Get("run_pitch", "0.002", 0);
        GameBase.run_roll = ConsoleVariables.Get("run_roll", "0.005", 0);
        GameBase.bob_up = ConsoleVariables.Get("bob_up", "0.005", 0);
        GameBase.bob_pitch = ConsoleVariables.Get("bob_pitch", "0.002", 0);
        GameBase.bob_roll = ConsoleVariables.Get("bob_roll", "0.002", 0);

        // flood control
        GameBase.flood_msgs = ConsoleVariables.Get("flood_msgs", "4", 0);
        GameBase.flood_persecond = ConsoleVariables.Get("flood_persecond", "4", 0);
        GameBase.flood_waitdelay = ConsoleVariables.Get("flood_waitdelay", "10", 0);

        // dm map list
        GameBase.sv_maplist = ConsoleVariables.Get("sv_maplist", "", 0);

        // items
        GameItems.InitItems();

        GameBase.game.helpmessage1 = "";
        GameBase.game.helpmessage2 = "";

        // initialize all entities for this game
        GameBase.game.maxentities = (int) GameBase.maxentities.value;
        CreateEdicts();

        // initialize all clients for this game
        GameBase.game.maxclients = (int) GameBase.maxclients.value;

        CreateClients();

        GameBase.num_edicts = GameBase.game.maxclients + 1;
    }

    /**
     * WriteGame
     * 
     * This will be called whenever the game goes to a new level, and when the
     * user explicitly saves the game.
     * 
     * Game information include cross level data, like multi level triggers,
     * help computer info, and all client states.
     * 
     * A single player death will automatically restore from the last save
     * position.
     */
    public static void WriteGame(String filename, boolean autosave) {
        try {
            QuakeFile f;

            if (!autosave)
                PlayerClient.SaveClientData();

            f = new QuakeFile(filename, "rw");

            if (f == null)
              Com.Error(Constants.ERR_FATAL, "Couldn't write to " + filename);

            GameBase.game.autosaved = autosave;
            GameBase.game.write(f);
            GameBase.game.autosaved = false;

            for (int i = 0; i < GameBase.game.maxclients; i++)
                GameBase.game.clients[i].write(f);

            Lib.fclose(f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void ReadGame(String filename) {

        QuakeFile f = null;

        try {

            f = new QuakeFile(filename, "r");
            CreateEdicts();

            GameBase.game.load(f);

            for (int i = 0; i < GameBase.game.maxclients; i++) {
                GameBase.game.clients[i] = new GameClient(i);
                GameBase.game.clients[i].read(f);
            }

            f.close();
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * WriteLevel
     */
    public static void WriteLevel(String filename) {
        try {
            int i;
            Entity ent;
            QuakeFile f;

            f = new QuakeFile(filename, "rw");
            if (f == null)
              Com.Error(Constants.ERR_FATAL, "Couldn't open for writing: " + filename);

            // write out level_locals_t
            GameBase.level.write(f);

            // write out all the entities
            for (i = 0; i < GameBase.num_edicts; i++) {
                ent = GameBase.g_edicts[i];
                if (!ent.inuse)
                    continue;
                f.writeInt(i);
                ent.write(f);
            }

            i = -1;
            f.writeInt(-1);

            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ReadLevel
     * 
     * SpawnEntities will allready have been called on the level the same way it
     * was when the level was saved.
     * 
     * That is necessary to get the baselines set up identically.
     * 
     * The server will have cleared all of the world links before calling
     * ReadLevel.
     * 
     * No clients are connected yet.
     */
    public static void ReadLevel(String filename) {
        try {
            Entity ent;

            QuakeFile f = new QuakeFile(filename, "r");

            if (f == null)
              Com.Error(Constants.ERR_FATAL, "Couldn't read level file " + filename);

            // wipe all the entities
            CreateEdicts();

            GameBase.num_edicts = (int) GameBase.maxclients.value + 1;

            // load the level locals
            GameBase.level.read(f);

            // load all the entities
            while (true) {
                int entnum = f.readInt();
                if (entnum == -1)
                    break;

                if (entnum >= GameBase.num_edicts)
                    GameBase.num_edicts = entnum + 1;

                ent = GameBase.g_edicts[entnum];
                ent.read(f);
                ent.cleararealinks();
                World.SV_LinkEdict(ent);
            }

            Lib.fclose(f);

            // mark all clients as unconnected
            for (int i = 0; i < GameBase.maxclients.value; i++) {
                ent = GameBase.g_edicts[i + 1];
                ent.client = GameBase.game.clients[i];
                ent.client.pers.connected = false;
            }

            // do any load time things at this point
            for (int i = 0; i < GameBase.num_edicts; i++) {
                ent = GameBase.g_edicts[i];

                if (!ent.inuse)
                    continue;

                // fire any cross-level triggers
                if (ent.classname != null)
                    if (Lib.strcmp(ent.classname, "target_crosslevel_target") == 0)
                        ent.nextthink = GameBase.level.time + ent.delay;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
