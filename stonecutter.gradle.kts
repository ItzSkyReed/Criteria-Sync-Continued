plugins {
    id("dev.kikugie.stonecutter")
    id("net.fabricmc.fabric-loom") apply false
	id("net.fabricmc.fabric-loom-remap") apply false
}

stonecutter tasks {
    order("publishModrinth")
}

stonecutter parameters {
    dependencies["java"] = node.project.property("java_version") as String

    replacements {
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }
    }
}

stonecutter active "26.1.2"
