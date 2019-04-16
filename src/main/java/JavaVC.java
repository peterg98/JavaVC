import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.security.MessageDigest;

public class JavaVC implements Serializable {
    private Commit HEAD;
    private Branch currentBranch; //Current Branch of the HEAD commit
    private HashSet<String> stagedFiles;
    private HashSet<String> removedFiles;
    private HashMap<String, Commit> branchNameToBranchHeadCommit;
    private HashSet<String> IGNORED_FILES;
    public String BLOB_DIR = ".javavc/blobs";
    private File cwd = new File(System.getProperty("user.dir"));

    public JavaVC() {
        HEAD = null;
        currentBranch = new Branch(HEAD, "master");
        stagedFiles = new HashSet<String>();
        removedFiles = new HashSet<String>();
        IGNORED_FILES = new HashSet<String>();
        branchNameToBranchHeadCommit = new HashMap<String, Commit>();
        Collections.addAll(IGNORED_FILES, ".javavc", ".idea", "src", "target", ".gitignore", "JavaVC.iml", "pom.xml", ".git");
    }


    /* Initialization of the .javavc repository. This is where all the
    * snapshots of file contents and commits will go to.*/
    public void init() {
        File folder = new File (".javavc");
        if (!folder.exists()) {
            folder.mkdir();
            this.currentBranch = new Branch(HEAD, "master");
            this.commit("Initial commit", true);
        } else {
            System.out.println("A javavc folder already exists inside this repository");
        }
    }

    public void commit(String commitMessage, boolean firstCommit) {
        Commit commit = new Commit(HEAD, currentBranch, commitMessage, new HashSet<String>(stagedFiles), new HashSet<String>(removedFiles));
        String commitHash = commit.getCommitHash();
        commit.serializeCommit();
        HEAD = commit;
        currentBranch.setBranchHead(commit);
        branchNameToBranchHeadCommit.put(currentBranch.getBranchName(), commit);

    }

    public void serializeAndWriteFile() {
        File blobDir = new File(BLOB_DIR);
        if (!blobDir.exists()) {
            blobDir.mkdirs();
        }
        File[] listOfFiles = cwd.listFiles();
        for (File f: listOfFiles) {
            if (f.getName().equals("test.txt")) {
                try {
                    String hash = generateBlobHash(f);
                    String path = ".javavc/blobs/" + hash;
                    File blobDest = new File(path);
                    if (!blobDest.exists()) {
                        blobDest.mkdirs();
                    }
//                FileOutputStream dir = new FileOutputStream(blobDir + "/" + hash);
//                ObjectOutputStream blob = new ObjectOutputStream(dir);
//                blob.writeObject(f);
//                blob.close();
                    Files.copy(f.toPath(), (new File(path + "/" + f.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    System.out.println(e);
                }
            }

        }
    }

    private String generateBlobHash(File f) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            InputStream fi = new FileInputStream(f);
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fi.read(buffer); //read 8192 bytes at a time for hash digest
                if (n > 0) {
                    md.update(buffer, 0, n);
                }
            }
            byte[] SHA1 = md.digest();
            return convertToHex(SHA1, false);

        } catch (Exception e) {
            System.out.println("Error during loading file or unknown hashing algorithm.");
            return "";
        }
    }

    public void status() {
        System.out.println("Files not staged for commit:");
        for (File f: cwd.listFiles()) {
            if (!IGNORED_FILES.contains(f.getName())) {
                System.out.println(f.getName());
            }
        }
    }

    public void add() {

    }

    public void log() {

    }

    public static String convertToHex(byte[] bytearray, boolean isCommit) {
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < bytearray.length; i++) {
            //By default, Java's bytes are signed. Mask out all negative bits and
            //trim digits after 255 by a bitwise AND operation with 255.
            String hex = Integer.toHexString(bytearray[i] & 0xff);
            //For hashing commits, make it easily recognizable (from branches and blobs)
            // by prepending a c to the beginning of the hash.
            if (isCommit && hex.length() == 1) {
                hash.append('c');
            }
            hash.append(hex);
        }
        System.out.println(hash);
        return hash.toString();
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        JavaVC vc = new JavaVC();
        vc.init();
    }
}
