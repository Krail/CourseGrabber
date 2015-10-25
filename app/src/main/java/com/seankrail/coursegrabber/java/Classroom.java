package com.seankrail.coursegrabber.java;

/**
 * Created by Krail on 3/15/15.
 */
public class Classroom
{
    private Building building;
    private short roomNumber; // Assumes all buildings are under 327 stories...
    private char roomLetter;
    private String roomName = "";

    /**
     * A room letter of '~' (tilde) represents null, a room number with no letter designation.
     * Otherwise the room letter should be capitalized.
     * @param building
     * @param roomNumber
     * @param roomLetter
     */
    public Classroom(Building building, short roomNumber, char roomLetter)
    {
        this.building = building;
        this.roomNumber = roomNumber;
        this.roomLetter = roomLetter;
    }
    public Classroom(BuildingEnum be, short roomNumber, char roomLetter)
    {
        this.building = new Building(be);
        this.roomNumber = roomNumber;
        this.roomLetter = roomLetter;
    }
    public Classroom(BuildingEnum be, String name) {
        this.building = new Building(be);
        this.roomName = name;
    }


    public Building getBuilding() {return this.building;}
    public short getRoomNumber() {return this.roomNumber;}
    public char getRoomLetter() {return this.roomLetter;}
    public String getName() {return this.roomName;}

    @Override
    public String toString() {
        String string = "";
        if (roomNumber < 100) string = "0" + roomNumber;
        else string = roomNumber + "";
        if (roomLetter != '~') string += roomLetter;
        return string + " " + this.building.getBuilding().getTitle();
    }
}
