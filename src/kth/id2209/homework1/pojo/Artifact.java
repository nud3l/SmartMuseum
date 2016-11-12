package kth.id2209.homework1.pojo;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by tharidu on 11/9/16.
 */
public class Artifact implements Serializable {
    private long id;
    private String name;
    private String creator;
    private int completedYear;
    private String placeOfCreation;
    private String genre;
    private Enums.interest[] category;

    public Artifact(long id, String name, String creator, int completedYear, String placeOfCreation, String genre, Enums.interest[] category) {
        this.id = id;
        this.name = name;
        this.creator = creator;
        this.completedYear = completedYear;
        this.placeOfCreation = placeOfCreation;
        this.genre = genre;
        this.category = category;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public int getCompletedYear() {
        return completedYear;
    }

    public void setCompletedYear(int completedYear) {
        this.completedYear = completedYear;
    }

    public String getPlaceOfCreation() {
        return placeOfCreation;
    }

    public void setPlaceOfCreation(String placeOfCreation) {
        this.placeOfCreation = placeOfCreation;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Enums.interest[] getCategory() {
        return category;
    }

    public void setCategory(Enums.interest[] category) {
        this.category = category;
    }

    public boolean matchCategory(Enums.interest interest) {
        for (int i = 0; i < category.length; i++) {
            if (category[i] == interest) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Artifact{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", creator='" + creator + '\'' +
                ", completedYear=" + completedYear +
                ", placeOfCreation='" + placeOfCreation + '\'' +
                ", genre='" + genre + '\'' +
                ", category=" + Arrays.toString(category) +
                '}';
    }
}
