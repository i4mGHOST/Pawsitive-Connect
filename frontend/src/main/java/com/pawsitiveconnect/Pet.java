package com.pawsitiveconnect;

public class Pet {
    private int id;
    private String name;
    private String species;
    private String breed;
    private int age;
    private String status;
    private String imageUrl;
    private boolean adoptionReady;
    private double adoptionFee;

    public Pet(int id, String name, String species, String breed, int age, String status) {
        this(id, name, species, breed, age, status, null, false, 0.0);
    }

    public Pet(int id, String name, String species, String breed, int age, String status, String imageUrl) {
        this(id, name, species, breed, age, status, imageUrl, false, 0.0);
    }

    public Pet(int id, String name, String species, String breed, int age, String status,
               String imageUrl, boolean adoptionReady, double adoptionFee) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.status = status;
        this.imageUrl = imageUrl;
        this.adoptionReady = adoptionReady;
        this.adoptionFee = adoptionFee;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecies() { return species; }
    public String getBreed() { return breed; }
    public int getAge() { return age; }
    public String getStatus() { return status; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAdoptionReady() { return adoptionReady; }
    public double getAdoptionFee() { return adoptionFee; }

    @Override
    public String toString() {
        return String.format("%s (%s - %s) • Fee: ₹%.2f", name, species, breed, adoptionFee);
    }
}