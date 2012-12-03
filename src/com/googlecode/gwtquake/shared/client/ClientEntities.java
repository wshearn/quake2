/*
 Copyright (C) 1997-2001 Id Software, Inc.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 */
/* Modifications
   Copyright 2003-2004 Bytonic Software
   Copyright 2010 Google Inc.
*/
package com.googlecode.gwtquake.shared.client;

import com.googlecode.gwtquake.shared.common.AsyncCallback;
import com.googlecode.gwtquake.shared.common.Buffers;
import com.googlecode.gwtquake.shared.common.Com;
import com.googlecode.gwtquake.shared.common.Constants;
import com.googlecode.gwtquake.shared.common.Globals;
import com.googlecode.gwtquake.shared.game.EntityState;
import com.googlecode.gwtquake.shared.game.PlayerMove;
import com.googlecode.gwtquake.shared.game.PlayerState;
import com.googlecode.gwtquake.shared.render.Model;
import com.googlecode.gwtquake.shared.util.Math3D;


/**
 * CL_ents
 */
//	   cl_ents.c -- entity parsing and management
/*
 * =========================================================================
 * 
 * FRAME PARSING
 * 
 * =========================================================================
 */
public class ClientEntities {

	static int bitcounts[] = new int[32]; /// just for protocol profiling

	static int bfg_lightramp[] = { 300, 400, 600, 300, 150, 75 };

	/*
	 * ================= CL_ParseEntityBits
	 * 
	 * Returns the entity number and the header bits =================
	 */
	public static int ParseEntityBits(int bits[]) {
		int b, total;
		int i;
		int number;

		total = Buffers.readUnsignedByte(Globals.net_message);
		if ((total & Constants.U_MOREBITS1) != 0) {
		    
			b = Buffers.readUnsignedByte(Globals.net_message);
			total |= b << 8;
		}
		if ((total & Constants.U_MOREBITS2) != 0) {
		    
			b = Buffers.readUnsignedByte(Globals.net_message);
			total |= b << 16;
		}
		if ((total & Constants.U_MOREBITS3) != 0) {
		    
			b = Buffers.readUnsignedByte(Globals.net_message);
			total |= b << 24;
		}

		// count the bits for net profiling
		for (i = 0; i < 32; i++)
			if ((total & (1 << i)) != 0)
				bitcounts[i]++;

		if ((total & Constants.U_NUMBER16) != 0)
			number = Globals.net_message.getShort();
		else
			number = Buffers.readUnsignedByte(Globals.net_message);

		bits[0] = total;

		return number;
	}

	/*
	 * ================== CL_ParseDelta
	 * 
	 * Can go from either a baseline or a previous packet_entity
	 * ==================
	 */
	public static void ParseDelta(EntityState from, EntityState to, int number, int bits) {
		// set everything to the state we are delta'ing from
		to.set(from);

		Math3D.VectorCopy(from.origin, to.old_origin);
		to.number = number;

		if ((bits & Constants.U_MODEL) != 0)
			to.modelindex = Buffers.readUnsignedByte(Globals.net_message);
		if ((bits & Constants.U_MODEL2) != 0)
			to.modelindex2 = Buffers.readUnsignedByte(Globals.net_message);
		if ((bits & Constants.U_MODEL3) != 0)
			to.modelindex3 = Buffers.readUnsignedByte(Globals.net_message);
		if ((bits & Constants.U_MODEL4) != 0)
			to.modelindex4 = Buffers.readUnsignedByte(Globals.net_message);

		if ((bits & Constants.U_FRAME8) != 0)
			to.frame = Buffers.readUnsignedByte(Globals.net_message);
		if ((bits & Constants.U_FRAME16) != 0)
			to.frame = Globals.net_message.getShort();

		if ((bits & Constants.U_SKIN8) != 0 && (bits & Constants.U_SKIN16) != 0) //used
																			 // for
																			 // laser
																			 // colors
			to.skinnum = Globals.net_message.getInt();
		else if ((bits & Constants.U_SKIN8) != 0)
			to.skinnum = Buffers.readUnsignedByte(Globals.net_message);
		else if ((bits & Constants.U_SKIN16) != 0)
			to.skinnum = Globals.net_message.getShort();

		if ((bits & (Constants.U_EFFECTS8 | Constants.U_EFFECTS16)) == (Constants.U_EFFECTS8 | Constants.U_EFFECTS16))
			to.effects = Globals.net_message.getInt();
		else if ((bits & Constants.U_EFFECTS8) != 0)
			to.effects = Buffers.readUnsignedByte(Globals.net_message);
		else if ((bits & Constants.U_EFFECTS16) != 0)
			to.effects = Globals.net_message.getShort();

		if ((bits & (Constants.U_RENDERFX8 | Constants.U_RENDERFX16)) == (Constants.U_RENDERFX8 | Constants.U_RENDERFX16))
			to.renderfx = Globals.net_message.getInt();
		else if ((bits & Constants.U_RENDERFX8) != 0)
			to.renderfx = Buffers.readUnsignedByte(Globals.net_message);
		else if ((bits & Constants.U_RENDERFX16) != 0)
			to.renderfx = Globals.net_message.getShort();

		if ((bits & Constants.U_ORIGIN1) != 0)
			to.origin[0] = Buffers.getCoord(Globals.net_message);
		if ((bits & Constants.U_ORIGIN2) != 0)
			to.origin[1] = Buffers.getCoord(Globals.net_message);
		if ((bits & Constants.U_ORIGIN3) != 0)
			to.origin[2] = Buffers.getCoord(Globals.net_message);

		if ((bits & Constants.U_ANGLE1) != 0)
			to.angles[0] = Buffers.getAngle(Globals.net_message);
		if ((bits & Constants.U_ANGLE2) != 0)
			to.angles[1] = Buffers.getAngle(Globals.net_message);
		if ((bits & Constants.U_ANGLE3) != 0)
			to.angles[2] = Buffers.getAngle(Globals.net_message);

		if ((bits & Constants.U_OLDORIGIN) != 0)
			Buffers.getPos(Globals.net_message, to.old_origin);

		if ((bits & Constants.U_SOUND) != 0)
			to.sound = Buffers.readUnsignedByte(Globals.net_message);

		if ((bits & Constants.U_EVENT) != 0)
			to.event = Buffers.readUnsignedByte(Globals.net_message);
		else
			to.event = 0;

		if ((bits & Constants.U_SOLID) != 0)
			to.solid = Globals.net_message.getShort();
	}

	/*
	 * ================== CL_DeltaEntity
	 * 
	 * Parses deltas from the given base and adds the resulting entity to the
	 * current frame ==================
	 */
	public static void DeltaEntity(FrameData frame, int newnum, EntityState old, int bits) {
		ClientEntity ent;
		EntityState state;

		ent = Globals.cl_entities[newnum];

		state = Globals.cl_parse_entities[Globals.cl.parse_entities & (Constants.MAX_PARSE_ENTITIES - 1)];
		Globals.cl.parse_entities++;
		frame.num_entities++;

		ParseDelta(old, state, newnum, bits);

		// some data changes will force no lerping
		if (state.modelindex != ent.current.modelindex || state.modelindex2 != ent.current.modelindex2
				|| state.modelindex3 != ent.current.modelindex3 || state.modelindex4 != ent.current.modelindex4
				|| Math.abs(state.origin[0] - ent.current.origin[0]) > 512 || Math.abs(state.origin[1] - ent.current.origin[1]) > 512
				|| Math.abs(state.origin[2] - ent.current.origin[2]) > 512 || state.event == Constants.EV_PLAYER_TELEPORT
				|| state.event == Constants.EV_OTHER_TELEPORT) {
			ent.serverframe = -99;
		}

		if (ent.serverframe != Globals.cl.frame.serverframe - 1) { // wasn't in
																   // last
																   // update, so
																   // initialize
																   // some
																   // things
			ent.trailcount = 1024; // for diminishing rocket / grenade trails
			// duplicate the current state so lerping doesn't hurt anything
			ent.prev.set(state);
			if (state.event == Constants.EV_OTHER_TELEPORT) {
				Math3D.VectorCopy(state.origin, ent.prev.origin);
				Math3D.VectorCopy(state.origin, ent.lerp_origin);
			} else {
				Math3D.VectorCopy(state.old_origin, ent.prev.origin);
				Math3D.VectorCopy(state.old_origin, ent.lerp_origin);
			}
		} else { // shuffle the last state to previous
			// Copy !
			ent.prev.set(ent.current);
		}

		ent.serverframe = Globals.cl.frame.serverframe;
		// Copy !
		ent.current.set(state);
	}

	// call by reference
	private static final int[] iw = {0};
	/*
	 * ================== CL_ParsePacketEntities
	 * 
	 * An svc_packetentities has just been parsed, deal with the rest of the
	 * data stream. ==================
	 */
	public static void ParsePacketEntities(FrameData oldframe, FrameData newframe) {
		int newnum;
		int bits = 0;

		EntityState oldstate = null;
		int oldnum;

		newframe.parse_entities = Globals.cl.parse_entities;
		newframe.num_entities = 0;

		// delta from the entities present in oldframe
		int oldindex = 0;
		if (oldframe == null)
			oldnum = 99999;
		else {
			// oldindex == 0. hoz
			//			if (oldindex >= oldframe.num_entities)
			//				oldnum = 99999;
			//			else {
			oldstate = Globals.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Constants.MAX_PARSE_ENTITIES - 1)];
			oldnum = oldstate.number;
			//			}
		}

		while (true) {
			//int iw[] = { bits };
			iw[0] = bits;
			newnum = ParseEntityBits(iw);
			bits = iw[0];

			if (newnum >= Constants.MAX_EDICTS)
				Com.Error(Constants.ERR_DROP, "CL_ParsePacketEntities: bad number:" + newnum);

			if (Globals.net_message.readcount > Globals.net_message.cursize)
				Com.Error(Constants.ERR_DROP, "CL_ParsePacketEntities: end of message");

			if (0 == newnum)
				break;

			while (oldnum < newnum) { // one or more entities from the old
									  // packet are unchanged
				if (Globals.cl_shownet.value == 3)
					Com.Printf("   unchanged: " + oldnum + "\n");
				DeltaEntity(newframe, oldnum, oldstate, 0);

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Globals.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Constants.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
			}

			if ((bits & Constants.U_REMOVE) != 0) { // the entity present in
												  // oldframe is not in the
												  // current frame
				if (Globals.cl_shownet.value == 3)
					Com.Printf("   remove: " + newnum + "\n");
				if (oldnum != newnum)
					Com.Printf("U_REMOVE: oldnum != newnum\n");

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Globals.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Constants.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
				continue;
			}

			if (oldnum == newnum) { // delta from previous state
				if (Globals.cl_shownet.value == 3)
					Com.Printf("   delta: " + newnum + "\n");
				DeltaEntity(newframe, newnum, oldstate, bits);

				oldindex++;

				if (oldindex >= oldframe.num_entities)
					oldnum = 99999;
				else {
					oldstate = Globals.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Constants.MAX_PARSE_ENTITIES - 1)];
					oldnum = oldstate.number;
				}
				continue;
			}

			if (oldnum > newnum) { // delta from baseline
				if (Globals.cl_shownet.value == 3)
					Com.Printf("   baseline: " + newnum + "\n");
				DeltaEntity(newframe, newnum, Globals.cl_entities[newnum].baseline, bits);
				continue;
			}

		}

		// any remaining entities in the old frame are copied over
		while (oldnum != 99999) { // one or more entities from the old packet
								  // are unchanged
			if (Globals.cl_shownet.value == 3)
				Com.Printf("   unchanged: " + oldnum + "\n");
			DeltaEntity(newframe, oldnum, oldstate, 0);

			oldindex++;

			if (oldindex >= oldframe.num_entities)
				oldnum = 99999;
			else {
				oldstate = Globals.cl_parse_entities[(oldframe.parse_entities + oldindex) & (Constants.MAX_PARSE_ENTITIES - 1)];
				oldnum = oldstate.number;
			}
		}
	}

	/*
	 * =================== CL_ParsePlayerstate ===================
	 */
	public static void ParsePlayerstate(FrameData oldframe, FrameData newframe) {
		int flags;
		PlayerState state;
		int i;
		int statbits;

		state = newframe.playerstate;

		// clear to old value before delta parsing
		if (oldframe != null)
			state.set(oldframe.playerstate);
		else
			//memset (state, 0, sizeof(*state));
			state.clear();

		flags = Globals.net_message.getShort();

		//
		// parse the pmove_state_t
		//
		if ((flags & Constants.PS_M_TYPE) != 0)
			state.pmove.pm_type = Buffers.readUnsignedByte(Globals.net_message);

		if ((flags & Constants.PS_M_ORIGIN) != 0) {
			state.pmove.origin[0] = Globals.net_message.getShort();
			state.pmove.origin[1] = Globals.net_message.getShort();
			state.pmove.origin[2] = Globals.net_message.getShort();
		}

		if ((flags & Constants.PS_M_VELOCITY) != 0) {
			state.pmove.velocity[0] = Globals.net_message.getShort();
			state.pmove.velocity[1] = Globals.net_message.getShort();
			state.pmove.velocity[2] = Globals.net_message.getShort();
		}

		if ((flags & Constants.PS_M_TIME) != 0) {
			state.pmove.pm_time = (byte) Buffers.readUnsignedByte(Globals.net_message);
		}

		if ((flags & Constants.PS_M_FLAGS) != 0)
			state.pmove.pm_flags = (byte) Buffers.readUnsignedByte(Globals.net_message);

		if ((flags & Constants.PS_M_GRAVITY) != 0)
			state.pmove.gravity = Globals.net_message.getShort();

		if ((flags & Constants.PS_M_DELTA_ANGLES) != 0) {
			state.pmove.delta_angles[0] = Globals.net_message.getShort();
			state.pmove.delta_angles[1] = Globals.net_message.getShort();
			state.pmove.delta_angles[2] = Globals.net_message.getShort();
		}

		if (Globals.cl.attractloop)
			state.pmove.pm_type = Constants.PM_FREEZE; // demo playback

		//
		// parse the rest of the player_state_t
		//
		if ((flags & Constants.PS_VIEWOFFSET) != 0) {
			state.viewoffset[0] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.viewoffset[1] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.viewoffset[2] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
		}

		if ((flags & Constants.PS_VIEWANGLES) != 0) {
			state.viewangles[0] = Buffers.ReadAngle16(Globals.net_message);
			state.viewangles[1] = Buffers.ReadAngle16(Globals.net_message);
			state.viewangles[2] = Buffers.ReadAngle16(Globals.net_message);
		}

		if ((flags & Constants.PS_KICKANGLES) != 0) {

			state.kick_angles[0] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.kick_angles[1] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.kick_angles[2] = Buffers.readSignedByte(Globals.net_message) * 0.25f;

		}

		if ((flags & Constants.PS_WEAPONINDEX) != 0) {
			state.gunindex = Buffers.readUnsignedByte(Globals.net_message);
		}

		if ((flags & Constants.PS_WEAPONFRAME) != 0) {
			state.gunframe = Buffers.readUnsignedByte(Globals.net_message);
			state.gunoffset[0] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.gunoffset[1] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.gunoffset[2] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.gunangles[0] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.gunangles[1] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
			state.gunangles[2] = Buffers.readSignedByte(Globals.net_message) * 0.25f;
		}

		if ((flags & Constants.PS_BLEND) != 0) {
			state.blend[0] = Buffers.readUnsignedByte(Globals.net_message) / 255.0f;
			state.blend[1] = Buffers.readUnsignedByte(Globals.net_message) / 255.0f;
			state.blend[2] = Buffers.readUnsignedByte(Globals.net_message) / 255.0f;
			state.blend[3] = Buffers.readUnsignedByte(Globals.net_message) / 255.0f;
		}

		if ((flags & Constants.PS_FOV) != 0)
			state.fov = Buffers.readUnsignedByte(Globals.net_message);

		if ((flags & Constants.PS_RDFLAGS) != 0)
			state.rdflags = Buffers.readUnsignedByte(Globals.net_message);

		// parse stats
		statbits = Globals.net_message.getInt();
		for (i = 0; i < Constants.MAX_STATS; i++)
			if ((statbits & (1 << i)) != 0)
				state.stats[i] = Globals.net_message.getShort();
	}

	/*
	 * ================== CL_FireEntityEvents
	 * 
	 * ==================
	 */
	public static void FireEntityEvents(FrameData frame) {
		EntityState s1;
		int pnum, num;

		for (pnum = 0; pnum < frame.num_entities; pnum++) {
			num = (frame.parse_entities + pnum) & (Constants.MAX_PARSE_ENTITIES - 1);
			s1 = Globals.cl_parse_entities[num];
			if (s1.event != 0)
				ClientEffects.EntityEvent(s1);

			// EF_TELEPORTER acts like an event, but is not cleared each frame
			if ((s1.effects & Constants.EF_TELEPORTER) != 0)
				ClientEffects.TeleporterParticles(s1);
		}
	}

	/*
	 * ================ CL_ParseFrame ================
	 */
	public static void ParseFrame() {
		int cmd;
		int len;
		FrameData old;

		//memset( cl.frame, 0, sizeof(cl.frame));
		Globals.cl.frame.reset();

		Globals.cl.frame.serverframe = Globals.net_message.getInt();
		Globals.cl.frame.deltaframe = Globals.net_message.getInt();
		Globals.cl.frame.servertime = Globals.cl.frame.serverframe * 100;

		// BIG HACK to let old demos continue to work
		if (Globals.cls.serverProtocol != 26)
			Globals.cl.surpressCount = Buffers.readUnsignedByte(Globals.net_message);

		if (Globals.cl_shownet.value == 3)
			Com.Printf("   frame:" + Globals.cl.frame.serverframe + "  delta:" + Globals.cl.frame.deltaframe + "\n");

		// If the frame is delta compressed from data that we
		// no longer have available, we must suck up the rest of
		// the frame, but not use it, then ask for a non-compressed
		// message
		if (Globals.cl.frame.deltaframe <= 0) {
			Globals.cl.frame.valid = true; // uncompressed frame
			old = null;
			Globals.cls.demowaiting = false; // we can start recording now
		} else {
			old = Globals.cl.frames[Globals.cl.frame.deltaframe & Constants.UPDATE_MASK];
			if (!old.valid) { // should never happen
				Com.Printf("Delta from invalid frame (not supposed to happen!).\n");
			}
			if (old.serverframe != Globals.cl.frame.deltaframe) { // The frame
																  // that the
																  // server did
																  // the delta
																  // from
				// is too old, so we can't reconstruct it properly.
				Com.Printf("Delta frame too old.\n");
			} else if (Globals.cl.parse_entities - old.parse_entities > Constants.MAX_PARSE_ENTITIES - 128) {
				Com.Printf("Delta parse_entities too old.\n");
			} else
				Globals.cl.frame.valid = true; // valid delta parse
		}

		// clamp time
		if (Globals.cl.time > Globals.cl.frame.servertime)
			Globals.cl.time = Globals.cl.frame.servertime;
		else if (Globals.cl.time < Globals.cl.frame.servertime - 100)
			Globals.cl.time = Globals.cl.frame.servertime - 100;

		// read areabits
		len = Buffers.readUnsignedByte(Globals.net_message);
		Buffers.ReadData(Globals.net_message, Globals.cl.frame.areabits, len);

		// read playerinfo
		cmd = Buffers.readUnsignedByte(Globals.net_message);
		ClientParser.SHOWNET(ClientParser.svc_strings[cmd]);
		if (cmd != Constants.svc_playerinfo)
			Com.Error(Constants.ERR_DROP, "CL_ParseFrame: not playerinfo");
		ParsePlayerstate(old, Globals.cl.frame);

		// read packet entities
		cmd = Buffers.readUnsignedByte(Globals.net_message);
		ClientParser.SHOWNET(ClientParser.svc_strings[cmd]);
		if (cmd != Constants.svc_packetentities)
			Com.Error(Constants.ERR_DROP, "CL_ParseFrame: not packetentities");

		ParsePacketEntities(old, Globals.cl.frame);

		// save the frame off in the backup array for later delta comparisons
		Globals.cl.frames[Globals.cl.frame.serverframe & Constants.UPDATE_MASK].set(Globals.cl.frame);

		if (Globals.cl.frame.valid) {
			// getting a valid frame message ends the connection process
			if (Globals.cls.state != Constants.ca_active) {
				Globals.cls.state = Constants.ca_active;
				Globals.cl.force_refdef = true;

				Globals.cl.predicted_origin[0] = Globals.cl.frame.playerstate.pmove.origin[0] * 0.125f;
				Globals.cl.predicted_origin[1] = Globals.cl.frame.playerstate.pmove.origin[1] * 0.125f;
				Globals.cl.predicted_origin[2] = Globals.cl.frame.playerstate.pmove.origin[2] * 0.125f;

				Math3D.VectorCopy(Globals.cl.frame.playerstate.viewangles, Globals.cl.predicted_angles);
				if (Globals.cls.disable_servercount != Globals.cl.servercount && Globals.cl.refresh_prepped) {
					Screen.EndLoadingPlaque(); // get rid of loading plaque
				} else {
				   System.out.println("" + Globals.cls.disable_servercount + "==" +  Globals.cl.servercount + " or " +
						   Globals.cl.refresh_prepped);	
				}
			}
			Globals.cl.sound_prepped = true; // can start mixing ambient sounds

			// fire entity events
			FireEntityEvents(Globals.cl.frame);
			ClientPrediction.CheckPredictionError();
		} else {
			System.out.println("invalid frame");
		}
	}

	/*
	 * ==========================================================================
	 * 
	 * INTERPOLATE BETWEEN FRAMES TO GET RENDERING PARMS
	 * 
	 * ==========================================================================
	 */

	// stack variable
	private static final EntityType ent = new EntityType();
	/*
	 * =============== 
	 * CL_AddPacketEntities
	 * ===============
	 */
	static void AddPacketEntities(FrameData frame) {
		EntityState s1;
		float autorotate;
		int i;
		int pnum;
		ClientEntity cent;
		int autoanim;
		ClientInfo ci;
		int effects, renderfx;

		// bonus items rotate at a fixed rate
		autorotate = Math3D.anglemod(Globals.cl.time / 10);

		// brush models can auto animate their frames
		autoanim = 2 * Globals.cl.time / 1000;

		//memset( ent, 0, sizeof(ent));
		ent.clear();

		for (pnum = 0; pnum < frame.num_entities; pnum++) {
			s1 = Globals.cl_parse_entities[(frame.parse_entities + pnum) & (Constants.MAX_PARSE_ENTITIES - 1)];

			cent = Globals.cl_entities[s1.number];

			effects = s1.effects;
			renderfx = s1.renderfx;

			// set frame
			if ((effects & Constants.EF_ANIM01) != 0)
				ent.frame = autoanim & 1;
			else if ((effects & Constants.EF_ANIM23) != 0)
				ent.frame = 2 + (autoanim & 1);
			else if ((effects & Constants.EF_ANIM_ALL) != 0)
				ent.frame = autoanim;
			else if ((effects & Constants.EF_ANIM_ALLFAST) != 0)
				ent.frame = Globals.cl.time / 100;
			else
				ent.frame = s1.frame;

			// quad and pent can do different things on client
			if ((effects & Constants.EF_PENT) != 0) {
				effects &= ~Constants.EF_PENT;
				effects |= Constants.EF_COLOR_SHELL;
				renderfx |= Constants.RF_SHELL_RED;
			}

			if ((effects & Constants.EF_QUAD) != 0) {
				effects &= ~Constants.EF_QUAD;
				effects |= Constants.EF_COLOR_SHELL;
				renderfx |= Constants.RF_SHELL_BLUE;
			}
			//	  ======
			//	   PMM
			if ((effects & Constants.EF_DOUBLE) != 0) {
				effects &= ~Constants.EF_DOUBLE;
				effects |= Constants.EF_COLOR_SHELL;
				renderfx |= Constants.RF_SHELL_DOUBLE;
			}

			if ((effects & Constants.EF_HALF_DAMAGE) != 0) {
				effects &= ~Constants.EF_HALF_DAMAGE;
				effects |= Constants.EF_COLOR_SHELL;
				renderfx |= Constants.RF_SHELL_HALF_DAM;
			}
			//	   pmm
			//	  ======
			ent.oldframe = cent.prev.frame;
			ent.backlerp = 1.0f - Globals.cl.lerpfrac;

			if ((renderfx & (Constants.RF_FRAMELERP | Constants.RF_BEAM)) != 0) {
				// step origin discretely, because the frames
				// do the animation properly
				Math3D.VectorCopy(cent.current.origin, ent.origin);
				Math3D.VectorCopy(cent.current.old_origin, ent.oldorigin);
			} else { // interpolate origin
				for (i = 0; i < 3; i++) {
					ent.origin[i] = ent.oldorigin[i] = cent.prev.origin[i] + Globals.cl.lerpfrac
							* (cent.current.origin[i] - cent.prev.origin[i]);
				}
			}

			// create a new entity

			// tweak the color of beams
			if ((renderfx & Constants.RF_BEAM) != 0) { // the four beam colors are
													 // encoded in 32 bits of
													 // skinnum (hack)
				ent.alpha = 0.30f;
				ent.skinnum = (s1.skinnum >> ((Globals.rnd.nextInt(4)) * 8)) & 0xff;
				Math.random();
				ent.model = null;
			} else {
				// set skin
				if (s1.modelindex == 255) { // use custom player skin
					ent.skinnum = 0;
					ci = Globals.cl.clientinfo[s1.skinnum & 0xff];
					ent.skin = ci.skin;
					ent.model = ci.model;
					if (null == ent.skin || null == ent.model) {
						ent.skin = Globals.cl.baseclientinfo.skin;
						ent.model = Globals.cl.baseclientinfo.model;
					}

					//	  ============
					//	  PGM
					if ((renderfx & Constants.RF_USE_DISGUISE) != 0) {
					  String modelToLoad = null;
						if (ent.skin.name.startsWith("players/male")) {
							ent.skin = Globals.re.RegisterSkin("players/male/disguise.pcx");
							modelToLoad = "players/male/tris.md2";
						} else if (ent.skin.name.startsWith("players/female")) {
							ent.skin = Globals.re.RegisterSkin("players/female/disguise.pcx");
							modelToLoad = "players/female/tris.md2";
						} else if (ent.skin.name.startsWith("players/cyborg")) {
							ent.skin = Globals.re.RegisterSkin("players/cyborg/disguise.pcx");
							modelToLoad = "players/cyborg/tris.md2";
						}

						if (modelToLoad != null) {
  						Globals.re.RegisterModel(modelToLoad, new AsyncCallback<Model>() {
                public void onSuccess(Model response) {
                  ent.model = response;
                }

                public void onFailure(Throwable e) {
                  // TODO(jgw): fail?
                }
              });
						}
					}
					//	  PGM
					//	  ============
				} else {
					ent.skinnum = s1.skinnum;
					ent.skin = null;
					ent.model = Globals.cl.model_draw[s1.modelindex];
				}
			}

			// only used for black hole model right now, FIXME: do better
			if (renderfx == Constants.RF_TRANSLUCENT)
				ent.alpha = 0.70f;

			// render effects (fullbright, translucent, etc)
			if ((effects & Constants.EF_COLOR_SHELL) != 0)
				ent.flags = 0; // renderfx go on color shell entity
			else
				ent.flags = renderfx;

			// calculate angles
			if ((effects & Constants.EF_ROTATE) != 0) { // some bonus items
													  // auto-rotate
				ent.angles[0] = 0;
				ent.angles[1] = autorotate;
				ent.angles[2] = 0;
			}
			// RAFAEL
			else if ((effects & Constants.EF_SPINNINGLIGHTS) != 0) {
				ent.angles[0] = 0;
				ent.angles[1] = Math3D.anglemod(Globals.cl.time / 2) + s1.angles[1];
				ent.angles[2] = 180;
				{
					float[] forward = { 0, 0, 0 };
					float[] start = { 0, 0, 0 };

					Math3D.AngleVectors(ent.angles, forward, null, null);
					Math3D.VectorMA(ent.origin, 64, forward, start);
					Video.AddLight(start, 100, 1, 0, 0);
				}
			} else { // interpolate angles
				float a1, a2;

				for (i = 0; i < 3; i++) {
					a1 = cent.current.angles[i];
					a2 = cent.prev.angles[i];
					ent.angles[i] = Math3D.LerpAngle(a2, a1, Globals.cl.lerpfrac);
				}
			}

			if (s1.number == Globals.cl.playernum + 1) {
				ent.flags |= Constants.RF_VIEWERMODEL; // only draw from mirrors
				// FIXME: still pass to refresh

				if ((effects & Constants.EF_FLAG1) != 0)
					Video.AddLight(ent.origin, 225, 1.0f, 0.1f, 0.1f);
				else if ((effects & Constants.EF_FLAG2) != 0)
					Video.AddLight(ent.origin, 225, 0.1f, 0.1f, 1.0f);
				else if ((effects & Constants.EF_TAGTRAIL) != 0) //PGM
					Video.AddLight(ent.origin, 225, 1.0f, 1.0f, 0.0f); //PGM
				else if ((effects & Constants.EF_TRACKERTRAIL) != 0) //PGM
					Video.AddLight(ent.origin, 225, -1.0f, -1.0f, -1.0f); //PGM

				continue;
			}

			// if set to invisible, skip
			if (s1.modelindex == 0)
				continue;

			if ((effects & Constants.EF_BFG) != 0) {
				ent.flags |= Constants.RF_TRANSLUCENT;
				ent.alpha = 0.30f;
			}

			// RAFAEL
			if ((effects & Constants.EF_PLASMA) != 0) {
				ent.flags |= Constants.RF_TRANSLUCENT;
				ent.alpha = 0.6f;
			}

			if ((effects & Constants.EF_SPHERETRANS) != 0) {
				ent.flags |= Constants.RF_TRANSLUCENT;
				// PMM - *sigh* yet more EF overloading
				if ((effects & Constants.EF_TRACKERTRAIL) != 0)
					ent.alpha = 0.6f;
				else
					ent.alpha = 0.3f;
			}
			//	  pmm

			// add to refresh list
			Video.AddEntity(ent);

			// color shells generate a seperate entity for the main model
//			if ((effects & Defines.EF_COLOR_SHELL) != 0) {
//				/*
//				 * PMM - at this point, all of the shells have been handled if
//				 * we're in the rogue pack, set up the custom mixing, otherwise
//				 * just keep going if(Developer_searchpath(2) == 2) { all of the
//				 * solo colors are fine. we need to catch any of the
//				 * combinations that look bad (double & half) and turn them into
//				 * the appropriate color, and make double/quad something special
//				 *  
//				 */
//				if ((renderfx & Defines.RF_SHELL_HALF_DAM) != 0) {
//					if (FS.Developer_searchpath(2) == 2) {
//						// ditch the half damage shell if any of red, blue, or
//						// double are on
//						if ((renderfx & (Defines.RF_SHELL_RED | Defines.RF_SHELL_BLUE | Defines.RF_SHELL_DOUBLE)) != 0)
//							renderfx &= ~Defines.RF_SHELL_HALF_DAM;
//					}
//				}
//
//				if ((renderfx & Defines.RF_SHELL_DOUBLE) != 0) {
//					if (FS.Developer_searchpath(2) == 2) {
//						// lose the yellow shell if we have a red, blue, or
//						// green shell
//						if ((renderfx & (Defines.RF_SHELL_RED | Defines.RF_SHELL_BLUE | Defines.RF_SHELL_GREEN)) != 0)
//							renderfx &= ~Defines.RF_SHELL_DOUBLE;
//						// if we have a red shell, turn it to purple by adding
//						// blue
//						if ((renderfx & Defines.RF_SHELL_RED) != 0)
//							renderfx |= Defines.RF_SHELL_BLUE;
//						// if we have a blue shell (and not a red shell), turn
//						// it to cyan by adding green
//						else if ((renderfx & Defines.RF_SHELL_BLUE) != 0)
//							// go to green if it's on already, otherwise do cyan
//							// (flash green)
//							if ((renderfx & Defines.RF_SHELL_GREEN) != 0)
//								renderfx &= ~Defines.RF_SHELL_BLUE;
//							else
//								renderfx |= Defines.RF_SHELL_GREEN;
//					}
//				}
//				//				}
//				// pmm
//				ent.flags = renderfx | Defines.RF_TRANSLUCENT;
//				ent.alpha = 0.30f;
//				V.AddEntity(ent);
//			}

			ent.skin = null; // never use a custom skin on others
			ent.skinnum = 0;
			ent.flags = 0;
			ent.alpha = 0;

			// duplicate for linked models
			if (s1.modelindex2 != 0) {
				if (s1.modelindex2 == 255) { // custom weapon
					ci = Globals.cl.clientinfo[s1.skinnum & 0xff];
					i = (s1.skinnum >> 8); // 0 is default weapon model
					if (0 == Globals.cl_vwep.value || i > Constants.MAX_CLIENTWEAPONMODELS - 1)
						i = 0;
					ent.model = ci.weaponmodel[i];
					if (null == ent.model) {
						if (i != 0)
							ent.model = ci.weaponmodel[0];
						if (null == ent.model)
							ent.model = Globals.cl.baseclientinfo.weaponmodel[0];
					}
				} else
					ent.model = Globals.cl.model_draw[s1.modelindex2];

				// PMM - check for the defender sphere shell .. make it
				// translucent
				// replaces the previous version which used the high bit on
				// modelindex2 to determine transparency
				if (Globals.cl.configstrings[Constants.CS_MODELS + (s1.modelindex2)].equalsIgnoreCase("models/items/shell/tris.md2")) {
					ent.alpha = 0.32f;
					ent.flags = Constants.RF_TRANSLUCENT;
				}
				// pmm

				Video.AddEntity(ent);

				//PGM - make sure these get reset.
				ent.flags = 0;
				ent.alpha = 0;
				//PGM
			}
			if (s1.modelindex3 != 0) {
				ent.model = Globals.cl.model_draw[s1.modelindex3];
				Video.AddEntity(ent);
			}
			if (s1.modelindex4 != 0) {
				ent.model = Globals.cl.model_draw[s1.modelindex4];
				Video.AddEntity(ent);
			}

			if ((effects & Constants.EF_POWERSCREEN) != 0) {
				ent.model = ClientTent.cl_mod_powerscreen;
				ent.oldframe = 0;
				ent.frame = 0;
				ent.flags |= (Constants.RF_TRANSLUCENT | Constants.RF_SHELL_GREEN);
				ent.alpha = 0.30f;
				Video.AddEntity(ent);
			}

			// add automatic particle trails
			if ((effects & ~Constants.EF_ROTATE) != 0) {
				if ((effects & Constants.EF_ROCKET) != 0) {
					ClientEffects.RocketTrail(cent.lerp_origin, ent.origin, cent);
					Video.AddLight(ent.origin, 200, 1, 1, 0);
				}
				// PGM - Do not reorder EF_BLASTER and EF_HYPERBLASTER.
				// EF_BLASTER | EF_TRACKER is a special case for EF_BLASTER2...
				// Cheese!
				else if ((effects & Constants.EF_BLASTER) != 0) {
					//					CL_BlasterTrail (cent.lerp_origin, ent.origin);
					//	  PGM
					if ((effects & Constants.EF_TRACKER) != 0) // lame...
															 // problematic?
					{
						ClientNewFx.BlasterTrail2(cent.lerp_origin, ent.origin);
						Video.AddLight(ent.origin, 200, 0, 1, 0);
					} else {
						ClientEffects.BlasterTrail(cent.lerp_origin, ent.origin);
						Video.AddLight(ent.origin, 200, 1, 1, 0);
					}
					//	  PGM
				} else if ((effects & Constants.EF_HYPERBLASTER) != 0) {
					if ((effects & Constants.EF_TRACKER) != 0) // PGM overloaded
															 // for blaster2.
						Video.AddLight(ent.origin, 200, 0, 1, 0); // PGM
					else
						// PGM
						Video.AddLight(ent.origin, 200, 1, 1, 0);
				} else if ((effects & Constants.EF_GIB) != 0) {
					ClientEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				} else if ((effects & Constants.EF_GRENADE) != 0) {
					ClientEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				} else if ((effects & Constants.EF_FLIES) != 0) {
					ClientEffects.FlyEffect(cent, ent.origin);
				} else if ((effects & Constants.EF_BFG) != 0) {

					if ((effects & Constants.EF_ANIM_ALLFAST) != 0) {
						ClientEffects.BfgParticles(ent);
						i = 200;
					} else {
						i = bfg_lightramp[s1.frame];
					}
					Video.AddLight(ent.origin, i, 0, 1, 0);
				}
				// RAFAEL
				else if ((effects & Constants.EF_TRAP) != 0) {
					ent.origin[2] += 32;
					ClientEffects.TrapParticles(ent);
					i = (Globals.rnd.nextInt(100)) + 100;
					Video.AddLight(ent.origin, i, 1, 0.8f, 0.1f);
				} else if ((effects & Constants.EF_FLAG1) != 0) {
					ClientEffects.FlagTrail(cent.lerp_origin, ent.origin, 242);
					Video.AddLight(ent.origin, 225, 1, 0.1f, 0.1f);
				} else if ((effects & Constants.EF_FLAG2) != 0) {
					ClientEffects.FlagTrail(cent.lerp_origin, ent.origin, 115);
					Video.AddLight(ent.origin, 225, 0.1f, 0.1f, 1);
				}
				//	  ======
				//	  ROGUE
				else if ((effects & Constants.EF_TAGTRAIL) != 0) {
					ClientNewFx.TagTrail(cent.lerp_origin, ent.origin, 220);
					Video.AddLight(ent.origin, 225, 1.0f, 1.0f, 0.0f);
				} else if ((effects & Constants.EF_TRACKERTRAIL) != 0) {
					if ((effects & Constants.EF_TRACKER) != 0) {
						float intensity;

						intensity = (float) (50 + (500 * (Math.sin(Globals.cl.time / 500.0) + 1.0)));
						// FIXME - check out this effect in rendition
						if (Globals.vidref_val == Constants.VIDREF_GL)
							Video.AddLight(ent.origin, intensity, -1.0f, -1.0f, -1.0f);
						else
							Video.AddLight(ent.origin, -1.0f * intensity, 1.0f, 1.0f, 1.0f);
					} else {
						ClientNewFx.Tracker_Shell(cent.lerp_origin);
						Video.AddLight(ent.origin, 155, -1.0f, -1.0f, -1.0f);
					}
				} else if ((effects & Constants.EF_TRACKER) != 0) {
					ClientNewFx.TrackerTrail(cent.lerp_origin, ent.origin, 0);
					// FIXME - check out this effect in rendition
					if (Globals.vidref_val == Constants.VIDREF_GL)
						Video.AddLight(ent.origin, 200, -1, -1, -1);
					else
						Video.AddLight(ent.origin, -200, 1, 1, 1);
				}
				//	  ROGUE
				//	  ======
				// RAFAEL
				else if ((effects & Constants.EF_GREENGIB) != 0) {
					ClientEffects.DiminishingTrail(cent.lerp_origin, ent.origin, cent, effects);
				}
				// RAFAEL
				else if ((effects & Constants.EF_IONRIPPER) != 0) {
					ClientEffects.IonripperTrail(cent.lerp_origin, ent.origin);
					Video.AddLight(ent.origin, 100, 1, 0.5f, 0.5f);
				}
				// RAFAEL
				else if ((effects & Constants.EF_BLUEHYPERBLASTER) != 0) {
					Video.AddLight(ent.origin, 200, 0, 0, 1);
				}
				// RAFAEL
				else if ((effects & Constants.EF_PLASMA) != 0) {
					if ((effects & Constants.EF_ANIM_ALLFAST) != 0) {
						ClientEffects.BlasterTrail(cent.lerp_origin, ent.origin);
					}
					Video.AddLight(ent.origin, 130, 1, 0.5f, 0.5f);
				}
			}

			Math3D.VectorCopy(ent.origin, cent.lerp_origin);
		}
	}
	
	// stack variable
	private static final EntityType gun = new EntityType();
	/*
	 * ============== CL_AddViewWeapon ==============
	 */
	static void AddViewWeapon(PlayerState ps, PlayerState ops) {
		int i;

		// allow the gun to be completely removed
		if (0 == Globals.cl_gun.value)
			return;

		// don't draw gun if in wide angle view
		if (ps.fov > 90)
			return;

		//memset( gun, 0, sizeof(gun));
		gun.clear();

		if (Globals.gun_model != null)
			gun.model = Globals.gun_model; // development tool
		else
			gun.model = Globals.cl.model_draw[ps.gunindex];

		if (gun.model == null)
			return;

		// set up gun position
		for (i = 0; i < 3; i++) {
			gun.origin[i] = Globals.cl.refdef.vieworg[i] + ops.gunoffset[i] + Globals.cl.lerpfrac
					* (ps.gunoffset[i] - ops.gunoffset[i]);
			gun.angles[i] = Globals.cl.refdef.viewangles[i] + Math3D.LerpAngle(ops.gunangles[i], ps.gunangles[i], Globals.cl.lerpfrac);
		}

		if (Globals.gun_frame != 0) {
			gun.frame = Globals.gun_frame; // development tool
			gun.oldframe = Globals.gun_frame; // development tool
		} else {
			gun.frame = ps.gunframe;
			if (gun.frame == 0)
				gun.oldframe = 0; // just changed weapons, don't lerp from old
			else
				gun.oldframe = ops.gunframe;
		}

		gun.flags = Constants.RF_MINLIGHT | Constants.RF_DEPTHHACK | Constants.RF_WEAPONMODEL;
		gun.backlerp = 1.0f - Globals.cl.lerpfrac;
		Math3D.VectorCopy(gun.origin, gun.oldorigin); // don't lerp at all
		Video.AddEntity(gun);
	}

	/*
	 * =============== CL_CalcViewValues
	 * 
	 * Sets cl.refdef view values ===============
	 */
	static void CalcViewValues() {
		int i;
		float lerp, backlerp;
		FrameData oldframe;
		PlayerState ps, ops;

		// find the previous frame to interpolate from
		ps = Globals.cl.frame.playerstate;

		i = (Globals.cl.frame.serverframe - 1) & Constants.UPDATE_MASK;
		oldframe = Globals.cl.frames[i];

		if (oldframe.serverframe != Globals.cl.frame.serverframe - 1 || !oldframe.valid)
			oldframe = Globals.cl.frame; // previous frame was dropped or
										 // involid
		ops = oldframe.playerstate;

		// see if the player entity was teleported this frame
		if (Math.abs(ops.pmove.origin[0] - ps.pmove.origin[0]) > 256 * 8
				|| Math.abs(ops.pmove.origin[1] - ps.pmove.origin[1]) > 256 * 8
				|| Math.abs(ops.pmove.origin[2] - ps.pmove.origin[2]) > 256 * 8)
			ops = ps; // don't interpolate

		lerp = Globals.cl.lerpfrac;

		// calculate the origin
		if ((Globals.cl_predict.value != 0) && 0 == (Globals.cl.frame.playerstate.pmove.pm_flags & PlayerMove.PMF_NO_PREDICTION)) { // use
																																 // predicted
																																 // values
			int delta;

			backlerp = 1.0f - lerp;
			for (i = 0; i < 3; i++) {
				Globals.cl.refdef.vieworg[i] = Globals.cl.predicted_origin[i] + ops.viewoffset[i] + Globals.cl.lerpfrac
						* (ps.viewoffset[i] - ops.viewoffset[i]) - backlerp * Globals.cl.prediction_error[i];
			}

			// smooth out stair climbing
			delta = (int) (Globals.cls.realtime - Globals.cl.predicted_step_time);
			if (delta < 100)
				Globals.cl.refdef.vieworg[2] -= Globals.cl.predicted_step * (100 - delta) * 0.01;
		} else { // just use interpolated values
			for (i = 0; i < 3; i++)
				Globals.cl.refdef.vieworg[i] = ops.pmove.origin[i] * 0.125f + ops.viewoffset[i] + lerp
						* (ps.pmove.origin[i] * 0.125f + ps.viewoffset[i] - (ops.pmove.origin[i] * 0.125f + ops.viewoffset[i]));
		}

		// if not running a demo or on a locked frame, add the local angle
		// movement
		if (Globals.cl.frame.playerstate.pmove.pm_type < Constants.PM_DEAD) { // use
																			// predicted
																			// values
			for (i = 0; i < 3; i++)
				Globals.cl.refdef.viewangles[i] = Globals.cl.predicted_angles[i];
		} else { // just use interpolated values
			for (i = 0; i < 3; i++)
				Globals.cl.refdef.viewangles[i] = Math3D.LerpAngle(ops.viewangles[i], ps.viewangles[i], lerp);
		}

		for (i = 0; i < 3; i++)
			Globals.cl.refdef.viewangles[i] += Math3D.LerpAngle(ops.kick_angles[i], ps.kick_angles[i], lerp);

		Math3D.AngleVectors(Globals.cl.refdef.viewangles, Globals.cl.v_forward, Globals.cl.v_right, Globals.cl.v_up);

		// interpolate field of view
		Globals.cl.refdef.fov_x = ops.fov + lerp * (ps.fov - ops.fov);

		// don't interpolate blend color
		for (i = 0; i < 4; i++)
			Globals.cl.refdef.blend[i] = ps.blend[i];

		// add the weapon
		AddViewWeapon(ps, ops);
	}

	/*
	 * =============== CL_AddEntities
	 * 
	 * Emits all entities, particles, and lights to the refresh ===============
	 */
	static void AddEntities() {
		if (Globals.cls.state != Constants.ca_active)
			return;

		if (Globals.cl.time > Globals.cl.frame.servertime) {
			if (Globals.cl_showclamp.value != 0)
				Com.Printf("high clamp " + (Globals.cl.time - Globals.cl.frame.servertime) + "\n");
			Globals.cl.time = Globals.cl.frame.servertime;
			Globals.cl.lerpfrac = 1.0f;
		} else if (Globals.cl.time < Globals.cl.frame.servertime - 100) {
			if (Globals.cl_showclamp.value != 0)
				Com.Printf("low clamp " + (Globals.cl.frame.servertime - 100 - Globals.cl.time) + "\n");
			Globals.cl.time = Globals.cl.frame.servertime - 100;
			Globals.cl.lerpfrac = 0;
		} else
			Globals.cl.lerpfrac = 1.0f - (Globals.cl.frame.servertime - Globals.cl.time) * 0.01f;

		if (Globals.cl_timedemo.value != 0)
			Globals.cl.lerpfrac = 1.0f;

		/*
		 * is ok.. CL_AddPacketEntities (cl.frame); CL_AddTEnts ();
		 * CL_AddParticles (); CL_AddDLights (); CL_AddLightStyles ();
		 */

		CalcViewValues();
		// PMM - moved this here so the heat beam has the right values for the
		// vieworg, and can lock the beam to the gun
		AddPacketEntities(Globals.cl.frame);

		ClientTent.AddTEnts();
		ClientEffects.AddParticles();
		ClientEffects.AddDLights();
		ClientEffects.AddLightStyles();
	}

	/*
	 * =============== CL_GetEntitySoundOrigin
	 * 
	 * Called to get the sound spatialization origin ===============
	 */
	public static void GetEntitySoundOrigin(int ent, float[] org) {
		ClientEntity old;

		if (ent < 0 || ent >= Constants.MAX_EDICTS)
			Com.Error(Constants.ERR_DROP, "CL_GetEntitySoundOrigin: bad ent");
		old = Globals.cl_entities[ent];
		Math3D.VectorCopy(old.lerp_origin, org);

		// FIXME: bmodel issues...
	}
}
