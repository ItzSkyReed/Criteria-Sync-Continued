import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension

val obfuscated = sc.current.parsed < "26.1"
plugins.apply(if(obfuscated) "net.fabricmc.fabric-loom-remap" else "net.fabricmc.fabric-loom")
val loom = the<LoomGradleExtensionAPI>()
val fabricApi = the<FabricApiExtension>()
val modImplementation = if(obfuscated) configurations.named("modImplementation") else configurations.implementation
val modJar = if(obfuscated) tasks.named<Zip>("remapJar") else tasks.named<Zip>("jar")

version = "${project.property("mod_version")}+${sc.current.version}"

base {
    archivesName = rootProject.name
}

dependencies {
    // https://github.com/FabricMC/fabric
    fun fapi(vararg modules: String) {
        modules.forEach {
            modImplementation(fabricApi.module(it, project.property("deps.fabric_api") as String))
        }
    }

    "minecraft"("com.mojang:minecraft:${sc.current.version}")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    fapi("fabric-command-api-v2")

    if (obfuscated) {
        "mappings"(loom.officialMojangMappings())
    }

    if (sc.current.parsed < "1.21.11") {
        compileOnly("org.jspecify:jspecify:1.0.0")
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to project.property("fmj.minecraft"),
        "fapi" to project.property("deps.fabric_api")
    )
    props.forEach { k, v -> inputs.property(k, v) }

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

extensions.configure<LoomGradleExtensionAPI>() {
    splitEnvironmentSourceSets()

    mods {
        create(rootProject.name) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}


tasks.register<Copy>("buildAndCollect") {
    group = "build"
    from(modJar.flatMap { it.archiveFile } /*, remapSourcesJar.map { it.archiveFile }*/)
    into(rootProject.layout.buildDirectory.file("libs"))
    dependsOn("build")
}

java {
    withSourcesJar()

    val j = JavaVersion.valueOf("VERSION_${project.property("java_version")}")
    targetCompatibility = j
    sourceCompatibility = j
}

tasks.jar {
    val name = rootProject.name
    inputs.property("project_name", name)

    from("LICENSE") {
        rename { "${it}_${name}" }
    }
}

tasks.test {
    useJUnitPlatform()
}
