import { ethers } from "hardhat";

const CONTRACT_ADDRESS = "0xEf722bf0F3178F366C25A27b849a928BFC4cdBA5";

// ERC-8004 addresses on Base Mainnet
const ERC8004_IDENTITY_REGISTRY    = "0x8004A169FB4a3325136EB29fA0ceB6D2e539a432";
const ERC8004_REPUTATION_REGISTRY  = "0x8004BAa17C55a88189AE136b182e5fdA19dE9b63";

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deployer:", deployer.address);

  const registry = await ethers.getContractAt("AgentRepRegistry", CONTRACT_ADDRESS);

  // Check current state
  const currentIdentity    = await registry.erc8004IdentityRegistry();
  const currentReputation  = await registry.erc8004ReputationRegistry();
  console.log("Current erc8004IdentityRegistry:   ", currentIdentity);
  console.log("Current erc8004ReputationRegistry: ", currentReputation);

  if (
    currentIdentity.toLowerCase()   === ERC8004_IDENTITY_REGISTRY.toLowerCase() &&
    currentReputation.toLowerCase() === ERC8004_REPUTATION_REGISTRY.toLowerCase()
  ) {
    console.log("Already configured. Nothing to do.");
    return;
  }

  console.log("\nSetting ERC-8004 addresses...");
  const tx = await registry.setERC8004Addresses(
    ERC8004_IDENTITY_REGISTRY,
    ERC8004_REPUTATION_REGISTRY
  );
  console.log("Tx hash:", tx.hash);
  await tx.wait();
  console.log("Done. ERC-8004 sync enabled on Base Mainnet.");
}

main().catch((e) => { console.error(e); process.exit(1); });
