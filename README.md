# JavaVC
A version control system made with Java modeled after git. This is for demonstration purposes only, and does not work for folders and files
except for .txt files. It utilizes Java's built-in serialization methods to preserve state and SHA1 hashing for commits and blobs.


### List of available commands:

`init` - Initializes a .javavc repository in the current working directory

`commit -m "your_message"` - commit your changes to this branch

`add .` - Adds all files to the staging area

`add -f [filename]` - Adds specified file to staging area

`status` - Shows the current staging directory

`rm [filename]` - Removes this file from the staging area

`reset [hash]` - Resets the current working directory to the commit identified by `hash`. Equivalent to `git reset [hash] --hard`

`log` - Shows all the commits local to this branch, and if any, the parent branch

`log --global` - Shows the history of all commits

`checkout --filename` - Replaces the file in the current working directory with name `filename` with the same file at HEAD

`checkout -c [hash] --filename` - Same as above, but retrieves the file from the commit identified with `hash`

`checkout -b [branchname]` - Checks out to a new branch with name `branchanme`

`checkout [branchname]` - Checks out of this current branch to the new branch

`branch -d [branchname]` - Removes the branch at `branchname`

`merge [branch]` - Merges the changes from the sub branch to this branch. If there are conflicts, the content from both of the branches
will be shown on the conflicting file.

###### Example usage:

`java JavaVC init`
*Create a .txt file in the working directory*
