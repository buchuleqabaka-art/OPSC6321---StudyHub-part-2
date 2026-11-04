package com.studyhub.app.util
import org.junit.Assert.*
import org.junit.Test
class ApiEndpointPolicyTest {
    @Test fun debugCanUsePrivateLocalApiOnly() {
        for (url in listOf("http://10.0.2.2:8080/","http://192.168.1.2:8080/","http://localhost:8080/")) {
            assertTrue(ApiEndpointPolicy.allowed(url,true)); assertFalse(ApiEndpointPolicy.allowed(url,false))
        }
        for (url in listOf("http://example.org/","http://10.evil.com/","https://example.com/","https://user:secret@api.example.org/")) assertFalse(ApiEndpointPolicy.allowed(url,true))
        assertTrue(ApiEndpointPolicy.allowed("https://api.example.org/",false))
    }
}
