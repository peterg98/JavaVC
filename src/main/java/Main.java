import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) {
//        try {
//            FileOutputStream out = new FileOutputStream(new File("output.java"));
//            FileInputStream in = new FileInputStream(new File("test.txt"));
//            out.write("<<<<<<<<<HEAD\n".getBytes(StandardCharsets.UTF_8));
//            byte[] buffer = new byte[1024];
//            int len;
//            while ((len = in.read(buffer)) > 0) {
//                out.write(buffer, 0, len);
//            }
//            out.close();
//            in.close();
//        } catch (Exception e) {
//            System.out.println(e);
//        }
        String s = new File("src/main/java").listFiles()[1].toString();
        System.out.println(s);
    }
}
