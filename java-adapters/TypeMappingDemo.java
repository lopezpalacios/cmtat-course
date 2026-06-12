package ch.bank.cmtat.course02;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicStruct;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint64;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

/**
 * Chapter 02 companion — TypeMappingDemo.
 *
 * Demonstrates, for every Solidity type used by InstrumentTypes.sol, the
 * web3j 4.x Java type that encodes/decodes it:
 *
 *   uint256  <-> org.web3j.abi.datatypes.generated.Uint256  (value: BigInteger)
 *   uint64   <-> Uint64   (BigInteger)
 *   uint8    <-> Uint8    (BigInteger) — also how enums travel
 *   bytes32  <-> Bytes32  (byte[32])
 *   address  <-> Address  (String, 0x-hex)
 *   bool     <-> Bool     (Boolean)
 *   string   <-> Utf8String (String)
 *   struct   <-> StaticStruct / DynamicStruct subclasses
 *
 * Illustrative: runs offline against hand-rolled ABI hex, no node required.
 */
public final class TypeMappingDemo {

    private TypeMappingDemo() {
    }

    // ------------------------------------------------------------------
    // bytes32 <-> ISIN/LEI (right-padded ASCII)
    // ------------------------------------------------------------------

    /**
     * Encode an ISIN (12 chars, [0-9A-Z]) into a 32-byte array,
     * left-aligned / right-padded with 0x00 — the exact layout
     * InstrumentIsinCodec.encodeIsin produces on-chain.
     */
    public static byte[] isinToBytes32(String isin) {
        if (isin == null || isin.length() != 12) {
            throw new IllegalArgumentException("ISIN must be exactly 12 chars: " + isin);
        }
        for (char c : isin.toCharArray()) {
            boolean digit = c >= '0' && c <= '9';
            boolean upper = c >= 'A' && c <= 'Z';
            if (!digit && !upper) {
                throw new IllegalArgumentException("Illegal ISIN char: " + c);
            }
        }
        byte[] out = new byte[32]; // zero-initialized => right padding for free
        byte[] ascii = isin.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(ascii, 0, out, 0, ascii.length);
        return out;
    }

    /** Decode right-padded-ASCII bytes32 back to a String (trim 0x00 tail). */
    public static String bytes32ToAscii(byte[] raw) {
        if (raw.length != 32) {
            throw new IllegalArgumentException("Expected 32 bytes, got " + raw.length);
        }
        int len = 0;
        while (len < 32 && raw[len] != 0x00) {
            len++;
        }
        return new String(raw, 0, len, StandardCharsets.US_ASCII);
    }

    // ------------------------------------------------------------------
    // uint256 minor units <-> BigDecimal major units
    // ------------------------------------------------------------------

    /**
     * Chain -> bank: uint256 minor units to a scaled BigDecimal.
     * 1234567 with decimals=2 -> 12345.67 (CHF).
     * movePointLeft is exact — no rounding ever happens chain->bank.
     */
    public static BigDecimal minorUnitsToDecimal(BigInteger minorUnits, int decimals) {
        return new BigDecimal(minorUnits).movePointLeft(decimals);
    }

    /**
     * Bank -> chain: BigDecimal to uint256 minor units.
     * RoundingMode.UNNECESSARY makes any precision loss throw ArithmeticException
     * instead of silently truncating — a sub-rappen amount must be a hard error
     * at the boundary, never a silent rounding.
     */
    public static BigInteger decimalToMinorUnits(BigDecimal amount, int decimals) {
        return amount.movePointRight(decimals)
                .setScale(0, RoundingMode.UNNECESSARY)
                .toBigIntegerExact();
    }

    // ------------------------------------------------------------------
    // address checksums (EIP-55)
    // ------------------------------------------------------------------

    /** Normalize then re-checksum an address; reject malformed input. */
    public static String toChecksummed(String address) {
        String clean = Numeric.cleanHexPrefix(address).toLowerCase();
        if (clean.length() != 40) {
            throw new IllegalArgumentException("Address must be 20 bytes hex: " + address);
        }
        return Keys.toChecksumAddress("0x" + clean);
    }

    // ------------------------------------------------------------------
    // Struct mirror: InstrumentTypes.InstrumentMetadata
    // ------------------------------------------------------------------

    /**
     * Java mirror of the Solidity struct. Contains a `string`, so the ABI
     * encoding is dynamic => extend DynamicStruct (a struct with only
     * fixed-size fields would extend StaticStruct instead).
     *
     * Field order MUST match the Solidity declaration order exactly:
     * (bytes32 isin, bytes32 lei, string name, uint8 tokenDecimals,
     *  uint256 nominalValueMinor, uint64 issueDate, uint8 state,
     *  address registrar) — the enum travels as uint8.
     */
    public static class InstrumentMetadataStruct extends DynamicStruct {
        public final byte[] isin;
        public final byte[] lei;
        public final String name;
        public final BigInteger tokenDecimals;
        public final BigInteger nominalValueMinor;
        public final BigInteger issueDate;
        public final BigInteger state;     // InstrumentLifecycle as uint8
        public final String registrar;

        public InstrumentMetadataStruct(Bytes32 isin,
                                        Bytes32 lei,
                                        Utf8String name,
                                        Uint8 tokenDecimals,
                                        Uint256 nominalValueMinor,
                                        Uint64 issueDate,
                                        Uint8 state,
                                        Address registrar) {
            super(isin, lei, name, tokenDecimals, nominalValueMinor,
                  issueDate, state, registrar);
            this.isin = isin.getValue();
            this.lei = lei.getValue();
            this.name = name.getValue();
            this.tokenDecimals = tokenDecimals.getValue();
            this.nominalValueMinor = nominalValueMinor.getValue();
            this.issueDate = issueDate.getValue();
            this.state = state.getValue();
            this.registrar = registrar.getValue();
        }
    }

    /** Human-readable lifecycle names, index = on-chain uint8 value. */
    private static final String[] LIFECYCLE = {
            "Draft", "Issued", "Active", "Suspended", "Matured", "Redeemed"
    };

    // ------------------------------------------------------------------
    // Calldata construction: what web3j sends for bookPosition(...)
    // ------------------------------------------------------------------

    /**
     * Build the exact calldata hex for
     * bookPosition(bytes32,address,uint256,bytes32).
     * First 4 bytes = function selector; then four 32-byte head words.
     */
    public static String buildBookPositionCalldata(byte[] instrumentId,
                                                   String holder,
                                                   BigInteger amountMinor,
                                                   byte[] bookingRef) {
        Function fn = new Function(
                "bookPosition",
                Arrays.asList(
                        new Bytes32(instrumentId),
                        new Address(holder),
                        new Uint256(amountMinor),
                        new Bytes32(bookingRef)),
                java.util.Collections.emptyList());
        return FunctionEncoder.encode(fn);
    }

    /** Compute a selector by hand to verify what FunctionEncoder produced. */
    public static String selectorOf(String signature) {
        byte[] hash = Hash.sha3(signature.getBytes(StandardCharsets.US_ASCII));
        return Numeric.toHexString(Arrays.copyOfRange(hash, 0, 4));
    }

    // ------------------------------------------------------------------
    // Return-data decoding: every type in one decode pass
    // ------------------------------------------------------------------

    /**
     * Decode the return data of
     * decodeBookingPayload(bytes) -> (bytes32, address, uint256)
     * from raw hex, exactly as a bank-side adapter would after eth_call.
     */
    public static void decodeBookingReturn(String returnDataHex) {
        List<TypeReference<?>> outputs = Arrays.asList(
                new TypeReference<Bytes32>() { },
                new TypeReference<Address>() { },
                new TypeReference<Uint256>() { });

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Type> decoded = FunctionReturnDecoder.decode(returnDataHex, (List) outputs);

        byte[] instrumentId = ((Bytes32) decoded.get(0)).getValue();
        String holder = ((Address) decoded.get(1)).getValue();
        BigInteger amount = ((Uint256) decoded.get(2)).getValue();

        System.out.println("  instrumentId : " + Numeric.toHexString(instrumentId));
        System.out.println("  holder       : " + toChecksummed(holder));
        System.out.println("  amountMinor  : " + amount
                + "  (= CHF " + minorUnitsToDecimal(amount, 2) + ")");
    }

    /**
     * Decode getInstrument(bytes32) return data into the struct mirror.
     * web3j needs the concrete subclass in the TypeReference so it knows
     * the component layout of the dynamic struct.
     */
    public static InstrumentMetadataStruct decodeInstrumentReturn(String returnDataHex) {
        List<TypeReference<?>> outputs = Arrays.asList(
                new TypeReference<InstrumentMetadataStruct>() { });

        @SuppressWarnings({"rawtypes", "unchecked"})
        List<Type> decoded = FunctionReturnDecoder.decode(returnDataHex, (List) outputs);
        return (InstrumentMetadataStruct) decoded.get(0);
    }

    // ------------------------------------------------------------------
    // Demo entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.out.println("== 1. bytes32 <-> ISIN round trip ==");
        byte[] isinBytes = isinToBytes32("CH0012345678");
        System.out.println("  encoded : " + Numeric.toHexString(isinBytes));
        System.out.println("  decoded : " + bytes32ToAscii(isinBytes));

        System.out.println("== 2. uint256 minor units <-> BigDecimal ==");
        BigInteger onChain = new BigInteger("1234567");
        System.out.println("  1234567 minor @2dp -> CHF "
                + minorUnitsToDecimal(onChain, 2));
        System.out.println("  CHF 12345.67 -> " + decimalToMinorUnits(
                new BigDecimal("12345.67"), 2) + " minor units");
        try {
            decimalToMinorUnits(new BigDecimal("0.005"), 2); // sub-rappen!
        } catch (ArithmeticException e) {
            System.out.println("  CHF 0.005 @2dp rejected: " + e.getMessage());
        }

        System.out.println("== 3. EIP-55 checksum ==");
        System.out.println("  "
                + toChecksummed("0x52908400098527886e0f7030069857d2e4169ee7"));

        System.out.println("== 4. function selector + calldata ==");
        String sig = "bookPosition(bytes32,address,uint256,bytes32)";
        System.out.println("  selector(" + sig + ") = " + selectorOf(sig));
        byte[] bookingRef = Hash.sha3("CBS-BOOKING-2026-000123"
                .getBytes(StandardCharsets.US_ASCII));
        String calldata = buildBookPositionCalldata(
                Hash.sha3(isinBytes), // demo instrument id
                "0x52908400098527886E0F7030069857D2E4169EE7",
                new BigInteger("1000000"),
                bookingRef);
        System.out.println("  calldata = " + calldata);

        System.out.println("== 5. decode (bytes32, address, uint256) return data ==");
        // Hand-assembled return data: 3 head words, as eth_call would return it.
        String returnData = "0x"
                + Numeric.toHexStringNoPrefix(Hash.sha3(isinBytes))
                + "00000000000000000000000052908400098527886e0f7030069857d2e4169ee7"
                + "00000000000000000000000000000000000000000000000000000000000f4240";
        decodeBookingReturn(returnData);

        System.out.println("Done. Full struct decoding via "
                + "InstrumentMetadataStruct is exercised by decodeInstrumentReturn().");
    }
}
