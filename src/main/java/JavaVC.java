import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.security.MessageDigest;

public class JavaVC implements Serializable {
    private static final String author = "Peter Gang";
    private Commit HEAD;
    private Commit latestCommit;
    private String currentBranch; //Current Branch of the HEAD commit
    private HashMap<String, String> stagedFiles; //Files ready to be committed
    private HashSet<String> removedFiles; //Files not present in the new staging area
    private HashMap<String, Commit> branchNameToBranchHeadCommit;
    private HashSet<String> ALLOWED_SUFFIXES;
    public String BLOB_DIR = ".javavc/blobs";
    public static File cwd = new File(System.getProperty("user.dir"));

    public JavaVC() {
        HEAD = null;
        currentBranch = "master";
        stagedFiles = new HashMap<>();
        removedFiles = new HashSet<>();
        ALLOWED_SUFFIXES = new HashSet<>();
        branchNameToBranchHeadCommit = new HashMap<>();
        ALLOWED_SUFFIXES.add(".txt");
        branchNameToBranchHeadCommit.put("master", HEAD);
    }


    /* Initialization of the .javavc repository. This is where all the
    * snapshots of file contents and commits will go to.*/
    public void init() {
        File folder = new File (".javavc");
        if (!folder.exists()) {
            folder.mkdir();
            this.currentBranch = "master";
            branchNameToBranchHeadCommit.put("master", HEAD);
            this.commit("Initial commit", true);
        } else {
            System.out.println("A javavc folder already exists inside this repository");
        }
    }

    /* Equivalent of git commit: takes all the files from the staging area and serializes the Commit. */
    private void commit(String commitMessage, boolean isFirst) {
        if (stagedFiles.isEmpty() && removedFiles.isEmpty() && !isFirst) {
            System.out.println("No changes made. Aborting");
            return;
        }
        Commit commit = new Commit(branchNameToBranchHeadCommit.get(currentBranch), latestCommit, currentBranch, commitMessage, author, new HashMap<>(stagedFiles), new HashSet<>(removedFiles));
        String commitHash = commit.getCommitHash();
        commit.serializeCommit();
        HEAD = commit;
        latestCommit = commit;
        stagedFiles = new HashMap<>();
        removedFiles = new HashSet<>();
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

    private boolean isAllowedFile(String fileName) {
        for (String suffix: ALLOWED_SUFFIXES) {
            if (!fileName.endsWith(suffix)) return false;
        }
        return true;
    }

    public void status() {
        System.out.println("Branches:\n");
        for (String b: branchNameToBranchHeadCommit.keySet()) {
            if (b.equals(currentBranch)) System.out.print("*");
            System.out.println(b);
        }
        System.out.println("\nFiles staged for commit:\n");
        for (String key: stagedFiles.keySet()) {
            System.out.printf("\t%s\n", key);
        }
        System.out.println("\nFiles not staged for commit \n");
        for (File f: cwd.listFiles()) {
            if (isAllowedFile(f.getName()) && !stagedFiles.containsKey(f.getName())) {
                System.out.printf("\t%s\n", f.getName());
            }
        }

        System.out.println("\nRemoved Files:\n");
        for (String s: removedFiles) {
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
                if (isAllowedFile(f.toString())) {
                    File fi = new File(f.getName());
                    String hash = serializeAndWriteFile(fi);
                    stagedFiles.put(f.getName(), hash);
                }

            }
        }
    }

    public void rm(String fileName) {
        if (stagedFiles.containsKey(fileName)) {
            removedFiles.add(fileName);
            File file = new File(fileName);
            file.delete();
        } else {
            System.out.println("File does not exist or is not in the staging area");
        }
    }

    public void log(String arg) {
        Commit h = arg.equals("--global") ? latestCommit : HEAD;
        while (h != null) {
            System.out.println("commit " + h.getCommitHash());
            System.out.println("Author: " + h.getCommitAuthor());
            System.out.println("Date: " + h.getCommitDate());
            System.out.println("Branch: " + h.getCommitBranch());
            System.out.printf("\n\n\t%s\n\n\n", h.getCommitMessage());
            if (!arg.equals("--global")) {
                h = h.getPrevCommit();
            } else {
                h = h.getGlobalPrevCommit();
            }
        }

    }

    public void checkout(String arg, String commitID, String branchName, String fileName) {
        FileInputStream file;
        ObjectInputStream in;
        Commit c;
        File path;
        if (arg.equals("-b")) {
            if (!branchNameToBranchHeadCommit.containsKey(branchName)) {
                branchNameToBranchHeadCommit.put(branchName, HEAD);
                currentBranch = branchName;
            } else {
                System.out.println("The branch at " + branchName + " already exists.");
            }
        } else if (!fileName.equals("") && !branchName.equals("")) {
            File f;
            if (arg.equals("-c")) {
                f = new File(".javavc/commits/" + commitID);
                if (!f.exists()) {
                    System.out.println("Commit at " + commitID + " does not exist");
                    return;
                }
                c = Commit.deserializeCommit(f.toString());

            } else { c = HEAD; }
            try {
                found : {
                    for (String fName : c.getStagedFiles().keySet()) {
                        if (fName.equals(fileName)) {
                            path = new File (".javavc/blobs/" + c.getStagedFiles().get(fName) + "/" + fName);
                            break found;
                        }
                    }
                    System.out.println("File " + fileName + " does not exist at commit " + commitID);
                    return;
                }
                Files.copy(path.toPath(), new File(fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                System.out.println(e);
            }
        } else {
            currentBranch = branchName;
            HEAD = branchNameToBranchHeadCommit.get(currentBranch);
        }
    }

    private void reset(String commitHash) {
        File commitLoc = new File(".javavc/commits/" + commitHash);
        if (!commitLoc.exists()) {
            System.out.println("The commit at " + commitHash + " does not exist.");
            return;
        }
        while (!HEAD.getCommitHash().equals(commitHash)) {
            branchNameToBranchHeadCommit.put(HEAD.getCommitBranch(), HEAD.getPrevCommit());
            HEAD = HEAD.getGlobalPrevCommit();
        }
        currentBranch = HEAD.getCommitBranch();
        stagedFiles = HEAD.getStagedFiles();
        removedFiles = HEAD.getRemovedFiles();
        for (File f: cwd.listFiles()) {
            if (isAllowedFile(f.getName())) {
                f.delete();
            }
        }
        for (String fileHash: stagedFiles.values()) {
            File dir = new File(".javavc/blobs/" + fileHash);
            for (File f: dir.listFiles()) {
                try {
                    Files.copy(f.toPath(), new File(f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }
    }

    private void merge() {

    }

    public static String convertToHex(byte[] bytearray, boolean isCommit) {
        StringBuffer hash = new StringBuffer();
        for (int i = 0; i < bytearray.length; i++) {
            //By default, Java's bytes are signed. Mask out all negative bits and
            //trim digits after 255 by a bitwise AND operation with hexadecimal 255.
            String hex = Integer.toHexString(bytearray[i] & 0xff);
            //For hashing commits, make it easily recognizable (from branches and blobs)
            // by prepending a c to the beginning of the hash.
            if (isCommit && hex.length() == 1) {
                hash.append('c');
            }
            hash.append(hex);
        }
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

    public static JavaVC deserialize() {
        String pathToSerial = ".javavc/JAVAVC.ser";
        try {
            FileInputStream file = new FileInputStream(pathToSerial);
            ObjectInputStream in = new ObjectInputStream(file);
            JavaVC deserialized = (JavaVC)(in.readObject());
            file.close();
            in.close();
            return deserialized;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
    }

    public static void main(String[] args) {
        File file = new File(".javavc/JAVAVC.ser");
        JavaVC vc = file.exists() ? deserialize() : new JavaVC();
//        vc.init();
//        vc.checkout("-b", "", "test", "");
//        vc.add(".", null);
//        vc.commit("Adding files to test branch", false);
//        vc.checkout("", "", "master", "");
//        vc.add(".", null);
//        vc.commit("Adding files to master", false);
//        vc.add(".", null);
//        vc.commit("Adding more files to master", false);
//        vc.checkout("", "", "master", "");
        vc.reset("9d86296cc4567f8059c35a98886390445496c6d5");
        vc.serializeStatus();
    }
}
