package kth.id2209.homework1.pojo;

import java.util.Date;

/**
 * Created by tharidu on 11/9/16.
 */
public class Artifact {
    private long id;
    private String name;
    private String description;
    private String creator;
    private Date dateOfCreation;
    private String placeOfCreation;
    private String genre;
    private Interests[] category;

    public Artifact(long id, String name, String description, String creator, Date dateOfCreation, String placeOfCreation, String genre, Interests[] category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.dateOfCreation = dateOfCreation;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Date getDateOfCreation() {
        return dateOfCreation;
    }

    public void setDateOfCreation(Date dateOfCreation) {
        this.dateOfCreation = dateOfCreation;
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

    public Interests[] getCategory() {
        return category;
    }

    public void setCategory(Interests[] category) {
        this.category = category;
    }
}
