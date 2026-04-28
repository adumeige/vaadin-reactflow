package com.example.reactflow

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@Push
@SpringBootApplication
open class Application : AppShellConfigurator

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
