package com.epam.drill.test.common

import java.net.*

fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
