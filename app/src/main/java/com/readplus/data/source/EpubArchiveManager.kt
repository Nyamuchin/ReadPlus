package com.readplus.data.source

import java.io.File
import java.util.zip.ZipFile

/**
 * EPUB 漫画解析器。
 *
 * EPUB 本质是 ZIP，但内容顺序由 OPF 文件（Package Document）中的 spine 定义，
 * 不能简单按文件名排序。
 *
 * 解析流程：
 *   1. 读 META-INF/container.xml → 找到 OPF 文件路径
 *   2. 读 OPF → 解析 manifest（id → href）和 spine（idref 列表）
 *   3. 按 spine 顺序：
 *      - 若 item 直接是图片 → 加入列表
 *      - 若是 XHTML → 打开查找 <img src>，解析相对路径 → 加入列表
 *
 * 失败时返回 null，调用方应回退到自然排序。
 */
object EpubArchiveManager {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")

    /**
     * 解析 EPUB，返回按阅读顺序排列的图片 entry 路径列表。
     * 解析失败返回 null。
     */
    fun parseContentOrder(zipPath: String): List<String>? {
        return runCatching {
            ZipFile(File(zipPath)).use { zf ->
                // 1. 读 container.xml
                val containerEntry = zf.getEntry("META-INF/container.xml")
                    ?: return@runCatching null
                val containerXml = zf.getInputStream(containerEntry)
                    .use { it.readBytes().toString(Charsets.UTF_8) }

                // 2. 找 rootfile full-path
                val opfPath = Regex("""full-path\s*=\s*["']([^"']+)["']""")
                    .find(containerXml)?.groupValues?.get(1)
                    ?: return@runCatching null

                // 3. 读 OPF
                val opfEntry = zf.getEntry(opfPath) ?: return@runCatching null
                val opfXml = zf.getInputStream(opfEntry)
                    .use { it.readBytes().toString(Charsets.UTF_8) }

                val opfDir = opfPath.substringBeforeLast('/', "")

                // 4. 解析 manifest
                val manifest = mutableMapOf<String, ManifestItem>()
                Regex("""<item\b[^>]*/?>""").findAll(opfXml).forEach { m ->
                    val tag = m.value
                    val id = Regex("""\bid\s*=\s*["']([^"']+)["']""")
                        .find(tag)?.groupValues?.get(1)
                    val href = Regex("""\bhref\s*=\s*["']([^"']+)["']""")
                        .find(tag)?.groupValues?.get(1)
                    val mediaType = Regex("""\bmedia-type\s*=\s*["']([^"']+)["']""")
                        .find(tag)?.groupValues?.get(1)
                    if (id != null && href != null) {
                        manifest[id] = ManifestItem(href, mediaType)
                    }
                }

                // 5. 解析 spine
                val spineIds = Regex("""<itemref\b[^>]*\bidref\s*=\s*["']([^"']+)["']""")
                    .findAll(opfXml).map { it.groupValues[1] }.toList()

                // 6. 遍历 spine，收集图片路径
                val ordered = mutableListOf<String>()
                for (id in spineIds) {
                    val item = manifest[id] ?: continue
                    val fullPath = resolvePath(opfDir, item.href)

                    val ext = item.href.substringAfterLast('.', "").lowercase()
                    val mt = item.mediaType ?: ""

                    if (mt.startsWith("image/") || ext in IMAGE_EXTENSIONS) {
                        // 直接是图片
                        ordered.add(fullPath)
                    } else if (mt == "application/xhtml+xml" || ext in setOf("xhtml", "html", "htm")) {
                        // 打开 XHTML 找图片
                        val htmlEntry = zf.getEntry(fullPath) ?: continue
                        val html = zf.getInputStream(htmlEntry)
                            .use { it.readBytes().toString(Charsets.UTF_8) }

                        // 支持 <img src="..."> 和 <image xlink:href="...">
                        val imgRegex = Regex(
                            """<(?:img|image)\b[^>]*?(?:src|xlink:href|href)\s*=\s*["']([^"']+)["']""",
                            RegexOption.IGNORE_CASE
                        )
                        val imgHref = imgRegex.find(html)?.groupValues?.get(1) ?: continue
                        val htmlDir = fullPath.substringBeforeLast('/', "")
                        val imgPath = resolvePath(htmlDir, imgHref)
                        ordered.add(imgPath)
                    }
                }

                if (ordered.isEmpty()) null else ordered
            }
        }.getOrNull()
    }

    /**
     * 解析相对路径，处理 "./" 和 "../"。
     */
    private fun resolvePath(base: String, relative: String): String {
        // 如果 relative 是绝对路径（以 / 开头），去掉前导 /
        val rel = relative.removePrefix("/")
        // 处理 URL 编码（如 %20 → 空格）
        val decoded = try {
            java.net.URLDecoder.decode(rel, "UTF-8")
        } catch (e: Exception) {
            rel
        }

        val combined = if (base.isEmpty()) decoded else "$base/$decoded"
        val parts = combined.split('/')
        val stack = mutableListOf<String>()
        for (p in parts) {
            when (p) {
                "", "." -> {}
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
                else -> stack.add(p)
            }
        }
        return stack.joinToString("/")
    }

    private data class ManifestItem(val href: String, val mediaType: String?)
}