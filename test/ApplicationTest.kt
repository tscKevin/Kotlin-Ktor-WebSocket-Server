package com.example

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.server.testing.*
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("HELLO TSMC IE!", response.content)
            }
        }
    }
    @Test
    fun testConversation() {
        withTestApplication(Application::module) {
            handleWebSocketConversation("/chat") { incoming, outgoing ->
                outgoing.send(Frame.Text("WebSocket test"))
                val greetingText = (incoming.receive() as Frame.Text).readText()
                assertEquals("WebSocket test", greetingText)

                outgoing.send(Frame.Text("send and receive test"))
                val responseText = (incoming.receive() as Frame.Text).readText()
                assertEquals("send and receive test", responseText)
            }
        }
    }
}
