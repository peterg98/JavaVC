import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.security.MessageDigest;
import java.util.Arrays;

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
        if (commitMessage.equals("")) {
            System.out.println("Commit Message is empty.");
            return;
        }
        if (stagedFiles.isEmpty() && removedFiles.isEmpty() && !isFirst) {
            System.out.println("No changes made. Aborting");
            return;
        }
        Commit commit = new Commit(branchNameToBranchHeadCommit.get(currentBranch), latestCommit, currentBranch, commitMessage, author, new HashMap<>(stagedFiles), new HashSet<>(removedFiles));
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
            stagedFiles.remove(fileName);
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
        Commit c;
        File path;
        if (arg.equals("-b")) { //checkout -b branchName
            if (!branchNameToBranchHeadCommit.containsKey(branchName)) {
                branchNameToBranchHeadCommit.put(branchName, HEAD);
                currentBranch = branchName;
            } else {
                System.out.println("The branch at " + branchName + " already exists.");
            }
        } else if (!fileName.equals("") && branchName.equals("")) {
            File f;
            if (arg.equals("-c")) { //checkout -c commitID --fileName
                f = new File(".javavc/commits/" + commitID);
                if (!f.exists()) {
                    System.out.println("Commit at " + commitID + " does not exist");
                    return;
                }
                c = Commit.deserializeCommit(commitID);

            } else { c = HEAD; } //checkout --fileName
            try {
                System.out.println(c.getCommitHash());
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
        } else { //checkout branchName NEEDS FIXING: UPDATE FILES IN CURRENT WORKING DIRECTORY
            currentBranch = branchName;
            HEAD = branchNameToBranchHeadCommit.get(currentBranch);
            for (File f: cwd.listFiles()) {
                if (isAllowedFile(f.getName())) {
                    f.delete();
                }
            }
            for (String fileHash: HEAD.getStagedFiles().values()) { //TO FIX

            }
        }
    }

    private void reset(String commitHash) {
        File commitLoc = new File(".javavc/commits/" + commitHash);
        if (!commitLoc.exists()) {
            System.out.println("The commit at " + commitHash + " does not exist.");
            return;
        }
        Commit H = latestCommit;
        while (!H.getCommitHash().equals(commitHash)) {
            branchNameToBranchHeadCommit.put(H.getCommitBranch(), H.getPrevCommit());
            H = H.getGlobalPrevCommit();
        }
        currentBranch = H.getCommitBranch();
        stagedFiles = H.getStagedFiles();
        removedFiles = H.getRemovedFiles();
        HEAD = H;
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
        switch (args[0]) {
            case "init":
                vc.init();
                break;
            case "add":
                if (args.length >= 2) {
                    if (args[1].equals(".")) vc.add(".", null);
                    else if (args[1].equals("-f")) {
                        if (args.length == 3) vc.add("-f", args[2]);
                        else System.out.println("add -f requires one argument filename");
                    }
                } else System.out.println("add requires arguments");


                break;
            case "commit":
                if (args.length == 3 && args[1].equals("-m")) vc.commit(args[2], false);
                else System.out.println("Invalid arguments provided for commit");
                break;
            case "rm":
                if (args.length >= 2) vc.rm(args[1]);
                else System.out.println("No arguments provided to rm");
                break;
            case "status":
                vc.status();
                break;
            case "reset":
                if (args.length < 2) System.out.println("reset requires one argument commitHash");
                vc.reset(args[1]);
                break;
            case "log":
                if (args.length == 1) vc.log("");
                else if (args.length == 2 && args[1].equals("--global")) vc.log("--global");
                else System.out.println("Invalid arguments supplied to log");
                break;
            /*Usage:
            * checkout --fileName
            * checkout -c commitHash --filename
            * checkout -b branchName
            * checkout branchName*/
            case "checkout":
                if (args.length < 2) System.out.println("checkout requires at least 1 argument");
                if (args.length == 2) {
                    if (args[1].startsWith("--")) {
                        vc.checkout("", "", "", args[1].substring(2));
                    } else {
                        vc.checkout("", "", args[1], "");
                    }
                } else if (args.length == 3) {
                    vc.checkout(args[1], "", args[2], "");
                } else if (args.length == 4 && args[1].equals("-c") && args[3].startsWith("--")) {
                    vc.checkout(args[1], args[2], "", args[3].substring(2));
                }
        }
        vc.serializeStatus();
    }
}
