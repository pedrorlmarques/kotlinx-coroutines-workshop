package com.example.coroutine_testing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoroutineTestingApplication

fun main(args: Array<String>) {
	runApplication<CoroutineTestingApplication>(*args)
}
