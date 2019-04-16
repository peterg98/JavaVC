import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.security.MessageDigest;

public class JavaVC implements Serializable {
    private static final String author = "Peter Gang";
    private Commit HEAD;
    private String currentBranch; //Current Branch of the HEAD commit
    private HashMap<String, String> stagedFiles; //Files ready to be committed
    private HashMap<String, String> removedFiles; //Files not present in the new staging area
    private HashMap<String, String> allFiles; //File name -> hash mapping
    private HashMap<String, Commit> branchNameToBranchHeadCommit;
    private HashSet<String> IGNORED_FILES;
    public String BLOB_DIR = ".javavc/blobs";
    public static File cwd = new File(System.getProperty("user.dir"));

    public JavaVC() {
        HEAD = null;
        currentBranch = "master";
        stagedFiles = new HashMap<>();
        removedFiles = new HashMap<>();
        allFiles = new HashMap<>();
        IGNORED_FILES = new HashSet<>();
        branchNameToBranchHeadCommit = new HashMap<>();
        Collections.addAll(IGNORED_FILES, ".javavc", ".idea", "src", "target", ".gitignore", "JavaVC.iml", "pom.xml", ".git");
    }


    /* Initialization of the .javavc repository. This is where all the
    * snapshots of file contents and commits will go to.*/
    public void init() {
        File folder = new File (".javavc");
        if (!folder.exists()) {
            folder.mkdir();
            this.currentBranch = "master";
            this.commit("Initial commit", true);
        } else {
            System.out.println("A javavc folder already exists inside this repository");
        }
    }

    public void commit(String commitMessage, boolean firstCommit) {
        Commit commit = new Commit(HEAD, currentBranch, commitMessage, author, new HashMap<>(stagedFiles), new HashMap<>(removedFiles));
        String commitHash = commit.getCommitHash();
        commit.serializeCommit();
        HEAD = commit;
        stagedFiles = new HashMap<>();
        removedFiles = new HashMap<>();
        branchNameToBranchHeadCommit.put(currentBranch, commit);

    }

    public String serializeAndWriteFile(File f) {
        File blobDir = new File(BLOB_DIR);
        if (!blobDir.exists()) {
            blobDir.mkdirs();
        }
        try {
            String hash = generateBlobHash(f);
            String path = ".javavc/blobs/" + hash;
            File blobDest = new File(path);
            if (!blobDest.exists()) {
                blobDest.mkdirs();
            }
            Files.copy(f.toPath(), (new File(path + "/" + f.getName())).toPath(), StandardCopyOption.REPLACE_EXISTING);
            return hash;
        } catch (IOException e) {
            System.out.println(e);
            return "";
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
            System.out.println(e);
            return "";
        }
    }

    public void status() {
        System.out.println("Files staged for commit:");
        for (String key: stagedFiles.keySet()) {
            System.out.printf("\t%s\n", key);
        }
        System.out.print("\n================================\n");
        System.out.println("Changes not staged for commit:");
        if (cwd.listFiles() != null) {
            for (File f: cwd.listFiles()) {
                if (!IGNORED_FILES.contains(f.getName())) {
                    System.out.printf("\t%s\n", f.getName());
                }
            }
        }
        System.out.print("\n================================\n");
        System.out.println("Removed Files:");
        for (String s: removedFiles.keySet()) {
            System.out.printf("\t%s\n", s);
        }

    }

    public void add(String arg, String fileName) {
        if (arg.equals("-f")) {
            File f = new File(fileName);
            String hash = serializeAndWriteFile(f);
            stagedFiles.put(f.getName(), hash);
        } else if (arg.equals(".")) {
            for (File f: cwd.listFiles()) {
                File fi = new File(f.getName());
                String hash = serializeAndWriteFile(fi);
                stagedFiles.put(f.getName(), hash);
            }
        }
    }

    public void log() {
        Commit h = HEAD;
        while (h != null) {
            System.out.println("commit " + h.getCommitHash());
            System.out.println("Author: " + h.getCommitAuthor());
            System.out.println("Date: " + h.getCommitDate());
            System.out.printf("\n\n\t%s\n\n", h.getCommitMessage());
            h = h.getPrevCommit();
        }
    }

    public void checkout(String flag, String name) {
        if (flag.equals("-b")) {
            if (!branchNameToBranchHeadCommit.containsKey(name)) {
                branchNameToBranchHeadCommit.put(name, HEAD);
            } else {
                currentBranch = name;
                HEAD = branchNameToBranchHeadCommit.get(currentBranch);
            }
        }
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

    public void serializeStatus() {
        File dir = new File(".javavc");
        if (!dir.exists()) {
            dir.mkdir();
        }
        String filePath = dir + "/" + "JAVAVC.ser";
        try {
            FileOutputStream file = new FileOutputStream(filePath);
            ObjectOutputStream serialized = new ObjectOutputStream(file);
            serialized.writeObject(this);
            serialized.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void deserialize() {

    }

    public static void main(String[] args) {
        File file = new File(".javavc/JAVAVC.ser");
        JavaVC vc = file.exists() ?
        vc.init();
    }
}
