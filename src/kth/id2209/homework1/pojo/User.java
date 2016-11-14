package kth.id2209.homework1.pojo;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by tharidu on 11/9/16.
 */

// POJO class for User
public class User {
    private int age;
    private String occupation;
    private String gender;
    private Enums.interest[] interests;
    private ArrayList<Long> visitedArtifactIds;

    public User(int age, String occupation, String gender, Enums.interest[] interests) {
        this.age = age;
        this.occupation = occupation;
        this.gender = gender;
        this.interests = interests;
        this.visitedArtifactIds = new ArrayList<>();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Enums.interest[] getInterests() {
        return interests;
    }

    public void setInterests(Enums.interest[] interests) {
        this.interests = interests;
    }

    public ArrayList<Long> getVisitedArtifactIds() {
        return visitedArtifactIds;
    }

    public void setVisitedArtifactIds(ArrayList<Long> visitedArtifactIds) {
        this.visitedArtifactIds = visitedArtifactIds;
    }

    @Override
    public String toString() {
        return "User{" +
                "age=" + age +
                ", occupation='" + occupation + '\'' +
                ", gender='" + gender + '\'' +
                ", interests=" + Arrays.toString(interests) +
                ", visitedArtifactIds=" + visitedArtifactIds +
                '}';
    }
}
