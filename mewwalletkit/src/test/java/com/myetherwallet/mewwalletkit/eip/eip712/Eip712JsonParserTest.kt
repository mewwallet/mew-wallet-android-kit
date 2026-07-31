package com.myetherwallet.mewwalletkit.eip.eip712

import com.myetherwallet.mewwalletkit.eip.eip712.data.Array712
import com.myetherwallet.mewwalletkit.eip.eip712.data.Literal712
import com.myetherwallet.mewwalletkit.eip.eip712.data.Struct712
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.ByteArrayInputStream

/**
 * Comprehensive tests for EIP712 JSON parsing functionality
 * This critical security component previously had no test coverage
 */
class Eip712JsonParserTest {

    private lateinit var parser: Eip712JsonParser
    private lateinit var jsonAdapter: MockEip712JsonAdapter

    @Before
    fun setup() {
        jsonAdapter = MockEip712JsonAdapter()
        parser = Eip712JsonParser(jsonAdapter)
    }

    @Test
    fun testBasicTypedDataParsing() {
        val basicJson = """
        {
            "types": {
                "EIP712Domain": [
                    {"name": "name", "type": "string"},
                    {"name": "version", "type": "string"},
                    {"name": "chainId", "type": "uint256"}
                ],
                "Person": [
                    {"name": "name", "type": "string"},
                    {"name": "wallet", "type": "address"}
                ]
            },
            "primaryType": "Person",
            "domain": {
                "name": "Test App",
                "version": "1",
                "chainId": 1
            },
            "message": {
                "name": "Alice",
                "wallet": "0x1234567890123456789012345678901234567890"
            }
        }
        """.trimIndent()

        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("version", "string"),
                    Eip712JsonAdapter.Parameter("chainId", "uint256")
                ),
                "Person" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("wallet", "address")
                )
            ),
            primaryType = "Person",
            domain = mapOf(
                "name" to "Test App",
                "version" to "1",
                "chainId" to 1
            ),
            message = mapOf(
                "name" to "Alice",
                "wallet" to "0x1234567890123456789012345678901234567890"
            )
        )

        val result = parser.parseMessage(basicJson)

        assertNotNull("Parsed result should not be null", result)
        assertEquals("Domain type should be EIP712Domain", "EIP712Domain", result.domain.typeName)
        assertEquals("Message type should be Person", "Person", result.message.typeName)
        assertEquals("Domain should have 3 parameters", 3, result.domain.parameters.size)
        assertEquals("Message should have 2 parameters", 2, result.message.parameters.size)
    }

    @Test
    fun testNestedStructParsing() {
        val nestedJson = """
        {
            "types": {
                "EIP712Domain": [
                    {"name": "name", "type": "string"}
                ],
                "Person": [
                    {"name": "name", "type": "string"},
                    {"name": "account", "type": "Account"}
                ],
                "Account": [
                    {"name": "address", "type": "address"},
                    {"name": "balance", "type": "uint256"}
                ]
            },
            "primaryType": "Person",
            "domain": {
                "name": "Nested Test"
            },
            "message": {
                "name": "Bob",
                "account": {
                    "address": "0x1234567890123456789012345678901234567890",
                    "balance": "1000000000000000000"
                }
            }
        }
        """.trimIndent()

        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "Person" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("account", "Account")
                ),
                "Account" to listOf(
                    Eip712JsonAdapter.Parameter("address", "address"),
                    Eip712JsonAdapter.Parameter("balance", "uint256")
                )
            ),
            primaryType = "Person",
            domain = mapOf("name" to "Nested Test"),
            message = mapOf(
                "name" to "Bob",
                "account" to mapOf(
                    "address" to "0x1234567890123456789012345678901234567890",
                    "balance" to "1000000000000000000"
                )
            )
        )

        val result = parser.parseMessage(nestedJson)

        assertNotNull("Nested parsing result should not be null", result)
        assertTrue("Message should be a struct", result.message is Struct712)

        val accountParam = result.message.parameters.find { it.name == "account" }
        assertNotNull("Account parameter should exist", accountParam)
        assertTrue("Account should be a nested struct", accountParam!!.value is Struct712)

        val accountStruct = accountParam.value as Struct712
        assertEquals("Account struct should have correct type", "Account", accountStruct.typeName)
        assertEquals("Account should have 2 parameters", 2, accountStruct.parameters.size)
    }

    @Test
    fun testArrayHandling() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "Group" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("members", "Person[]")
                ),
                "Person" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("age", "uint8")
                )
            ),
            primaryType = "Group",
            domain = mapOf("name" to "Array Test"),
            message = mapOf(
                "name" to "Test Group",
                "members" to listOf(
                    mapOf("name" to "Alice", "age" to 25),
                    mapOf("name" to "Bob", "age" to 30)
                )
            )
        )

        val result = parser.parseMessage("")

        val membersParam = result.message.parameters.find { it.name == "members" }
        assertNotNull("Members parameter should exist", membersParam)
        assertTrue("Members should be an array", membersParam!!.value is Array712)

        val membersArray = membersParam.value as Array712
        assertEquals("Array should have correct type", "Person[]", membersArray.typeName)
        assertEquals("Array should have 2 members", 2, membersArray.parameters.size)
    }

    @Test
    fun testPrimitiveTypeParsing() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestData" to listOf(
                    Eip712JsonAdapter.Parameter("stringVal", "string"),
                    Eip712JsonAdapter.Parameter("boolVal", "bool"),
                    Eip712JsonAdapter.Parameter("uint256Val", "uint256"),
                    Eip712JsonAdapter.Parameter("int256Val", "int256"),
                    Eip712JsonAdapter.Parameter("addressVal", "address"),
                    Eip712JsonAdapter.Parameter("bytesVal", "bytes"),
                    Eip712JsonAdapter.Parameter("bytes32Val", "bytes32")
                )
            ),
            primaryType = "TestData",
            domain = mapOf("name" to "Primitive Test"),
            message = mapOf(
                "stringVal" to "Hello World",
                "boolVal" to true,
                "uint256Val" to "12345",
                "int256Val" to "-67890",
                "addressVal" to "0x1234567890123456789012345678901234567890",
                "bytesVal" to "0x48656c6c6f",
                "bytes32Val" to "0x1234567890123456789012345678901234567890123456789012345678901234"
            )
        )

        val result = parser.parseMessage("")

        val parameters = result.message.parameters
        assertEquals("Should have 7 parameters", 7, parameters.size)

        // Verify each parameter exists and has correct type
        val paramNames = parameters.map { it.name }
        assertTrue("Should contain stringVal", paramNames.contains("stringVal"))
        assertTrue("Should contain boolVal", paramNames.contains("boolVal"))
        assertTrue("Should contain uint256Val", paramNames.contains("uint256Val"))
        assertTrue("Should contain int256Val", paramNames.contains("int256Val"))
        assertTrue("Should contain addressVal", paramNames.contains("addressVal"))
        assertTrue("Should contain bytesVal", paramNames.contains("bytesVal"))
        assertTrue("Should contain bytes32Val", paramNames.contains("bytes32Val"))

        // All primitive values should be Literal712
        parameters.forEach { param ->
            assertTrue("Parameter ${param.name} should be literal", param.value is Literal712)
        }
    }

    @Test
    fun testNumberParsing() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "Numbers" to listOf(
                    Eip712JsonAdapter.Parameter("hexNumber", "uint256"),
                    Eip712JsonAdapter.Parameter("decimalNumber", "uint256"),
                    Eip712JsonAdapter.Parameter("largeNumber", "uint256"),
                    Eip712JsonAdapter.Parameter("negativeNumber", "int256")
                )
            ),
            primaryType = "Numbers",
            domain = mapOf("name" to "Number Test"),
            message = mapOf(
                "hexNumber" to "0xff",
                "decimalNumber" to "255",
                "largeNumber" to "1000000000000000000000",
                "negativeNumber" to "-12345"
            )
        )

        val result = parser.parseMessage("")

        val parameters = result.message.parameters
        assertEquals("Should have 4 number parameters", 4, parameters.size)

        parameters.forEach { param ->
            assertTrue("Number parameter ${param.name} should be literal", param.value is Literal712)
            val literal = param.value as Literal712
            assertNotNull("Number literal should have a value", literal.value)
        }
    }

    @Test
    fun testBooleanParsing() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "Booleans" to listOf(
                    Eip712JsonAdapter.Parameter("trueVal", "bool"),
                    Eip712JsonAdapter.Parameter("falseVal", "bool"),
                    Eip712JsonAdapter.Parameter("stringTrue", "bool"),
                    Eip712JsonAdapter.Parameter("stringFalse", "bool")
                )
            ),
            primaryType = "Booleans",
            domain = mapOf("name" to "Boolean Test"),
            message = mapOf(
                "trueVal" to true,
                "falseVal" to false,
                "stringTrue" to "true",
                "stringFalse" to "false"
            )
        )

        val result = parser.parseMessage("")

        val parameters = result.message.parameters
        assertEquals("Should have 4 boolean parameters", 4, parameters.size)

        parameters.forEach { param ->
            assertTrue("Boolean parameter ${param.name} should be literal", param.value is Literal712)
            assertEquals("Boolean parameter should have bool type", "bool", (param.value as Literal712).typeName)
        }
    }

    @Test
    fun testInputStreamParsing() {
        val jsonString = """{"test": "data"}"""
        val inputStream = ByteArrayInputStream(jsonString.toByteArray())

        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "Test" to listOf(
                    Eip712JsonAdapter.Parameter("test", "string")
                )
            ),
            primaryType = "Test",
            domain = mapOf("name" to "Stream Test"),
            message = mapOf("test" to "data")
        )

        val result = parser.parseMessage(inputStream)

        assertNotNull("Stream parsing result should not be null", result)
        assertEquals("Should parse from input stream", "Test", result.message.typeName)
    }

    @Test
    fun testMissingParameters() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestStruct" to listOf(
                    Eip712JsonAdapter.Parameter("requiredField", "string"),
                    Eip712JsonAdapter.Parameter("optionalField", "string")
                )
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Missing Test"),
            message = mapOf(
                "requiredField" to "present"
                // optionalField is missing
            )
        )

        val result = parser.parseMessage("")

        val parameters = result.message.parameters
        assertEquals("Should have 2 parameters even if one is missing", 2, parameters.size)

        val optionalParam = parameters.find { it.name == "optionalField" }
        assertNotNull("Optional parameter should exist", optionalParam)
        assertTrue("Missing parameter should be literal", optionalParam!!.value is Literal712)

        val literal = optionalParam.value as Literal712
        assertNull("Missing parameter should have null value", literal.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMissingTypeDefinition() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                )
                // Missing TestStruct definition
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Error Test"),
            message = mapOf("field" to "value")
        )

        parser.parseMessage("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSolidityType() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestStruct" to listOf(
                    Eip712JsonAdapter.Parameter("invalidField", "invalidType")
                )
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Invalid Test"),
            message = mapOf("invalidField" to "value")
        )

        parser.parseMessage("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidNumberValue() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestStruct" to listOf(
                    Eip712JsonAdapter.Parameter("numberField", "uint256")
                )
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Number Error Test"),
            message = mapOf("numberField" to "not_a_number")
        )

        parser.parseMessage("")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBooleanValue() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestStruct" to listOf(
                    Eip712JsonAdapter.Parameter("boolField", "bool")
                )
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Boolean Error Test"),
            message = mapOf("boolField" to "not_a_boolean")
        )

        parser.parseMessage("")
    }

    @Test
    fun testEmptyArrayHandling() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string")
                ),
                "TestStruct" to listOf(
                    Eip712JsonAdapter.Parameter("emptyArray", "string[]")
                )
            ),
            primaryType = "TestStruct",
            domain = mapOf("name" to "Empty Array Test"),
            message = mapOf("emptyArray" to emptyList<String>())
        )

        val result = parser.parseMessage("")

        val arrayParam = result.message.parameters.find { it.name == "emptyArray" }
        assertNotNull("Array parameter should exist", arrayParam)
        assertTrue("Parameter should be an array", arrayParam!!.value is Array712)

        val array = arrayParam.value as Array712
        assertEquals("Array should be empty", 0, array.parameters.size)
    }

    @Test
    fun testComplexNestedStructure() {
        jsonAdapter.setMockResult(
            types = mapOf(
                "EIP712Domain" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("version", "string")
                ),
                "Order" to listOf(
                    Eip712JsonAdapter.Parameter("trader", "Person"),
                    Eip712JsonAdapter.Parameter("items", "Item[]"),
                    Eip712JsonAdapter.Parameter("total", "uint256")
                ),
                "Person" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("wallet", "address")
                ),
                "Item" to listOf(
                    Eip712JsonAdapter.Parameter("name", "string"),
                    Eip712JsonAdapter.Parameter("price", "uint256")
                )
            ),
            primaryType = "Order",
            domain = mapOf(
                "name" to "Trading App",
                "version" to "1.0"
            ),
            message = mapOf(
                "trader" to mapOf(
                    "name" to "Alice",
                    "wallet" to "0x1234567890123456789012345678901234567890"
                ),
                "items" to listOf(
                    mapOf("name" to "Item 1", "price" to "100"),
                    mapOf("name" to "Item 2", "price" to "200")
                ),
                "total" to "300"
            )
        )

        val result = parser.parseMessage("")

        assertNotNull("Complex structure result should not be null", result)
        assertEquals("Should parse Order type", "Order", result.message.typeName)
        assertEquals("Order should have 3 parameters", 3, result.message.parameters.size)

        val traderParam = result.message.parameters.find { it.name == "trader" }
        assertNotNull("Trader parameter should exist", traderParam)
        assertTrue("Trader should be a struct", traderParam!!.value is Struct712)

        val itemsParam = result.message.parameters.find { it.name == "items" }
        assertNotNull("Items parameter should exist", itemsParam)
        assertTrue("Items should be an array", itemsParam!!.value is Array712)

        val itemsArray = itemsParam.value as Array712
        assertEquals("Items array should have 2 elements", 2, itemsArray.parameters.size)
    }

    @Test
    fun testLargeJsonNumberPreservesHashPrecision() {
        val maxUint256 = "115792089237316195423570985008687907853269984665640564039457584007913129639935"
        val typedData = { value: String ->
            """
            {
              "types": {
                "EIP712Domain": [{"name":"name","type":"string"}],
                "Permit": [{"name":"value","type":"uint256"}]
              },
              "primaryType":"Permit",
              "domain":{"name":"Test"},
              "message":{"value":$value}
            }
            """.trimIndent()
        }

        val numericHash = Eip712Utils.getHash(typedData(maxUint256))
        val stringHash = Eip712Utils.getHash(typedData("\"$maxUint256\""))

        assertArrayEquals(stringHash, numericHash)
    }

    /**
     * Mock adapter for testing EIP712JsonParser without external dependencies
     */
    private class MockEip712JsonAdapter : Eip712JsonAdapter {
        private var mockResult: Eip712JsonAdapter.Result? = null

        fun setMockResult(
            types: Map<String, List<Eip712JsonAdapter.Parameter>>,
            primaryType: String,
            domain: Map<String, Any>,
            message: Map<String, Any>
        ) {
            mockResult = Eip712JsonAdapter.Result(primaryType, domain, message, types)
        }

        override fun parse(json: String): Eip712JsonAdapter.Result {
            return mockResult ?: throw IllegalStateException("Mock result not set")
        }

        override fun parse(inputStream: java.io.InputStream): Eip712JsonAdapter.Result {
            return mockResult ?: throw IllegalStateException("Mock result not set")
        }
    }
}
