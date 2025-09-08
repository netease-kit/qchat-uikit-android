
/*
 * Copyright (c) 2022 NetEase, Inc.  All rights reserved.
 * Use of this source code is governed by a MIT license that can be found in the LICENSE file.
 */

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.netease.yunxin.kit.qchatkit.ui"

    compileSdk = 34

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "versionName", "\"9.4.1\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(project(":qchatkit"))
    api("com.netease.yunxin.kit.common:common-ui:1.7.0")
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21")
    implementation("androidx.appcompat:appcompat:1.4.2") 
    implementation("com.google.android.material:material:1.5.0") 
    implementation("androidx.recyclerview:recyclerview:1.2.1") 
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.github.bumptech.glide:glide:4.13.1")
    implementation("com.github.bumptech.glide:compiler:4.13.1")
}

