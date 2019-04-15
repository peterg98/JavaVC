import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/* A commit. Each commit has a hash to the previous commit in the commit tree.*/
public class Commit implements Serializable {
    private String commitHash;
    private Date date;
    private SimpleDateFormat format;
    private String formattedDate;
    private String commitMessage;
    private Commit prevCommit;
    private Branch branch;
    private HashSet<String> stagedFiles;
    private HashSet<String> oldFiles;
    private HashSet<String> removedFiles;
    private HashSet<String> allFiles;
    private String hash;


    public Commit(Commit prevCommit, Branch branch, String commitMessage, HashSet<String> stagedFiles, HashSet<String> removedFiles) {
        this.prevCommit = prevCommit;
        this.branch = branch;
        this.commitMessage = commitMessage;
        this.stagedFiles = stagedFiles;
        this.format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.date = new Date();
        this.formattedDate = format.format(this.date);
        this.removedFiles = removedFiles;
        this.hash = this.commitHash();
    }

    private String commitHash() {
        //digest of the SHA-1 hash
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String str = this.commitMessage + this.formattedDate;
            //Convert sting to SHA-1 byte array
            byte[] SHA1 = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return JavaVC.convertToHex(SHA1, true);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Hashing algorithm not found");
            return "";
        }
    }
    public String getCommitHash() { return this.hash; }
}
