package org.github.mibo.shell.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.github.mibo.shell.support.OAuthTokenService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.lang.IllegalArgumentException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@ShellComponent
class SecureConnect(val env: Environment, val args: ApplicationArguments) {

  private var connected = false
  private var token: String = "<not requested>"
  private val config = loadConfig()

  private final fun loadConfig(): Map<String, Any> {
    val configArg = args.getOptionValues("config")
    if (configArg.isEmpty()) {
      println("No configuration name given")
      throw IllegalArgumentException("No configuration name given")
    }
    val configName = configArg[0]
    println("Config name: $configName")
    val yaml = Yaml()
    val configPath = Path.of(configName)
    if (Files.exists(configPath)) {
      return yaml.load(FileInputStream(File(configName)))
    } else {
      println("No file found at $configPath. Try classpath with ${configPath.fileName}.")
      val inputStream = this.javaClass
        .classLoader
        .getResourceAsStream(configPath.fileName.toString())
      return yaml.load(inputStream)
    }
  }

  @ShellMethod("Secure Connect to server")
  fun connect() {
    val endpoint = getRequiredProperty("oauth2", "endpoint")
    val clientId = getRequiredProperty("oauth2", "clientid")
    val clientSecret = getRequiredProperty("oauth2", "clientsecret")
    token = OAuthTokenService().jwtToken(clientId, clientSecret, endpoint)
  }


  @ShellMethod("Show current config settings")
  fun config() {
    println("Endpoint: ${getProperty("oauth2", "clientid").orElse("<not set>")}")
    println("Client id: ${getProperty("oauth2", "clientid").isPresent}")
    println("Client secret: ${getProperty("oauth2", "clientsecret").isPresent}")
    urls()
  }

  fun getProperty(vararg names: String): Optional<String> {
    var map = config
    var value: Any? = null
    var counter = 0
    for (name in names) {
      value = map[name]
      if (value is Map<*, *>) {
        map = value as Map<String, Any>
        counter++
      }
    }
    if (counter == names.size-1) {
      if (value is String) {
        return Optional.of(value)
      }
    }
    return Optional.empty()
  }

  fun getPropertyMap(vararg names: String): Optional<Map<String, Any>> {
    var map = config
    var value: Any?
    for (name in names) {
      value = map[name]
      if (value is Map<*, *>) {
        map = value as Map<String, Any>
      } else {
        return Optional.empty()
      }
    }
    return Optional.of(map)
  }

  fun getRequiredProperty(vararg names: String): String {
    return getProperty(*names).orElseThrow()
  }

  @ShellMethod(key = ["get", "g"], value = "Get request with token")
  fun get(@ShellOption(value = ["-u", "--url"]) urlName: String,
          @ShellOption(value = ["-p", "--param"], defaultValue = "") params: List<String>) {

    if (!connected) {
      connect()
    }
    val url = getProperty("urls", urlName)
    if (url.isPresent) {
      val uri = URI(replacePlaceholder(url.get(), params))
      val rest = RestTemplateBuilder().build()
      val headers = HttpHeaders()
      headers["Authorization"] = "Bearer $token"
      val requestEntity = HttpEntity(null, headers)
      println("Send get request to $uri")
  //    println("Request headers $headers")
      val response = rest.exchange(uri, HttpMethod.GET, requestEntity, String::class.java)
  //    println(response.statusCode)
      if (response.statusCode.is2xxSuccessful) {
        response.body?.let {
          val gson = GsonBuilder().setPrettyPrinting().create()
          val json = gson.fromJson(it, JsonObject::class.java)
          println(gson.toJson(json))
        }
      } else {
        println("Not successful request: ${response.statusCode}")
      }
    } else {
      println("Given url name $urlName is not configured")
    }
  }

  @ShellMethod("List all configured urls")
  fun urls() {
    val url = getPropertyMap("urls")
    url.ifPresent {
      for (entry in it.entries) {
        println("Url name ${entry.key} => ${entry.value}")
      }
    }
  }

  private fun replacePlaceholder(url: String, params: List<String>): String {
    println("Replace placeholders in $url with $params")
    val regex = Regex("\\{}")
    val placeholderCount = regex.findAll(url).count()
    if (placeholderCount != params.size) {
      println("Placeholder count not fit to parameter count")
      throw IllegalArgumentException("Placeholder count not fit to parameter count")
    }
    var result = url
    params.forEach {
      result = result.replaceFirst("{}", it)
    }
    return result
  }
}