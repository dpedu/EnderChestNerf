package com.dpedu.ecnerf;

import java.io.Serializable;

//Class for holding chest at locations to player name associations 
class PlacedChestData implements Serializable {
	int x,y,z;
	String owner;
	String world;
	public PlacedChestData(int _x, int _y, int _z, String _owner, String _world) {
		x=_x;
		y=_y;
		z=_z;
		owner=_owner;
		world=_world;
	}
}
