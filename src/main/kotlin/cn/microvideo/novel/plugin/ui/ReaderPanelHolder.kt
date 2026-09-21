package cn.microvideo.novel.plugin.ui

/**
 * 持有当前活跃的阅读面板与全局交互句柄
 */
object ReaderPanelHolder {

    @Volatile
    private var panel: ReaderPanel? = null

    @Volatile
    private var toggleToolbarCallback: (() -> Unit)? = null

    @Volatile
    private var toggleCatalogCallback: (() -> Unit)? = null

    @Volatile
    private var toggleCamouflageCallback: (() -> Unit)? = null

    @Volatile
    private var openBookCallback: (() -> Unit)? = null

    fun register(
        panel: ReaderPanel,
        onToggleToolbar: () -> Unit,
        onToggleCatalog: () -> Unit,
        onToggleCamouflage: () -> Unit,
        onOpenBook: () -> Unit
    ) {
        this.panel = panel
        this.toggleToolbarCallback = onToggleToolbar
        this.toggleCatalogCallback = onToggleCatalog
        this.toggleCamouflageCallback = onToggleCamouflage
        this.openBookCallback = onOpenBook
    }

    fun get(): ReaderPanel? = panel

    fun toggleToolbar() {
        toggleToolbarCallback?.invoke()
    }

    fun toggleCatalog() {
        toggleCatalogCallback?.invoke()
    }

    fun toggleCamouflage() {
        toggleCamouflageCallback?.invoke() ?: panel?.toggleCamouflage()
    }

    fun openBook() {
        openBookCallback?.invoke()
    }
}
