# Secure Voting System using Merkle Tree

This project implements a Secure Voting System using a Merkle Tree data structure in Java. It provides functionalities to cast votes, verify individual votes, and ensure the integrity of the entire election process.

## Architecture

The system uses a `MerkleTree` constructed from individual votes to ensure that no vote can be tampered with after it is cast.

- **Node Class:** Represents a single node in the Merkle Tree, containing a left child, right child, value (hash), and the actual vote content. It uses SHA-256 for cryptographic hashing.
- **MerkleTree Class:** Manages the construction and updating of the tree as new leaves (votes) are added incrementally.
- **VotingSystem Class:** Orchestrates the voting process, maintains a Priority Queue of candidates by vote count, and provides methods to verify individual votes or the integrity of the entire election.

## How to Run
1. Ensure you have Java installed.
2. Compile the code: `javac CaseStudy.java`
3. Run the application: `java CaseStudy`

## Operations Demonstrated
- **Automated Vote Casting:** Simulates users casting votes for various candidates.
- **Vote Verification:** Verifies that specific votes exist in the Merkle Tree.
- **Merkle Tree Visualization:** Prints out the hierarchical structure of the generated Merkle Tree.
- **Integrity Verification:** Allows a simulated tampering to happen, and then detects if the data was modified by comparing the recomputed root hash against the official root hash.
