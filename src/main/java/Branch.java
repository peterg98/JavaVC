import java.io.Serializable;

/* A clone of git's branching system. Each branch has a head denoting the latest commit to
* that branch, and a branch name.*/
public class Branch implements Serializable {
    private Commit head;
    private String branchName;
    public Branch(Commit head, String branchName) {
        this.head = head;
        this.branchName = branchName;
    }

    public void setBranchHead(Commit commit) {
        this.head = commit;
    }

    public void setBranchName(String name) {
        this.branchName = name;
    }

    public String getBranchName() { return this.branchName; }

    public Commit getCommit() { return this.head; }
}
