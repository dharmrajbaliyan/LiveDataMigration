dependencies {
    // The library deliberately depends on Spring only for transaction
    // synchronisation. It knows nothing about JPA, DynamoDB or the AWS SDK:
    // the target store is reached through the TargetStore interface.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.1"))
    implementation("org.springframework:spring-tx")
    implementation("org.slf4j:slf4j-api")
}
