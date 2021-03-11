package org.github.mibo.shell.commands

import org.github.mibo.shell.support.OAuthTokenService
import org.springframework.core.env.Environment
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

@ShellComponent
class SecureConnect(val config: Environment) {

  private var connected = false
  private var token: String = "<not requested>"

  @ShellMethod("Secure Connect to server")
  fun secon() {
//  fun connect(@ShellOption(value = ["-u", "--url"]) url: String) {
    val endpoint = config.getRequiredProperty("secureConnect.oauth2.endpoint")
    val clientId = config.getRequiredProperty("secureConnect.oauth2.clientid")
    val clientSecret = config.getRequiredProperty("secureConnect.oauth2.clientsecret")
    val token = OAuthTokenService().jwtToken(clientId, clientSecret, endpoint)

    println("Token: $token")
//    val uri = URI(url)
//    val rest = RestTemplateBuilder().build()
//    val response = rest.exchange(uri, HttpMethod.GET, HttpEntity.EMPTY, String::class.java)
//    println(response.statusCode)
//    if (response.statusCode.is2xxSuccessful) {
//      connected = true
//      connectedUri = uri
//    }
  }
}