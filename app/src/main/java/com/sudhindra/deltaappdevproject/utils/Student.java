package com.sudhindra.deltaappdevproject.utils;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;

public class Student {

    public static final String FIRST_NAME_KEY = "firstName", LAST_NAME_KEY = "lastName", HAS_PROFILE_PIC = "hasProfilePic", FOLLOWERS_KEY = "followers", FOLLOWING_KEY = "following", REGISTRATION_TOKENS = "registrationTokens";
    private String firstName, lastName;
    private int yearOfStudy;
    private int branch;
    private boolean hasProfilePic;
    private ArrayList<String> followers = new ArrayList<>(), following = new ArrayList<>();
    private ArrayList<String> registrationTokens;

    public Student() {

    }

    public Student(String firstName, String lastName, int yearOfStudy, int branch, boolean hasProfilePic, ArrayList<String> registrationTokens) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.yearOfStudy = yearOfStudy;
        this.branch = branch;
        this.hasProfilePic = hasProfilePic;
        this.registrationTokens = registrationTokens;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(int yearOfStudy) {
        this.yearOfStudy = yearOfStudy;
    }

    public int getBranch() {
        return branch;
    }

    public void setBranch(int branch) {
        this.branch = branch;
    }

    public boolean getHasProfilePic() {
        return hasProfilePic;
    }

    public void setHasProfilePic(boolean hasProfilePic) {
        this.hasProfilePic = hasProfilePic;
    }

    public ArrayList<String> getFollowers() {
        return followers;
    }

    public void setFollowers(ArrayList<String> followers) {
        this.followers = followers;
    }

    public ArrayList<String> getFollowing() {
        return following;
    }

    public void setFollowing(ArrayList<String> following) {
        this.following = following;
    }

    public ArrayList<String> getRegistrationTokens() {
        return registrationTokens;
    }

    public void setRegistrationTokens(ArrayList<String> registrationTokens) {
        this.registrationTokens = registrationTokens;
    }

    @Exclude
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Exclude
    public String getYOSString() {
        switch (yearOfStudy) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            case 4:
                return "4th";
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return "Student{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", yearOfStudy=" + yearOfStudy +
                ", branch=" + branch +
                ", followers=" + followers +
                ", following=" + following +
                '}';
    }
}
