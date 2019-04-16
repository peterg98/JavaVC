import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        JavaVC vc = new JavaVC();
        Commit.deserializeCommit("4687b16b2cbcc73020645e29d4f1412255679cc9");
    }
}
