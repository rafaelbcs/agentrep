// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

// ─── ERC-8004 Interfaces ──────────────────────────────────────────────────────
// Minimal interfaces for ERC-8004 integration (deployed Jan 29, 2026).
// Base Mainnet:
//   IdentityRegistry:   0x8004A169FB4a3325136EB29fA0ceB6D2e539a432
//   ReputationRegistry: 0x8004BAa17C55a88189AE136b182e5fdA19dE9b63
// Set address(0) to disable (default on testnet).
// NOTE: interface signatures to be verified against actual ERC-8004 ABI before mainnet deploy.

interface IERC8004IdentityRegistry {
    function isRegistered(address agent) external view returns (bool);
}

interface IERC8004ReputationRegistry {
    /// @param agent      Agent wallet address
    /// @param score      Score 0–10000 (×100, e.g. 8732 = 87.32)
    /// @param providerId keccak256 identifier of the data provider (AgentRep)
    function updateScore(address agent, uint256 score, bytes32 providerId) external;
}

// ─── AgentRepRegistry ─────────────────────────────────────────────────────────

/**
 * @title AgentRepRegistry
 * @notice Evaluation and dispute layer for AI agent reputation.
 *         Stores aggregated scores and emits events — full outcome data lives
 *         off-chain in PostgreSQL (MVP design).
 *
 *         Integrates with ERC-8004 as the data provider:
 *         - Reads identity from ERC-8004 IdentityRegistry (optional check)
 *         - Writes calculated scores to ERC-8004 ReputationRegistry after each update
 */
contract AgentRepRegistry is Ownable, ReentrancyGuard {

    // ─── Structs ──────────────────────────────────────────────────────────────

    struct AgentScore {
        uint32  score;          // 0–10000 (×100, e.g. 8732 = 87.32/100)
        uint32  totalOutcomes;
        uint32  successCount;
        uint32  disputeCount;
        bool    registered;
        uint128 registeredAt;
        uint128 updatedAt;
    }

    struct CategoryScore {
        uint32 score;
        uint32 count;
    }

    // ─── State ────────────────────────────────────────────────────────────────

    mapping(address => AgentScore)                          public agentScores;
    mapping(address => mapping(bytes32 => CategoryScore))   public categoryScores;

    address public backendSigner;
    bool    public paused;

    // ERC-8004 integration — address(0) = disabled (default on testnet)
    address public erc8004IdentityRegistry;
    address public erc8004ReputationRegistry;

    // Identifies AgentRep as data provider to ERC-8004
    bytes32 public constant ERC8004_PROVIDER_ID = keccak256("agentrep.xyz");

    // ─── Events ───────────────────────────────────────────────────────────────

    event AgentRegistered(address indexed agent, uint128 timestamp);
    event ScoreUpdated(
        address indexed agent,
        uint32  newScore,
        uint32  totalOutcomes,
        uint32  successCount,
        uint128 timestamp
    );
    event OutcomeRecorded(
        address indexed contractorAgent,
        address indexed requesterAgent,
        bytes32 indexed categoryHash,
        bool    success,
        bytes32 outcomeId,
        uint128 timestamp
    );
    event DisputeOpened(
        address indexed agent,
        bytes32 indexed outcomeId,
        uint128 timestamp
    );
    event DisputeResolved(
        bytes32 indexed outcomeId,
        bool    requesterWins,
        uint128 timestamp
    );
    event ERC8004ScoreSynced(address indexed agent, uint32 score, bool success);
    event ERC8004AddressesUpdated(address identity, address reputation);

    // ─── Modifiers ────────────────────────────────────────────────────────────

    modifier onlyBackend() {
        require(msg.sender == backendSigner, "AgentRep: caller is not backend");
        _;
    }

    modifier notPaused() {
        require(!paused, "AgentRep: contract is paused");
        _;
    }

    modifier onlyRegistered(address agent) {
        require(agentScores[agent].registered, "AgentRep: agent not registered");
        _;
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    constructor(address _backendSigner) Ownable(msg.sender) {
        require(_backendSigner != address(0), "AgentRep: zero address");
        backendSigner = _backendSigner;
        // ERC-8004 addresses start as zero (disabled) — set via setERC8004Addresses()
    }

    // ─── Admin ────────────────────────────────────────────────────────────────

    function setBackendSigner(address _signer) external onlyOwner {
        require(_signer != address(0), "AgentRep: zero address");
        backendSigner = _signer;
    }

    function setPaused(bool _paused) external onlyOwner {
        paused = _paused;
    }

    /**
     * @notice Configure ERC-8004 integration addresses.
     *         Pass address(0) to disable either integration.
     *         On testnet: leave both as zero.
     *         On mainnet: set to official ERC-8004 addresses.
     */
    function setERC8004Addresses(address _identity, address _reputation) external onlyOwner {
        erc8004IdentityRegistry  = _identity;
        erc8004ReputationRegistry = _reputation;
        emit ERC8004AddressesUpdated(_identity, _reputation);
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * @notice Register an agent in AgentRep.
     *         If ERC-8004 IdentityRegistry is configured, verifies the agent
     *         exists there (agents should register on ERC-8004 first).
     */
    function registerAgent(address agent) external onlyBackend notPaused {
        require(!agentScores[agent].registered, "AgentRep: already registered");

        // Optional: verify agent exists in ERC-8004 IdentityRegistry
        if (erc8004IdentityRegistry != address(0)) {
            try IERC8004IdentityRegistry(erc8004IdentityRegistry).isRegistered(agent)
                returns (bool exists)
            {
                // Not blocking — agents can register on AgentRep before ERC-8004 in MVP
                // This check is informational only; we log but do not revert
                if (!exists) {
                    // Future: emit warning or require ERC-8004 registration
                }
            } catch {
                // ERC-8004 call failed — proceed anyway
            }
        }

        agentScores[agent] = AgentScore({
            score:         0,
            totalOutcomes: 0,
            successCount:  0,
            disputeCount:  0,
            registered:    true,
            registeredAt:  uint128(block.timestamp),
            updatedAt:     uint128(block.timestamp)
        });
        emit AgentRegistered(agent, uint128(block.timestamp));
    }

    // ─── Score Update ─────────────────────────────────────────────────────────

    /**
     * @notice Record an outcome and update aggregated score.
     *         Called by backend after off-chain LLM Judge evaluation.
     *         Syncs the new score to ERC-8004 ReputationRegistry if configured.
     *
     * @param agent          Contractor agent wallet address
     * @param newScore       Score scaled ×100 (e.g. 8732 = 87.32/100)
     * @param totalOutcomes  Total resolved outcomes
     * @param successCount   Number of SUCCESS outcomes
     * @param categoryHash   keccak256 of category string
     * @param catScore       Category score scaled ×100
     * @param catCount       Category outcome count
     * @param outcomeId      Off-chain outcome UUID as bytes32
     * @param success        Whether this specific outcome was SUCCESS
     * @param requesterAgent Requester agent address
     */
    function registerOutcome(
        address agent,
        uint32  newScore,
        uint32  totalOutcomes,
        uint32  successCount,
        bytes32 categoryHash,
        uint32  catScore,
        uint32  catCount,
        bytes32 outcomeId,
        bool    success,
        address requesterAgent
    ) external onlyBackend notPaused onlyRegistered(agent) {
        AgentScore storage s = agentScores[agent];
        s.score         = newScore;
        s.totalOutcomes = totalOutcomes;
        s.successCount  = successCount;
        s.updatedAt     = uint128(block.timestamp);

        categoryScores[agent][categoryHash] = CategoryScore({
            score: catScore,
            count: catCount
        });

        emit ScoreUpdated(agent, newScore, totalOutcomes, successCount, uint128(block.timestamp));
        emit OutcomeRecorded(agent, requesterAgent, categoryHash, success, outcomeId, uint128(block.timestamp));

        // Sync to ERC-8004 ReputationRegistry (non-blocking)
        _syncScoreToERC8004(agent, newScore);
    }

    function recordDispute(address agent, bytes32 outcomeId)
        external onlyBackend notPaused onlyRegistered(agent)
    {
        agentScores[agent].disputeCount++;
        emit DisputeOpened(agent, outcomeId, uint128(block.timestamp));
    }

    function recordDisputeResolution(bytes32 outcomeId, bool requesterWins)
        external onlyBackend notPaused
    {
        emit DisputeResolved(outcomeId, requesterWins, uint128(block.timestamp));
    }

    // ─── ERC-8004 Sync ────────────────────────────────────────────────────────

    /**
     * @notice Manually trigger a score sync to ERC-8004 for a specific agent.
     *         Useful for backfilling after enabling ERC-8004 integration.
     */
    function syncToERC8004(address agent) external onlyBackend onlyRegistered(agent) {
        _syncScoreToERC8004(agent, agentScores[agent].score);
    }

    /**
     * @dev Internal — pushes score to ERC-8004 ReputationRegistry.
     *      Non-blocking: emits ERC8004ScoreSynced(success=false) on failure,
     *      never reverts the parent transaction.
     */
    function _syncScoreToERC8004(address agent, uint32 score) internal {
        if (erc8004ReputationRegistry == address(0)) return;
        try IERC8004ReputationRegistry(erc8004ReputationRegistry)
            .updateScore(agent, uint256(score), ERC8004_PROVIDER_ID)
        {
            emit ERC8004ScoreSynced(agent, score, true);
        } catch {
            emit ERC8004ScoreSynced(agent, score, false);
        }
    }

    // ─── Views ────────────────────────────────────────────────────────────────

    function getScore(address agent) external view returns (AgentScore memory) {
        return agentScores[agent];
    }

    function getCategoryScore(address agent, bytes32 categoryHash)
        external view returns (CategoryScore memory)
    {
        return categoryScores[agent][categoryHash];
    }

    function getCategoryScoreByName(address agent, string calldata category)
        external view returns (CategoryScore memory)
    {
        return categoryScores[agent][keccak256(abi.encodePacked(category))];
    }

    function isRegistered(address agent) external view returns (bool) {
        return agentScores[agent].registered;
    }
}
