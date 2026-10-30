# 精选新闻详情内容流 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将新闻详情真实的 `items` 内容块按接口顺序展示为原生图文视频内容流。

**Architecture:** `NewsDetailBean` 仅描述详情 id 与接口条目；详情页面归属 `ui/featured/detail`。Activity 将头部和接口条目提交给单一 `RecyclerView`，由工厂直接生成文字、图片或视频条目。视频复用封面加载能力并交给系统播放器处理 URL。

**Tech Stack:** Kotlin、Gson、LiveData、Data Binding、Android Intent、ImageLoader。

**Spec:** `docs/superpowers/specs/2026-10-30-featured-news-content-flow-design.md`

## Global Constraints

- 严格按 `items` 数组顺序展示 `text`、`img`、`video`。
- 不新增第三方依赖或 WebView。
- 空字段和未知类型不展示占位。

## Review Focus

- 图文交错时图片必须位于相邻段落之间，而非统一追加到文末。
- 视频缺少封面或可播放地址时不应崩溃。
- `videoUrl` 是数组时只使用第一个非空地址。
- 页面仍需支持深色模式和返回操作。
- 详情接口不含标题元信息时，标题仍应能从列表项传入并显示。

---

### Task 1: 内容块模型与解析测试

**Files:**
- Modify: `feature/home/src/main/java/com/hash/home/data/model/NewsDetailBean.kt`
- Modify: `feature/home/src/test/java/com/hash/home/data/model/NewsDetailBeanTest.kt`

**Interfaces:**
- Produces: `NewsDetailBean(id: String, items: List<NewsDetailItem>)`。
- Produces: `NewsDetailItem(type: String, content: String, imageUrl: String, videoUrls: List<String>)`。

- [x] **Step 1: Write failing JSON parsing tests**
- [x] **Step 2: Run the focused test and confirm it fails**
- [x] **Step 3: Implement the exact response model**
- [x] **Step 4: Re-run the focused test and confirm it passes**

### Task 2: 顺序渲染与视频入口

**Files:**
- Modify: `feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt`
- Modify: `feature/home/src/main/res/layout/activity_news_detail.xml`
- Create: `feature/home/src/main/res/layout/item_news_detail_text.xml`
- Create: `feature/home/src/main/res/layout/item_news_detail_image.xml`
- Create: `feature/home/src/main/res/layout/item_news_detail_video.xml`
- Create: `feature/home/src/main/res/drawable/ic_news_detail_play.xml`

**Interfaces:**
- Consumes: `NewsDetailBean.items`。
- Produces: 顺序内容流与系统视频播放 Intent。

- [x] **Step 1: Write failing content-item classification test**
- [x] **Step 2: Run the focused test and confirm it fails**
- [x] **Step 3: Implement content-item classification and native rendering**
- [x] **Step 4: Re-run the focused test and confirm it passes**

### Task 3: Verification

- [x] **Step 1: Run `./gradlew :feature:home:testDebugUnitTest`**
- [x] **Step 2: Run `./gradlew :feature:home:packageDebugResources :feature:home:packageReleaseResources`**
- [ ] **Step 3: Manually verify text, image and video responses in light and dark mode**

### Task 4: RecyclerView 内容流重构

**Files:**
- Modify: `feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt`
- Modify: `feature/home/src/main/res/layout/activity_news_detail.xml`
- Create: `feature/home/src/main/java/com/hash/home/ui/featured/detail/adapter/NewsDetailAdapter.kt`
- Create: `feature/home/src/main/java/com/hash/home/ui/featured/detail/adapter/NewsDetailPageItem.kt`
- Create: `feature/home/src/main/res/layout/item_news_detail_header.xml`
- Create: `feature/home/src/main/res/layout/item_news_detail_empty.xml`

- [x] **Step 1: 为头部与内容块组合规则编写失败测试**
- [x] **Step 2: 以 RecyclerView 替换动态 addView 渲染**
- [x] **Step 3: 完成 debug/release 编译与资源校验**
