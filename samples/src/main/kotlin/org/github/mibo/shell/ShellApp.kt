package org.github.mibo.shell

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ShellApp

fun main(args: Array<String>) {
  runApplication<ShellApp>(*args)
}
