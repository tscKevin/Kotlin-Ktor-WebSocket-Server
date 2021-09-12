package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.css.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.cio.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val client = HttpClient(Apache) {
    }

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        get("/") {
            call.respondText("HELLO TSMC IE!", contentType = ContentType.Text.Plain)
        }
        webSocket("/echoTest") {
            while (true) {
                val receive = incoming.receive()
                when (receive) {
                    is Frame.Text -> {
                        val msg = receive.readText()
                        outgoing.send(Frame.Text(msg))
                        if (msg.equals("bye", ignoreCase = true)) {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Client Send BYE"))
                        }
                        println(msg)
                    }
                }
            }
        }
        var connectList: ArrayList<DefaultWebSocketServerSession> = ArrayList()
        webSocket("/chat") {
            connectList.add(this)
            try {
                while (true) {
                    val receive = incoming.receive()
                    when (receive) {
                        is Frame.Text -> {
                            val msg = receive.readText()
                            connectList.forEach {
                                it.outgoing.send(Frame.Text(msg))
                            }
                            if (msg.equals("bye", ignoreCase = true)) {
                                connectList.remove(this)
                                close(CloseReason(CloseReason.Codes.NORMAL, "Client Send BYE"))
                            }
                            println(msg)
                        }
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                this.close()
            } finally {
                connectList.remove(this)
            }
        }
    }
}

fun FlowOrMetaDataContent.styleCss(builder: CSSBuilder.() -> Unit) {
    style(type = ContentType.Text.CSS.toString()) {
        +CSSBuilder().apply(builder).toString()
    }
}

fun CommonAttributeGroupFacade.style(builder: CSSBuilder.() -> Unit) {
    this.style = CSSBuilder().apply(builder).toString().trim()
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
