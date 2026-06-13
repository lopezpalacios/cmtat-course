# Chapter 07 — Compliance Modules: Pause, Freeze, Transfer Rules `[shared] [BANK-heavy]`

**Track:** shared  
**Emphasis threads:** `[BANK]` `[TYPES]`  
**Chapter learning objective:** Implement CMTAT compliance modules including PauseModule, EnforcementModule, and ValidationModule. Understand how to integrate these modules into a token contract for regulatory compliance.  
**Prerequisites:** Chapters 01-06  
**You will build:** `contracts/shared/ComplianceToken.sol`, `contracts/shared/WhitelistRuleEngine.sol`, and `java-adapters/ComplianceMonitor.java`

In the world of core banking, ensuring compliance with regulations is paramount. Just as a bank must adhere to strict rules regarding transactions, transfers, and account management, blockchain-based token systems need robust mechanisms to enforce similar rules. This chapter will guide you through implementing three critical compliance modules: PauseModule, EnforcementModule, and ValidationModule. These modules will help manage market-wide halts, address freezes for sanctions or court orders, and transfer validations based on a whitelist rule engine.

## Lesson 1 — Implementing the PauseModule

**Learning objective:** Understand how to implement a module that can pause all token transfers globally.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, there are times when transactions need to be halted due to market conditions or regulatory requirements. Similarly, in blockchain-based token systems, it's essential to have the ability to pause all token transfers globally. This lesson will guide you through implementing the PauseModule.

### Step 1.1 — Define the PauseModule contract

**Instruction:** Create a new file `PauseModule.sol` and define the basic structure of the PauseModule contract.

**Explanation:** Just as a bank might have a system to halt all transactions during market volatility, we need a way to pause all token transfers in our blockchain-based system. This step involves defining the basic structure of the PauseModule contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract PauseModule {
    // Define state variables and events here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract PauseModule {
    bool public isPaused;
    
    event Paused();
    event Unpaused();

    function pause() external {
        require(!isPaused, "Already paused");
        isPaused = true;
        emit Paused();
    }

    function unpause() external {
        require(isPaused, "Not paused");
        isPaused = false;
        emit Unpaused();
    }
}
```

**Validation rule:** The PauseModule contract should have the `isPaused` state variable, `Paused` and `Unpaused` events, and `pause` and `unpause` functions.

```checker
{"id": "ch07-l1-s1", "type": "regex", "pattern": "bool\\s+public\\s+isPaused;", "flags": "", "target": "solidity", "error_hint": "Ensure the PauseModule contract has the required state variables, events, and functions."}
```

## Lesson 2 — Implementing the EnforcementModule

**Learning objective:** Understand how to implement a module that can freeze specific addresses for regulatory actions.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, there are times when certain accounts need to be frozen due to sanctions or court orders. Similarly, in blockchain-based token systems, it's essential to have the ability to freeze specific addresses for regulatory compliance. This lesson will guide you through implementing the EnforcementModule.

### Step 2.1 — Define the EnforcementModule contract

**Instruction:** Create a new file `EnforcementModule.sol` and define the basic structure of the EnforcementModule contract.

**Explanation:** Just as a bank might have a system to freeze certain accounts, we need a way to freeze specific addresses in our blockchain-based system. This step involves defining the basic structure of the EnforcementModule contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EnforcementModule {
    // Define state variables and events here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract EnforcementModule {
    mapping(address => bool) public frozenAddresses;
    
    event AddressFrozen(address indexed addr);
    event AddressUnfrozen(address indexed addr);

    function freezeAddress(address _addr) external {
        require(!frozenAddresses[_addr], "Address already frozen");
        frozenAddresses[_addr] = true;
        emit AddressFrozen(_addr);
    }

    function unfreezeAddress(address _addr) external {
        require(frozenAddresses[_addr], "Address not frozen");
        frozenAddresses[_addr] = false;
        emit AddressUnfrozen(_addr);
    }
}
```

**Validation rule:** The EnforcementModule contract should have the `frozenAddresses` mapping, `AddressFrozen` and `AddressUnfrozen` events, and `freezeAddress` and `unfreezeAddress` functions.

```checker
{
  "id": "ch07-l2-s1",
  "type": "regex",
  "pattern": "mapping\\(address\\s+=>\\s+bool\\)\\s+public\\s+frozenAddresses;[\\s\\S]*event\\s+AddressFrozen\\(address\\s+indexed\\s+addr\\);[\\s\\S]*event\\s+AddressUnfrozen\\(address\\s+indexed\\s+addr\\);[\\s\\S]*function\\s+freezeAddress\\(address\\s+_addr\\)\\s*external[\\s\\S]*function\\s+unfreezeAddress\\(address\\s+_addr\\)\\s*external",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Ensure the EnforcementModule contract has the required state variables, events, and functions."
}
```

## Lesson 3 — Implementing the ValidationModule

**Learning objective:** Understand how to implement a module that validates transfers based on a whitelist rule engine.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, there are times when certain transactions need to be validated against a whitelist of allowed accounts or addresses. Similarly, in blockchain-based token systems, it's essential to have the ability to validate transfers based on a whitelist rule engine. This lesson will guide you through implementing the ValidationModule.

### Step 3.1 — Define the WhitelistRuleEngine contract

**Instruction:** Create a new file `WhitelistRuleEngine.sol` and define the basic structure of the WhitelistRuleEngine contract.

**Explanation:** Just as a bank might have a system to validate transactions against a whitelist, we need a way to validate transfers in our blockchain-based system. This step involves defining the basic structure of the WhitelistRuleEngine contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract WhitelistRuleEngine {
    // Define state variables and events here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract WhitelistRuleEngine {
    mapping(address => bool) public whitelist;
    
    event AddressWhitelisted(address indexed addr);
    event AddressRemovedFromWhitelist(address indexed addr);

    function addToWhitelist(address _addr) external {
        require(!whitelist[_addr], "Address already whitelisted");
        whitelist[_addr] = true;
        emit AddressWhitelisted(_addr);
    }

    function removeFromWhitelist(address _addr) external {
        require(whitelist[_addr], "Address not whitelisted");
        whitelist[_addr] = false;
        emit AddressRemovedFromWhitelist(_addr);
    }
}
```

**Validation rule:** The WhitelistRuleEngine contract should have the `whitelist` mapping, `AddressWhitelisted` and `AddressRemovedFromWhitelist` events, and `addToWhitelist` and `removeFromWhitelist` functions.

```checker
{
  "id": "ch07-l3-s1",
  "type": "regex",
  "pattern": "mapping\\(address\\s+=>\\s+bool\\)\\s+public\\s+whitelist;[\\s\\S]*event\\s+AddressWhitelisted\\(address\\s+indexed\\s+addr\\);[\\s\\S]*event\\s+AddressRemovedFromWhitelist\\(address\\s+indexed\\s+addr\\);[\\s\\S]*function\\s+addToWhitelist\\(address\\s+_addr\\)\\s*external[\\s\\S]*function\\s+removeFromWhitelist\\(address\\s+_addr\\)\\s*external",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Ensure the WhitelistRuleEngine contract has the required state variables, events, and functions."
}
```

## Lesson 4 — Integrating Compliance Modules into ComplianceToken

**Learning objective:** Understand how to integrate the PauseModule, EnforcementModule, and ValidationModule into a single token contract.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, different modules work together to ensure compliance with regulations. Similarly, in blockchain-based token systems, it's essential to integrate different compliance modules into a single token contract. This lesson will guide you through integrating the PauseModule, EnforcementModule, and ValidationModule into the ComplianceToken contract.

### Step 4.1 — Define the ComplianceToken contract

**Instruction:** Create a new file `ComplianceToken.sol` and define the basic structure of the ComplianceToken contract.

**Explanation:** Just as a bank might have different modules working together to ensure compliance, we need to integrate different compliance modules into our blockchain-based system. This step involves defining the basic structure of the ComplianceToken contract.

**Starter code:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./PauseModule.sol";
import "./EnforcementModule.sol";
import "./WhitelistRuleEngine.sol";

contract ComplianceToken is PauseModule, EnforcementModule, WhitelistRuleEngine {
    // Define state variables and events here
}
```

**Solution:**
```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./PauseModule.sol";
import "./EnforcementModule.sol";
import "./WhitelistRuleEngine.sol";

contract ComplianceToken is PauseModule, EnforcementModule, WhitelistRuleEngine {
    string public name;
    string public symbol;
    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;

    event Transfer(address indexed from, address indexed to, uint256 value);

    constructor(string memory _name, string memory _symbol, uint256 _initialSupply) {
        name = _name;
        symbol = _symbol;
        totalSupply = _initialSupply;
        balanceOf[msg.sender] = _initialSupply;
        emit Transfer(address(0), msg.sender, _initialSupply);
    }

    function transfer(address _to, uint256 _value) external returns (bool success) {
        require(!isPaused, "Transfers are paused");
        require(!frozenAddresses[msg.sender], "Sender address is frozen");
        require(whitelist[_to], "Recipient address is not whitelisted");
        require(balanceOf[msg.sender] >= _value, "Insufficient balance");

        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;
        emit Transfer(msg.sender, _to, _value);
        return true;
    }
}
```

**Validation rule:** The ComplianceToken contract should have the `name`, `symbol`, and `totalSupply` state variables, `Transfer` event, constructor, and `transfer` function.

```checker
{
  "id": "ch07-l4-s1",
  "type": "regex",
  "pattern": "string\\s+public\\s+name;[\\s\\S]*string\\s+public\\s+symbol;[\\s\\S]*uint256\\s+public\\s+totalSupply;[\\s\\S]*mapping\\(address\\s+=>\\s+uint256\\)\\s+public\\s+balanceOf;[\\s\\S]*event\\s+Transfer\\(address\\s+indexed\\s+from,\\s+address\\s+indexed\\s+to,\\s+uint256\\s+value\\);[\\s\\S]*constructor\\([\\s\\S]*function\\s+transfer\\(address\\s+_to,\\s+uint256\\s+_value\\)\\s*external",
  "flags": "m",
  "target": "solidity",
  "error_hint": "Ensure the ComplianceToken contract has the required state variables, events, and functions."
}
```

## Lesson 5 — Java Adapter for Compliance Monitoring

**Learning objective:** Understand how to create a Java adapter that monitors compliance actions.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, monitoring tools are essential to track and verify compliance actions. Similarly, in blockchain-based token systems, it's essential to have a Java adapter that can monitor compliance actions. This lesson will guide you through creating the ComplianceMonitor.java adapter.

### Step 5.1 — Define the ComplianceMonitor class

**Instruction:** Create a new file `ComplianceMonitor.java` and define the basic structure of the ComplianceMonitor class.

**Explanation:** Just as a bank might have monitoring tools to track compliance actions, we need a Java adapter that can monitor compliance actions in our blockchain-based system. This step involves defining the basic structure of the ComplianceMonitor class.

**Starter code:**
```java
package com.example;

public class ComplianceMonitor {
    // Define methods here
}
```

**Solution:**
```java
package com.example;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import java.math.BigInteger;

public class ComplianceMonitor {
    private Web3j web3j;

    public ComplianceMonitor(String nodeUrl) {
        this.web3j = Web3j.build(new HttpService(nodeUrl));
    }

    public TransactionReceipt monitorPauseEvent() throws Exception {
        // Implement monitoring for Pause event
        return null;
    }

    public TransactionReceipt monitorUnpauseEvent() throws Exception {
        // Implement monitoring for Unpause event
        return null;
    }

    public TransactionReceipt monitorFreezeAddressEvent() throws Exception {
        // Implement monitoring for AddressFrozen event
        return null;
    }

    public TransactionReceipt monitorUnfreezeAddressEvent() throws Exception {
        // Implement monitoring for AddressUnfrozen event
        return null;
    }
}
```

**Validation rule:** The ComplianceMonitor class should have the `monitorPauseEvent`, `monitorUnpauseEvent`, `monitorFreezeAddressEvent`, and `monitorUnfreezeAddressEvent` methods.

```checker
{
  "id": "ch07-l5-s1",
  "type": "regex",
  "pattern": "public\\s+TransactionReceipt\\s+monitorPauseEvent\\(\\)\\s*throws\\s+Exception[\\s\\S]*public\\s+TransactionReceipt\\s+monitorUnpauseEvent\\(\\)\\s*throws\\s+Exception[\\s\\S]*public\\s+TransactionReceipt\\s+monitorFreezeAddressEvent\\(\\)\\s*throws\\s+Exception[\\s\\S]*public\\s+TransactionReceipt\\s+monitorUnfreezeAddressEvent\\(\\)\\s*throws\\s+Exception",
  "flags": "m",
  "target": "java",
  "error_hint": "Ensure the ComplianceMonitor class has the required methods."
}
```

## Lesson 6 — Testing and Deployment

**Learning objective:** Understand how to test and deploy the compliance modules.  
**Emphasis tags:** `[BANK]` `[TYPES]`  
**Track:** shared

In core banking systems, testing and deployment are critical steps to ensure that all components work together seamlessly. Similarly, in blockchain-based token systems, it's essential to have a robust testing and deployment process for the compliance modules. This lesson will guide you through testing and deploying the PauseModule, EnforcementModule, ValidationModule, and ComplianceToken contracts.

### Step 6.1 — Test the Compliance Modules

**Instruction:** Write tests for the PauseModule, EnforcementModule, and WhitelistRuleEngine contracts using a testing framework like Truffle or Hardhat.

**Explanation:** Just as a bank might have rigorous testing processes to ensure that all components work together seamlessly, we need a robust testing process for our blockchain-based system. This step involves writing tests for the PauseModule, EnforcementModule, and WhitelistRuleEngine contracts.

**Starter code:**
```javascript
// Write your tests here using Truffle or Hardhat
```

**Solution:**
```javascript
const { expect } = require("chai");
const { ethers } = require("hardhat");

describe("PauseModule", function () {
    it("should pause and unpause transfers", async function () {
        const PauseModule = await ethers.getContractFactory("PauseModule");
        const pauseModule = await PauseModule.deploy();
        await pauseModule.pause();
        expect(await pauseModule.isPaused()).to.be.true;
        await pauseModule.unpause();
        expect(await pauseModule.isPaused()).to.be.false;
    });
});

describe("EnforcementModule", function () {
    it("should freeze and unfreeze addresses", async function () {
        const EnforcementModule = await ethers.getContractFactory("EnforcementModule");
        const enforcementModule = await EnforcementModule.deploy();
        await enforcementModule.freezeAddress(accounts[1].address);
        expect(await enforcementModule.frozenAddresses(accounts[1].address)).to.be.true;
        await enforcementModule.unfreezeAddress(accounts[1].address);
        expect(await enforcementModule.frozenAddresses(accounts[1].address)).to.be.false;
    });
});

describe("WhitelistRuleEngine", function () {
    it("should add and remove addresses from whitelist", async function () {
        const WhitelistRuleEngine = await ethers.getContractFactory("WhitelistRuleEngine");
        const whitelistRuleEngine = await WhitelistRuleEngine.deploy();
        await whitelistRuleEngine.addToWhitelist(accounts[1].address);
        expect(await whitelistRuleEngine.whitelist(accounts[1].address)).to.be.true;
        await whitelistRuleEngine.removeFromWhitelist(accounts[1].address);
        expect(await whitelistRuleEngine.whitelist(accounts[1].address)).to.be.false;
    });
});
```

**Validation rule:** The tests should cover the basic functionality of the PauseModule, EnforcementModule, and WhitelistRuleEngine contracts.

```checker
{
  "id": "ch07-l6-s1",
  "type": "compile",
  "pattern": "",
  "flags": "",
  "target": "solidity",
  "error_hint": "Ensure all tests compile successfully."
}
```

### Step 6.2 — Deploy the ComplianceToken contract

**Instruction:** Deploy the ComplianceToken contract to a test network using a deployment script.

**Explanation:** Just as a bank might deploy new systems to a test environment before going live, we need to deploy our blockchain-based system to a test network. This step involves deploying the ComplianceToken contract to a test network.

**Starter code:**
```javascript
// Write your deployment script here using Truffle or Hardhat
```

**Solution:**
```javascript
const { ethers } = require("hardhat");

async function main() {
    const [deployer] = await ethers.getSigners();
    console.log("Deploying contracts with the account:", deployer.address);

    const ComplianceToken = await ethers.getContractFactory("ComplianceToken");
    const complianceToken = await ComplianceToken.deploy("MyToken", "MTK", 1000);
    await complianceToken.deployed();

    console.log("ComplianceToken deployed to:", complianceToken.address);
}

main()
    .then(() => process.exit(0))
    .catch((error) => {
        console.error(error);
        process.exit(1);
    });
```

**Validation rule:** The ComplianceToken contract should be successfully deployed to a test network.

```checker
{
  "id": "ch07-l6-s2",
  "type": "compile",
  "pattern": "",
  "flags": "",
  "target": "solidity",
  "error_hint": "Ensure the deployment script compiles successfully."
}
```

## Assembled Contract

Here is the assembled contract for this chapter:

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

contract PauseModule {
    bool public isPaused;
    
    event Paused();
    event Unpaused();

    function pause() external {
        require(!isPaused, "Already paused");
        isPaused = true;
        emit Paused();
    }

    function unpause() external {
        require(isPaused, "Not paused");
        isPaused = false;
        emit Unpaused();
    }
}

contract EnforcementModule {
    mapping(address => bool) public frozenAddresses;
    
    event AddressFrozen(address indexed addr);
    event AddressUnfrozen(address indexed addr);

    function freezeAddress(address _addr) external {
        require(!frozenAddresses[_addr], "Address already frozen");
        frozenAddresses[_addr] = true;
        emit AddressFrozen(_addr);
    }

    function unfreezeAddress(address _addr) external {
        require(frozenAddresses[_addr], "Address not frozen");
        frozenAddresses[_addr] = false;
        emit AddressUnfrozen(_addr);
    }
}

contract WhitelistRuleEngine {
    mapping(address => bool) public whitelist;
    
    event AddressWhitelisted(address indexed addr);
    event AddressRemovedFromWhitelist(address indexed addr);

    function addToWhitelist(address _addr) external {
        require(!whitelist[_addr], "Address already whitelisted");
        whitelist[_addr] = true;
        emit AddressWhitelisted(_addr);
    }

    function removeFromWhitelist(address _addr) external {
        require(whitelist[_addr], "Address not whitelisted");
        whitelist[_addr] = false;
        emit AddressRemovedFromWhitelist(_addr);
    }
}

contract ComplianceToken is PauseModule, EnforcementModule, WhitelistRuleEngine {
    string public name;
    string public symbol;
    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;

    event Transfer(address indexed from, address indexed to, uint256 value);

    constructor(string memory _name, string memory _symbol, uint256 _initialSupply) {
        name = _name;
        symbol = _symbol;
        totalSupply = _initialSupply;
        balanceOf[msg.sender] = _initialSupply;
        emit Transfer(address(0), msg.sender, _initialSupply);
    }

    function transfer(address _to, uint256 _value) external returns (bool success) {
        require(!isPaused, "Transfers are paused");
        require(!frozenAddresses[msg.sender], "Sender address is frozen");
        require(whitelist[_to], "Recipient address is not whitelisted");
        require(balanceOf[msg.sender] >= _value, "Insufficient balance");

        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;
        emit Transfer(msg.sender, _to, _value);
        return true;
    }
}
```

## Quiz

**Q1 (multiple choice).** Which of the following best describes the purpose of the PauseModule in a CMTAT token contract?
a) To enforce transfer rules — b) To pause all token transfers temporarily — c) To validate transaction data — d) To monitor compliance with regulatory standards
**Answer: b.** The PauseModule is designed to temporarily halt all token transfers, allowing for maintenance or emergency situations.

**Q2 (multiple choice).** When integrating the ValidationModule into a ComplianceToken contract, what is its primary function?
a) To pause all token transfers — b) To enforce transfer rules — c) To validate transaction data against predefined criteria — d) To monitor compliance with regulatory standards
**Answer: c.** The ValidationModule checks that each transaction complies with specific criteria before allowing the transfer to proceed.

**Q3 (multiple choice).** In the context of a Swiss core-banking system, why is it important to implement a Java Adapter for Compliance Monitoring?
a) To integrate blockchain compliance checks into existing banking systems — b) To pause all token transfers temporarily — c) To validate transaction data against predefined criteria — d) To monitor compliance with regulatory standards
**Answer: a.** The Java Adapter allows Swiss core-banking systems, which are typically built on Java or .NET frameworks, to interact seamlessly with blockchain-based compliance checks.

**Q4 (short answer).** Explain how the EnforcementModule contributes to the overall security of a token contract.
**Answer:** The EnforcementModule enforces strict rules and conditions for token transfers, ensuring that all transactions adhere to regulatory requirements. This helps prevent unauthorized or non-compliant activities, thereby enhancing the security and integrity of the token contract.

**Q5 (short answer).** Describe how you would test the PauseModule in a ComplianceToken contract.
**Answer:** To test the PauseModule, you would first deploy the token contract with the module integrated. Then, trigger the pause function to halt all transfers and verify that no transactions can be processed during this period. After confirming the pause functionality, lift the pause and ensure that transfers resume as expected.
