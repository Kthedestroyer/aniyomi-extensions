apply(from = "repositories.gradle.kts")

include(":core")

// Shared helper libraries (optional) live in /lib and /lib-multisrc
File(rootDir, "lib").eachDir { include("lib:${it.name}") }
File(rootDir, "lib-multisrc").eachDir { include("lib-multisrc:${it.name}") }

// Every folder at src/<lang>/<name> is an extension and gets built automatically.
File(rootDir, "src").eachDir { lang ->
    lang.eachDir { ext -> include("src:${lang.name}:${ext.name}") }
}

fun File.eachDir(block: (File) -> Unit) {
    val files = listFiles() ?: return
    for (file in files) {
        if (file.isDirectory && file.name != ".gradle" && file.name != "build") {
            block(file)
        }
    }
}
