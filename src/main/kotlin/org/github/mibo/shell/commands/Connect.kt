package org.github.mibo.shell.commands

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.shell.Availability
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellMethodAvailability
import org.springframework.shell.standard.ShellOption
import java.net.URI

@ShellComponent
class Connect {

  private var connected = false
  private var connectedUri: URI? = null

  @ShellMethod("Connect to server")
  fun connect(@ShellOption(value = ["-u", "--url"]) url: String) {
    val uri = URI(url)
    val rest = RestTemplateBuilder().build()
    val response = rest.exchange(uri, HttpMethod.GET, HttpEntity.EMPTY, String::class.java)
    println(response.statusCode)
    if (response.statusCode.is2xxSuccessful) {
      connected = true
      connectedUri = uri
    }
  }

  @ShellMethod("Read from pre-connected server")
  fun read() {
    val rest = RestTemplateBuilder().build()
    val response = connectedUri?.let { rest.exchange(it, HttpMethod.GET, HttpEntity.EMPTY, String::class.java) }
    response?.body?.let { println(it) }
  }

  @ShellMethodAvailability(value = ["read"])
  fun isConnected(): Availability {
    return if (connected)
      Availability.available() else
      Availability.unavailable("Not yet connected")
  }
}