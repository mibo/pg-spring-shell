package org.github.mibo.shell.commands

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.github.mibo.shell.support.OAuthTokenService
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.convert.converter.Converter
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import org.springframework.stereotype.Component
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
  private var verbose = false
  private var outFile = Optional.empty<File>()
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

  @ShellMethod("Set verbosity")
  fun verbose(@ShellOption(value = ["off"]) off: Boolean) {
    println("Set verbose to ${!off} (was ${this.verbose})")
    this.verbose = !off
  }

  @ShellMethod("Set file to write response to")
  fun outfile(@ShellOption(value = ["--name"]) outFileName: String) {
    println("Set out file to $outFileName (was ${this.outFile})")
    val file = File(outFileName)
    if (file.exists()) {
      if (file.isFile && file.canWrite()) {
        this.outFile = Optional.of(file)
      } else {
        println("Given file $outFileName exists but, it is not a file or is not writeable.")
      }
    } else {
      val parentFile = file.parentFile
      if (parentFile.exists() && parentFile.isDirectory && parentFile.canWrite()) {
        file.createNewFile()
        this.outFile = Optional.of(file)
      } else {
        println("Given file $outFileName does not exists, is not a file or is not writeable.")
      }
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
          @ShellOption(value = ["-p", "--param"], defaultValue = "") params: List<String>,
          @ShellOption(value = ["-h", "--header"], defaultValue = "") headers: HttpHeaders ) {

    if (!connected) {
      connect()
    }
    val url = readUrl(urlName)
    if (url.isPresent) {
      val uri = URI(replacePlaceholder(url.get().url, params))
      val rest = RestTemplateBuilder().build()
      headers["Authorization"] = "Bearer $token"
      headers.addAll(url.get().headers)
      val requestEntity = HttpEntity(null, headers)
      println("Send get request to $uri")
      if (verbose) {
        println("Request headers: ${format(headers)}")
      }
      val response = rest.exchange(uri, HttpMethod.GET, requestEntity, String::class.java)
      if (response.statusCode.is2xxSuccessful) {
        val body = convertBody(response)
        body.ifPresent { 
          println("Body:\n```\n${body.get()}\n```\n")
          outFile.ifPresent {
            writeToFile(it, body.get())
          }
        }
      } else {
        println("Not successful request: ${response.statusCode}")
      }
    } else {
      println("Given url name $urlName is not configured")
    }
  }

  private fun format(headers: HttpHeaders): String {

    return headers
      .map { "Name: ${it.key} => ${it.value.joinToString(" | ", "\"", "\"")}" }
      .joinToString("\n", "\n---\n", "\n---\n")
  }

  private fun writeToFile(file: File, content: String) {
    file.writeText(content)
  }

  fun convertBody(response: ResponseEntity<String>): Optional<String> {
    val body = Optional.ofNullable(response.body)
    return body.map {
      if (isJsonContent(response)) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.fromJson(it, JsonObject::class.java)
        return@map gson.toJson(json)
      }
      return@map it
    }
  }

  private fun isJsonContent(response: ResponseEntity<String>): Boolean {
    val ct = response.headers["Content-Type"]
    return ct?.contains("application/json") == true
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

  private fun readUrl(url: String): Optional<UrlConfig> {
    val urls = getPropertyMap("urls")
                .orElseThrow { IllegalArgumentException("No urls property found in config") }
    val urlConfig = urls[url]
    if (urlConfig is String) {
      return Optional.of(UrlConfig(urlConfig, HttpHeaders.EMPTY))
    } else if (urlConfig is Map<*, *>) {
      val configUrl = urlConfig["url"] as String
      val configHeaders = urlConfig["headers"] as Map<String, *>
      val httpHeaders = HttpHeaders()
      configHeaders.entries.forEach { httpHeaders.add(it.key, String.format("%s", it.value)) }
      return Optional.of(UrlConfig(configUrl, httpHeaders))
    }
    println("Given url name $url is not configured")
    return Optional.empty()
  }

  class UrlConfig(val url: String, val headers: HttpHeaders) {
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

  @Component
  internal class CustomDomainConverter : Converter<String, HttpHeaders> {
    override fun convert(headersParam: String): HttpHeaders {
      if (headersParam.isEmpty()) {
        return HttpHeaders()
      }
      val httpHeaders = HttpHeaders()
      val headers = headersParam.split(",")
      headers.forEach {
        val keyValue = it.split(":")
        if (keyValue.size == 2) {
          httpHeaders[keyValue[0]] = listOf(keyValue[1])
        } else {
          println("Invalid header format. Specify each header as `name:value`.")
        }
      }
      return httpHeaders
    }
  }
}