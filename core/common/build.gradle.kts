plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.hash.common"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        //android.dataBinding.enabled has been replace to android.buildFeatures.dataBinding
        dataBinding = true
    }
    sourceSets {
        getByName("main") {
            res.srcDir("src/main/res-sw")
        }
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.material)
    api(libs.androidx.appcompat)
    api(project(":core:widget")) //widget模块

    api(project(":core:bean")) // provide shared bean classes like UserInfoBean

    api(libs.androidx.activity.ktx) //activity扩展
    api(libs.androidx.fragment.ktx) //fragment扩展

    api(libs.androidx.startup.runtime)//启动库

    implementation(libs.immersionbar) // 沉浸式状态栏基础依赖包，必须要依赖
    implementation(libs.immersionbar.ktx) //kotlin扩展（可选)
    implementation(libs.immersionbar.components) //fragment快速实现（可选)
    api(libs.lottie) //lottie动画库

    api(libs.timber)
    // 图片加载框架：https://github.com/bumptech/glide
    // 官方使用文档：https://github.com/Muyangmin/glide-docs-cn
    implementation (libs.glide)
    implementation(libs.androidx.swiperefreshlayout)
    kapt(libs.compiler)

    // Gson 解析容错：https://github.com/getActivity/GsonFactory
    api("com.github.getActivity:GsonFactory:10.5")
    api(libs.gson)
    api(libs.magicindicator)

    api(libs.baserecyclerviewadapterhelper4)
    implementation(libs.mmkv)

    // SmartRefreshLayout，
    api(libs.refresh.layout.kernel)
    api(libs.refresh.header.material)
    api(libs.refresh.footer.classics)
}