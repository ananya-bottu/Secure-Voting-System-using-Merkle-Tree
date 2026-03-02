import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

class Node {
    Node left, right, parent;
    String value, content;

    Node(Node left, Node right, String value, String content) {
        this.left = left;
        this.right = right;
        this.value = value;
        this.content = content;
        if (left != null) left.parent = this;
        if (right != null) right.parent = this;
    }
    static String hash(String val) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(val.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

class MerkleTree {
    Node root;
    List<Node> leaves;
    List<String> originalValues;

    MerkleTree(List<String> values) throws NoSuchAlgorithmException {
        this.leaves = new ArrayList<>();
        this.originalValues = new ArrayList<>(values);
        for (String val : values) {
            leaves.add(new Node(null, null, Node.hash(val), val));
        }
        root = buildIncrementalTree();
    }
    public Node buildIncrementalTree() throws NoSuchAlgorithmException {
        if (leaves.isEmpty()) return new Node(null, null, Node.hash(""), "");

        List<Node> currentLevel = new ArrayList<>(leaves);
        while (currentLevel.size() > 1) {
            List<Node> nextLevel = new ArrayList<>();

            for (int i = 0; i < currentLevel.size(); i += 2) {
                Node left = currentLevel.get(i);
                Node right;

                if (i + 1 < currentLevel.size()) {
                    right = currentLevel.get(i + 1);
                } else {
                    right = new Node(null, null, left.value, left.content);
                }

                String combinedHash = Node.hash(left.value + right.value);
                String combinedContent = left.content + "+" + right.content;
                Node parent = new Node(left, right, combinedHash, combinedContent);

                nextLevel.add(parent);
            }
            currentLevel = nextLevel;
        }

        return currentLevel.get(0); 
    }

    public void addLeaf(String value) throws NoSuchAlgorithmException {
        Node newLeaf = new Node(null, null, Node.hash(value), value);
        leaves.add(newLeaf);
        originalValues.add(value);
        root = buildIncrementalTree();
    }

    void printTree() {
        System.out.println("=== MERKLE TREE STRUCTURE ===");
        printTreeRec(root, 0);
    }

    private void printTreeRec(Node node, int level) {
        if (node != null) {
            String indent = "  ".repeat(level);
            System.out.println(indent + "Level " + level + ":");
            System.out.println(indent + "Value: " + (node.value.length() > 20 ? node.value.substring(0, 20) + "..." : node.value));
            //System.out.println(indent + "Content: " + (node.content.length() > 50 ? node.content.substring(0, 50) + "..." : node.content));
            if (node.left != null || node.right != null) {
                System.out.println(indent + "--- Children ---");
                printTreeRec(node.left, level + 1);
                printTreeRec(node.right, level + 1);
            }
        }
    }

    String getRootHash() {
        return root != null ? root.value : "";
    }

    int getVoteCount() {
        return originalValues.size();
    }
}
class VotingSystem {
    private MerkleTree merkleTree;
    private List<String> votes;
    private Map<String, String> voterToCandidate;
    private Map<String, Integer> candidateVotes;
    private boolean electionActive;
    private String officialRoot;
    private PriorityQueue<Map.Entry<String, Integer>> rankingQueue; 

    public VotingSystem() throws NoSuchAlgorithmException {
        this.votes = new ArrayList<>();
        this.voterToCandidate = new HashMap<>();
        this.candidateVotes = new HashMap<>();
        this.electionActive = true;
        this.merkleTree = new MerkleTree(new ArrayList<>());
        this.rankingQueue = new PriorityQueue<>((a, b) -> b.getValue() - a.getValue()); 
    }

    public void castVote(String voterId, String candidate) throws NoSuchAlgorithmException {
        if (!electionActive) {
            System.out.println(" Election has ended!");
            return;
        }
        if (voterToCandidate.containsKey(voterId)) {
            System.out.println(" Voter " + voterId + " has already voted!");
            return;
        }

        String voteData = voterId + ":" + candidate + ":" + System.currentTimeMillis();
        votes.add(voteData);
        voterToCandidate.put(voterId, candidate);
        candidateVotes.put(candidate, candidateVotes.getOrDefault(candidate, 0) + 1);

        rankingQueue.clear();
        rankingQueue.addAll(candidateVotes.entrySet());

        merkleTree.addLeaf(voteData);

        System.out.println(" Voter " + voterId + " successfully voted ");
        System.out.println("Current Merkle Root: " + merkleTree.getRootHash().substring(0, 16) + "...");
    }

    public void showResults() {
        System.out.println("\n=== ELECTION RESULTS ===");
        System.out.println("Total Votes: " + merkleTree.getVoteCount());
        System.out.println("Merkle Root: " + merkleTree.getRootHash());
        System.out.println("Election Status: " + (electionActive ? "ACTIVE" : "ENDED"));

        System.out.println("\n Candidate Results (by votes):");
        PriorityQueue<Map.Entry<String, Integer>> pqCopy = new PriorityQueue<>(rankingQueue);
        while (!pqCopy.isEmpty()) {
            Map.Entry<String, Integer> entry = pqCopy.poll();
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " votes");
        }
    }

    public void verifyVote(String voterId) {
        if (!voterToCandidate.containsKey(voterId)) {
            System.out.println(" No vote found for voter: " + voterId);
            return;
        }
        System.out.println(" Vote verification successful!");
        System.out.println("   Voter: " + voterId);
        System.out.println("   Merkle Root: " + merkleTree.getRootHash());
    }

    public void verifyElectionIntegrity() throws NoSuchAlgorithmException {
        MerkleTree recomputedTree = new MerkleTree(votes);
        String recomputedRoot = recomputedTree.getRootHash();

        System.out.println("\n=== INTEGRITY VERIFICATION ===");
        if (officialRoot == null) {
            System.out.println(" No official root stored! Run endElection() first.");
            return;
        }
        if (recomputedRoot.equals(officialRoot)) {
            System.out.println("Election data is INTACT. No votes were tampered with.");
        } else {
            System.out.println(" Election data COMPROMISED!");
            System.out.println("Expected Root: " + officialRoot);
            System.out.println("Found Root:    " + recomputedRoot);
        }
    }

    public void tamperVote(int index, String newVote) {
        if (index < 0 || index >= votes.size()) {
            System.out.println("Tamper failed: invalid index");
            return;
        }
        System.out.println("\n!! DEMO TAMpering: replacing vote at index " + index + " !!");
        votes.set(index, newVote);
        try {
            merkleTree = new MerkleTree(votes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void displayMerkleTree() {
        merkleTree.printTree();
    }

    public void endElection() {
        electionActive = false;
        officialRoot = merkleTree.getRootHash();
        System.out.println("\n ELECTION ENDED");
        System.out.println(" Final Merkle Root: " + officialRoot);
    }
}
public class CaseStudy {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.out.println("  VOTECHAIN - SECURE VOTING SYSTEM\n");

        VotingSystem election = new VotingSystem();
        String[] candidates = {"Alice", "Bob", "Charlie"};
        Random random = new Random();

        System.out.println("=== AUTOMATED VOTE CASTING ===");
        for (int i = 1; i <= 6; i++) {
            String voterId = "Voter" + i;
            String candidate = candidates[random.nextInt(candidates.length)];
            election.castVote(voterId, candidate);

            if (i % 2 == 0) System.out.println("--- " + i + " votes cast so far ---");
        }

        election.showResults();

        System.out.println("\n=== VOTE VERIFICATION ===");
        election.verifyVote("Voter2");
        election.verifyVote("Voter4");
        election.verifyVote("Voter6");

        System.out.println("\n=== MERKLE TREE VISUALIZATION ===");
        election.displayMerkleTree();

        election.endElection();

        //election.tamperVote(2, "TamperedVoter:FakeCandidate:999999999");

        election.verifyElectionIntegrity();
    }
}
