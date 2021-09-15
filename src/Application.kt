package com.example

import arrow.core.Either
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.css.CSSBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.FlowOrMetaDataContent
import kotlinx.html.style
import java.time.Duration

data class JsonPacket(val serverCmd: ServerCmd?, val chatMessage: ChatMessage?)
data class ChatMessage(val userName: String?, val msg: String?)
data class ServerCmd(val cmdFun: String?, val cmd: Array<String>?, val userList: Array<String>?)
data class userList(val userList: ArrayList<String>)

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
        val connectList: ArrayList<DefaultWebSocketServerSession> = ArrayList()
        val onlineList: ArrayList<String> = ArrayList()
        webSocket("/chat") {
            connectList.add(this)
            val userName = (incoming.receive() as Frame.Text).readText()
            onlineList.add(userName)
            updateOnlineUserList(connectList, onlineList)
            receive@ while (true) {
                when (val result = getReceivedText()) {
                    is Either.Left -> {
                        this.close()
                        connectList.remove(this)
                        onlineList.remove(userName)
                        updateOnlineUserList(connectList, onlineList)
                        println("$userName close connect")
                        break@receive
                    }
                    is Either.Right -> {
                        connectList.forEach { it.outgoing.send(Frame.Text(result.value)) }
                        println(result.value)
                    }
                }
            }
        }
    }
}

sealed class SocketReceiveException() : Exception() {
    object ClosedReceiveChannelException : SocketReceiveException()
    object CancellationException : SocketReceiveException()
}

private suspend fun DefaultWebSocketServerSession.getReceivedText(): Either<SocketReceiveException, String> =
    Either.catch { (incoming.receive() as Frame.Text).readText() }
        .mapLeft { SocketReceiveException.ClosedReceiveChannelException }

private suspend fun updateOnlineUserList(
    connectList: ArrayList<DefaultWebSocketServerSession>,
    onlineList: ArrayList<String>,
) {
    connectList.forEach { socket ->
        socket.outgoing.send(Frame.Text("{\"serverCmd\":" +
                "{\"cmdFun\":\"UPDATE_USER\"," +
                "${
                    Gson().toJson(userList(onlineList))
                        .replace("{", "").replace("}", "")
                }}" +
                "}"))
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
