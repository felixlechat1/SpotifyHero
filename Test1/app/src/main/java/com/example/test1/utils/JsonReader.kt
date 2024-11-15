package com.example.test1.utils

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

fun ReadJSONFromAssets(context: Context?, path: String): String {
    println("Opening file $path")
    val file = context?.assets?.open(path)
    println("Found file $path")
    val bufferedReader = BufferedReader(InputStreamReader(file))
    val stringBuilder = StringBuilder()
    bufferedReader.useLines { lines ->
        lines.forEach {
            stringBuilder.append(it)
        }
    }
    val jsonString = stringBuilder.toString()
    return jsonString

}