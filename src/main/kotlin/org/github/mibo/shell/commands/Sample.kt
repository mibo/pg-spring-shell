package org.github.mibo.shell.commands

import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod


@ShellComponent
class Sample {
  @ShellMethod("Add two integers together.")
  fun add(a: Int, b: Int): Int {
    return a + b
  }
}