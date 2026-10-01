package lk.janith.smart_tourism.model;

public class User {

    private String uid;
    private String fname;
    private String lname;
    private String email;
    private String country;
    private String birthday;
    private String profilePic;

    public User() {
    }

    public User(String uid, String fname, String lname, String email, String country, String birthday, String profilePic) {
        this.uid = uid;
        this.fname = fname;
        this.lname = lname;
        this.email = email;
        this.country = country;
        this.birthday = birthday;
        this.profilePic = profilePic;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFname() {
        return fname;
    }

    public void setFname(String fname) {
        this.fname = fname;
    }

    public String getLname() {
        return lname;
    }

    public void setLname(String lname) {
        this.lname = lname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }
}