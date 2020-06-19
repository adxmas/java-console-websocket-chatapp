import java.sql.Blob;

public class serverUser {

    private String name;
    private String pw;

    public serverUser(String name, String pw){
        this.name = name;
        this.pw = pw;
    }

    public String getName() {
        return name;
    }
    public String getPw() {
        return pw;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setPw(String pw) {
        this.pw = pw;
    }

    @Override
    public String toString() {
        return this.name + " " + this.pw;
    }
}
