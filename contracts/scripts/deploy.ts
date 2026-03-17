import { ethers } from "hardhat";

async function main() {
  const [deployer] = await ethers.getSigners();
  console.log("Deploying AgentRepRegistry with account:", deployer.address);

  const backendSigner = process.env.BACKEND_SIGNER_ADDRESS ?? deployer.address;
  console.log("Backend signer:", backendSigner);

  const AgentRepRegistry = await ethers.getContractFactory("AgentRepRegistry");
  const registry = await AgentRepRegistry.deploy(backendSigner);
  await registry.waitForDeployment();

  const address = await registry.getAddress();
  console.log("AgentRepRegistry deployed to:", address);
  console.log("Set CONTRACT_ADDRESS=" + address + " in backend .env");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
