import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        JavaVC vc = new JavaVC();
        Commit.deserializeCommit("6136cc92d46d59a3886efd3dcabe38a3a3c54cc5");
    }
}
