plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

val springSecurityKerberosVersion = "2.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security.kerberos:spring-security-kerberos-core:$springSecurityKerberosVersion")
    implementation("org.springframework.security.kerberos:spring-security-kerberos-web:$springSecurityKerberosVersion")
    implementation("org.springframework.security.kerberos:spring-security-kerberos-client:$springSecurityKerberosVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // For LDAP integration (optional, for group lookup)
    implementation("org.springframework.boot:spring-boot-starter-data-ldap")
    implementation("org.springframework.security:spring-security-ldap")
    implementation("org.springframework.ldap:spring-ldap-core")
    
    // For JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // For logging
    implementation("org.springframework.boot:spring-boot-starter-logging")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks {
    test {
        useJUnitPlatform()
    }

    bootRun {
        jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
        systemProperties = mapOf(
            "sun.security.krb5.debug" to "true",
            "java.security.krb5.conf" to "/Users/rafaelferreira/GitHub/openldap-kerberos-dev/krb5-client.conf",     
        )
    }
}

