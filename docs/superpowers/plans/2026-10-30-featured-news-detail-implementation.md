# 精选新闻详情页实现计划

> **执行说明：** 实现时必须按任务逐项执行；步骤使用复选框（`- [ ]`）记录进度。

**目标：** 为精选新闻列表增加可请求、可返回、适配深色模式的原生新闻详情页。

**架构：** 在 `feature:home` 内增加详情模型、Repository、ViewModel 和 `NewsDetailActivity`，继续复用 `RollApi`、`BaseBindingActivity`、ARouter 和现有请求封装。列表只传递 `newsId`；详情页独立请求并把接口正文转换为原生可读文本，不嵌入网页。

**技术栈：** Kotlin、LiveData、ViewModel、Retrofit、Gson、Data Binding、ARouter、AndroidX HTML 兼容工具。

**设计说明：** `docs/superpowers/specs/2026-10-30-featured-news-detail-design.md`

## 全局约束

- 仅使用 MXNZP 新闻 V2 详情接口 `GET /api/news/details/v2`，参数为现有认证参数和 `newsId`。
- 所有接口模型字段使用 `@field:SerializedName`；接口可能缺失的正文、图片、视频字段使用可空类型。
- 不使用 WebView，不打开外部新闻站点，不新增第三方依赖。
- 页面与列表均使用 `background`、`surface` 和 `text*` 语义色，支持深色模式。
- 详情页不做缓存，不修改精选列表既有缓存策略。

## 审查重点

- 详情响应中缺失图片、正文或扩展媒体字段时仍应显示标题、来源和可返回页面。
- 接口返回 HTML 正文时，标签应被转换为可读原生文本而非以源码形式展示。
- 列表项复用后点击的 `newsId` 必须与当前位置项目一致。
- 详情请求失败时不得留下 Loading，也不得导致 Activity 无法返回。
- 深色模式下详情页的背景、标题、正文、元信息和分割线均须具备足够对比度。

---

### Task 1：详情模型与正文转换

**文件：**
- 新建：`feature/home/src/main/java/com/hash/home/data/model/NewsDetailBean.kt`
- 新建：`feature/home/src/main/java/com/hash/home/ui/detail/NewsDetailContentFormatter.kt`
- 新建：`feature/home/src/test/java/com/hash/home/ui/detail/NewsDetailContentFormatterTest.kt`

**接口：**
- 产出：`NewsDetailBean`，包含 `newsId`、`title`、`source`、`postTime`、`content`、`digest`、`imgList` 等接口详情字段。
- 产出：`NewsDetailContentFormatter.toPlainText(content: String?): String` 与 `format(content: String?): CharSequence`。

- [ ] **步骤 1：写正文转换失败测试**

```kotlin
@Test
fun `toPlainText converts HTML paragraphs and line breaks to readable text`() {
    assertEquals("第一段\n第二段", NewsDetailContentFormatter.toPlainText("<p>第一段</p><p>第二段</p>").trim())
}

@Test
fun `toPlainText returns empty text for missing content`() {
    assertTrue(NewsDetailContentFormatter.toPlainText(null).isEmpty())
}
```

- [ ] **步骤 2：运行测试并确认失败**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*NewsDetailContentFormatterTest'`

预期：因 `NewsDetailContentFormatter` 尚不存在而失败。

- [ ] **步骤 3：实现模型与 `format(content: String?): CharSequence`**

使用纯 Kotlin 的 `toPlainText` 处理段落、换行和标签，`format` 再使用 `HtmlCompat.fromHtml` 提供界面富文本；模型媒体字段使用空列表或可空类型表达接口差异。

- [ ] **步骤 4：重新运行测试并确认通过**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*NewsDetailContentFormatterTest'`

预期：两个测试通过。

### Task 2：请求层与详情状态

**文件：**
- 修改：`feature/home/src/main/java/com/hash/home/data/api/RollApi.kt`
- 新建：`feature/home/src/main/java/com/hash/home/data/repository/NewsDetailRepository.kt`
- 新建：`feature/home/src/main/java/com/hash/home/ui/detail/NewsDetailViewModel.kt`
- 新建：`feature/home/src/test/java/com/hash/home/data/model/NewsDetailBeanTest.kt`

**接口：**
- 消费：`NewsDetailBean`。
- 产出：`suspend fun NewsDetailRepository.load(newsId: String): NewsDetailBean`。
- 产出：`NewsDetailViewModel.load(newsId: String)`、`detail: LiveData<NewsDetailBean>`、`errorMessage: LiveData<String>`。

- [ ] **步骤 1：写模型反序列化失败测试**

```kotlin
@Test
fun `detail model accepts missing optional media fields`() {
    val detail = gson.fromJson("{\"newsId\":\"n1\",\"title\":\"标题\"}", NewsDetailBean::class.java)
    assertEquals("n1", detail.newsId)
    assertNull(detail.content)
}
```

- [ ] **步骤 2：运行测试并确认失败**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*NewsDetailBeanTest'`

预期：因详情模型尚不存在而失败。

- [ ] **步骤 3：新增 `newsDetail` 接口、Repository 与 ViewModel**

`RollApi.newsDetail` 使用既有 `app_id`、`app_secret` 和 `newsId`；Repository 使用现有 `request` 链路，将网络错误转为异常；ViewModel 在 `viewModelScope` 中发布成功数据或用户可见错误文本。

- [ ] **步骤 4：重新运行模型测试并确认通过**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*NewsDetailBeanTest'`

预期：测试通过。

### Task 3：原生详情页与列表跳转

**文件：**
- 修改：`core/router/src/main/java/com/hash/router/RouterActivityPath.kt`
- 修改：`feature/home/src/main/AndroidManifest.xml`
- 新建：`feature/home/src/main/java/com/hash/home/ui/detail/NewsDetailActivity.kt`
- 新建：`feature/home/src/main/res/layout/activity_news_detail.xml`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/FeaturedTabFragment.kt`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/adapter/FeaturedTabAdapter.kt`
- 修改：`feature/home/src/main/res/values/strings.xml`

**接口：**
- 消费：`RouterActivityPath.Home.NEWS_DETAIL`，参数键 `newsId`。
- 消费：`NewsDetailViewModel.detail` 与 `NewsDetailViewModel.errorMessage`。
- 产出：列表项点击后由 ARouter 跳转详情 Activity。

- [ ] **步骤 1：为 Adapter 写点击项目失败测试**

在现有 Adapter 测试目录新增测试，提交一条 `newsId = "n1"` 的数据并触发点击，断言回调收到该数据项。

- [ ] **步骤 2：运行测试并确认失败**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*FeaturedTabAdapter*'`

预期：因 Adapter 尚未提供点击回调而失败。

- [ ] **步骤 3：实现路由、Activity、布局与点击跳转**

详情布局使用可滚动原生内容区：顶部返回按钮、标题、来源/时间、正文、可选图片列表和空正文提示；颜色只使用语义资源。`FeaturedTabFragment` 接收 Adapter 点击回调并通过 ARouter 传递 `newsId`。

- [ ] **步骤 4：运行 Adapter 测试并确认通过**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*FeaturedTabAdapter*'`

预期：点击回调测试通过。

### Task 4：整体验证与提交

**文件：**
- 修改：上述实现文件及本计划、设计说明中的勾选状态。

- [ ] **步骤 1：运行详情相关单元测试**

运行：`./gradlew :feature:home:testDebugUnitTest --tests '*NewsDetail*' --tests '*FeaturedTabAdapter*'`

预期：全部通过。

- [ ] **步骤 2：编译与资源校验**

运行：`./gradlew :feature:home:compileReleaseKotlin :feature:home:packageReleaseResources`

预期：编译成功；若依赖链因本机 JDK 17 无法处理项目 Java 21 配置失败，记录完整失败模块与错误。

- [ ] **步骤 3：手工验证**

在浅色与深色模式分别验证：点击列表项进入详情、返回、含图片、无正文、请求失败；确认不打开外部网页且 Loading 结束。

- [ ] **步骤 4：提交**

```bash
git add docs/superpowers/specs/2026-10-30-featured-news-detail-design.md \
  docs/superpowers/plans/2026-10-30-featured-news-detail-implementation.md \
  feature/home core/router
git commit -m "新增精选新闻详情页"
```

## 自检

- 设计中的原生展示、无 WebView、无缓存、深色模式、媒体可选和失败可返回，分别由任务 1 至任务 4 覆盖。
- 正文转换、模型缺字段、列表点击与错误/主题手工检查均有对应验证。
- 新增接口、Repository、ViewModel、Activity 的命名和参数在任务间一致。
