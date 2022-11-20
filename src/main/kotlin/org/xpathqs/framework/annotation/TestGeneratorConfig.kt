package org.xpathqs.framework.annotation

@Target(
    AnnotationTarget.CLASS,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestGeneratorConfig(
    val enableStaticSelectors: Boolean = false,
    val enableDynamicSelectors: Boolean = false,
    val enableValidations: Boolean = false,
    val enableNavigations: Boolean = false,
)

internal fun Any.getGeneratorConfig(): TestGeneratorConfig? {
    return this::class.annotations.filterIsInstance<TestGeneratorConfig>().firstOrNull()
}