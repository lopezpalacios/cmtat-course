// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/*//////////////////////////////////////////////////////////////////////////
    Chapter 02 — Solidity Datatypes for Banking Integrators
    InstrumentTypes.sol — self-contained, zero imports.

    Three top-level units:
      * InstrumentMoneyMath   — fixed-point money helpers (modeled on
                                OpenZeppelin Math.mulDiv / Rounding)
      * InstrumentIsinCodec   — bytes32 <-> ASCII codec for ISIN / LEI
      * InstrumentTypes       — instrument registry + position-keeping demo
//////////////////////////////////////////////////////////////////////////*/

/// @title InstrumentMoneyMath
/// @notice Integer money math on smallest-unit (minor-unit) amounts.
/// @dev modeled on OpenZeppelin Math (mulDiv + Rounding enum), re-implemented
///      inline so this file has zero imports. All amounts are uint256 integers
///      in the instrument's smallest unit (e.g. rappen for CHF-2-decimals).
library InstrumentMoneyMath {
    /// Rounding policy must be EXPLICIT in money code. Floor = truncate
    /// toward zero, Ceil = round up if any remainder, HalfUp = commercial
    /// rounding (>= .5 rounds up). True banker's rounding (half-to-even)
    /// needs a parity check on the quotient — see HalfUp caveat in Ch. 02.
    enum Rounding {
        Floor,
        Ceil,
        HalfUp
    }

    /// 1 basis point = 0.01% => denominator 10_000.
    uint256 internal constant BPS_DENOMINATOR = 10_000;

    error MoneyMathDivByZero();

    /// @notice result = amount * numerator / denominator with explicit rounding.
    /// @dev Solidity ^0.8 reverts on overflow of `amount * numerator`
    ///      (checked arithmetic by default — no SafeMath needed).
    function mulDiv(
        uint256 amount,
        uint256 numerator,
        uint256 denominator,
        Rounding mode
    ) internal pure returns (uint256 result) {
        if (denominator == 0) revert MoneyMathDivByZero();
        uint256 product = amount * numerator; // reverts on overflow (^0.8 checked math)
        result = product / denominator;       // integer division truncates (Floor)
        uint256 remainder = product % denominator;
        if (remainder == 0) {
            return result;
        }
        if (mode == Rounding.Ceil) {
            result += 1;
        } else if (mode == Rounding.HalfUp && remainder * 2 >= denominator) {
            result += 1;
        }
        // Rounding.Floor: keep truncated result.
    }

    /// @notice Apply a rate quoted in basis points to a minor-unit amount.
    ///         e.g. applyRateBps(1_000_000, 250, HalfUp) = 2.50% of 10'000.00 CHF.
    function applyRateBps(
        uint256 amountMinor,
        uint256 rateBps,
        Rounding mode
    ) internal pure returns (uint256) {
        return mulDiv(amountMinor, rateBps, BPS_DENOMINATOR, mode);
    }

    /// @notice Scale a major-unit amount (whole CHF) into minor units.
    /// @dev Reverts on overflow of the power or the multiplication.
    function toMinorUnits(uint256 majorUnits, uint8 decimals_)
        internal
        pure
        returns (uint256)
    {
        return majorUnits * (10 ** uint256(decimals_));
    }
}

/// @title InstrumentIsinCodec
/// @notice Right-padded ASCII codec: fixed-length identifiers (ISIN = 12
///         chars, LEI = 20 chars) packed into a single bytes32 storage slot.
/// @dev modeled on the bytes32-identifier convention used by CMTAT's
///      BaseModule metadata fields (tokenId / terms references).
library InstrumentIsinCodec {
    uint256 internal constant ISIN_LENGTH = 12;
    uint256 internal constant LEI_LENGTH = 20;

    error IdentifierLengthInvalid(uint256 actual, uint256 expected);
    error IdentifierCharInvalid(bytes1 ch);

    /// @notice Encode an ASCII string of exactly `expectedLength` chars into
    ///         bytes32, left-aligned (right-padded with 0x00).
    /// @dev Only digits 0-9 and uppercase A-Z are accepted — the legal
    ///      alphabet of ISIN and LEI.
    function encodeAscii(string memory value, uint256 expectedLength)
        internal
        pure
        returns (bytes32 out)
    {
        bytes memory raw = bytes(value);
        if (raw.length != expectedLength) {
            revert IdentifierLengthInvalid(raw.length, expectedLength);
        }
        for (uint256 i = 0; i < raw.length; i++) {
            bytes1 ch = raw[i];
            bool isDigit = (ch >= 0x30 && ch <= 0x39); // '0'..'9'
            bool isUpper = (ch >= 0x41 && ch <= 0x5A); // 'A'..'Z'
            if (!isDigit && !isUpper) revert IdentifierCharInvalid(ch);
            // bytes1 -> bytes32 left-aligns the byte; shift right i*8 bits
            // to place it at byte position i, then OR into the accumulator.
            out |= bytes32(ch) >> (i * 8);
        }
    }

    function encodeIsin(string memory isin) internal pure returns (bytes32) {
        return encodeAscii(isin, ISIN_LENGTH);
    }

    function encodeLei(string memory lei) internal pure returns (bytes32) {
        return encodeAscii(lei, LEI_LENGTH);
    }

    /// @notice Decode a right-padded-ASCII bytes32 back into a string,
    ///         stopping at the first 0x00 pad byte.
    function decodeAscii(bytes32 encoded) internal pure returns (string memory) {
        uint256 len = 0;
        while (len < 32 && encoded[len] != 0x00) {
            len++;
        }
        bytes memory out = new bytes(len);
        for (uint256 i = 0; i < len; i++) {
            out[i] = encoded[i];
        }
        return string(out);
    }
}

/// @title InstrumentTypes
/// @notice Instrument master-data registry + minimal position-keeping table.
///         Demonstrates every Solidity type that crosses the bank boundary:
///         uint256 minor-unit money, bytes32 identifiers, address validation,
///         enum lifecycle, struct metadata, mapping registers.
/// @dev Registry/registrar pattern modeled on CMTAT BaseModule metadata +
///      AuthorizationModule's role gating (simplified to a per-instrument
///      registrar address for this chapter; full RBAC arrives in Chapter 05).
contract InstrumentTypes {
    using InstrumentMoneyMath for uint256;

    // ----------------------------------------------------------------- types

    /// Lifecycle states of a tokenized instrument. Order matters: the enum's
    /// underlying uint8 value is what events and web3j decoding will carry.
    enum InstrumentLifecycle {
        Draft,      // 0 — master data captured, not yet issued
        Issued,     // 1 — issuance executed, not yet trading
        Active,     // 2 — freely transferable (subject to rules)
        Suspended,  // 3 — regulatory halt (compare CMTAT PauseModule, Ch. 07)
        Matured,    // 4 — past maturity, awaiting redemption
        Redeemed    // 5 — terminal state
    }

    /// Instrument master data. Mixed static + dynamic fields, so the ABI
    /// encoding of this struct is DYNAMIC (web3j: DynamicStruct).
    struct InstrumentMetadata {
        bytes32 isin;                 // right-padded ASCII, 12 chars used
        bytes32 lei;                  // issuer LEI, right-padded ASCII, 20 chars used
        string name;                  // human-readable, e.g. "Helvetia 2.5% 2031"
        uint8 tokenDecimals;          // monetary scale, e.g. 2 for CHF/rappen
        uint256 nominalValueMinor;    // nominal per unit, in minor units
        uint64 issueDate;             // unix epoch seconds (fits until year 584e9)
        InstrumentLifecycle state;    // current lifecycle state
        address registrar;            // operator allowed to mutate this record
    }

    // ---------------------------------------------------------------- errors

    error InstrumentZeroAddress();
    error InstrumentAlreadyRegistered(bytes32 instrumentId);
    error InstrumentUnknown(bytes32 instrumentId);
    error InstrumentNotRegistrar(address caller, bytes32 instrumentId);
    error InstrumentBadStateTransition(
        InstrumentLifecycle fromState,
        InstrumentLifecycle toState
    );
    error InstrumentZeroAmount();

    // ---------------------------------------------------------------- events

    /// Every state change emits an event — events are the integration
    /// contract the bank's Java listeners consume (Chapter 03 deepens this).
    event InstrumentRegistered(
        bytes32 indexed instrumentId,
        bytes32 indexed isin,
        address indexed registrar,
        uint8 tokenDecimals,
        uint256 nominalValueMinor
    );

    event LifecycleChanged(
        bytes32 indexed instrumentId,
        InstrumentLifecycle indexed fromState,
        InstrumentLifecycle indexed toState,
        address operator
    );

    /// bookingRef = idempotency key supplied by the core-banking adapter so
    /// off-chain reconciliation can match this log to a booking entry.
    event PositionBooked(
        bytes32 indexed instrumentId,
        address indexed holder,
        bytes32 indexed bookingRef,
        uint256 amountMinor,
        uint256 newPositionMinor
    );

    // --------------------------------------------------------------- storage

    /// Registry: instrumentId => master data. The on-chain "instrument table".
    mapping(bytes32 => InstrumentMetadata) private _instruments;

    /// Position-keeping: instrumentId => holder => position in minor units.
    /// This nested mapping IS the securities register, like a composite-key
    /// (instrument_id, holder_id) positions table in core banking.
    mapping(bytes32 => mapping(address => uint256)) private _positions;

    /// Mappings are not iterable — keep an index array for enumeration.
    bytes32[] private _instrumentIds;

    // ------------------------------------------------------------- modifiers

    modifier notZeroAddress(address account) {
        if (account == address(0)) revert InstrumentZeroAddress();
        _;
    }

    modifier onlyRegistrar(bytes32 instrumentId_) {
        if (_instruments[instrumentId_].registrar == address(0)) {
            revert InstrumentUnknown(instrumentId_);
        }
        if (_instruments[instrumentId_].registrar != msg.sender) {
            revert InstrumentNotRegistrar(msg.sender, instrumentId_);
        }
        _;
    }

    // ------------------------------------------------------- id construction

    /// @notice Deterministic instrument id from (ISIN, LEI).
    /// @dev abi.encode (NOT encodePacked) — both args are fixed 32-byte
    ///      values here, but abi.encode is the collision-safe habit.
    function computeInstrumentId(bytes32 isin, bytes32 lei)
        public
        pure
        returns (bytes32)
    {
        return keccak256(abi.encode(isin, lei));
    }

    // ----------------------------------------------------------- registration

    /// @notice Register instrument master data. Caller becomes registrar.
    /// @param isinStr  12-char ISIN, e.g. "CH0012345678"
    /// @param leiStr   20-char LEI of the issuer
    function registerInstrument(
        string calldata isinStr,
        string calldata leiStr,
        string calldata name,
        uint8 tokenDecimals,
        uint256 nominalValueMinor,
        uint64 issueDate
    ) external returns (bytes32 instrumentId_) {
        bytes32 isin = InstrumentIsinCodec.encodeIsin(isinStr);
        bytes32 lei = InstrumentIsinCodec.encodeLei(leiStr);
        instrumentId_ = computeInstrumentId(isin, lei);

        if (_instruments[instrumentId_].registrar != address(0)) {
            revert InstrumentAlreadyRegistered(instrumentId_);
        }

        _instruments[instrumentId_] = InstrumentMetadata({
            isin: isin,
            lei: lei,
            name: name,
            tokenDecimals: tokenDecimals,
            nominalValueMinor: nominalValueMinor,
            issueDate: issueDate,
            state: InstrumentLifecycle.Draft,
            registrar: msg.sender
        });
        _instrumentIds.push(instrumentId_);

        emit InstrumentRegistered(
            instrumentId_,
            isin,
            msg.sender,
            tokenDecimals,
            nominalValueMinor
        );
    }

    // -------------------------------------------------------------- lifecycle

    /// @notice Move an instrument to a new lifecycle state.
    /// @dev Transition rules: never back to Draft, never out of Redeemed,
    ///      and Suspended may only return to Active. Everything else must
    ///      move strictly forward in enum order.
    function setLifecycle(bytes32 instrumentId_, InstrumentLifecycle toState)
        external
        onlyRegistrar(instrumentId_)
    {
        InstrumentLifecycle fromState = _instruments[instrumentId_].state;

        bool allowed;
        if (fromState == InstrumentLifecycle.Redeemed) {
            allowed = false; // terminal
        } else if (toState == InstrumentLifecycle.Draft) {
            allowed = false; // no resurrection of drafts
        } else if (fromState == InstrumentLifecycle.Suspended) {
            allowed = (toState == InstrumentLifecycle.Active);
        } else {
            // forward-only, or a jump into Suspended from any live state
            allowed = (uint8(toState) > uint8(fromState));
        }
        if (!allowed) revert InstrumentBadStateTransition(fromState, toState);

        _instruments[instrumentId_].state = toState;
        emit LifecycleChanged(instrumentId_, fromState, toState, msg.sender);
    }

    // --------------------------------------------------------------- booking

    /// @notice Credit `amountMinor` units to `holder`'s position.
    /// @param bookingRef idempotency key from the bank-side adapter
    ///        (e.g. keccak256 of the core-banking booking id).
    function bookPosition(
        bytes32 instrumentId_,
        address holder,
        uint256 amountMinor,
        bytes32 bookingRef
    ) external onlyRegistrar(instrumentId_) notZeroAddress(holder) {
        if (amountMinor == 0) revert InstrumentZeroAmount();

        // Checked addition: reverts on overflow instead of wrapping (^0.8).
        uint256 newPosition = _positions[instrumentId_][holder] + amountMinor;
        _positions[instrumentId_][holder] = newPosition;

        emit PositionBooked(
            instrumentId_,
            holder,
            bookingRef,
            amountMinor,
            newPosition
        );
    }

    // ----------------------------------------------------------------- views

    function getInstrument(bytes32 instrumentId_)
        external
        view
        returns (InstrumentMetadata memory)
    {
        if (_instruments[instrumentId_].registrar == address(0)) {
            revert InstrumentUnknown(instrumentId_);
        }
        return _instruments[instrumentId_];
    }

    function getPosition(bytes32 instrumentId_, address holder)
        external
        view
        returns (uint256)
    {
        return _positions[instrumentId_][holder];
    }

    /// Enumeration helpers — mappings cannot be iterated; the index array can.
    /// O(n) loops over unbounded arrays belong OFF-chain (web3j paging), not
    /// inside state-changing functions.
    function instrumentCount() external view returns (uint256) {
        return _instrumentIds.length;
    }

    function instrumentIdAt(uint256 index) external view returns (bytes32) {
        return _instrumentIds[index]; // reverts with panic 0x32 if out of bounds
    }

    // ---------------------------------------------------- money-math surface

    /// @notice Coupon/fee preview: rateBps applied to a holding, half-up.
    function previewRateAmount(uint256 holdingMinor, uint256 rateBps)
        external
        pure
        returns (uint256)
    {
        return holdingMinor.applyRateBps(
            rateBps,
            InstrumentMoneyMath.Rounding.HalfUp
        );
    }

    /// @notice Convert major units (whole CHF) to minor units (rappen) for
    ///         a given instrument's scale.
    function quoteMinorUnits(bytes32 instrumentId_, uint256 majorUnits)
        external
        view
        returns (uint256)
    {
        InstrumentMetadata storage meta = _instruments[instrumentId_];
        if (meta.registrar == address(0)) revert InstrumentUnknown(instrumentId_);
        return InstrumentMoneyMath.toMinorUnits(majorUnits, meta.tokenDecimals);
    }

    // ------------------------------------------------------- ABI demo surface

    /// @notice ABI-encode a booking payload exactly as web3j would for
    ///         off-chain signing / hashing. Returns head+tail encoded bytes.
    function encodeBookingPayload(
        bytes32 instrumentId_,
        address holder,
        uint256 amountMinor
    ) external pure returns (bytes memory) {
        return abi.encode(instrumentId_, holder, amountMinor);
    }

    /// @notice Inverse of encodeBookingPayload — typed decode of raw bytes.
    function decodeBookingPayload(bytes calldata payload)
        external
        pure
        returns (bytes32 instrumentId_, address holder, uint256 amountMinor)
    {
        (instrumentId_, holder, amountMinor) =
            abi.decode(payload, (bytes32, address, uint256));
    }

    /// @notice Collision-resistant digest of a booking — uses abi.encode,
    ///         never encodePacked, when multiple dynamic values could collide.
    function bookingDigest(
        bytes32 instrumentId_,
        address holder,
        uint256 amountMinor
    ) external pure returns (bytes32) {
        return keccak256(abi.encode(instrumentId_, holder, amountMinor));
    }

    /// @notice First 4 bytes of keccak256("bookPosition(bytes32,address,uint256,bytes32)").
    ///         This is the routing key the EVM dispatcher matches on — and the
    ///         first 4 bytes of every calldata blob web3j builds for this call.
    function bookPositionSelector() external pure returns (bytes4) {
        return this.bookPosition.selector;
    }

    // -------------------------------------------------- codec demo surface

    /// @notice Expose the library codec for off-chain round-trip testing.
    function encodeIsin(string calldata isinStr) external pure returns (bytes32) {
        return InstrumentIsinCodec.encodeIsin(isinStr);
    }

    function decodeIdentifier(bytes32 encoded)
        external
        pure
        returns (string memory)
    {
        return InstrumentIsinCodec.decodeAscii(encoded);
    }
}
