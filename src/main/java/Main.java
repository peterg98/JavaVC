import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
        try {
            FileOutputStream out = new FileOutputStream(new File("output.java"));
            out.write("HEAD".getBytes(StandardCharsets.UTF_8), 0, 4);
        } catch (Exception e) {
            System.out.println(e);
        }

    }
}
