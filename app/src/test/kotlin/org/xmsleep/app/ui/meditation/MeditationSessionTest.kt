package org.xmsleep.app.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MeditationSession / MeditationCategory 本地化名称取值的纯逻辑单测。
 * 回归保护：LanguageManager.Language.code 使用下划线（"zh_CN" / "zh_TW"），
 * 调用方（MeditationPlayerScreen / BreathingListScreen）传入的即为 .code。
 */
class MeditationSessionTest {

    private val session = MeditationSession(
        id = "s1",
        name = "简体名",
        nameEn = "English Name",
        nameJa = "日本語名",
        nameKo = "한국어명",
        nameRu = "Русское имя",
        nameZhTW = "繁體名",
        category = "cat",
        duration = 300,
        audioUrl = "https://example.com/a.mp3"
    )

    private val category = MeditationCategory(
        id = "c1",
        name = "简体分类",
        nameEn = "English Category",
        nameJa = "日本語分類",
        nameKo = "한국어 분류",
        nameRu = "Русская категория",
        nameZhTW = "繁體分類",
        order = 0
    )

    @Test
    fun simplifiedChineseCodeReturnsBaseName() {
        assertEquals("简体名", session.getLocalizedName("zh_CN"))
        assertEquals("简体分类", category.getLocalizedName("zh_CN"))
    }

    @Test
    fun legacyZhCodeReturnsBaseName() {
        assertEquals("简体名", session.getLocalizedName("zh"))
    }

    @Test
    fun traditionalChineseCodeReturnsTraditionalName() {
        assertEquals("繁體名", session.getLocalizedName("zh_TW"))
        assertEquals("繁體分類", category.getLocalizedName("zh_TW"))
    }

    @Test
    fun englishCodeReturnsEnglishName() {
        assertEquals("English Name", session.getLocalizedName("en"))
    }

    @Test
    fun otherCodesReturnTheirOwnNames() {
        assertEquals("日本語名", session.getLocalizedName("ja"))
        assertEquals("한국어명", session.getLocalizedName("ko"))
        assertEquals("Русское имя", session.getLocalizedName("ru"))
    }

    @Test
    fun traditionalFallsBackToBaseNameWhenEmpty() {
        val s = session.copy(nameZhTW = "")
        assertEquals("简体名", s.getLocalizedName("zh_TW"))
    }

    @Test
    fun unknownCodeFallsBackToBaseName() {
        assertEquals("简体名", session.getLocalizedName("xx"))
    }

    // ---- QA 追加边界用例（独立于工程师自测）----

    /** 繁体：category 的 nameZhTW 为空时也应回退到简体名。 */
    @Test
    fun categoryTraditionalFallsBackToBaseNameWhenEmpty() {
        val c = category.copy(nameZhTW = "")
        assertEquals("简体分类", c.getLocalizedName("zh_TW"))
    }

    /** 空语言码（调用方未取到语言时）应回退到简体名，不得崩溃。 */
    @Test
    fun emptyLanguageCodeFallsBackToBaseName() {
        assertEquals("简体名", session.getLocalizedName(""))
        assertEquals("简体分类", category.getLocalizedName(""))
    }

    /**
     * 锁定契约：仅接受下划线语言码（zh_CN / zh_TW）。
     * 连字符形式 "zh-TW" 不是合法 code，按未知码回退到简体名——
     * 这正是本缺陷的根因（旧实现用连字符分支，永远匹配不到）。
     */
    @Test
    fun hyphenatedZhTwIsNotRecognizedFallsBackToBaseName() {
        assertEquals("简体名", session.getLocalizedName("zh-TW"))
    }

    /** 大小写敏感：语言码含大小写变体时应按未知处理（防回归误判）。 */
    @Test
    fun caseSensitiveLanguageCodeFallsBackToBaseName() {
        assertEquals("简体名", session.getLocalizedName("ZH_TW"))
    }
}
