package com.epam.drill.test.common

import java.io.*
import java.net.*

@Throws(UnsupportedEncodingException::class)
fun String.urlEncode() = URLEncoder.encode(this, Charsets.UTF_8.name())
