plugins { application }

dependencies {
    implementation(project(":dualwrite-lib"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.1"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    implementation(platform("software.amazon.awssdk:bom:2.31.0"))
    implementation("software.amazon.awssdk:dynamodb")

    // DynamoDB Local, started in-process so the example runs with no
    // external infrastructure. See DynamoDbLocalServer.
    implementation("com.amazonaws:DynamoDBLocal:2.6.1")
}

application { mainClass.set("com.example.shipment.ShipmentApplication") }

// DynamoDB Local needs the sqlite4java natives on java.library.path.
val nativesDir = layout.buildDirectory.dir("natives")
val copyNatives by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath.get().filter {
        it.name.contains("sqlite4java") || it.name.endsWith(".dylib") || it.name.endsWith(".so")
    })
    into(nativesDir)
}
tasks.named<JavaExec>("run") {
    dependsOn(copyNatives)
    systemProperty("sqlite4java.library.path", nativesDir.get().asFile.absolutePath)
}
