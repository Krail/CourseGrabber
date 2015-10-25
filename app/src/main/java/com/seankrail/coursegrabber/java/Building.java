package com.seankrail.coursegrabber.java;

/**
 * Created by Krail on 3/15/15.
 */
public class Building
{
    private BuildingEnum building;
    // Consider how necessary it is to have double instead of float
    private double latitude;
    private double longitude;

    public Building(String abbr, double latitude, double longitude)
    {
        this.building = BuildingEnum.valueOf(abbr);
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public Building(BuildingEnum be)
    {
        this.building = be;
    }

    public BuildingEnum getBuilding() {return this.building;}
    public double getLatitude() {return this.latitude;}
    public double getLongitude() {return this.longitude;}
}
